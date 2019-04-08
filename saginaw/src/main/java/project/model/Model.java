package project.model;

import project.map.*;
import project.utils.ImageFile;
import project.utils.UnsupportedImageTypeException;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * User: Alan P. Sexton
 * Date: 20/06/13
 * Time: 23:36
 */

/**
 * The <code>Model</code> class manages the data of the application
 * <p>
 * Other than possibly a draw method, which draws a representation of the object
 * on a graphics context, and possibly a toString method, which generates a
 * <code>String</code> representation of the object, it should not know about
 * the user interface
 * </p>
 */

public class Model {
	private BufferedImage image = null;
	private SMap map;
	private List<Rectangle> rects = new ArrayList<Rectangle>();
	private String region = "britain";
	String mapDir = System.getProperty("user.dir").concat("/res/");
	private int x, y, level;
	private BigDecimal zoom, baseScale;
	private Point2D.Double centreCoord;
	private Point2D.Double origin;
	private double geomXD, geomYD;
	private int imageEdge = 1024;
	private double modZoom;
	private Route route;
	public boolean pivotMode;
	public int dragThresholdPx = 20;


	public Model() {

	}

	public void startUp(){
		x = 1;
		y = 1;
		baseScale = BigDecimal.valueOf(SMap.scale);
		zoom = BigDecimal.valueOf(2);
		modZoom = Math.pow(2, zoom.doubleValue());
		level = (int) Math.pow(2, Math.floor(zoom.doubleValue()));

		File f = new File(mapDir.concat(region).concat(".osm.pbf"));
		try {
			route = new Route(new Graph(f, region));
			map = new SMap(f, region, imageEdge, false);
		} catch (IOException e) {
			e.printStackTrace();
		}

		pivotMode = false;

		centreCoord = map.getCentre();
		origin = map.getOrigin();

	}

