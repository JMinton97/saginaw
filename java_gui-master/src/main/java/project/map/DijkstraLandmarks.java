package project.map;


import gnu.trove.map.hash.THashMap;
import javafx.util.Pair;
import sun.misc.GC;

import java.util.*;

public class DijkstraLandmarks {
    long pollTimeStart, pollTimeEnd, totalPollTime, addTimeStart, addTimeEnd, totalAddTime, relaxTimeStart, relaxTimeEnd, totalRelaxTime, putTimeStart, putTimeEnd, totalPutTime;
    THashMap<Long, double[]> distTo;
    THashMap<Long, long[]> edgeTo;
    PriorityQueue<DijkstraEntry> pq;
    long startNode, endNode;
    public int explored;
    ArrayList<Long> landmarks;

    public DijkstraLandmarks(project.map.MyGraph graph, ArrayList<Long> startNodes){
        distTo = new THashMap<>();
        edgeTo = new THashMap<>();
        pq = new PriorityQueue();

        landmarks = startNodes;

        double[] initDistance = new double[startNodes.size()];
        for(int x = 0; x < initDistance.length; x++){
            initDistance[x] = Double.MAX_VALUE;
        }

        for(Long vert : graph.getGraph().keySet()){
            distTo.put(vert, initDistance.clone());
        }

        for(int x = 0; x < startNodes.size(); x++){
//            System.out.println(Arrays.toString(distTo.get(startNodes.get(x))));
            DijkstraAlgorithm(graph, startNodes.get(x), x);
        }

    }

    public void DijkstraAlgorithm(MyGraph graph, long startNode, int index){
//        System.out.println();
//        System.out.println("Start " + startNode + " Index " + index);

        double[] distToStart = distTo.get(startNode);
//        System.out.println(Arrays.toString(distToStart));
        distToStart[index] = 0.0;
//        System.out.println(Arrays.toString(distToStart));
        distTo.put(startNode, distToStart);

        Comparator<DijkstraEntry> comparator = new DistanceComparator();
        pq = new PriorityQueue<>(comparator);

        pq.add(new DijkstraEntry(startNode, 0.0));

//        System.out.println(pq.peek().getNode());

        relaxTimeStart = System.nanoTime();
        while(!pq.isEmpty()){
//            System.out.println("get");
            long v = pq.poll().getNode();
            for (double[] e : graph.adj(v)){
//                System.out.println("relax");
                relax(v, e, index);
            }
        }
        relaxTimeEnd = System.nanoTime();
//        System.out.println("Landmark time: " + (((float) relaxTimeEnd - (float)relaxTimeStart) / 1000000000));

        pq = null;
    }


    private void relax(Long v, double[] edge, int index){
        explored++;
        long w = (long) edge[0];
        double weight = edge[1];
        double distToV = distTo.get(v)[index];
        double[] distToW = distTo.get(w);
        if (distToW[index] > (distToV + weight)){
            putTimeStart = System.nanoTime();
            distToW[index] = distToV + weight;
            distTo.put(w, distToW);
            putTimeEnd = System.nanoTime();
            totalPutTime += (putTimeEnd - putTimeStart);
            addTimeStart = System.nanoTime();
            pq.add(new DijkstraEntry(w, distToV + weight)); //inefficient?
            addTimeEnd = System.nanoTime();
            totalAddTime += (addTimeEnd - addTimeStart);
        }
    }

    public THashMap<Long, double[]> getDistTo() {
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

    public double getDistance(long x, int index){
        return distTo.get(x)[index];
    }

    public void clear(){
        THashMap<Long, double[]> distTo = null;
        THashMap<Long, long[]> edgeTo = null;
        PriorityQueue<DijkstraEntry> pq = null;
        landmarks = null;
    }
}



