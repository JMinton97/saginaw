package project.search;


import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import project.map.Graph;


import java.util.*;

public class RemovalDijkstra implements Searcher {

    public int src, dst;

    private Graph graph;

    private Int2DoubleOpenHashMap distTo;
    private Int2LongOpenHashMap edgeTo;
    private Int2IntOpenHashMap nodeTo;
    private HashMap<Integer, Double> keyMap;

    private IndexMinPQ<Double> pq;
    public int explored;
    private ArrayList<Integer> relaxedNodes;

    private boolean routeFound;
    private String name = "dijkstra";

    public RemovalDijkstra(Graph graph) {

        distTo = new Int2DoubleOpenHashMap();
        edgeTo = new Int2LongOpenHashMap();
        nodeTo = new Int2IntOpenHashMap();

        this.graph = graph;

        Comparator<DijkstraEntry> comparator = new DistanceComparator();
        pq = new IndexMinPQ<>(graph.getFwdGraph().size());
        keyMap = new HashMap<>();

    }

    public void search(int src, int dst){

        explored = 0;

        this.dst = dst;
        this.src = src;

        relaxedNodes = new ArrayList<>();

        distTo.put(src, 0.0);
        pq = new IndexMinPQ<>(graph.getFwdGraph().size());
        pq.insert(src, 0.0);

        while(!pq.isEmpty()){
            explored++;
            int v = pq.delMin();
            relaxedNodes.add(v);
            if(v == dst){
                routeFound = true;
                return;
            }
            for (double[] e : graph.fwdAdj(v)){
                relax(v, e);
            }
        }

        routeFound = false;
        System.out.println("No route found here.");

    }

    private void relax(int v, double[] edge){
        int w = (int) edge[0];
        double weight = edge[1];
        double distToV = distTo.getOrDefault(v, Double.MAX_VALUE);
        if (distTo.getOrDefault(w, Double.MAX_VALUE) > (distToV + weight)){
            distTo.put(w, distToV + weight);
            edgeTo.put(w, (long) edge[2]); //should be 'nodeBefore'
            nodeTo.put(w, v);
            if(pq.contains(w)){
                pq.decreaseKey(w, distToV + weight);
            } else {
                pq.insert(w, distToV + weight);
            }
            keyMap.put(w, distToV + weight);
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

    public ArrayList<Integer> getRoute(){
        if(routeFound){
            ArrayList<Integer> route = new ArrayList<>();
            int node = dst;
            route.add(node);
            while(node != src){
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
                int node = dst;
                while(node != dst){
                    way = edgeTo.get(node);
                    node = nodeTo.get(node);
                    route.add(way);
                }

            }catch(NullPointerException n){
                System.out.println("ERROR");
            }
            return route;
        }else{
            return new ArrayList<>();
        }

    }


    public double getDist(){
        return distTo.get(dst);
    }

    public void clear(){
        distTo.clear();
        edgeTo.clear();
        nodeTo.clear();
        routeFound = false;
    }

    public int getExplored(){
        return explored;
    }

    public boolean routeFound(){
        return routeFound;
    }

    public ArrayList<ArrayList<Integer>> getRelaxedNodes() {
        ArrayList<ArrayList<Integer>> relaxedNodes = new ArrayList();
        relaxedNodes.add(this.relaxedNodes);
        return relaxedNodes;
    }

    public String getName(){
        return name;
    }

    @Override
    public ALTPreProcess getALT() {
        return null;
    }
}



