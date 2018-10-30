package project.map;

import javafx.scene.shape.Path;
import javafx.scene.shape.Polyline;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DouglasPeucker {

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

    public static ArrayList<Point2D> simplify(ArrayList<Point2D> nodes){
        PathHull[] leftAndRight = build(nodes, 0, nodes.size() - 1);
        ArrayList<Point2D> newNodes = new ArrayList<>();
        newNodes.add(nodes.get(0));
        newNodes.addAll(DPHull(nodes, 0, nodes.size() - 1, leftAndRight));
        return newNodes;
    }

    public static ArrayList<Point2D> DPHull(ArrayList<Point2D> points, int i, int j, PathHull[] leftAndRight) {
        ArrayList<Point2D> returnPoints = new ArrayList<>();
        PathHull left = leftAndRight[0];
        PathHull right = leftAndRight[1];
        System.out.println("ph: " + left.getPhTag());
        System.out.println(left.getPhTag() == right.getPhTag());
        Line2D line = new Line2D.Double(points.get(i), points.get(j));
        if (j - i < 1) {
            returnPoints.add(points.get(j));
            return returnPoints;
        }
        int lextr = findExtreme(points, left, line);
        System.out.println("found left");
        Double ldist = line.ptLineDist(points.get(lextr));
        System.out.println(left.getQueueAsList());
        System.out.println(right.getQueueAsList());
        int rextr = findExtreme(points, right, line);
        System.out.println("found right");
        Double rdist = line.ptLineDist(points.get(rextr));
        System.out.println("i AND j: " + i + " " + j);
        System.out.println("LEFT AND RIGHT: " + lextr + " " + rextr);
        if (ldist <= rdist) { //split on rextr
            System.out.println("SPLIT ON RIGHT, " + rextr);
            if (rdist < 40) {
                //no split, return j
                System.out.println("no split");
                returnPoints.add(points.get(j));
                return returnPoints;
            } else {
                PathHull[] leftAndRightR;
                if (right.getPhTag() == rextr) {
                    System.out.println("yip");
                    leftAndRight = build(points, i, rextr);
                } else {
                    right.split(rextr);
                    leftAndRight = new PathHull[]{left, right};
                }
//                System.out.println("leftandRightR: " + leftAndRightR[0].deque + " " + leftAndRightR[1].deque);
                returnPoints = DPHull(points, i, rextr, leftAndRight);
                System.out.println(returnPoints);
                System.out.println("here");
                leftAndRight = build(points, rextr, j);
                returnPoints.addAll(DPHull(points, rextr, j, leftAndRight));
                return returnPoints;
            }
        } else { //split on lextr
            System.out.println("SPLIT ON LEFT, " + lextr);
            if (ldist < 40) {
                System.out.println("nosplit");
                //no split, return j
                returnPoints.add(points.get(j));
                return returnPoints;
            } else {
                ArrayList<Point2D> returnPoints2 = new ArrayList<>();
                left.split(lextr);
                System.out.println("here");
                returnPoints2.addAll(DPHull(points, lextr, j, new PathHull[]{left, right}));
                System.out.println("i: " + i + " lextr: " + lextr + " j: " + j);
                leftAndRight = build(points, i, lextr);
                returnPoints = DPHull(points, i, lextr, leftAndRight);
                returnPoints.addAll(returnPoints2);
                return returnPoints;
            }
        }
    }

    public static PathHull[] build(ArrayList<Point2D> points, int i, int j){
        System.out.println("Building with i: " + i + " j: " + j);
        int phTag = i + ((j - i) / 2); //or points.size() - 1 / 2?
        PathHull left = new PathHull(phTag, phTag - 1);
        for(int k = phTag - 2; k >= i; k--){
            left.add(k, points);
//            System.out.println(k);
        }
        System.out.println("left deque " + left.deque);
        PathHull right = new PathHull(phTag, phTag + 1);
        for(int k = phTag + 2; k <= j; k++){     //
            right.add(k, points);
//            System.out.println(k);
        }
        System.out.println(right.deque);
        right.setPhTag(phTag);
        left.setPhTag(phTag);

        return new PathHull[] {left, right};
    }

    public static int findExtreme(ArrayList<Point2D> nodes, PathHull pathHull, Line2D line){
        ArrayList<Integer> list = pathHull.getQueueAsList();
        System.out.println(list.size());
        if(list.size() > 6){
            System.out.println("doing a thing");
            int brk, mid, m1, m2;
            int low = 0;
            int high = list.size() - 2;
            boolean signBreak;
            boolean signBase = slopeSign(line, nodes.get(list.get(low)), nodes.get(list.get(high)));
//            System.out.println(nodes.get(list.get(low)) + " " + nodes.get(list.get(high)));
//            System.out.println(signBase);
//            System.out.println();
//            for (int x = 0; x < list.size() - 1; x++){
//                System.out.println(x + " " + slopeSign(line, nodes.get(list.get(x)), nodes.get(list.get(x + 1))));
//            }
            do{
//                System.out.println("loop");
                brk = (low + high) / 2;
//                System.out.println(low + " " + high + " " + brk);
                signBreak = slopeSign(line, nodes.get(list.get(brk)), nodes.get(list.get(brk + 1)));
//                System.out.println(signBreak);
                if (signBase == signBreak){
//                    System.out.println("yeah");
                    if (signBase == (slopeSign(line, nodes.get(list.get(low)), nodes.get(list.get(brk + 1))))){
//                        System.out.println("low");
                        low = brk + 1;
                    }else{
//                        System.out.println("high");
                        high = brk;
                    }
                }
//                System.out.println(nodes.get(list.get(low)) + " " + nodes.get(list.get(high)));
            }while (signBase == signBreak);

//            System.out.println("brk " + brk);
//            low = 0;
//            high = list.size() - 2;
            m1 = brk;
            System.out.println(low + " " + high);
            while (low < m1){
//                System.out.println("loop");
                mid = (low + m1) / 2;
                if (signBase == (slopeSign(line, nodes.get(list.get(mid)), nodes.get(list.get(mid + 1))))){
                    low = mid + 1;
                } else {
                    m1 = mid;
                }
            }
//            System.out.println(list.get(m1));

//            low = 0;
//            high = list.size() - 2;
            m2 = brk;
//            System.out.println(brk + " " + high);
            while (m2 < high){
                mid = (m2 + high) / 2;
                if (signBase == (slopeSign(line, nodes.get(list.get(mid)), nodes.get(list.get(mid + 1))))){
                    high = mid;
                } else {
                    m2 = mid + 1;
                }
            }
//            System.out.println(list.get(m2));
            if(line.ptLineDist(nodes.get(list.get(low))) > line.ptLineDist(nodes.get(list.get(m2)))){
                System.out.println("returning " + list.get(low));
                return list.get(low);
            } else {
                System.out.println("returning " + list.get(low));
                return list.get(m2);
            }
        } else {
            System.out.println(list);
            System.out.println("lower than seven");
            double maxDist = 0;
            int max = 0;
            for(Integer p : list){
                System.out.println(p);
                System.out.println(nodes.size());
                if (line.ptLineDist(nodes.get(p)) > maxDist){
                    maxDist = line.ptLineDist(nodes.get(p));
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
        return ( ((q.getX() - p.getX()) * (r.getY() - p.getY()) - (q.getY() - p.getY()) * (r.getX() - p.getX()) ) >= 0);
    }
}