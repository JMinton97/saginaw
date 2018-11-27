package project.map;


import javafx.util.Pair;

import java.util.*;

public class Dijkstra {
    HashMap<Long, Double> distTo;
    HashMap<Long, Long> edgeTo;
    PriorityQueue<Long> pq;

    public Dijkstra(project.map.MyGraph graph, Long startNode){
        distTo = new HashMap<>();
        edgeTo = new HashMap<>();
        pq = new PriorityQueue();

        for(Long vert : graph.getGraph().keySet()){
            distTo.put(vert, Double.MAX_VALUE);
        }
        distTo.put(startNode, 0.0);

        Comparator<Long> comparator = new DistanceComparator();
        pq = new PriorityQueue<>(comparator);

        pq.add(startNode);

        while(!pq.isEmpty()){
//            System.out.println("Dijkstra!");
            long v = pq.poll();
            for (double[] e : graph.adj(v)){
                relax(v, e);
            }
        }
    }

    public Dijkstra(project.map.MyGraph graph, Long startNode, Long endNode){
        distTo = new HashMap<>();
        edgeTo = new HashMap<>();
        pq = new PriorityQueue();

        for(Long vert : graph.getGraph().keySet()){
            distTo.put(vert, Double.MAX_VALUE);
        }
        distTo.put(startNode, 0.0);

        Comparator<Long> comparator = new DistanceComparator();
        pq = new PriorityQueue<>(comparator);

        pq.add(startNode);

        long startTime = System.nanoTime();
        while(!pq.isEmpty()){
//            System.out.println("Dijkstra!");
            long v = pq.poll();
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
        long w = (long) edge[0];
        double weight = edge[1];
        double distToV = distTo.get(v);
        if (distTo.get(w) > (distToV + weight)){
            distTo.put(w, distToV + weight);
            edgeTo.put(w, v); //should be 'nodeBefore'
            if(pq.contains(w)){
                pq.remove(w);
            }
            pq.add(w); //inefficient?
        }
    }

    public HashMap<Long, Double> getDistTo() {
        return distTo;
    }

    public HashMap<Long, Long> getEdgeTo() {
        return edgeTo;
    }

    public class DistanceComparator implements Comparator<Long>{
        public int compare(Long x, Long y){
            if(distTo.get(x) < distTo.get(y)){
                return -1;
            }
            if(distTo.get(x) > distTo.get(y)){
                return 1;
            }
            else return 0;
        }
    }
}



