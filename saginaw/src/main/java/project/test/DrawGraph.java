package project.test;

import project.map.Graph;
import project.search.Searcher;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class DrawGraph {
    private String region;
    private double northMost, southMost, eastMost, westMost, xScale, yScale;
    private ArrayList<ArrayList<double[]>> graph;

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
        } else if (region == "britain") {
            northMost = 58.7;
            westMost = -8;
            southMost = 49.5;   //BRITAIN
            eastMost = 2;
        } else if (region == "london") {
            northMost = 51.8;
            westMost = -0.7;
            southMost = 51.2;   //LONDON
            eastMost = 0.49;
        } else if (region == "stratford") {
            northMost = 52.214004;
            westMost = -1.767409;
            southMost = 52.166644;   //LONDON
            eastMost = -1.649358;
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

    public BufferedImage drawGraph(Graph graph, ArrayList<double[]> dictionary){
        BufferedImage img = new BufferedImage(1000, 1000, 1);
        Graphics2D g = img.createGraphics();
        this.graph = graph.getFwdGraph();
        g.setPaint(new Color(255, 255, 255));
        g.fillRect(0, 0, 1000, 1000);
        g.setStroke(new BasicStroke(4));
        g.setPaint(new Color(0, 0, 0));
        int n = 0;
        for(ArrayList<double[]> v : this.graph){
//            System.out.println(n);
            double[] loc = graph.getGraphNodeLocation(n);
            double x = loc[0];
            double y = loc[1];
            x = (x - westMost) * xScale;
            y = (northMost - y) * yScale;
//            System.out.println(x + " " + y);

            for(double[] edge : v){
                loc = graph.getGraphNodeLocation((int) edge[0]);
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

            n++;

        }

        File outputfile =  new File("full.png");
        try {
            ImageIO.write(img, "png", outputfile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return img;
    }

    public BufferedImage drawCore(Graph sGraph, ArrayList<double[]> dictionary){
        BufferedImage img = new BufferedImage(1000, 1000, 1);
        Graphics2D g = img.createGraphics();
        Map<Integer, ArrayList<double[]>> graph = sGraph.getFwdCore();
        g.setPaint(new Color(255, 255, 255));
        g.fillRect(0, 0, 1000, 1000);
        g.setStroke(new BasicStroke(4));
        g.setPaint(new Color(0, 0, 0));

        for(Integer n : graph.keySet()){
            double[] loc = sGraph.getGraphNodeLocation(n);
            double x = loc[0];
            double y = loc[1];
            x = (x - westMost) * xScale;
            y = (northMost - y) * yScale;
//            System.out.println(x + " " + y);

            for(double[] edge : graph.get(n)){
                loc = sGraph.getGraphNodeLocation((int) edge[0]);
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

            n++;

        }

        File outputfile =  new File("core.png");
        try {
            ImageIO.write(img, "png", outputfile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return img;
    }

    public BufferedImage drawSearch(Searcher searcher, Graph graph, int j, int level, int src, int dst) throws IOException {
        System.out.println();
        BufferedImage img = new BufferedImage(1000, 1000, 1);
        Graphics g = img.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 1000, 1000);

        for(int node : searcher.getRelaxedNodes().get(0)){
            double[] loc = graph.getGraphNodeLocation(node);
            double x = loc[0];
            double y = loc[1];
            x = (x - westMost) * xScale;
            y = (northMost - y) * yScale;
            int red = Color.DARK_GRAY.getRGB();
            try{
                img.setRGB((int) x, (int) y, red);
            }catch(ArrayIndexOutOfBoundsException e){
                System.out.println(e.getStackTrace());
                System.out.println(x + " " + y);
            }
        }

        double[] loc = graph.getGraphNodeLocation(src);
        double x = loc[0];
        double y = loc[1];
        x = (x - westMost) * xScale;
        y = (northMost - y) * yScale;
        g.setColor(Color.RED.brighter());
        g.fillOval((int) x, (int) y, 10, 10);


        if(searcher.getRelaxedNodes().size() > 1){
            for(int node : searcher.getRelaxedNodes().get(1)){
                loc = graph.getGraphNodeLocation(node);
                x = loc[0];
                y = loc[1];
                x = (x - westMost) * xScale;
                y = (northMost - y) * yScale;
                int red = Color.DARK_GRAY.getRGB();
                img.setRGB((int) x, (int) y, red);
            }
        }

        loc = graph.getGraphNodeLocation(dst);
        x = loc[0];
        y = loc[1];
        x = (x - westMost) * xScale;
        y = (northMost - y) * yScale;
        g.setColor(Color.BLUE.brighter());
        g.fillOval((int) x, (int) y, 10, 10);

        if(searcher.getALT() != null){
            g.setColor(Color.GREEN);

            for(Integer landmark : searcher.getALT().getLandmarks()){
                System.out.println(landmark);
                loc = graph.getGraphNodeLocation(landmark);
                x = loc[0];
                y = loc[1];
                x = (x - westMost) * xScale;
                y = (northMost - y) * yScale;
//                System.out.println(x + " " + y);
                g.fillOval((int) x, (int) y, 10, 10);
            }
        }


        g.setColor(Color.BLACK);

        g.setFont(new Font("Courier", Font.PLAIN, 35));
        g.drawString(searcher.getName(), 10, 40);
        g.drawString("Explored: " + searcher.getExplored(), 10, 80);

        new File("test/searchDrawings/" + region + "/" + j + "/").mkdirs();
        File outputfile =  new File("test/searchDrawings/" + region + "/" + j + "/" + level + "-" + searcher.getName() + ".png");
        ImageIO.write(img, "png", outputfile);
        return img;
    }


}
