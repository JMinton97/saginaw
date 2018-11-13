package project.map;

import com.sun.tools.javac.util.Pair;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import crosby.binary.*;
import crosby.binary.Osmformat.*;
import crosby.binary.file.*;
//import crosby.binary.test.MyNode;

import javax.imageio.ImageIO;
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
    private static HashMap<Long, HashSet<Long>> allWayNodes;
    private static ArrayList<Long> tileWays;
    private static double northMost, westMost, southMost, eastMost;
    private static long northMostNode, westMostNode, southMostNode, eastMostNode;
    private double spaceModifierX;
    private double spaceModifierY;
    private double paneHeight = 2000.00;
    private double paneWidth;
    private static double interval;
    private BufferedImage map;
    protected BufferedImage[][] mapArray;
    private int level;
    private static boolean parsingNodes, findingMax, tile;
    private static int counter;
    private static HashSet<Long> junctions;
    private static int linesDrawn, maxEdge;
    public static int bX, bY;
    public static double xLow, xHigh, yLow, yHigh;

    private static File file;
    public static double[][][] bounds;


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
//        System.out.println("Number of way nodes: " + allWayNodes.size());
//        System.out.println("Number of junction nodes: " + junctions.size());
//        System.out.println("Map roads pre-split:      " + mapRoads.size());
        mapRoadsSplit = splitWays(mapRoads);
