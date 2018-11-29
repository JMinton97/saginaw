package project.map;

import javafx.util.Pair;
import org.mapdb.BTreeMap;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class BiDijkstra {
    long startTime, endTime, relaxTimeStart, relaxTimeEnd, totalRelaxTime, arelaxTimeStart, arelaxTimeEnd, atotalRelaxTime, containsTimeStart, containsTimeEnd, totalContainsTime;
    HashMap<Long, Double> uDistTo;
    HashMap<Long, Long> uEdgeTo;
    HashMap<Long, Double> vDistTo;
    HashMap<Long, Long> vEdgeTo;
    PriorityQueue<Long> uPq;
    PriorityQueue<Long> vPq;
    private HashSet<Long> uRelaxed;
    private HashSet<Long> vRelaxed;
    Long overlapNode;
    private double maxDist; //how far from the nodes we have explored - have we covered minimum distance yet?
    public double bestSeen;
    public int explored;
    private long startNode, endNode;

    public BiDijkstra(MyGraph graph, Long startNode, Long endNode, BTreeMap<Long, double[]> dictionary){
        uDistTo = new HashMap<>(graph.getGraph().size());
        uEdgeTo = new HashMap<>(graph.getGraph().size());

        vDistTo = new HashMap<>(graph.getGraph().size());
        vEdgeTo = new HashMap<>(graph.getGraph().size());

//        timerStart();
//        for(Long vert : graph.getGraph().keySet()){
//            uDistTo.put(vert, Double.MAX_VALUE);
//        }
//        for(Long vert : graph.getGraph().keySet()){
//            vDistTo.put(vert, Double.MAX_VALUE);
//        }
//        timerEnd("Filling maps");

        this.startNode = startNode;
        this.endNode = endNode;

        uDistTo.put(startNode, 0.0);
        vDistTo.put(endNode, 0.0);

        uPq = new PriorityQueue<>(new DistanceComparatorU());
        vPq = new PriorityQueue<>(new DistanceComparatorV());

        uPq.add(startNode);
        vPq.add(endNode);

        uRelaxed = new HashSet<>();
        vRelaxed = new HashSet<>();

        bestSeen = Double.MAX_VALUE;
        long bestPathNode = 0;

        double minDist = haversineDistance(startNode, endNode, dictionary);
        double uFurthest, vFurthest = 0;

        double competitor;

        maxDist = 0;

        totalRelaxTime = 0;

        long startTime = System.nanoTime();
        OUTER: while(!(uPq.isEmpty()) && !(vPq.isEmpty())){ //check
            containsTimeStart = System.nanoTime();
            long v1 = uPq.poll();
            containsTimeEnd = System.nanoTime();
            totalContainsTime += (containsTimeEnd - containsTimeStart);
            uFurthest = uDistTo.get(v1);
//            System.out.println(v1);
            for (double[] e : graph.adj(v1)){
                relax(v1, e, true);
//                System.out.println("...   ");
                if(uFurthest + vFurthest >= minDist){
//                    System.out.println("   ...");
                    if (vRelaxed.contains((long) e[0])) {
                        competitor = (uDistTo.get(v1) + e[1] + vDistTo.get((long) e[0]));
                        if (bestSeen > competitor) {
                            bestSeen = competitor;
                            bestPathNode = v1;
                        }
                    }
                    if (vRelaxed.contains(v1)) {
                        System.out.println("truth");
                        if((uDistTo.get(v1) + vDistTo.get(v1)) < bestSeen){
                            overlapNode = v1;
                        } else {
                            overlapNode = bestPathNode;
                        }
                        break OUTER;
                    }
//                    if (v1 == endNode) {
//                        break;
//                    }
                }
            }
            containsTimeStart = System.nanoTime();
            long v2 = vPq.poll();
            containsTimeEnd = System.nanoTime();
            totalContainsTime += (containsTimeEnd - containsTimeStart);
            vFurthest = vDistTo.get(v2);
            for (double[] e : graph.adj(v2)) {
                relax(v2, e, false);
//                System.out.println("...   ");
                if(uFurthest + vFurthest >= minDist){
//                    System.out.println("   ...");
                    if (uRelaxed.contains((long) e[0])) {
                        competitor = (vDistTo.get(v2) + e[1] + uDistTo.get((long) e[0]));
                        if (bestSeen > competitor) {
                            bestSeen = competitor;
                            bestPathNode = v2;
                        }
                    }
                    if (uRelaxed.contains(v2)) { //FINAL TERMINATION
                        System.out.println("truth");
                        if((uDistTo.get(v2) + vDistTo.get(v2)) < bestSeen){
                            overlapNode = v2;
                        } else {
                            overlapNode = bestPathNode;
                        }
                        break OUTER;
                    }
//                    if (v2 == startNode) {
//                        break;
//                    }
                }
            }
        }
        long endTime = System.nanoTime();
        System.out.println("BiDijkstra time: " + (((float) endTime - (float)startTime) / 1000000000));
    }

    private void relax(Long x, double[] edge, boolean u){
        arelaxTimeStart = System.nanoTime();
        explored++;
        long w = (long) edge[0];
        double weight = edge[1];
        if(u){
            uRelaxed.add(x);
            double distToX = uDistTo.getOrDefault(x, Double.MAX_VALUE);
            if (uDistTo.getOrDefault(w, Double.MAX_VALUE) > (distToX + weight)){
                uDistTo.put(w, distToX + weight);
                uEdgeTo.put(w, x); //should be 'nodeBefore'
                uPq.add(w); //inefficient?
            }
        } else {
            vRelaxed.add(x);
            double distToX = vDistTo.getOrDefault(x, Double.MAX_VALUE);
            if (vDistTo.getOrDefault(w, Double.MAX_VALUE) > (distToX + weight)){
                vDistTo.put(w, distToX + weight);
                vEdgeTo.put(w, x); //should be 'nodeBefore'
                vPq.add(w); //inefficient?
            }
        }
        arelaxTimeEnd = System.nanoTime();
        atotalRelaxTime += (arelaxTimeEnd - arelaxTimeStart);
    }

    public Double getDist() {
        return uDistTo.get(overlapNode) + vDistTo.get(overlapNode);
    }

    public class DistanceComparatorU implements Comparator<Long>{
        public int compare(Long x, Long y){
            if(uDistTo.getOrDefault(x, Double.MAX_VALUE) < uDistTo.getOrDefault(y, Double.MAX_VALUE)){
                return -1;
            }
            if(uDistTo.getOrDefault(x, Double.MAX_VALUE) > uDistTo.getOrDefault(y, Double.MAX_VALUE)){
                return 1;
            }
            else return 0;
        }
    }
    public class DistanceComparatorV implements Comparator<Long>{
        public int compare(Long x, Long y){
            if(vDistTo.getOrDefault(x, Double.MAX_VALUE) < vDistTo.getOrDefault(y, Double.MAX_VALUE)){
                return -1;
            }
            if(vDistTo.getOrDefault(x, Double.MAX_VALUE) > vDistTo.getOrDefault(y, Double.MAX_VALUE)){
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
        while(node != startNode){
            node = uEdgeTo.get(node);
            route.add(node);
        }
        Collections.reverse(route);
        node = overlapNode;
        while(node != endNode){
            node = vEdgeTo.get(node);
            route.add(node);
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

}



