package project.kdtree;

import javafx.util.Pair;
import org.mapdb.BTreeMap;

import java.io.Serializable;
import java.util.ArrayList;

public class Node implements Serializable{
    private double[] point;
    private long id;
    private Node left;
    private Node right;
    private int depth;
    private boolean leaf;
    private ArrayList<Long> ids;

    public void setPoint(double[] point) {
        this.point = point;
    }

    public void setLeft(Node left) {
        this.left = left;
    }

    public void setRight(Node right) {
        this.right = right;
    }

    protected Node(long id, double[] point, int depth, boolean isLeaf){
        this.point = point;
        this.depth = depth;
        if(isLeaf){
            this.ids = new ArrayList<Long>();
            ids.add(id);
            leaf = true;
        } else {
            this.id = id;
            leaf = false;
        }
        this.left = null;
        this.right = null;
    }

    public boolean isLeaf(){
        return leaf;
    }

    public Node getLeft(){
        return left;
    }

    public Node getRight() {
        return right;
    }

    public double[] getPoint(){
        return point;
    }

    public long getId(){
        return id;
    }

    public void setId(long id){
        if(leaf) {
            ids.add(id);
        } else {
            this.id = id;
        }
    }

    public int getDepth() {
        return depth;
    }

    public Pair<Long, Double> findClosest(double[] searchLoc, BTreeMap<Long, double[]> dictionary){
        long closest = ids.get(0);
        double closestDistance = Double.MAX_VALUE;
        double thisDist;
        for(long id : ids){
            thisDist = distance(searchLoc, dictionary.get(id));
            if(thisDist < closestDistance){
                closest = id;
                closestDistance = thisDist;

            }
        }
        return new Pair<>(closest, closestDistance);
    }

    public double distance(double[] a, double[] b){
        return (((b[0] - a[0]) * (b[0] - a[0])) + ((b[1] - a[1]) * (b[1] - a[1])));
    }
}