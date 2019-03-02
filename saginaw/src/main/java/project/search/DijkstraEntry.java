package project.search;

public class DijkstraEntry {
    private int node;
    private double distance;

    public DijkstraEntry(int node, double distance){
        this.node = node;
        this.distance = distance;
    }

    public void setNode(int node) {
        this.node = node;
    }

    public int getNode(){
        return node;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getDistance() {
        return distance;
    }
}
