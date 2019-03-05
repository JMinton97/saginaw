package project.search;


import gnu.trove.map.hash.THashMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import project.map.MyGraph;


import java.util.*;

public class Dijkstra implements Searcher {

    public int src, dst;

    private MyGraph graph;

    private Int2DoubleOpenHashMap distTo;
    private Int2LongOpenHashMap edgeTo;
    private Int2IntOpenHashMap nodeTo;

    private PriorityQueue<DijkstraEntry> pq;
    public int explored;

    public Dijkstra(project.map.MyGraph graph) {

        distTo = new Int2DoubleOpenHashMap();
        edgeTo = new Int2LongOpenHashMap();
        nodeTo = new Int2IntOpenHashMap();

        this.graph = graph;

        Comparator<DijkstraEntry> comparator = new DistanceComparator();
        pq = new PriorityQueue<DijkstraEntry>(comparator);

    }

    public void search(int src, int dst){

        explored = 0;

        this.dst = dst;
        this.src = src;

        distTo.put(src, 0.0);

        pq.add(new DijkstraEntry(src, 0.0));

        OUTER: while(!pq.isEmpty()){
            explored++;
            int v = pq.poll().getNode();
            for (double[] e : graph.fwdAdj(v)){
                relax(v, e);
                if(v == dst){
                    return;
                }
            }
        }

        System.out.println("No route found.");

    }

    private void relax(int v, double[] edge){
        int w = (int) edge[0];
        double weight = edge[1];
        double distToV = distTo.getOrDefault(v, Double.MAX_VALUE);
        if (distTo.getOrDefault(w, Double.MAX_VALUE) > (distToV + weight)){
            distTo.put(w, distToV + weight);
            edgeTo.put(w, (long) edge[2]); //should be 'nodeBefore'
            nodeTo.put(w, v);
            pq.add(new DijkstraEntry(w, distToV + weight)); //inefficient?
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
        ArrayList<Integer> route = new ArrayList<>();
        int node = dst;
        route.add(node);
        while(node != dst){
            node = nodeTo.get(node);
            route.add(node);
        }
        Collections.reverse(route);
        return route;
    }

    public ArrayList<Long> getRouteAsWays(){
        ArrayList<Long> route = new ArrayList<>();
        try{
            long way = 0;
            int node = dst;
            while(node != dst){
                way = edgeTo.get(node);
                node = nodeTo.get(node);
                route.add(way);
            }

        }catch(NullPointerException n){ }
        return route;
    }


    public double getDist(){
        return distTo.get(dst);
    }

    public void clear(){
        distTo.clear();
        edgeTo.clear();
        nodeTo.clear();
        pq.clear();
    }

    public int getExplored(){
        return explored;
    }
}



