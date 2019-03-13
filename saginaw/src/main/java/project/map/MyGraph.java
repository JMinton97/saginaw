package project.map;

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

public class MyGraph {
    //    public static ArrayList<MyNode> mapNodes = new ArrayList<MyNode>();
//    public static BTreeMap<Long, double[]> dictionary; //maps a node id to a double array containing the coordinates of the node
    public static ArrayList<double[]> dictionary;
    private static Map<Long, int[]> mapRoads; //a list of all connections between nodes. Later becomes all graph edges.
    private static Map<Long, int[]> coreMapRoads; //a list of all connections between nodes, in the core.
    private static ConcurrentMap<Integer, Integer> allWayNodes; //maps the nodes contained in the extracted ways to a counter of the number of ways each one is part of
    private static boolean parsingNodes;
//    private static HashSet<Long> junctions;
    private static Map<Integer, ArrayList<double[]>> fwdGraph;
    private static Map<Integer, ArrayList<double[]>> bckGraph;
    private static Map<Integer, ArrayList<double[]>> fwdCore;
    private static Map<Integer, ArrayList<double[]>> bckCore;
    private static HashMap<Integer, Double> keys = new HashMap<>();
    private Pair<Map, Map> graph;
    private Tree tree;
    private int bypassNode;
    private double contractionParameter = 2.5;
    private double hopLimiter = 50;
    private final double DOU_THRESHOLD = .0001;
    private static HashMap<Long, Integer> nodeLong2Int;
    private static HashMap<Integer, Long> nodeInt2Long;
    private int maxId = 0;

    private long startTime;
    private long endTime;

    private float totalMapDBTime;

    private String filePrefix, region;

    Set<long[]> edges = new HashSet<>();
    int noOfEdges;

