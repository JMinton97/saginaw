package project.search;


import gnu.trove.map.hash.THashMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongArrayMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import javafx.util.Pair;
import project.map.MyGraph;


import java.util.*;

public class Dijkstra implements Searcher {
    long pollTimeStart, pollTimeEnd, totalPollTime, addTimeStart, addTimeEnd, totalAddTime, relaxTimeStart, relaxTimeEnd, totalRelaxTime, putTimeStart, putTimeEnd, totalPutTime;
    THashMap<Long, Double> distTo;
    THashMap<Long, Long> edgeTo;

    public int src, dst;

    private MyGraph graph;

    Int2DoubleOpenHashMap distTo2;
    Int2LongOpenHashMap edgeTo2;
    Int2IntOpenHashMap nodeTo2;

    PriorityQueue<DijkstraEntry> pq;
    int startNode, endNode;
    public int explored;

    public Dijkstra(project.map.MyGraph graph, int startNode) {
        distTo = new THashMap<>();
        edgeTo = new THashMap<>();

        distTo2 = new Int2DoubleOpenHashMap();
        edgeTo2 = new Int2LongOpenHashMap();
        nodeTo2 = new Int2IntOpenHashMap();

        pq = new PriorityQueue();

        for (int vert : graph.getFwdGraph().keySet()) {
            distTo2.put(vert, Double.MAX_VALUE);
        }
        distTo2.put(startNode, 0.0);

        Comparator<DijkstraEntry> comparator = new DistanceComparator();
        pq = new PriorityQueue<DijkstraEntry>(comparator);

        pq.add(new DijkstraEntry(startNode, 0.0));

        while (!pq.isEmpty()) {
            int v = pq.poll().getNode();
            for (double[] e : graph.fwdAdj(v)) {
                relax(v, e);
            }
        }
    }

    public Dijkstra(project.map.MyGraph graph){
        this.graph = graph;
    }


    public ArrayList<Long> search(int src, int dst){

        explored = 0;

        this.dst = dst;
        this.src = src;

        distTo = new THashMap<>();
        edgeTo = new THashMap<>();

        distTo2 = new Int2DoubleOpenHashMap();
        edgeTo2 = new Int2LongOpenHashMap();
        nodeTo2 = new Int2IntOpenHashMap();

        this.startNode = src;
        this.endNode = dst;

        for(int vert : graph.getFwdGraph().keySet()){
            distTo2.put(vert, Double.MAX_VALUE);
        }
        distTo2.put(startNode, 0.0);

        Comparator<DijkstraEntry> comparator = new DistanceComparator();
        pq = new PriorityQueue<>(comparator);

        pq.add(new DijkstraEntry(startNode, 0.0));

        long startTime = System.nanoTime();
        OUTER: while(!pq.isEmpty()){
            explored++;
            int v = pq.poll().getNode();
            for (double[] e : graph.fwdAdj(v)){
                relax(v, e);
                if(v == endNode){
                    return getRouteAsWays();
                }
            }
        }
        System.out.println("No route found.");
        long endTime = System.nanoTime();
//        System.out.println("Dijkstra time: " + (((float) endTime - (float)startTime) / 1000000000));
        return new ArrayList<>();
    }

    private void relax(int v, double[] edge){
        int w = (int) edge[0];
        double weight = edge[1];
        double distToV = distTo2.get(v);
        if (distTo2.get(w) > (distToV + weight)){
            distTo2.put(w, distToV + weight);
            edgeTo2.put(w, (long) edge[2]); //should be 'nodeBefore'
            nodeTo2.put(w, v);
            pq.add(new DijkstraEntry(w, distToV + weight)); //inefficient?
        }
    }

    public Int2DoubleOpenHashMap getDistTo() {
        return distTo2;
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

//    public ArrayList<Integer> getRoute(){
//        ArrayList<Integer> route = new ArrayList<>();
//        int node = endNode;
//        route.add(node);
//        while(node != startNode){
//            node = edgeTo2.get(node);
//            route.add(node);
//        }
//        Collections.reverse(route);
//        return route;
//    }

    public ArrayList<Long> getRouteAsWays(){
        ArrayList<Long> route = new ArrayList<>();
        try{
//            System.out.println("GETROUTEASWAYS");
            long way = 0;
            int node = endNode;
            while(node != startNode){
//            System.out.println(node + ",");
                way = edgeTo2.get(node);
                node = nodeTo2.get(node);
//            System.out.println(way);
                route.add(way);
            }

        }catch(NullPointerException n){
//            System.out.println("Null: " + node);
//            System.out.println(n.getStackTrace());
        }
        return route;
    }


    public double getDist(){
        return distTo2.get(dst);
    }

    public void clear(){
        distTo2.clear();
        edgeTo2.clear();
        pq.clear();
    }

    public int getExplored(){
        return explored;
    }
}



