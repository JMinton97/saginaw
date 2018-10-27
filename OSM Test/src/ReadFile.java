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

/**
 * Demonstrates how to read a file. Reads sample.pbf from the resources folder
 * and prints details about it to the standard output.
 *
 * @author Michael Tandy
 */
public class ReadFile {

    public static ArrayList<MyNode> mapNodes = new ArrayList<MyNode>();
    public static Map<Long, MyNode> dictionary = new HashMap<Long, MyNode>(); //NOTE - STORING NODE ID TWICE!!!
    public static ArrayList<MyWay> mapWays = new ArrayList<MyWay>();
    public static ArrayList<MyWay> mapWaysTrains = new ArrayList<MyWay>();

    public Double northMost, westMost, eastMost, southMost;

    public static void main(String[] args) throws Exception {
        InputStream input = ReadFile.class.getResourceAsStream("/Birmingham.osm.pbf");
        BlockReaderAdapter brad = new TestBinaryParser();
        new BlockInputStream(input, brad).process();
        System.out.println(mapNodes.size());
        System.out.println(mapWays.size());
        Double northMost = -Double.MAX_VALUE;
        Double westMost = Double.MAX_VALUE;
        Double southMost = Double.MAX_VALUE;
        Double eastMost = -Double.MAX_VALUE;
        for (MyNode n : mapNodes) {
//            System.out.println(n.print());
            if (n.getLati() > northMost) {
                northMost = n.getLati();
            }
            if (n.getLongi() < westMost) {
                westMost = n.getLongi();
            }
            if (n.getLati() < southMost) {
                southMost = n.getLati();
            }
//            System.out.println(n.getLongi() + " " + eastMost);
            if (n.getLongi() > eastMost) {
                eastMost = n.getLongi();
            }
        }
//        System.out.println(northMost + " " + westMost + " " + " " + southMost + " " + eastMost);
//        System.out.println("Width: " + (Math.abs(eastMost) - Math.abs(westMost)));
//        System.out.println("Height: " + (Math.abs(southMost) - Math.abs(northMost)));
//        System.out.println(mapWays.size());

        BufferedImage map = new BufferedImage(8000, 4000, 1);
//        for (MyNode n : mapNodes) {
//            Double y = Math.abs(n.getLati() - northMost) * 4000;
//            Double x = Math.abs(n.getLongi() - westMost) * 3000;
////            System.out.println(x + " " + y);
//            int rgb = new Color(255, 255, 255).getRGB();
////            System.out.println(x.intValue() + " " + y.intValue());
//            map.setRGB(x.intValue(), y.intValue(), rgb);
//        }
        Graphics2D mapGraphics = map.createGraphics();
        BasicStroke bs = new BasicStroke(1);
        mapGraphics.setColor(new Color(255, 255, 243));
        mapGraphics.fillRect(0, 0, map.getWidth(), map.getHeight());
        mapGraphics.setStroke(bs);

        mapGraphics.setPaint(Color.black);

        for (MyWay w : mapWays) {
            if (w.getType().equals(WayType.ROAD)){
                ArrayList<Long> wayNodes = w.getWayNodes();
                for (int node = 0; node < wayNodes.size() - 1; node++) {
//                System.out.println(dictionary.get(wayNodes.get(node)).getNodeId());
//                System.out.println(dictionary.get(wayNodes.get(node + 1)).getNodeId());
                    if (dictionary.containsKey(wayNodes.get(node)) && dictionary.containsKey(wayNodes.get(node + 1))) {
                        MyNode u = dictionary.get(wayNodes.get(node)); //efficiency by using previous v?
                        MyNode v = dictionary.get(wayNodes.get(node + 1));
                        Double uy = Math.abs(u.getLati() - northMost) * 16000;
                        Double ux = Math.abs(u.getLongi() - westMost) * 12000;
                        Double vy = Math.abs(v.getLati() - northMost) * 16000;
                        Double vx = Math.abs(v.getLongi() - westMost) * 12000;
                        mapGraphics.drawLine(ux.intValue(), uy.intValue(), vx.intValue(), vy.intValue());
                    }
                }
            }
        }

        for (MyWay w : mapWays) {
            if (w.getType().equals(WayType.WATERBODY)){
                ArrayList<Long> wayNodes = w.getWayNodes();
                GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD, wayNodes.size() - 1);
                for (int node = 0; node < wayNodes.size(); node++) {
//                System.out.println(dictionary.get(wayNodes.get(node)).getNodeId());
//                System.out.println(dictionary.get(wayNodes.get(node + 1)).getNodeId());
                    if (dictionary.containsKey(wayNodes.get(node))) {
                        MyNode u = dictionary.get(wayNodes.get(node)); //efficiency by using previous v?
                        Double uy = Math.abs(u.getLati() - northMost) * 16000;
                        Double ux = Math.abs(u.getLongi() - westMost) * 12000;
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

            }
        }

//        for (MyWay w : mapWaysTrains) {
////            System.out.println("end");
//            ArrayList<Long> wayNodes = w.getWayNodes();
//            for (int node = 0; node < wayNodes.size() - 1; node++) {
////                System.out.println(dictionary.get(wayNodes.get(node)).getNodeId());
////                System.out.println(dictionary.get(wayNodes.get(node + 1)).getNodeId());
//                if (dictionary.containsKey(wayNodes.get(node)) && dictionary.containsKey(wayNodes.get(node + 1))) {
//                    MyNode u = dictionary.get(wayNodes.get(node)); //efficiency by using previous v?
//                    MyNode v = dictionary.get(wayNodes.get(node + 1));
//                    Double uy = Math.abs(u.getLati() - northMost) * 16000;
//                    Double ux = Math.abs(u.getLongi() - westMost) * 12000;
//                    Double vy = Math.abs(v.getLati() - northMost) * 16000;
//                    Double vx = Math.abs(v.getLongi() - westMost) * 12000;
//                    mapGraphics.drawLine(ux.intValue(), uy.intValue(), vx.intValue(), vy.intValue());
//                }
//            }
//        }
//
//        mapGraphics.setPaint(Color.gray);
//        for (MyWay w : mapWays) {
////            System.out.println("end");
//            ArrayList<Long> wayNodes = w.getWayNodes();
//            for (int node = 0; node < wayNodes.size() - 1; node++) {
////                System.out.println(dictionary.get(wayNodes.get(node)).getNodeId());
////                System.out.println(dictionary.get(wayNodes.get(node + 1)).getNodeId());
//                if (dictionary.containsKey(wayNodes.get(node)) && dictionary.containsKey(wayNodes.get(node + 1))) {
//                    MyNode u = dictionary.get(wayNodes.get(node)); //efficiency by using previous v?
//                    MyNode v = dictionary.get(wayNodes.get(node + 1));
//                    Double uy = Math.abs(u.getLati() - northMost) * 16000;
//                    Double ux = Math.abs(u.getLongi() - westMost) * 12000;
//                    Double vy = Math.abs(v.getLati() - northMost) * 16000;
//                    Double vx = Math.abs(v.getLongi() - westMost) * 12000;
//                    mapGraphics.drawLine(ux.intValue(), uy.intValue(), vx.intValue(), vy.intValue());
//                }
//            }
//        }

        try {
            File outputfile = new File("saved.png");
            ImageIO.write(map, "png", outputfile);
        } catch (IOException e) {
            // handle exception
        }
        System.out.println("All finished.");
    }

    public void drawWays(ArrayList<MyWay> ways, BufferedImage mapImage) {
        Graphics2D mapGraphics = mapImage.createGraphics();
        for (MyWay w : ways) {
            if (w.getType().equals(WayType.WATERBODY)){
                ArrayList<Long> wayNodes = w.getWayNodes();
                for (int node = 0; node < wayNodes.size() - 1; node++) {
//                System.out.println(dictionary.get(wayNodes.get(node)).getNodeId());
//                System.out.println(dictionary.get(wayNodes.get(node + 1)).getNodeId());
                    if (dictionary.containsKey(wayNodes.get(node)) && dictionary.containsKey(wayNodes.get(node + 1))) {
                        MyNode u = dictionary.get(wayNodes.get(node)); //efficiency by using previous v?
                        MyNode v = dictionary.get(wayNodes.get(node + 1));
                        Double uy = Math.abs(u.getLati() - northMost) * 16000;
                        Double ux = Math.abs(u.getLongi() - westMost) * 12000;
                        Double vy = Math.abs(v.getLati() - northMost) * 16000;
                        Double vx = Math.abs(v.getLongi() - westMost) * 12000;
                        mapGraphics.drawLine(ux.intValue(), uy.intValue(), vx.intValue(), vy.intValue());
                    }
                }
            }
        }
    }


    private static class TestBinaryParser extends BinaryParser {

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
                        include = true;
                    }
                    if(getStringById(w.getKeys(i)).equals("railway")){
                        tempWay.setType(WayType.RAILWAY);
                        include = true;
                    }
                    if(getStringById(w.getVals(i)).equals("grass") || getStringById(w.getVals(i)).equals("meadow") || getStringById(w.getVals(i)).equals("recreation_ground") || getStringById(w.getVals(i)).equals("conservation")  || getStringById(w.getVals(i)).equals("park")){
                        tempWay.setType(WayType.WATERBODY);
                        include = true;
                    }
                    if(getStringById(w.getVals(i)).equals("river") || getStringById(w.getVals(i)).equals("canal")){
                        tempWay.setType(WayType.WATERWAY);
                        include = true;
                    }
                }
                if(include){mapWays.add(tempWay);}
//                if(road){mapWays.add(tempWay);}
//                if(rail){mapWaysTrains.add(tempWay);}
//                mapWays.add(tempWay);
//                System.out.println(" ");
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
