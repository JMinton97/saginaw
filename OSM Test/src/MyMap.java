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
    public static ArrayList<MyNode> mapNodes = new ArrayList<MyNode>();
    public static Map<Long, MyNode> dictionary = new HashMap<Long, MyNode>(); //NOTE - STORING NODE ID TWICE!!!
    public static ArrayList<MyWay> mapRoads = new ArrayList<MyWay>();
    public static ArrayList<MyWay> mapRails = new ArrayList<MyWay>();
    public static ArrayList<MyWay> mapGreens = new ArrayList<MyWay>();
    public static ArrayList<MyWay> mapWaterBodies = new ArrayList<MyWay>();
    public static ArrayList<MyWay> mapWaterWays = new ArrayList<MyWay>();
    public static ArrayList<MyWay> mapCycles = new ArrayList<MyWay>();
    public Double northMost, westMost, southMost, eastMost;

    public MyMap(String mapName) throws IOException {
        InputStream input = ReadFile.class.getResourceAsStream(mapName);
        BlockReaderAdapter brad = new MyMap.TestBinaryParser();
        new BlockInputStream(input, brad).process();
        northMost = -Double.MAX_VALUE;
        westMost = Double.MAX_VALUE;
        southMost = Double.MAX_VALUE;
        eastMost = -Double.MAX_VALUE;
        for (MyNode n : mapNodes) {
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
    }

    public void drawMap(){
        BufferedImage map = new BufferedImage(14000, 8000, 1);
        Graphics2D mapGraphics = map.createGraphics();
        mapGraphics.setColor(new Color(255, 255, 243));
        mapGraphics.fillRect(0, 0, map.getWidth(), map.getHeight());
        BasicStroke bs = new BasicStroke(1);
        mapGraphics.setStroke(bs);
        for(MyWay w: mapGreens){
            drawWay(w, mapGraphics);
        }
        for(MyWay w: mapWaterBodies){
            drawWay(w, mapGraphics);
        }
        for(MyWay w: mapWaterWays){
            drawWay(w, mapGraphics);
        }
        for(MyWay w: mapRails){
            drawWay(w, mapGraphics);
        }
        for(MyWay w: mapRoads){
            drawWay(w, mapGraphics);
        }
        for(MyWay w: mapCycles){
            drawWay(w, mapGraphics);
        }
        try {
            File outputfile = new File("saved.png");
            ImageIO.write(map, "png", outputfile);
        } catch (IOException e) {
            // handle exception
        }
        System.out.println("All finished.");
    }

    public void drawWay(MyWay way, Graphics2D mapGraphics){
        ArrayList<Long> wayNodes = way.getWayNodes();
        switch (way.getType()) {
            case ROAD:
                mapGraphics.setPaint(Color.GRAY);
                for (int node = 0; node < wayNodes.size() - 1; node++) {
//                System.out.println(dictionary.get(wayNodes.get(node)).getNodeId());
//                System.out.println(dictionary.get(wayNodes.get(node + 1)).getNodeId());
                    if (dictionary.containsKey(wayNodes.get(node)) && dictionary.containsKey(wayNodes.get(node + 1))) {
                        MyNode u = dictionary.get(wayNodes.get(node)); //efficiency by using previous v?
                        MyNode v = dictionary.get(wayNodes.get(node + 1));
                        Double uy = Math.abs(u.getLati() - northMost) * 32000;
                        Double ux = Math.abs(u.getLongi() - westMost) * 24000;
                        Double vy = Math.abs(v.getLati() - northMost) * 32000;
                        Double vx = Math.abs(v.getLongi() - westMost) * 24000;
                        mapGraphics.drawLine(ux.intValue(), uy.intValue(), vx.intValue(), vy.intValue());
                    }
                }
                break;

            case RAILWAY:
                mapGraphics.setPaint(Color.BLACK);
                for (int node = 0; node < wayNodes.size() - 1; node++) {
//                System.out.println(dictionary.get(wayNodes.get(node)).getNodeId());
//                System.out.println(dictionary.get(wayNodes.get(node + 1)).getNodeId());
                    if (dictionary.containsKey(wayNodes.get(node)) && dictionary.containsKey(wayNodes.get(node + 1))) {
                        MyNode u = dictionary.get(wayNodes.get(node)); //efficiency by using previous v?
                        MyNode v = dictionary.get(wayNodes.get(node + 1));
                        Double uy = Math.abs(u.getLati() - northMost) * 32000;
                        Double ux = Math.abs(u.getLongi() - westMost) * 24000;
                        Double vy = Math.abs(v.getLati() - northMost) * 32000;
                        Double vx = Math.abs(v.getLongi() - westMost) * 24000;
                        mapGraphics.drawLine(ux.intValue(), uy.intValue(), vx.intValue(), vy.intValue());
                    }
                }
                break;

            case WATERWAY:
                mapGraphics.setPaint(new Color(102, 178, 255));
                for (int node = 0; node < wayNodes.size() - 1; node++) {
//                System.out.println(dictionary.get(wayNodes.get(node)).getNodeId());
//                System.out.println(dictionary.get(wayNodes.get(node + 1)).getNodeId());
                    if (dictionary.containsKey(wayNodes.get(node)) && dictionary.containsKey(wayNodes.get(node + 1))) {
                        MyNode u = dictionary.get(wayNodes.get(node)); //efficiency by using previous v?
                        MyNode v = dictionary.get(wayNodes.get(node + 1));
                        Double uy = Math.abs(u.getLati() - northMost) * 32000;
                        Double ux = Math.abs(u.getLongi() - westMost) * 24000;
                        Double vy = Math.abs(v.getLati() - northMost) * 32000;
                        Double vx = Math.abs(v.getLongi() - westMost) * 24000;
                        mapGraphics.drawLine(ux.intValue(), uy.intValue(), vx.intValue(), vy.intValue());
                    }
                }
                break;

            case CYCLE:
                BasicStroke bs = new BasicStroke(4);
                mapGraphics.setStroke(bs);
                mapGraphics.setPaint(Color.RED);
                for (int node = 0; node < wayNodes.size() - 1; node++) {
//                System.out.println(dictionary.get(wayNodes.get(node)).getNodeId());
//                System.out.println(dictionary.get(wayNodes.get(node + 1)).getNodeId());
                    if (dictionary.containsKey(wayNodes.get(node)) && dictionary.containsKey(wayNodes.get(node + 1))) {
                        MyNode u = dictionary.get(wayNodes.get(node)); //efficiency by using previous v?
                        MyNode v = dictionary.get(wayNodes.get(node + 1));
                        Double uy = Math.abs(u.getLati() - northMost) * 32000;
                        Double ux = Math.abs(u.getLongi() - westMost) * 24000;
                        Double vy = Math.abs(v.getLati() - northMost) * 32000;
                        Double vx = Math.abs(v.getLongi() - westMost) * 24000;
                        mapGraphics.drawLine(ux.intValue(), uy.intValue(), vx.intValue(), vy.intValue());
                    }
                }
                break;

            case GREEN:
                GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, wayNodes.size() - 1);
                for (int node = 0; node < wayNodes.size(); node++) {
                    if (dictionary.containsKey(wayNodes.get(node))) {
                        MyNode u = dictionary.get(wayNodes.get(node)); //efficiency by using previous v?
                        Double uy = Math.abs(u.getLati() - northMost) * 32000;
                        Double ux = Math.abs(u.getLongi() - westMost) * 24000;
                        if(node == 0){
                            path.moveTo (ux, uy);
                        } else {
                            path.lineTo (ux, uy);
                        }

                    }
                }
                path.closePath();
                mapGraphics.setPaint(new Color(153, 255, 153));
                mapGraphics.fill(path);
                mapGraphics.setPaint(new Color(102, 255, 102));
                mapGraphics.draw(path);
                break;

            case WATERBODY:
                GeneralPath waterPath = new GeneralPath(GeneralPath.WIND_EVEN_ODD, wayNodes.size() - 1);
                for (int node = 0; node < wayNodes.size(); node++) {
                    if (dictionary.containsKey(wayNodes.get(node))) {
                        MyNode u = dictionary.get(wayNodes.get(node)); //efficiency by using previous v?
                        Double uy = Math.abs(u.getLati() - northMost) * 32000;
                        Double ux = Math.abs(u.getLongi() - westMost) * 24000;
                        if(node == 0){
                            waterPath.moveTo (ux, uy);
                        } else {
                            waterPath.lineTo (ux, uy);
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

    public static class TestBinaryParser extends BinaryParser {

        @Override
        protected void parseRelations(List<Relation> rels) {
            if (!rels.isEmpty())
                System.out.println("Got some relations to parse.");
            Relation r = null;
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
                mapNodes.add(tempDense);
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
                boolean include = false;
                MyWay tempWay = new MyWay();
                boolean road = false;
                boolean rail = false;
                tempWay.setWayId(w.getId());
                long lastRef = 0;
                for (Long ref : w.getRefsList()) {
                    lastRef+= ref;
                    tempWay.addWayNode(lastRef);
                }
//                sb.append("\n  Key=value pairs: ");
                for (int i=0 ; i<w.getKeysCount() ; i++) {
                    if(getStringById(w.getKeys(i)).equals("highway")){
                        tempWay.setType(WayType.ROAD);
                        mapRoads.add(tempWay);
                    }
                    if(getStringById(w.getKeys(i)).equals("railway")){
                        tempWay.setType(WayType.RAILWAY);
                        mapRails.add(tempWay);
                    }
                    if(getStringById(w.getVals(i)).equals("grass") || getStringById(w.getVals(i)).equals("meadow") || getStringById(w.getVals(i)).equals("recreation_ground") || getStringById(w.getVals(i)).equals("conservation")  || getStringById(w.getVals(i)).equals("park")){
                        tempWay.setType(WayType.GREEN);
                        mapGreens.add(tempWay);
                    }
                    if(getStringById(w.getVals(i)).matches("river|stream|canal")){
                        tempWay.setType(WayType.WATERWAY);
                        mapWaterWays.add(tempWay);
                    }
                    if((getStringById(w.getKeys(i)).equals("natural") && getStringById(w.getVals(i)).equals("water")) || getStringById(w.getVals(i)).matches("reservoir|basin")){
                        tempWay.setType(WayType.WATERBODY);
                        mapWaterBodies.add(tempWay);
                    }
                    if(getStringById(w.getKeys(i)).equals("cycleway") || getStringById(w.getVals(i)).equals("cycleway") || (getStringById(w.getKeys(i)).equals("route") && getStringById(w.getVals(i)).equals("bicycle"))){
                        tempWay.setType(WayType.CYCLE);
                        mapCycles.add(tempWay);
                        System.out.println(tempWay.getWayNodes().get(0).toString());
                    }
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
