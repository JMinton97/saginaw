package project.map;


import gnu.trove.map.hash.THashMap;
import javafx.util.Pair;
import org.mapdb.BTreeMap;
import gnu.trove.map.hash.TLongDoubleHashMap;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;

import java.io.*;
import java.util.*;

public class AStar {
    private long pollTimeStart, pollTimeEnd, totalPollTime, addTimeStart, addTimeEnd, totalAddTime, relaxTimeStart, relaxTimeEnd, totalRelaxTime, putTimeStart, putTimeEnd, totalPutTime;
    private THashMap<Long, Double> distTo;
    private THashMap<Long, Long> edgeTo;
    private PriorityQueue<DijkstraEntry> pq;
    private long startNode, endNode;
    public int explored;
    private MyGraph myGraph;
    private ArrayList<Long> landmarks;
    private HashMap<Long, double[]> distancesTo;
    private HashMap<Long, double[]> distancesFrom;

    public AStar(MyGraph graph){
        this.myGraph = graph;
        landmarks = new ArrayList<>();
        try {
            Precomputation();
        } catch(IOException ie) {
            ie.printStackTrace();
        }
    }

    public ArrayList<Long> search(long start, long end){
        System.out.println("search");
        distTo = new THashMap<Long, Double>(myGraph.getGraph().size());
        edgeTo = new THashMap<Long, Long>(myGraph.getGraph().size());
        pq = new PriorityQueue();

//        HashMap<Long, Double> nodeWeights = MakeNodeWeights(graph.getGraph());

        for(Long vert : myGraph.getGraph().keySet()){
            distTo.put(vert, Double.MAX_VALUE);
        }
        distTo.put(start, 0.0);

        Comparator<DijkstraEntry> comparator = new DistanceComparator();
        pq = new PriorityQueue<>(comparator);

        pq.add(new DijkstraEntry(start, 0.0));

        long startTime = System.nanoTime();
        OUTER: while(!pq.isEmpty()){
            pollTimeStart = System.nanoTime();
            long v = pq.poll().getNode();
//            System.out.println("next is " + v);
            pollTimeEnd = System.nanoTime();
            totalPollTime += (pollTimeEnd - pollTimeStart);
            for (double[] e : myGraph.adj(v)){
                relax(v, e, end);
                if(v == end){
                    System.out.println("AStar terminate.");
                    break OUTER;
                }
            }
        }
        long endTime = System.nanoTime();
        System.out.println("done");
        System.out.println("Inner AStar time: " + (((float) endTime - (float)startTime) / 1000000000));
        return getRoute();
    }

    private void relax(Long v, double[] edge, long t){
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
//            System.out.println("distTo " + distTo.get(w) + " lower bound " + lowerBound(w, t));
            pq.add(new DijkstraEntry(w, distToV + weight + lowerBound(w, t))); //inefficient?
            addTimeEnd = System.nanoTime();
            totalAddTime += (addTimeEnd - addTimeStart);
        }
        relaxTimeEnd = System.nanoTime();
        totalRelaxTime += (relaxTimeEnd - relaxTimeStart);
    }

    public void Precomputation() throws IOException {
        Map<Long, Set<double[]>> graph = myGraph.getGraph();
        BTreeMap<Long, double[]> dictionary = myGraph.getDictionary();
        distancesTo = new HashMap<Long, double[]>(); //need to compute
        distancesFrom = new HashMap<Long, double[]>();
        GenerateLandmarks();
        DijkstraLandmarks dj;

        File dfDir = new File("files//astar//distancesFrom.ser");
        if(dfDir.exists()){
            System.out.println("Found distancesFrom.");
            FileInputStream fileIn = new FileInputStream(dfDir);
            FSTObjectInput objectIn = new FSTObjectInput(fileIn);
            try {
                distancesFrom = (HashMap<Long, double[]>) objectIn.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            fileIn.close();
            objectIn.close();
        } else {
            dj = new DijkstraLandmarks(this.myGraph, landmarks);
            distancesFrom = dj.getDistTo();
            FileOutputStream fileOut = new FileOutputStream(dfDir);
            FSTObjectOutput objectOut = new FSTObjectOutput(fileOut);
            objectOut.writeObject(distancesFrom);
            objectOut.close();
            dj.clear();
        }

        File dtDir = new File("files//astar//distancesTo.ser");
        if(dtDir.exists()){
            System.out.println("Found distancesTo.");
            FileInputStream fileIn = new FileInputStream(dtDir);
            FSTObjectInput objectIn = new FSTObjectInput(fileIn);
            try {
                distancesTo = (HashMap<Long, double[]>) objectIn.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            fileIn.close();
            objectIn.close();
        } else {
            dj = new DijkstraLandmarks(this.myGraph, landmarks);                             // <-- need reverse graph here
            distancesTo = dj.getDistTo();
            FileOutputStream fileOut = new FileOutputStream(dtDir);
            FSTObjectOutput objectOut = new FSTObjectOutput(fileOut);
            objectOut.writeObject(distancesTo);
            objectOut.close();
            dj.clear();
        }
    }

    public void GenerateLandmarks(){
        Map<Long, Set<double[]>> graph = myGraph.getGraph();
        int size = graph.size();
        Random random = new Random();
        List<Long> nodes = new ArrayList<>(graph.keySet());

        for(int x = 0; x < 4; x++){
            landmarks.add(nodes.get(random.nextInt(size)));
        }

//        landmarks.clear();
//
//        landmarks.add(Long.parseLong("27103812"));
//        landmarks.add(Long.parseLong("299818750"));
//        landmarks.add(Long.parseLong("312674444"));
//        landmarks.add(Long.parseLong("273662"));
//        landmarks.add(Long.parseLong("14644591"));
//        landmarks.add(Long.parseLong("27210725"));
//        landmarks.add(Long.parseLong("817576914"));
//        landmarks.add(Long.parseLong("262840382"));
//        landmarks.add(Long.parseLong("344881575"));
//        landmarks.add(Long.parseLong("1795462073"));
    }

    public double lowerBound(long u, long v){
        double max = 0;
        double[] dTU, dFU, dTV, dFV;
        dTU = distancesTo.get(u);
        dFU = distancesFrom.get(u);
        dTV = distancesTo.get(v);
        dFV = distancesFrom.get(v);

        for(int l = 0; l < landmarks.size(); l++){
            max = Math.max(max, Math.max(dTU[l] - dTV[l], dFV[l] - dFU[l]));
        }
        return max;
    }

    public double getDistTo(long node) {
        return distTo.get(node);
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

    public void clear(){
        distTo.clear();
        edgeTo.clear();
        pq.clear();
        landmarks.clear();
        distancesTo.clear();
        distancesFrom.clear();
        myGraph = null;
    }

}



