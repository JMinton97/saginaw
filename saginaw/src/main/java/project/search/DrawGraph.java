package project.search;

import org.mapdb.BTreeMap;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DrawGraph {
    private String region;
    private double northMost, southMost, eastMost, westMost, xScale, yScale;
    private HashMap<Long, ArrayList<double[]>> graph;

    public DrawGraph(String region){
        this.region = region;
        if (region == "wales") {
            northMost = 53.5; //56;
            westMost = -5.5; //-6;          //WALES
            southMost = 51.3; //49.5;
            eastMost = -2.5; //2;
        } else if (region == "england") {
            northMost = 56;
            westMost = -6;
            southMost = 49.5;               //IT"S COMING HOME
            eastMost = 2;
        } else if (region == "france") {
            northMost = 51.1;
            westMost = -5.3;
            southMost = 42.3;               //FRANCE
            eastMost = 8.4;
        } else if (region == "birmingham") {
            northMost = 52.620580;
            westMost = -2.240133;
            southMost = 52.336874;          //BIRMINGHAM
            eastMost = -1.655798;
        }

        double height, width;

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

        int imgWidth = 1000;
        int imgHeight = 1000;

        xScale = imgWidth/ width;
        yScale = imgHeight / height;

    }

    public BufferedImage draw(Map<Long, ArrayList<double[]>> graph, BTreeMap<Long, double[]> dictionary){
        BufferedImage img = new BufferedImage(1000, 1000, 1);
        Graphics2D g = img.createGraphics();
        this.graph = (HashMap) graph;
        g.setStroke(new BasicStroke(1));
        g.setPaint(new Color(102, 178, 255));
        for(Map.Entry<Long, ArrayList<double[]>> v : graph.entrySet()){
//            System.out.println(n);
            Long n = v.getKey();
            double[] loc = dictionary.get(n);
            double x = loc[0];
            double y = loc[1];
            x = (x - westMost) * xScale;
            y = (northMost - y) * yScale;
//            System.out.println(x + " " + y);

            for(double[] edge : v.getValue()){
                loc = dictionary.get((long) edge[0]);
                double tx = loc[0];
                double ty = loc[1];
                tx = (tx - westMost) * xScale;
                ty = (northMost - ty) * yScale;
                try{
                    g.drawLine((int) x, (int) y, (int) tx, (int) ty);
                } catch(ArrayIndexOutOfBoundsException e){
                    continue;
                }
            }


        }
        return img;
    }


}
