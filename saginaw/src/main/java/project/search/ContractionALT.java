package project.search;

import gnu.trove.map.hash.THashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.mapdb.BTreeMap;
import project.map.MyGraph;

import java.util.*;

public class ContractionALT implements Searcher {
    long startTime, endTime, relaxTimeStart, relaxTimeEnd, totalRelaxTime, arelaxTimeStart, arelaxTimeEnd, atotalRelaxTime, containsTimeStart, containsTimeEnd, totalContainsTime, pollTimeStart, pollTimeEnd, totalPollTime, relaxPutTimeStart, relaxPutTimeEnd, totalRelaxPutTime;
    THashMap<Long, Double> uDistTo;
    THashMap<Long, Long> uEdgeTo;
    THashMap<Long, Long> uNodeTo;
    THashMap<Long, Double> vDistTo;
    THashMap<Long, Long> vEdgeTo;
    THashMap<Long, Long> vNodeTo;
    PriorityQueue<DijkstraEntry> uPq, vPq, coreSQ, coreTQ;
    private HashSet<Long> uRelaxed;
    private HashSet<Long> vRelaxed;
    public Long overlapNode;
    private double maxDist; //how far from the nodes we have explored - have we covered minimum distance yet?
    public double bestSeen;
    public long bestPathNode;
    public int exploredA, exploredB;
    private long startNode, endNode;
    public String filePrefix;
    private MyGraph graph;
    ArrayList<Long> landmarks;
    Long2ObjectOpenHashMap distancesTo;
    Long2ObjectOpenHashMap distancesFrom;
    private boolean foundRoute;
    private long start, end;

    public ContractionALT(MyGraph graph, ALTPreProcess altPreProcess) {
        int size = graph.getFwdGraph().size();

//        System.out.println("SIZE " + size);
        uDistTo = new THashMap<>(size);
        uEdgeTo = new THashMap<>(size);
        uNodeTo = new THashMap<>(size);

        vDistTo = new THashMap<>(size);
        vEdgeTo = new THashMap<>(size);
        vNodeTo = new THashMap<>(size);

        this.graph = graph;

        landmarks = new ArrayList<>();

        filePrefix = graph.getFilePrefix();

        this.landmarks = altPreProcess.landmarks;
        this.distancesFrom = altPreProcess.distancesFrom;
        this.distancesTo = altPreProcess.distancesTo;

        size = graph.getFwdGraph().size();

        uDistTo = new THashMap<>(size);
        uEdgeTo = new THashMap<>(size);
        uNodeTo = new THashMap<>(size);
        vDistTo = new THashMap<>(size);
        vEdgeTo = new THashMap<>(size);
        vNodeTo = new THashMap<>(size);

    }

