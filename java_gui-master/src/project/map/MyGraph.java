package project.map;

import com.sun.tools.javac.util.Pair;
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

public class MyGraph {
    //    public static ArrayList<MyNode> mapNodes = new ArrayList<MyNode>();
    private static Map<Long, MyNode> dictionary;
    private static Map<Long, MyWay> mapRoads;
    private static HashMap<Long, ArrayList<Long>> allWayNodes;  //contains each node referenced in the ways we extract,
                                                                // mapped to a list of way ids that the node is part of
    private static boolean parsingNodes;
    private static int counter;
    private static HashSet<Long> junctions;
    private static HashSet<Long> junctions2;


    public MyGraph(File file) throws IOException {
        dictionary = new HashMap<>(); //NOTE - STORING NODE ID TWICE!!!
        mapRoads = new HashMap<>();
        allWayNodes = new HashMap<>();
        junctions = new HashSet<>();
        junctions2 = new HashSet<>();
        parsingNodes = false;
        counter = 0;
        InputStream input = new FileInputStream(file);
        BlockReaderAdapter brad = new TestBinaryParser();
        new BlockInputStream(input, brad).process();
        System.out.println("Map roads pre-split:      " + mapRoads.size());
        mapRoads = splitWays(mapRoads, false);
        System.out.println("Map roads post-split:     " + mapRoads.size());
        System.out.println("Number of way nodes:      " + allWayNodes.size());
//        System.out.println("Number of junction nodes: " + junctions.size());

        System.out.println(junctions2.size());

//        for(Long j : junctions){
//            if(!junctions2.contains(j)){
//                System.out.println("AWWW NO");
//            }
//        }

        parsingNodes = true;
        InputStream input2 = new FileInputStream(file);
        BlockReaderAdapter brad2 = new TestBinaryParser();
        new BlockInputStream(input2, brad2).process();

        for(MyWay way : mapRoads.values()){
            double length = 0;
            long lastNode = way.getWayNodes().get(0);
            for(long node : way.getWayNodes()){
                length = length + haversineDistance(lastNode, node);
                lastNode = node;
            }
            way.setLength(length);
        }

        Map<Long, Set<Pair<Long, Double>>> graph = new HashMap<Long, Set<Pair<Long, Double>>>();


        for(Long node : allWayNodes.keySet()){ //adding each vertex to the graph
            graph.put(node, new HashSet<Pair<Long, Double>>());
        }

        for(Long node : allWayNodes.keySet()){ //iterate through each vertice
            for(long way : allWayNodes.get(node)){ //iterate through each edge for each vertice
                List<Long> edgeNodes = mapRoads.get(mapRoads.lastIndexOf(way)).wayNodes; //get the edge
                long otherEnd;
                if(edgeNodes.get(0).equals(node)) {
                    otherEnd = edgeNodes.get(edgeNodes.size() - 1);
                } else {
                    otherEnd = edgeNodes.get(0);
                }
                Double length = mapRoads.get(mapRoads.lastIndexOf(way)).getLength();
                graph.get(node).add(new Pair(otherEnd, length));
            }
        }
    }

    public double haversineDistance(long a, long b){
        MyNode nodeA = dictionary.get(a);
        MyNode nodeB = dictionary.get(b);
        double rad = 6371000; //radius of earth in metres
        double aLatRadians = Math.toRadians(nodeA.getLati());
        double bLatRadians = Math.toRadians(nodeB.getLati());
        double deltaLatRadians = Math.toRadians(nodeB.getLati() - nodeA.getLati());
        double deltaLongRadians = Math.toRadians(nodeB.getLongi() - nodeA.getLongi());

        double x = Math.sin(deltaLatRadians/2) * Math.sin(deltaLatRadians/2) +
                Math.cos(aLatRadians) * Math.cos(bLatRadians) *
                        Math.sin(deltaLongRadians/2) * Math.sin(deltaLongRadians/2);
        double y = 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1-x));
        double distance = rad * y;
        return distance;

