package project.map;

import crosby.binary.*;
import crosby.binary.Osmformat.*;
import crosby.binary.file.*;
//import crosby.binary.test.MyNode;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

public class MyMap {
//    public static ArrayList<MyNode> mapNodes = new ArrayList<MyNode>();
    private static Map<Long, MyNode> dictionary;
    private static ArrayList<MyWay> mapRoads;
    private static ArrayList<MyWay> mapRoadsSplit;
    private static ArrayList<MyWay> mapRails;
    private static ArrayList<MyWay> mapGreens;
    private static ArrayList<MyWay> mapForests;
    private static ArrayList<MyWay> mapWaterBodies;
    private static ArrayList<MyWay> mapWaterWays;
    private static ArrayList<MyWay> mapCycles;
    private static HashMap<Long, Integer> allWayNodes;
    private double northMost, westMost, southMost, eastMost;
    private double spaceModifierX;
    private double spaceModifierY;
    private double paneHeight = 2000.00;
    private double paneWidth;
    private BufferedImage map;
    protected BufferedImage[][] mapArray;
    private int level;
    private static boolean parsingNodes;
    private static int counter;
    private static HashSet<Long> junctions;
    private int linesDrawn;


    public MyMap(File file) throws IOException {
        int level;
        dictionary = new HashMap<>(); //NOTE - STORING NODE ID TWICE!!!
        mapRoads = new ArrayList<>();
        mapRails = new ArrayList<>();
        mapGreens = new ArrayList<>();
        mapForests = new ArrayList<>();
        mapWaterBodies = new ArrayList<>();
        mapWaterWays = new ArrayList<>();
        mapCycles = new ArrayList<>();
        allWayNodes = new HashMap<>();
        junctions = new HashSet<>();
        parsingNodes = false;
        counter = 0;
        InputStream input = new FileInputStream(file);
        BlockReaderAdapter brad = new TestBinaryParser();
        new BlockInputStream(input, brad).process();
        System.out.println("Number of way nodes: " + allWayNodes.size());
        System.out.println("Number of junction nodes: " + junctions.size());
        System.out.println("Map roads pre-split:      " + mapRoads.size());
        mapRoadsSplit = splitWays(mapRoads);
        System.out.println("Map roads post-split:     " + mapRoadsSplit.size());
        parsingNodes = true;
        InputStream input2 = new FileInputStream(file);
        BlockReaderAdapter brad2 = new TestBinaryParser();
        new BlockInputStream(input2, brad2).process();
        northMost = -Double.MAX_VALUE;
        westMost = Double.MAX_VALUE;
        southMost = Double.MAX_VALUE;
        eastMost = -Double.MAX_VALUE;
        for (MyNode n : dictionary.values()) {
            if (n.getLati() > northMost) {
                northMost = n.getLati();
            }
            if (n.getLongi() < westMost) {
                westMost = n.getLongi();
            }
            if (n.getLati() < southMost) {
                southMost = n.getLati();
            }
            if (n.getLongi() > eastMost) {
                eastMost = n.getLongi();
            }
        }
        System.out.println("Dictionary size: " + dictionary.size());  //what if douglas peucker returns a list of deleted nodes?
        double height, width;
        if(northMost > southMost) {
            height = Math.abs(northMost - southMost);
        } else {
            height = Math.abs(southMost - northMost);
        }
        if(eastMost > westMost) {
            width = Math.abs(eastMost - westMost);
        } else {
            width = Math.abs(westMost - eastMost);
        }
        System.out.println(northMost + " " + southMost);
        System.out.println("Height is " + height);
        paneWidth = (paneHeight * (width / height)) * 0.75;
        spaceModifierY = paneHeight / height;
        spaceModifierX = spaceModifierY * 0.75;
    }

