package project.map;

import gnu.trove.map.hash.THashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import javafx.util.Pair;
import org.mapdb.BTreeMap;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class BiAStar {
    long startTime, endTime, relaxTimeStart, relaxTimeEnd, totalRelaxTime, arelaxTimeStart, arelaxTimeEnd, atotalRelaxTime, containsTimeStart, containsTimeEnd, totalContainsTime, pollTimeStart, pollTimeEnd, totalPollTime, relaxPutTimeStart, relaxPutTimeEnd, totalRelaxPutTime;
    THashMap<Long, Double> uDistTo;
    THashMap<Long, Long> uEdgeTo;
    THashMap<Long, Long> uNodeTo;
    THashMap<Long, Double> vDistTo;
    THashMap<Long, Long> vEdgeTo;
    THashMap<Long, Long> vNodeTo;
    PriorityQueue<DijkstraEntry> uPq;
    PriorityQueue<DijkstraEntry> vPq;
    long start, end;
    MyGraph myGraph;
    ArrayList<Long> landmarks;
    Long2ObjectOpenHashMap distancesTo;
    Long2ObjectOpenHashMap distancesFrom;
    private HashSet<Long> uRelaxed;
    private HashSet<Long> vRelaxed;
    public Long overlapNode;
    private double maxDist; //how far from the nodes we have explored - have we covered minimum distance yet?
    public double bestSeen;
    public int explored, size;
    public String filePrefix;

    public BiAStar(MyGraph myGraph) {
        this.myGraph = myGraph;
        landmarks = new ArrayList<>();

        filePrefix = myGraph.getFilePrefix();

        try {
            Precomputation();
        } catch (IOException ie) {
            ie.printStackTrace();
        }

//        Map<Long, Set<double[]>> graph = myGraph.getGraph();

        size = myGraph.getFwdGraph().size();

        uDistTo = new THashMap<>(size);
        uEdgeTo = new THashMap<>(size);
        uNodeTo = new THashMap<>(size);
        vDistTo = new THashMap<>(size);
        vEdgeTo = new THashMap<>(size);
        vNodeTo = new THashMap<>(size);

    }

    public ArrayList<Long> search(long start, long end){

//        System.out.println("From " + start + " to " + end);

        explored = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        Calendar cal = Calendar.getInstance();

//        System.out.println(sdf.format(cal.getTime()));
//        cal = Calendar.getInstance();
//        System.out.println(sdf.format(cal.getTime()));

        uDistTo.clear();
        uEdgeTo.clear();
        uNodeTo.clear();
        vDistTo.clear();
        vEdgeTo.clear(); //.clear() to retain size
        vNodeTo.clear();



//        timerStart();
//        for(Long vert : graph.getGraph().keySet()){
//            uDistTo.put(vert, Double.MAX_VALUE);
//        }
//        for(Long vert : graph.getGraph().keySet()){
//            vDistTo.put(vert, Double.MAX_VALUE);
//        }
//        timerEnd("Filling maps");

        this.start = start;
        this.end = end;

        uDistTo.put(start, 0.0);
        vDistTo.put(end, 0.0);

        uPq = new PriorityQueue<>(new DistanceComparator());
        vPq = new PriorityQueue<>(new DistanceComparator());

        uPq.add(new DijkstraEntry(start, 0.0));
        vPq.add(new DijkstraEntry(end, 0.0));

        uRelaxed = new HashSet<>();
        vRelaxed = new HashSet<>();

        bestSeen = Double.MAX_VALUE;
        long bestPathNode = 0;

//        double minDist = haversineDistance(startNode, endNode, dictionary);
        double uFurthest, vFurthest = 0;

        double competitor;

        maxDist = 0;

        long startTime = System.nanoTime();
        OUTER: while(!(uPq.isEmpty()) && !(vPq.isEmpty())){ //check
            pollTimeStart = System.nanoTime();
            long v1 = uPq.poll().getNode();
            pollTimeEnd = System.nanoTime();
            totalPollTime += (pollTimeEnd - pollTimeStart);
            for (double[] e : myGraph.fwdAdj(v1)){
                relax(v1, e, true);
                if (vRelaxed.contains((long) e[0])) {
                    competitor = (uDistTo.get(v1) + e[1] + vDistTo.get((long) e[0]));
                    if (bestSeen > competitor) {
                        bestSeen = competitor;
                        bestPathNode = v1;
                    }
                }
                if (vRelaxed.contains(v1)) {
//                    System.out.println("truth");
                    if((uDistTo.get(v1) + vDistTo.get(v1)) < bestSeen){
                        overlapNode = v1;
                    } else {
                        overlapNode = bestPathNode;
                    }
                    long endTime = System.nanoTime();
//                    System.out.println("Inner Bi-AStar time: " + (((float) endTime - (float)startTime) / 1000000000));
                    return getRouteAsWays();
                }
            }
            pollTimeStart = System.nanoTime();
            long v2 = vPq.poll().getNode();
            pollTimeEnd = System.nanoTime();
            totalPollTime += (pollTimeEnd - pollTimeStart);
            for (double[] e : myGraph.bckAdj(v2)) {
                relax(v2, e, false);
                containsTimeStart = System.nanoTime();
                if (uRelaxed.contains((long) e[0])) {
                    competitor = (vDistTo.get(v2) + e[1] + uDistTo.get((long) e[0]));
                    if (bestSeen > competitor) {
                        bestSeen = competitor;
                        bestPathNode = v2;
                    }
                }
                containsTimeEnd = System.nanoTime();
                totalContainsTime += (containsTimeEnd - containsTimeStart);
                containsTimeStart = System.nanoTime();
                if (uRelaxed.contains(v2)) { //FINAL TERMINATION
//                    System.out.println("truth");
                    if((uDistTo.get(v2) + vDistTo.get(v2)) < bestSeen){
                        overlapNode = v2;
                    } else {
                        overlapNode = bestPathNode;
                    }
                    long endTime = System.nanoTime();
                    System.out.println("Inner Bi-AStar time: " + (((float) endTime - (float)startTime) / 1000000000));
                    return getRouteAsWays();
                }
                containsTimeEnd = System.nanoTime();
                totalContainsTime += (containsTimeEnd - containsTimeStart);
            }
        }
        long endTime = System.nanoTime();
        System.out.println("Inner Bi-AStar time: " + (((float) endTime - (float)startTime) / 1000000000));
        System.out.println("No route found.");
        return new ArrayList<>();
    }

    private void relax(Long x, double[] edge, boolean u){
        relaxTimeStart = System.nanoTime();
        explored++;
        long w = (long) edge[0];
        double weight = edge[1];
        double wayId = edge[2];
        if(u){
            uRelaxed.add(x);
            double distToX = uDistTo.getOrDefault(x, Double.MAX_VALUE);
            if (uDistTo.getOrDefault(w, Double.MAX_VALUE) > (distToX + weight)){
                relaxPutTimeStart = System.nanoTime();
                uDistTo.put(w, distToX + weight);
                uNodeTo.put(w, x); //should be 'nodeBefore'
                uEdgeTo.put(w, (long) wayId); //should be 'nodeBefore'
                relaxPutTimeEnd = System.nanoTime();
                totalRelaxPutTime += (relaxPutTimeEnd - relaxPutTimeStart);
                arelaxTimeStart = System.nanoTime();
                uPq.add(new DijkstraEntry(w, distToX + weight + lowerBound(w, true))); //inefficient?
                arelaxTimeEnd = System.nanoTime();
                atotalRelaxTime += (arelaxTimeEnd - arelaxTimeStart);
            }
        } else {
            vRelaxed.add(x);
            double distToX = vDistTo.getOrDefault(x, Double.MAX_VALUE);
            if (vDistTo.getOrDefault(w, Double.MAX_VALUE) > (distToX + weight)){
                relaxPutTimeStart = System.nanoTime();
                vDistTo.put(w, distToX + weight);
                vNodeTo.put(w, x); //should be 'nodeBefore'
                vEdgeTo.put(w, (long) wayId); //should be 'nodeBefore'
                relaxPutTimeEnd = System.nanoTime();
                totalRelaxPutTime += (relaxPutTimeEnd - relaxPutTimeStart);
                arelaxTimeStart = System.nanoTime();
                vPq.add(new DijkstraEntry(w, distToX + weight + lowerBound(w, false))); //inefficient?
                arelaxTimeEnd = System.nanoTime();
                atotalRelaxTime += (arelaxTimeEnd - arelaxTimeStart);
            }
        }
        relaxTimeEnd = System.nanoTime();
        totalRelaxTime += (relaxTimeEnd - relaxTimeStart);
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

    public void Precomputation() throws IOException {
        distancesTo = new Long2ObjectOpenHashMap<double[]>(); //need to compute
        distancesFrom = new Long2ObjectOpenHashMap<double[]>();
        GenerateLandmarks();
        DijkstraLandmarks dj;

        File dfDir = new File(filePrefix.concat("distancesFrom.ser"));
        if(dfDir.exists()){
            System.out.println("Found distancesFrom.");
            FileInputStream fileIn = new FileInputStream(dfDir);
            FSTObjectInput objectIn = new FSTObjectInput(fileIn);
            try {
                distancesFrom = (Long2ObjectOpenHashMap<double[]>) objectIn.readObject();
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
            distancesFrom = null;
        }
        System.out.println("Done first bit");

        File dtDir = new File(filePrefix.concat("distancesTo.ser"));
        if(dtDir.exists()){
            System.out.println("Found distancesTo.");
            FileInputStream fileIn = new FileInputStream(dtDir);
            FSTObjectInput objectIn = new FSTObjectInput(fileIn);
            try {
                distancesTo = (Long2ObjectOpenHashMap<double[]>) objectIn.readObject();
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
            distancesTo = null;
        }
    }

    public void GenerateLandmarks(){
        Map<Long, HashSet<double[]>> graph = myGraph.getFwdGraph();
        int size = graph.size();
        Random random = new Random();
        List<Long> nodes = new ArrayList<>(graph.keySet());

        if(myGraph.getRegion().equals("england")){
            landmarks.add(Long.parseLong("27103812"));
            landmarks.add(Long.parseLong("299818750"));
            landmarks.add(Long.parseLong("312674444"));
            landmarks.add(Long.parseLong("424430268"));
            landmarks.add(Long.parseLong("29833172"));
            landmarks.add(Long.parseLong("27210725"));
            landmarks.add(Long.parseLong("817576914"));
            landmarks.add(Long.parseLong("262840382"));
            landmarks.add(Long.parseLong("344881575"));
            landmarks.add(Long.parseLong("25276649"));
        } else if(myGraph.getRegion().equals("wales")){
            landmarks.add(Long.parseLong("260093216"));
            landmarks.add(Long.parseLong("1886093447"));
            landmarks.add(Long.parseLong("4254105731"));
            landmarks.add(Long.parseLong("1491252547"));
            landmarks.add(Long.parseLong("296030988"));
            landmarks.add(Long.parseLong("20956464"));
            landmarks.add(Long.parseLong("306247928"));
    ////        landmarks.add(Long.parseLong("262840382"));
    ////        landmarks.add(Long.parseLong("344881575"));
    ////        landmarks.add(Long.parseLong("1795462073"));
        } else if(myGraph.getRegion().equals("france")){
            landmarks.add(Long.parseLong("1997249188"));
            landmarks.add(Long.parseLong("420592228"));
            landmarks.add(Long.parseLong("1203772336"));
            landmarks.add(Long.parseLong("292093917"));
            landmarks.add(Long.parseLong("629419387"));
            landmarks.add(Long.parseLong("1161458782"));
            landmarks.add(Long.parseLong("702241324"));
            landmarks.add(Long.parseLong("31898581"));
            landmarks.add(Long.parseLong("600118738"));
            landmarks.add(Long.parseLong("268366322"));
        } else {
            for(int x = 0; x < 5; x++){
                landmarks.add(nodes.get(random.nextInt(size)));
                System.out.println(landmarks.get(x));
            }
        }
    }

    public double lowerBound(long u, boolean forwards){
        double maxForward = 0;
        double maxBackward = 0;
//        double[] dTU, dFU, dTV, dFV;

        double[] forDTU = (double[]) distancesTo.get(u);
        double[] forDFU = (double[]) distancesFrom.get(u);
        double[] forDTV = (double[]) distancesTo.get(end);
        double[] forDFV = (double[]) distancesFrom.get(end);

        double[] backDTU = (double[]) distancesTo.get(u);
        double[] backDFU = (double[]) distancesFrom.get(u);
        double[] backDTV = (double[]) distancesTo.get(start);
        double[] backDFV = (double[]) distancesFrom.get(start);

        for(int l = 0; l < landmarks.size(); l++){
            maxForward = Math.max(maxForward, Math.max(forDTU[l] - forDTV[l], forDFV[l] - forDFU[l]));
        }

        for(int l = 0; l < landmarks.size(); l++){
            maxBackward = Math.max(maxBackward, Math.max(backDTU[l] - backDTV[l], backDFV[l] - backDFU[l]));
        }

        if(forwards){
            return (maxForward - maxBackward) / 2;
        } else {
            return (maxBackward - maxForward) / 2;
        }
    }

    public Double getDist() {
        return uDistTo.get(overlapNode) + vDistTo.get(overlapNode);
    }

    private double haversineDistance(long a, long b, BTreeMap<Long, double[]> dictionary){
        double[] nodeA = dictionary.get(a);
        double[] nodeB = dictionary.get(b);
        double rad = 6371000; //radius of earth in metres
        double aLatRadians = Math.toRadians(nodeA[0]); //0 = latitude, 1 = longitude
        double bLatRadians = Math.toRadians(nodeB[0]);
        double deltaLatRadians = Math.toRadians(nodeB[0] - nodeA[0]);
        double deltaLongRadians = Math.toRadians(nodeB[1] - nodeA[1]);

        double x = Math.sin(deltaLatRadians/2) * Math.sin(deltaLatRadians/2) +
                Math.cos(aLatRadians) * Math.cos(bLatRadians) *
                        Math.sin(deltaLongRadians/2) * Math.sin(deltaLongRadians/2);
        double y = 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1-x));
        return rad * y;
    }

    public ArrayList<Long> getRoute(){
        ArrayList<Long> route = new ArrayList<>();
        long node = overlapNode;
        route.add(overlapNode);
        while(node != start){
            node = uEdgeTo.get(node);
            route.add(node);
        }
        Collections.reverse(route);
        node = overlapNode;
        while(node != end){
            node = vEdgeTo.get(node);
            route.add(node);
        }
        return route;
    }

    public ArrayList<Long> getRouteAsWays(){
        long node = overlapNode;
        ArrayList<Long> route = new ArrayList<>();
        try{
//            System.out.println("GETROUTEASWAYS");
            long way = 0;
            while(node != start && node != end){
//            System.out.println(node + ",");
                way = uEdgeTo.get(node);
                node = uNodeTo.get(node);
//            System.out.println(way);
                route.add(way);
            }

            Collections.reverse(route);
            node = overlapNode;
            while(node != start && node != end){
//            System.out.println(node + ".");
                way = vEdgeTo.get(node);
                node = vNodeTo.get(node);
//            System.out.println(way);
                route.add(way);
            }

        }catch(NullPointerException n){
            System.out.println("Null: " + node);
        }
//        System.out.println(route.size());
        return route;
    }

    private void timerStart(){
        startTime = System.nanoTime();
    }

    private void timerEnd(String string){
        endTime = System.nanoTime();
        System.out.println(string + " time: " + (((float) endTime - (float)startTime) / 1000000000));
    }

    public void clear(){
        uDistTo.clear();
        uEdgeTo.clear();
        vDistTo.clear();
        vEdgeTo.clear();
        vPq.clear();
        uPq.clear();
        vRelaxed.clear();
        uRelaxed.clear();
        landmarks.clear();
        distancesTo.clear();
        distancesFrom.clear();
        myGraph = null;
    }

}



