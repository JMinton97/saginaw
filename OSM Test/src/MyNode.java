public class MyNode {
    public Double getLati() {
        return lati;
    }

    public void setLati(Double lati) {
        this.lati = lati;
    }

    public Double getLongi() {
        return longi;
    }

    public void setLongi(Double longi) {
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

    protected Double lati;
    protected Double longi;
    protected long nodeId;
}
