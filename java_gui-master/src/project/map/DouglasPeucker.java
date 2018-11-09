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

//    public static List<MyNode> decimate(List<MyNode> nodes, Double threshold) {
//        MyNode first = nodes.get(0);
//        MyNode last = nodes.get(nodes.size() - 1);
//        Line2D line = new Line2D.Double(first, last);
//        int furthest = 0;
//        Double distance = 0.0;
//        for(int i = 0; i < nodes.size(); i++) {
//            Double thisDistance = line.ptLineDist(nodes.get(i));
//            if (thisDistance > distance) {
//                furthest = i;
//                distance = thisDistance;
//            }
//        }
//        if(distance > threshold){
//            List<MyNode> segment1 = decimate(nodes.subList(0, furthest + 1), threshold);
//            List<MyNode> segment2 = decimate(nodes.subList(furthest, nodes.size()), threshold);
//            segment1.addAll(segment2.subList(1, segment2.size()));
//            return segment1;
//        } else {
//            ArrayList<MyNode> returnList = new ArrayList<MyNode>();
//            returnList.add(nodes.get(0));
//            returnList.add(nodes.get(nodes.size() - 1));
//            return returnList;
//        }
//    }

    public static ArrayList<MyNode> simplify(ArrayList<MyNode> nodes, double tolerance){
        long startTime = System.nanoTime();
        PathHull[] leftAndRight = build(nodes, 0, nodes.size() - 1);
        long endTime = System.nanoTime();
        System.out.println("Inner: " + (endTime - startTime) / 1000);
        ArrayList<MyNode> newNodes = new ArrayList<>();
        newNodes.add(nodes.get(0));
        newNodes.addAll(DPHull(nodes, 0, nodes.size() - 1, leftAndRight, tolerance));
        return newNodes;
    }

    public static ArrayList<MyNode> DPHull(ArrayList<MyNode> points, int i, int j, PathHull[] leftAndRight, double tolerance) {
//        System.out.println("DPHull");
        System.out.println("i = " + i + " j = " + j );
        ArrayList<MyNode> returnPoints = new ArrayList<>();
        PathHull left = leftAndRight[0];
        PathHull right = leftAndRight[1];
        double[] line = crossProduct(points.get(i), points.get(j));
        double len_sq = line[1] * line[1] + line[2] * line[2];

        if (j - i <= 1) {
            returnPoints.add(points.get(j));
            return returnPoints;
        }

        int lextr = findExtreme(points, left, line);
        double ldist = dotProduct(points.get(lextr), line);
        System.out.println("ldist " + ldist);
        if(left.getQueueAsList().length == 0){
            ldist = 0;
            System.exit(0);
        }

        int rextr = findExtreme(points, right, line);
        double rdist = dotProduct(points.get(rextr), line);
        System.out.println("rdist " + rdist);
        if(right.getQueueAsList().length == 0){
            rdist = 0;
            System.out.println("WAAAAAAH");
            System.exit(0);
        }
        System.out.println(points.get(lextr).getNodeId());

//        System.out.println(i + " " + j);
//        System.out.println(points.get(i).getNodeId() + " " + points.get(j).getNodeId());
//        System.out.println((rdist == 0 && ldist == 0));
//        if((rdist == 0 && ldist == 0)){
//            System.exit(0);
//        }
        if (ldist <= rdist) { //split on rextr
//            System.out.println("right");
            if (rdist * rdist <= len_sq * (tolerance * tolerance)) {
                returnPoints.add(points.get(j));
                return returnPoints;
            } else {
                System.out.println("OOOOOHHHHHH");
                if (right.getPhTag() == rextr) {
                    leftAndRight = build(points, i, rextr);
                } else {
                    right.split(rextr);
                    leftAndRight = new PathHull[]{left, right};
                }
                System.out.println("Hulling to rextr " + rextr);
                returnPoints = DPHull(points, i, rextr, leftAndRight, tolerance);
                leftAndRight = build(points, rextr, j);
                System.out.println("Hulling from rextr " + rextr);
                returnPoints.addAll(DPHull(points, rextr, j, leftAndRight, tolerance));
                return returnPoints;
            }
        } else { //split on lextr
//            System.out.println("left");
            if (ldist * ldist <= len_sq * (tolerance * tolerance)) {
                returnPoints.add(points.get(j));
                return returnPoints;
            } else {
                left.split(lextr);
                System.out.println("Hulling from lextr " + lextr);
                ArrayList<MyNode> returnPoints2 = new ArrayList<>();
                returnPoints2 = DPHull(points, lextr, j, new PathHull[]{left, right}, tolerance);
                leftAndRight = build(points, i, lextr);
//                System.out.println("i " + i + " lextr " + lextr);
                System.out.println("Hulling to lextr " + lextr);
                returnPoints = DPHull(points, i, lextr, leftAndRight, tolerance);
                returnPoints.addAll(returnPoints2);
                return returnPoints;
            }
        }
    }

    private static PathHull[] build(ArrayList<MyNode> points, int i, int j){
        int phTag = i + ((j - i) / 2);
        PathHull left = new PathHull(phTag, phTag - 1);
        for(int k = phTag - 2; k >= i; k--){
            left.add(k, points);
        }
        PathHull right = new PathHull(phTag, phTag + 1);
        for(int k = phTag + 2; k <= j; k++){
            right.add(k, points);
        }
//        System.out.println("Built from " + i + " to " + j + ". Left length: " + left.getQueueAsList().length + ". Right length: " + right.getQueueAsList().length);
        right.setPhTag(phTag);
        left.setPhTag(phTag);

        return new PathHull[] {left, right};
    }

    private static int findExtreme(ArrayList<MyNode> nodes, PathHull pathHull, double[] line){
//        ArrayList<Integer> list = pathHull.getQueueAsList();
        int[] list = pathHull.getQueueAsList();
//        System.out.println("List length" + list.length);
        if(list.length > 6){
            int brk, mid, m1, m2;
            int low = 0;
            int high = list.length - 2;
            boolean signBreak;
            boolean signBase = slopeSign(line, nodes.get(list[high]), nodes.get(list[low]));
            do{
//                System.out.println("here");
//                System.out.println("high " + high + " low " + low);
//                for(int x : pathHull.getQueueAsList()){
//                    System.out.print(nodes.get(x).getNodeId() + " ");
//                }
                brk = (low + high) / 2;
                signBreak = slopeSign(line, nodes.get(list[brk]), nodes.get(list[brk + 1]));
                if (signBase == signBreak){
                    if (signBase == (slopeSign(line, nodes.get(list[low]), nodes.get(list[brk + 1])))){
                        low = brk + 1;
                    }else{

                        high = brk;
                    }
                }
            }while (signBase == signBreak);

            m1 = brk;
            while (low < m1){
                mid = (low + m1) / 2;
                if (signBase == (slopeSign(line, nodes.get(list[mid]), nodes.get(list[mid + 1])))){
                    low = mid + 1;
                } else {
                    m1 = mid;
                }
            }

            m2 = brk;
            while (m2 < high){
                mid = (m2 + high) / 2;
                if (signBase == (slopeSign(line, nodes.get(list[mid]), nodes.get(list[mid + 1])))){
                    high = mid;
                } else {
                    m2 = mid + 1;
                }
            }

            if(dotProduct(nodes.get(list[low]), line) > dotProduct(nodes.get(list[m2]), line)){
                System.out.println("extremity is " + list[low]);
                return list[low];
            } else {
                System.out.println("extremity is " + list[m2]);
                return list[m2];
            }
        } else {
//            System.out.println("yeah");
            System.out.println("less than six, in fact " + list.length);
            double maxDist = 0;
            int max = list[0];
            for(Integer p : list){
                if (dotProduct(nodes.get(p), line) > maxDist){
                    maxDist = dotProduct(nodes.get(p), line);
                    max = p;
                }
            }
            System.out.println("extremity (6) is " + max + "at distance " + maxDist);
            return max;
        }
    }

    public static boolean slopeSign (double[] line, MyNode a, MyNode b){ //returns true for a 'positive' line, false otherwise
        double res = (line[1] * (a.getX() - b.getX())) + (line[2] * (a.getY() - b.getY()));
        return res >= 0;
    }

    public static double[] crossProduct(MyNode p, MyNode q){
        double[] line = new double[3];
        line[0] = (p.getX() * q.getY()) - (p.getY() * q.getX());
        line[1] = - q.getY() + p.getY();
        line[2] = q.getX() - p.getX();
        return line;
    }

    public static double dotProduct(MyNode p, double[] line){
        return Math.abs(line[0] + (p.getX() * line[1]) + (p.getY() * line[2]));
    }


    /** ∗ ∗ ∗ ∗ ∗ ∗
     Calculates if {@code r} is to the left of {@code p} as {@code p} faces {@code q} @param p the base {@code Point}
     @param q the direction {@code Point} that p is facing
     @param r the query {@code Point}
     @return {@code true} if {@code r} is to the left of {@code p} as {@code p} faces {@code q}, else {@code false}
     */

    public static boolean leftOf(MyNode p, MyNode q, MyNode r) {
        double qx = q.getX();
        double qy = q.getY();
        double px = p.getX();
        double py = p.getY();
        double rx = r.getX();
        double ry = r.getY();

        return ( ((qx - px) * (ry - py) - (qy - py) * (rx - px) ) > 0);
    }
}