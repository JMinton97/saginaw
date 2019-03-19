package project.search;

import gnu.trove.map.hash.THashMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import javafx.util.Pair;
import org.mapdb.BTreeMap;
import project.map.MyGraph;

import java.util.*;

public class ContractionALT implements Searcher {
    private Int2DoubleOpenHashMap uDistTo;
    private Int2LongOpenHashMap uEdgeTo;
    private Int2IntOpenHashMap uNodeTo;
    private Int2DoubleOpenHashMap vDistTo;
    private Int2LongOpenHashMap vEdgeTo;
    private Int2IntOpenHashMap vNodeTo;
    private PriorityQueue<DijkstraEntry> uPq, vPq, coreSQ, coreTQ;
    private HashSet<Integer> uRelaxed;
    private HashSet<Integer> vRelaxed;
    private int overlapNode;
    private double maxDist; //how far from the nodes we have explored - have we covered minimum distance yet?
    private double bestSeen;
    private int bestPathNode;
    private int explored;
    private MyGraph graph;
    private ArrayList<Integer> landmarks;
    private Int2ObjectOpenHashMap distancesTo;
    private Int2ObjectOpenHashMap distancesFrom;
    private boolean routeFound;
    private int start, end;
    private int proxyStart, proxyEnd;
    private double[] forDTV, forDFV, backDTV, backDFV;
    private String name = "calt";
    private ALTPreProcess alt;


    public ContractionALT(MyGraph graph, ALTPreProcess altPreProcess) {
        int size = graph.getFwdGraph().size();

        coreSQ = new PriorityQueue<>(new DistanceComparator());
        coreTQ = new PriorityQueue<>(new DistanceComparator());

        this.graph = graph;

        landmarks = new ArrayList<>();

        this.landmarks = altPreProcess.landmarks;
        this.distancesFrom = altPreProcess.distancesFrom;
        this.distancesTo = altPreProcess.distancesTo;

        uDistTo = new Int2DoubleOpenHashMap();
        uEdgeTo = new Int2LongOpenHashMap();
        uNodeTo = new Int2IntOpenHashMap();

        vDistTo = new Int2DoubleOpenHashMap();
        vEdgeTo = new Int2LongOpenHashMap();
        vNodeTo = new Int2IntOpenHashMap();

        uRelaxed = new HashSet<>();
        vRelaxed = new HashSet<>();

        Comparator<DijkstraEntry> comparator = new DistanceComparator();
        uPq = new PriorityQueue<DijkstraEntry>(comparator);
        vPq = new PriorityQueue<DijkstraEntry>(comparator);

        this.alt = altPreProcess;

    }

    public void search(int startNode, int endNode){

        routeFound = false;

        explored = 0;

        overlapNode = -1;

        this.start = startNode;
        this.end = endNode;

        uDistTo.put(startNode, 0.0);
        vDistTo.put(endNode, 0.0);

        uPq.add(new DijkstraEntry(startNode, 0.0));
        vPq.add(new DijkstraEntry(endNode, 0.0));

        bestSeen = Double.MAX_VALUE;
        bestPathNode = 0;

        double competitor;

        maxDist = 0;

        boolean isCore;

        DijkstraEntry v;

        STAGE1: while(!uPq.isEmpty() || !vPq.isEmpty()){ //check
            if(!uPq.isEmpty()){
                explored++;
                v = uPq.poll();
                int v1 = v.getNode();
                isCore = graph.isCoreNode(v1);
                if(isCore){
                    coreSQ.add(v);
                }
                for (double[] e : graph.fwdAdj(v1)){
                    if(!isCore) {
                        relax(v1, e, true);
                        if (vRelaxed.contains((int) e[0])) {
                            competitor = (uDistTo.get(v1) + e[1] + vDistTo.get((int) e[0]));
                            if (bestSeen > competitor) {
                                bestSeen = competitor;
                                bestPathNode = v1;
                            }
                        }
                        if (vRelaxed.contains(v1)) {
                            if ((uDistTo.get(v1) + vDistTo.get(v1)) < bestSeen) {
                                bestSeen = uDistTo.get(v1) + vDistTo.get(v1);
                                overlapNode = v1;
                            } else {
                                overlapNode = bestPathNode;
                            }
//                            System.out.println(bestSeen);
//                            System.out.println(coreSQ.peek().getDistance());
//                            System.out.println(coreTQ.peek().getDistance());
                            if(bestSeen < (coreSQ.peek().getDistance() + coreTQ.peek().getDistance())){
                                routeFound = true;
//                                System.out.println("break stage 1");
                                break STAGE1;
                            }
                        }
                    }
                }
            }

            if(!vPq.isEmpty()){
                explored++;
                v = vPq.poll();
                int v2 = v.getNode();
                isCore = graph.isCoreNode(v2);
                if(isCore){
                    coreTQ.add(v);
                }
                for (double[] e : graph.bckAdj(v2)) {
                    if(!isCore) {
                        relax(v2, e, false);
                        if (uRelaxed.contains((int) e[0])) {
                            competitor = (vDistTo.get(v2) + e[1] + uDistTo.get((int) e[0]));
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
//                            System.out.println(bestSeen);
//                            System.out.println(coreSQ.peek().getDistance());
//                            System.out.println(coreTQ.peek().getDistance());
                            if(bestSeen < (coreSQ.peek().getDistance() + coreTQ.peek().getDistance())){
                                routeFound = true;
//                                System.out.println("break stage 1");
                                break STAGE1;
                            }
                        }
                    }
                }
            }
        }

        if(!routeFound){
            //do second stage to get overlap, otherwise we continue below
//            System.out.println("First stage: " + explored);
            secondStage();
            if(!routeFound){
//                System.out.println("No route found.");
                routeFound = false;
            } else {}
        } else {
            System.out.println("NO SECOND STAGE");
        }

//        System.out.println("Ended search");

    }

