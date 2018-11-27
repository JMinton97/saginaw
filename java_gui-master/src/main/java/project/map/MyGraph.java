package project.map;

import crosby.binary.*;
import crosby.binary.Osmformat.*;
import crosby.binary.file.*;
import javafx.scene.canvas.GraphicsContext;
//import crosby.binary.test.MyNode;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import javafx.util.Pair;
import org.mapdb.*;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

public class MyGraph {
    //    public static ArrayList<MyNode> mapNodes = new ArrayList<MyNode>();
    public static BTreeMap<Long, double[]> dictionary; //maps a node id to a MyNode object containing more details
    private static ArrayList<MyWay> mapRoads; //a list of all connections between nodes. Later becomes all graph edges.
    private static ConcurrentMap<Long, Integer> allWayNodes; //maps the nodes contained in the extracted ways to a list of ways each one is part of
    private static boolean parsingNodes;
//    private static HashSet<Long> junctions;
    private static Map<Long, Set<double[]>> graph;

    private long startTime;
    private long endTime;


    public MyGraph(File file) throws IOException {

        DB db = DBMaker
                .fileDB("files//wayNodes.db")
                .fileMmapEnable()
                .checksumHeaderBypass()
                .closeOnJvmShutdown()
                .make();


        allWayNodes = db
                .treeMap("map", Serializer.LONG, Serializer.INTEGER)
                .createOrOpen();

        DB db2 = DBMaker
                .fileDB("files//dictionary.db")
                .fileMmapEnable()
                .checksumHeaderBypass()
                .closeOnJvmShutdown()
                .make();

        dictionary = db2.treeMap("map", Serializer.LONG, Serializer.DOUBLE_ARRAY).createOrOpen();



        parsingNodes = false;
        mapRoads = new ArrayList<>();


        if(allWayNodes.isEmpty()){
            timerStart();
            InputStream input = new FileInputStream(file);
            BlockReaderAdapter brad = new OSMBinaryParser();
            long startTime = System.nanoTime();
            new BlockInputStream(input, brad).process();
            long endTime = System.nanoTime();
            timerEnd("Getting ways");
        } else {
            System.out.println("allWayNodes found; skipping read");
        }

        timerStart();
        ArrayList<MyWay> edges;
        System.out.println("Map roads pre-split:      " + mapRoads.size());
        edges = splitWays(mapRoads, true);
        mapRoads = null;
        System.out.println("Map roads post-split:     " + edges.size());
        timerEnd("Splitting ways");

        parsingNodes = true;

        if(dictionary.isEmpty()){
            timerStart();
            InputStream input2 = new FileInputStream(file);
            BlockReaderAdapter brad2 = new OSMBinaryParser();
            new BlockInputStream(input2, brad2).process();
            timerEnd("Getting nodes");
        } else {
            System.out.println("dictionary found; skipping read");
        }

        timerStart();
        for(MyWay way : edges){ //determine the length of every edge
            double length = 0;
            List<Long> nodes = way.getWayNodes();
            long lastNode = nodes.get(0);
            for(long node : nodes){
                length = length + haversineDistance(lastNode, node);
//                length = 0;
                lastNode = node;
            }
            if (length < 0){
                System.out.println("waaaah");
            }
            way.setLength(length);
        }
        timerEnd("Measuring edges");


        File graphDir = new File("files//graph.ser");
        if(graphDir.exists()){
            System.out.println("Found graph.");
            timerStart();
            FileInputStream fileIn = new FileInputStream(graphDir);
            FSTObjectInput objectIn = new FSTObjectInput(fileIn);
            try {
                graph = (HashMap) objectIn.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            timerEnd("Read graph");
        } else {
            System.out.println("No graph found, creating now.");
            graph = new HashMap<>();

            timerStart();
            System.out.println("Adding connections");
            for(MyWay way : edges){ //iterate through every edge and add neighbours to graph vertices accordingly
//            System.out.println(way.getWayId());
                List<Long> wayNodes = way.getWayNodes();
                if(wayNodes.size() > 1){
                    long fstVert = wayNodes.get(0);
                    long lstVert = wayNodes.get(wayNodes.size() - 1); //could be .get(1) if we've stripped the ways
                    if(!graph.containsKey(fstVert)){
                        graph.put(fstVert, new HashSet<>()); //because cul-de-sacs don't count as junctions so haven't been added yet.
                    }
                    if(!graph.containsKey(lstVert)){
                        graph.put(lstVert, new HashSet<>()); //because cul-de-sacs don't count as junctions so haven't been added yet.
                    }
                    graph.get(fstVert).add(new double[]{(double) wayNodes.get(1), way.getLength()});
                    graph.get(lstVert).add(new double[]{(double) wayNodes.get(0), way.getLength()});
                }
            }
            timerEnd("Creating graph");

            timerStart();
            FileOutputStream fileOut = new FileOutputStream(graphDir);
            FSTObjectOutput objectOut = new FSTObjectOutput(fileOut);
            objectOut.writeObject(graph);
            objectOut.close();
            timerEnd("Writing graph");
        }


//        System.out.println(graph.size());

//        dictionary = null;
//        edges = null;

    }

    public void print(){ //print graph contents
        for(long vert : graph.keySet()){
            Set<double[]> neighbours = graph.get(vert);
            ArrayList<Long> neighbourNodes = new ArrayList<>();
            for(double[] neighbour : neighbours){
                neighbourNodes.add((long) neighbour[0]);
            }
//            System.out.println("Node: " + vert + " Neighbours: " + neighbourNodes.toString());
        }

        System.out.println(graph.size());
    }

    private double haversineDistance(long a, long b){
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
                        tempDense[1] = parseLat(lastLat);
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
                for (Way w : ways) {
                    String key;
                    String value;
                    for (int i=0 ; i<w.getKeysCount() ; i++) {
                        key = getStringById(w.getKeys(i));
                        value = getStringById(w.getVals(i));
                        if(key.equals("highway")){
                            if(value.matches("motorway|motorway_link")){
                                MyWay tempWay = buildMyWay(w);
                                tempWay.setType(WayType.ROAD);
                                tempWay.setRoadType(RoadType.MOTORWAY);
                                mapRoads.add(tempWay);
                            } else if (value.matches("trunk|trunk_link")){
                                MyWay tempWay = buildMyWay(w);
                                tempWay.setType(WayType.ROAD);
                                tempWay.setRoadType(RoadType.TRUNK);
                                mapRoads.add(tempWay);
                            } else if (value.matches("primary|primary_link")){
                                MyWay tempWay = buildMyWay(w);
                                tempWay.setType(WayType.ROAD);
                                tempWay.setRoadType(RoadType.PRIMARY);
                                mapRoads.add(tempWay);
                            } else if (value.matches("secondary|secondary_link")){
                                MyWay tempWay = buildMyWay(w);
                                tempWay.setType(WayType.ROAD);
                                tempWay.setRoadType(RoadType.SECONDARY);
                                mapRoads.add(tempWay);
                            } else if (value.matches("tertiary|unclassified|residential|service|tertiary_link|road")){
                                MyWay tempWay = buildMyWay(w);
                                tempWay.setType(WayType.ROAD);
                                tempWay.setRoadType(RoadType.ROAD);
                                mapRoads.add(tempWay);
                            }
                        }
//                    if(key.equals("cycleway") || value.equals("cycleway") ||
//                            (key.equals("route") && value.equals("bicycle"))){
//                        tempWay.setType(WayType.CYCLE);
//                        mapCycles.add(tempWay);
//                    }
                    }
                }
            }
        }

