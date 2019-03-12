package project.model;

import project.map.MyGraph;

import java.awt.geom.Point2D;
import java.util.ArrayList;

public class Segment {
    private double[] startNode;
    private double[] endNode;
    private ArrayList<Point2D.Double> points;
    private ArrayList<Long> wayIds;
    private boolean routed;
    private boolean resolved;


    public Segment(double[] startNode, double[] endNode){
        this.startNode = startNode;
        this.endNode = endNode;
        resolved = false;
    }

    public void setPoints(ArrayList<Point2D.Double> points){
        this.points = points;
    }

    public ArrayList<Point2D.Double> getPoints(){
        return points;
    }

    public void setWayIds(ArrayList<Long> wayIds){
        this.wayIds = wayIds;
    }

    public ArrayList<Long> getWayIds(){
        return wayIds;
    }

    public void setStartNode(double[] node){
        this.startNode = node;
    }

    public void setEndNode(double[] node){
        this.endNode = node;
    }

    public double[] getStartNode() {
        return startNode;
    }

    public double[] getEndNode() {
        return endNode;
    }

    public void setResolved(boolean bool){
        resolved = bool;
    }

    public boolean isResolved(){
        return resolved;
    }

    public void getFullDetailRoute(MyGraph graph){
        points.clear();
        points.addAll(graph.wayListToNodes(wayIds));

    }

    public boolean hasPoints(){
        return points != null;
    }
}