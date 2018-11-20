package project.map;

import com.sun.tools.javac.util.Pair;

import java.util.*;

public class BiDijkstra {
    HashMap<Long, Double> uDistTo;
    HashMap<Long, Long> uEdgeTo;
    HashMap<Long, Double> vDistTo;
    HashMap<Long, Long> vEdgeTo;
    PriorityQueue<Long> uPq;
    PriorityQueue<Long> vPq;
    private HashSet<Long> uRelaxed;
    private HashSet<Long> vRelaxed;
    Long overlapNode;

    public BiDijkstra(MyGraph graph, Long startNode, Long endNode){
        uDistTo = new HashMap<>();
        uEdgeTo = new HashMap<>();

        vDistTo = new HashMap<>();
        vEdgeTo = new HashMap<>();


        for(Long vert : graph.getGraph().keySet()){
            uDistTo.put(vert, Double.MAX_VALUE);
        }
        uDistTo.put(startNode, 0.0);

        for(Long vert : graph.getGraph().keySet()){
            vDistTo.put(vert, Double.MAX_VALUE);
        }
        vDistTo.put(endNode, 0.0);

        uPq = new PriorityQueue<>(new DistanceComparatorU());
        vPq = new PriorityQueue<>(new DistanceComparatorV());

        uPq.add(startNode);
        vPq.add(endNode);

        uRelaxed = new HashSet<>();
        vRelaxed = new HashSet<>();

        long startTime = System.nanoTime();
        OUTER: while(!(uPq.isEmpty()) && !(vPq.isEmpty())){ //check
            long v1 = uPq.poll();
            for (Pair<Long, Double> e : graph.adj(v1)){
                if(vRelaxed.contains(v1)){
                    System.out.println("truth");
                    overlapNode = v1;
                    break OUTER;
                }
                relax(v1, e, true);
                if(v1 == endNode){
                    break;
                }
            }
            long v2 = vPq.poll();
            for (Pair<Long, Double> e : graph.adj(v2)){
                if(uRelaxed.contains(v2)){
                    System.out.println("truth");
                    overlapNode = v2;
                    break OUTER;
                }
                relax(v2, e, false);
                if(v2 == startNode){
                    break;
                }
            }
        }
        long endTime = System.nanoTime();
        System.out.println("BiDijkstra time: " + (((float) endTime - (float)startTime) / 1000000000));
    }

    private void relax(Long v, Pair<Long, Double> edge, boolean u){
        long w = edge.fst;
        double weight = edge.snd;
        if(u){
            uRelaxed.add(v);
            double distToV = uDistTo.get(v);
            if (uDistTo.get(w) > (distToV + weight)){
                uDistTo.put(w, distToV + weight);
                uEdgeTo.put(w, v); //should be 'nodeBefore'
                if(uPq.contains(w)){
                    uPq.remove(w);
                }
                uPq.add(w); //inefficient?
            }
        } else {
            vRelaxed.add(v);
            double distToV = vDistTo.get(v);
            if (vDistTo.get(w) > (distToV + weight)){
                vDistTo.put(w, distToV + weight);
                vEdgeTo.put(w, v); //should be 'nodeBefore'
                if(vPq.contains(w)){
                    vPq.remove(w);
                }
                vPq.add(w); //inefficient?
            }
        }

    }

    public Double getDist() {
        return uDistTo.get(overlapNode) + vDistTo.get(overlapNode);
    }
//
//    public HashMap<Long, Long> getEdgeTo() {
//        return edgeTo;
//    }

    public class DistanceComparatorU implements Comparator<Long>{
        public int compare(Long x, Long y){
            if(uDistTo.get(x) < uDistTo.get(y)){
                return -1;
            }
            if(uDistTo.get(x) > uDistTo.get(y)){
                return 1;
            }
            else return 0;
        }
    }
    public class DistanceComparatorV implements Comparator<Long>{
        public int compare(Long x, Long y){
            if(vDistTo.get(x) < vDistTo.get(y)){
                return -1;
            }
            if(vDistTo.get(x) > vDistTo.get(y)){
                return 1;
            }
            else return 0;
        }
    }
}



