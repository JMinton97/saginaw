package project.test;

import org.junit.Assert;
import org.junit.Test;
import project.kdtree.Tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class TreeTest {

    @Test
    public void TestTree() {
        Tree tree = new Tree(20);

        Random r = new Random();

        tree.insert(0, new double[]{5, 6});
        ArrayList<double[]> dictionary = new ArrayList<double[]>();
        for(int x = 0; x < 1000; x++){
            double[] loc = new double[]{r.nextInt(100), r.nextInt(100)};
            tree.insert(x, loc);
            dictionary.add(loc);
        }

        double[] findLoc = new double[]{75, 25};

        Integer found = tree.nearest(findLoc, dictionary).getKey();

        Integer actualClosest = -1;
        double[] closestNode = new double[]{Integer.MAX_VALUE, Integer.MAX_VALUE};

        for(int x = 0; x < 1000; x++){
            if(distance(findLoc, dictionary.get(x)) < distance(findLoc, closestNode)){
                closestNode = dictionary.get(x);
                actualClosest = x;
            }
        }


        Integer foundClosest = tree.nearest(findLoc, dictionary).getKey();
        System.out.println(dictionary.get(actualClosest)[0] + "," + dictionary.get(actualClosest)[1] + " " + dictionary.get(foundClosest)[0] + "," + dictionary.get(foundClosest)[1]);
        Assert.assertEquals(distance(closestNode, findLoc), distance(dictionary.get(foundClosest), findLoc), 0);
    }

    //Credit to https://medium.com/allthingsdata/java-implementation-of-haversine-formula-for-distance-calculation-between-two-points-a3af9562ff1
    public static double distance(double[] nodeA, double[] nodeB){
        double xDist = Math.abs(nodeA[0] - nodeB[0]);
        double yDist = Math.abs(nodeA[1] - nodeB[1]);
        return Math.sqrt(Math.pow(xDist, 2) + Math.pow(yDist, 2));
    }
}
