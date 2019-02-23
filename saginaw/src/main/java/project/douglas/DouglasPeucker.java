package project.douglas;

import java.awt.geom.Point2D;
import java.util.ArrayList;

public class DouglasPeucker {

    public static ArrayList<Point2D.Double> simplify(ArrayList<Point2D.Double> nodes, double tolerance){
        long startTime = System.nanoTime();
        PathHull[] leftAndRight = build(nodes, 0, nodes.size() - 1);
        long endTime = System.nanoTime();
//        System.out.println("Build time: " + (endTime - startTime) / 1000);
        ArrayList<Point2D.Double> newNodes = new ArrayList<>();
        newNodes.add(nodes.get(0));
        newNodes.addAll(DPHull(nodes, 0, nodes.size() - 1, leftAndRight, tolerance));
        return newNodes;
    }

    private static ArrayList<Point2D.Double> DPHull(ArrayList<Point2D.Double> points, int i, int j, PathHull[] leftAndRight, double tolerance) {
        ArrayList<Point2D.Double> returnPoints = new ArrayList<>();
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
        int rextr = findExtreme(points, right, line);
        double rdist = dotProduct(points.get(rextr), line);

        if (ldist <= rdist) { //split on rextr
            if (rdist * rdist <= len_sq * (tolerance * tolerance)) {
                returnPoints.add(points.get(j));
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
                returnPoints.add(points.get(j));
                return returnPoints;
            } else {
                left.split(lextr);
                ArrayList<Point2D.Double> returnPoints2;
                returnPoints2 = DPHull(points, lextr, j, new PathHull[]{left, right}, tolerance);
                leftAndRight = build(points, i, lextr);
                returnPoints = DPHull(points, i, lextr, leftAndRight, tolerance);
                returnPoints.addAll(returnPoints2);
                return returnPoints;
            }
        }
    }

    private static PathHull[] build(ArrayList<Point2D.Double> points, int i, int j){
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

    private static int findExtreme(ArrayList<Point2D.Double> nodes, PathHull pathHull, double[] line){
        int[] list = pathHull.getQueueAsList();
        if(list.length > 6){
            int brk, mid, m1, m2;
            int low = 0;
            int high = list.length - 2;
            boolean signBreak;
            boolean signBase = slopeSign(line, nodes.get(list[high]), nodes.get(list[low]));
            do{
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
                return list[low];
            } else {
                return list[m2];
            }
        } else {
            double maxDist = 0;
            int max = 0;
            for(Integer p : list){
                if (dotProduct(nodes.get(p), line) > maxDist){
                    maxDist = dotProduct(nodes.get(p), line);
                    max = p;
                }
            }
            return max;
        }
    }

    private static boolean slopeSign (double[] line, Point2D.Double a, Point2D.Double b){ //returns true for a 'positive' line, false otherwise
        double res = (line[1] * (a.getX() - b.getX())) + (line[2] * (a.getY() - b.getY()));
        return res >= 0;
    }

    private static double[] crossProduct(Point2D.Double p, Point2D.Double q){
        double[] line = new double[3];
        line[0] = (p.getX() * q.getY()) - (p.getY() * q.getX());
        line[1] = - q.getY() + p.getY();
        line[2] = q.getX() - p.getX();
        return line;
    }

    private static double dotProduct(Point2D.Double p, double[] line){
        return Math.abs(line[0] + (p.getX() * line[1]) + (p.getY() * line[2]));
    }


    /** ∗ ∗ ∗ ∗ ∗ ∗
     Calculates if {@code r} is to the left of {@code p} as {@code p} faces {@code q} @param p the base {@code Point}
     @param q the direction {@code Point} that p is facing
     @param r the query {@code Point}
     @return {@code true} if {@code r} is to the left of {@code p} as {@code p} faces {@code q}, else {@code false}
     */

    public static boolean leftOf(Point2D.Double p, Point2D.Double q, Point2D.Double r) {
        double qx = q.getX();
        double qy = q.getY();
        double px = p.getX();
        double py = p.getY();
        double rx = r.getX();
        double ry = r.getY();

        return ( ((qx - px) * (ry - py) - (qy - py) * (rx - px) ) > 0);
    }
}