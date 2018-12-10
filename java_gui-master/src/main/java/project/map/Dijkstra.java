package project.map;


import gnu.trove.map.hash.THashMap;
import javafx.util.Pair;

import java.util.*;

public class Dijkstra {
    long pollTimeStart, pollTimeEnd, totalPollTime, addTimeStart, addTimeEnd, totalAddTime, relaxTimeStart, relaxTimeEnd, totalRelaxTime, putTimeStart, putTimeEnd, totalPutTime;
    THashMap<Long, Double> distTo;
    THashMap<Long, Long> edgeTo;
    PriorityQueue<DijkstraEntry> pq;
    long startNode, endNode;
    public int explored;

    public Dijkstra(project.map.MyGraph graph, Long startNode){
        distTo = new THashMap<>();
        edgeTo = new THashMap<>();
        pq = new PriorityQueue();

        for(Long vert : graph.getGraph().keySet()){
            distTo.put(vert, Double.MAX_VALUE);
        }
        distTo.put(startNode, 0.0);

        Comparator<DijkstraEntry> comparator = new DistanceComparator();
        pq = new PriorityQueue<DijkstraEntry>(comparator);

        pq.add(new DijkstraEntry(startNode, 0.0));

        while(!pq.isEmpty()){
            long v = pq.poll().getNode();
            for (double[] e : graph.adj(v)){
                relax(v, e);
            }
        }
    }

    public Dijkstra(project.map.MyGraph graph, Long startNode, Long endNode){
        distTo = new THashMap<>();
        edgeTo = new THashMap<>();
        pq = new PriorityQueue();

        this.startNode = startNode;
        this.endNode = endNode;

        for(Long vert : graph.getGraph().keySet()){
            distTo.put(vert, Double.MAX_VALUE);
        }
        distTo.put(startNode, 0.0);

        Comparator<DijkstraEntry> comparator = new DistanceComparator();
        pq = new PriorityQueue<>(comparator);

        pq.add(new DijkstraEntry(startNode, 0.0));

        long startTime = System.nanoTime();
        while(!pq.isEmpty()){
            pollTimeStart = System.nanoTime();
            long v = pq.poll().getNode();
            pollTimeEnd = System.nanoTime();
            totalPollTime += (pollTimeEnd - pollTimeStart);
            for (double[] e : graph.adj(v)){
                relax(v, e);
                if(v == endNode){
                    break;
                }
            }
        }
        long endTime = System.nanoTime();
        System.out.println("Dijkstra time: " + (((float) endTime - (float)startTime) / 1000000000));
    }

    private void relax(Long v, double[] edge){
        explored++;
        relaxTimeStart = System.nanoTime();
        long w = (long) edge[0];
        double weight = edge[1];
        double distToV = distTo.get(v);
        if (distTo.get(w) > (distToV + weight)){
            putTimeStart = System.nanoTime();
            distTo.put(w, distToV + weight);
            edgeTo.put(w, v); //should be 'nodeBefore'
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

    public THashMap<Long, Double> getDistTo() {
        return distTo;
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
            node = edgeTo.get(node);
            route.add(node);
        }
        Collections.reverse(route);
        return route;
    }

    public double getDistance(long x){
        return distTo.get(x);
    }

    public void clear(){
        distTo.clear();
        edgeTo.clear();
        pq.clear();
    }
}



