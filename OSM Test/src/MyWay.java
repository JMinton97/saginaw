import java.util.ArrayList;
import java.util.List;

public class MyWay {
    public List<Long> getWayNodes() {
        return wayNodes;
    }

    public void setWayNodes(ArrayList<Long> wayNodes) {
        this.wayNodes = wayNodes;
    }

    public void addWayNode(long wayNode) {
        wayNodes.add(wayNode);
    }

    public List<Long> wayNodes = new ArrayList<Long>();

    public long getWayId() {
        return wayId;
    }

    public void setWayId(long wayId) {
        this.wayId = wayId;
    }

    public long wayId;

    public String print() {
        StringBuilder sb = new StringBuilder().append(String.valueOf(wayId));
        for (long n : wayNodes) {
            sb.append(String.valueOf(n) + " ");
        }
        return sb.toString();
    }

    public WayType getType() {
        return type;
    }

    public void setType(WayType type) {
        this.type = type;
    }

    WayType type;

    public RoadType getRoadType() {
        return roadType;
    }

    public void setRoadType(RoadType roadType) {
        this.roadType = roadType;
    }

    RoadType roadType;

}
