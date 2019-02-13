package project.search;

import gnu.trove.map.hash.THashMap;
import org.mapdb.BTreeMap;
import project.map.MyGraph;

import java.util.*;

public class BiDijkstra implements Searcher {
    long startTime, endTime, relaxTimeStart, relaxTimeEnd, totalRelaxTime, arelaxTimeStart, arelaxTimeEnd, atotalRelaxTime, containsTimeStart, containsTimeEnd, totalContainsTime, pollTimeStart, pollTimeEnd, totalPollTime, relaxPutTimeStart, relaxPutTimeEnd, totalRelaxPutTime;
    THashMap<Long, Double> uDistTo;
    THashMap<Long, Long> uEdgeTo;
    THashMap<Long, Long> uNodeTo;
    THashMap<Long, Double> vDistTo;
    THashMap<Long, Long> vEdgeTo;
    THashMap<Long, Long> vNodeTo;
    PriorityQueue<DijkstraEntry> uPq;
    PriorityQueue<DijkstraEntry> vPq;
    private HashSet<Long> uRelaxed;
    private HashSet<Long> vRelaxed;
    public Long overlapNode;
    private double maxDist; //how far from the nodes we have explored - have we covered minimum distance yet?
    public double bestSeen;
    public int explored;
    private long startNode, endNode;
    private MyGraph graph;

    public BiDijkstra(MyGraph graph) {
        int size = graph.getFwdGraph().size();

//        System.out.println("SIZE " + size);
        uDistTo = new THashMap<>(size);
        uEdgeTo = new THashMap<>(size);
        uNodeTo = new THashMap<>(size);

        vDistTo = new THashMap<>(size);
        vEdgeTo = new THashMap<>(size);
        vNodeTo = new THashMap<>(size);

        this.graph = graph;

//        timerStart();
//        for(Long vert : graph.getGraph().keySet()){
//            uDistTo.put(vert, Double.MAX_VALUE);
//        }
//        for(Long vert : graph.getGraph().keySet()){
//            vDistTo.put(vert, Double.MAX_VALUE);
//        }
//        timerEnd("Filling maps");

    }

    public ArrayList<Long> search(long startNode, long endNode){

        overlapNode = null;

        uDistTo.clear();
        vDistTo.clear();

        this.startNode = startNode;
        this.endNode = endNode;

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

        uPq.add(new DijkstraEntry(startNode, 0.0));
        vPq.add(new DijkstraEntry(endNode, 0.0));

        uRelaxed = new HashSet<>();
        vRelaxed = new HashSet<>();

        bestSeen = Double.MAX_VALUE;
        long bestPathNode = 0;

//        double minDist = haversineDistance(startNode, endNode, dictionary);
        double uFurthest, vFurthest = 0;

        double competitor;

        maxDist = 0;
        explored = 0;

        long startTime = System.nanoTime();
        OUTER: while(!(uPq.isEmpty()) && !(vPq.isEmpty())){ //check
            explored += 2;
            long v1 = uPq.poll().getNode();
            for (double[] e : graph.fwdAdj(v1)){
                relax(v1, e, true);
                if (vRelaxed.contains((long) e[0])) {
                    competitor = (uDistTo.get(v1) + e[1] + vDistTo.get((long) e[0]));
                    if (bestSeen > competitor) {
                        bestSeen = competitor;
                        bestPathNode = v1;
                    }
                }
                if (vRelaxed.contains(v1)) {
//                    System.out.println("truth");
                    if((uDistTo.get(v1) + vDistTo.get(v1)) < bestSeen){
//                        System.out.println("A");
                        overlapNode = v1;
                    } else {
//                        System.out.println("B");
                        overlapNode = bestPathNode;
                    }
//                    System.out.println("Explored: " + explored);
                    return getRouteAsWays();
                }
            }

            long v2 = vPq.poll().getNode();
            for (double[] e : graph.bckAdj(v2)) {
                relax(v2, e, false);
                if (uRelaxed.contains((long) e[0])) {
                    competitor = (vDistTo.get(v2) + e[1] + uDistTo.get((long) e[0]));
                    if (bestSeen > competitor) {
                        bestSeen = competitor;
                        bestPathNode = v2;
                    }
                }
                if (uRelaxed.contains(v2)) { //FINAL TERMINATION
//                    System.out.println("truth");
                    if((uDistTo.get(v2) + vDistTo.get(v2)) < bestSeen){
//                        System.out.println("A");
                        overlapNode = v2;
                    } else {
//                        System.out.println("B");
                        overlapNode = bestPathNode;
                    }
//                    System.out.println("Explored: " + explored);
                    return getRouteAsWays();
                }
            }
        }
        if(uPq.isEmpty() || vPq.isEmpty()){
            System.out.println("No route found.");
        }

        long endTime = System.nanoTime();
//        System.out.println("OVERLAP: " + overlapNode);
//        System.out.println("BiDijkstra time: " + (((float) endTime - (float)startTime) / 1000000000));

        return new ArrayList<>();

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
        return explored;
    }

}



