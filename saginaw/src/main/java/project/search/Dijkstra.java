package project.search;


import gnu.trove.map.hash.THashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;


import java.util.*;

public class Dijkstra {
    long pollTimeStart, pollTimeEnd, totalPollTime, addTimeStart, addTimeEnd, totalAddTime, relaxTimeStart, relaxTimeEnd, totalRelaxTime, putTimeStart, putTimeEnd, totalPutTime;
    THashMap<Long, Double> distTo;
    THashMap<Long, Long> edgeTo;

    Long2DoubleOpenHashMap distTo2;
    Long2LongOpenHashMap edgeTo2;

    PriorityQueue<DijkstraEntry> pq;
    long startNode, endNode;
    public int explored;

    public Dijkstra(project.map.MyGraph graph, long startNode){
        distTo = new THashMap<>();
        edgeTo = new THashMap<>();

        distTo2 = new Long2DoubleOpenHashMap();
        edgeTo2 = new Long2LongOpenHashMap();

        pq = new PriorityQueue();

        for(long vert : graph.getFwdGraph().keySet()){
            distTo2.put(vert, Double.MAX_VALUE);
        }
        distTo2.put(startNode, 0.0);

        Comparator<DijkstraEntry> comparator = new DistanceComparator();
        pq = new PriorityQueue<DijkstraEntry>(comparator);

        pq.add(new DijkstraEntry(startNode, 0.0));

        while(!pq.isEmpty()){
            long v = pq.poll().getNode();
            for (double[] e : graph.fwdAdj(v)){
                relax(v, e);
            }
        }
    }

    public Dijkstra(project.map.MyGraph graph, long startNode, long endNode){
        distTo = new THashMap<>();
        edgeTo = new THashMap<>();

        distTo2 = new Long2DoubleOpenHashMap();
        edgeTo2 = new Long2LongOpenHashMap();

        pq = new PriorityQueue();

        this.startNode = startNode;
        this.endNode = endNode;

        for(long vert : graph.getFwdGraph().keySet()){
            distTo2.put(vert, Double.MAX_VALUE);
        }
        distTo2.put(startNode, 0.0);

        Comparator<DijkstraEntry> comparator = new DistanceComparator();
        pq = new PriorityQueue<>(comparator);

        pq.add(new DijkstraEntry(startNode, 0.0));

        long startTime = System.nanoTime();
        OUTER: while(!pq.isEmpty()){
            pollTimeStart = System.nanoTime();
            long v = pq.poll().getNode();
            pollTimeEnd = System.nanoTime();
            totalPollTime += (pollTimeEnd - pollTimeStart);
            for (double[] e : graph.fwdAdj(v)){
                relax(v, e);
                if(v == endNode){
                    System.out.println("Dijkstra terminate.");
                    break OUTER;
                }
            }
        }
        long endTime = System.nanoTime();
        System.out.println("Dijkstra time: " + (((float) endTime - (float)startTime) / 1000000000));
    }

    private void relax(long v, double[] edge){
        explored++;
        relaxTimeStart = System.nanoTime();
        long w = (long) edge[0];
        double weight = edge[1];
        double distToV = distTo2.get(v);
        if (distTo2.get(w) > (distToV + weight)){
            putTimeStart = System.nanoTime();
            distTo2.put(w, distToV + weight);
            edgeTo2.put(w, v); //should be 'nodeBefore'
            putTimeEnd = System.nanoTime();
            totalPutTime += (putTimeEnd - putTimeStart);
            addTimeStart = System.nanoTime();
            pq.add(new DijkstraEntry(w, distToV + weight)); //inefficient?
            addTimeEnd = System.nanoTime();
            totalAddTime += (addTimeEnd - addTimeStart);
        }
        relaxTimeEnd = System.nanoTime();
        totalRelaxTime += (relaxTimeEnd - relaxTimeStart);
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

    public double getDistance(long x){
        return distTo2.get(x);
    }

    public void clear(){
        distTo2.clear();
        edgeTo2.clear();
        pq.clear();
    }
}



