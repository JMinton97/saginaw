package project.map;

public class DijkstraEntry {
    private long node;
    private double distance;

    public DijkstraEntry(long node, double distance){
        this.node = node;
        this.distance = distance;
    }

    public void setNode(long node) {
        this.node = node;
    }

    public long getNode(){
        return node;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getDistance() {
        return distance;
    }
}
