package project.search;


import gnu.trove.map.hash.THashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.mapdb.BTreeMap;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import project.map.MyGraph;

import java.io.*;
import java.util.*;

public class AStar implements Searcher {
    private long pollTimeStart, pollTimeEnd, totalPollTime, addTimeStart, addTimeEnd, totalAddTime, relaxTimeStart, relaxTimeEnd, totalRelaxTime, putTimeStart, putTimeEnd, totalPutTime;
    private THashMap<Long, Double> distTo;
    private THashMap<Long, Long> edgeTo;
    private PriorityQueue<DijkstraEntry> pq;
    private long startNode, endNode;
    public int explored;
    private MyGraph myGraph;
    private ArrayList<Long> landmarks;
    private Long2ObjectOpenHashMap distancesTo;
    private Long2ObjectOpenHashMap distancesFrom;
    private long src, dst;

    public AStar(MyGraph graph){
        this.myGraph = graph;
        landmarks = new ArrayList<>();
        try {
            Precomputation();
        } catch(IOException ie) {
            ie.printStackTrace();
        }
    }

    public AStar(MyGraph graph, ALTPreProcess altPreProcess){
        this.myGraph = graph;
        landmarks = new ArrayList<>();
        this.landmarks = altPreProcess.landmarks;
        this.distancesFrom = altPreProcess.distancesFrom;
        this.distancesTo = altPreProcess.distancesTo;
    }

    public ArrayList<Long> search(long src, long dst){
        this.dst = dst;
        this.src = src;
//        System.out.println("search");
        distTo = new THashMap<Long, Double>(myGraph.getFwdGraph().size());
        edgeTo = new THashMap<Long, Long>(myGraph.getFwdGraph().size());
        pq = new PriorityQueue();

//        HashMap<Long, Double> nodeWeights = MakeNodeWeights(graph.getGraph());

        for(Long vert : myGraph.getFwdGraph().keySet()){
            distTo.put(vert, Double.MAX_VALUE);
        }
        distTo.put(src, 0.0);

        Comparator<DijkstraEntry> comparator = new DistanceComparator();
        pq = new PriorityQueue<>(comparator);

        pq.add(new DijkstraEntry(src, 0.0));

        explored = 0;

        long startTime = System.nanoTime();
        OUTER: while(!pq.isEmpty()){
            explored++;
            pollTimeStart = System.nanoTime();
            long v = pq.poll().getNode();
//            System.out.println("next is " + v);
            pollTimeEnd = System.nanoTime();
            totalPollTime += (pollTimeEnd - pollTimeStart);
            for (double[] e : myGraph.fwdAdj(v)){
                relax(v, e, dst);
                if(v == dst){
//                    System.out.println("AStar terminate.");
                    break OUTER;
                }
            }
        }
        long endTime = System.nanoTime();
//        System.out.println("done");
//        System.out.println("Inner AStar time: " + (((float) endTime - (float)startTime) / 1000000000));
        return getRoute();
    }

    private void relax(Long v, double[] edge, long t){
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
        Map<Long, ArrayList<double[]>> graph = myGraph.getFwdGraph();
        BTreeMap<Long, double[]> dictionary = myGraph.getDictionary();
        distancesTo = new Long2ObjectOpenHashMap<double[]>(); //need to compute
        distancesFrom = new Long2ObjectOpenHashMap<double[]>();
        GenerateLandmarks();
        DijkstraLandmarks dj;

        File dfDir = new File("files//astar//distancesFrom.ser");
        if(dfDir.exists()){
            System.out.println("Found distancesFrom.");
            FileInputStream fileIn = new FileInputStream(dfDir);
            FSTObjectInput objectIn = new FSTObjectInput(fileIn);
            try {
                distancesFrom = (Long2ObjectOpenHashMap) objectIn.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            fileIn.close();
            objectIn.close();
        } else {
            dj = new DijkstraLandmarks(this.myGraph, landmarks, true);
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
                distancesTo = (Long2ObjectOpenHashMap) objectIn.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            fileIn.close();
            objectIn.close();
        } else {
            dj = new DijkstraLandmarks(this.myGraph, landmarks, false);                             // <-- need reverse graph here
            distancesTo = dj.getDistTo();
            FileOutputStream fileOut = new FileOutputStream(dtDir);
            FSTObjectOutput objectOut = new FSTObjectOutput(fileOut);
            objectOut.writeObject(distancesTo);
            objectOut.close();
            dj.clear();
        }
    }

    public void GenerateLandmarks(){
        Map<Long, ArrayList<double[]>> graph = myGraph.getFwdGraph();
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
        dTU = (double[]) distancesTo.get(u);
        dFU = (double[]) distancesFrom.get(u);
        dTV = (double[]) distancesTo.get(v);
        dFV = (double[]) distancesFrom.get(v);

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

    public double getDist(){
        return distTo.get(dst);
    }

    public int getExplored(){
        return explored;
    }

}



