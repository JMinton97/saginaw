package project.search;


import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import project.map.MyGraph;
import java.util.*;

public class ALT implements Searcher {

    private Int2DoubleOpenHashMap distTo;
    private Int2LongOpenHashMap edgeTo;
    private Int2IntOpenHashMap nodeTo;
    private PriorityQueue<DijkstraEntry> pq;
    private int startNode, endNode;
    public int explored;
    private MyGraph graph;
    private ArrayList<Integer> landmarks;
    private Int2ObjectOpenHashMap distancesTo;
    private Int2ObjectOpenHashMap distancesFrom;
    private boolean routeFound;
    private double[] dTV, dFV;

    public ALT(MyGraph graph, ALTPreProcess altPreProcess){
        this.graph = graph;
        landmarks = new ArrayList<>();
        this.landmarks = altPreProcess.landmarks;
        this.distancesFrom = altPreProcess.distancesFrom;
        this.distancesTo = altPreProcess.distancesTo;

        distTo = new Int2DoubleOpenHashMap();
        edgeTo = new Int2LongOpenHashMap();
        nodeTo = new Int2IntOpenHashMap();

        pq = new PriorityQueue();

    }

    public void search(int src, int dst){

        dTV = (double[]) distancesTo.get(dst);
        dFV = (double[]) distancesFrom.get(dst);

        pq = new PriorityQueue();

        this.startNode = src;
        this.endNode = dst;

        distTo.put(src, 0.0);

        Comparator<DijkstraEntry> comparator = new DistanceComparator();
        pq = new PriorityQueue<>(comparator);

        pq.add(new DijkstraEntry(src, 0.0));

        explored = 0;

        OUTER: while(!pq.isEmpty()){
            System.out.println("search");
            explored++;
            int v = pq.poll().getNode();
            if(v == endNode){
                routeFound = true;
                return;
            }
            for (double[] e : graph.fwdAdj(v)){
                relax(v, e);
            }
        }

        System.out.println("No route found.");
        routeFound = false;
    }

    private void relax(int v, double[] edge){
        int w = (int) edge[0];
        System.out.println(w);
        double weight = edge[1];
        double distToV = distTo.getOrDefault(v, Double.MAX_VALUE);
        if (distTo.getOrDefault(w, Double.MAX_VALUE) > (distToV + weight)){
            System.out.println(distTo.getOrDefault(w, Double.MAX_VALUE) + " > " + (distToV + weight));
            distTo.put(w, distToV + weight);
            edgeTo.put(w, (long) edge[2]); //should be 'nodeBefore'
            nodeTo.put(w, v);
            pq.add(new DijkstraEntry(w, distToV + weight + lowerBound(w))); //inefficient?
        }
    }


    public double lowerBound(int u){
        double max = 0;
        double[] dTU, dFU;
        dTU = (double[]) distancesTo.get(u);
        dFU = (double[]) distancesFrom.get(u);

        for(int l = 0; l < landmarks.size(); l++){
            max = Math.max(max, Math.max(dTU[l] - dTV[l], dFV[l] - dFU[l]));
        }
        return max;
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

    public ArrayList<Integer> getRoute(){
        if(routeFound) {
            ArrayList<Integer> route = new ArrayList<>();
            int node = endNode;
            route.add(node);
            while (node != startNode) {
                node = nodeTo.get(node);
                route.add(node);
            }
            Collections.reverse(route);
            return route;
        }else{
            return new ArrayList<>();
        }
    }

    public ArrayList<Long> getRouteAsWays(){
        if(routeFound){
            ArrayList<Long> route = new ArrayList<>();
            try{
                long way = 0;
                int node = endNode;
                while(node != startNode){
                    way = edgeTo.get(node);
                    node = nodeTo.get(node);
                    route.add(way);
                }

            }catch(NullPointerException n){ }
            return route;
        } else {
            return new ArrayList<>();
        }
    }

    public void clear(){
        distTo.clear();
        edgeTo.clear();
        nodeTo.clear();
        pq.clear();
        routeFound = false;

    }

    public double getDist(){
        return distTo.get(endNode);
    }

    public int getExplored(){
        return explored;
    }

    public boolean routeFound(){
        return routeFound;
    }

}