    public MyGraph(File file, String region) throws IOException {

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

        makeDictionary(file);

//        File fwdGraphDir = new File(filePrefix.concat("fwdGraph.ser"));
//        File bckGraphDir = new File(filePrefix.concat("bckGraph.ser"));
        File fwdGraphDir = new File(filePrefix.concat("fwdGraph.ser"));
        File bckGraphDir = new File(filePrefix.concat("bckGraph.ser"));
        File treeDir = new File(filePrefix.concat("tree.ser"));
        File fwdCoreDir = new File(filePrefix.concat("fwdCore.ser"));
        File bckCoreDir = new File(filePrefix.concat("bckCore.ser"));
        if(fwdGraphDir.exists()){
            System.out.println("Found graph.");
            timerStart();
            FileInputStream fileIn = new FileInputStream(fwdGraphDir);
            FSTObjectInput objectIn = new FSTObjectInput(fileIn);
            try {
                fwdGraph = (Map<Integer, ArrayList<double[]>>) objectIn.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            fileIn = new FileInputStream(bckGraphDir);
            objectIn = new FSTObjectInput(fileIn);
            try {
                bckGraph = (Map<Integer, ArrayList<double[]>>) objectIn.readObject();
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
                    System.out.println("AHAFIUHDSFIUHSDFIUHDF");
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

    private Pair<Map, Map> makeDijkstraGraph(Map<Long, int[]> edges, int noOfEdges){
        fwdGraph = new HashMap<>(noOfEdges);
        bckGraph = new HashMap<>(noOfEdges);

        timerStart();
        System.out.println("Adding connections");
        int counter = 0;
        for(Map.Entry<Long, int[]> way : edges.entrySet()){ //iterate through every edge and add neighbours to graph vertices accordingly
//            System.out.println(nodeInt2Long.get(way.getValue()[0]) + " to " + nodeInt2Long.get(way.getValue()[way.getValue().length - 1]) + " by " + way.getKey());
            counter++;
            if((counter % 200000) == 0){
                System.out.println(((double) counter / (double) noOfEdges) * 100);
            }
//            System.out.println(way.getWayId());

            int[] wayNodes = way.getValue();
            if(wayNodes.length > 1){
                int fstVert = wayNodes[0];
                int lstVert = wayNodes[wayNodes.length - 1]; //could be .get(1) if we've stripped the ways
                if(fstVert != lstVert){

                    if(!fwdGraph.containsKey(fstVert)){
                        fwdGraph.put(fstVert, new ArrayList<>()); //because cul-de-sacs don't count as junctions so haven't been added yet.
                    }
                    if(!bckGraph.containsKey(lstVert)){
                        bckGraph.put(lstVert, new ArrayList<>()); //because cul-de-sacs don't count as junctions so haven't been added yet.
                    }
                    double length = lengthOfEdge(wayNodes);
//                    double length = 0;
                    fwdGraph.get(fstVert).add(new double[]{(double) lstVert, length, way.getKey().doubleValue(), 1}); //edge array stores target of edge, length of edge,
                    bckGraph.get(lstVert).add(new double[]{(double) fstVert, length, way.getKey().doubleValue(), 1}); //wayId of edge, and hop number (for contraction)

                    //check if the double[] list ever contains two edges going to same vertex

//                double[] xy = dictionary.get(fstVert);
//                System.out.println(xy[0] + " " + xy[1]);
//                if(!tree.contains(xy)){
//                    tree.insert(fstVert, xy);
//                }
                }
            }
        }
        timerEnd("Creating graph");
        return new Pair<>(fwdGraph, bckGraph);
    }

    private void contract() {

        Comparator<Pair<Integer, Double>> comp = new KeyComparator(); //maybe we can use
        PriorityQueue<Pair<Integer, Double>> heap = new PriorityQueue<>(comp);
        keys = new HashMap<>(fwdGraph.size());
        boolean stopFlag = true;

        fwdCore = fwdGraph.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> new ArrayList(e.getValue())));

        bckCore = bckGraph.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> new ArrayList(e.getValue())));

        System.out.println("Original size: " + fwdGraph.size() + " " + bckGraph.size());

        int edgeCounter = 0;
        for (Map.Entry<Integer, ArrayList<double[]>> nodeEntry : fwdGraph.entrySet()) {
            edgeCounter = nodeEntry.getValue().size() + edgeCounter;
        }

        int originalSize = fwdGraph.size();

        System.out.println("Original edge number: " + edgeCounter);

        System.out.println("Contract size: " + fwdCore.size());

        edgeCounter = 0;

        int i = 0;

        for (Map.Entry<Integer, ArrayList<double[]>> nodeEntry : fwdCore.entrySet()) {

            i++;

//            if (i % 1000 == 0) {
//                System.out.println(i);
//            }

            edgeCounter = nodeEntry.getValue().size() + edgeCounter;
            int node = nodeEntry.getKey();
            vertexHeapAdd(node, heap);

        }

        System.out.println("Edge number: " + edgeCounter);

        i = 0;

        while (!heap.isEmpty()) {
            i++;
//            System.out.println(i);
            if (i % 500000 == 0) {
//                    System.out.println(i + " " + fwdCore.size() + " " + totalHeapTime + " " + totalNonHeapTime + " " + sdf.format(cal.getTime()));
                System.out.println(i + " " + (100 * ((float) fwdCore.size() / (float) originalSize)) + "%. Heap size " + heap.size());
            }
            Pair<Integer, Double> entry = heap.poll();

            if(keys.containsKey(entry.getKey())){
                if(!entry.getValue().equals(keys.get(entry.getKey()))){
//                        System.out.println(entry.getValue() + " " + keys.get(entry.getKey()));
                    continue;
                }
            } else {
                continue;
            }

//            System.out.println("Key is " + checkKey(Long.parseLong("2322558864")));

            bypassNode = entry.getKey();
//            System.out.println("    " + bypassNode);
//            System.out.println((long) fwdCore.get(bypassNode).get(0)[0] + " " + (long) bckCore.get(bypassNode).get(0)[0]);

            if(!doubleEquals(checkKey(entry.getKey()), entry.getValue())){
//                System.out.println("SOMETHING WRONG");
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
//            System.out.println(i + "   " + bypassNode);
//                System.out.println("Got key.");
//                System.out.println(bypassNode);
            if(fwdCore.containsKey(bypassNode)) {

//                ArrayList<double[]> beforeEdges = new ArrayList<>();
//                beforeEdges.addAll(bckCore.get(bypassNode));
//                ArrayList<double[]> afterEdges = new ArrayList<>();
//                afterEdges.addAll(fwdCore.get(bypassNode));
//
//                Iterator bckIt = bckCore.get(bypassNode).iterator();
//                while(bckIt.hasNext()){
//                    long backNeighbour = (long) ((double[]) bckIt.next())[0];
//                    for(double[] edge : fwdCore.get(backNeighbour)){
//                        if(edge[0] == bypassNode){
//                            fwdCore.get(backNeighbour).remove(edge);
//                        }
//                    }
//                }
//
//                Iterator fwdIt = fwdCore.get(bypassNode).iterator();
//                while(fwdIt.hasNext()){
//                    long forwardNeighbour = (long) ((double[]) fwdIt.next())[0];
//                    for(double[] edge : bckCore.get(forwardNeighbour)){
//                        if(edge[0] == bypassNode){
//                            bckCore.get(forwardNeighbour).remove(edge);
//                        }
//                    }
//                }
//
//                for(double[] afterEdge : afterEdges){
//                    for(double[] beforeEdge : beforeEdges){
//                        if(connectedInCore((long) beforeEdge[0], (long) afterEdge[0])){
//                            for(double[] existingEdges : fwdCore.get((long) beforeEdge[0])){
//                                if(existingEdges[0] == afterEdge[0]){
//
//                                }
//                            }
//                        }
//                        fwdCore.get((long) beforeEdge[0]).add(new double[]{afterEdge[0], afterEdge[1] + beforeEdge[1], beforeEdge[2], beforeEdge[3] + afterEdge[3]});
//                        bckCore.get((long) afterEdge[0]).add(new double[]{beforeEdge[0], afterEdge[1] + beforeEdge[1], beforeEdge[2], beforeEdge[3] + afterEdge[3]});
//                    }
//                }
















                ArrayList<double[]> beforeEdges = new ArrayList<>();
                beforeEdges.addAll(bckCore.get(bypassNode));
                ArrayList<double[]> afterEdges = new ArrayList<>();
                afterEdges.addAll(fwdCore.get(bypassNode));
//                System.out.println("Got edges.");
                Iterator beforeItr = beforeEdges.iterator();
//                System.out.println(beforeEdges.size() + " " + afterEdges.size());

//                System.out.println(checkKey(bypassNode));
                int addedEdgesCtr = 0;
                int notAddedAdgesCtr = 0;
                while (beforeItr.hasNext()) {
//                    System.out.println("before edge");
                    double[] beforeEdge = (double[]) beforeItr.next();
                    Iterator afterItr = afterEdges.iterator();
                    INNER: while (afterItr.hasNext()) {
//                        System.out.println("combo");
                        double[] afterEdge = (double[]) afterItr.next();
                        if ((int) afterEdge[0] == (long) beforeEdge[0]) {    //check we're not making an x to x edge
//                            System.out.println("No edge added.");
                            notAddedAdgesCtr++;
//                            System.out.println(bypassNode);
                            continue;
                        }
//                        ArrayList<double[]> existingEdges = new ArrayList<>();
//                        existingEdges.addAll(fwdCore.get((long) beforeEdge[0]));
////                        System.out.println("Got existing edges.");
//                        Iterator existingItr = existingEdges.iterator();
//                        while (existingItr.hasNext()) {           //checking if the shortcut to be added already exists
//                            double[] edge = (double[]) existingItr.next();
//                            if (edge[0] == afterEdge[0]) {
//                                if (beforeEdge[1] + afterEdge[1] < edge[1]) {
////                                    existingItr.add(new double[]{afterEdge[0], beforeEdge[1] + afterEdge[1], beforeEdge[2], beforeEdge[3] + afterEdge[3]});
//                                    fwdCore.get((long) beforeEdge[0]).add(new double[]{afterEdge[0], beforeEdge[1] + afterEdge[1], beforeEdge[2], beforeEdge[3] + afterEdge[3]});
//                                    bckCore.get((long) afterEdge[0]).add(new double[]{beforeEdge[0], beforeEdge[1] + afterEdge[1], beforeEdge[2], beforeEdge[3] + afterEdge[3]});   //if shortcut is shorter, add it
////                                    Iterator fwdIt = fwdCore.get((long) beforeEdge[0]).iterator();
////                                    Iterator bckIt = bckCore.get((long) afterEdge[0]).iterator();
////                                    while(fwdIt.hasNext()){
////                                        if((long) ((double[]) fwdIt.next())[0] == bypassNode){
////                                            fwdIt.remove();
////                                        }
////                                    }
////                                    while(fwdIt.hasNext()){
////                                        if((long) ((double[]) fwdIt.next())[0] == bypassNode){
////                                            bckIt.remove();
////                                        }
////                                    }
//                                    alteredNodes.add((long) afterEdge[0]);
//                                    alteredNodes.add((long) beforeEdge[0]);
//                                    if((long) afterEdge[0] == Long.parseLong("2322558864")){
//                                        System.out.println("Touched in loop at " + bypassNode);
//                                    }
//                                    if((long) beforeEdge[0] == Long.parseLong("2322558864")){
//                                        System.out.println("Touched in loop at " + bypassNode);
//                                    }
////                                    System.out.println("Added an edge.");
////                                    System.out.println("Added shortcuts.");
////                                    System.out.println("Added shorter edge from " + (long) beforeEdge[0] + " to " + (long) afterEdge[0] + " of length " + (beforeEdge[1] + afterEdge[1]));
//                                } else {
//                                    continue INNER;
//                                }
//                            }
//                        }
//                        Iterator fwdIt = fwdCore.get((long) beforeEdge[0]).iterator();
//                        Iterator bckIt = bckCore.get((long) afterEdge[0]).iterator();
                        long fwdId = joinWays((long) beforeEdge[2], (long) afterEdge[2]);
                        long bckId = joinWays((long) afterEdge[2], (long) beforeEdge[2]);
                        fwdCore.get((int) beforeEdge[0]).add(new double[]{afterEdge[0], beforeEdge[1] + afterEdge[1], fwdId, beforeEdge[3] + afterEdge[3]});   //add shortcut to forward and backward graph
                        bckCore.get((int) afterEdge[0]).add(new double[]{beforeEdge[0], beforeEdge[1] + afterEdge[1], bckId, beforeEdge[3] + afterEdge[3]});


//                        while(fwdIt.hasNext()){
//                            if((long) ((double[]) fwdIt.next())[0] == bypassNode){
//                                fwdIt.remove();
//                            }
//                        }
//                        while(bckIt.hasNext()){
//                            if((long) ((double[]) bckIt.next())[0] == bypassNode){
//                                bckIt.remove();
//                            }
//                        }
                        alteredNodes.add((int) afterEdge[0]);
                        alteredNodes.add((int) beforeEdge[0]);
                        addedEdgesCtr++;
//                        System.out.println("Added these shortcuts.");
//                        System.out.println("Added edge from " + (long) beforeEdge[0] + " to " + (long) afterEdge[0] + " of length " + (beforeEdge[1] + afterEdge[1]) + " and hop size " + (beforeEdge[3] + afterEdge[3]));
                    }
                }

//                System.out.println("Added " + addedEdgesCtr + ". Didn't add " + notAddedAdgesCtr + ".");

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
//                            System.out.println("removed edge.");
                        }
                    }
                }
//                System.out.println("Removed bckgraph.");

                for (double[] awayEdge : bckCore.get(bypassNode)) {
                    Iterator it = fwdCore.get((int) awayEdge[0]).iterator();
                    while (it.hasNext()) {
                        double[] returnEdge = (double[]) it.next();
                        if (returnEdge[0] == bypassNode) {
                            it.remove();
//                            System.out.println("removed edge.");
                        }
//                        System.out.println("Removed fwdgraph.");
                    }
                }

//                System.out.println("Before removes.");

                fwdCore.remove(bypassNode);
                bckCore.remove(bypassNode);
                keys.remove(bypassNode);
//                System.out.println();


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
//                    System.out.println(node);
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
//                    System.out.println(node);
                    vertexHeapAdd(node, heap);
                }
//                System.out.println();

//                System.out.println(alteredNodes);


//                for(long node : alteredNodes){
////                    System.out.println("heap add");
//                    vertexHeapAdd(node, heap);
//                }
//                System.out.println("Done heap add");
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

