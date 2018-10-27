package project.map;

import javafx.scene.shape.Path;
import javafx.scene.shape.Polyline;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DouglasPeucker {

    Point2D phTag;

    public static List<Long> decimate(List<Long> nodes, Double threshold, Map<Long, MyNode> dictionary) {
        Point2D first = dictionary.get(nodes.get(0)).getPoint();
        Point2D last = dictionary.get(nodes.get(nodes.size() - 1)).getPoint();
        Line2D line = new Line2D.Double(first, last);
        int furthest = 0;
        Double distance = 0.0;
        for(int i = 0; i < nodes.size(); i++) {
            Double thisDistance = line.ptLineDist(dictionary.get(nodes.get(i)).getPoint());
            if (thisDistance > distance) {
                furthest = i;
                distance = thisDistance;
            }
        }
        if(distance > threshold){
            List<Long> segment1 = decimate(nodes.subList(0, furthest), threshold, dictionary);
            List<Long> segment2 = decimate(nodes.subList(furthest, nodes.size()), threshold, dictionary);
            segment1.addAll(segment2.subList(1, segment2.size()));
            return segment1;
        } else {
            ArrayList<Long> returnList = new ArrayList<Long>();
            returnList.add(nodes.get(0));
            returnList.add(nodes.get(nodes.size() - 1));
            return returnList;
        }
    }

    public ArrayList<Point2D> simplify(ArrayList<Point2D> nodes){
        PathHull[] leftAndRight = build(nodes);
        ArrayList<Point2D> newNodes = new ArrayList<>();
        newNodes.add(nodes.get(0));
        newNodes.addAll(DPHull(nodes.get(0), nodes.get(nodes.size() - 1), leftAndRight));
        return newNodes;
    }

    public ArrayList<Point2D> DPHull(Point2D i, Point2D j, PathHull[] leftAndRight) {
        ArrayList<Point2D> returnPoints = new ArrayList<>();
        PathHull left = leftAndRight[0];
        PathHull right = leftAndRight[1];
        Line2D line = new Line2D.Double(i, j);
        if (right.getQueueAsList().size() + left.getQueueAsList().size() < 4) {
            returnPoints.add(j);
            return returnPoints;
        }
        Point2D lextr = findExtreme(left, line);
        Double ldist = line.ptLineDist(lextr);
        Point2D rextr = findExtreme(right, line);
        Double rdist = line.ptLineDist(rextr);
        if (ldist <= rdist) {
            if (false) {
                //no split, return j
                returnPoints.add(j);
                return returnPoints;
            } else {
                PathHull[] leftAndRightR;
                if (right. == rextr) {
                    leftAndRightR = build(left.getQueueAsList());
                } else {
                    right.split(rextr);
                    leftAndRightR = new PathHull[]{left, right};
                }
                returnPoints = DPHull(i, rextr, leftAndRightR);
                leftAndRight = build(right.getQueueAsList());
                returnPoints.addAll(DPHull(rextr, j, leftAndRight));
                return returnPoints;
            }
        } else {
            if (false) {
                //no split, return j
                returnPoints.add(j);
                return returnPoints;
            } else {
                PathHull[] leftAndRightL;
                leftAndRight = build(right.getQueueAsList());
                returnPoints = DPHull(i, lextr, leftAndRight);
                left.split(lextr);
                leftAndRightL = new PathHull[]{left, right};
                returnPoints.addAll(DPHull(lextr, j, leftAndRightL));
                return returnPoints;
            }
        }
    }

    public PathHull[] build(ArrayList<Point2D> points){
        int phTag = points.size() / 2; //or points.size() - 1 / 2?
        PathHull left = new PathHull(points.get(phTag), points.get(phTag - 1));
        for(int k = phTag - 2; k >= 0; k--){
            left.add(points.get(k));
        }
        PathHull right = new PathHull(points.get(phTag), points.get(phTag + 1));
        for(int k = phTag + 2; k < points.size(); k++){
            right.add(points.get(k));
        }
        this.phTag = points.get(phTag);
        return new PathHull[] {left, right};
    }

    public static Point2D findExtreme(PathHull pathHull, Line2D line){
        ArrayList<Point2D> list = pathHull.getQueueAsList();
        if(pathHull.deque.size() > 6){
            int brk, mid, m1, m2;
            int low = 0;
            int high = list.size() - 1;
            boolean signBreak;
            boolean signBase = slopeSign(line, list.get(low), list.get(high));
            do{
                brk = (low + high) / 2;
                if (signBase == (signBreak = slopeSign(line, list.get(brk), list.get(brk + 1)))){
                    if (signBase == (slopeSign(line, list.get(low), list.get(brk + 1)))){
                        low = brk + 1;
                    }else{
                        high = brk;
                    }
                }
            }while (signBase == signBreak);

            m1 = brk;
            while (low < m1){
                mid = (low + m1) / 2;
                if (signBase == (slopeSign(line, list.get(mid), list.get(mid + 1)))){
                    low = mid + 1;
                } else {
                    m1 = mid;
                }
            }

            m2 = brk;
            while (m2 < high){
                mid = (m2 + high) / 2;
                if (signBase == (slopeSign(line, list.get(mid), list.get(mid + 1)))){
                    high = mid;
                } else {
                    m2 = mid + 1;
                }
            }
            if(line.ptLineDist(list.get(low)) > line.ptLineDist(list.get(m2))){
                return list.get(low);
            } else {
                return list.get(m2);
            }
        } else {
            double maxDist = 0;
            Point2D max = list.get(0);
            for(Point2D p : list){
                if (line.ptLineDist(p) > maxDist){
                    maxDist = line.ptLineDist(p);
                    max = p;
                }
            }
            return max;
        }
    }



    public static boolean slopeSign (Line2D line, Point2D a, Point2D b){ //returns true for a 'positive' line, false otherwise
        double l1x = line.getP1().getX();
        double l1y = line.getP1().getY();
        double l2x = line.getP2().getX();
        double l2y = line.getP2().getY();
        double lX = -l1y + l2y;
        double lY = l1x - l2x;
        double res = (lX * (a.getX() - b.getX())) + (lY * (a.getY() - b.getY()));
        return res >= 0;
    }

    /** ∗ ∗ ∗ ∗ ∗ ∗
     Calculates if {@code r} is to the left of {@code p} as {@code p} faces {@code q} @param p the base {@code Point}
     @param q the direction {@code Point} that p is facing
     @param r the query {@code Point}
     @return {@code true} if {@code r} is to the left of {@code p} as {@code p} faces {@code q}, else {@code false}
     */

    public static boolean leftOf(Point2D p, Point2D q, Point2D r) {
        return ( ((q.getX() - p.getX()) * (r.getY() - p.getY()) + (q.getY() - p.getY()) * (r.getX() - p.getX()) ) >= 0);
    }
}