//        System.out.println("Map roads post-split:     " + mapRoadsSplit.size());
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

    public MyMap(File file, double scale) throws IOException{
        this.file = file;
        scale = 40000; //40000 pixels per degree!

        dictionary = new HashMap<>();
        mapRoads = new ArrayList<>();
        mapRails = new ArrayList<>();
        mapGreens = new ArrayList<>();
        mapForests = new ArrayList<>();
        mapWaterBodies = new ArrayList<>();
        mapWaterWays = new ArrayList<>();
        mapCycles = new ArrayList<>();
        allWayNodes = new HashMap<>();

        northMost = -Double.MAX_VALUE;
        westMost = Double.MAX_VALUE;
        southMost = Double.MAX_VALUE;
        eastMost = -Double.MAX_VALUE;

        parsingNodes = false;
        InputStream input = new FileInputStream(file);
        BlockReaderAdapter brad = new TestBinaryParser();
        BlockInputStream wayReader = new BlockInputStream(input, brad);
        wayReader.process(); //first we find all the ways we want
        wayReader.close();

        System.out.println("This many nodes in the region: " + allWayNodes.size());

        findingMax = true;
        System.out.println("Finding the extremities.");
        InputStream input2 = new FileInputStream(file);
        BlockReaderAdapter brad2 = new TestBinaryParser();
        BlockInputStream extremityReader = new BlockInputStream(input2, brad2);
        extremityReader.process(); //then find the extremities of the map
        extremityReader.close();

//        System.out.println("North most " + northMostNode);
//        System.out.println("South most " + southMostNode);
//        System.out.println("East most " + eastMostNode);
//        System.out.println("West most " + westMostNode);

        MyNode origin = new MyNode();
        origin.setLati(northMost);
        origin.setLongi(westMost);

//        System.out.println("West most degree" + westMost);
//        System.out.println("East most degree" + eastMost);

        double height;
        if(northMost > southMost) {
            height = Math.abs(northMost - southMost);
        } else {
            height = Math.abs(southMost - northMost);
        }

        double width;
        if(eastMost > westMost) {
            width = Math.abs(eastMost - westMost);
        } else {
            width = Math.abs(westMost - eastMost);
        }

        System.out.println("Total map width in px: " + scale * width);

        maxEdge = 15000; //max edge length of an image
        interval = width / ((scale * width) / maxEdge);
        spaceModifierX = maxEdge / interval;
//        System.out.println("Interval " + interval);

        BufferedImage[][] tiles = new BufferedImage[(int) Math.ceil((scale * width) / maxEdge)][(int) Math.ceil((scale * height) / maxEdge)];
        bounds = new double[(int) Math.ceil((scale * width) / maxEdge) + 1][(int) Math.ceil((scale * height) / maxEdge) + 1][2];

        for(int x = 0; x < bounds.length; x++) {
            bounds[x][0][0] = northMost;
        }

        for(int y = 0; y < bounds[0].length; y++) {
            bounds[y][0][1] = westMost;
        }

        for(int x = 0; x < bounds.length; x++){
            for(int y = 0; y < bounds[0].length; y++){
                if(!(x == 0 && y == 0)) {
                    Double lat, lon;
                    if (x == 0) {
                        lon = westMost ; //only works in northern hemisphere????
                    } else {
                        lon = bounds[x - 1][y][1] + interval;
                    }
                    if (y == 0) {
                        lat = northMost;
                    } else {
                        lat = bounds[x][y - 1][0] - interval;
                    }

                    bounds[x][y][0] = lat;
                    bounds[x][y][1] = lon;
                    bX = x;
                    bY = y;
//                    System.out.print("(" + x + " " + y + " " + bounds[x][y][0] + " " + bounds[x][y][1] + ")");

//                    drawArray();
                }
            }
        }
        drawArray();
        for(int x = 0; x < bounds.length; x++) {
            for (int y = 0; y < bounds[0].length; y++) {
                if (x != 0 && y != 0) {
                    xLow = bounds[x-1][y][1];
                    xHigh = bounds[x][y][1];
                    yLow = bounds[x][y-1][0];
                    yHigh = bounds[x][y][0];
                    long startTime = System.nanoTime();
                    readTile(x, y, bounds);
                    long endTime = System.nanoTime();
                    System.out.println("Read time: " + (((float) endTime - (float)startTime) / 1000000000));

                    startTime = System.nanoTime();
                    tiles[x - 1][y - 1] = drawTile(x, y, bounds, maxEdge);
                    endTime = System.nanoTime();
                    System.out.println("Draw time: " + (((float) endTime - (float)startTime) / 1000000000));

                    startTime = System.nanoTime();
                    saveMap(tiles[x - 1][y - 1], x, y);
                    endTime = System.nanoTime();
                    System.out.println("Save time: " + (((float) endTime - (float)startTime) / 1000000000));
                    tiles[x - 1][y - 1] = null;
                    System.out.println();
                }
            }
        }
    }

    public void readTile(int x, int y, double[][][] bounds) throws IOException{
        dictionary.clear();
        tileWays.clear();
        tile = true;
        parsingNodes = true;
        bX = x;
        bY = y;
        System.out.println("Reading " + bX + " " + bY);
        this.bounds = bounds;
        InputStream input2 = new FileInputStream(file);
        BlockReaderAdapter brad2 = new TestBinaryParser();
        BlockInputStream tileReader = new BlockInputStream(input2, brad2);

        tileReader.process();
        tileReader.close();
        System.out.println("DICTIONARY SIZE " + dictionary.size());
    }

    public BufferedImage drawTile(int x, int y, double[][][] bounds, int edge){
        System.out.println("Drawing tile.");
        double[] axis = new double[2];
        if(x == 0){
            axis[1] = westMost;
        } else { axis[1] = bounds[x - 1][y][1];}
        if(y == 0){
            axis[0] = northMost;
        } else { axis[0] = bounds[x][y - 1][0];}

        BufferedImage tile = new BufferedImage(edge, edge, 1);

        Graphics2D mapGraphics = tile.createGraphics();

        mapGraphics.setColor(new Color(255, 255, 255));
        mapGraphics.fillRect(0, 0, maxEdge, maxEdge);
        BasicStroke bs = new BasicStroke(1);
        mapGraphics.setStroke(bs);

        for(MyWay w: mapGreens){
            drawWay(w, mapGraphics, false, axis);
        }
//        System.out.println("drew greens");

        for(MyWay w: mapForests){
            drawWay(w, mapGraphics, false, axis);
        }
//        System.out.println("drew forests");

        for(MyWay w: mapWaterBodies){
            drawWay(w, mapGraphics, false, axis);
        }
//        System.out.println("drew water");

        for(MyWay w: mapWaterWays){
            drawWay(w, mapGraphics, false, axis);
        }
//        System.out.println("drew rivers");

        for(MyWay w: mapRails){
            drawWay(w, mapGraphics, false, axis);
        }
//        System.out.println("drew rails");

        linesDrawn = 0;
        for(MyWay w: mapRoads){
            drawWay(w, mapGraphics, true, axis);
        }
//        System.out.println("drew roads under");
//        System.out.println(linesDrawn);

        linesDrawn = 0;
        for(MyWay w: mapRoads){
            drawWay(w, mapGraphics, false, axis);
        }
//        System.out.println("drew roads over");
        System.out.println(linesDrawn);

        for(MyWay w: mapCycles){
            drawWay(w, mapGraphics, false, axis);
        }

        return tile;
    }


    public void drawMap(int level){
//        this.level = level;
//        map = new BufferedImage((int) Math.round(paneWidth), (int) Math.round(paneHeight), 1);
//        Graphics2D mapGraphics = map.createGraphics();
//        mapGraphics.setColor(new Color(255, 255, 243));
//        mapGraphics.fillRect(0, 0, map.getWidth(), map.getHeight());
//        BasicStroke bs = new BasicStroke(1);
//        mapGraphics.setStroke(bs);
//
//        for(MyWay w: mapGreens){
//            drawWay(w, mapGraphics, false);
//        }
////        System.out.println("drew greens");
//
//        for(MyWay w: mapForests){
//            drawWay(w, mapGraphics, false);
//        }
////        System.out.println("drew forests");
//
//        for(MyWay w: mapWaterBodies){
//            drawWay(w, mapGraphics, false);
//        }
////        System.out.println("drew water");
//
//        for(MyWay w: mapWaterWays){
//            drawWay(w, mapGraphics, false);
//        }
////        System.out.println("drew rivers");
//
//        for(MyWay w: mapRails){
//            drawWay(w, mapGraphics, false);
//        }
////        System.out.println("drew rails");
//
//        linesDrawn = 0;
//        for(MyWay w: mapRoads){
//            drawWay(w, mapGraphics, true);
//        }
////        System.out.println("drew roads under");
////        System.out.println(linesDrawn);
//
//        linesDrawn = 0;
//        for(MyWay w: mapRoadsSplit){
//            drawWay(w, mapGraphics, false);
//        }
////        System.out.println("drew roads over");
//        System.out.println(linesDrawn);
//
//        for(MyWay w: mapCycles){
//            drawWay(w, mapGraphics, false);
//        }
////        System.out.println("drew cycles");
////        try {
////            File outputfile = new File("saved.png");
////            ImageIO.write(map, "png", outputfile);
////        } catch (IOException e) {
////            // handle exception
////        }
//        System.out.println("All finished.");
    }

    public BufferedImage getMap() {
        return map;
    }

    private void drawWay(MyWay way, Graphics2D mapGraphics, boolean underlay, double[] bound) {
        if (isVisible(way.getType(), level)) {
            Color wayColor = Color.WHITE;
            Color inColor;
            Color outColor;
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
                        drawPolyline(mapGraphics, wayNodes, bound);
                        break;

                    case RAILWAY:
                        mapGraphics.setStroke(new BasicStroke(2));
                        mapGraphics.setPaint(Color.BLACK);
                        drawPolyline(mapGraphics, wayNodes, bound);
                        break;

                    case WATERWAY:
                        mapGraphics.setStroke(new BasicStroke(4));
                        mapGraphics.setPaint(new Color(102, 178, 255));
                        drawPolyline(mapGraphics, wayNodes, bound);
                        break;

                    case CYCLE:
                        mapGraphics.setStroke(new BasicStroke(4));
                        mapGraphics.setPaint(Color.RED);
                        drawPolyline(mapGraphics, wayNodes, bound);
                        break;

                    case GREEN:
                        mapGraphics.setStroke(new BasicStroke(4));
                        inColor = new Color(153, 255, 153);
                        outColor = new Color(102, 255, 102);
                        drawArea(mapGraphics, wayNodes, bound, inColor, outColor);
                        break;

                    case TREE:
                        mapGraphics.setStroke(new BasicStroke(4));
                        inColor = new Color(0, 204, 102);
                        outColor = new Color(0, 153, 76);
                        drawArea(mapGraphics, wayNodes, bound, inColor, outColor);
                        break;

                    case WATERBODY:
                        mapGraphics.setStroke(new BasicStroke(4));
                        inColor = new Color(153, 204, 255);
                        outColor = new Color(102, 178, 255);
                        drawArea(mapGraphics, wayNodes, bound, inColor, outColor);
                        break;
                }
            }
        }
    }

    private void drawPolyline(Graphics2D mapGraphics, List<Long> wayNodes, double[] bound) {
        MyNode u, v;
        for (int node = 0; node < wayNodes.size() - 1; node++) {
            if (dictionary.containsKey(wayNodes.get(node)) && dictionary.containsKey(wayNodes.get(node + 1))) {
                u = dictionary.get(wayNodes.get(node)); //dictionary.get(wayNodes.get(node)); //efficiency by using previous v?
                v = dictionary.get(wayNodes.get(node + 1));
//                System.out.println(u.toString());
                double uy = Math.abs(u.getLati() - bound[0]) * spaceModifierX;
                double ux = Math.abs(u.getLongi() - bound[1]) * spaceModifierX;
                double vy = Math.abs(v.getLati() - bound[0]) * spaceModifierX;
                double vx = Math.abs(v.getLongi() - bound[1]) * spaceModifierX;
                mapGraphics.drawLine((int) Math.round(ux), (int) Math.round(uy), (int) Math.round(vx), (int) Math.round(vy));
                linesDrawn++;
            }
        }
    }

    private void drawArea(Graphics2D mapGraphics, List<Long> wayNodes, double[] bound, Color inColor, Color outColor){
        boolean first = true;
        GeneralPath waterPath = new GeneralPath(GeneralPath.WIND_EVEN_ODD, wayNodes.size() - 1);
        for (int node = 0; node < wayNodes.size(); node++) {
            if (dictionary.containsKey(wayNodes.get(node))) {
                MyNode u = dictionary.get(wayNodes.get(node)); //efficiency by using previous v?
                double uy = Math.abs(u.getLati() - bound[0]) * spaceModifierX;
                double ux = Math.abs(u.getLongi() - bound[1]) * spaceModifierX;
                if (first) {
                    waterPath.moveTo(ux, uy);
                    first = false;
                } else {
                    waterPath.lineTo(ux, uy);
                }

            }
        }
//        waterPath.closePath();
        mapGraphics.setPaint(inColor);
        mapGraphics.fill(waterPath);
        mapGraphics.setPaint(outColor);
        mapGraphics.draw(waterPath);
    }

    public void drawRoute(List<Long> route) {
//        Graphics2D mapGraphics = map.createGraphics();
//        mapGraphics.setPaint(Color.RED);
//        mapGraphics.setStroke(new BasicStroke((8)));
//        drawWay2(mapGraphics, route, bounds);
    }

    public void saveMap(BufferedImage map, int x, int y){
        try {

            String filename = "draw/tile-".concat(String.valueOf(y)).concat("-").concat(String.valueOf(x)).concat(".png");
            File outputfile = new File(filename);
            ImageIO.write(map, "png", outputfile);
        } catch (IOException e) {
            // handle exception
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
        private int bXL, bYL;
        public void setBounds(int bX, int bY){
            this.bXL = bX;
            this.bYL = bY;
        }

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
            if(findingMax){
                long lastId=0;
                long lastLat=0;
                long lastLon=0;
                for (int i=0 ; i<nodes.getIdCount() ; i++) {
                    lastId += nodes.getId(i);
                    lastLat += nodes.getLat(i);
                    lastLon += nodes.getLon(i);
                    if(allWayNodes.containsKey(lastId)) {
                        if (parseLat(lastLat) > northMost) {
                            northMost = parseLat(lastLat);
                            northMostNode = lastId;
                        }
                        if (parseLon(lastLon) < westMost) {
                            westMost = parseLon(lastLon);
                            westMostNode = lastId;
                        }
                        if (parseLat(lastLat) < southMost) {
                            southMost = parseLat(lastLat);
                            southMostNode = lastId;
                        }
                        if (parseLon(lastLon) > eastMost) {
                            eastMost = parseLon(lastLon);
                            eastMostNode = lastId;
                        }
                    }
                }
            }
            if(parsingNodes){

//                System.out.println("Parsing dense nodes.");
                long lastId=0;
                long lastLat=0;
                long lastLon=0;

                if(tile){
                    for (int i=0 ; i<nodes.getIdCount() ; i++) {
                        lastId += nodes.getId(i);
                        lastLat += nodes.getLat(i);
                        lastLon += nodes.getLon(i);
                        if(allWayNodes.containsKey(lastId)){
//                            setBounds(bX, bY);
//                            int xLow = bX - 1;
//                            int xHigh = bX;
//                            int yLow = bY - 1;
//                            int yHigh = bY;
//                            System.out.println(bounds[xLow][yHigh][1] + " " + bounds[xHigh][yHigh][1]);
                            if(between(parseLat(lastLat), parseLon(lastLon))) {
//                                if(lastId == Long.parseLong("1368883963")){
//                                    System.out.println("FOUND IT ###################################");
//                                }
                                MyNode tempDense = new MyNode();
                                tempDense.setLati(parseLat(lastLat));
                                tempDense.setLongi(parseLon(lastLon));
                                tempDense.setNodeId(lastId);
                                dictionary.put(lastId, tempDense);
                                tileWays.addAll(allWayNodes.get(lastId));
                            }
                        }
                    }
                } else {
                    for (int i=0 ; i<nodes.getIdCount() ; i++) {
                        lastId += nodes.getId(i);
                        lastLat += nodes.getLat(i);
                        lastLon += nodes.getLon(i);
                        if(allWayNodes.containsKey(lastId)){
                            MyNode tempDense = new MyNode();
                            tempDense.setLati(parseLat(lastLat));
                            tempDense.setLongi(parseLon(lastLon));
                            tempDense.setNodeId(lastId);
                            dictionary.put(lastId, tempDense);
                        }
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
//                            System.out.println("GOT A ROAD");
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
//                            MyWay tempWay = buildMyWay(w);
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
//                            MyWay tempWay = buildMyWay(w);
//                            tempWay.setType(WayType.GREEN);
//                            mapGreens.add(tempWay);
//                        }
//                        if(key.equals("waterway") && (value.matches("river|stream|canal"))){
//                            MyWay tempWay = buildMyWay(w);
//                            tempWay.setType(WayType.WATERWAY);
//                            mapWaterWays.add(tempWay);
//                        }
//                        if((key.equals("natural") && value.equals("water"))
//                                || value.matches("reservoir|basin")){
//                            MyWay tempWay = buildMyWay(w);
//                            tempWay.setType(WayType.WATERBODY);
//                            mapWaterBodies.add(tempWay);
//                        }
//                        if((key.equals("natural") && value.equals("wood"))
//                                || (key.equals("landuse") && value.equals("forest"))){
//                            MyWay tempWay = buildMyWay(w);
//                            tempWay.setType(WayType.TREE);
//                            mapForests.add(tempWay);
//                        }
//                        if(key.equals("cycleway") || value.equals("cycleway") ||
//                                (key.equals("route") && value.equals("bicycle"))){
//                            MyWay tempWay = buildMyWay(w);
//                            tempWay.setType(WayType.CYCLE);
//                            mapCycles.add(tempWay);
//                        }
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
                lastRef += ref;
                tempWay.addWayNode(lastRef);
//                int wayCount;
                if (allWayNodes.get(lastRef) != null) {
                    allWayNodes.get(lastRef).add(id);
                } else {
                    HashSet<Long> waySet = new HashSet<>();
                    waySet.add(id);
                    allWayNodes.put(lastRef, waySet);
                }
//                allWayNodes.put(lastRef, wayCount + 1);
            }
            return tempWay;
        }

        @Override
        protected void parse(HeaderBlock header) {
//            System.out.println("Got header block.");
        }

        public void complete() {
            System.out.println("Completed parse.");
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
            if(allWayNodes.get(way.getWayNodes().get(i)).size() > 1){
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

    private static boolean between(double lat, double lon){
//        System.out.println(xLow + " " + lon + " " + xHigh);
//        System.out.println(j);

//        boolean in = true;
        if(xLow < xHigh) {
            if (!((lon >= xLow) && (lon <= xHigh))) {
                return false;
            }
        } else {
            if(!((lon <= xLow) && (lon >= xHigh))){
                return false;
            }
        }

        if(yLow < yHigh) {
            if (!((lat >= yLow) && (lat <= yHigh))) {               //something is wrong here - dictionary sizes are the same for each level
                return false;                                       //FIXED?
            }
        } else {
            if(!((lat <= yLow) && (lat >= yHigh))){
                return false;
            }
        }
//        System.out.println(xLow + " " + lon + " " + xHigh);
//        System.out.println(yLow + " " + lat + " " + yHigh);
//        System.out.println();
        return true;
    }

    private static void drawArray(){
        for(int y = 0; y < bounds[0].length; y++){
            for(int x = 0; x < bounds.length; x++){
                System.out.print("(" + bounds[x][y][0] + " " + bounds[x][y][1] + ")      ");
            }
            System.out.println();
        }
        System.out.println();
    }
}
