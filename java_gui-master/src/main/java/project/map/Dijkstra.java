package project.map;


import javafx.util.Pair;

import java.util.*;

public class Dijkstra {
    long pollTimeStart, pollTimeEnd, totalPollTime, addTimeStart, addTimeEnd, totalAddTime, relaxTimeStart, relaxTimeEnd, totalRelaxTime, putTimeStart, putTimeEnd, totalPutTime;
    HashMap<Long, Double> distTo;
    HashMap<Long, Long> edgeTo;
    PriorityQueue<DijkstraEntry> pq;
    long startNode, endNode;
    public int explored;

    public Dijkstra(project.map.MyGraph graph, Long startNode){
        distTo = new HashMap<>();
        edgeTo = new HashMap<>();
        pq = new PriorityQueue();

        for(Long vert : graph.getGraph().keySet()){
            distTo.put(vert, Double.MAX_VALUE);
        }
        distTo.put(startNode, 0.0);

        Comparator<DijkstraEntry> comparator = new DistanceComparator();
        pq = new PriorityQueue<DijkstraEntry>(comparator);

        pq.add(new DijkstraEntry(startNode, 0.0));

        while(!pq.isEmpty()){
//            System.out.println("Dijkstra!");
            long v = pq.poll().getNode();
            for (double[] e : graph.adj(v)){
                relax(v, e);
            }
        }
    }

    public Dijkstra(project.map.MyGraph graph, Long startNode, Long endNode){
        distTo = new HashMap<>();
        edgeTo = new HashMap<>();
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
//            System.out.println("Dijkstra!");
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
            pq.add(new DijkstraEntry(w, distTo.get(w))); //inefficient?
            addTimeEnd = System.nanoTime();
            totalAddTime += (addTimeEnd - addTimeStart);
        }
        relaxTimeEnd = System.nanoTime();
        totalRelaxTime += (relaxTimeEnd - relaxTimeStart);
    }

    public HashMap<Long, Double> getDistTo() {
        return distTo;
    }

    public HashMap<Long, Long> getEdgeTo() {
        return edgeTo;
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
//        System.out.println("adding " + node);
        route.add(node);
        while(node != startNode){
            node = edgeTo.get(node);
            route.add(node);
        }
        Collections.reverse(route);
//        System.out.println("first " + route.get(0));
//        System.out.println("last " + route.get(route.size() - 1));
        return route;
    }
}



