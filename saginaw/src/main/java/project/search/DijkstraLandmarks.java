package project.search;


import gnu.trove.map.hash.THashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import project.map.Graph;

import java.text.SimpleDateFormat;
import java.util.*;

public class DijkstraLandmarks {
    long pollTimeStart, pollTimeEnd, totalPollTime, addTimeStart, addTimeEnd, totalAddTime, relaxTimeStart, relaxTimeEnd, totalRelaxTime, putTimeStart, putTimeEnd, totalPutTime;
    Int2ObjectOpenHashMap distTo;

    PriorityQueue<DijkstraEntry> pq;
    public int explored;
    ArrayList<Integer> landmarks;

    public DijkstraLandmarks(Graph graph, ArrayList<Integer> startNodes, boolean forwards, boolean core){

        distTo = new Int2ObjectOpenHashMap<double[]>();
        pq = new PriorityQueue();

        landmarks = startNodes;

        double[] initDistance = new double[startNodes.size()];
        for(int x = 0; x < initDistance.length; x++){
            initDistance[x] = Double.MAX_VALUE;
        }

        if(core){
            for(int vert : graph.getFwdCore().keySet()){
                distTo.put(vert, initDistance.clone());
            }

            for(int vert : graph.getBckCore().keySet()){
                if(vert == Long.parseLong("694020801")){
                    System.out.println("here");
                }
                distTo.put(vert, initDistance.clone());
            }
        }else{
            for(int vert  = 0; vert < graph.getFwdGraph().size(); vert++){
                if(vert == Long.parseLong("694020801")){
                    System.out.println("here");
                }
                distTo.put(vert, initDistance.clone());
            }

            for(int vert  = 0; vert < graph.getBckGraph().size(); vert++){
                if(vert == Long.parseLong("694020801")){
                    System.out.println("here");
                }
                distTo.put(vert, initDistance.clone());
            }
        }


        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        for(int x = 0; x < startNodes.size(); x++){
            Calendar cal = Calendar.getInstance();
            System.out.println(x + 1 + " of " + startNodes.size() + " " + sdf.format(cal.getTime()));
            DijkstraAlgorithm(graph, startNodes.get(x), x, forwards, core);

        }

        for(int node : distTo.keySet()){
            double[] distances = (double[]) distTo.get(node);
            for(int x = 0; x < distances.length; x++){
                if(distances[x] == Double.MAX_VALUE){
                    distances[x] = 0;
                }
            }
            distTo.put(node, distances);
        }
    }

    public void DijkstraAlgorithm(Graph graph, int startNode, int index, boolean forwards, boolean core){

        double[] distToStart = (double[]) distTo.get(startNode);
        distToStart[index] = 0.0;
        distTo.put(startNode, distToStart);

        Comparator<DijkstraEntry> comparator = new DistanceComparator();
        pq = new PriorityQueue<>(comparator);

        pq.add(new DijkstraEntry(startNode, 0.0));

        while(!pq.isEmpty()){
            int v = pq.poll().getNode();
            if(forwards){
                if(core){
                    for (double[] e : graph.fwdCoreAdj(v)){
                        relax(v, e, index);
                    }
                } else {
                    for (double[] e : graph.fwdAdj(v)){
                        relax(v, e, index);
                    }
                }
            } else {
                if(core){
                    for (double[] e : graph.bckCoreAdj(v)){
                        relax(v, e, index);
                    }
                } else {
                    for (double[] e : graph.bckAdj(v)){
                        relax(v, e, index);
                    }
                }
            }
        }
        pq = null;
    }


    private void relax(int v, double[] edge, int index){
        explored++;
        int w = (int) edge[0];
//        System.out.println(w);
        double weight = edge[1];
        double distToV = ((double[]) (distTo.get(v)))[index];
        double[] distToW = (double[]) distTo.get(w);
        if (distToW[index] > (distToV + weight)){
            distToW[index] = distToV + weight;
            distTo.put(w, distToW);
            pq.add(new DijkstraEntry(w, distToV + weight)); //inefficient?
        }
    }

    public Int2ObjectOpenHashMap<double[]> getDistTo() {
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

    public double getDistance(int x, int index){
        return ((double[]) distTo.get(x))[index];
    }

    public void clear(){
        THashMap<Long, double[]> distTo = null;
        THashMap<Long, long[]> edgeTo = null;
        PriorityQueue<DijkstraEntry> pq = null;
        landmarks = null;
    }
}



