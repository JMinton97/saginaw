package project.search;

import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.mapdb.BTreeMap;
import project.map.MyGraph;

import java.util.*;

public class BiALT implements Searcher {
    private Int2DoubleOpenHashMap uDistTo;
    private Int2LongOpenHashMap uEdgeTo;
    private Int2IntOpenHashMap uNodeTo;
    private Int2DoubleOpenHashMap vDistTo;
    private Int2LongOpenHashMap vEdgeTo;
    private Int2IntOpenHashMap vNodeTo;

    private PriorityQueue<DijkstraEntry> uPq;
    private PriorityQueue<DijkstraEntry> vPq;
    private int start, end;
    private MyGraph graph;
    private ArrayList<Integer> landmarks;
    private Int2ObjectOpenHashMap distancesTo;
    private Int2ObjectOpenHashMap distancesFrom;
    private HashSet<Integer> uRelaxed;
    private HashSet<Integer> vRelaxed;
    private int overlapNode;
    private int explored;
    private double bestSeen;
    private int bestPathNode;
    private boolean routeFound;

    public BiALT(MyGraph graph, ALTPreProcess altPreProcess) {
        this.graph = graph;
        landmarks = new ArrayList<>();

        landmarks = altPreProcess.landmarks;
        distancesFrom = altPreProcess.distancesFrom;
        distancesTo = altPreProcess.distancesTo;

        int size = graph.getFwdGraph().size();

        uDistTo = new Int2DoubleOpenHashMap();
        uEdgeTo = new Int2LongOpenHashMap();
        uNodeTo = new Int2IntOpenHashMap();

        vDistTo = new Int2DoubleOpenHashMap();
        vEdgeTo = new Int2LongOpenHashMap();
        vNodeTo = new Int2IntOpenHashMap();

        Comparator<DijkstraEntry> comparator = new DistanceComparator();
        uPq = new PriorityQueue<DijkstraEntry>(comparator);
        vPq = new PriorityQueue<DijkstraEntry>(comparator);

        uRelaxed = new HashSet<>();
        vRelaxed = new HashSet<>();
    }

    public void search(int start, int end){

        explored = 0;

        overlapNode = -1;

        this.start = start;
        this.end = end;

        uDistTo.put(start, 0.0);
        vDistTo.put(end, 0.0);

        uPq.add(new DijkstraEntry(start, 0.0));
        vPq.add(new DijkstraEntry(end, 0.0));

        bestSeen = Double.MAX_VALUE;
        bestPathNode = 0;

        double competitor;

        explored = 0;

        OUTER: while(!(uPq.isEmpty()) && !(vPq.isEmpty())) { //check
//            System.out.println("loop");
            explored += 2;
            int v1 = uPq.poll().getNode();
            for (double[] e : graph.fwdAdj(v1)) {
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
                        overlapNode = v1;
                    } else {
                        overlapNode = bestPathNode;
                    }
                    routeFound = true;
                    return;
                }
            }
            int v2 = vPq.poll().getNode();
            for (double[] e : graph.bckAdj(v2)) {
                relax(v2, e, false);
                if (uRelaxed.contains((int) e[0])) {
                    competitor = (vDistTo.get(v2) + e[1] + uDistTo.get((int) e[0]));
                    if (bestSeen > competitor) {
                        bestSeen = competitor;
                        bestPathNode = v2;
                    }
                }
                if (uRelaxed.contains(v2)) { //FINAL TERMINATION
                    if ((uDistTo.get(v2) + vDistTo.get(v2)) < bestSeen) {
                        overlapNode = v2;
                    } else {
                        overlapNode = bestPathNode;
                    }
                    routeFound = true;
                    return;
                }
            }
        }

        System.out.println("No route found.");
        routeFound = false;
    }

    private void relax(int x, double[] edge, boolean u){
        int w = (int) edge[0];
        double weight = edge[1];
        double wayId = edge[2];
        if(u){
            uRelaxed.add(x);
            double distToX = uDistTo.getOrDefault(x, Double.MAX_VALUE);
            if (uDistTo.getOrDefault(w, Double.MAX_VALUE) > (distToX + weight)){
                uDistTo.put(w, distToX + weight);
                uNodeTo.put(w, x); //should be 'nodeBefore'
                uEdgeTo.put(w, (long) wayId); //should be 'nodeBefore'
                uPq.add(new DijkstraEntry(w, distToX + weight + lowerBound(w, true))); //inefficient?
            }
        } else {
            vRelaxed.add(x);
            double distToX = vDistTo.getOrDefault(x, Double.MAX_VALUE);
            if (vDistTo.getOrDefault(w, Double.MAX_VALUE) > (distToX + weight)){
                vDistTo.put(w, distToX + weight);
                vNodeTo.put(w, x); //should be 'nodeBefore'
                vEdgeTo.put(w, (long) wayId); //should be 'nodeBefore'
                vPq.add(new DijkstraEntry(w, distToX + weight + lowerBound(w, false))); //inefficient?
            }
        }

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

    public double lowerBound(int u, boolean forwards){
        double maxForward = 0;
        double maxBackward = 0;

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
            ArrayList<Integer> route = new ArrayList<>();
            int node = overlapNode;
            route.add(overlapNode);
            while(node != start){
                node = uNodeTo.get(node);
                route.add(node);
            }
            Collections.reverse(route);
            node = overlapNode;
            while(node != end){
                node = vNodeTo.get(node);
                route.add(node);
            }
            return route;
        }else{
            return new ArrayList<>();
        }
    }

    public ArrayList<Long> getRouteAsWays(){
        if(routeFound){
            int node = overlapNode;
            ArrayList<Long> route = new ArrayList<>();
            try{
                long way = 0;
                while(node != start && node != end){
                    way = uEdgeTo.get(node);
                    node = uNodeTo.get(node);
                    route.add(way);
                }

                Collections.reverse(route);
                node = overlapNode;
                while(node != start && node != end){
                    way = vEdgeTo.get(node);
                    node = vNodeTo.get(node);
                    route.add(way);
                }

            }catch(NullPointerException n){
            }
            return route;
        }else{
            return new ArrayList<>();
        }
    }

    public void clear(){
        uDistTo.clear();
        uEdgeTo.clear();
        uNodeTo.clear();
        vNodeTo.clear();
        vDistTo.clear();
        vEdgeTo.clear();
        vPq.clear();
        uPq.clear();
        vRelaxed.clear();
        uRelaxed.clear();
        routeFound = false;

    }

    public int getExplored(){
        return explored;
    }

    public boolean routeFound(){
        return routeFound;
    }
}