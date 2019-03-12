package project.model;

import java.awt.geom.Point2D;
import java.util.ArrayList;

public class Segment {
    private int startNode;
    private int endNode;
    private ArrayList<Point2D.Double> points;
    private boolean routed;
    private ArrayList<double[]> markers;

    public Segment(int startNode, int endNode){
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public void setPoints(ArrayList<Point2D.Double> points){
        this.points = points;
    }

}