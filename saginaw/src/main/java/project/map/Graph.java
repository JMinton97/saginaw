package project.map;

import com.google.common.collect.HashBiMap;
import crosby.binary.*;
import crosby.binary.Osmformat.*;
import crosby.binary.file.*;
//import crosby.binary.test.MyNode;

import java.awt.geom.Point2D;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import javafx.util.Pair;
import org.mapdb.*;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import project.kdtree.Tree;
//import project.search.Dijkstra;
import project.search.DijkstraEntry;

public class Graph {
    //    public static ArrayList<MyNode> mapNodes = new ArrayList<MyNode>();
//    public static BTreeMap<Long, double[]> dictionary; //maps a node id to a double array containing the coordinates of the node
    public static ArrayList<double[]> dictionary;
    public static Map<Long, int[]> mapRoads; //a list of all connections between nodes. Later becomes all graph edges.
    private static Map<Long, int[]> coreMapRoads; //a list of all connections between nodes, in the core.
    private static ConcurrentMap<Integer, Integer> allWayNodes; //maps the nodes contained in the extracted ways to a counter of the number of ways each one is part of
    private static boolean parsingNodes; //flag used when parsing to indicate whether to parse nodes or ways
    private static ArrayList<ArrayList<double[]>> fwdGraph;
    private static ArrayList<ArrayList<double[]>> bckGraph;
    private static Map<Integer, ArrayList<double[]>> fwdCore;
    private static Map<Integer, ArrayList<double[]>> bckCore;
    private static HashMap<Integer, Double> keys = new HashMap<>(); //holds the keys in
    private Pair<ArrayList, ArrayList> graph;
    private Tree tree;
    private int bypassNode;
    private double contractionParameter = 2.5;
    private double hopLimiter = 50;
    private final double DOU_THRESHOLD = .0001;
    private static HashMap<Long, Integer> nodeLong2Int;
    private static HashMap<Integer, Long> nodeInt2Long;
    private static HashMap<Integer, Integer> graphNodeMappings;
    private int maxId = 0;

    private long startTime;
    private long endTime;


    private String filePrefix, region;

    Set<long[]> edges = new HashSet<>();
    int noOfEdges;