    public void drawMap(int level){
        this.level = level;
        map = new BufferedImage((int) Math.round(paneWidth), (int) Math.round(paneHeight), 1);
        Graphics2D mapGraphics = map.createGraphics();
        mapGraphics.setColor(new Color(255, 255, 243));
        mapGraphics.fillRect(0, 0, map.getWidth(), map.getHeight());
        BasicStroke bs = new BasicStroke(1);
        mapGraphics.setStroke(bs);

        for(MyWay w: mapGreens){
            drawWay(w, mapGraphics, false);
        }
        System.out.println("drew greens");

        for(MyWay w: mapForests){
            drawWay(w, mapGraphics, false);
        }
        System.out.println("drew forests");

        for(MyWay w: mapWaterBodies){
            drawWay(w, mapGraphics, false);
        }
        System.out.println("drew water");

        for(MyWay w: mapWaterWays){
            drawWay(w, mapGraphics, false);
        }
        System.out.println("drew rivers");

        for(MyWay w: mapRails){
            drawWay(w, mapGraphics, false);
        }
        System.out.println("drew rails");

        linesDrawn = 0;
        for(MyWay w: mapRoads){
            drawWay(w, mapGraphics, true);
        }
        System.out.println("drew roads under");
        System.out.println(linesDrawn);

        linesDrawn = 0;
        for(MyWay w: mapRoadsSplit){
            drawWay(w, mapGraphics, false);
        }
        System.out.println("drew roads over");
        System.out.println(linesDrawn);

        for(MyWay w: mapCycles){
            drawWay(w, mapGraphics, false);
        }
        System.out.println("drew cycles");
//        try {
//            File outputfile = new File("saved.png");
//            ImageIO.write(map, "png", outputfile);
//        } catch (IOException e) {
//            // handle exception
//        }
        System.out.println("All finished.");
    }

    public BufferedImage getMap() {
        return map;
    }

