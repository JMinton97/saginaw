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
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

//import gnu.trove.map.hash.THashMap;
//import gnu.trove.set.hash.THashSet;

//import org.mapdb.*;

public class MyMap2 {
    private static double northMost, westMost, southMost, eastMost;
    private double spaceModifierX;
    private double spaceModifierY;
    private static double interval;
    private static boolean parsingNodes;
    private static boolean parsingPlaces;
    private static boolean parsingWays;
    private static int counter;
    private static int linesDrawn, maxEdge;
    public static int bX1, bX2, bY1, bY2;
    public long startTime, endTime;
    public static HashMap<Long, double[]> tileNodes;
    public static HashMap<Long, MyWay>[][] tileWays;
    private int xDimension, yDimension;
    private int scale;
    private double height, width;
    private Point2D.Double centre, origin;
    private static ArrayList<Place> cities;
    private static ArrayList<Place> towns;

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

    public MyMap2(File file, String region, int maxEdge, boolean alreadyFiled) throws IOException {

        this.maxEdge = maxEdge; //max edge length of an image

        this.region = region;

        counter = 0;
        this.file = file;
        scale = 40000; //pixels per degree!

        if (region == "wales") {
            northMost = 53.5; //56;
            westMost = -5.5; //-6;          //WALES
            southMost = 51.3; //49.5;
            eastMost = -2.5; //2;
        } else if (region == "england") {
            northMost = 56;
            westMost = -6;
            southMost = 49.5;   //IT"S COMING HOME
            eastMost = 2;

//            northMost = 52.66;
//            westMost = -2.24;
//            southMost = 49.5;   //IT"S COMING HOME
//            eastMost = 2;
        } else if (region == "france") {
            northMost = 51.1;
            westMost = -5.3;
            southMost = 42.3;   //FRANCE
            eastMost = 8.4;
        } else if (region == "birmingham") {
            northMost = 52.620580;
            westMost = -2.240133;
            southMost = 52.336874;   //BIRMINGHAM
            eastMost = -1.655798;
        }

        counter = 0;

        if (northMost > southMost) {
            height = Math.abs(northMost - southMost);
        } else {
            height = Math.abs(southMost - northMost);
        }

        if (eastMost > westMost) {
            width = Math.abs(eastMost - westMost);
        } else {
            width = Math.abs(westMost - eastMost);
        }

        centre = new Point2D.Double(westMost + (width / 2), northMost - (height / 2));
        origin = new Point2D.Double(westMost, northMost);

        System.out.println(centre);

//        System.out.println("Total map width in px: " + scale * width);


        interval = width / ((scale * width) / maxEdge);
        spaceModifierX = maxEdge / interval;


        xDimension = (int) Math.ceil((scale * width) / maxEdge);
        yDimension = (int) Math.ceil((scale * height) / maxEdge) + 1;
        System.out.println("SIZE " + xDimension + " " + yDimension);

        FileInputStream fis = null;
        ObjectInputStream in = null;
        try {
            fis = new FileInputStream("files/" + region + "/towns.ser");
            in = new ObjectInputStream(fis);
            towns = (ArrayList<Place>) in.readObject();
            in.close();
            fis = new FileInputStream("files/" + region + "/cities.ser");
            in = new ObjectInputStream(fis);
            cities = (ArrayList<Place>) in.readObject();
            in.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void draw() throws IOException{

        BufferedImage[][] tiles = new BufferedImage[(int) Math.ceil((scale * width) / maxEdge)][(int) Math.ceil((scale * height) / maxEdge)];
        bounds = new double[(int) Math.ceil((scale * width) / maxEdge) + 1][(int) Math.ceil((scale * height) / maxEdge) + 1][2];

        PngEncoder encoder = new PngEncoder();

        int j = 32;

        tileNodes = new HashMap<>();
        tileWays = new HashMap[j][j];
        towns = new ArrayList<>();
        cities = new ArrayList<>();

        parsingPlaces = true;
        parsingWays = false;
        parsingNodes = false;
        InputStream placeInput = new FileInputStream(file);
        BlockReaderAdapter placeBrad = new TestBinaryParser();
        BlockInputStream placeReader = new BlockInputStream(placeInput, placeBrad);

        placeReader.process();

        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        try {
            fos = new FileOutputStream("files/" + region + "/towns.ser");
            out = new ObjectOutputStream(fos);
            out.writeObject(towns);
            out.close();
            fos = new FileOutputStream("files/" + region + "/cities.ser");
            out = new ObjectOutputStream(fos);
            out.writeObject(cities);
            out.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

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

        counter = 0;
        System.out.println("up to " + xDimension);
        for(int x = 0; x < xDimension; x = x + j){
            for(int y = 0; y < yDimension; y = y + j){
                bX1 = x;
                bX2 = x + j;
                bY1 = y;
                bY2 = y + j;
                System.out.println("bX1: " + bX1 + " bX2: " + bX2 + " bY1: " + bY1 + " bY2: " + bY2);
                tileNodes.clear();
                parsingNodes = true;
                parsingPlaces = false;
                parsingWays = false;
                input = new FileInputStream(file);
                brad = new TestBinaryParser();
                reader = new BlockInputStream(input, brad);
                timerStart();
                reader.process(); //collect nodes in tile
                timerEnd("Reading nodes");
                System.out.println("Nodes found: " + tileNodes.size());
                if(tileNodes.size() > 0){
                    for(int jx = 0; jx < tileWays.length; jx++){
                        for(int jy = 0; jy < tileWays[0].length; jy++){
                            tileWays[jx][jy] = new HashMap<>();
                        }
                    }
                    parsingNodes = false;                                   //or just have a jxj array of hashmaps and put it into the appropriate one.
                    parsingWays = true;
                    parsingPlaces = false;
                    input = new FileInputStream(file);
                    brad = new TestBinaryParser();
                    reader = new BlockInputStream(input, brad);
                    timerStart();
                    reader.process(); //collect ways in tile
                    timerEnd("Reading ways");
//                        System.out.println("Ways found: " + tileWays.size());
                } else {
                    System.out.println("Skipping way read.");
                }

                for(int xDraw = x; xDraw < x + j; xDraw++){
                    for(int yDraw = y; yDraw < y + j; yDraw++){
                        counter++;
                        System.out.println("Drawing " + counter + " of " + ((bounds.length) * (bounds[0].length)) + " (" + (int) ((counter / ((bounds.length - 1f) * (bounds[0].length - 1f))) * 100) + "%)");
                        if(tileWays[xDraw - x][yDraw - y].size() == 0){
                            System.out.println("Nothing to draw!");
                        } else {
                            startTime = System.nanoTime();
                            BufferedImage tile = drawTile(xDraw, yDraw, x, y, maxEdge);
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
        }


        combineTiles(tiles);

    }


    public BufferedImage drawTile(int x, int y, int xO, int yO, int edge){
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

        HashMap<Long, MyWay> subTileWays = tileWays[x - xO][y - yO];
        System.out.println("This many ways stored: " + subTileWays.size());

        for(MyWay w : subTileWays.values()){
            if(w.getType() == WayType.CITY){
                drawWay(w, mapGraphics, false, axis, tileNodes);
            }
        } //URBAN AREAS

        for(MyWay w : subTileWays.values()){
            if(w.getType() == WayType.GREEN){
                drawWay(w, mapGraphics, false, axis, tileNodes);
            }
        } //GREENS

        for(MyWay w : subTileWays.values()){
            if(w.getType() == WayType.MOOR){
                drawWay(w, mapGraphics, false, axis, tileNodes);
            }
        } //MOORS

        for(MyWay w : subTileWays.values()){
            if(w.getType() == WayType.TREE){
                drawWay(w, mapGraphics, false, axis, tileNodes);
            }
        } //FORESTS



        for(MyWay w : subTileWays.values()){
            if(w.getType() == WayType.WATERWAY){
                drawWay(w, mapGraphics, false, axis, tileNodes);
            }
        } //RIVERS

        for(MyWay w : subTileWays.values()){
            if(w.getType() == WayType.WATERBODY){
                drawWay(w, mapGraphics, false, axis, tileNodes);
            }
        } //LAKES

        for(MyWay w : subTileWays.values()){
            if(w.getType() == WayType.RAILWAY){
                drawWay(w, mapGraphics, false, axis, tileNodes);
            }
        } //RAILWAYS

//        for(MyWay w : subTileWays){
//            if(w.getType() == WayType.ROAD){
//                drawWay(w, mapGraphics, false, axis, tileNodes);
//            }
//        } //ROADS UNDER

        for(MyWay w : subTileWays.values()){
            if(w.getType() == WayType.ROAD){
                drawWay(w, mapGraphics, false, axis, tileNodes);
            }
        } //ROADS OVER

//        for(MyWay w : subTileWays){
//            if(w.getType() == WayType.CYCLE){
//                drawWay(w, mapGraphics, false, axis, tileNodes);
//            }
//        }

        mapGraphics.dispose();

        return tile;
    }


    private void drawWay(MyWay way, Graphics2D mapGraphics, boolean underlay, double[] bound, HashMap<Long, double[]> dictionary) {
//        System.out.println("Drawing way " + way.getKey()[0]);
        Color wayColor = Color.WHITE;
        Color inColor;
        Color outColor;
        long[] wayNodes = way.getWayNodes();
//                List<Long> allNodes = DouglasPeucker.decimate(way.getWayNodes(), 0.0001, dictionary);
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
                }
                drawPolyline(mapGraphics, wayNodes, bound, dictionary);
                break;

            case RAILWAY:
                mapGraphics.setStroke(new BasicStroke(2));
                mapGraphics.setPaint(Color.BLACK);
                drawPolyline(mapGraphics, wayNodes, bound, dictionary);
                break;

            case WATERWAY:
                mapGraphics.setStroke(new BasicStroke(4));
                mapGraphics.setPaint(new Color(102, 178, 255));
                drawPolyline(mapGraphics, wayNodes, bound, dictionary);
                break;

            case CYCLE:
                mapGraphics.setStroke(new BasicStroke(4));
                mapGraphics.setPaint(Color.RED);
                drawPolyline(mapGraphics, wayNodes, bound, dictionary);
                break;

            case GREEN:
                mapGraphics.setStroke(new BasicStroke(4));
                inColor = new Color(153, 255, 153);
                outColor = new Color(102, 255, 102);
                drawArea(mapGraphics, wayNodes, bound, inColor, outColor, dictionary);
                break;

            case TREE:
                mapGraphics.setStroke(new BasicStroke(4));
                inColor = new Color(0, 204, 102);
                outColor = new Color(0, 153, 76);
                drawArea(mapGraphics, wayNodes, bound, inColor, outColor, dictionary);
                break;

            case WATERBODY:
                mapGraphics.setStroke(new BasicStroke(4));
                inColor = new Color(153, 204, 255);
                outColor = new Color(102, 178, 255);
                drawArea(mapGraphics, wayNodes, bound, inColor, outColor, dictionary);
                break;

            case CITY:
                mapGraphics.setStroke(new BasicStroke(4));
                inColor = new Color(185, 179, 177);
                outColor = inColor;
                drawArea(mapGraphics, wayNodes, bound, inColor, outColor, dictionary);
                break;

            case MOOR:
                mapGraphics.setStroke(new BasicStroke(4));
                inColor = new Color(143, 185, 106);
                outColor = inColor;
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
                double uy = (bound[0] - u[0]) * spaceModifierX;
                double ux = (u[1] - bound[1]) * spaceModifierX;
                double vy = (bound[0] - v[0]) * spaceModifierX;
                double vx = (v[1] - bound[1]) * spaceModifierX;
                mapGraphics.drawLine((int) Math.round(ux), (int) Math.round(uy), (int) Math.round(vx), (int) Math.round(vy));
            }
        }
        linesDrawn++;
    }

    private void drawArea(Graphics2D mapGraphics, long[] wayNodes, double[] bound, Color inColor, Color outColor, HashMap<Long, double[]> dictionary){
        boolean first = true;
        GeneralPath waterPath = new GeneralPath(GeneralPath.WIND_EVEN_ODD, wayNodes.length - 1);
        for (int node = 0; node < wayNodes.length; node++) {
            if (dictionary.containsKey(wayNodes[node])) {
                double[] u = dictionary.get(wayNodes[node]); //efficiency by using previous v?
                double uy = (bound[0] - u[0]) * spaceModifierX;
                double ux = (u[1] - bound[1]) * spaceModifierX;
                if (first) {
                    waterPath.moveTo(ux, uy);
                    first = false;
                } else {
                    waterPath.lineTo(ux, uy);
                }

            }
        }
        linesDrawn++;

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
            for(int z = 2; z < 1024; z = z * 2){
                new File("draw/" + region + "/" + z + "/").mkdirs();
//                if(Math.max(tiles.length, tiles[0].length))
                System.out.println("z" + z);
                //need to wrap this bit in a loop over blocks - done below?
                for(int jumpY = 0; jumpY <= tiles[0].length + 1; jumpY = jumpY + z){
//                    System.out.println(tiles[0].length);
                    for(int jumpX = 0; jumpX <= tiles.length + 1; jumpX = jumpX + z){
                        int emptyCount = 0;
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
                                    emptyCount++;
                                    images[x - jumpX][y - jumpY] = new BufferedImage((maxEdge / z), (maxEdge/z), 1);
                                    Graphics2D g = images[x - jumpX][y - jumpY].createGraphics();
                                    g.setPaint(new Color(153, 204, 255));
                                    g.fillRect(0, 0, maxEdge, maxEdge);
                                }
                            }
                        }
                        if(emptyCount == 4){
                            System.out.println("No images in this tile; skipping.");
                            continue;
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
//            if(!parsingNodes){
//                if (!rels.isEmpty()) {
////                    System.out.println("Got some relations to parse.");
//
//                    for (Relation r : rels){
//                        if(r.getId() == Long.parseLong("3084421")){
//                            System.out.println("TRAWS");
//                        }
//                        for (int i = 0; i < r.getKeysCount(); i++) {
//                            if(getStringById(r.getKeys(i)).equals("natural") && getStringById(r.getVals(i)).equals("water")){
//                                System.out.println("YES");
//                                long lastRef = 0;
//                                for (int k = 0; k < r.getMemidsCount(); k++) { //SMALLER THAN OR EQUAL TO OR NOT?
//                                    lastRef += (r.getMemids(k));
//                                    if (getStringById(r.getRolesSid(k)).equals("outer")){
//                                        System.out.println("Found reservoir: " + lastRef);
//                                    }
//                                }
//                            }
////                            System.out.println(getStringById(r.getKeys(i)) + " " + getStringById(r.getVals(i)));
//
//                        }
//                        for (int i = 0; i < r.getRolesSidCount(); i++) {
////                            System.out.println(getStringById(r.getRolesSid(i)));
//                        }
////                        System.out.println(r.getRolesSidList());
////                        System.out.println();
//                    }
//
//                    Relation r = null;
//                }
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
                    if((x >= bX1 && x < bX2) && (y >= bY1 && y < bY2)){
//                        System.out.println("FOUND");
                        double[] tempDense = new double[4];
                        tempDense[0] = parseLat(lastLat);
                        tempDense[1] = parseLon(lastLon);
                        tempDense[2] = x - bX1;
                        tempDense[3] = y - bY1;
                        tileNodes.put(lastId, tempDense);
                    }
                }
            }
            if(parsingPlaces){
                long lastId = 0;
                long lastLat = 0;
                long lastLon = 0;
                int tagCounter = 0;
                String key;
                String value;
                String name = "";
                List<Integer> keysVals = nodes.getKeysValsList();
                boolean exitFlag, saveCity, saveTown;

                for (int i = 0; i < nodes.getIdCount(); i++) {
                    lastId += nodes.getId(i);
                    lastLat += nodes.getLat(i);
                    lastLon += nodes.getLon(i);
//                    System.out.println(lastId);
                    exitFlag = false;
                    saveTown = false;
                    saveCity = false;
                    do{
                        if(keysVals.get(tagCounter) != 0){
                            key = getStringById(keysVals.get(tagCounter));
                            value = getStringById(keysVals.get(tagCounter + 1));
                            if(key.equals("name")){
                                name = value;
                            }
                            if(key.equals("place") && value.equals("town")){
                                saveTown = true;
                            }
                            if(key.equals("place") && value.equals("city")){
                                saveCity = true;
                            }
                            tagCounter += 2;
                        } else {
                            tagCounter += 1;
                            exitFlag = true;
                        }
                    }while(exitFlag == false);
                    if(saveTown){
                        towns.add(new Place(name, parseLon(lastLon), parseLat(lastLat)));
                        System.out.println(name);
                    }
                    if(saveCity){
                        cities.add(new Place(name, parseLon(lastLon), parseLat(lastLat)));
                        System.out.println(name);
                    }
//                    System.out.println(getStringById(nodes.getKeysValsList().get(i)));
//                    System.out.println("------------------------");
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
            if (parsingWays) {
//                System.out.println("Parsing ways.");
                for (Way w : ways) {
                    long lastRef = 0;
                    WAY: for (Long ref : w.getRefsList()) {
                        lastRef += ref;
                        if (tileNodes.containsKey(lastRef)) {
//                            if(w.getId() == Long.parseLong("22815916")){
//                                System.out.println("found ref at x " + bX1 + " y " + bY1);
//                            }
                            double[] data = tileNodes.get(lastRef);
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
                                        tileWays[(int) data[2]][(int) data[3]].put(lastRef, tempWay);
                                    } else if (value.matches("trunk|trunk_link")){
                                        MyWay tempWay = buildMyWay(w);
                                        tempWay.setType(WayType.ROAD);
                                        tempWay.setRoadType(RoadType.TRUNK);
                                        tileWays[(int) data[2]][(int) data[3]].put(lastRef, tempWay);
                                    } else if (value.matches("primary|primary_link")){
                                        MyWay tempWay = buildMyWay(w);
                                        tempWay.setType(WayType.ROAD);
                                        tempWay.setRoadType(RoadType.PRIMARY);
                                        tileWays[(int) data[2]][(int) data[3]].put(lastRef, tempWay);
                                    } else if (value.matches("secondary|secondary_link")){
                                        MyWay tempWay = buildMyWay(w);
                                        tempWay.setType(WayType.ROAD);
                                        tempWay.setRoadType(RoadType.SECONDARY);
                                        tileWays[(int) data[2]][(int) data[3]].put(lastRef, tempWay);
                                    } else if (value.matches("tertiary|unclassified|residential|service|tertiary_link|road")){
                                        MyWay tempWay = buildMyWay(w);
                                        tempWay.setType(WayType.ROAD);
                                        tempWay.setRoadType(RoadType.ROAD);
                                        tileWays[(int) data[2]][(int) data[3]].put(lastRef, tempWay);
                                    }
                                }
                                if(key.equals("railway")){
                                    MyWay tempWay = buildMyWay(w);
                                    tempWay.setType(WayType.RAILWAY);
                                    tileWays[(int) data[2]][(int) data[3]].put(lastRef, tempWay);
                                }
                                if((key.equals("natural") && value.equals("grass"))
                                        || (key.equals("leisure") && value.equals("common"))
                                        || (key.equals("leisure") && value.equals("park"))
                                        || (key.equals("leisure") && value.equals("golf_course"))
                                        || value.equals("meadow")
                                        || value.equals("recreation_ground")
                                        || value.equals("conservation")
                                        || value.equals("park")){
                                    MyWay tempWay = buildMyWay(w);
                                    tempWay.setType(WayType.GREEN);
                                    tileWays[(int) data[2]][(int) data[3]].put(lastRef, tempWay);
                                }
                                if((key.equals("natural") && (value.equals("moor") || value.equals("heath")))){
                                    MyWay tempWay = buildMyWay(w);
                                    tempWay.setType(WayType.MOOR);
                                    tileWays[(int) data[2]][(int) data[3]].put(lastRef, tempWay);
                                }
                                if(key.equals("waterway") && (value.matches("river|stream|canal"))){
                                    MyWay tempWay = buildMyWay(w);
                                    tempWay.setType(WayType.WATERWAY);
                                    tileWays[(int) data[2]][(int) data[3]].put(lastRef, tempWay);
                                }
                                if((key.equals("natural") && value.equals("water"))
                                        || value.matches("reservoir|basin")){
                                    MyWay tempWay = buildMyWay(w);
                                    tempWay.setType(WayType.WATERBODY);
                                    tileWays[(int) data[2]][(int) data[3]].put(lastRef, tempWay);
                                }
                                if((key.equals("natural") && value.equals("wood"))
                                        || (key.equals("landuse") && value.equals("forest"))){
                                    MyWay tempWay = buildMyWay(w);
                                    tempWay.setType(WayType.TREE);
                                    tileWays[(int) data[2]][(int) data[3]].put(lastRef, tempWay);
                                }
                                if(key.equals("cycleway") || value.equals("cycleway") ||
                                        (key.equals("route") && value.equals("bicycle"))){
                                    MyWay tempWay = buildMyWay(w);
                                    tempWay.setType(WayType.CYCLE);
                                    tileWays[(int) data[2]][(int) data[3]].put(lastRef, tempWay);
                                }
                                if(key.equals("landuse") && (value.equals("residential") || value.equals("retail") || value.equals("school") || value.equals("industrial"))){
                                    MyWay tempWay = buildMyWay(w);
                                    tempWay.setType(WayType.CITY);
//                                    System.out.println("CITY: " + w.getId());
                                    tileWays[(int) data[2]][(int) data[3]].put(lastRef, tempWay);
                                }
                            }
                        }
                    }
                }
            }
        }

        private MyWay buildMyWay(Way w){
            MyWay tempWay = new MyWay();
            long id = w.getId();
            tempWay.setWayId(id);
            long lastRef = 0;
            long[] wayNodes = new long[w.getRefsList().size()];
            int wayCtr = 0;
            for (Long ref : w.getRefsList()) {
                lastRef += ref;
                wayNodes[wayCtr] = lastRef;
                wayCtr++;
            }
            tempWay.setWayNodes(wayNodes);
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

    public int getTileWidth(){
        return xDimension;
    }

    public int getTileHeight(){
        return yDimension;
    }

    public Point2D.Double getCentre(){
        return centre;
    }

    public Point2D.Double getOrigin(){
        return origin;
    }

    private void timerStart(){
        startTime = System.nanoTime();
    }

    private void timerEnd(String string){
        endTime = System.nanoTime();
        System.out.println(string + " time: " + (((float) endTime - (float)startTime) / 1000000000));
    }

    public ArrayList<Place> getCities() {
        return cities;
    }

    public ArrayList<Place> getTowns() {
        return towns;
    }
}
