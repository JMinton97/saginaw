package project.map;

import crosby.binary.*;
import crosby.binary.Osmformat.*;
import crosby.binary.file.*;
import javafx.scene.canvas.GraphicsContext;
//import crosby.binary.test.MyNode;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import javafx.util.Pair;
import org.mapdb.*;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

public class MyGraph {
    //    public static ArrayList<MyNode> mapNodes = new ArrayList<MyNode>();
    public static BTreeMap<Long, double[]> dictionary; //maps a node id to a double array containing the coordinates of the node
    private static Map<Long, long[]> mapRoads; //a list of all connections between nodes. Later becomes all graph edges.
    private static ConcurrentMap<Long, Integer> allWayNodes; //maps the nodes contained in the extracted ways to a list of ways each one is part of
    private static boolean parsingNodes;
//    private static HashSet<Long> junctions;
    private static Map<Long, Set<double[]>> fwdGraph;
    private static Map<Long, Set<double[]>> bckGraph;
    private Pair<Map, Map> graph;

    private long startTime;
    private long endTime;

    private String filePrefix;

    Set<long[]> edges = new HashSet<>();
    int noOfEdges;



    public MyGraph(File file, String region) throws IOException {

        filePrefix = "files//".concat(region + "//");

        DB db3 = DBMaker
                .fileDB(filePrefix.concat("mapRoads.db"))
                .fileMmapEnable()
                .checksumHeaderBypass()
                .closeOnJvmShutdown()
                .make();

        mapRoads = db3.treeMap("map", Serializer.LONG, Serializer.LONG_ARRAY).createOrOpen();

        makeDictionary(file);

//        File fwdGraphDir = new File(filePrefix.concat("fwdGraph.ser"));
//        File bckGraphDir = new File(filePrefix.concat("bckGraph.ser"));
        File graphDir = new File(filePrefix.concat("graph.ser"));
        if(graphDir.exists()){
            System.out.println("Found graph.");
            timerStart();
            FileInputStream fileIn = new FileInputStream(graphDir);
            FSTObjectInput objectIn = new FSTObjectInput(fileIn);
            try {
                graph = (Pair<Map, Map>) objectIn.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
//            FileInputStream bFileIn = new FileInputStream(bckGraphDir);
//            FSTObjectInput bObjectIn = new FSTObjectInput(bFileIn);
//            try {
//                bckGraph = (HashMap) bObjectIn.readObject();
//            } catch (ClassNotFoundException e) {
//                e.printStackTrace();
//            }
            timerEnd("Read graph");
            fwdGraph = graph.getKey();
            bckGraph = graph.getValue();
//            graph = new Pair<>(fwdGraph, bckGraph);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            System.out.println(sdf.format(cal.getTime()));

        } else {
            System.out.println("No graph found, creating now.");

            graph = makeDijkstraGraph(mapRoads, noOfEdges);

            timerStart();
            FileOutputStream fileOut = new FileOutputStream(graphDir);
            FSTObjectOutput objectOut = new FSTObjectOutput(fileOut);
            objectOut.writeObject(graph);
            objectOut.close();
            timerEnd("Writing graph");
        }
    }

    private void makeDictionary(File file) throws IOException{
        DB db2 = DBMaker
                .fileDB(filePrefix.concat("dictionary.db"))
                .fileMmapEnable()
                .checksumHeaderBypass()
                .closeOnJvmShutdown()
                .make();

        dictionary = db2.treeMap("map", Serializer.LONG, Serializer.DOUBLE_ARRAY).createOrOpen();

        if(dictionary.isEmpty()){
            System.out.println("Generating dictionary.");
            DB db = DBMaker
                    .fileDB(filePrefix.concat("wayNodes.db"))
                    .fileMmapEnable()
                    .checksumHeaderBypass()
                    .closeOnJvmShutdown()
                    .make();


            allWayNodes = db
                    .treeMap("map", Serializer.LONG, Serializer.INTEGER)
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
        } else {
            System.out.println("Dictionary found; skipping creation.");
        }
    }

    private Pair<Map, Map> makeDijkstraGraph(Map<Long, long[]> edges, int noOfEdges){
        fwdGraph = new HashMap<>(noOfEdges);
        bckGraph = new HashMap<>(noOfEdges);


        timerStart();
        System.out.println("Adding connections");
        int counter = 0;
        for(Map.Entry<Long, long[]> way : edges.entrySet()){ //iterate through every edge and add neighbours to graph vertices accordingly
            counter++;
//                System.out.println(((double) counter / (double) noOfEdges) * 100);
//            System.out.println(way.getWayId());

            long[] wayNodes = way.getValue();
            if(wayNodes.length > 1){
                long fstVert = wayNodes[0];
                long lstVert = wayNodes[wayNodes.length - 1]; //could be .get(1) if we've stripped the ways
                if(fstVert == (Long.parseLong("2310931045")) || lstVert == (Long.parseLong("2310931045"))){
                    System.out.println("Something ain't right here");
                }
                if(!fwdGraph.containsKey(fstVert)){
                    fwdGraph.put(fstVert, new HashSet<>()); //because cul-de-sacs don't count as junctions so haven't been added yet.
                }
                if(!bckGraph.containsKey(lstVert)){
                    bckGraph.put(lstVert, new HashSet<>()); //because cul-de-sacs don't count as junctions so haven't been added yet.
                }
                double length = lengthOfEdge(wayNodes);
//                    double length = 0;
                fwdGraph.get(fstVert).add(new double[]{(double) lstVert, length, way.getKey().doubleValue()});
                bckGraph.get(lstVert).add(new double[]{(double) fstVert, length, way.getKey().doubleValue()});
            }
        }
        timerEnd("Creating graph");
        return new Pair<>(fwdGraph, bckGraph);
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

    private double lengthOfEdge(long[] edge){
        double length = 0;
        double[] nodeA = dictionary.get(edge[0]);
        for(long node : edge){
            double[] nodeB = dictionary.get(node);
            length = length + haversineDistance(nodeA, nodeB);
            nodeA = nodeB;
        }
        return length;
    }

    private double haversineDistance(double[] nodeA, double[] nodeB){
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
                    if(allWayNodes.containsKey(lastId)){
//                System.out.printf("Dense node, ID %d @ %.6f,%.6f\n",
//                        lastId,parseLat(lastLat),parseLon(lastLon));
                        double[] tempDense = new double[3];
//                        MyNode tempDense = new MyNode();
//                        tempDense.setLati(parseLat(lastLat));
//                        tempDense.setLongi(parseLon(lastLon));
//                        tempDense.setNodeId(lastId);
                        tempDense[0] = parseLat(lastLat);
                        tempDense[1] = parseLon(lastLon);
                        tempDense[2] = lastId;
                        dictionary.put(lastId, tempDense);
//                        counter++;
//                        System.out.println(counter);
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
                        if (key.equals("oneway") && value.equals("true")) {
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
                            if(value.matches("motorway|motorway_link")){
                                addWay(w, oneWay, lastRef);
                            } else if (value.matches("trunk|trunk_link")){
                                addWay(w, oneWay, lastRef);
                            } else if (value.matches("primary|primary_link")){
                                addWay(w, oneWay, lastRef);
                            } else if (value.matches("secondary|secondary_link")){
                                addWay(w, oneWay, lastRef);
                            } else if (value.matches("tertiary|unclassified|residential|service|tertiary_link|road")){
                                addWay(w, oneWay, lastRef);
                            }
                        }
                    }
                }
            }
        }

