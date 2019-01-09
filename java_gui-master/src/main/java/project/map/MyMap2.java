package project.map;

import com.objectplanet.image.PngEncoder;
import crosby.binary.BinaryParser;
import crosby.binary.Osmformat.*;
import crosby.binary.file.BlockInputStream;
import crosby.binary.file.BlockReaderAdapter;
import net.coobird.thumbnailator.Thumbnails;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

//import gnu.trove.map.hash.THashMap;
//import gnu.trove.set.hash.THashSet;

//import org.mapdb.*;

public class MyMap2 {
    private static HashMap<Long, long[]> putWayNodes;
    private static double northMost, westMost, southMost, eastMost;
    private static long northMostNode, westMostNode, southMostNode, eastMostNode;
    private double spaceModifierX;
    private double spaceModifierY;
    private static double interval;
    private static boolean parsingNodes, tile;
    private static int counter;
    private static int linesDrawn, maxEdge;
    public static int bX1, bX2, bY1, bY2;
    public static double xLow, xHigh, yLow, yHigh;
    public long startTime, endTime;
    public int level;
    public static HashMap<Long, double[]> tileNodes;
    public static HashMap<long[], long[]> tileWays;

    private static File file;
    public static double[][][] bounds;

    private String region;


    public MyMap2(File file) throws IOException {
        parsingNodes = true;
        InputStream input = new FileInputStream(file);
        BlockReaderAdapter brad = new TestBinaryParser();
        timerStart();
        new BlockInputStream(input, brad).process();
        timerEnd("Testing");

    }