    private void relax(int x, double[] edge, boolean u){
        int w = (int) edge[0];
        double weight = edge[1];
        double wayId = edge[2];
        if(u){
            double distToX = uDistTo.getOrDefault(x, Double.MAX_VALUE);
            if (uDistTo.getOrDefault(w, Double.MAX_VALUE) > (distToX + weight)){
                uDistTo.put(w, distToX + weight);
                uNodeTo.put(w, x); //should be 'nodeBefore'
                uEdgeTo.put(w, (long) wayId);
                uPq.add(new DijkstraEntry(w, distToX + weight)); //inefficient?
            }else{
//                if(uDistTo.get(x) == null){
//                    System.out.println("AAAGHHGHH");
//                }
            }
            uRelaxed.add(x);
        } else {
            double distToX = vDistTo.getOrDefault(x, Double.MAX_VALUE);
            if (vDistTo.getOrDefault(w, Double.MAX_VALUE) > (distToX + weight)){
                vDistTo.put(w, distToX + weight);
                vNodeTo.put(w, x); //should be 'nodeBefore'
                vEdgeTo.put(w, (long) wayId);
                vPq.add(new DijkstraEntry(w, distToX + weight)); //inefficient?
            }else {
//                if (vDistTo.get(x) == null) {
//                    System.out.println("AAAGHHGHH");
//                }
            }
            vRelaxed.add(x);
        }
    }

