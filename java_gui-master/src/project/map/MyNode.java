package project.map;

import java.awt.geom.Point2D;

public class MyNode {
    public double getLati() {
        return lati;
    }

    public void setLati(double lati) {
        this.lati = lati;
    }

    public double getLongi() {
        return longi;
    }

    public void setLongi(double longi) {
        this.longi = longi;
    }

    public long getNodeId() {
        return nodeId;
    }

    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

    public String print() {
        return String.valueOf(nodeId) + " " + String.valueOf(lati) + " " + String.valueOf(longi);
    }

    protected double lati;
    protected double longi;
    protected long nodeId;

    public Point2D.Double getPoint() {
        return new Point2D.Double(this.longi, this.lati);
    }

    public int visited = 0;

    public void visit() {
        visited++;
    }

    public double getX(){
        return longi;
    }

    public double getY(){
        return lati;
    }

}