    public ArrayList<Long> search(long startNode, long endNode){

        exploredA = 0;
        exploredB = 0;

        overlapNode = null;

        uDistTo.clear();
        vDistTo.clear();

        this.start = startNode;
        this.end = endNode;

        uDistTo.put(startNode, 0.0);
        vDistTo.put(endNode, 0.0);

//        System.out.println(uDistTo.size());
//        System.out.println(vDistTo.size());

        uEdgeTo.clear();
        vEdgeTo.clear();

        uNodeTo.clear();
        vNodeTo.clear();

//        System.out.println(uNodeTo.size());
//        System.out.println(uEdgeTo.size());

        uPq = new PriorityQueue<>(new DistanceComparator());
        vPq = new PriorityQueue<>(new DistanceComparator());

        coreSQ = new PriorityQueue<>(new DistanceComparator());
        coreTQ = new PriorityQueue<>(new DistanceComparator());

        uPq.add(new DijkstraEntry(startNode, 0.0));
        vPq.add(new DijkstraEntry(endNode, 0.0));

        uRelaxed = new HashSet<>();
        vRelaxed = new HashSet<>();

        bestSeen = Double.MAX_VALUE;
        bestPathNode = 0;


        double competitor;

        maxDist = 0;

        boolean isCore;

        DijkstraEntry v;

        STAGE1: while(!uPq.isEmpty() || !vPq.isEmpty()){ //check
//            System.out.println("LOOP");
//            explored += 2;
            if(!uPq.isEmpty()){
                v = uPq.poll();
                long v1 = v.getNode();
                isCore = graph.isCoreNode(v1);
//                System.out.println(v1);
                if(isCore){
//                    System.out.println("Found core node.");
                    coreSQ.add(v);
                }
//                System.out.println(graph.fwdAdj(v1).size() + " edges.");
                for (double[] e : graph.fwdAdj(v1)){
                    if(!isCore) {
//                        System.out.println("got here u");
                        relax(v1, e, true);
                        if (vRelaxed.contains((long) e[0])) {
                            competitor = (uDistTo.get(v1) + e[1] + vDistTo.get((long) e[0]));
                            if (bestSeen > competitor) {
                                bestSeen = competitor;
                                bestPathNode = v1;
                            }
                        }
                        if (vRelaxed.contains(v1)) {
                            if ((uDistTo.get(v1) + vDistTo.get(v1)) < bestSeen) {
                                overlapNode = v1;
                            } else {
                                overlapNode = bestPathNode;
                            }
                            if(bestSeen < (coreSQ.peek().getDistance() + coreTQ.peek().getDistance())){
                                foundRoute = true;
                                break STAGE1;
                            }
                        }
                    }
                }
            }

            if(!vPq.isEmpty()){
                v = vPq.poll();
                long v2 = v.getNode();
//                System.out.println(v2);
                isCore = graph.isCoreNode(v2);
                if(isCore){
//                    System.out.println("Found core node.");
                    coreTQ.add(v);
                }
//                System.out.println(graph.fwdAdj(v2).size() + " edges.");
                for (double[] e : graph.bckAdj(v2)) {
                    if(!isCore) {
//                        System.out.println("got here v");
                        relax(v2, e, false);
                        if (uRelaxed.contains((long) e[0])) {
                            competitor = (vDistTo.get(v2) + e[1] + uDistTo.get((long) e[0]));
                            if (bestSeen > competitor) {
                                bestSeen = competitor;
                                bestPathNode = v2;
                            }
                        }
                        if (uRelaxed.contains(v2)) {
                            if ((uDistTo.get(v2) + vDistTo.get(v2)) < bestSeen) {
                                bestSeen = uDistTo.get(v2) + vDistTo.get(v2);
                                overlapNode = v2;
                            } else {
                                overlapNode = bestPathNode;
                            }
//                            System.out.println("Found a route of " + bestSeen + " but core distances are " + coreSQ.peek().getDistance() + " " + coreTQ.peek().getDistance());
                            if(bestSeen < (coreSQ.peek().getDistance() + coreTQ.peek().getDistance())){
                                foundRoute = true;
                                break STAGE1;
                            }
                        }
                    }
                }
            }
        }

//        Runnable s = () -> {
//            boolean isCore;
//            while(!uPq.isEmpty() && !Thread.currentThread().isInterrupted()){
//                System.out.println("Outer loop.");
//                System.out.println(uPq.isEmpty());
//                exploredA++;
//                DijkstraEntry v = uPq.poll();
//                long v1 = v.getNode();
//                isCore = graph.isCoreNode(v1);
//                if(isCore){
//                    System.out.println("Found core node.");
//                    coreSQ.add(v);
//                }
//                for (double[] e : graph.fwdAdj(v1)){
//                    if(!Thread.currentThread().isInterrupted()) {
//                        if(!(isCore && graph.isCoreNode((long) e[0]))){
//                            relax(v1, e, true);
//                            if (vRelaxed.contains((long) e[0])) {
//                                System.out.println(uDistTo.get(v1));
//                                System.out.println(vDistTo.get((long) e[0]));
//                                double competitor = (uDistTo.get(v1) + e[1] + vDistTo.get((long) e[0]));
//                                if (bestSeen > competitor) {
//                                    bestSeen = competitor;
//                                    bestPathNode = v1;
//                                }
//                            }
//                            if (vRelaxed.contains(v1)) {
//                                System.out.println("contains");
//                                if ((uDistTo.get(v1) + vDistTo.get(v1)) < bestSeen) {
//                                    bestSeen = uDistTo.get(v1) + vDistTo.get(v1);
//                                    overlapNode = v1;
//                                } else {
//                                    overlapNode = bestPathNode;
//                                }
//                                if(bestSeen < (coreSQ.peek().getDistance() + coreTQ.peek().getDistance())){
//                                    foundRoute = true;
//                                    Thread.currentThread().interrupt();
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        };
//
//        Runnable t = () -> {
//            boolean isCore;
//            while(!vPq.isEmpty() && !Thread.currentThread().isInterrupted()){
//                System.out.println("Outer loop.");
//                exploredB++;
//                DijkstraEntry v = vPq.poll();
//                long v2 = v.getNode();
//                isCore = graph.isCoreNode(v2);
//                if(isCore){
//                    System.out.println("Found core node.");
//                    coreTQ.add(v);
//                }
//                for (double[] e : graph.bckAdj(v2)) {
//                    if(!Thread.currentThread().isInterrupted()){
//                        if(!(isCore && graph.isCoreNode((long) e[0]))){
//                            relax(v2, e, false);
//                            if (uRelaxed.contains((long) e[0])) {
//                                double competitor = (vDistTo.get(v2) + e[1] + uDistTo.get((long) e[0]));
//                                if (bestSeen > competitor) {
//                                    bestSeen = competitor;
//                                    bestPathNode = v2;
//                                }
//                            }
//                            if (uRelaxed.contains(v2)) {
//                                System.out.println("contains");
//                                if ((uDistTo.get(v2) + vDistTo.get(v2)) < bestSeen) {
//                                    bestSeen = uDistTo.get(v2) + vDistTo.get(v2);
//                                    overlapNode = v2;
//                                } else {
//                                    overlapNode = bestPathNode;
//                                }
//                                if(bestSeen < (coreSQ.peek().getDistance() + coreTQ.peek().getDistance())){
//                                    foundRoute = true;
//                                    Thread.currentThread().interrupt();
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        };
//
//        Thread sThread = new Thread(s);
//        Thread tThread = new Thread(t);
//
//        sThread.start();
//        tThread.start();
//
//        while(sThread.isAlive() && tThread.isAlive()){
//
//        }
//
//        sThread.interrupt();
//        tThread.interrupt();

        if(!foundRoute){
//            System.out.println("SECOND STAGE");
            //do second stage to get overlap, otherwise we continue below
            secondStage(coreSQ, coreTQ);
        }

        if(overlapNode == null){
            System.out.println("No route found.");
            return new ArrayList<>();
        }

        long endTime = System.nanoTime();