        private MyWay buildMyWay(Way w){
            MyWay tempWay = new MyWay();
            long id = w.getId();
            tempWay.setWayId(id);
            long lastRef = 0;
            for (Long ref : w.getRefsList()) {
                lastRef+= ref;
                tempWay.addWayNode(lastRef);
                Integer wayCount;
                if(allWayNodes.get(lastRef) != null){
                    wayCount = allWayNodes.get(lastRef);
                } else {
                    wayCount = 0;
                }
                allWayNodes.put(lastRef, wayCount + 1);

//                if(allWayNodes.containsKey(lastRef)){
//                    junctions.add(lastRef);
//                    allWayNodes.get(lastRef).add(w.getId());
//                } else {
//                    ArrayList<Long> nodeWays = new ArrayList<>();
//                    nodeWays.add(w.getId());
//                    allWayNodes.put(lastRef, nodeWays);
//                }
            }
            return tempWay;
        }

        @Override
        protected void parse(HeaderBlock header) {
            System.out.println("Got header block.");
        }

        public void complete() {
            System.out.println("Complete!");
        }
    }

    private ArrayList<MyWay> splitWays(ArrayList<MyWay> ways, boolean strip){
        ArrayList<MyWay> newWays = new ArrayList<>();
        for(MyWay w : ways){
            ArrayList<MyWay> splitWays = splitWay(w, strip);
                newWays.addAll(splitWays);
            }
        return newWays;
    }

    private MyWay stripWay(MyWay way){
        ArrayList<Long> newNodes = new ArrayList<Long>();
        newNodes.add(way.wayNodes.get(0));
        newNodes.add(way.wayNodes.get(way.wayNodes.size() - 1));
        way.wayNodes = newNodes;
        return way;
    }

    private ArrayList<MyWay> splitWay(MyWay way, boolean strip){
        ArrayList<MyWay> returnWays = new ArrayList<>();
        returnWays.add(way);
        for(int i = 1; i < (way.getWayNodes().size() - 1); i++){
            if(allWayNodes.get(way.getWayNodes().get(i)) > 1){
                MyWay tempWay = new MyWay (way.getWayNodes().subList(0, i + 1));
//                allWayNodes.get(way.getWayNodes().get(i)).add(way.wayId);
                ArrayList<MyWay> tempWayRest = splitWay(new MyWay (way.getWayNodes().subList(i, way.getWayNodes().size())), strip);
                tempWayRest.add(tempWay);
                returnWays = tempWayRest;
                break;
            }
        }
        if(strip){
            for(MyWay w : returnWays){
                w = stripWay(w);
            }
        }
        return returnWays;
    }

    public Map<Long, Set<double[]>> getGraph() {
        return graph;
    }

    public Set<double[]> adj(Long v){
        return graph.get(v);
    }

//    public ArrayList<MyNode> refsToNodes(ArrayList<Long> refs){
//        ArrayList<MyNode> nodes = new ArrayList<>();
//        for(Long ref : refs){
//            nodes.add(dictionary.get(ref));
//        }
//        return nodes;
//    }

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
}
