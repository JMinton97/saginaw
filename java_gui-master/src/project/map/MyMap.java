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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyMap {
//    public static ArrayList<MyNode> mapNodes = new ArrayList<MyNode>();
    protected static Map<Long, MyNode> fullDictionary;
    protected static Map<Long, MyNode> dictionary;
    protected static ArrayList<MyWay> mapRoads;
    protected static ArrayList<MyWay> mapRails;
    protected static ArrayList<MyWay> mapGreens;
    protected static ArrayList<MyWay> mapForests;
    protected static ArrayList<MyWay> mapWaterBodies;
    protected static ArrayList<MyWay> mapWaterWays;
    protected static ArrayList<MyWay> mapCycles;
    protected Double northMost, westMost, southMost, eastMost;
    protected Double spaceModifierX;
    protected Double spaceModifierY;
    protected Double paneHeight = 10000.00;
    protected Double paneWidth;
    protected BufferedImage map;
    protected int level;

    public MyMap(File file) throws IOException {
        int level;
        fullDictionary = new HashMap<Long, MyNode>(); //NOTE - STORING NODE ID TWICE!!!
        dictionary = new HashMap<>(); //NOTE - STORING NODE ID TWICE!!!
        mapRoads = new ArrayList<>();
        mapRails = new ArrayList<>();
        mapGreens = new ArrayList<>();
        mapForests = new ArrayList<>();
        mapWaterBodies = new ArrayList<>();
        mapWaterWays = new ArrayList<>();
        mapCycles = new ArrayList<>();
//        MyMap.class.getResourceAsStream(mapName)
        InputStream input = new FileInputStream(file);
        BlockReaderAdapter brad = new TestBinaryParser();
        new BlockInputStream(input, brad).process();
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
        System.out.println(dictionary.size());  //what if douglas peucker returns a list of deleted nodes?
        trimDictionary();
        Double height, width;
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
        paneWidth = (paneHeight * (width / height)) * 0.75;
        spaceModifierY = paneHeight / height;
        spaceModifierX = spaceModifierY * 0.75;
    }

    public void drawMap(int level){
        this.level = level;
        map = new BufferedImage(paneWidth.intValue(), paneHeight.intValue(), 1);
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

        for(MyWay w: mapRoads){
            drawWay(w, mapGraphics, true);
        }
        System.out.println("drew roads under");

        for(MyWay w: mapRoads){
            drawWay(w, mapGraphics, false);
        }
        System.out.println("drew roads over");

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

    public void trimDictionary(){
    }

    public BufferedImage getMap() {
        return map;
    }

    public void drawWay(MyWay way, Graphics2D mapGraphics, boolean underlay) {
        if (isVisible(way.getType(), level)) {
            Color wayColor = Color.WHITE;
            if (isVisible(way.getRoadType(), level)) {
//                List<Long> wayNodes = way.getWayNodes();
                List<Long> wayNodes = DouglasPeucker.decimate(way.getWayNodes(), 0.0001, dictionary);
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
                                Double uy = Math.abs(u.getLati() - northMost) * spaceModifierY;
                                Double ux = Math.abs(u.getLongi() - westMost) * spaceModifierX;
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
                                Double uy = Math.abs(u.getLati() - northMost) * spaceModifierY;
                                Double ux = Math.abs(u.getLongi() - westMost) * spaceModifierX;
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
                                Double uy = Math.abs(u.getLati() - northMost) * spaceModifierY;
                                Double ux = Math.abs(u.getLongi() - westMost) * spaceModifierX;
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
        for (int node = 0; node < wayNodes.size() - 1; node++) {
//                System.out.println(dictionary.get(wayNodes.get(node)).getNodeId());
//                System.out.println(dictionary.get(wayNodes.get(node + 1)).getNodeId());
            if (dictionary.containsKey(wayNodes.get(node)) && dictionary.containsKey(wayNodes.get(node + 1))) {
                MyNode u = dictionary.get(wayNodes.get(node)); //efficiency by using previous v?
                MyNode v = dictionary.get(wayNodes.get(node + 1));
                Double uy = Math.abs(u.getLati() - northMost) * spaceModifierY;
                Double ux = Math.abs(u.getLongi() - westMost) * spaceModifierX;
                Double vy = Math.abs(v.getLati() - northMost) * spaceModifierY;
                Double vx = Math.abs(v.getLongi() - westMost) * spaceModifierX;
                mapGraphics.drawLine(ux.intValue(), uy.intValue(), vx.intValue(), vy.intValue());
            }
        }
    }

    public boolean isVisible(Enum type, int level) {
        if(type != null){
            if(level == 0){
                return true;
            } else if(level == 1){
                if(type.equals(RoadType.ROAD)){
                    return false;
                }
            } else {
                return true;
            }
            return true;
        }
        return true;
    }

    public static class TestBinaryParser extends BinaryParser {

        @Override
        protected void parseRelations(List<Relation> rels) {
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
            long lastId=0;
            long lastLat=0;
            long lastLon=0;

            for (int i=0 ; i<nodes.getIdCount() ; i++) {
                MyNode tempDense = new MyNode();
                lastId += nodes.getId(i);
                lastLat += nodes.getLat(i);
                lastLon += nodes.getLon(i);
//                System.out.printf("Dense node, ID %d @ %.6f,%.6f\n",
//                        lastId,parseLat(lastLat),parseLon(lastLon));
                tempDense.setLati(parseLat(lastLat));
                tempDense.setLongi(parseLon(lastLon));
                tempDense.setNodeId(lastId);
                dictionary.put(lastId, tempDense);
//                mapNodes.add(tempDense);
            }
        }

        @Override
        protected void parseNodes(List<Node> nodes) {
            for (Node n : nodes) {
                System.out.printf("Regular node, ID %d @ %.6f,%.6f\n",
                        n.getId(),parseLat(n.getLat()),parseLon(n.getLon()));
            }
        }

        @Override
        protected void parseWays(List<Way> ways) {
            for (Way w : ways) {
                MyWay tempWay = new MyWay();
                tempWay.setWayId(w.getId());
                long lastRef = 0;
                for (Long ref : w.getRefsList()) {
                    lastRef+= ref;
                    tempWay.addWayNode(lastRef);
                }
                String key;
                String value;
                for (int i=0 ; i<w.getKeysCount() ; i++) {
                    key = getStringById(w.getKeys(i));
                    value = getStringById(w.getVals(i));
                    if(key.equals("highway")){
                        tempWay.setType(WayType.ROAD);
                        if(value.matches("motorway|motorway_link")){
                            tempWay.setRoadType(RoadType.MOTORWAY);
                            mapRoads.add(tempWay);
                        } else if (value.matches("trunk|trunk_link")){
                            tempWay.setRoadType(RoadType.TRUNK);
                            mapRoads.add(tempWay);
                        } else if (value.matches("primary|primary_link")){
                            tempWay.setRoadType(RoadType.PRIMARY);
                            mapRoads.add(tempWay);
                        } else if (value.matches("secondary|secondary_link")){
                            tempWay.setRoadType(RoadType.SECONDARY);
                            mapRoads.add(tempWay);
                        } else if (value.matches("tertiary|unclassified|residential|service|tertiary_link|road")){
                            tempWay.setRoadType(RoadType.ROAD);
                            mapRoads.add(tempWay);
                        }
                        if(w.getId() == 596189407){
                            System.out.println("hey");
                            System.out.println(tempWay.getType());
                            System.out.println(tempWay.getRoadType());
                            System.out.println(mapRoads);
                        }
                    }
                    if(key.equals("railway")){
                        tempWay.setType(WayType.RAILWAY);
                        mapRails.add(tempWay);
                    }
                    if((key.equals("natural") && value.equals("grass"))
                            || (key.equals("leisure") && value.equals("common"))
                            || (key.equals("leisure") && value.equals("park"))
                            || (key.equals("leisure") && value.equals("golf_course"))
                            || value.equals("meadow")
                            || value.equals("recreation_ground")
                            || value.equals("conservation")
                            || value.equals("park")){
                        tempWay.setType(WayType.GREEN);
                        mapGreens.add(tempWay);
                    }
                    if((key.equals("natural")) && (value.matches("river|stream|canal"))){
                        tempWay.setType(WayType.WATERWAY);
                        mapWaterWays.add(tempWay);
                    }
                    if((key.equals("natural") && value.equals("water"))
                            || value.matches("reservoir|basin")){
                        tempWay.setType(WayType.WATERBODY);
                        mapWaterBodies.add(tempWay);
                    }
                    if((key.equals("natural") && value.equals("wood"))
                            || (key.equals("landuse") && value.equals("forest"))){
                        tempWay.setType(WayType.TREE);
                        mapForests.add(tempWay);
                    }
//                    if(key.equals("cycleway") || value.equals("cycleway") ||
//                            (key.equals("route") && value.equals("bicycle"))){
//                        tempWay.setType(WayType.CYCLE);
//                        mapCycles.add(tempWay);
//                    }
                }
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



}
