package project.search;


import gnu.trove.map.hash.THashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import javafx.util.Pair;
import project.map.MyGraph;


import java.util.*;

public class Dijkstra implements Searcher {
    long pollTimeStart, pollTimeEnd, totalPollTime, addTimeStart, addTimeEnd, totalAddTime, relaxTimeStart, relaxTimeEnd, totalRelaxTime, putTimeStart, putTimeEnd, totalPutTime;
    THashMap<Long, Double> distTo;
    THashMap<Long, Long> edgeTo;

    public long src, dst;

    private MyGraph graph;

    Long2DoubleOpenHashMap distTo2;
    Long2LongOpenHashMap edgeTo2;

    PriorityQueue<DijkstraEntry> pq;
    long startNode, endNode;
    public int explored;

    public Dijkstra(project.map.MyGraph graph, long startNode) {
        distTo = new THashMap<>();
        edgeTo = new THashMap<>();

        distTo2 = new Long2DoubleOpenHashMap();
        edgeTo2 = new Long2LongOpenHashMap();

        pq = new PriorityQueue();

        for (long vert : graph.getFwdGraph().keySet()) {
            distTo2.put(vert, Double.MAX_VALUE);
        }
        distTo2.put(startNode, 0.0);

        Comparator<DijkstraEntry> comparator = new DistanceComparator();
        pq = new PriorityQueue<DijkstraEntry>(comparator);

        pq.add(new DijkstraEntry(startNode, 0.0));

        while (!pq.isEmpty()) {
            long v = pq.poll().getNode();
            for (double[] e : graph.fwdAdj(v)) {
                relax(v, e);
            }
        }
    }

    public Dijkstra(project.map.MyGraph graph){
        this.graph = graph;
    }


    public ArrayList<Long> search(long src, long dst){

        explored = 0;

        this.dst = dst;
        this.src = src;

        distTo = new THashMap<>();
        edgeTo = new THashMap<>();

        distTo2 = new Long2DoubleOpenHashMap();
        edgeTo2 = new Long2LongOpenHashMap();

        this.startNode = src;
        this.endNode = dst;

        for(long vert : graph.getFwdGraph().keySet()){
            distTo2.put(vert, Double.MAX_VALUE);
        }
        distTo2.put(startNode, 0.0);

        Comparator<DijkstraEntry> comparator = new DistanceComparator();
        pq = new PriorityQueue<>(comparator);

        pq.add(new DijkstraEntry(startNode, 0.0));

        long startTime = System.nanoTime();
        OUTER: while(!pq.isEmpty()){
            explored++;
            long v = pq.poll().getNode();
            for (double[] e : graph.fwdAdj(v)){
                relax(v, e);
                if(v == endNode){
                    return getRoute();
                }
            }
        }
        System.out.println("No route found.");
        long endTime = System.nanoTime();
//        System.out.println("Dijkstra time: " + (((float) endTime - (float)startTime) / 1000000000));
        return new ArrayList<>();
    }

    private void relax(long v, double[] edge){
        long w = (long) edge[0];
        double weight = edge[1];
        double distToV = distTo2.get(v);
        if (distTo2.get(w) > (distToV + weight)){
            distTo2.put(w, distToV + weight);
            edgeTo2.put(w, v); //should be 'nodeBefore'
            pq.add(new DijkstraEntry(w, distToV + weight)); //inefficient?
        }
    }

    public Long2DoubleOpenHashMap getDistTo() {
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

    public ArrayList<Long> getRoute(){
        ArrayList<Long> route = new ArrayList<>();
        long node = endNode;
        route.add(node);
        while(node != startNode){
            node = edgeTo2.get(node);
            route.add(node);
        }
        Collections.reverse(route);
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


