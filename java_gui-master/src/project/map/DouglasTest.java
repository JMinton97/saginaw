package project.map;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

public class DouglasTest {

    static Point2D phTag;

    public static void main(String[] args) {
        Point2D[] nodes = new Point2D[10000];
        ArrayList<Point2D> nodes2 = new ArrayList<>();

        Random rand = new Random(12);

        for(int x = 0; x < 10000; x++) {
            nodes[x] = new Point2D.Double(x + rand.nextInt(10), x - rand.nextInt(10));
        }

//        for(Point2D p : nodes){
//            System.out.println(p.toString());
//        }

        rand = new Random(1);
//
        for(int x = 0; x < 10000; x++) {
                nodes2.add( new Point2D.Double(x + rand.nextInt(10), x - rand.nextInt(10)));
        }

//        {
//            nodes2.add(new Point2D.Double(10, 10));
//            nodes2.add(new Point2D.Double(20, 5));
//            nodes2.add(new Point2D.Double(30, 20));
//            nodes2.add(new Point2D.Double(40, 40));
//            nodes2.add(new Point2D.Double(50, 20));
//            nodes2.add(new Point2D.Double(60, 10));
//            nodes2.add(new Point2D.Double(70, 15));
//            nodes2.add(new Point2D.Double(80, 5));
//            nodes2.add(new Point2D.Double(99, 50));
//            nodes2.add(new Point2D.Double(80, 60));
//            nodes2.add(new Point2D.Double(75, 50));
//            nodes2.add(new Point2D.Double(80, 70));
//            nodes2.add(new Point2D.Double(70, 70));
//            nodes2.add(new Point2D.Double(60, 40));
//            nodes2.add(new Point2D.Double(50, 45)); //
//            nodes2.add(new Point2D.Double(50, 75));
//            nodes2.add(new Point2D.Double(40, 99));
//            nodes2.add(new Point2D.Double(30, 80));
//            nodes2.add(new Point2D.Double(20, 60)); //
//            nodes2.add(new Point2D.Double(15, 50)); //
//            nodes2.add(new Point2D.Double(10, 60)); //
//            nodes2.add(new Point2D.Double(5, 35)); //
//            nodes[0] = new Point2D.Double(10, 10);
//            nodes[1] = new Point2D.Double(20, 5);
//            nodes[2] = new Point2D.Double(30, 20);
//            nodes[3] = new Point2D.Double(40, 40);
//            nodes[4] = new Point2D.Double(50, 20);
//            nodes[5] = new Point2D.Double(60, 10);
//            nodes[6] = new Point2D.Double(70, 15);
//            nodes[7] = new Point2D.Double(80, 5);
//            nodes[8] = new Point2D.Double(99, 50);
//            nodes[9] = new Point2D.Double(80, 60);
//            nodes[10] = new Point2D.Double(75, 50);
//            nodes[11] = new Point2D.Double(80, 70);
//            nodes[12] = new Point2D.Double(70, 70);
//            nodes[13] = new Point2D.Double(60, 40);
//            nodes[14] = new Point2D.Double(50, 45); //
//            nodes[15] = new Point2D.Double(50, 75);
//            nodes[16] = new Point2D.Double(40, 99);
//            nodes[17] = new Point2D.Double(30, 80);
//            nodes[18] = new Point2D.Double(20, 60); //
//            nodes[19] = new Point2D.Double(15, 50); //
//            nodes[20] = new Point2D.Double(10, 60); //
//            nodes[21] = new Point2D.Double(5, 35); //
//        }

//        try {
//            Thread.sleep(7000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        long startTime = System.nanoTime();
        ArrayList<Point2D> newNodes = DouglasPeucker.simplify(nodes, 7);
        long endTime = System.nanoTime();
        System.out.println("Size " + newNodes.size());
        System.out.println((endTime - startTime) / 10000);
         startTime = System.nanoTime();
        ArrayList<Point2D> newNodes2 = (ArrayList<Point2D>) DouglasPeucker.decimate(nodes2,7.0);
         endTime = System.nanoTime();
        System.out.println((endTime - startTime) / 10000);
//
//
//        try {
//            Thread.sleep(20000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        BufferedImage image = new BufferedImage(1000, 1000, 1);
        Graphics2D graphics = image.createGraphics();

//        int phTag = (nodes.size() - 1) / 2; //or points.size() - 1 / 2?
//        System.out.println(phTag);
//        PathHull left = new PathHull(phTag, phTag - 1);
//        for (int k = phTag - 2; k >= 0; k--) {
//            left.add(k, nodes);
//        }
////        System.exit(0);
//        PathHull right = new PathHull(phTag, phTag + 1);
//        for (int k = phTag + 2; k < nodes.size(); k++) {
//            right.add(k, nodes);
//        }
//        System.out.println(right.getQueueAsList().toString());
//        System.out.println(left.getQueueAsList().toString());
//        right.setPhTag(phTag);
//        left.setPhTag(phTag);
//        System.out.println();
////
////        left.split(3);
////        right.split(19);
//
//        ArrayList<Integer> leftNodes = left.getQueueAsList();
//        graphics.setPaint(Color.RED);
//        graphics.setStroke(new BasicStroke(4));
//        for (int node = 0; node < leftNodes.size() - 1; node++) {
//            double uy = nodes.get(leftNodes.get(node)).getY() * 10;
//            double ux = nodes.get(leftNodes.get(node)).getX() * 10;
//            double vy = nodes.get(leftNodes.get(node + 1)).getY() * 10;
//            double vx = nodes.get(leftNodes.get(node + 1)).getX() * 10;
//            graphics.drawLine((int) ux, (int) uy, (int) vx, (int) vy);
//        }
//
//        ArrayList<Integer> rightNodes = right.getQueueAsList();
//        graphics.setPaint(Color.RED);
//        graphics.setStroke(new BasicStroke(4));
//        for (int node = 0; node < rightNodes.size() - 1; node++) {
//            double uy = nodes.get(rightNodes.get(node)).getY() * 10;
//            double ux = nodes.get(rightNodes.get(node)).getX() * 10;
//            double vy = nodes.get(rightNodes.get(node + 1)).getY() * 10;
//            double vx = nodes.get(rightNodes.get(node + 1)).getX() * 10;
//            graphics.drawLine((int) ux, (int) uy, (int) vx, (int) vy);
//        }

        graphics.setPaint(Color.WHITE);
        graphics.setStroke(new BasicStroke(1));
        for (int node = 0; node < nodes2.size() - 1; node++) {
            double uy = nodes2.get(node).getY() * 10;
            double ux = nodes2.get(node).getX() * 10;
            double vy = nodes2.get(node + 1).getY() * 10;
            double vx = nodes2.get(node + 1).getX() * 10;
            graphics.drawLine((int) ux, (int) uy, (int) vx, (int) vy);
        }

        graphics.setPaint(Color.WHITE);
        graphics.setStroke(new BasicStroke(1));
        for (int node = 0; node < nodes.length - 1; node++) {
            double uy = nodes[node].getY() * 10;
            double ux = nodes[node].getX() * 10;
            double vy = nodes[node + 1].getY() * 10;
            double vx = nodes[node + 1].getX() * 10;
            graphics.drawLine((int) ux, (int) uy, (int) vx, (int) vy);
        }

        graphics.setPaint(Color.BLUE);
        graphics.setStroke(new BasicStroke(2));
        for (int node = 0; node < newNodes.size() - 1; node++) {
            double uy = newNodes.get(node).getY() * 10;
            double ux = newNodes.get(node).getX() * 10;
            double vy = newNodes.get(node + 1).getY() * 10;
            double vx = newNodes.get(node + 1).getX() * 10;
            graphics.drawLine((int) ux, (int) uy, (int) vx, (int) vy);
        }

        graphics.setPaint(Color.GREEN);
        graphics.setStroke(new BasicStroke(1));
        for (int node = 0; node < newNodes2.size() - 1; node++) {
            double uy = newNodes2.get(node).getY() * 10;
            double ux = newNodes2.get(node).getX() * 10;
            double vy = newNodes2.get(node + 1).getY() * 10;
            double vx = newNodes2.get(node + 1).getX() * 10;
            graphics.drawLine((int) ux, (int) uy, (int) vx, (int) vy);
        }

        try {
            File outputfile = new File("saved.png");
            ImageIO.write(image, "png", outputfile);
        } catch (IOException e) {
            // handle exception
        }

    }
}