    public MyMap2(File file, String region, int maxEdge, boolean alreadyFiled) throws IOException{

        this.maxEdge = maxEdge; //max edge length of an image

        this.region = region;

        counter = 0;
        this.file = file;
        int scale = 40000; //pixels per degree!


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


        int xDimension = (int) Math.ceil((scale * width) / maxEdge) + 1;
        int yDimension = (int) Math.ceil((scale * height) / maxEdge) + 1;

        BufferedImage[][] tiles = new BufferedImage[(int) Math.ceil((scale * width) / maxEdge)][(int) Math.ceil((scale * height) / maxEdge)];
        bounds = new double[(int) Math.ceil((scale * width) / maxEdge) + 1][(int) Math.ceil((scale * height) / maxEdge) + 1][2];

        PngEncoder encoder = new PngEncoder();

        tileNodes = new HashMap<>();
        tileWays = new HashMap<>();

        InputStream input = new FileInputStream(file);
        BlockReaderAdapter brad = new TestBinaryParser();
        BlockInputStream reader = new BlockInputStream(input, brad);

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
//                    System.out.print("(" + x + " " + y + " " + bounds[x][y][0] + " " + bounds[x][y][1] + ")");

//                    drawArray();
                }
            }
        }

        int j = 8;

        counter = 0;
        for(int x = 1; x < xDimension; x = x + j){
            for(int y = 1; y < yDimension; y = y + j){
                    bX1 = x;
                    bX2 = x + j;
                    bY1 = y;
                    bY2 = y + j;
                    System.out.println("bX1: " + bX1 + " bX2: " + bX2 + " bY1: " + bY1 + " bY2: " + bY2);
                    tileNodes.clear();
                    parsingNodes = true;
                    input = new FileInputStream(file);
                    brad = new TestBinaryParser();
                    reader = new BlockInputStream(input, brad);
                    timerStart();
                    reader.process(); //collect nodes in tile
                    timerEnd("Reading nodes");
                    System.out.println("Nodes found: " + tileNodes.size());

                    tileWays.clear();
                    parsingNodes = false;
                    input = new FileInputStream(file);
                    brad = new TestBinaryParser();
                    reader = new BlockInputStream(input, brad);
                    timerStart();
                    reader.process(); //collect ways in tile
                    timerEnd("Reading ways");
                    System.out.println("Ways found: " + tileWays.size());

                    for(int xDraw = x; xDraw < x + j; xDraw++){
                        for(int yDraw = y; yDraw < y + j; yDraw++){
                            counter++;
                            System.out.println("Drawing " + counter + " of " + ((bounds.length - 1) * (bounds[0].length - 1)) + " (" + (int) ((counter / ((bounds.length - 1f) * (bounds[0].length - 1f))) * 100) + "%)");
                            startTime = System.nanoTime();
                            BufferedImage tile = drawTile(xDraw, yDraw, maxEdge);
                            endTime = System.nanoTime();
                            System.out.print("Draw time: " + (((float) endTime - (float)startTime) / 1000000000));
                            System.out.print(". Number of ways: " + linesDrawn + ", " + ((linesDrawn + 1) / (((float) endTime - (float)startTime) / 1000000000)) + "w/s");

                            startTime = System.nanoTime();
                            saveMap(tile, xDraw, yDraw, encoder);
                            endTime = System.nanoTime();
                            System.out.println(". Save time: " + (((float) endTime - (float)startTime) / 1000000000));
                            System.out.println();
                        }
                    }

            }
        }


        combineTiles(tiles);

    }


    public BufferedImage drawTile(int x, int y, int edge){
//        System.out.print("Drawing tile. ");
        double[] axis = new double[2];

        BufferedImage tile = new BufferedImage(edge, edge, 1);

        Graphics2D mapGraphics = tile.createGraphics();

        mapGraphics.setColor(new Color(244, 243, 236));
        mapGraphics.fillRect(0, 0, maxEdge, maxEdge);
        BasicStroke bs = new BasicStroke(1);
        mapGraphics.setStroke(bs);

        try{
            if(x == 0){
                axis[1] = westMost;
            } else { axis[1] = bounds[x][y][1];}

            if(y == 0){
                axis[0] = northMost;
            } else { axis[0] = bounds[x][y][0];}
        }catch(ArrayIndexOutOfBoundsException e){
            mapGraphics.dispose();
            return tile;
        }

        linesDrawn = 0;


        System.out.println(x + " " + y + " is this big " + tileWays.size());
        System.out.println(axis[0] + " " + axis[1]);

        for(Map.Entry<long[], long[]> w : (tileWays.entrySet())){
            if(w.getKey()[0] == Long.parseLong("22815916")){
                System.out.println("found ref draw");
            }
            if(w.getKey()[1] == Long.parseLong("6")) {
                drawWay(w, mapGraphics, false, axis, tileNodes);
            }
        } //GREENS

        for(Map.Entry<long[], long[]> w : (tileWays.entrySet())){
            if(w.getKey()[1] == Long.parseLong("9")) {
                drawWay(w, mapGraphics, false, axis, tileNodes);
            }
        } //FORESTS

        for(Map.Entry<long[], long[]> w : (tileWays.entrySet())){
            if(w.getKey()[1] == Long.parseLong("7")) {
                drawWay(w, mapGraphics, false, axis, tileNodes);
            }
        } //RAILS

        for(Map.Entry<long[], long[]> w : (tileWays.entrySet())){
            if(w.getKey()[1] == Long.parseLong("8")) {
                drawWay(w, mapGraphics, false, axis, tileNodes);
            }
        } //WATERBODIES

        for(Map.Entry<long[], long[]> w : (tileWays.entrySet())){
            if(w.getKey()[1] == Long.parseLong("5")) {
                drawWay(w, mapGraphics, false, axis, tileNodes);
            }
        } //RAILWAYS

//        for(Map.Entry<long[], long[]> w : (tileWasy.entrySet())){
//            if(w.getKey()[1] == (Long.parseLong("0")) || w.getKey()[1] == (Long.parseLong("1")) || w.getKey()[1] == (Long.parseLong("2")) || w.getKey()[1] == (Long.parseLong("3")) || w.getKey()[1] == (Long.parseLong("4"))) {
//                drawWay(w, mapGraphics, true, axis, tileNodes.get(x - 1).get(y - 1));
//            }
//        } //ROADS UNDER

        for(Map.Entry<long[], long[]> w : (tileWays.entrySet())){
            if(w.getKey()[1] == (Long.parseLong("0")) || w.getKey()[1] == (Long.parseLong("1")) || w.getKey()[1] == (Long.parseLong("2")) || w.getKey()[1] == (Long.parseLong("3")) || w.getKey()[1] == (Long.parseLong("4"))) {
                drawWay(w, mapGraphics, false, axis, tileNodes);
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


    private void drawWay(Map.Entry<long[], long[]> way, Graphics2D mapGraphics, boolean underlay, double[] bound, HashMap<Long, double[]> dictionary) {
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

    private void drawPolyline(Graphics2D mapGraphics, long[] wayNodes, double[] bound, HashMap<Long, double[]> dictionary) {
        double[] u, v;
        for (int node = 0; node < wayNodes.length - 1; node++) {
            if (tileNodes.containsKey(wayNodes[node]) && tileNodes.containsKey(wayNodes[node + 1])) {
                u = tileNodes.get(wayNodes[node]); //dictionary.get(wayNodes.get(node)); //efficiency by using previous v?
                v = tileNodes.get(wayNodes[node + 1]);
                double uy = (u[0] - bound[0]) * spaceModifierX;
                double ux = (u[1] - bound[1]) * spaceModifierX;
                double vy = (v[0] - bound[0]) * spaceModifierX;
                double vx = (v[1] - bound[1]) * spaceModifierX;
                mapGraphics.drawLine((int) Math.round(ux), (int) Math.round(uy), (int) Math.round(vx), (int) Math.round(vy));

                linesDrawn++;
            }
        }
    }

    private void drawArea(Graphics2D mapGraphics, long[] wayNodes, double[] bound, Color inColor, Color outColor, HashMap<Long, double[]> dictionary){
        boolean first = true;
        GeneralPath waterPath = new GeneralPath(GeneralPath.WIND_EVEN_ODD, wayNodes.length - 1);
        for (int node = 0; node < wayNodes.length; node++) {
            if (dictionary.containsKey(wayNodes[node])) {
                double[] u = dictionary.get(wayNodes[node]); //efficiency by using previous v?
                double uy = (u[0] - bound[0]) * spaceModifierX;
                double ux = (u[1] - bound[1]) * spaceModifierX;
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
            String filename = "draw/" + region + "/1/" + x + "-" + y + ".png";
            FileOutputStream fout = new FileOutputStream(filename);
//            ImageIO.write(map, "png", outputfile);
            encoder.encode(map, fout);

//            BufferedImage scaledImage = new BufferedImage(maxEdge / 2, maxEdge / 2, 1);
//            Graphics g = scaledImage.createGraphics();
//            g.drawImage(map, 0, 0, x / 2, y / 2, null);
//            g.dispose();

//            for(int z = 2; z < 64; z = z * 2){
//                new File("draw/" + region + "/" + z + "s/").mkdirs();
//                map = Thumbnails.of(map)
//                        .size(maxEdge/z, maxEdge/z)
//                        .asBufferedImage();
//
////                filename = "draw/stitches/tile-".concat(String.valueOf(y)).concat("-").concat(String.valueOf(x)).concat("_").concat(Integer.toString(z)).concat(".png");
//                filename = "draw/" + region + "/" + z + "s/" + x + "-" + y;
//                fout = new FileOutputStream(filename);
////                ImageIO.write(map, "png", outputfile);
//                encoder.encode(map, fout);
//            }

        } catch (IOException e) {
            // handle exception
        }
    }

    public void combineTiles(BufferedImage[][] tiles) {
        System.out.println("Tiles.length: " + tiles.length + " tiles[0].length: " + tiles[0].length);
        String filename;
        File inputfile, outputfile;
        BufferedImage map;
        try{
            for(int z = 2; z < 128; z = z * 2){
                new File("draw/" + region + "/" + z + "/").mkdirs();
//                if(Math.max(tiles.length, tiles[0].length))
                System.out.println("z" + z);
                //need to wrap this bit in a loop over blocks - done below?
                for(int jumpY = 1; jumpY <= tiles[0].length + 1; jumpY = jumpY + z){
//                    System.out.println(tiles[0].length);
                    for(int jumpX = 1; jumpX <= tiles.length + 1; jumpX = jumpX + z){

                        BufferedImage[][] images = new BufferedImage[z][z];
                        System.out.println();
                        System.out.println("Drawing " + jumpX + "-" + jumpY);
                        for(int x = jumpX; x < z + jumpX; x = x + (z / 2)) {
                            for (int y = jumpY; y < z + jumpY; y = y + (z / 2)) {
                                System.out.println("x " + x + "y " + y);
                                filename = "draw/" + region + "/" + (z / 2) + "/" + x + "-" + y + ".png";
                                inputfile = new File(filename);
                                if(inputfile.exists()){
//                                    System.out.println((x - jumpX) + " " + (y - jumpY));
//                                    System.out.println("reading draw/stitches/tile-".concat(String.valueOf(y)).concat("-").concat(String.valueOf(x)).concat("_").concat(Integer.toString(z)).concat(".png"));
                                    timerStart();
                                    images[x - jumpX][y - jumpY] = ImageIO.read(inputfile);
                                    timerEnd("Load image");
                                } else {
                                    System.out.println(filename + " doesn't exist.");
                                    images[x - jumpX][y - jumpY] = new BufferedImage((maxEdge / z), (maxEdge/z), 1);
                                }
                            }
                        }
                        BufferedImage out = new BufferedImage(maxEdge, maxEdge, 1);
                        Graphics g = out.createGraphics();
                        int increment = maxEdge / z;
                        for(int x = 1; x <= z; x = x + (z / 2)) {
                            for (int y = 1; y <= z; y = y + (z / 2)) {
                                map = Thumbnails.of(images[x - 1][y - 1])
                                        .size(maxEdge/2, maxEdge/2)
                                        .asBufferedImage();
                                g.drawImage(map, (x * increment) - increment,  (y * increment) - increment,null);
                            }
                        }
                        filename = "draw/" + region + "/" + z + "/" + jumpX + "-" + jumpY + ".png";
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
            if(parsingNodes) {
//                System.out.println("Parsing nodes.");
                long lastId = 0;
                long lastLat = 0;
                long lastLon = 0;
                for (int i = 0; i < nodes.getIdCount(); i++) {
                    lastId += nodes.getId(i);
                    lastLat += nodes.getLat(i);
                    lastLon += nodes.getLon(i);
//                    System.out.println("lastId " + lastId);
//                    System.out.println("lastLon " + parseLon(lastLon));
                    Double xOffset = Math.abs(parseLon(lastLon) - westMost);
                    Double yOffset = Math.abs(parseLat(lastLat) - northMost);
//                    System.out.println("xOffset " + xOffset);
                    int x = (int) Math.floor(xOffset / interval);
                    int y = (int) Math.floor(yOffset / interval);
//                    System.out.println("x " + x);
                    if((x >= bX1 && x < bX2) && (y >= bY1 && y < bY2)){
//                        System.out.println("FOUND");
                        double[] tempDense = new double[2];
                        tempDense[0] = parseLat(lastLat);
                        tempDense[1] = parseLon(lastLon);
                        tileNodes.put(lastId, tempDense);
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
            if (!parsingNodes) {
//                System.out.println("Parsing ways.");
                for (Way w : ways) {
                    long lastRef = 0;
                    WAY: for (Long ref : w.getRefsList()) {
                        lastRef += ref;
                        if (tileNodes.containsKey(lastRef)) {
                            if(w.getId() == Long.parseLong("22815916")){
                                System.out.println("found ref at x " + bX1 + " y " + bY1);
                            }
                            String key;
                            String value;
                            for (int i = 0; i < w.getKeysCount(); i++) {
                                key = getStringById(w.getKeys(i));
                                value = getStringById(w.getVals(i));
                                if (key.equals("highway")) {
//                            System.out.println("GOT A ROAD");
                                    if (value.matches("motorway|motorway_link")) {
                                        long[] wayBody = buildMyWay(w, Long.parseLong("0"));
                                        long[] wayHeader = new long[]{w.getId(), Long.parseLong("0")};
//                                tempWay.setType(WayType.ROAD);
//                                tempWay.setRoadType(RoadType.MOTORWAY);
                                        tileWays.put(wayHeader, wayBody);
                                    } else if (value.matches("trunk|trunk_link")) {
                                        long[] wayBody = buildMyWay(w, Long.parseLong("1"));
                                        long[] wayHeader = new long[]{w.getId(), Long.parseLong("1")};
//                                tempWay.setType(WayType.ROAD);
//                                tempWay.setRoadType(RoadType.TRUNK);
                                        tileWays.put(wayHeader, wayBody);
                                    } else if (value.matches("primary|primary_link")) {
                                        long[] wayBody = buildMyWay(w, Long.parseLong("2"));
                                        long[] wayHeader = new long[]{w.getId(), Long.parseLong("2")};
//                                tempWay.setRoadType(RoadType.PRIMARY);
                                        tileWays.put(wayHeader, wayBody);
                                    } else if (value.matches("secondary|secondary_link")) {
                                        long[] wayBody = buildMyWay(w, Long.parseLong("3"));
                                        long[] wayHeader = new long[]{w.getId(), Long.parseLong("3")};
//                                tempWay.setType(WayType.ROAD);
//                                tempWay.setRoadType(RoadType.SECONDARY);
                                        tileWays.put(wayHeader, wayBody);
                                    } else if (value.matches("tertiary|unclassified|residential|service|tertiary_link|road")) {
                                        long[] wayBody = buildMyWay(w, Long.parseLong("4"));
                                        long[] wayHeader = new long[]{w.getId(), Long.parseLong("4")};
//                                tempWay.setType(WayType.ROAD);
//                                tempWay.setRoadType(RoadType.ROAD);
                                        tileWays.put(wayHeader, wayBody);
                                    }
                                }
                                if (key.equals("railway")) {
                                    long[] wayBody = buildMyWay(w, Long.parseLong("5"));
                                    long[] wayHeader = new long[]{w.getId(), Long.parseLong("5")};
//                            tempWay.setType(WayType.RAILWAY);
                                    tileWays.put(wayHeader, wayBody);
                                }
                                if ((key.equals("natural") && value.equals("grass"))
                                        || (key.equals("leisure") && value.equals("common"))
                                        || (key.equals("leisure") && value.equals("park"))
                                        || (key.equals("leisure") && value.equals("golf_course"))
                                        || value.equals("meadow")
                                        || value.equals("recreation_ground")
                                        || value.equals("conservation")
                                        || value.equals("park")) {
                                    long[] wayBody = buildMyWay(w, Long.parseLong("6"));
                                    long[] wayHeader = new long[]{w.getId(), Long.parseLong("6")};
//                            tempWay.setType(WayType.GREEN);
                                    tileWays.put(wayHeader, wayBody);
                                }
                                if (key.equals("waterway") && (value.matches("river|stream|canal"))) {
                                    long[] wayBody = buildMyWay(w, Long.parseLong("7"));
                                    long[] wayHeader = new long[]{w.getId(), Long.parseLong("7")};
//                            tempWay.setType(WayType.WATERWAY);
                                    tileWays.put(wayHeader, wayBody);
                                }
                                if ((key.equals("natural") && value.equals("water"))
                                        || value.matches("reservoir|basin")) {
                                    long[] wayBody = buildMyWay(w, Long.parseLong("8"));
                                    long[] wayHeader = new long[]{w.getId(), Long.parseLong("8")};
//                            tempWay.setType(WayType.WATERBODY);
                                    tileWays.put(wayHeader, wayBody);
                                }
                                if ((key.equals("natural") && value.equals("wood"))
                                        || (key.equals("landuse") && value.equals("forest"))) {
                                    long[] wayBody = buildMyWay(w, Long.parseLong("9"));
                                    long[] wayHeader = new long[]{w.getId(), Long.parseLong("9")};
//                            tempWay.setType(WayType.TREE);
                                    tileWays.put(wayHeader, wayBody);
                                }
                                if (key.equals("cycleway") || value.equals("cycleway") ||
                                        (key.equals("route") && value.equals("bicycle"))) {
                                    long[] wayBody = buildMyWay(w, Long.parseLong("10"));
                                    long[] wayHeader = new long[]{w.getId(), Long.parseLong("10")};
//                            tempWay.setType(WayType.CYCLE);
                                    tileWays.put(wayHeader, wayBody);
                                }
                            }
                            break WAY;
                        }
                    }
                }
            }
        }

        private long[] buildMyWay(Way w, long type){
            long[] tempWay = new long[w.getRefsList().size()];
            long lastRef = 0;
            int wayCtr = 0;
            for (Long ref : w.getRefsList()) {
                lastRef += ref;
                tempWay[wayCtr] = lastRef;
                wayCtr++;
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