    public Graph(File file, String region) throws IOException {

        this.region = region;

        filePrefix = "files//".concat(region + "//");

        nodeLong2Int = new HashMap<>();
        nodeInt2Long = new HashMap<>();

        DB db3 = DBMaker
                .fileDB(filePrefix.concat("mapRoads.db"))
                .fileMmapEnable()
                .checksumHeaderBypass()
                .closeOnJvmShutdown()
                .make();

        mapRoads = db3.treeMap("map", Serializer.LONG, Serializer.INT_ARRAY).createOrOpen();

        coreMapRoads = db3.treeMap("coreMap", Serializer.LONG, Serializer.INT_ARRAY).createOrOpen();

        graphNodeMappings = new HashMap<>();

        makeDictionary(file);

        File fwdGraphDir = new File(filePrefix.concat("fwdGraph.ser"));
        File bckGraphDir = new File(filePrefix.concat("bckGraph.ser"));
        File treeDir = new File(filePrefix.concat("tree.ser"));
        File fwdCoreDir = new File(filePrefix.concat("fwdCore.ser"));
        File bckCoreDir = new File(filePrefix.concat("bckCore.ser"));
        File graphNodeMappingsDir = new File(filePrefix.concat("mappings.ser"));
        if(fwdGraphDir.exists()){
            System.out.println("Found graph.");
            timerStart();
            FileInputStream fileIn = new FileInputStream(fwdGraphDir);
            FSTObjectInput objectIn = new FSTObjectInput(fileIn);
            try {
                fwdGraph = (ArrayList<ArrayList<double[]>>) objectIn.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            fileIn = new FileInputStream(bckGraphDir);
            objectIn = new FSTObjectInput(fileIn);
            try {
                bckGraph = (ArrayList<ArrayList<double[]>>) objectIn.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            fileIn = new FileInputStream(graphNodeMappingsDir);
            objectIn = new FSTObjectInput(fileIn);
            try {
                graphNodeMappings = (HashMap<Integer, Integer>) objectIn.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            timerEnd("Read graph");

            if(treeDir.exists()){
                System.out.println("Found tree.");
                FileInputStream treeIn = new FileInputStream(treeDir);
                FSTObjectInput treeObjectIn = new FSTObjectInput(treeIn);
                try {
                    tree = (Tree) treeObjectIn.readObject();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("No tree found, making now.");
                makeTree();
                timerStart();
                FileOutputStream fileOut = new FileOutputStream(treeDir);
                FSTObjectOutput objectOut = new FSTObjectOutput(fileOut);
                objectOut.writeObject(tree);
                objectOut.close();
                timerEnd("Writing tree");
            }

            if(fwdCoreDir.exists()){
                System.out.println("Found core.");
                timerStart();
                fileIn = new FileInputStream(fwdCoreDir);
                objectIn = new FSTObjectInput(fileIn);
                try {
                    fwdCore = (Map<Integer, ArrayList<double[]>>) objectIn.readObject();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                fileIn = new FileInputStream(bckCoreDir);
                objectIn = new FSTObjectInput(fileIn);
                try {
                    bckCore = (Map<Integer, ArrayList<double[]>>) objectIn.readObject();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                System.out.println("Read core.");
            } else {
                System.out.println("No core found, making now.");
                contract();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                timerStart();
                System.out.println(fwdCore.size());
                System.out.println(bckCore.size());
                try{
                    FileOutputStream fileOut = new FileOutputStream(fwdCoreDir);
                    FSTObjectOutput objectOut = new FSTObjectOutput(fileOut);
                    objectOut.writeObject(fwdCore);
                    objectOut.flush();
                    fileOut = new FileOutputStream(bckCoreDir);
                    objectOut = new FSTObjectOutput(fileOut);
                    objectOut.writeObject(bckCore);
                    objectOut.flush();
                }catch(IOException e){
                    e.printStackTrace();
                }
                timerEnd("Writing core");
            }



//            System.out.println(sdf.format(cal.getTime()));

        } else {
            System.out.println("No graph found, creating now.");

            graph = makeDijkstraGraph(mapRoads, mapRoads.size());

            nodeInt2Long = null;
            nodeLong2Int = null;

            timerStart();
            FileOutputStream fileOut = new FileOutputStream(fwdGraphDir);
            FSTObjectOutput objectOut = new FSTObjectOutput(fileOut);
            objectOut.writeObject(fwdGraph);
            objectOut.close();

            fileOut = new FileOutputStream(bckGraphDir);
            objectOut = new FSTObjectOutput(fileOut);
            objectOut.writeObject(bckGraph);
            objectOut.close();

            fileOut = new FileOutputStream(graphNodeMappingsDir);
            objectOut = new FSTObjectOutput(fileOut);
            objectOut.writeObject(graphNodeMappings);
            objectOut.close();

            timerEnd("Writing graph");

            makeTree();

            timerStart();
            fileOut = new FileOutputStream(treeDir);
            objectOut = new FSTObjectOutput(fileOut);
            objectOut.writeObject(tree);
            objectOut.close();
            timerEnd("Writing tree");

            contract();

            timerStart();
            fileOut = new FileOutputStream(fwdCoreDir);
            objectOut = new FSTObjectOutput(fileOut);
            objectOut.writeObject(fwdCore);
            objectOut.flush();
            fileOut = new FileOutputStream(bckCoreDir);
            objectOut = new FSTObjectOutput(fileOut);
            objectOut.writeObject(bckCore);
            objectOut.flush();
            timerEnd("Writing core");
        }
    }

    private void makeDictionary(File file) throws IOException{
        DB db2 = DBMaker
                .fileDB(filePrefix.concat("dictionary.db"))
                .fileMmapEnable()
                .checksumHeaderBypass()
                .closeOnJvmShutdown()
                .make();

        File dictionaryDir = new File(filePrefix.concat("dictionary.ser"));

        if(dictionaryDir.exists()){
            System.out.println("Found dictionary.");
            timerStart();
            FileInputStream fileIn = new FileInputStream(dictionaryDir);
            FSTObjectInput objectIn = new FSTObjectInput(fileIn);
            try {
                dictionary = (ArrayList<double[]>) objectIn.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            timerEnd("Read dictionary");
        }else{

            dictionary = new ArrayList<>();

            System.out.println("Generating dictionary.");
            DB db = DBMaker
                    .fileDB(filePrefix.concat("wayNodes.db"))
                    .fileMmapEnable()
                    .checksumHeaderBypass()
                    .closeOnJvmShutdown()
                    .make();


            allWayNodes = db
                    .treeMap("map", Serializer.INTEGER, Serializer.INTEGER)
                    .createOrOpen();


            parsingNodes = false;

            edges = new HashSet<>();

            noOfEdges = 0;

            if(allWayNodes.isEmpty()){
                timerStart();
                InputStream input = new FileInputStream(file);
                BlockReaderAdapter brad = new OSMBinaryParser();
                long startTime = System.nanoTime();
                new BlockInputStream(input, brad).process();
                long endTime = System.nanoTime();
                timerEnd("Getting ways");

                timerStart();
                System.out.println("Map roads pre-split:      " + mapRoads.size());
                splitWays(mapRoads, true);
                noOfEdges = mapRoads.size();
                System.out.println("Map roads post-split:     " + noOfEdges);
                timerEnd("Splitting ways");
            } else {
                System.out.println("allWayNodes found; skipping read");
            }

            parsingNodes = true;

            timerStart();
            InputStream input2 = new FileInputStream(file);
            BlockReaderAdapter brad2 = new OSMBinaryParser();
            new BlockInputStream(input2, brad2).process();
            timerEnd("Getting nodes");

            timerStart();
            FileOutputStream fileOut = new FileOutputStream(dictionaryDir);
            FSTObjectOutput objectOut = new FSTObjectOutput(fileOut);
            objectOut.writeObject(dictionary);
            objectOut.close();
            timerEnd("Writing dictionary");


        }
    }

    private Pair<ArrayList, ArrayList> makeDijkstraGraph(Map<Long, int[]> edges, int noOfEdges){
        fwdGraph = new ArrayList<ArrayList<double[]>>();
        bckGraph = new ArrayList<ArrayList<double[]>>();

        timerStart();
        System.out.println("Adding connections");
        int counter = 0;
        for(Map.Entry<Long, int[]> way : edges.entrySet()){ //iterate through every edge and add neighbours to graph vertices accordingly
            counter++;
            if((counter % 200000) == 0){
                System.out.println(((double) counter / (double) noOfEdges) * 100);
            }

            int[] wayNodes = way.getValue();
            int fstVert, lstVert;
            if(wayNodes.length > 1){
                if(graphNodeMappings.containsKey(wayNodes[0])){
                    fstVert = graphNodeMappings.get(wayNodes[0]);

                } else {
                    graphNodeMappings.put(wayNodes[0], graphNodeMappings.size());
                    fstVert = graphNodeMappings.get(wayNodes[0]);
                    fwdGraph.add(new ArrayList<>());
                    bckGraph.add(new ArrayList<>());
                }

                if(graphNodeMappings.containsKey(wayNodes[wayNodes.length - 1])){
                    lstVert = graphNodeMappings.get(wayNodes[wayNodes.length - 1]);

                } else {
                    graphNodeMappings.put(wayNodes[wayNodes.length - 1], graphNodeMappings.size());
                    lstVert = graphNodeMappings.get(wayNodes[wayNodes.length - 1]);
                    fwdGraph.add(new ArrayList<>());
                    bckGraph.add(new ArrayList<>());
                }

                if(fstVert != lstVert){
                    double length = lengthOfEdge(wayNodes);
                    fwdGraph.get(fstVert).add(new double[]{(double) lstVert, length, way.getKey().doubleValue(), 1}); //edge array stores target of edge, length of edge,
                    bckGraph.get(lstVert).add(new double[]{(double) fstVert, length, way.getKey().doubleValue(), 1}); //wayId of edge, and hop number (for contraction)

                }
            }
        }
        timerEnd("Creating graph");

        graphNodeMappings = invertMappings(graphNodeMappings);

        return new Pair<>(fwdGraph, bckGraph);
    }

    private HashMap<Integer, Integer> invertMappings(HashMap<Integer, Integer> graphNodeMappings){
        HashMap<Integer, Integer> inverseMap = new HashMap<>();
        for(Map.Entry<Integer, Integer> entry : graphNodeMappings.entrySet()){
            inverseMap.put(entry.getValue(), entry.getKey());
        }

        return inverseMap;
    }


    private void contract() {

        Comparator<Pair<Integer, Double>> comp = new KeyComparator(); //maybe we can use
        PriorityQueue<Pair<Integer, Double>> heap = new PriorityQueue<>(comp);
        keys = new HashMap<>(fwdGraph.size());
        fwdCore = new HashMap<>();
        bckCore = new HashMap<>();

        for(int i = 0; i < fwdGraph.size(); i++){
            ArrayList<double[]> newEdges = new ArrayList<>();
            for(double[] edge : fwdGraph.get(i)){
                double[] newEdge = new double[edge.length];
                for(int j = 0; j < edge.length; j++){
                    newEdge[j] = edge[j];
                }
                newEdges.add(newEdge);
            }
            fwdCore.put(i, newEdges);
        }

        for(int i = 0; i < bckGraph.size(); i++){
            ArrayList<double[]> newEdges = new ArrayList<>();
            for(double[] edge : bckGraph.get(i)){
                double[] newEdge = new double[edge.length];
                for(int j = 0; j < edge.length; j++){
                    newEdge[j] = edge[j];
                }
                newEdges.add(newEdge);
            }
            bckCore.put(i, newEdges);
        }

        System.out.println("Original size: " + fwdGraph.size() + " " + bckGraph.size());

        int edgeCounter = 0;
        for (ArrayList<double[]> nodeEntry : fwdGraph) {
            edgeCounter = nodeEntry.size() + edgeCounter;
        }

        int originalSize = fwdGraph.size();

        System.out.println("Original edge number: " + edgeCounter);

        System.out.println("Contract size: " + fwdCore.size());

        edgeCounter = 0;

        int i = 0;

        for (Map.Entry<Integer, ArrayList<double[]>> nodeEntry : fwdCore.entrySet()) {

            i++;
            edgeCounter = nodeEntry.getValue().size() + edgeCounter;
            int node = nodeEntry.getKey();
            vertexHeapAdd(node, heap);

        }

        System.out.println("Edge number: " + edgeCounter);

        i = 0;

        while (!heap.isEmpty()) {
            i++;
            if (i % 500000 == 0) {
                System.out.println(i + " " + (100 * ((float) fwdCore.size() / (float) originalSize)) + "%. Heap size " + heap.size());
            }
            Pair<Integer, Double> entry = heap.poll();

            if(keys.containsKey(entry.getKey())){
                if(!entry.getValue().equals(keys.get(entry.getKey()))){
                    continue;
                }
            } else {
                continue;
            }

            bypassNode = entry.getKey();


            if(!doubleEquals(checkKey(entry.getKey()), entry.getValue())){
                System.out.println("Error with " + bypassNode);
                System.out.println("Expected: " + checkKey(entry.getKey()) + ", actual: " + entry.getValue());
                System.exit(0);
                continue;
            }

            if(checkKey(entry.getKey()) == Double.MAX_VALUE){
                System.out.println("Nope...");
                continue;
            }

            HashSet<Integer> alteredNodes = new HashSet<>();

            if(fwdCore.containsKey(bypassNode)) {

                ArrayList<double[]> beforeEdges = new ArrayList<>();
                beforeEdges.addAll(bckCore.get(bypassNode));
                ArrayList<double[]> afterEdges = new ArrayList<>();
                afterEdges.addAll(fwdCore.get(bypassNode));
                Iterator beforeItr = beforeEdges.iterator();

                while (beforeItr.hasNext()) {
                    double[] beforeEdge = (double[]) beforeItr.next();
                    Iterator afterItr = afterEdges.iterator();
                    INNER: while (afterItr.hasNext()) {
                        double[] afterEdge = (double[]) afterItr.next();
                        if ((int) afterEdge[0] == (long) beforeEdge[0]) {    //check we're not making an x to x edge
                            continue;
                        }

                        long fwdId = joinWays((long) beforeEdge[2], (long) afterEdge[2]);
                        long bckId = joinWays((long) afterEdge[2], (long) beforeEdge[2]);
                        fwdCore.get((int) beforeEdge[0]).add(new double[]{afterEdge[0], beforeEdge[1] + afterEdge[1], fwdId, beforeEdge[3] + afterEdge[3]});   //add shortcut to forward and backward graph
                        bckCore.get((int) afterEdge[0]).add(new double[]{beforeEdge[0], beforeEdge[1] + afterEdge[1], bckId, beforeEdge[3] + afterEdge[3]});


                        alteredNodes.add((int) afterEdge[0]);
                        alteredNodes.add((int) beforeEdge[0]);
                    }
                }


                ArrayList<double[]> backwardsEdges = new ArrayList<>();
                backwardsEdges.addAll(bckCore.get(bypassNode));
                ArrayList<double[]> forwardsEdges = new ArrayList<>();
                forwardsEdges.addAll(fwdCore.get(bypassNode));

                for (double[] awayEdge : fwdCore.get(bypassNode)) {
                    Iterator it = bckCore.get((int) awayEdge[0]).iterator();
                    while (it.hasNext()) {
                        double[] returnEdge = (double[]) it.next();
                        if (returnEdge[0] == bypassNode) {
                            it.remove();
                        }
                    }
                }

                for (double[] awayEdge : bckCore.get(bypassNode)) {
                    Iterator it = fwdCore.get((int) awayEdge[0]).iterator();
                    while (it.hasNext()) {
                        double[] returnEdge = (double[]) it.next();
                        if (returnEdge[0] == bypassNode) {
                            it.remove();
                        }
                    }
                }

                fwdCore.remove(bypassNode);
                bckCore.remove(bypassNode);
                keys.remove(bypassNode);

                for (double[] backwardsEdge : backwardsEdges) {
                    int node = (int) backwardsEdge[0];
                    if(bckCore.containsKey(node)){
                        for(double[] backBackEdge : bckCore.get(node)){
//                            System.out.println((long) backBackEdge[0]);
                            vertexHeapAdd((int) backBackEdge[0], heap);
                        }
                    }
                    if(fwdCore.containsKey(node)){
                        for (double[] forForEdge : fwdCore.get(node)) {
//                            System.out.println((long) forForEdge[0]);
                            vertexHeapAdd((int) forForEdge[0], heap);
                        }
                    }
                    vertexHeapAdd(node, heap);
                }

                for (double[] forwardsEdge : forwardsEdges) {
                    int node = (int) forwardsEdge[0];
                    if(fwdCore.containsKey(node)){
                        for (double[] forForEdge : fwdCore.get(node)) {
//                            System.out.println((long) forForEdge[0]);
                            vertexHeapAdd((int) forForEdge[0], heap);
                        }
                    }
                    if(bckCore.containsKey(node)){
                        for(double[] backBackEdge : bckCore.get(node)){
//                            System.out.println((long) backBackEdge[0]);
                            vertexHeapAdd((int) backBackEdge[0], heap);
                        }
                    }
                    vertexHeapAdd(node, heap);
                }
            }
        }



        edgeCounter = 0;

        int emptyFwdCounter = 0;
        int emptyBckCounter = 0;

        for (Map.Entry<Integer, ArrayList<double[]>> nodeEntry : fwdCore.entrySet()) {
            if(nodeEntry.getValue().size() == 0){
//                fwdCore.remove(nodeEntry.getKey());
                emptyFwdCounter++;
            }
        }

        for (Map.Entry<Integer, ArrayList<double[]>> nodeEntry : bckCore.entrySet()) {
            if(nodeEntry.getValue().size() == 0){
//                bckCore.remove(nodeEntry.getKey());
                emptyBckCounter++;
            }
        }

        System.out.println("Empty forward:" + emptyFwdCounter);
        System.out.println("Empty back:" + emptyBckCounter);


        System.out.println("Edges before:   " + edgeCounter);


        for (Map.Entry<Integer, ArrayList<double[]>> nodeEntry : fwdCore.entrySet()) {
            edgeReduce(nodeEntry.getKey());
        }

        for (Map.Entry<Integer, ArrayList<double[]>> nodeEntry : bckCore.entrySet()) {
            edgeReduce(nodeEntry.getKey());
        }

        edgeCounter = 0;

        for (Map.Entry<Integer, ArrayList<double[]>> nodeEntry : fwdCore.entrySet()) {
            edgeCounter += nodeEntry.getValue().size();
        }

        System.out.println("Edges after:    " + edgeCounter);


        System.out.println("Final size: " + fwdCore.size() + " " + bckCore.size());
        System.out.println("Full size: " + fwdGraph.size() + " " + bckGraph.size());
    }

    private Double checkKey(int node){

        ArrayList<double[]> beforeEdges = bckCore.get(node);
        ArrayList<double[]> afterEdges = fwdCore.get(node);


        Double cDegInDegOut;

        Double shortcutCount = 0.0;
        Double maxHop = 0.0;
        if(afterEdges != null && beforeEdges != null) {
//            System.out.println(afterEdges.size() + " * " + beforeEdges.size());
            for(double[] afterEdge : afterEdges) {
                for (double[] beforeEdge : beforeEdges) {
//                    if(!connectedInCore((long) beforeEdge[0], (long) afterEdge[0])){
//                        if (beforeEdge[0] != afterEdge[0]) {
                            shortcutCount++;
//                        }
//                    }
                    if(node == Long.parseLong("2322558864")) {
                        System.out.println(beforeEdge[3] + " ... " + afterEdge[3]);
                    }
                    if (beforeEdge[3] + afterEdge[3] > maxHop) {
                        maxHop = beforeEdge[3] + afterEdge[3];
                    }
                }
            }
            cDegInDegOut = contractionParameter * (beforeEdges.size() + afterEdges.size());

            if(node == Long.parseLong("2322558864")){
                System.out.println(beforeEdges.size() + " " + afterEdges.size() + " " + shortcutCount + " " + maxHop);
            }

        } else {
            return Double.MAX_VALUE;
        }

//        System.out.println(shortcutCount + " <= " + contractionParameter + " x (" + (beforeEdges.size() + " + " + afterEdges.size() + ")"));

        if(shortcutCount > 0){
            if(shortcutCount <= cDegInDegOut){
                if(maxHop < hopLimiter){
                    return maxHop * shortcutCount / (beforeEdges.size() + afterEdges.size());
                }
            }
        }
        return Double.MAX_VALUE;
    }

    private void vertexHeapAdd(int node, PriorityQueue heap){

//        if(keys.containsKey(node)){
//            startTime = System.nanoTime();
//            heap.remove(new Pair<>(node, keys.get(node)));
//            endTime = System.nanoTime();
//        }
//        System.out.println("Check " + node);
//        System.out.println(heap.size());

        ArrayList<double[]> beforeEdges = bckCore.get(node);
        ArrayList<double[]> afterEdges = fwdCore.get(node);

        Double shortcutCount = 0.0;
        Double maxHop = 0.0;

//        System.out.println(node);
//
//

//        System.out.println("node: " + node);

        if(afterEdges != null && beforeEdges != null) {
//            System.out.println(afterEdges.size() + " * " + beforeEdges.size());
            for(double[] afterEdge : afterEdges) {
                if(afterEdge[0] == Long.parseLong("2322558864")){
                    System.out.println("Touched it at " + node);
                }
                for (double[] beforeEdge : beforeEdges) {
                    if(beforeEdge[0] == Long.parseLong("2322558864")){
                        System.out.println("Touched it at " + node);
                    }
                    if(node == Long.parseLong("2322558864")) {
                        System.out.println(beforeEdge[3] + " " + afterEdge[3]);
                    }
//                    if(!connectedInCore((long) beforeEdge[0], (long) afterEdge[0])){
//                        if (beforeEdge[0] != afterEdge[0]) {
                            shortcutCount++;
//                        }
//                    }
                    if (beforeEdge[3] + afterEdge[3] > maxHop) {
                        maxHop = beforeEdge[3] + afterEdge[3];
                    }
                }
            }
        }
//        System.out.println("this loop");

//        System.out.println("ShortcutCount: " + shortcutCount);

        if(node == Long.parseLong("2322558864")){
            System.out.println(beforeEdges.size() + " " + afterEdges.size() + " " + shortcutCount + " " + maxHop);
        }

        if(shortcutCount > 0){

            Double cDegInDegOut = contractionParameter * (beforeEdges.size() + afterEdges.size());

//            System.out.println("cDegInDegOut: " + cDegInDegOut);

//            System.out.println(shortcutCount + " compared to " + cDegInDegOut);

            if(shortcutCount <= cDegInDegOut){
                if(maxHop < hopLimiter){
//                    long startTime = System.nanoTime();
                    Double key = maxHop * (shortcutCount / (beforeEdges.size() + afterEdges.size()));
                    heap.add(new Pair<>(node, key));
                    keys.put(node, key);
                    if(node == Long.parseLong("2322558864")){
                        System.out.println("Adding key: " + key);
                    }
//                    long endTime = System.nanoTime();
//                    totalHeapTime += (((float) endTime - (float) startTime) / 100000000);
                } else {
//                            System.out.println("No; " + maxHop + " >= " + hopLimiter);
                    keys.remove(node);
                }
            } else {
                keys.remove(node);
//                        System.out.println("No; " + shortcutCount + " > " + cDegInDegOut);
            }
        }  else {
            keys.remove(node);
//                    System.out.println("No; " + shortcutCount + " = 0");
        }
    }

    private void edgeReduce(int node){
//        System.out.println("REDUCE " + node);
        ArrayList<Integer> neighbours = new ArrayList<>();
        ArrayList<Integer> neighboursFound = new ArrayList<>();
        HashMap<Integer, Double> distTo = new HashMap<>();
        HashMap<Integer, Integer> edgeTo = new HashMap<>();
        for(double[] neighbour : fwdCore.get(node)) {
            neighbours.add((int) neighbour[0]);
            neighboursFound.add((int) neighbour[0]);
        }

        Comparator<DijkstraEntry> comparator = new DistanceComparator();
        PriorityQueue<DijkstraEntry> queue = new PriorityQueue<>(comparator);
        distTo.put(node, 0.0);
        queue.add(new DijkstraEntry(node, 0));
        while(!neighboursFound.isEmpty()){
//            System.out.println(neighboursFound);
//            System.out.println(queue.size());
            int v = queue.poll().getNode();
            Iterator foundIt = neighboursFound.iterator();
            while(foundIt.hasNext()){
                if((Integer) foundIt.next() == v){
                    foundIt.remove();
                }
            }

            if(fwdCore.containsKey(v)){
                for(double[] edge : fwdCore.get(v)){
                    int w = (int) edge[0];
                    double weight = edge[1];
//                System.out.println("EDGE TO " + w + " of length " + weight);
                    double distToV = distTo.get(v);
                    if (!distTo.containsKey(w) || distTo.get(w) > (distToV + weight)){
                        distTo.put(w, distToV + weight);
                        edgeTo.put(w, v); //should be 'nodeBefore'
                        queue.add(new DijkstraEntry(w, distToV + weight)); //inefficient?
                    }
                }
            }
        }

        Iterator neighbourIt = fwdCore.get(node).iterator();

        while(neighbourIt.hasNext()){
            double[] edge = (double[]) neighbourIt.next();
            int neighbour = (int) edge[0];
            if(neighbour != node) {
                if (!edgeTo.get(neighbour).equals(node)) {
                    int prev = neighbour;
                    do{
                        prev = edgeTo.get(prev);
//                        System.out.println(prev);
                    } while (prev != node);
                    neighbourIt.remove();
//                    System.out.println("Removed.");
                }
            }
        }

//        System.out.println(fwdCore.get(node).size());

    }

    private long joinWays(long fst, long snd){
        long id = generateNewWayRef();
//        System.out.println(Arrays.toString(mapRoads.get(fst)));
//        System.out.println(Arrays.toString(mapRoads.get(snd)));
//        System.out.println();
        int[] first = mapRoads.get(fst);
        int[] second = mapRoads.get(snd);

        if(first == null){
            first = coreMapRoads.get(fst);
        }
        if(second == null){
            second = coreMapRoads.get(snd);
        }

        if(first[first.length - 1] == second[0]){
            coreMapRoads.put(id, concat(first, second));
        } else {
            coreMapRoads.put(id, concat(second, first));
        }


        return id;
    }


    private void makeTree(){

        long median = 0;
        long sort = 0;
        long insert = 0;
        long remove = 0;

        timerStart();
        tree = new Tree(120);
        ArrayList<Integer> nodes = new ArrayList<>();
        for(int i = 0; i < fwdGraph.size(); i++){
            nodes.add(i);
        }
        System.out.println("done");
        Random rand = new Random();
        boolean vertical = true;
        int counter = 0;
        int sizeStart = nodes.size();
        int size = sizeStart;
        ArrayList<Pair<Integer, Integer>> medians = new ArrayList<>();
        TREE: while(!nodes.isEmpty()){
            if(size < 21){
                for(int n : nodes){
                    tree.insert(n, dictionary.get(graphNodeMappings.get(n)));
                    break TREE;
                }
            } else {
//                System.out.println();
                if((counter % 1000) == 0){
                    System.out.println(((double) counter / (double) sizeStart) * 100);
                    System.out.println();
                    System.out.println("Median  " + median);
                    System.out.println("Sort    " + sort);
                    System.out.println("Insert  " + insert);
                    System.out.println("Remove  " + remove);
                }
//            System.out.println(nodes.size());
                timerStart();
                for(int i = 0; i <= 20; i++){
//                    System.out.println(size);
                    int r = rand.nextInt((size--));
                    medians.add(new Pair<>(nodes.remove(r), r));
                }
                median = timerEnd(median);
                timerStart();
                if(vertical){
                    Collections.sort(medians, new SortByLat());

                } else {
                    Collections.sort(medians, new SortByLong());
                }
                sort = timerEnd(sort);
                vertical = !vertical;
                timerStart();
                tree.insert(medians.get(10).getKey(), dictionary.get(graphNodeMappings.get(medians.get(10).getKey())));
//                System.out.println(dictionary.get(medians.get(10).getKey())[0] + " " + dictionary.get(medians.get(10).getKey())[1]);
                for(int x = 1; x < 5; x++){
                    tree.insert(medians.get(10 + x).getKey(), dictionary.get(graphNodeMappings.get(medians.get(10 + x).getKey())));
                    tree.insert(medians.get(10 - x).getKey(), dictionary.get(graphNodeMappings.get(medians.get(10 - x).getKey())));
                }
                insert = timerEnd(insert);
//            System.out.println(medians.get(5).getValue());
//            timerStart();
//            nodes.remove((int) medians.get(2).getValue());
//            remove = timerEnd(remove);
//            size--;
                medians.clear();
//            System.out.println(medians.size());
                counter = counter + 21;
            }
        }
        timerEnd("Making tree");
    }

    public void makeCoreTree(){

        long median = 0;
        long sort = 0;
        long insert = 0;
        long remove = 0;

        timerStart();
        tree = new Tree(120);
        ArrayList<Integer> nodes = new ArrayList<>();
        for(int i : fwdCore.keySet()){
            nodes.add(i);
        }
        System.out.println("done");
        Random rand = new Random();
        boolean vertical = true;
        int counter = 0;
        int sizeStart = nodes.size();
        int size = sizeStart;
        ArrayList<Pair<Integer, Integer>> medians = new ArrayList<>();
        TREE: while(!nodes.isEmpty()){
            if(size < 21){
                for(int n : nodes){
                    tree.insert(n, dictionary.get(graphNodeMappings.get(n)));
                    break TREE;
                }
            } else {
//                System.out.println();
                if((counter % 1000) == 0){
                    System.out.println(((double) counter / (double) sizeStart) * 100);
                    System.out.println();
                    System.out.println("Median  " + median);
                    System.out.println("Sort    " + sort);
                    System.out.println("Insert  " + insert);
                    System.out.println("Remove  " + remove);
                }
//            System.out.println(nodes.size());
                timerStart();
                for(int i = 0; i <= 20; i++){
//                    System.out.println(size);
                    int r = rand.nextInt((size--));
                    medians.add(new Pair<>(nodes.remove(r), r));
                }
                median = timerEnd(median);
                timerStart();
                if(vertical){
                    Collections.sort(medians, new SortByLat());

                } else {
                    Collections.sort(medians, new SortByLong());
                }
                sort = timerEnd(sort);
                vertical = !vertical;
                timerStart();
                tree.insert(medians.get(10).getKey(), dictionary.get(graphNodeMappings.get(medians.get(10).getKey())));
//                System.out.println(dictionary.get(medians.get(10).getKey())[0] + " " + dictionary.get(medians.get(10).getKey())[1]);
                for(int x = 1; x < 5; x++){
                    tree.insert(medians.get(10 + x).getKey(), dictionary.get(graphNodeMappings.get(medians.get(10 + x).getKey())));
                    tree.insert(medians.get(10 - x).getKey(), dictionary.get(graphNodeMappings.get(medians.get(10 - x).getKey())));
                }
                insert = timerEnd(insert);
//            System.out.println(medians.get(5).getValue());
//            timerStart();
//            nodes.remove((int) medians.get(2).getValue());
//            remove = timerEnd(remove);
//            size--;
                medians.clear();
//            System.out.println(medians.size());
                counter = counter + 21;
            }
        }
        timerEnd("Making tree");
    }

    private double lengthOfEdge(int[] edge){
        double length = 0;
        double[] nodeA = dictionary.get(edge[0]);
        for(int node : edge){
            double[] nodeB = dictionary.get(node);
            length = length + haversineDistance(nodeA, nodeB);
            nodeA = nodeB;
        }
        return length;
    }

    //Credit to https://medium.com/allthingsdata/java-implementation-of-haversine-formula-for-distance-calculation-between-two-points-a3af9562ff1
    public static double haversineDistance(double[] nodeA, double[] nodeB){
        double rad = 6371000; //radius of earth in metres
        double aLatRadians = Math.toRadians(nodeA[1]); //0 = latitude, 1 = longitude
        double bLatRadians = Math.toRadians(nodeB[1]);
        double deltaLatRadians = Math.toRadians(nodeB[1] - nodeA[1]);
        double deltaLongRadians = Math.toRadians(nodeB[0] - nodeA[0]);

        double x = Math.sin(deltaLatRadians/2) * Math.sin(deltaLatRadians/2) +
                Math.cos(aLatRadians) * Math.cos(bLatRadians) *
                        Math.sin(deltaLongRadians/2) * Math.sin(deltaLongRadians/2);
        double y = 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1-x));
        return rad * y;

//        return Math.sqrt(Math.pow((nodeB.getLati() - nodeA.getLati()), 2) + Math.pow((nodeB.getLongi() - nodeA.getLongi()), 2));
    }

    public static class OSMBinaryParser extends BinaryParser {

        @Override
        protected void parseRelations(List<Relation> rels) {
//            System.out.println("Parsing relations.");
//            if (!rels.isEmpty()) {
//                System.out.println("Got some relations to parse.");
//
//                for (Relation r : rels){
//                    for (int i = 0; i < r.getKeysCount(); i++) {
//                        System.out.println(getStringById(r.getKeys(i)) + " " + getStringById(r.getVals(i)));
//                    }
//                    System.out.println();
//                    long lastRef = 0;
//                    for (int i = 0; i < r.getMemidsCount(); i++) { //SMALLER THAN OR EQUAL TO OR NOT?
//                        System.out.println(lastRef += (r.getMemids(i)));
//                        if (getStringById(r.getRolesSid(i)).equals("inner"))
//                    }
//                    for (int i = 0; i < r.getRolesSidCount(); i++) {
//                        System.out.println(getStringById(r.getRolesSid(i)));
//                    }
//                    System.out.println(r.getRolesSidList());
//                    System.out.println();
//            }
//
//            Relation r = null;
//
//
//            }
        }

        @Override
        protected void parseDense(DenseNodes nodes) {
            if(parsingNodes){
//                System.out.println("Parsing dense nodes.");
                long lastId=0;
                long lastLat=0;
                long lastLon=0;

                for (int i=0 ; i<nodes.getIdCount() ; i++) {
                    lastId += nodes.getId(i);
                    lastLat += nodes.getLat(i);
                    lastLon += nodes.getLon(i);
                    if(nodeLong2Int.containsKey(lastId)){
//                    System.out.printf("Dense node, ID %d @ %.6f,%.6f\n",
//                        lastId,parseLat(lastLat),parseLon(lastLon));
                        double[] tempDense = new double[3];
//                        MyNode tempDense = new MyNode();
//                        tempDense.setLati(parseLat(lastLat));
//                        tempDense.setLongi(parseLon(lastLon));
//                        tempDense.setNodeId(lastId);
                        tempDense[0] = parseLon(lastLon);
                        tempDense[1] = parseLat(lastLat);
                        tempDense[2] = dictionary.size();
                        dictionary.set(nodeLong2Int.get(lastId), tempDense);
//                        System.out.println(dictionary.get(nodeLong2Int.get(lastId))[0] + " " + dictionary.get(nodeLong2Int.get(lastId))[1]);
//                        System.out.println(lastId + " " + tempDense[0] + " " + tempDense[1]);
//                        System.out.println(tempDense[0] + " " + tempDense[1]);
//                        counter++;
//                        System.out.println(counter)
                    }
                }
            }
        }

        @Override
        protected void parseNodes(List<Node> nodes) {
//            System.out.println("Parsing nodes.");
            for (Node n : nodes) {
                System.out.printf("Regular node, ID %d @ %.6f,%.6f\n",
                        n.getId(),parseLat(n.getLat()),parseLon(n.getLon()));
            }
        }

        @Override
        protected void parseWays(List<Way> ways) {
            if(!parsingNodes){
                long lastRef = 0;
                for (Way w : ways) {
                    lastRef += w.getId();
                    String key;
                    String value;
                    boolean oneWay = false;
                    ONEWAY: for (int i=0 ; i<w.getKeysCount() ; i++) {
                        key = getStringById(w.getKeys(i));
                        value = getStringById(w.getVals(i));
                        if (key.equals("oneway") && value.equals("yes")) {
                            oneWay = true;
                            break ONEWAY;
                        }
                    }
                    for (int i=0 ; i<w.getKeysCount() ; i++) {
                        key = getStringById(w.getKeys(i));
                        value = getStringById(w.getVals(i));
                        if(value.equals("cycleway") ||
                                (key.equals("route") && value.equals("bicycle"))
                                || (key.equals("bicycle") && value.equals("yes"))){
                            addWay(w, oneWay, lastRef);
                        } else if(key.equals("highway")){
                            if (value.matches("primary|primary_link")){
                                addWay(w, oneWay, lastRef);
                            } else if (value.matches("secondary|secondary_link")){
                                addWay(w, oneWay, lastRef);
                            } else if (value.matches("tertiary|unclassified|residential|service|tertiary_link|road|cycleway")) {
                                addWay(w, oneWay, lastRef);
                            }
//                            } else if(value.matches("motorway|motorway_link")){
//                                addWay(w, oneWay, lastRef);
//                            } else if (value.matches("trunk|trunk_link")) {
//                                addWay(w, oneWay, lastRef);
//                            }
                        }
                    }
                }
            }
        }

        private void addWay(Way w, boolean oneWay, long wayId){
            int[] fwdWay = new int[w.getRefsList().size()];
            int[] bckWay = new int[w.getRefsList().size()];
            long lastRef = 0;
            int fwdCtr = 0;
            int intRef;
            int bckCtr = w.getRefsCount() - 1;
            for (Long ref : w.getRefsList()) {
                lastRef+= ref;
                if(nodeLong2Int.containsKey(lastRef)){
                    intRef = nodeLong2Int.get(lastRef);
                }else{
                    intRef = nodeLong2Int.size();
                    nodeLong2Int.put(lastRef, intRef);
                    nodeInt2Long.put(intRef, lastRef);
                    dictionary.add(new double[]{});
                }
                fwdWay[fwdCtr] = intRef;
                if(!oneWay){
                    bckWay[bckCtr] = intRef;
                }
                int wayCount;
                if(allWayNodes.get(intRef) != null){
                    wayCount = allWayNodes.get(intRef);
                } else {
                    wayCount = 0;
                }
                allWayNodes.put(intRef, wayCount + 1);
                fwdCtr++;
                bckCtr--;
            }

            if(oneWay){
                mapRoads.put(wayId, fwdWay);
            }else{
                mapRoads.put(wayId, fwdWay);
                mapRoads.put(generateNewWayRef(), bckWay);
            }
        }

        @Override
        protected void parse(HeaderBlock header) {
            System.out.println("Got header block.");
        }

        public void complete() {
            System.out.println("Complete!");
        }
    }

    private void splitWays(Map<Long, int[]> ways, boolean strip){
        for(Map.Entry<Long, int[]> w : ways.entrySet()){
            splitWay(w.getKey(), w.getValue(), strip);
        }
    }

    private void splitWay(Long id, int[] nodes, boolean strip){
        for(int i = 1; i < (nodes.length - 1); i++){
            if(allWayNodes.get(nodes[i]) > 1){
                if(nodes[i] == (Long.parseLong("2310931045"))){
                    System.out.println("Huh");
                }
                long restOfWayId = generateNewWayRef();
                int[] headerWayNodes = Arrays.copyOfRange(nodes, 0, i + 1);
                mapRoads.put(id, headerWayNodes);
                splitWay(restOfWayId, Arrays.copyOfRange(nodes, i, nodes.length), strip);
                return;
            }
        }
        mapRoads.put(id, nodes);
    }

    public ArrayList<ArrayList<double[]>> getFwdGraph() {
        return fwdGraph;
    }

    public ArrayList<ArrayList<double[]>> getBckGraph() {
        return bckGraph;
    }

    public ArrayList<double[]> fwdAdj(int v){
        ArrayList x = fwdGraph.get(v);
        if(fwdGraph.get(v) != null){
            return x;
        } else {
            return new ArrayList<>();
        }
    }

    public ArrayList<double[]> bckAdj(int v){
        ArrayList x = bckGraph.get(v);
        if(bckGraph.get(v) != null){
            return x;
        } else {
            return new ArrayList<>();
        }
    }

    public Map<Integer, ArrayList<double[]>> getFwdCore() {
        return fwdCore;
    }

    public Map<Integer, ArrayList<double[]>> getBckCore() {
        return bckCore;
    }

    public ArrayList<double[]> fwdCoreAdj(int v){
        ArrayList x = fwdCore.get(v);
        if(fwdCore.get(v) != null){
            return x;
        } else {
            return new ArrayList<>();
        }
    }

    public ArrayList<double[]> bckCoreAdj(int v){
        ArrayList x = bckCore.get(v);
        if(bckCore.get(v) != null){
            return x;
        } else {
            return new ArrayList<>();
        }
    }

    public boolean isCoreNode(int id){
        return(fwdCore.containsKey(id) || bckCore.containsKey(id));
    }

    public ArrayList<Point2D.Double> refsToNodes(ArrayList<Integer> refs){
        ArrayList<Point2D.Double> nodes = new ArrayList<>();
        for(int ref : refs){
            Integer mapRef = graphNodeMappings.get(ref);
            nodes.add(new Point2D.Double(dictionary.get(mapRef)[0], dictionary.get(mapRef)[1]));
        }
        return nodes;
    }

    public ArrayList<Point2D.Double> wayListToNodes(ArrayList<Long> wayList){
        ArrayList<Point2D.Double> points = new ArrayList<>();
        ArrayList<int[]> allIDs = new ArrayList();
        for(Long wayId : wayList) {
            Thread.yield();
            if(coreMapRoads.containsKey(wayId)){
                allIDs.add(coreMapRoads.get(wayId));
            } else {
                allIDs.add(mapRoads.get(wayId));
            }
        }

        for(int[] ids : allIDs){
            Thread.yield();
            for(int i : ids){
                double[] point = dictionary.get(i);
                points.add(new Point2D.Double(point[0], point[1]));
            }
        }

        return points;
    }

    public double[] getGraphNodeLocation(int node){
        return dictionary.get(graphNodeMappings.get(node));
    }

    private void timerStart(){
        startTime = System.nanoTime();
    }

    private void timerEnd(String string){
        endTime = System.nanoTime();
        System.out.println(string + " time: " + (((float) endTime - (float)startTime) / 1000000000));
    }

    private long timerEnd(long timer){
        endTime = System.nanoTime();
        return timer + (endTime - startTime);
    }

    public ArrayList<double[]> getDictionary(){
        return dictionary;
    }

    public static long generateNewWayRef(){
        boolean check;
        Random rand = new Random(); //time it with the id as seed, and with no seed!
        Long newWayRef;
        do {
            newWayRef = Math.abs(rand.nextLong());
            check = ((long) (newWayRef.doubleValue()) != newWayRef) || mapRoads.containsKey(newWayRef) || coreMapRoads.containsKey(newWayRef);
        } while(check);
        return newWayRef;
    }

    public int findClosest(double[] loc){
        return tree.nearest(loc, dictionary).getKey();
    }

    class SortByLong implements Comparator<Pair<Integer, Integer>>
    {
        public int compare(Pair<Integer, Integer> a, Pair<Integer, Integer> b)
        {
            return (int) (dictionary.get(a.getKey())[0] - dictionary.get(b.getKey())[0]);
        }
    }

    class SortByLat implements Comparator<Pair<Integer, Integer>>
    {
        public int compare(Pair<Integer, Integer> a, Pair<Integer, Integer> b)
        {
            return (int) (dictionary.get(a.getKey())[1] - dictionary.get(b.getKey())[1]);
        }
    }

    public String getFilePrefix(){
        return filePrefix;
    }

    public String getRegion(){
        return region;
    }

    public class KeyComparator implements Comparator<Pair<Integer, Double>>{
        public int compare(Pair<Integer, Double> x, Pair<Integer, Double> y){
            if(x.getValue() < y.getValue()){
                return -1;
            }
            if(x.getValue() > y.getValue()){
                return 1;
            }
            else return 0;
        }
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

    public static int[] concat(int[] first, int[] second) {
        int[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    } //from stackoverflow: https://stackoverflow.com/a/784842/3032936

    private boolean connectedInCore(long a, long b){
        try{
            for(double[] edge : fwdCore.get(a)){
                if((long) edge[0] == b){
                    return true;
                }
            }

            for(double[] edge : fwdCore.get(b)){
                if((long) edge[0] == a){
                    return true;
                }
            }
        } catch(NullPointerException n){}
        return false;

    }

    private boolean doubleEquals(Double a, Double b){
        return (Math.abs(a - b) < DOU_THRESHOLD);
    }
}
