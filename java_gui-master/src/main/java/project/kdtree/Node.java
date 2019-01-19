package project.kdtree;

import java.io.Serializable;

public class Node implements Serializable{
    private double[] point;
    private long id;
    private Node left;
    private Node right;

    public void setPoint(double[] point) {
        this.point = point;
    }

    public void setLeft(Node left) {
        this.left = left;
    }

    public void setRight(Node right) {
        this.right = right;
    }

    protected Node(long id, double[] point){
        this.point = point;
        this.id = id;
        this.left = null;
        this.right = null;
    }

    public boolean isLeaf(){
        return (left == null) && (right == null);
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
        this.id = id;
    }
}
