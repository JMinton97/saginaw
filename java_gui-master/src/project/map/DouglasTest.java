package project.map;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;

public class DouglasTest {

    static Point2D phTag;

    public static void main(String[] args) {
        ArrayList<Point2D> nodes = new ArrayList<>();

        nodes.add(new Point2D.Double(10, 10));
        nodes.add(new Point2D.Double(20, 5));
        nodes.add(new Point2D.Double(30, 20));
        nodes.add(new Point2D.Double(40, 40));
        nodes.add(new Point2D.Double(50, 20));
        nodes.add(new Point2D.Double(60, 10));
        nodes.add(new Point2D.Double(70, 15));
        nodes.add(new Point2D.Double(80, 5));
        nodes.add(new Point2D.Double(90, 50));
        nodes.add(new Point2D.Double(80, 60));
        nodes.add(new Point2D.Double(75, 50));
        nodes.add(new Point2D.Double(80, 70));
        nodes.add(new Point2D.Double(70, 70));
        nodes.add(new Point2D.Double(60, 40));
        nodes.add(new Point2D.Double(50, 45)); //
        nodes.add(new Point2D.Double(50, 75));
        nodes.add(new Point2D.Double(40, 99));
        nodes.add(new Point2D.Double(30, 80));
        nodes.add(new Point2D.Double(20, 60)); //
        nodes.add(new Point2D.Double(15, 50)); //
        nodes.add(new Point2D.Double(10, 60)); //
        nodes.add(new Point2D.Double(5, 35)); //

        ArrayList<Point2D> newNodes = DouglasPeucker.simplify(nodes);


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
        for (int node = 0; node < nodes.size() - 1; node++) {
            double uy = nodes.get(node).getY() * 10;
            double ux = nodes.get(node).getX() * 10;
            double vy = nodes.get(node + 1).getY() * 10;
            double vx = nodes.get(node + 1).getX() * 10;
            graphics.drawLine((int) ux, (int) uy, (int) vx, (int) vy);
        }

        graphics.setPaint(Color.BLUE);
        graphics.setStroke(new BasicStroke(1));
        for (int node = 0; node < newNodes.size() - 1; node++) {
            double uy = newNodes.get(node).getY() * 10;
            double ux = newNodes.get(node).getX() * 10;
            double vy = newNodes.get(node + 1).getY() * 10;
            double vx = newNodes.get(node + 1).getX() * 10;
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
