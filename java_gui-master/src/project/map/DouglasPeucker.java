package project.map;

import javafx.scene.shape.Path;
import javafx.scene.shape.Polyline;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DouglasPeucker {

    public static List<Point2D> decimate(List<Point2D> nodes, Double threshold) {
        Point2D first = nodes.get(0);
        Point2D last = nodes.get(nodes.size() - 1);
        Line2D line = new Line2D.Double(first, last);
        int furthest = 0;
        Double distance = 0.0;
        for(int i = 0; i < nodes.size(); i++) {
            Double thisDistance = line.ptLineDist(nodes.get(i));
            if (thisDistance > distance) {
                furthest = i;
                distance = thisDistance;
            }
        }
        if(distance > threshold){
            List<Point2D> segment1 = decimate(nodes.subList(0, furthest + 1), threshold);
            List<Point2D> segment2 = decimate(nodes.subList(furthest, nodes.size()), threshold);
            segment1.addAll(segment2.subList(1, segment2.size()));
            return segment1;
        } else {
            ArrayList<Point2D> returnList = new ArrayList<Point2D>();
            returnList.add(nodes.get(0));
            returnList.add(nodes.get(nodes.size() - 1));
            return returnList;
        }
    }

    public static ArrayList<Point2D> simplify(Point2D[] nodes, double tolerance){
        long startTime = System.nanoTime();
        PathHull[] leftAndRight = build(nodes, 0, nodes.length - 1);
        long endTime = System.nanoTime();
        System.out.println("Inner: " + (endTime - startTime) / 10000);
        ArrayList<Point2D> newNodes = new ArrayList<>();
        newNodes.add(nodes[0]);
        newNodes.addAll(DPHull(nodes, 0, nodes.length - 1, leftAndRight, tolerance));
        return newNodes;
    }

    public static ArrayList<Point2D> DPHull(Point2D[] points, int i, int j, PathHull[] leftAndRight, double tolerance) {
        System.out.println("hulling");
        ArrayList<Point2D> returnPoints = new ArrayList<>();
        PathHull left = leftAndRight[0];
        PathHull right = leftAndRight[1];
        double[] line = crossProduct(points[i], points[j]);
        if (j - i <= 1) {
            returnPoints.add(points[j]);
            return returnPoints;
        }
        int lextr = findExtreme(points, left, line);
        double ldist = dotProduct(points[lextr], line);
        double len_sq = line[1] * line[1] + line[2] * line[2];
        int rextr = findExtreme(points, right, line);
        double rdist = dotProduct(points[rextr], line);
        if (ldist <= rdist) { //split on rextr
            if (rdist * rdist <= len_sq * (tolerance * tolerance)) {

                returnPoints.add(points[j]);
                return returnPoints;
            } else {
                if (right.getPhTag() == rextr) {
                    leftAndRight = build(points, i, rextr);
                } else {
                    right.split(rextr);
                    leftAndRight = new PathHull[]{left, right};
                }
                returnPoints = DPHull(points, i, rextr, leftAndRight, tolerance);
                leftAndRight = build(points, rextr, j);
                returnPoints.addAll(DPHull(points, rextr, j, leftAndRight, tolerance));
                return returnPoints;
            }
        } else { //split on lextr
            if (ldist * ldist <= len_sq * (tolerance * tolerance)) {
                returnPoints.add(points[j]);
                return returnPoints;
            } else {
                left.split(lextr);
                ArrayList<Point2D> returnPoints2 = new ArrayList<>(DPHull(points, lextr, j, new PathHull[]{left, right}, tolerance));                leftAndRight = build(points, i, lextr);
                returnPoints = DPHull(points, i, lextr, leftAndRight, tolerance);
                returnPoints.addAll(returnPoints2);
                return returnPoints;
            }
        }
    }

    private static PathHull[] build(Point2D[] points, int i, int j){
        System.out.println("building");
        int phTag = i + ((j - i) / 2);
        PathHull left = new PathHull(phTag, phTag - 1);
        for(int k = phTag - 2; k >= i; k--){
            left.add(k, points);
        }
        PathHull right = new PathHull(phTag, phTag + 1);
        for(int k = phTag + 2; k <= j; k++){
            right.add(k, points);
        }
        right.setPhTag(phTag);
        left.setPhTag(phTag);

        return new PathHull[] {left, right};
    }

    private static int findExtreme(Point2D[] nodes, PathHull pathHull, double[] line){
        System.out.println("looking");
//        ArrayList<Integer> list = pathHull.getQueueAsList();
        int[] list = pathHull.getQueueAsList();
        if(list.length > 6){
            int brk, mid, m1, m2;
            int low = 0;
            int high = list.length - 2;
            boolean signBreak;
            boolean signBase = slopeSign(line, nodes[list[low]], nodes[list[high]]);
            do{
//                System.out.println("here");
                brk = (low + high) / 2;
                signBreak = slopeSign(line, nodes[list[brk]], nodes[list[brk + 1]]);
                if (signBase == signBreak){
                    if (signBase == (slopeSign(line, nodes[list[low]], nodes[list[brk + 1]]))){
                        low = brk + 1;
                    }else{
                        high = brk;
                    }
                }
            }while (signBase == signBreak);

            m1 = brk;
            while (low < m1){
                mid = (low + m1) / 2;
                if (signBase == (slopeSign(line, nodes[list[mid]], nodes[list[mid + 1]]))){
                    low = mid + 1;
                } else {
                    m1 = mid;
                }
            }

            m2 = brk;
            while (m2 < high){
                mid = (m2 + high) / 2;
                if (signBase == (slopeSign(line, nodes[list[mid]], nodes[list[mid + 1]]))){
                    high = mid;
                } else {
                    m2 = mid + 1;
                }
            }

            if(dotProduct(nodes[list[low]], line) > dotProduct(nodes[list[m2]], line)){
                return list[low];
            } else {
                return list[m2];
            }
        } else {
            double maxDist = 0;
            int max = 0;
            for(Integer p : list){
                if (dotProduct(nodes[p], line) > maxDist){
                    maxDist = dotProduct(nodes[p], line);
                    max = p;
                }
            }
            return max;
        }
    }

    public static boolean slopeSign (double[] line, Point2D a, Point2D b){ //returns true for a 'positive' line, false otherwise
        double res = (line[1] * (a.getX() - b.getX())) + (line[2] * (a.getY() - b.getY()));
        return res >= 0;
    }

    public static double[] crossProduct(Point2D p, Point2D q){
        double[] line = new double[3];
        line[0] = (p.getX() * q.getY()) - (p.getY() * q.getX());
        line[1] = - q.getY() + p.getY();
        line[2] = q.getX() - p.getX();
        return line;
    }

    public static double dotProduct(Point2D p, double[] line){
        return Math.abs(line[0] + (p.getX() * line[1]) + (p.getY() * line[2]));
    }


    /** ∗ ∗ ∗ ∗ ∗ ∗
     Calculates if {@code r} is to the left of {@code p} as {@code p} faces {@code q} @param p the base {@code Point}
     @param q the direction {@code Point} that p is facing
     @param r the query {@code Point}
     @return {@code true} if {@code r} is to the left of {@code p} as {@code p} faces {@code q}, else {@code false}
     */

    public static boolean leftOf(Point2D p, Point2D q, Point2D r) {
        return ( ((q.getX() - p.getX()) * (r.getY() - p.getY()) - (q.getY() - p.getY()) * (r.getX() - p.getX()) ) > 0);
    }
}