        private void addWay(Way w, boolean oneWay, long wayId){
            long[] fwdWay = new long[w.getRefsList().size()];
            long[] bckWay = new long[w.getRefsList().size()];
            long lastRef = 0;
            int fwdCtr = 0;
            int bckCtr = w.getRefsCount() - 1;
            for (Long ref : w.getRefsList()) {
                lastRef+= ref;
                fwdWay[fwdCtr] = lastRef;
                if(!oneWay){
                    bckWay[bckCtr] = lastRef;
                }
                int wayCount;
                if(allWayNodes.get(lastRef) != null){
                    wayCount = allWayNodes.get(lastRef);
                } else {
                    wayCount = 0;
                }
                allWayNodes.put(lastRef, wayCount + 1);
                if(lastRef == (Long.parseLong("2310931045"))){
                    System.out.println(w.getId());
                    System.out.println(wayCount);
                }
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

    private void splitWays(Map<Long, long[]> ways, boolean strip){
//        DB db3 = DBMaker
//                .fileDB("files//edges.db")
//                .fileMmapEnable()
//                .checksumHeaderBypass()
//                .closeOnJvmShutdown()
//                .make();
//
//        Map<Long, long[]> edges = db3.treeMap("set", Serializer.LONG, Serializer.LONG_ARRAY).createOrOpen();

//        if(edges.isEmpty()){
            for(Map.Entry<Long, long[]> w : ways.entrySet()){
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

    private void splitWay(Long id, long[] nodes, boolean strip){
        for(int i = 1; i < (nodes.length - 1); i++){
            if(allWayNodes.get(nodes[i]) > 1){
                if(nodes[i] == (Long.parseLong("2310931045"))){
                    System.out.println("Huh");
                }
                long restOfWayId = generateNewWayRef();
                long[] headerWayNodes = Arrays.copyOfRange(nodes, 0, i + 1);
                mapRoads.put(id, headerWayNodes);
                splitWay(restOfWayId, Arrays.copyOfRange(nodes, i, nodes.length), strip);
                return;
            }
        }
        mapRoads.put(id, nodes);
    }

    public Map<Long, Set<double[]>> getGraph() {
        return fwdGraph;
    }

    public Set<double[]> fwdAdj(Long v){
        return fwdGraph.get(v);
    }

    public Set<double[]> bckAdj(Long v){
        return bckGraph.get(v);
    }

    public ArrayList<Point2D.Double> refsToNodes(ArrayList<Long> refs){
        ArrayList<Point2D.Double> nodes = new ArrayList<>();
        for(Long ref : refs){
            nodes.add(new Point2D.Double(dictionary.get(ref)[1], dictionary.get(ref)[0]));
        }
        return nodes;
    }

    public ArrayList<Long> wayToNodes(long wayId){
        ArrayList<Long> returnNodes = new ArrayList<>();
//        System.out.println(mapRoads.containsKey(wayId));
        long[] nodes = mapRoads.get(wayId);
        for(int i = 0; i < nodes.length; i++){
            returnNodes.add(nodes[i]);
        }
        return returnNodes;
    }

    public ArrayList<Long> nodesToRefs(ArrayList<MyNode> nodes){
        ArrayList<Long> refs = new ArrayList<>();
        for(MyNode node : nodes){
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

    public BTreeMap<Long, double[]> getDictionary(){
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
}