        edgeCounter = 0;

        for (Map.Entry<Integer, ArrayList<double[]>> nodeEntry : fwdCore.entrySet()) {
            edgeCounter += nodeEntry.getValue().size();
        }

        System.out.println("Edges after:    " + edgeCounter);


        System.out.println("Final size: " + fwdCore.size() + " " + bckCore.size());
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
//            System.out.println(v);
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
        nodes.addAll(fwdGraph.keySet());
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
                    tree.insert(n, dictionary.get(n));
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
                tree.insert(medians.get(10).getKey(), dictionary.get(medians.get(10).getKey()));
//                System.out.println(dictionary.get(medians.get(10).getKey())[0] + " " + dictionary.get(medians.get(10).getKey())[1]);
                for(int x = 1; x < 5; x++){
                    tree.insert(medians.get(10 + x).getKey(), dictionary.get(medians.get(10 + x).getKey()));
                    tree.insert(medians.get(10 - x).getKey(), dictionary.get(medians.get(10 - x).getKey()));
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

//    public void print(){ //print graph contents
//        for(long vert : graph.keySet()){
//            Set<double[]> neighbours = graph.get(vert);
//            ArrayList<Long> neighbourNodes = new ArrayList<>();
//            for(double[] neighbour : neighbours){
//                neighbourNodes.add((long) neighbour[0]);
//            }
////            System.out.println("Node: " + vert + " Neighbours: " + neighbourNodes.toString());
//        }
//
//        System.out.println(graph.size());
//    }

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
//            System.out.println("Parsing way");
            if(!parsingNodes){
//                System.out.println("Parsing ways.");
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
                                (key.equals("route") && value.equals("bicycle"))){
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
//            System.out.println(Arrays.toString(fwdWay));
//            System.out.println(Arrays.toString(bckWay));

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
//        DB db3 = DBMaker
//                .fileDB("files//edges.db")
//                .fileMmapEnable()
//                .checksumHeaderBypass()
//                .closeOnJvmShutdown()
//                .make();
//
//        Map<Long, long[]> edges = db3.treeMap("set", Serializer.LONG, Serializer.LONG_ARRAY).createOrOpen();

//        if(edges.isEmpty()){
            for(Map.Entry<Long, int[]> w : ways.entrySet()){
                splitWay(w.getKey(), w.getValue(), strip);
            }
//        } else {
//            System.out.println("edges found; skipping split");
//        }
    }

    private long[] stripWay(long[] way){
        long[] newNodes = new long[2];
        newNodes[0] = way[0];
        newNodes[1] = way[way.length - 1];
        return way;
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

    public Map<Integer, ArrayList<double[]>> getFwdGraph() {
        return fwdGraph;
    }

    public Map<Integer, ArrayList<double[]>> getBckGraph() {
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
            nodes.add(new Point2D.Double(dictionary.get(ref)[0], dictionary.get(ref)[1]));
        }
        return nodes;
    }

    public ArrayList<Integer> wayToRefs(long wayId){
        ArrayList<Integer> returnNodes = new ArrayList<>();
//        System.out.println(mapRoads.containsKey(wayId));
        int[] nodes = mapRoads.get(wayId);
        for(int i = 0; i < nodes.length; i++){
            returnNodes.add(nodes[i]);
        }
        return returnNodes;
    }


    public ArrayList<Point2D.Double> wayToNodes(long wayId){
        ArrayList<Point2D.Double> points = new ArrayList<>();
        int[] ids = mapRoads.get(wayId);
        for(int i = 0; i < ids.length; i++){
            double[] point = dictionary.get(ids[i]);
            points.add(new Point2D.Double(point[0], point[1]));
        }
        return points;
    }

    public ArrayList<Point2D.Double> wayListToNodes(ArrayList<Long> wayList){
        ArrayList<Point2D.Double> points = new ArrayList<>();
        totalMapDBTime = 0;
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
//
//
//
//            startTime = System.nanoTime();
//            int[] ids = mapRoads.get(wayId);
//            endTime = System.nanoTime();
//            totalMapDBTime += (((float) endTime - (float) startTime) / 1000000000);
//
//
//        }
//        System.out.println("MapDB time: " + totalMapDBTime);
        return points;
    }

    public ArrayList<Point2D.Double> wayListToFirstNodes(ArrayList<Long> wayList){
        ArrayList<Point2D.Double> points = new ArrayList<>();
        for(Long wayId : wayList){
            int[] ids = mapRoads.get(wayId);
            for(int i = 0; i < 1; i++){
                double[] point = dictionary.get(ids[i]);
                points.add(new Point2D.Double(point[0], point[1]));
            }
        }
        return points;
    }

    public ArrayList<Point2D.Double> wayToFirstNodes(long wayId){
        ArrayList<Point2D.Double> points = new ArrayList<>();
        int[] ids = mapRoads.get(wayId);
        for(int i = 0; i < 1; i++){
            double[] point = dictionary.get(ids[i]);
            points.add(new Point2D.Double(point[0], point[1]));
        }
        return points;
    }

    public ArrayList<Long> nodesToRefs(ArrayList<project.map.MyNode> nodes){
        ArrayList<Long> refs = new ArrayList<>();
        for(project.map.MyNode node : nodes){
            refs.add(node.getNodeId());
        }
        return refs;
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
        Long restOfWayId;
        do {
            restOfWayId = Math.abs(rand.nextLong());
            check = ((long) (restOfWayId.doubleValue()) != restOfWayId) || mapRoads.containsKey(restOfWayId);
        } while(check);
        return restOfWayId;
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
