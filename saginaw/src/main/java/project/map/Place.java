package project.map;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.Serializable;

public class Place implements Serializable{
    private String name;
    private Point2D.Double location;

    public Place(String name, double lon, double lat){
        this.name = name;
        this.location = new Point2D.Double(lon, lat);
    }

    public String getName() {
        return name;
    }

    public Point2D.Double getLocation() {
        return location;
    }

    public void setName(String name){
        this.name = name;
    }
}
