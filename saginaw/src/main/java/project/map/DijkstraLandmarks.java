package project.map;


import gnu.trove.map.hash.THashMap;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import javafx.util.Pair;
import sun.misc.GC;

import java.text.SimpleDateFormat;
import java.util.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class DijkstraLandmarks {
    long pollTimeStart, pollTimeEnd, totalPollTime, addTimeStart, addTimeEnd, totalAddTime, relaxTimeStart, relaxTimeEnd, totalRelaxTime, putTimeStart, putTimeEnd, totalPutTime;
    Long2ObjectOpenHashMap distTo;


//    THashMap<Long, double[]> distTo;
//    THashMap<Long, long[]> edgeTo;
    PriorityQueue<DijkstraEntry> pq;
    long startNode, endNode;
    public int explored;
    ArrayList<Long> landmarks;

    public DijkstraLandmarks(project.map.MyGraph graph, ArrayList<Long> startNodes, boolean forwards){

        distTo = new Long2ObjectOpenHashMap<double[]>();
        pq = new PriorityQueue();

        landmarks = startNodes;

        double[] initDistance = new double[startNodes.size()];
        for(int x = 0; x < initDistance.length; x++){
            initDistance[x] = Double.MAX_VALUE;
        }

        for(long vert : graph.getGraph().keySet()){
            distTo.put(vert, initDistance.clone());
        }

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        for(int x = 0; x < startNodes.size(); x++){
            Calendar cal = Calendar.getInstance();
            System.out.println(x + 1 + " of " + startNodes.size() + " " + sdf.format(cal.getTime()));
            DijkstraAlgorithm(graph, startNodes.get(x), x, forwards);
        }

    }

    public void DijkstraAlgorithm(MyGraph graph, long startNode, int index, boolean forwards){
//        System.out.println();
//        System.out.println("Start " + startNode + " Index " + index);

        double[] distToStart = (double[]) distTo.get(startNode);
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
            if(forwards){
                for (double[] e : graph.fwdAdj(v)){
//                System.out.println("relax");
                    relax(v, e, index);
                }
            } else {
                for (double[] e : graph.bckAdj(v)){
//                System.out.println("relax");
                    relax(v, e, index);
                }
            }

        }
        relaxTimeEnd = System.nanoTime();
//        System.out.println("Landmark time: " + (((float) relaxTimeEnd - (float)relaxTimeStart) / 1000000000));

        pq = null;
    }


    private void relax(Long v, double[] edge, int index){
        explored++;
        long w = (long) edge[0];
//        System.out.println(w);
        double weight = edge[1];
        double distToV = ((double[]) (distTo.get(v)))[index];
        double[] distToW = (double[]) distTo.get(w);
        System.out.println();
        System.out.println(v);
        System.out.println(w);
//        System.out.println(index);
        System.out.println(distToW[index]);
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

    public Long2ObjectOpenHashMap<double[]> getDistTo() {
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
        return ((double[]) distTo.get(x))[index];
    }

    public void clear(){
        THashMap<Long, double[]> distTo = null;
        THashMap<Long, long[]> edgeTo = null;
        PriorityQueue<DijkstraEntry> pq = null;
        landmarks = null;
    }
}



