package project.map;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.io.Serializable;

public class Place implements Serializable{
    private String name;
    private Point2D.Double location;
    private Shape textShape;

    private GlyphVector glyphVector;

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

    public Shape getTextShape() {
        return textShape;
    }

    public void setTextShape(Shape textShape) {
        this.textShape = textShape;
    }

    public boolean hasTextShape(){
        return (textShape != null);
    }


    public GlyphVector getGlyphVector() {
        return glyphVector;
    }

    public void setGlyphVector(GlyphVector glyphVector) {
        this.glyphVector = glyphVector;
    }

}
