package project.map;

import crosby.binary.*;
import crosby.binary.Osmformat.*;
import crosby.binary.file.*;
import net.coobird.thumbnailator.Thumbnails;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import com.objectplanet.image.PngEncoder;

import org.mapdb.*;

//import gnu.trove.map.hash.THashMap;
//import gnu.trove.set.hash.THashSet;

//import org.mapdb.*;

public class MyMap {
    private static Map<Long, MyNode> dictionary;
    private static HashMap<Long, long[]> mapWays;
    private static ArrayList<ArrayList<ConcurrentMap>> tileNodes;
    private static ArrayList<ArrayList<ConcurrentMap>> tileWays;
    private static ConcurrentMap<Long, long[]> allNodes, allNodes2;
    private static HashMap<Long, long[]> putWayNodes;
    private static double northMost, westMost, southMost, eastMost;
    private static long northMostNode, westMostNode, southMostNode, eastMostNode;
    private double spaceModifierX;
    private double spaceModifierY;
    private double paneHeight = 2000.00;
    private double paneWidth;
    private static double interval;
    private static boolean parsingNodes, tile;
    private static int counter;
    private static int linesDrawn, maxEdge;
    public static int bX, bY;
    public static double xLow, xHigh, yLow, yHigh;
    public long startTime, endTime;
    public int level;
    public static DB[][] tileNodesDBs, tileWaysDBs;

    private static File file;
    public static double[][][] bounds;

    private String region;
    private String filePrefix;


    public MyMap(File file) throws IOException {
//        int level;
//
//        dictionary = new HashMap<>(); //NOTE - STORING NODE ID TWICE!!!
//        mapRoads = new ArrayList<>();
//        mapRails = new ArrayList<>();
//        mapGreens = new ArrayList<>();
//        mapForests = new ArrayList<>();
//        mapWaterBodies = new ArrayList<>();
//        mapWaterWays = new ArrayList<>();
//        mapCycles = new ArrayList<>();
////        allNodes = new THashMap<>();
//        junctions = new HashSet<>();
//        parsingNodes = false;
//        counter = 0;
//        InputStream input = new FileInputStream(file);
//        BlockReaderAdapter brad = new TestBinaryParser();
//        new BlockInputStream(input, brad).process();
//        System.out.println("Number of way nodes: " + allNodes.size());
//        System.out.println("Number of junction nodes: " + junctions.size());
//        System.out.println("Map roads pre-split:      " + mapRoads.size());
//        mapRoadsSplit = splitWays(mapRoads);
//        System.out.println("Map roads post-split:     " + mapRoadsSplit.size());
//        parsingNodes = true;
//        InputStream input2 = new FileInputStream(file);
//        BlockReaderAdapter brad2 = new TestBinaryParser();
//        new BlockInputStream(input2, brad2).process();
//        northMost = -Double.MAX_VALUE;
//        westMost = Double.MAX_VALUE;
//        southMost = Double.MAX_VALUE;
//        eastMost = -Double.MAX_VALUE;
//        for (MyNode n : dictionary.values()) {
//            if (n.getLati() > northMost) {
//                northMost = n.getLati();
//            }
//            if (n.getLongi() < westMost) {
//                westMost = n.getLongi();
//            }
//            if (n.getLati() < southMost) {
//                southMost = n.getLati();
//            }
//            if (n.getLongi() > eastMost) {
//                eastMost = n.getLongi();
//            }
//        }
//        System.out.println("Dictionary size: " + dictionary.size());  //what if douglas peucker returns a list of deleted nodes?
//        double height, width;
//        if(northMost > southMost) {
//            height = Math.abs(northMost - southMost);
//        } else {
//            height = Math.abs(southMost - northMost);
//        }
//        if(eastMost > westMost) {
//            width = Math.abs(eastMost - westMost);
//        } else {
//            width = Math.abs(westMost - eastMost);
//        }
//        System.out.println(northMost + " " + southMost);
//        System.out.println("Height is " + height);
//        paneWidth = (paneHeight * (width / height)) * 0.75;
//        spaceModifierY = paneHeight / height;
//        spaceModifierX = spaceModifierY * 0.75;
    }