	public BufferedImage getImage() {
		return image;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public double getZ() {
		return modZoom;
	}

	public int getLevel(){
		return level;
	}

	public String getRegion() {
		return region;
	}

	public Point2D getCentre() {
		return centreCoord;
	}

	public Point2D getOrigin() {
		return origin;
	}

	public BigDecimal getScale() {
		return baseScale;
	}


	/**
	 * Get the list of rectangles currently set in the model.
	 * 
	 * @return the <code>List</code> object containing the rectangles set
	 */
	public List<Rectangle> getRects()
	{
		return rects;
	}

	/**
	 * Sets or replaces the current image in the <code>Model</code> and clears
	 * the list of rectangles.
	 *
	 * @param bi
	 *            the image to set in the <code>Model</code>
	 */
	public void setImage(BufferedImage bi)
	{
		image = bi;
		rects.clear();
	}

	/**
	 * Get the dimensions of the image loaded
	 * 
	 * @return the <code>Dimension</code> object containing the dimensions of
	 *         the image loaded, or <code>(0, 0)</code> if there is no image
	 *         loaded.
	 */
	public Dimension getDimensions()
	{
		if (image != null)
			return new Dimension(image.getWidth(), image.getHeight());
		else
			return new Dimension(0, 0);
	}

	/**
	 * Adds a new <code>Rectangle</code> to the <code>Model</code>
	 *
	 * @param rect
	 *            the <code>Rectangle</code> to add to the <code>Model</code>
	 */
	public void addRect(Rectangle rect)
	{
		rects.add(rect);
	}

	/**
	 * Tests if the model is active, i.e. whether it currently has an image
	 *
	 * @return <code>true</code> if the model has an image, false otherwise
	 */
	public boolean isActive()
	{
		return true;
	}

	/**
	 * Loads an image from a file.
	 * 
	 * Any pre-existing rectangles will be cleared.
	 * 
	 * @param file
	 *            The <code>File</code> object identifying the file containing
	 *            the image to load
	 * @throws IOException
	 *             if there is a problem reading the file or if the file
	 *             contains no images
	 * @throws UnsupportedImageTypeException
	 *             if the file does not contain an image of a type supported
	 */
	public void loadImage(File file)
			throws IOException, UnsupportedImageTypeException
	{
		ImageFile newImageFile = new ImageFile(file);
		int numImages = newImageFile.getNumImages();
		if (numImages == 0)
			throw new IOException("Image file contains no images");
		BufferedImage bi = newImageFile.getBufferedImage(0);
		setImage(bi);
	}

	public void moveMap(double xD, double yD) {
		xD = modZoom * modZoom * xD;
		yD = modZoom * modZoom * yD;
//		xD = (zoom.multiply(zoom)).multiply(BigDecimal.valueOf(xD)).doubleValue();
//		yD = (zoom.multiply(zoom)).multiply(BigDecimal.valueOf(yD)).doubleValue();
		double baseScaleZoom = baseScale.doubleValue() * modZoom;
		geomXD = (xD / baseScaleZoom) + centreCoord.getX();
		geomYD = (yD / baseScaleZoom) + centreCoord.getY();
		if(geomXD < origin.getX()){
			geomXD = origin.getX();
		}
		if(geomYD > origin.getY()){
			geomYD = origin.getY();
		}
		centreCoord.setLocation(geomXD, geomYD);
	}


	public void zoomIn() {
//        System.out.println(zoom);
		if(zoom.compareTo(BigDecimal.valueOf(0.1)) > 0){
			zoom = zoom.subtract(BigDecimal.valueOf(0.05));
		}

		modZoom = Math.pow(2, zoom.doubleValue());
		level = (int) Math.pow(2, Math.floor(zoom.doubleValue()));
	}

	public void zoomIn(double[] zoomPoint) {
//		System.out.println("Zoom in from " + zoom);
		double xDif = zoomPoint[0] - centreCoord.getX();
		double yDif = zoomPoint[1] - centreCoord.getY();

		double oldZoom = modZoom;

//        System.out.println(zoom);

		if(zoom.add(BigDecimal.valueOf(1)).compareTo(BigDecimal.valueOf(0.1)) > 0){
			zoom = zoom.subtract(BigDecimal.valueOf(0.05));
		}

		modZoom = Math.pow(2, zoom.doubleValue());
		level = (int) Math.pow(2, Math.floor(zoom.doubleValue()));
		if(level < 1){
		    level = 1;
        }

		double scaleDif = oldZoom - modZoom;
		centreCoord.x += (xDif * (scaleDif / oldZoom));
		centreCoord.y += (yDif * (scaleDif / oldZoom));
	}

	public void zoomOut() {
		if(zoom.multiply(BigDecimal.valueOf(1.1)).compareTo(BigDecimal.valueOf(11)) < 0){
			zoom = zoom.add(BigDecimal.valueOf(0.05));
		}

		modZoom = Math.pow(2, zoom.doubleValue());
		level = (int) Math.pow(2, Math.floor(zoom.doubleValue()));
	}

	public void zoomOut(double[] zoomPoint) {
		double xDif = zoomPoint[0] - centreCoord.getX();
		double yDif = zoomPoint[1] - centreCoord.getY();

		double oldZoom = modZoom;
		if(zoom.multiply(BigDecimal.valueOf(1.1)).compareTo(BigDecimal.valueOf(11)) < 0){
			zoom = zoom.add(BigDecimal.valueOf(0.05));
		}

		modZoom = Math.pow(2, zoom.doubleValue());
		level = (int) Math.pow(2, Math.floor(zoom.doubleValue()));

        if(level < 1){
            level = 1;
        }

		double scaleDif = oldZoom - modZoom;

		centreCoord.x += (xDif * (scaleDif / oldZoom));
		centreCoord.y += (yDif * (scaleDif / oldZoom));
	}

	public int getImageEdge(){
		return imageEdge;
	}

	public SMap getMap(){
		return map;
	}

	public Route getRoute(){
		return route;
	}

	public void loadFullRoute(){
		route.loadFullRoute();
	}

	public boolean clickedRoute(double[] clickPoint){

		double dragThreshold = ((1 / (baseScale.doubleValue() / Math.pow(2, zoom.doubleValue()))) * dragThresholdPx) * 100000;
		return route.adjustRoute(clickPoint, dragThreshold);
	}
}