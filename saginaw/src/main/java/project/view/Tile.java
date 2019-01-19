package project.view;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Tile {

    private Point2D topLeft;
    private Point2D bottomRight;
    private int x;
    private int y;
    private int z;
    private double scale;
    private double imageEdge;
    private BufferedImage image;
    private long lastAccess;
    private String region;

    public Tile(int x, int y, int z, double scale, double imageEdge, String region){
        this.x = x;
        this.y = y;                                             //ADD 1 TO THESE FOR IMAGES?!?
        this.z = z;
        this.scale = scale;
        this.imageEdge = imageEdge;
        this.region = region;
    }

    public void setTopLeftAndBottomRight(Point2D topLeft, Point2D bottomRight){
        this.topLeft = topLeft;
        this.bottomRight = bottomRight;
    }

    public Point2D getTopLeft(){
        return topLeft;
    }


    public void loadImage(){
        try{
            System.out.println("Loading image.");
            image = ImageIO.read(new File("draw/" + region + "/" + z + "/" + x + "-" + y + ".png"));
        } catch(IOException e){
            System.out.println("Couldn't load " + "/" + z + "/" + x + "-" + y + ".png");
            image = new BufferedImage((int) imageEdge, (int) imageEdge, 1);
            Graphics g = image.getGraphics();
            g.setColor(Color.cyan);
            g.fillRect(0, 0, (int) imageEdge, (int) imageEdge);
        }
    }

    public BufferedImage getImage(){
        if(image == null){
            loadImage();
        }
        return image;
    }

    public boolean overlaps(Point2D viewTopLeft, Point2D viewBottomRight){

//        System.out.println(viewTopLeft + " v " + topLeft + "    " + viewBottomRight + " v " + bottomRight);
//        if (topLeft.getX() > viewBottomRight.getX() || viewTopLeft.getX() > bottomRight.getX()){
//            return false;
//        }
//
//        if (topLeft.getY() < viewBottomRight.getY() || viewTopLeft.getY() < bottomRight.getY()){
//            return false;
//        }
//
//        return true;

//        System.out.println(viewTopLeft.getX() + " < " + bottomRight.getX() + "   " + viewBottomRight.getX() + " > " + topLeft.getX() + "   " + viewTopLeft.getY() + " > " + bottomRight.getY() + "   " + viewBottomRight.getY() + " < " + topLeft.getY());

        if((viewTopLeft.getX() <= bottomRight.getX()) && (viewBottomRight.getX() >= topLeft.getX()) && //add some tolerances for .99999s
                (viewTopLeft.getY() >= bottomRight.getY()) && (viewBottomRight.getY() <= topLeft.getY())){
//            System.out.println("#####" + viewTopLeft.getX() + ", " + viewTopLeft.getY() + "   " + topLeft.getX() + ", " + topLeft.getY());
            return true;
        } else {
            return false;
        }
    }
}