    public MyMap(File file, String region, int maxEdge, boolean alreadyFiled) throws IOException{

        this.maxEdge = maxEdge; //max edge length of an image

        this.region = region;

        filePrefix = "files//".concat(region + "//");

        counter = 0;
        this.file = file;
        int scale = 40000; //pixels per degree!

        dictionary = new HashMap<>();
//        mapRoads = new ArrayList<>();                //IMPORTANT - memoryDB option in quickstart?
        mapWays = new HashMap<>();

        northMost = 53.5; //56;
        westMost = -5.5; //-6;          //WALES
        southMost = 51.3; //49.5;
        eastMost = -2.5; //2;

//        northMost = 56;
//        westMost = -6;
//        southMost = 49.5;   //IT"S COMING HOME
//        eastMost = 2;

//        northMost = 51.1;
//        westMost = -5.3;
//        southMost = 42.3;   //FRANCE
//        eastMost = 8.4;


        DB db = DBMaker
                .fileDB(filePrefix.concat("allNodes.db"))
                .fileMmapEnable()
                .checksumHeaderBypass()
                .closeOnJvmShutdown()
                .make();


        allNodes = db
                .treeMap("allNodes", Serializer.LONG, Serializer.LONG_ARRAY)
                .createOrOpen();

        System.out.println("done");

        if(!alreadyFiled){
            parsingNodes = false;
            putWayNodes = new HashMap<>();
            InputStream input4 = new FileInputStream(file);
            BlockReaderAdapter brad4 = new TestBinaryParser();
            BlockInputStream wayReader4 = new BlockInputStream(input4, brad4);
            long startTime = System.nanoTime();
            wayReader4.process(); //first we find all the ways we want
            long endTime = System.nanoTime();
            System.out.println("Initial time: " + (((float) endTime - (float)startTime) / 1000000000));
            wayReader4.close();
        }

        db.commit();

//        System.out.println("This many nodes in the region: " + allNodes.size()); //enable size counter?

        counter = 0;


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


        interval = width / ((scale * width) / maxEdge);
        spaceModifierX = maxEdge / interval;
//        System.out.println("Interval " + interval);

        BufferedImage[][] tiles = new BufferedImage[(int) Math.ceil((scale * width) / maxEdge)][(int) Math.ceil((scale * height) / maxEdge)];

        tileNodesDBs = new DB[(int) Math.ceil((scale * width) / maxEdge)][(int) Math.ceil((scale * height) / maxEdge)];
        tileWaysDBs = new DB[(int) Math.ceil((scale * width) / maxEdge)][(int) Math.ceil((scale * height) / maxEdge)];
        bounds = new double[(int) Math.ceil((scale * width) / maxEdge) + 1][(int) Math.ceil((scale * height) / maxEdge) + 1][2];

        for(int x = 0; x < bounds.length; x++) {
            bounds[x][0][0] = northMost;
        }

        for(int y = 0; y < bounds[0].length; y++) {
            bounds[y][0][1] = westMost;
        }

        for(int x = 0; x < (int) Math.ceil((scale * width) / maxEdge); x++){
            for(int y = 0; y < (int) Math.ceil((scale * height) / maxEdge); y++){
                tileNodesDBs[x][y] = DBMaker
                        .fileDB(filePrefix.concat(String.valueOf(x).concat("-").concat(String.valueOf(y)).concat(".nodes.db")))
                        .fileMmapEnable()
                        .checksumHeaderBypass()
                        .closeOnJvmShutdown()
                        .make();
            }
        }

        for(int x = 0; x < (int) Math.ceil((scale * width) / maxEdge); x++){
            for(int y = 0; y < (int) Math.ceil((scale * height) / maxEdge); y++){
                tileWaysDBs[x][y] = DBMaker
                        .fileDB(filePrefix.concat(String.valueOf(x).concat("-").concat(String.valueOf(y)).concat(".ways.db")))
                        .fileMmapEnable()
                        .checksumHeaderBypass()
                        .closeOnJvmShutdown()
                        .make();
            }
        }


        tileNodes = new ArrayList<>();
        tileWays = new ArrayList<>();
        for(int i = 0; i < (int) Math.ceil((scale * width) / maxEdge); i++) {
            System.out.println(i);
            tileNodes.add(new ArrayList<>());
            tileWays.add(new ArrayList<>());
            for(int k = 0; k < (int) Math.ceil((scale * height) / maxEdge); k++) {
                tileNodes.get(i).add(tileNodesDBs[i][k].treeMap(String.valueOf(i).concat("-").concat(String.valueOf(k)).concat(".db"), Serializer.LONG, Serializer.DOUBLE_ARRAY).createOrOpen());
                tileWays.get(i).add(tileWaysDBs[i][k].treeMap(String.valueOf(i).concat("-").concat(String.valueOf(k)).concat(".db"), Serializer.LONG_ARRAY, Serializer.LONG_ARRAY).createOrOpen());
            }
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

        if(!alreadyFiled){
            timerStart();
            parsingNodes = true;
            tile = true;
            InputStream input = new FileInputStream(file);
            BlockReaderAdapter brad = new TestBinaryParser();
            BlockInputStream wayReader = new BlockInputStream(input, brad);
            wayReader.process(); //then collect the nodes
            wayReader.close();
            timerEnd("Reading nodes");
        }

        counter = 0;

        for(ConcurrentMap tw : tileWays.get(3)){
            System.out.println(tw.size());
            System.out.println(tileNodes.get(3).get(counter).size());
            counter++;
            System.out.println();
        }

        PngEncoder encoder = new PngEncoder();

//        if(alreadyFiled) {
//            combineTiles(tiles);
//        } else {
            int counter = 0;
            for(int x = 0; x < bounds.length; x++) {
                for (int y = 0; y < bounds[0].length; y++) {
                    if (x != 0 && y != 0) {
//                    xLow = bounds[x-1][y][1];
//                    xHigh = bounds[x][y][1];
//                    yLow = bounds[x][y-1][0];
//                    yHigh = bounds[x][y][0];
//                     startTime = System.nanoTime();
//                    readTile(x, y, bounds);
//                     endTime = System.nanoTime();
//                    System.out.println("Read time: " + (((float) endTime - (float)startTime) / 1000000000));
                        counter++;
                        System.out.println("Drawing " + counter + " of " + ((bounds.length - 1) * (bounds[0].length - 1)) + " (" + (int) ((counter / ((bounds.length - 1f) * (bounds[0].length - 1f))) * 100) + "%)");
                        startTime = System.nanoTime();
                        tiles[x - 1][y - 1] = drawTile(x, y, maxEdge);
                        endTime = System.nanoTime();
                        System.out.print("Draw time: " + (((float) endTime - (float)startTime) / 1000000000));
                        System.out.print(". Number of ways: " + linesDrawn + ", " + ((linesDrawn + 1) / (((float) endTime - (float)startTime) / 1000000000)) + "w/s");

                        startTime = System.nanoTime();
                        saveMap(tiles[x - 1][y - 1], x, y, encoder);
                        endTime = System.nanoTime();
                        System.out.println(". Save time: " + (((float) endTime - (float)startTime) / 1000000000));
                        tiles[x - 1][y - 1] = null;
                        System.out.println();
                    }
                }
            }
            combineTiles(tiles);
//        }
    }

    public void readTile(int x, int y, double[][][] bounds) throws IOException{
        tileWays.clear();
        dictionary.clear();
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

    public BufferedImage drawTile(int x, int y, int edge){
//        System.out.print("Drawing tile. ");
        double[] axis = new double[2];
        if(x == 0){
            axis[1] = westMost;
        } else { axis[1] = bounds[x - 1][y][1];}
        if(y == 0){
            axis[0] = northMost;
        } else { axis[0] = bounds[x][y - 1][0];}

        BufferedImage tile = new BufferedImage(edge, edge, 1);

        Graphics2D mapGraphics = tile.createGraphics();

        mapGraphics.setColor(new Color(244, 243, 236));
        mapGraphics.fillRect(0, 0, maxEdge, maxEdge);
        BasicStroke bs = new BasicStroke(1);
        mapGraphics.setStroke(bs);

        linesDrawn = 0;

        Map<long[], long[]> tileWay = tileWays.get(x - 1).get(y - 1);

        System.out.println(x + " " + y + " is this big " + tileWay.size());

        for(Map.Entry<long[], long[]> w : (tileWay.entrySet())){
            if(w.getKey()[1] == Long.parseLong("6")) {
                drawWay(w, mapGraphics, false, axis, tileNodes.get(x - 1).get(y - 1));
            }
        } //GREENS

        for(Map.Entry<long[], long[]> w : (tileWay.entrySet())){
            if(w.getKey()[1] == Long.parseLong("9")) {
                drawWay(w, mapGraphics, false, axis, tileNodes.get(x - 1).get(y - 1));
            }
        } //FORESTS

        for(Map.Entry<long[], long[]> w : (tileWay.entrySet())){
            if(w.getKey()[1] == Long.parseLong("7")) {
                drawWay(w, mapGraphics, false, axis, tileNodes.get(x - 1).get(y - 1));
            }
        } //RAILS

        for(Map.Entry<long[], long[]> w : (tileWay.entrySet())){
            if(w.getKey()[1] == Long.parseLong("8")) {
                drawWay(w, mapGraphics, false, axis, tileNodes.get(x - 1).get(y - 1));
            }
        } //WATERBODIES

        for(Map.Entry<long[], long[]> w : (tileWay.entrySet())){
            if(w.getKey()[1] == Long.parseLong("5")) {
                drawWay(w, mapGraphics, false, axis, tileNodes.get(x - 1).get(y - 1));
            }
        } //RAILWAYS

//        for(Map.Entry<long[], long[]> w : (tileWay.entrySet())){
//            if(w.getKey()[1] == (Long.parseLong("0")) || w.getKey()[1] == (Long.parseLong("1")) || w.getKey()[1] == (Long.parseLong("2")) || w.getKey()[1] == (Long.parseLong("3")) || w.getKey()[1] == (Long.parseLong("4"))) {
//                drawWay(w, mapGraphics, true, axis, tileNodes.get(x - 1).get(y - 1));
//            }
//        } //ROADS UNDER

        for(Map.Entry<long[], long[]> w : (tileWay.entrySet())){
            if(w.getKey()[1] == (Long.parseLong("0")) || w.getKey()[1] == (Long.parseLong("1")) || w.getKey()[1] == (Long.parseLong("2")) || w.getKey()[1] == (Long.parseLong("3")) || w.getKey()[1] == (Long.parseLong("4"))) {
                drawWay(w, mapGraphics, false, axis, tileNodes.get(x - 1).get(y - 1));
            }
        } //ROADS OVER

//        for(Map.Entry<long[], long[]> w : (tileWay.entrySet())){
//            if(w.getKey()[1] == Long.parseLong("10")) {
//                drawWay(w, mapGraphics, false, axis, tileNodes.get(x - 1).get(y - 1));
//            }
//        }

        mapGraphics.dispose();

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

    private void drawWay(Map.Entry<long[], long[]> way, Graphics2D mapGraphics, boolean underlay, double[] bound, ConcurrentMap<Long, double[]> dictionary) {
//        System.out.println("Drawing way " + way.getKey()[0]);
        Color wayColor = Color.WHITE;
        Color inColor;
        Color outColor;
        long[] wayNodes = way.getValue();
//                List<Long> allNodes = DouglasPeucker.decimate(way.getWayNodes(), 0.0001, dictionary);
        switch (Long.toString(way.getKey()[1])) {
            case "0": //motorway
                wayColor = Color.BLUE;
                if (underlay) {
                    mapGraphics.setPaint(wayColor.darker());
                    mapGraphics.setStroke(new BasicStroke((8)));
                } else {
                    mapGraphics.setPaint(wayColor);
                    mapGraphics.setStroke(new BasicStroke((4)));
                }
                drawPolyline(mapGraphics, wayNodes, bound, dictionary);
                break;

            case "1": //trunk
                wayColor = Color.MAGENTA;
                if (underlay) {
                    mapGraphics.setPaint(wayColor.darker());
                    mapGraphics.setStroke(new BasicStroke((8)));
                } else {
                    mapGraphics.setPaint(wayColor);
                    mapGraphics.setStroke(new BasicStroke((4)));
                }
                drawPolyline(mapGraphics, wayNodes, bound, dictionary);
                break;

            case "2": //primary
                wayColor = Color.ORANGE;
                if (underlay) {
                    mapGraphics.setPaint(wayColor.darker());
                    mapGraphics.setStroke(new BasicStroke((8)));
                } else {
                    mapGraphics.setPaint(wayColor);
                    mapGraphics.setStroke(new BasicStroke((4)));
                }
                drawPolyline(mapGraphics, wayNodes, bound, dictionary);
                break;

            case "3": //secondary
                wayColor = Color.YELLOW;
                if (underlay) {
                    mapGraphics.setPaint(wayColor.darker());
                    mapGraphics.setStroke(new BasicStroke((8)));
                } else {
                    mapGraphics.setPaint(wayColor);
                    mapGraphics.setStroke(new BasicStroke((4)));
                }
                drawPolyline(mapGraphics, wayNodes, bound, dictionary);
                break;

            case "4": //road
                wayColor = Color.WHITE;
                if (underlay) {
                    mapGraphics.setPaint(wayColor.darker());
                    mapGraphics.setStroke(new BasicStroke((8)));
                } else {
                    mapGraphics.setPaint(wayColor);
                    mapGraphics.setStroke(new BasicStroke((3)));
                }
                drawPolyline(mapGraphics, wayNodes, bound, dictionary);
                break;

            case "5": //rails
                mapGraphics.setStroke(new BasicStroke(2));
                mapGraphics.setPaint(Color.BLACK);
                drawPolyline(mapGraphics, wayNodes, bound, dictionary);
                break;

            case "7": //rivers
                mapGraphics.setStroke(new BasicStroke(3));
                mapGraphics.setPaint(new Color(102, 178, 255));
                drawPolyline(mapGraphics, wayNodes, bound, dictionary);
                break;

            case "10": //cycles
                mapGraphics.setStroke(new BasicStroke(2));
                mapGraphics.setPaint(Color.RED);
                drawPolyline(mapGraphics, wayNodes, bound, dictionary);
                break;

            case "6": //green
                mapGraphics.setStroke(new BasicStroke(2));
                inColor = new Color(153, 255, 153);
                outColor = new Color(102, 255, 102);
                drawArea(mapGraphics, wayNodes, bound, inColor, outColor, dictionary);
                break;

            case "9": //tree
                mapGraphics.setStroke(new BasicStroke(2));
                inColor = new Color(0, 204, 102);
                outColor = new Color(0, 153, 76);
                drawArea(mapGraphics, wayNodes, bound, inColor, outColor, dictionary);
                break;

            case "8": //waterbody
                mapGraphics.setStroke(new BasicStroke(2));
                inColor = new Color(153, 204, 255);
                outColor = new Color(102, 178, 255);
                drawArea(mapGraphics, wayNodes, bound, inColor, outColor, dictionary);
                break;
            }
    }

    private void drawPolyline(Graphics2D mapGraphics, long[] wayNodes, double[] bound, ConcurrentMap<Long, double[]> dictionary) {
        double[] u, v;
        for (int node = 0; node < wayNodes.length - 1; node++) {
            if (dictionary.containsKey(wayNodes[node]) && dictionary.containsKey(wayNodes[node + 1])) {
                u = dictionary.get(wayNodes[node]); //dictionary.get(wayNodes.get(node)); //efficiency by using previous v?
                v = dictionary.get(wayNodes[node + 1]);
//                System.out.println(u.toString());
                double uy = Math.abs(u[0] - bound[0]) * spaceModifierX;
                double ux = Math.abs(u[1] - bound[1]) * spaceModifierX;
                double vy = Math.abs(v[0] - bound[0]) * spaceModifierX;
                double vx = Math.abs(v[1] - bound[1]) * spaceModifierX;
                mapGraphics.drawLine((int) Math.round(ux), (int) Math.round(uy), (int) Math.round(vx), (int) Math.round(vy));
                linesDrawn++;
            }
        }
    }

    private void drawArea(Graphics2D mapGraphics, long[] wayNodes, double[] bound, Color inColor, Color outColor, ConcurrentMap<Long, double[]> dictionary){
        boolean first = true;
        GeneralPath waterPath = new GeneralPath(GeneralPath.WIND_EVEN_ODD, wayNodes.length - 1);
        for (int node = 0; node < wayNodes.length; node++) {
            if (dictionary.containsKey(wayNodes[node])) {
                double[] u = dictionary.get(wayNodes[node]); //efficiency by using previous v?
                double uy = Math.abs(u[0] - bound[0]) * spaceModifierX;
                double ux = Math.abs(u[1] - bound[1]) * spaceModifierX;
                if (first) {
                    waterPath.moveTo(ux, uy);
                    first = false;
                } else {
                    waterPath.lineTo(ux, uy);
                }
                linesDrawn++;
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

    public void saveMap(BufferedImage map, int x, int y, PngEncoder encoder){
        try {
            new File("draw/" + region + "/1").mkdirs();
            String filename = "draw/" + region + "/1/" + x + "-" + y;
            FileOutputStream fout = new FileOutputStream(filename);
//            ImageIO.write(map, "png", outputfile);
            encoder.encode(map, fout);

//            BufferedImage scaledImage = new BufferedImage(maxEdge / 2, maxEdge / 2, 1);
//            Graphics g = scaledImage.createGraphics();
//            g.drawImage(map, 0, 0, x / 2, y / 2, null);
//            g.dispose();

            for(int z = 2; z < 64; z = z * 2){
                new File("draw/" + region + "/" + z + "s/").mkdirs();
                map = Thumbnails.of(map)
                        .size(maxEdge/z, maxEdge/z)
                        .asBufferedImage();

//                filename = "draw/stitches/tile-".concat(String.valueOf(y)).concat("-").concat(String.valueOf(x)).concat("_").concat(Integer.toString(z)).concat(".png");
                filename = "draw/" + region + "/" + z + "s/" + x + "-" + y;
                fout = new FileOutputStream(filename);
//                ImageIO.write(map, "png", outputfile);
                encoder.encode(map, fout);
            }

        } catch (IOException e) {
            // handle exception
        }
    }

    public void combineTiles(BufferedImage[][] tiles) {
        String filename;
        File inputfile, outputfile;
        try{
            for(int z = 2; z < 128; z = z * 2){
                new File("draw/" + region + "/" + z + "/").mkdirs();
//                if(Math.max(tiles.length, tiles[0].length))
                System.out.println("z" + z);
                //need to wrap this bit in a loop over blocks - done below?
                for(int jumpY = 1; jumpY <= tiles.length + 1; jumpY = jumpY + z){
//                    System.out.println(tiles[0].length);
                    for(int jumpX = 1; jumpX <= tiles[0].length + 1; jumpX = jumpX + z){
                        BufferedImage[][] images = new BufferedImage[z][z];
                        System.out.println();
                        System.out.println("Drawing " + jumpX + "-" + jumpY);
                        for(int x = jumpX; x < z + jumpX; x++) {
                            for (int y = jumpY; y < z + jumpY; y++) {
                                System.out.println("x " + x + "y " + y);
                                filename = "draw/" + region + "/" + z + "s/" + x + "-" + y;
                                inputfile = new File(filename);
                                if(inputfile.exists()){
//                                    System.out.println((x - jumpX) + " " + (y - jumpY));
//                                    System.out.println("reading draw/stitches/tile-".concat(String.valueOf(y)).concat("-").concat(String.valueOf(x)).concat("_").concat(Integer.toString(z)).concat(".png"));
                                    images[x - jumpX][y - jumpY] = ImageIO.read(inputfile);
                                } else {
                                    System.out.println("Doesn't exist.");
                                    images[x - jumpX][y - jumpY] = new BufferedImage((maxEdge / z), (maxEdge/z), 1);
                                }
                            }
                        }
                        BufferedImage out = new BufferedImage(maxEdge, maxEdge, 1);
                        Graphics g = out.createGraphics();
                        int increment = maxEdge / z;
                        for(int x = 1; x <= z; x++) {
                            for (int y = 1; y <= z; y++) {
                                System.out.println("Drawing " + x + " " + y + " at " + ((x * increment) - increment) + " " + ((y * increment) - increment));
                                g.drawImage(images[x - 1][y - 1], (x * increment) - increment,  (y * increment) - increment,null);
                            }
                        }
                        filename = "draw/" + region + "/" + z + "/" + jumpX + "-" + jumpY;



//                        filename = "draw/".concat().concat(Integer.toString(z)).concat("/tile-L".concat(Integer.toString(z)).concat("_").concat(Integer.toString(jumpY)).concat("-").concat(Integer.toString(jumpX)).concat(".png"));
                        outputfile = new File(filename);
                        ImageIO.write(out, "png", outputfile);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
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
//            if(findingMax){
//                long lastId=0;
//                long lastLat=0;
//                long lastLon=0;
//                for (int i=0 ; i<nodes.getIdCount() ; i++) {
//                    lastId += nodes.getId(i);
//                    lastLat += nodes.getLat(i);
//                    lastLon += nodes.getLon(i);
//                    if(allNodes.containsKey(lastId)) {
//                        if (parseLat(lastLat) > northMost) {
//                            northMost = parseLat(lastLat);
//                            northMostNode = lastId;
//                        }
//                        if (parseLon(lastLon) < westMost) {
//                            westMost = parseLon(lastLon);
//                            westMostNode = lastId;
//                        }
//                        if (parseLat(lastLat) < southMost) {
//                            southMost = parseLat(lastLat);
//                            southMostNode = lastId;
//                        }
//                        if (parseLon(lastLon) > eastMost) {
//                            eastMost = parseLon(lastLon);
//                            eastMostNode = lastId;
//                        }
//                    }
//                }
//            }
            if(parsingNodes) {
                System.out.println(counter);
                System.out.println("time " + System.currentTimeMillis() / 1000);

//                System.out.println("Parsing dense nodes.");
                long lastId = 0;
                long lastLat = 0;
                long lastLon = 0;
                for (int i = 0; i < nodes.getIdCount(); i++) {

                    lastId += nodes.getId(i);
                    lastLat += nodes.getLat(i);
                    lastLon += nodes.getLon(i);
                    if (allNodes.containsKey(lastId)) {
                        counter++;

                        Double xOffset = Math.abs(parseLon(lastLon) - westMost);
                        Double yOffset = Math.abs(parseLat(lastLat) - northMost);

                        int x = (int) Math.floor(xOffset / interval);
                        int y = (int) Math.floor(yOffset / interval);

                        double[] tempDense = new double[2];
                        tempDense[0] = parseLat(lastLat);
                        tempDense[1] = parseLon(lastLon);

                        tileNodes.get(x).get(y).put(lastId, tempDense); //use pump?
                        for (long way : (allNodes.get(lastId))) {
                            if (way != 0) {

                                long[] wayNodesAndType = mapWays.get(way);
                                long[] wayNodes = new long[wayNodesAndType.length - 1];
                                long type = wayNodesAndType[wayNodesAndType.length - 1];
                                System.arraycopy(wayNodesAndType, 0, wayNodes, 0, wayNodes.length);
                                tileWays.get(x).get(y).put(new long[]{way, type}, wayNodes);

                            }
                        }
                    }
                }
            }
            nodes = null;
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
                if(counter != 0){
                    System.out.println(counter);
                }
//                System.out.println("Parsing ways.");
                for (Way w : ways) {
                    counter++;
//                    System.out.println(counter);
                    if(w.getId() == Long.parseLong("120403195")){
                        System.out.println("HERE --- " + w.getRefsList().size());
                    }
                    String key;
                    String value;
                    for (int i=0 ; i<w.getKeysCount() ; i++) {
                        key = getStringById(w.getKeys(i));
                        value = getStringById(w.getVals(i));
                        if(key.equals("highway")){
//                            System.out.println("GOT A ROAD");
                            if(value.matches("motorway|motorway_link")){
                                long[] wayBody = buildMyWay(w, Long.parseLong("0"));
                                long wayHeader = w.getId();
//                                tempWay.setType(WayType.ROAD);
//                                tempWay.setRoadType(RoadType.MOTORWAY);
                                mapWays.put(wayHeader, wayBody);
                            } else if (value.matches("trunk|trunk_link")){
                                long[] wayBody = buildMyWay(w, Long.parseLong("1"));
                                long wayHeader = w.getId();
//                                tempWay.setType(WayType.ROAD);
//                                tempWay.setRoadType(RoadType.TRUNK);
                                mapWays.put(wayHeader, wayBody);
                            } else if (value.matches("primary|primary_link")){
                                long[] wayBody = buildMyWay(w, Long.parseLong("2"));
                                long wayHeader = w.getId();
//                                tempWay.setRoadType(RoadType.PRIMARY);
                                mapWays.put(wayHeader, wayBody);
                            } else if (value.matches("secondary|secondary_link")){
                                long[] wayBody = buildMyWay(w, Long.parseLong("3"));
                                long wayHeader = w.getId();
//                                tempWay.setType(WayType.ROAD);
//                                tempWay.setRoadType(RoadType.SECONDARY);
                                mapWays.put(wayHeader, wayBody);
                            } else if (value.matches("tertiary|unclassified|residential|service|tertiary_link|road")){
                                long[] wayBody = buildMyWay(w, Long.parseLong("4"));
                                long wayHeader = w.getId();
//                                tempWay.setType(WayType.ROAD);
//                                tempWay.setRoadType(RoadType.ROAD);
                                mapWays.put(wayHeader, wayBody);
                            }
                        }
                        if(key.equals("railway")){
                            long[] wayBody = buildMyWay(w, Long.parseLong("5"));
                            long wayHeader = w.getId();
//                            tempWay.setType(WayType.RAILWAY);
                            mapWays.put(wayHeader, wayBody);
                        }
                        if((key.equals("natural") && value.equals("grass"))
                                || (key.equals("leisure") && value.equals("common"))
                                || (key.equals("leisure") && value.equals("park"))
                                || (key.equals("leisure") && value.equals("golf_course"))
                                || value.equals("meadow")
                                || value.equals("recreation_ground")
                                || value.equals("conservation")
                                || value.equals("park")){
                            long[] wayBody = buildMyWay(w, Long.parseLong("6"));
                            long wayHeader = w.getId();
//                            tempWay.setType(WayType.GREEN);
                            mapWays.put(wayHeader, wayBody);
                        }
                        if(key.equals("waterway") && (value.matches("river|stream|canal"))){
                            long[] wayBody = buildMyWay(w, Long.parseLong("7"));
                            long wayHeader = w.getId();
//                            tempWay.setType(WayType.WATERWAY);
                            mapWays.put(wayHeader, wayBody);
                        }
                        if((key.equals("natural") && value.equals("water"))
                                || value.matches("reservoir|basin")){
                            long[] wayBody = buildMyWay(w, Long.parseLong("8"));
                            long wayHeader = w.getId();
//                            tempWay.setType(WayType.WATERBODY);
                            mapWays.put(wayHeader, wayBody);
                        }
                        if((key.equals("natural") && value.equals("wood"))
                                || (key.equals("landuse") && value.equals("forest"))){
                            long[] wayBody = buildMyWay(w, Long.parseLong("9"));
                            long wayHeader = w.getId();
//                            tempWay.setType(WayType.TREE);
                            mapWays.put(wayHeader, wayBody);
                        }
                        if(key.equals("cycleway") || value.equals("cycleway") ||
                                (key.equals("route") && value.equals("bicycle"))){
                            long[] wayBody = buildMyWay(w, Long.parseLong("10"));
                            long wayHeader = w.getId();
//                            tempWay.setType(WayType.CYCLE);
                            mapWays.put(wayHeader, wayBody);
                        }
                    }
                }
                if(!putWayNodes.isEmpty()){
                    allNodes.putAll(putWayNodes);
                    putWayNodes.clear();
                }
            }
            ways = null;
        }

        private long[] buildMyWay(Way w, long type){
            long[] tempWay = new long[w.getRefsList().size() + 1];
            long id = w.getId();
            long lastRef = 0;
            int wayCtr = 0;
            for (Long ref : w.getRefsList()) {
                lastRef += ref;
                tempWay[wayCtr] = lastRef;
                if (allNodes.get(lastRef) == null) { //used to be != ... hmmmm
                    long[] wayset = new long[10];
                    wayset[0] = id;
                    putWayNodes.put(lastRef, wayset);
                } else {
                    long[] wayset = allNodes.get(lastRef);
                    INNER: for(int x = 0; x < wayset.length; x++){
                        if(wayset[x] == Long.parseLong("0")){
                            wayset[x] = id;
                            break INNER;
                        }
                    }
                    putWayNodes.put(lastRef, wayset);
                }
                wayCtr++;
            }
            tempWay[wayCtr] = type;
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


    private static void drawArray(){
        for(int y = 0; y < bounds[0].length; y++){
            for(int x = 0; x < bounds.length; x++){
                System.out.print("(" + bounds[x][y][0] + " " + bounds[x][y][1] + ")      ");
            }
            System.out.println();
        }
        System.out.println();
    }

    private void serialise(Object obj, String name) throws IOException {
        FileOutputStream fio = new FileOutputStream(name);
        ObjectOutputStream objo = new ObjectOutputStream(fio);
        objo.writeObject(obj);
        objo.close();
        fio.close();
    }

    private HashMap<Long, long[]> readHash(String name) throws IOException, ClassNotFoundException {
        FileInputStream fii = new FileInputStream(name);
        ObjectInputStream obji = new ObjectInputStream(fii);
        HashMap<Long, long[]> hashMap = (HashMap<Long, long[]>) obji.readObject();
        return hashMap;
    }

    private void timerStart(){
        startTime = System.nanoTime();
    }

    private void timerEnd(String string){
        endTime = System.nanoTime();
        System.out.println(string + " time: " + (((float) endTime - (float)startTime) / 1000000000));
    }
}
