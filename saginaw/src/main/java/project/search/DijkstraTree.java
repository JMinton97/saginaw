package project.search;


import gnu.trove.map.hash.THashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import project.map.MyGraph;


import java.util.*;

public class DijkstraTree implements project.search.Searcher {
    long pollTimeStart, pollTimeEnd, totalPollTime, addTimeStart, addTimeEnd, totalAddTime, relaxTimeStart, relaxTimeEnd, totalRelaxTime, putTimeStart, putTimeEnd, totalPutTime;

    public long src, dst;

    public double epsilon, twoEpsilon;

    private MyGraph graph;

    Long2DoubleOpenHashMap distTo;
    Long2LongOpenHashMap edgeTo;

    PriorityQueue<project.search.DijkstraEntry> pq;
    long startNode, endNode;
    public int explored;

    public DijkstraTree(project.map.MyGraph graph, double epsilon){
        this.graph = graph;
        this.epsilon = epsilon;
        this.twoEpsilon = epsilon * 2;
    }


    public double getReach(long src){

        explored = 0;

        this.src = src;

        distTo = new Long2DoubleOpenHashMap();
        edgeTo = new Long2LongOpenHashMap();

        pq = new PriorityQueue();

        this.startNode = src;

        for(long vert : graph.getFwdGraph().keySet()){
            distTo.put(vert, Double.MAX_VALUE);
        }
        distTo.put(startNode, 0.0);

        Comparator<project.search.DijkstraEntry> comparator = new DistanceComparator();
        pq = new PriorityQueue<>(comparator);

        pq.add(new project.search.DijkstraEntry(startNode, 0.0));

        long startTime = System.nanoTime();
        OUTER: while(!pq.isEmpty()){
            explored++;
            long v = pq.poll().getNode();
            for (double[] e : graph.fwdAdj(v)){
                relax(v, e);
            }
        }
        long endTime = System.nanoTime();
        System.out.println("Tree time: " + (((float) endTime - (float)startTime) / 1000000000));
        System.out.println("Reach: " + epsilon);
        return epsilon;
    }


    private void relax(long v, double[] edge){
        long w = (long) edge[0];
        double weight = edge[1];
        double distToV = distTo.get(v);
        if (distTo.get(w) > (distToV + weight)){
            distTo.put(w, distToV + weight);
            edgeTo.put(w, v); //should be 'nodeBefore'
            if((distToV + weight) <= (twoEpsilon)){
                pq.add(new project.search.DijkstraEntry(w, distToV + weight)); //inefficient?
            }
        }
    }

    public Long2DoubleOpenHashMap getDistTo() {
        return distTo;
    }

    public class DistanceComparator implements Comparator<project.search.DijkstraEntry>{
        public int compare(project.search.DijkstraEntry x, project.search.DijkstraEntry y){
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


    public double getDist(){
        return distTo.get(dst);
    }

    public void clear(){
        distTo.clear();
        edgeTo.clear();
        pq.clear();
    }

    public int getExplored(){
        return explored;
    }
}



