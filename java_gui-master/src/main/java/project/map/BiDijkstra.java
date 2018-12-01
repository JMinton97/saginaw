package project.map;

import javafx.util.Pair;
import org.mapdb.BTreeMap;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class BiDijkstra {
    long startTime, endTime, relaxTimeStart, relaxTimeEnd, totalRelaxTime, arelaxTimeStart, arelaxTimeEnd, atotalRelaxTime, containsTimeStart, containsTimeEnd, totalContainsTime, pollTimeStart, pollTimeEnd, totalPollTime, relaxPutTimeStart, relaxPutTimeEnd, totalRelaxPutTime;
    HashMap<Long, Double> uDistTo;
    HashMap<Long, Long> uEdgeTo;
    HashMap<Long, Double> vDistTo;
    HashMap<Long, Long> vEdgeTo;
    PriorityQueue<DijkstraEntry> uPq;
    PriorityQueue<DijkstraEntry> vPq;
    private HashSet<Long> uRelaxed;
    private HashSet<Long> vRelaxed;
    public Long overlapNode;
    private double maxDist; //how far from the nodes we have explored - have we covered minimum distance yet?
    public double bestSeen;
    public int explored;
    private long startNode, endNode;

    public BiDijkstra(MyGraph graph, Long startNode, Long endNode, BTreeMap<Long, double[]> dictionary){
        System.out.println("SIZE " + graph.getGraph().size());
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

        uPq = new PriorityQueue<>(new DistanceComparator());
        vPq = new PriorityQueue<>(new DistanceComparator());

        uPq.add(new DijkstraEntry(startNode, 0.0));
        vPq.add(new DijkstraEntry(endNode, 0.0));

        uRelaxed = new HashSet<>();
        vRelaxed = new HashSet<>();

        bestSeen = Double.MAX_VALUE;
        long bestPathNode = 0;

        double minDist = haversineDistance(startNode, endNode, dictionary);
        double uFurthest, vFurthest = 0;

        double competitor;

        maxDist = 0;

        long startTime = System.nanoTime();
        OUTER: while(!(uPq.isEmpty()) && !(vPq.isEmpty())){ //check
            pollTimeStart = System.nanoTime();
            long v1 = uPq.poll().getNode();
            pollTimeEnd = System.nanoTime();
            totalPollTime += (pollTimeEnd - pollTimeStart);
//            uFurthest = uDistTo.get(v1);
//            System.out.println(v1);
            for (double[] e : graph.adj(v1)){
                relax(v1, e, true);
//                System.out.println("...   ");
//                if(uFurthest + vFurthest >= minDist){
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
//                }
            }
            pollTimeStart = System.nanoTime();
            long v2 = vPq.poll().getNode();
            pollTimeEnd = System.nanoTime();
            totalPollTime += (pollTimeEnd - pollTimeStart);
//            vFurthest = vDistTo.get(v2);
            for (double[] e : graph.adj(v2)) {
                relax(v2, e, false);
//                System.out.println("...   ");
//                if(uFurthest + vFurthest >= minDist){
//                    System.out.println("   ...");
                    containsTimeStart = System.nanoTime();
                    if (uRelaxed.contains((long) e[0])) {

                        competitor = (vDistTo.get(v2) + e[1] + uDistTo.get((long) e[0]));
                        if (bestSeen > competitor) {
                            bestSeen = competitor;
                            bestPathNode = v2;
                        }
                    }
                    containsTimeEnd = System.nanoTime();
                    totalContainsTime += (containsTimeEnd - containsTimeStart);
                    containsTimeStart = System.nanoTime();
                    if (uRelaxed.contains(v2)) { //FINAL TERMINATION
                        System.out.println("truth");
                        if((uDistTo.get(v2) + vDistTo.get(v2)) < bestSeen){
                            overlapNode = v2;
                        } else {
                            overlapNode = bestPathNode;
                        }
                        break OUTER;
                    }
                    containsTimeEnd = System.nanoTime();
                    totalContainsTime += (containsTimeEnd - containsTimeStart);
//                    if (v2 == startNode) {
//                        break;
//                    }
//                }
            }
        }
        long endTime = System.nanoTime();
        System.out.println("BiDijkstra time: " + (((float) endTime - (float)startTime) / 1000000000));
    }

    private void relax(Long x, double[] edge, boolean u){
        relaxTimeStart = System.nanoTime();
        explored++;
        long w = (long) edge[0];
        double weight = edge[1];
        if(u){
            uRelaxed.add(x);
            double distToX = uDistTo.getOrDefault(x, Double.MAX_VALUE);
            if (uDistTo.getOrDefault(w, Double.MAX_VALUE) > (distToX + weight)){
                relaxPutTimeStart = System.nanoTime();
                uDistTo.put(w, distToX + weight);
                uEdgeTo.put(w, x); //should be 'nodeBefore'
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
                vEdgeTo.put(w, x); //should be 'nodeBefore'
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

    public Double getDist() {
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