//        return Math.sqrt(Math.pow((nodeB.getLati() - nodeA.getLati()), 2) + Math.pow((nodeB.getLongi() - nodeA.getLongi()), 2));
    }

    public static class TestBinaryParser extends BinaryParser {

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
                        MyNode tempDense = new MyNode();
                        tempDense.setLati(parseLat(lastLat));
                        tempDense.setLongi(parseLon(lastLon));
                        tempDense.setNodeId(lastId);
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
                                mapRoads.put(w.getId(), tempWay);
                            } else if (value.matches("trunk|trunk_link")){
                                MyWay tempWay = buildMyWay(w);
                                tempWay.setType(WayType.ROAD);
                                tempWay.setRoadType(RoadType.TRUNK);
                                mapRoads.put(w.getId(), tempWay);
                            } else if (value.matches("primary|primary_link")){
                                MyWay tempWay = buildMyWay(w);
                                tempWay.setType(WayType.ROAD);
                                tempWay.setRoadType(RoadType.PRIMARY);
                                mapRoads.put(w.getId(), tempWay);
                            } else if (value.matches("secondary|secondary_link")){
                                MyWay tempWay = buildMyWay(w);
                                tempWay.setType(WayType.ROAD);
                                tempWay.setRoadType(RoadType.SECONDARY);
                                mapRoads.put(w.getId(), tempWay);
                            } else if (value.matches("tertiary|unclassified|residential|service|tertiary_link|road")){
                                MyWay tempWay = buildMyWay(w);
                                tempWay.setType(WayType.ROAD);
                                tempWay.setRoadType(RoadType.ROAD);
                                mapRoads.put(w.getId(), tempWay);
                            }
                        }
//                        if(key.equals("railway")){
//                            tempWay.setType(WayType.RAILWAY);
//                            mapRails.add(tempWay);
//                        }
//                        if((key.equals("natural") && value.equals("grass"))
//                                || (key.equals("leisure") && value.equals("common"))
//                                || (key.equals("leisure") && value.equals("park"))
//                                || (key.equals("leisure") && value.equals("golf_course"))
//                                || value.equals("meadow")
//                                || value.equals("recreation_ground")
//                                || value.equals("conservation")
//                                || value.equals("park")){
//                            tempWay.setType(WayType.GREEN);
//                            mapGreens.add(tempWay);
//                        }
//                        if((key.equals("natural")) && (value.matches("river|stream|canal"))){
//                            tempWay.setType(WayType.WATERWAY);
//                            mapWaterWays.add(tempWay);
//                        }
//                        if((key.equals("natural") && value.equals("water"))
//                                || value.matches("reservoir|basin")){
//                            tempWay.setType(WayType.WATERBODY);
//                            mapWaterBodies.add(tempWay);
//                        }
//                        if((key.equals("natural") && value.equals("wood"))
//                                || (key.equals("landuse") && value.equals("forest"))){
//                            tempWay.setType(WayType.TREE);
//                            mapForests.add(tempWay);
//                        }
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
            tempWay.setWayId(w.getId());
            long lastRef = 0;
            for (Long ref : w.getRefsList()) {
                lastRef+= ref;
                tempWay.addWayNode(lastRef);
                if(allWayNodes.containsKey(lastRef)){
                    junctions.add(lastRef);
                    allWayNodes.get(lastRef).add(w.getId());
                } else {
                    ArrayList<Long> nodeWays = new ArrayList<>();
                    nodeWays.add(w.getId());
                    allWayNodes.put(lastRef, nodeWays);
                }
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

    private ArrayList<MyWay> splitWays(Map<Long, MyWay> ways, boolean strip){
        ArrayList<MyWay> newWays = new ArrayList<>();
        for(MyWay w : ways.values()){
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
            if(allWayNodes.get(way.getWayNodes().get(i)).size() > 1){
                MyWay tempWay = new MyWay (way.getWayNodes().subList(0, i + 1));
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
}