//        System.out.println("OVERLAP: " + overlapNode);
//        System.out.println("BiDijkstra time: " + (((float) endTime - (float) startTime) / 1000000000));

        return getRouteAsWays();

    }

    private void relax(Long x, double[] edge, boolean u){
        relaxTimeStart = System.nanoTime();
        long w = (long) edge[0];
        double weight = edge[1];
        double wayId = edge[2];
        if(u){
            uRelaxed.add(x);
            double distToX = uDistTo.getOrDefault(x, Double.MAX_VALUE);
            if (uDistTo.getOrDefault(w, Double.MAX_VALUE) > (distToX + weight)){
//                System.out.println("add u.");
                relaxPutTimeStart = System.nanoTime();
                uDistTo.put(w, distToX + weight);
                uNodeTo.put(w, x); //should be 'nodeBefore'
                uEdgeTo.put(w, (long) wayId);
//                System.out.println(w + " " + Math.round(wayId));
                relaxPutTimeEnd = System.nanoTime();
                totalRelaxPutTime += (relaxPutTimeEnd - relaxPutTimeStart);
                arelaxTimeStart = System.nanoTime();
                uPq.add(new DijkstraEntry(w, distToX + weight)); //inefficient?
                arelaxTimeEnd = System.nanoTime();
                atotalRelaxTime += (arelaxTimeEnd - arelaxTimeStart);
            }
        } else {
            vRelaxed.add(x);
            double distToX = vDistTo.getOrDefault(x, Double.MAX_VALUE);
            if (vDistTo.getOrDefault(w, Double.MAX_VALUE) > (distToX + weight)){
//                System.out.println("add v.");
                relaxPutTimeStart = System.nanoTime();
                vDistTo.put(w, distToX + weight);
                vNodeTo.put(w, x); //should be 'nodeBefore'
                vEdgeTo.put(w, (long) wayId);
//                System.out.println(w + " " + Math.round(wayId));
                relaxPutTimeEnd = System.nanoTime();
                totalRelaxPutTime += (relaxPutTimeEnd - relaxPutTimeStart);
                arelaxTimeStart = System.nanoTime();
                vPq.add(new DijkstraEntry(w, distToX + weight)); //inefficient?
                arelaxTimeEnd = System.nanoTime();
                atotalRelaxTime += (arelaxTimeEnd - arelaxTimeStart);
            }
        }
        relaxTimeEnd = System.nanoTime();
        totalRelaxTime += (relaxTimeEnd - relaxTimeStart);
    }


    private void secondStage(PriorityQueue coreSQ, PriorityQueue coreTQ){

        uPq = coreSQ;
        vPq = coreTQ;

//        System.out.println("uPq " + coreSQ.size());
//        System.out.println("vPq " + coreTQ.size());

        uRelaxed = new HashSet<>();
        vRelaxed = new HashSet<>();

        bestSeen = Double.MAX_VALUE;

        exploredB = 0;
        exploredA = 0;

        Runnable s = () -> {
            long startTime = System.nanoTime();
            while(!uPq.isEmpty() && !Thread.currentThread().isInterrupted()){
                exploredA++;
                long v1 = uPq.poll().getNode();
                for (double[] e : graph.fwdCoreAdj(v1)){
                    if(!Thread.currentThread().isInterrupted()) {
                        relaxALT(v1, e, true);
                        if (vRelaxed.contains((long) e[0])) {
                            double competitor = (uDistTo.get(v1) + e[1] + vDistTo.get((long) e[0]));
                            if (bestSeen > competitor) {
                                bestSeen = competitor;
                                bestPathNode = v1;
                            }
                        }
                        if (vRelaxed.contains(v1)) {
//                            System.out.println("done");
                            if ((uDistTo.get(v1) + vDistTo.get(v1)) < bestSeen) {
                                overlapNode = v1;
                            } else {
                                overlapNode = bestPathNode;
                            }
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        };

        Runnable t = () -> {
            long startTime = System.nanoTime();
            while(!vPq.isEmpty() && !Thread.currentThread().isInterrupted()){
                exploredB++;
                long v2 = vPq.poll().getNode();
//                System.out.println(graph.bckCoreAdj(v2).size() + " size bckCoreAdj");
                for (double[] e : graph.bckCoreAdj(v2)){
                    if(!Thread.currentThread().isInterrupted()) {
//                        System.out.println("t relax");
                        relaxALT(v2, e, false);
                        if (uRelaxed.contains((long) e[0])) {
                            double competitor = (vDistTo.get(v2) + e[1] + uDistTo.get((long) e[0]));
                            if (bestSeen > competitor) {
                                bestSeen = competitor;
                                bestPathNode = v2;
                            }
                        }
                        if (uRelaxed.contains(v2)) {
//                            System.out.println("done");
                            if ((uDistTo.get(v2) + vDistTo.get(v2)) < bestSeen) {
                                overlapNode = v2;
                            } else {
                                overlapNode = bestPathNode;
                            }
                            long endTime = System.nanoTime();
//                            System.out.println(Thread.currentThread().getId() + ": " + (((float) endTime - (float) startTime) / 1000000000));
                            Thread.currentThread().interrupt();
                        }
                    }
                }
//                System.out.println("vPq empty: " + vPq.isEmpty());
            }
        };

        Thread sThread = new Thread(s);
        Thread tThread = new Thread(t);

        sThread.start();
        tThread.start();

        while(sThread.isAlive() && tThread.isAlive()){
        }
        sThread.interrupt();
        tThread.interrupt();
//        System.out.println(uRelaxed.size());
//        System.out.println(vRelaxed.size());
//        System.out.println(exploredA);
//        System.out.println(exploredB);
    }



    private void relaxALT(Long x, double[] edge, boolean u){
//        System.out.println("Relaxing " + x);
        relaxTimeStart = System.nanoTime();
        long w = (long) edge[0];
        double weight = edge[1];
        double wayId = edge[2];
        if(u){
            uRelaxed.add(x);
//            System.out.println("Relax " + x);
            double distToX = uDistTo.getOrDefault(x, Double.MAX_VALUE);
            if (uDistTo.getOrDefault(w, Double.MAX_VALUE) > (distToX + weight)){
//                System.out.println("true");
                relaxPutTimeStart = System.nanoTime();
                uDistTo.put(w, distToX + weight);
                uNodeTo.put(w, x); //should be 'nodeBefore'
                uEdgeTo.put(w, (long) wayId); //should be 'nodeBefore'
                relaxPutTimeEnd = System.nanoTime();
                totalRelaxPutTime += (relaxPutTimeEnd - relaxPutTimeStart);
                arelaxTimeStart = System.nanoTime();
                uPq.add(new DijkstraEntry(w, distToX + weight + lowerBound(w, true))); //inefficient?
                arelaxTimeEnd = System.nanoTime();
                atotalRelaxTime += (arelaxTimeEnd - arelaxTimeStart);
            } else {
//                System.out.println("false");
            }
        } else {
            vRelaxed.add(x);
            double distToX = vDistTo.getOrDefault(x, Double.MAX_VALUE);
            if (vDistTo.getOrDefault(w, Double.MAX_VALUE) > (distToX + weight)){
                relaxPutTimeStart = System.nanoTime();
                vDistTo.put(w, distToX + weight);
                vNodeTo.put(w, x); //should be 'nodeBefore'
                vEdgeTo.put(w, (long) wayId); //should be 'nodeBefore'
                relaxPutTimeEnd = System.nanoTime();
                totalRelaxPutTime += (relaxPutTimeEnd - relaxPutTimeStart);
                arelaxTimeStart = System.nanoTime();
                vPq.add(new DijkstraEntry(w, distToX + weight + lowerBound(w, false))); //inefficient?
                arelaxTimeEnd = System.nanoTime();
                atotalRelaxTime += (arelaxTimeEnd - arelaxTimeStart);
            }
        }
        relaxTimeEnd = System.nanoTime();
        totalRelaxTime += (relaxTimeEnd - relaxTimeStart);
    }

    public double lowerBound(long u, boolean forwards){
        double maxForward = 0;
        double maxBackward = 0;
//        double[] dTU, dFU, dTV, dFV;

//        System.out.println(u);
//        System.out.println(u);
//        System.out.println(distancesTo.get(u));
        double[] forDTU = (double[]) distancesTo.get(u);
        double[] forDFU = (double[]) distancesFrom.get(u);
        double[] forDTV = (double[]) distancesTo.get(end);
        double[] forDFV = (double[]) distancesFrom.get(end);

        double[] backDTU = (double[]) distancesTo.get(u);
        double[] backDFU = (double[]) distancesFrom.get(u);
        double[] backDTV = (double[]) distancesTo.get(start);
        double[] backDFV = (double[]) distancesFrom.get(start);

        for(int l = 0; l < landmarks.size(); l++){
            maxForward = Math.max(maxForward, Math.max(forDTU[l] - forDTV[l], forDFV[l] - forDFU[l]));
        }

        for(int l = 0; l < landmarks.size(); l++){
            maxBackward = Math.max(maxBackward, Math.max(backDTU[l] - backDTV[l], backDFV[l] - backDFU[l]));
        }

        if(forwards){
            return (maxForward - maxBackward) / 2;
        } else {
            return (maxBackward - maxForward) / 2;
        }
    }

    public double getDist() {
        return uDistTo.get(overlapNode) + vDistTo.get(overlapNode);
    }

    public class DistanceComparator implements Comparator<DijkstraEntry>{
        public int compare(DijkstraEntry x, DijkstraEntry y){
            if(x.getDistance() < y.getDistance()){
                return -1;
            }
            if(x.getDistance() > y.getDistance()){
                return 1;
            }
            else return 0;
        }
    }

    private double haversineDistance(long a, long b, BTreeMap<Long, double[]> dictionary){
        double[] nodeA = dictionary.get(a);
        double[] nodeB = dictionary.get(b);
        double rad = 6371000; //radius of earth in metres
        double aLatRadians = Math.toRadians(nodeA[0]); //0 = latitude, 1 = longitude
        double bLatRadians = Math.toRadians(nodeB[0]);
        double deltaLatRadians = Math.toRadians(nodeB[0] - nodeA[0]);
        double deltaLongRadians = Math.toRadians(nodeB[1] - nodeA[1]);

        double x = Math.sin(deltaLatRadians/2) * Math.sin(deltaLatRadians/2) +
                Math.cos(aLatRadians) * Math.cos(bLatRadians) *
                        Math.sin(deltaLongRadians/2) * Math.sin(deltaLongRadians/2);
        double y = 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1-x));
        return rad * y;
    }

    public ArrayList<Long> getRoute(){
        ArrayList<Long> route = new ArrayList<>();
        long node = overlapNode;
        route.add(overlapNode);
        while(node != startNode){
            node = uNodeTo.get(node);
            route.add(node);
        }
        Collections.reverse(route);
        node = overlapNode;
        while(node != endNode){
            node = vNodeTo.get(node);
            route.add(node);
        }
        return route;
    }

    public ArrayList<Long> getRouteAsWays(){
        long node = overlapNode;
        ArrayList<Long> route = new ArrayList<>();
        try{
//            System.out.println("GETROUTEASWAYS");
            long way = 0;
            while(node != startNode && node != endNode){
//            System.out.println(node + ",");
                way = uEdgeTo.get(node);
                node = uNodeTo.get(node);
//            System.out.println(way);
                route.add(way);
            }

            Collections.reverse(route);
            node = overlapNode;
            while(node != startNode && node != endNode){
//            System.out.println(node + ".");
                way = vEdgeTo.get(node);
                node = vNodeTo.get(node);
//            System.out.println(way);
                route.add(way);
            }

        }catch(NullPointerException n){
//            System.out.println("Null: " + node);
//            System.out.println(n.getStackTrace());
        }
        return route;
    }




    private void timerStart(){
        startTime = System.nanoTime();
    }

    private void timerEnd(String string){
        endTime = System.nanoTime();
        System.out.println(string + " time: " + (((float) endTime - (float)startTime) / 1000000000));
    }

    public void clear(){
        uDistTo.clear();
        uEdgeTo.clear();
        vDistTo.clear();
        vEdgeTo.clear();
        vPq.clear();
        uPq.clear();
        vRelaxed.clear();
        uRelaxed.clear();
    }

    public int getExplored(){
        return exploredA + exploredB;
    }
}