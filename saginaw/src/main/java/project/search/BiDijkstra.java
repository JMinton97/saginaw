package project.search;

import gnu.trove.map.hash.THashMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import org.mapdb.BTreeMap;
import project.map.MyGraph;

import java.util.*;

public class BiDijkstra implements Searcher {
    private Int2DoubleOpenHashMap uDistTo;
    private Int2LongOpenHashMap uEdgeTo;
    private Int2IntOpenHashMap uNodeTo;
    private Int2DoubleOpenHashMap vDistTo;
    private Int2LongOpenHashMap vEdgeTo;
    private Int2IntOpenHashMap vNodeTo;
    private PriorityQueue<DijkstraEntry> uPq;
    private PriorityQueue<DijkstraEntry> vPq;
    private HashSet<Integer> uRelaxed;
    private HashSet<Integer> vRelaxed;
    private int overlapNode;
    private double bestSeen;
    private int explored;
    private long startNode, endNode;
    private MyGraph graph;
    private boolean routeFound;

    public BiDijkstra(MyGraph graph) {
        int size = graph.getFwdGraph().size();

        uDistTo = new Int2DoubleOpenHashMap();
        uEdgeTo = new Int2LongOpenHashMap();
        uNodeTo = new Int2IntOpenHashMap();

        vDistTo = new Int2DoubleOpenHashMap();
        vEdgeTo = new Int2LongOpenHashMap();
        vNodeTo = new Int2IntOpenHashMap();

        uRelaxed = new HashSet<>();
        vRelaxed = new HashSet<>();

        uPq = new PriorityQueue<>(new DistanceComparator());
        vPq = new PriorityQueue<>(new DistanceComparator());

        this.graph = graph;
    }

    public void search(int startNode, int endNode){

        overlapNode = -1;

        uDistTo.clear();
        vDistTo.clear();

        this.startNode = startNode;
        this.endNode = endNode;

        uDistTo.put(startNode, 0.0);
        vDistTo.put(endNode, 0.0);

        uPq.add(new DijkstraEntry(startNode, 0.0));
        vPq.add(new DijkstraEntry(endNode, 0.0));

        bestSeen = Double.MAX_VALUE;
        int bestPathNode = 0;

        double competitor;

        explored = 0;

        while(!(uPq.isEmpty()) && !(vPq.isEmpty())){ //check
//            System.out.println("searching");
            explored += 2;
            int v1 = uPq.poll().getNode();
            for (double[] e : graph.fwdAdj(v1)){
                relax(v1, e, true);
                if (vRelaxed.contains((int) e[0])) {
                    competitor = (uDistTo.get(v1) + e[1] + vDistTo.get((int) e[0]));
                    if (bestSeen > competitor) {
                        bestSeen = competitor;
                        bestPathNode = v1;
                    }
                }
                if (vRelaxed.contains(v1)) {
                    if((uDistTo.get(v1) + vDistTo.get(v1)) < bestSeen){
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
                    if((uDistTo.get(v2) + vDistTo.get(v2)) < bestSeen){
                        overlapNode = v2;
                    } else {
                        overlapNode = bestPathNode;
                    }
                    routeFound = true;
                    return;
                }
            }
        }
        if(uPq.isEmpty() || vPq.isEmpty()) {
            System.out.println("No route found.");
            routeFound = false;
        }
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
                uEdgeTo.put(w, (long) wayId);
                uPq.add(new DijkstraEntry(w, distToX + weight)); //inefficient?

            }
        } else {
            vRelaxed.add(x);
            double distToX = vDistTo.getOrDefault(x, Double.MAX_VALUE);
            if (vDistTo.getOrDefault(w, Double.MAX_VALUE) > (distToX + weight)){
                vDistTo.put(w, distToX + weight);
                vNodeTo.put(w, x); //should be 'nodeBefore'
                vEdgeTo.put(w, (long) wayId);
                vPq.add(new DijkstraEntry(w, distToX + weight)); //inefficient?
            }
        }

    }

    public double getDist() {
        System.out.println(overlapNode);
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
        if(routeFound) {
            ArrayList<Integer> route = new ArrayList<>();
            int node = overlapNode;
            route.add(overlapNode);
            while (node != startNode) {
                node = uNodeTo.get(node);
                route.add(node);
            }
            Collections.reverse(route);
            node = overlapNode;
            while (node != endNode) {
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
                while(node != startNode && node != endNode){
                    way = uEdgeTo.get(node);
                    node = uNodeTo.get(node);
                    route.add(way);
                }

                Collections.reverse(route);
                node = overlapNode;
                while(node != startNode && node != endNode){
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
        vDistTo.clear();
        vEdgeTo.clear();
        vPq.clear();
        uPq.clear();
        vRelaxed.clear();
        uRelaxed.clear();
        vNodeTo.clear();
        uNodeTo.clear();
        routeFound = false;

    }

    public int getExplored(){
        return explored;
    }

}



