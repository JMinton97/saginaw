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

        uDistTo.put(startNode, 0.0);
        vDistTo.put(endNode, 0.0);

        uPq = new PriorityQueue<>(new DistanceComparatorU());
        vPq = new PriorityQueue<>(new DistanceComparatorV());

        uPq.add(startNode);
        vPq.add(endNode);

        uRelaxed = new HashSet<>();
        vRelaxed = new HashSet<>();

        bestSeen = Double.MAX_VALUE;
        ArrayList<Long> bestPath = new ArrayList<>();

        double minDist = haversineDistance(startNode, endNode, dictionary);

        double competitor;

        maxDist = 0;

        totalRelaxTime = 0;

        long startTime = System.nanoTime();
        OUTER: while(!(uPq.isEmpty()) && !(vPq.isEmpty())){ //check
            long v1 = uPq.poll();
            for (double[] e : graph.adj(v1)){
                relax(v1, e, true);
                    if (vRelaxed.contains((long) e[0])) {
                        competitor = (uDistTo.get(v1) + e[1] + vDistTo.get((long) e[0]));
                        if (bestSeen > competitor) {
                            bestSeen = competitor;
                        }
                    }
                    if (vRelaxed.contains(v1)) {
                        System.out.println("truth");
                        overlapNode = v1;
                        break OUTER;
                    }
//                    if (v1 == endNode) {
//                        break;
//                    }
            }
            long v2 = vPq.poll();
            for (double[] e : graph.adj(v2)) {
                relax(v2, e, false);
                    if (uRelaxed.contains((long) e[0])) {
                        competitor = (vDistTo.get(v2) + e[1] + uDistTo.get((long) e[0]));
                        if (bestSeen > competitor) {
                            bestSeen = competitor;
                        }
                    }
                    if (uRelaxed.contains(v2)) {
                        System.out.println("truth");
                        overlapNode = v2;
                        break OUTER;
                    }
//                    if (v2 == startNode) {
//                        break;
//                    }
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

    private void timerStart(){
        startTime = System.nanoTime();
    }

    private void timerEnd(String string){
        endTime = System.nanoTime();
        System.out.println(string + " time: " + (((float) endTime - (float)startTime) / 1000000000));
    }

}



