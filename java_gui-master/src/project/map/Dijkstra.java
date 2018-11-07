package project.map;

import com.sun.tools.javac.util.Pair;

import java.util.*;

public class Dijkstra {
    HashMap<Long, Double> distTo;
    HashMap<Long, Long> edgeTo;
    PriorityQueue<Long> pq;

    public Dijkstra(MyGraph graph, Long startNode){
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
            System.out.println("Dijkstra!");
            long v = pq.poll();
            for (Pair<Long, Double> e : graph.adj(v)){
                relax(v, e);
            }
        }
    }

    private void relax(Long v, Pair<Long, Double> edge){
        long w = edge.fst;
        double weight = edge.snd;
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