    private void secondStage(){
        uRelaxed.clear();
        vRelaxed.clear();

        bestSeen = Double.MAX_VALUE;

        proxyStart = coreSQ.peek().getNode();
        proxyEnd = coreTQ.peek().getNode();

        forDTV = (double[]) distancesTo.get(proxyEnd);
        forDFV = (double[]) distancesFrom.get(proxyEnd);
        backDTV = (double[]) distancesTo.get(proxyStart);
        backDFV = (double[]) distancesFrom.get(proxyStart);

//        System.out.println("stage 2");

        Runnable s = () -> {
            while(!coreSQ.isEmpty() && !Thread.currentThread().isInterrupted()){
                explored++;
                int v1 = coreSQ.poll().getNode();
                for (double[] e : graph.fwdCoreAdj(v1)){
                    if(!Thread.currentThread().isInterrupted()) {
                        relaxALT(v1, e, true);
                        if (vRelaxed.contains((int) e[0])) {
//                            System.out.println("uDistTo.get(v1) " + uDistTo.get(v1));
//                            System.out.println("e[1] " + e[1]);
//                            System.out.println("vDistTo.get((int) e[0]) " + vDistTo.get((int) e[0]));
                            double competitor = (uDistTo.get(v1) + e[1] + vDistTo.get((int) e[0]));
                            if (bestSeen > competitor) {
                                bestSeen = competitor;
                                bestPathNode = v1;
                            }
                        }
                        if (vRelaxed.contains(v1)) {
                            if ((uDistTo.get(v1) + vDistTo.get(v1)) < bestSeen) {
                                overlapNode = v1;
                            } else {
//                                System.out.println("u ALTERNATE");
//                                System.out.println("u bestPathNode: " + bestPathNode);
//                                System.out.println("u uNode to " + uNodeTo.get(bestPathNode));
//                                System.out.println("u bestPathNode: " + bestPathNode);
//                                System.out.println("u uDist to " + uDistTo.get(bestPathNode));
//                                System.out.println("u bestPathNode: " + bestPathNode);
//                                System.out.println("u vNode to " + vNodeTo.get(bestPathNode));
//                                System.out.println("u bestPathNode: " + bestPathNode);
//                                System.out.println("u vDist to " + vDistTo.get(bestPathNode));
//                                System.out.println("u bestPathNode: " + bestPathNode);
                                overlapNode = bestPathNode;
                            }
                            routeFound = true;
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        };

        Runnable t = () -> {
            while(!coreTQ.isEmpty() && !Thread.currentThread().isInterrupted()){
                explored++;
                int v2 = coreTQ.poll().getNode();
                for (double[] e : graph.bckCoreAdj(v2)){
                    if(!Thread.currentThread().isInterrupted()) {
                        relaxALT(v2, e, false);
                        if (uRelaxed.contains((int) e[0])) {
//                            System.out.println("vDistTo.get(v2) " + vDistTo.get(v2));
//                            System.out.println("e[1] " + e[1]);
//                            System.out.println("uDistTo.get((int) e[0]) " + uDistTo.get((int) e[0]));
                            double competitor = (vDistTo.get(v2) + e[1] + uDistTo.get((int) e[0]));
                            if (bestSeen > competitor) {
                                bestSeen = competitor;
                                bestPathNode = v2;
                            }
                        }
                        if (uRelaxed.contains(v2)) {
                            if ((uDistTo.get(v2) + vDistTo.get(v2)) < bestSeen) {
                                overlapNode = v2;
                            } else {
//                                System.out.println("v ALTERNATE");
//                                System.out.println("v bestPathNode: " + bestPathNode);
//                                System.out.println("v uNode to " + uNodeTo.get(bestPathNode));
//                                System.out.println("v bestPathNode: " + bestPathNode);
//                                System.out.println("v uDist to " + uDistTo.get(bestPathNode));
//                                System.out.println("v bestPathNode: " + bestPathNode);
//                                System.out.println("v vNode to " + vNodeTo.get(bestPathNode));
//                                System.out.println("v bestPathNode: " + bestPathNode);
//                                System.out.println("v vDist to " + vDistTo.get(bestPathNode));
//                                System.out.println("v bestPathNode: " + bestPathNode);
                                overlapNode = bestPathNode;
                            }
                            routeFound = true;
                            Thread.currentThread().interrupt();
                        }
                    }
                }
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
    }

    private void relaxALT(int x, double[] edge, boolean u){
        int w = (int) edge[0];
        double weight = edge[1];
        double wayId = edge[2];
        if(u){
            double distToX = uDistTo.getOrDefault(x, Double.MAX_VALUE);
            if (uDistTo.getOrDefault(w, Double.MAX_VALUE) > (distToX + weight)){
                uDistTo.put(w, distToX + weight);
                uNodeTo.put(w, x); //should be 'nodeBefore'
                uEdgeTo.put(w, (long) wayId); //should be 'nodeBefore'
                coreSQ.add(new DijkstraEntry(w, distToX + weight + lowerBound(w, true))); //inefficient?
            }
            uRelaxed.add(x);
//            if(uDistTo.get(x) == null){
//                System.out.println("problem");
//            }
        } else {
            double distToX = vDistTo.getOrDefault(x, Double.MAX_VALUE);
            if (vDistTo.getOrDefault(w, Double.MAX_VALUE) > (distToX + weight)){
                vDistTo.put(w, distToX + weight);
                vNodeTo.put(w, x); //should be 'nodeBefore'
                vEdgeTo.put(w, (long) wayId); //should be 'nodeBefore'
                coreTQ.add(new DijkstraEntry(w, distToX + weight + lowerBound(w, false))); //inefficient?
            }
            vRelaxed.add(x);
//            if(vDistTo.get(x) == null){
//                System.out.println("problem");
//            }
        }
    }

    public double lowerBound(int u, boolean forwards){
//        System.out.println();
        double maxForward = 0;
        double maxBackward = 0;

        double[] forDTU = (double[]) distancesTo.get(u);
        double[] forDFU = (double[]) distancesFrom.get(u);


        double[] backDTU = (double[]) distancesTo.get(u);
        double[] backDFU = (double[]) distancesFrom.get(u);

        for(int l = 0; l < landmarks.size(); l++){
            maxForward = Math.max(maxForward, Math.max(forDTU[l] - forDTV[l], forDFV[l] - forDFU[l]));
        }

//        System.out.println("MAXBACKWARD " + maxBackward);
        for(int l = 0; l < landmarks.size(); l++){
            maxBackward = Math.max(maxBackward, Math.max(backDTU[l] - backDTV[l], backDFV[l] - backDFU[l]));
//            System.out.println("A " + backDTU[l] + " " + backDTV[l] + " " + backDFV[l] + " " + backDFU[l]);
//            System.out.println("B " + Math.max(backDTU[l] - backDTV[l], backDFV[l] - backDFU[l]));
        }
//        System.out.println("C " + maxBackward);

        if(forwards){
//            System.out.println("D " + (maxForward - maxBackward) / 2);
            return (maxForward - maxBackward) / 2;
        } else {
//            System.out.println("D " + (maxForward - maxBackward) / 2);
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

    public ArrayList<Integer> getRoute(){
        if(routeFound){
            int node = overlapNode;
//            System.out.println(overlapNode);
            ArrayList<Integer> route = new ArrayList<>();
            try{
                long way = 0;
                while(node != start && node != end){
//                    way = uEdgeTo.get(node);
                    node = uNodeTo.get(node);
//                    if(node == -1){
//                        break;
//                    }
                    route.add(node);
//                    System.out.println(node);
                }

//                System.out.println("Done to.");

                Collections.reverse(route);
//                System.out.println(overlapNode);
                node = overlapNode;
                while(node != start && node != end){
//                    way = vEdgeTo.get(node);
                    node = vNodeTo.get(node);
//                    if(node == -1){
//                        break;
//                    }
                    route.add(node);
//                    System.out.println(node);
                }

            }catch(NullPointerException n){
                System.out.println("null!");
                n.printStackTrace();
            }
            return route;
        }else{
            return new ArrayList<>();
        }
    }

    public ArrayList<Long> getRouteAsWays(){

//        System.out.println(start + " " + end);
        if(routeFound){
            int node = overlapNode;
//            System.out.println(overlapNode);
            ArrayList<Long> route = new ArrayList<>();
            try{
                long way = 0;
                while(node != start && node != end){
                    way = uEdgeTo.get(node);
                    node = uNodeTo.get(node);
//                    if(node == -1){
//                        break;
//                    }
                    route.add(way);
//                    System.out.println(node);
                }

//                System.out.println("Done to.");

                Collections.reverse(route);
//                System.out.println(overlapNode);
                node = overlapNode;
                while(node != start && node != end){
                    way = vEdgeTo.get(node);
                    node = vNodeTo.get(node);
//                    if(node == -1){
//                        break;
//                    }
                    route.add(way);
//                    System.out.println(node);
                }

            }catch(NullPointerException n){
                System.out.println("null!");
            }
            return route;
        }else{
            return new ArrayList<>();
        }
    }


    public void clear(){
        uDistTo.clear();
        uEdgeTo.clear();
        vDistTo.clear();
        vEdgeTo.clear();
        uNodeTo.clear();
        vNodeTo.clear();
        coreSQ.clear();
        coreTQ.clear();
        if(vPq != null){if(!vPq.isEmpty()){vPq.clear();}}
        if(uPq != null){if(!uPq.isEmpty()){uPq.clear();}}
        if(vRelaxed != null){vRelaxed.clear();}
        if(uRelaxed != null){uRelaxed.clear();}
        routeFound = false;
//        overlapNode = -1;
    }

    public int getExplored(){
        return explored;
    }

    public boolean routeFound(){
        return routeFound;
    }

    public ArrayList<ArrayList<Integer>> getRelaxedNodes() {
        ArrayList<ArrayList<Integer>> relaxedNodes = new ArrayList();
        relaxedNodes.add(new ArrayList<>(uRelaxed));
        relaxedNodes.add(new ArrayList<>(vRelaxed));
        return relaxedNodes;
    }

    public String getName(){
        return name;
    }

    @Override
    public ALTPreProcess getALT() {
        return alt;
    }
}