    private void drawWay(MyWay way, Graphics2D mapGraphics, boolean underlay) {
        if (isVisible(way.getType(), level)) {
            Color wayColor = Color.WHITE;
            if (isVisible(way.getRoadType(), level)) {
                List<Long> wayNodes = way.getWayNodes();
//                List<Long> allWayNodes = DouglasPeucker.decimate(way.getWayNodes(), 0.0001, dictionary);
                switch (way.getType()) {
                    case ROAD:
                        switch (way.getRoadType()) {
                            case MOTORWAY:
                                wayColor = Color.BLUE;
                                break;
                            case TRUNK:
                                wayColor = Color.MAGENTA;
                                break;
                            case PRIMARY:
                                wayColor = Color.ORANGE;
                                break;
                            case SECONDARY:
                                wayColor = Color.YELLOW;
                                break;
                            case ROAD:
                                wayColor = Color.WHITE;
                        }
                        if (underlay) {
                            mapGraphics.setPaint(wayColor.darker());
                            mapGraphics.setStroke(new BasicStroke((8)));
                        } else {
                            mapGraphics.setPaint(wayColor);
                            mapGraphics.setStroke(new BasicStroke((4)));
//                            MyNode u = dictionary.get(wayNodes.get(0)); //efficiency by using previous v?
//                            double uy = Math.abs(u.getLati() - northMost) * spaceModifierY;
//                            double ux = Math.abs(u.getLongi() - westMost) * spaceModifierX;
//                            mapGraphics.drawOval((int) ux, (int) uy, 3, 3);
//                            break;
                        }
                        drawWay2(mapGraphics, wayNodes);
                        break;

                    case RAILWAY:
                        mapGraphics.setStroke(new BasicStroke(2));
                        mapGraphics.setPaint(Color.BLACK);
                        drawWay2(mapGraphics, wayNodes);
                        break;

                    case WATERWAY:
                        mapGraphics.setStroke(new BasicStroke(4));
                        mapGraphics.setPaint(new Color(102, 178, 255));
                        drawWay2(mapGraphics, wayNodes);
                        break;

                    case CYCLE:
                        mapGraphics.setStroke(new BasicStroke(4));
                        mapGraphics.setPaint(Color.RED);
                        drawWay2(mapGraphics, wayNodes);
                        break;

                    case GREEN:
                        mapGraphics.setStroke(new BasicStroke(4));
                        GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, wayNodes.size() - 1);
                        for (int node = 0; node < wayNodes.size(); node++) {
                            if (dictionary.containsKey(wayNodes.get(node))) {
                                MyNode u = dictionary.get(wayNodes.get(node)); //efficiency by using previous v?
                                double uy = Math.abs(u.getLati() - northMost) * spaceModifierY;
                                double ux = Math.abs(u.getLongi() - westMost) * spaceModifierX;
                                if (node == 0) {
                                    path.moveTo(ux, uy);
                                } else {
                                    path.lineTo(ux, uy);
                                }

                            }
                        }
                        //                path.closePath();
                        mapGraphics.setPaint(new Color(153, 255, 153));
                        mapGraphics.fill(path);
                        mapGraphics.setPaint(new Color(102, 255, 102));
                        mapGraphics.draw(path);
                        break;

                    case TREE:
                        mapGraphics.setStroke(new BasicStroke(4));
                        GeneralPath treePath = new GeneralPath(GeneralPath.WIND_EVEN_ODD, wayNodes.size() - 1);
                        for (int node = 0; node < wayNodes.size(); node++) {
                            if (dictionary.containsKey(wayNodes.get(node))) {
                                MyNode u = dictionary.get(wayNodes.get(node)); //efficiency by using previous v?
                                double uy = Math.abs(u.getLati() - northMost) * spaceModifierY;
                                double ux = Math.abs(u.getLongi() - westMost) * spaceModifierX;
                                if (node == 0) {
                                    treePath.moveTo(ux, uy);
                                } else {
                                    treePath.lineTo(ux, uy);
                                }

                            }
                        }
                        //                treePath.closePath();
                        mapGraphics.setPaint(new Color(0, 204, 102));
                        mapGraphics.fill(treePath);
                        mapGraphics.setPaint(new Color(0, 153, 76));
                        mapGraphics.draw(treePath);
                        break;

                    case WATERBODY:
                        mapGraphics.setStroke(new BasicStroke(4));
                        GeneralPath waterPath = new GeneralPath(GeneralPath.WIND_EVEN_ODD, wayNodes.size() - 1);
                        for (int node = 0; node < wayNodes.size(); node++) {
                            if (dictionary.containsKey(wayNodes.get(node))) {
                                MyNode u = dictionary.get(wayNodes.get(node)); //efficiency by using previous v?
                                double uy = Math.abs(u.getLati() - northMost) * spaceModifierY;
                                double ux = Math.abs(u.getLongi() - westMost) * spaceModifierX;
                                if (node == 0) {
                                    waterPath.moveTo(ux, uy);
                                } else {
                                    waterPath.lineTo(ux, uy);
                                }

                            }
                        }
                        waterPath.closePath();
                        mapGraphics.setPaint(new Color(153, 204, 255));
                        mapGraphics.fill(waterPath);
                        mapGraphics.setPaint(new Color(102, 178, 255));
                        mapGraphics.draw(waterPath);
                        break;
                }
            }
        }
    }

    private void drawWay2(Graphics2D mapGraphics, List<Long> wayNodes) {
        MyNode u;
        MyNode v = dictionary.get(wayNodes.get(0));
        for (int node = 0; node < wayNodes.size() - 1; node++) {
//                System.out.println(dictionary.get(allWayNodes.get(node)).getNodeId());
//                System.out.println(dictionary.get(allWayNodes.get(node + 1)).getNodeId());
            if (dictionary.containsKey(wayNodes.get(node)) && dictionary.containsKey(wayNodes.get(node + 1))) {
                u = v; //dictionary.get(wayNodes.get(node)); //efficiency by using previous v?
                v = dictionary.get(wayNodes.get(node + 1));
                System.out.println(u.toString());
                double uy = Math.abs(u.getLati() - northMost) * spaceModifierY;
                double ux = Math.abs(u.getLongi() - westMost) * spaceModifierX;
                double vy = Math.abs(v.getLati() - northMost) * spaceModifierY;
                double vx = Math.abs(v.getLongi() - westMost) * spaceModifierX;
                mapGraphics.drawLine((int) Math.round(ux), (int) Math.round(uy), (int) Math.round(vx), (int) Math.round(vy));
                linesDrawn++;
            }
        }
    }

    private boolean isVisible(Enum type, int level) {
        if(type != null){
            if(level == 0){
                return true;
            } else if(level == 1){
                    return type.equals(RoadType.ROAD);
            } else {
                return true;
            }
        }
        return true;
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
                    allWayNodes.put(lastRef, allWayNodes.get(lastRef) + 1);
                } else {
                    allWayNodes.put(lastRef, 1);
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

    private ArrayList<MyWay> splitWays(ArrayList<MyWay> ways){
        ArrayList<MyWay> newWays = new ArrayList<>();
        for(MyWay w : ways){
            ArrayList<MyWay> splitWays = splitWay(w);
            if (splitWays.size() > 1){
                newWays.addAll(splitWays);
            } else {
                newWays.add(w);
            }
        }
        return newWays;
    }

    private ArrayList<MyWay> splitWay(MyWay way){
        ArrayList<MyWay> returnWays = new ArrayList<>();
        returnWays.add(way);
        for(int i = 1; i < (way.getWayNodes().size() - 1); i++){
            if(allWayNodes.get(way.getWayNodes().get(i)) > 1){
                MyWay firstWay = new MyWay (way.getWayNodes().subList(0, i + 1));
                firstWay.setRoadType(way.getRoadType());
                firstWay.setType(way.getType());
                MyWay restWay = new MyWay (way.getWayNodes().subList(i, way.getWayNodes().size()));
                restWay.setRoadType(way.getRoadType());
                restWay.setType(way.getType());
                ArrayList<MyWay> restWays = splitWay(restWay);
                restWays.add(firstWay);
                returnWays = restWays;
                break;
            }
        }
        return returnWays;
    }
}
