package project.view;

import project.kdtree.Tree;
import project.map.MyGraph;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Marker extends JLabel {

    private ArrayList<Marker> markers;
    private double[] location;
    private MyGraph graph;
    private int closestNode;

    public Marker(double[] location, ArrayList<Marker> markers, MyGraph graph){
        this.location = location;
        this.markers = markers;
        this.graph = graph;
        try{
            String filename = "res/icon/middle.png";
            File inputfile = new File(filename);
            setIcon(new ImageIcon(ImageIO.read(inputfile)));
        }catch(IOException e){
            System.out.println("Failed image load.");
        }
    }

    public boolean isStart(){
        return markers.indexOf(this) == 0;
    }

    public boolean isEnd(){
        return markers.indexOf(this) == markers.size() - 1;
    }

    public double[] getGeoLocation() {
        return location;
    }

    public void setGeoLocation(double[] location) {
        this.location = location;
        findClosestNode();
    }


    public int getClosestNode() {
        return closestNode;
    }

    public void findClosestNode() {
        this.closestNode = graph.findClosest(location);
    }

}
