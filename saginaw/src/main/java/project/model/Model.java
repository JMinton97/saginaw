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
	private MyMap2 map;
	private List<Rectangle> rects = new ArrayList<Rectangle>();
	private String region = "england";
	String mapDir = System.getProperty("user.dir").concat("/res/");
	private int x, y, level;
	private BigDecimal zoom, baseScale;
	private Point2D.Double centreCoord;
//	private Point2D.Double centreCoord = new Point2D.Double(-5.425, 53.425);
	private Point2D.Double origin;
	private double geomXD, geomYD;
	private int imageEdge = 1024;
	private ArrayList<double[]> markers;
	private ArrayList<Boolean> flags;

	private BiDijkstra bdijk;
	private BiAStar bstar;
	private MyGraph graph;
	private ArrayList<Long> routeWays;
	private ArrayList<Point2D.Double> routeNodes;
	private HashMap<double[], Long> closestNodes;

	public boolean hasRoute;
	public boolean pivoted;

	public Model() {
		x = 1;
		y = 1;
		baseScale = BigDecimal.valueOf(40000);
		zoom = BigDecimal.valueOf(32);
		level = 6;
		markers = new ArrayList<>();

		File f = new File(mapDir.concat(region).concat(".osm.pbf"));
		try {
			graph = new MyGraph(f, region);
			map = new MyMap2(f, region, imageEdge, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
//		bdijk = new BiDijkstra(graph, graph.getDictionary());
		bstar = new BiAStar(graph);

		routeNodes = new ArrayList<>();
		flags = new ArrayList<>();
		pivoted = false;

//		Long src = Long.parseLong("1349207723"); //wales
		Long src, dst;

//		src = Long.parseLong("510837046");
//		dst = Long.parseLong("3462287546");

//		src = Long.parseLong("370459811"); //wolverton to sheffield
//		dst = Long.parseLong("1014466202");

//		routeWays = bdijk.search(src, dst);
//		routeWays = bstar.search(src, dst);
//		System.out.println("Distance: " + bstar.getDist());
		centreCoord = map.getCentre();
		origin = map.getOrigin();

		closestNodes = new HashMap<>();

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

	public BigDecimal getZ() {
		return zoom;
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

	public ArrayList<Point2D.Double> getRoute() {
		return routeNodes;
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

	public void loadMap(File file)
			throws IOException
	{
//		this.map = null;
//		this.map = new MyGraph(file); //note - loading file twice
	//		int numImages = newImageFile.getNumImages();
	//		if (numImages == 0)
	//			throw new IOException("Image file contains no images");
	//		map.drawMap(0);
	//		BufferedImage bi = map.getMap();
	//		setImage(bi);
	}

	public void moveMap(double xD, double yD) {
		xD = (zoom.multiply(zoom)).multiply(BigDecimal.valueOf(xD)).doubleValue();
		yD = (zoom.multiply(zoom)).multiply(BigDecimal.valueOf(yD)).doubleValue();
		double baseScaleZoom = (baseScale.multiply(zoom)).doubleValue();
		geomXD = (xD / baseScaleZoom) + centreCoord.getX();
		geomYD = (yD / baseScaleZoom) + centreCoord.getY();
		if(geomXD < origin.getX()){
			geomXD = origin.getX();
		}
		if(geomYD > origin.getY()){
			geomYD = origin.getY();
		}
		centreCoord.setLocation(geomXD, geomYD);

//		x = x + (xD * z);
//		y = y + (yD * z);
//		if(x < 1){
//			x = 1;
//		}
//		if(y < 1){
//			y = 1;
//		}
	}

	public void findRoute(long src, long dst){
		routeWays = bstar.search(src, dst);
		System.out.println("Distance: " + bdijk.getDist());
		routeNodes = new ArrayList<>();
		for(Long w : routeWays){
//			System.out.println(w);
//			System.out.println(graph.wayToNodes(w));
			routeNodes.addAll(graph.refsToNodes(graph.wayToNodes(w)));
//			System.out.println();
		}
	}

	public void findRandomRoute(){
		Random generator = new Random();
		Object[] keys = graph.getFwdGraph().keySet().toArray();
		Object randomSrc = keys[generator.nextInt(keys.length)];
		Object randomDst = keys[generator.nextInt(keys.length)];
		System.out.println(randomSrc + "    " + randomDst);
		routeWays = bstar.search((Long) randomSrc, (Long) randomDst);
		System.out.println("Distance: " + bstar.getDist());
		routeNodes = new ArrayList<>();
		for(Long w : routeWays){
//			System.out.println(w);
//			System.out.println(graph.wayToNodes(w));
			routeNodes.addAll(graph.refsToNodes(graph.wayToNodes(w)));
//			System.out.println();
		}
	}

	public void zoomIn() {
		if(zoom.compareTo(BigDecimal.valueOf(0.20)) > 0){
			if(zoom.compareTo(BigDecimal.valueOf(1.99)) > 0){
				if(zoom.compareTo(BigDecimal.valueOf(3.99)) > 0){
					if(zoom.compareTo(BigDecimal.valueOf(7.99)) > 0){
						if(zoom.compareTo(BigDecimal.valueOf(15.99)) > 0){
							if(zoom.compareTo(BigDecimal.valueOf(31.99)) > 0){
								if(zoom.compareTo(BigDecimal.valueOf(63.99)) > 0){
									level = 6;
									zoom = zoom.subtract(BigDecimal.valueOf(12.8));
									return;
								} else {
									level = 6;
									zoom = zoom.subtract(BigDecimal.valueOf(6.4));
									return;
								}
							} else {
								level = 5;
								zoom = zoom.subtract(BigDecimal.valueOf(3.2));
								return;
							}
						} else {
							level = 4;
							zoom = zoom.subtract(BigDecimal.valueOf(1.6));
							return;
						}
					} else {
						level = 3;
						zoom = zoom.subtract(BigDecimal.valueOf(0.8));
						return;
					}
				} else {
					level = 2;
					zoom = zoom.subtract(BigDecimal.valueOf(0.4));
					return;
				}
			} else {
				level = 1;
				zoom = zoom.subtract(BigDecimal.valueOf(0.2));
				return;
			}
		}
	}

	public void zoomOut() {
		if(zoom.compareTo(BigDecimal.valueOf(0.19)) > 0){
			if(zoom.compareTo(BigDecimal.valueOf(1.99)) > 0){
				if(zoom.compareTo(BigDecimal.valueOf(3.99)) > 0){
					if(zoom.compareTo(BigDecimal.valueOf(7.99)) > 0){
						if(zoom.compareTo(BigDecimal.valueOf(15.99)) > 0){
							if(zoom.compareTo(BigDecimal.valueOf(31.99)) > 0){
								if(zoom.compareTo(BigDecimal.valueOf(63.99)) > 0){
									level = 6;
									zoom = zoom.add(BigDecimal.valueOf(12.8));
									return;
								} else {
									level = 6;
									zoom = zoom.add(BigDecimal.valueOf(6.4));
									return;
								}
							} else {
								level = 5;
								zoom = zoom.add(BigDecimal.valueOf(3.2));
								return;
							}
						} else {
							level = 4;
							zoom = zoom.add(BigDecimal.valueOf(1.6));
							return;
						}
					} else {
						level = 3;
						zoom = zoom.add(BigDecimal.valueOf(0.8));
						return;
					}
				} else {
					level = 2;
					zoom = zoom.add(BigDecimal.valueOf(0.4));
					return;
				}
			} else {
				level = 1;
				zoom = zoom.add(BigDecimal.valueOf(0.2));
				return;
			}
		}
	}

	public int getImageEdge(){
		return imageEdge;
	}

	public MyMap2 getMap(){
		return map;
	}

	public void addMarker(double[] location){
//		if(markers.size() > 1){
//			markers.clear();
//		}
//		System.out.println(location[0] + " " + location[1]);
		markers.add(location);
		flags.add(false);
		findRoute();
	}

	public void addPivot(double[] location){
		if(pivoted){
			markers.set(1, location);
		} else {
			markers.add(1, location);
		}
		pivoted = true;
		flags.set(0, false);
		flags.set(1, false);
        long startTime = System.nanoTime();
		findRoute();
        long endTime = System.nanoTime();
        System.out.println("findRoute time: " + (((float) endTime - (float)startTime) / 1000000000));
	}

	public void clearMarkers(){
		markers.clear();
		flags.clear();
		pivoted = false;
		hasRoute = false;
	}

	public ArrayList<double[]> getMarkers() {
		return markers;
	}

	public void findRoute() {
	    long startTime, endTime;
		if(markers.size() > 1){
			hasRoute = true;
			routeNodes = new ArrayList<>();
			for(int x = 0; x < markers.size() - 1; x++){
				if(!flags.get(x)){
//					System.out.println(markers.get(x)[0] + " " + markers.get(x)[1]);
					startTime = System.nanoTime();
					long src, dst;
					if(closestNodes.get(markers.get(x)) == null){
						 src = graph.findClosest(markers.get(x));
						 closestNodes.put(markers.get(x), src);
					} else {
						System.out.println("truuuuuuuuuuuuuth");
						 src = closestNodes.get(markers.get(x));
					}
					if(closestNodes.get(markers.get(x + 1)) == null){
						dst = graph.findClosest(markers.get(x + 1));
						closestNodes.put(markers.get(x + 1), dst);
					} else {
						dst = closestNodes.get(markers.get(x + 1));
					}
//					System.out.println(src);
//					System.out.println(dst);
					endTime = System.nanoTime();
                    System.out.println("	closest time: " + (((float) endTime - (float)startTime) / 1000000000));
                    startTime = System.nanoTime();
					routeWays = bstar.search(src, dst);
                    endTime = System.nanoTime();
                    System.out.println("	search time: " + (((float) endTime - (float)startTime) / 1000000000));
					System.out.println("ROUTE LENGTH" + routeWays.size());
					startTime = System.nanoTime();
					for(Long w : routeWays){
						routeNodes.addAll(graph.refsToNodes(graph.wayToNodes(w)));
					}
					flags.set(x, true);
					endTime = System.nanoTime();
					System.out.println("	adding time: " + (((float) endTime - (float)startTime) / 1000000000));
				}
			}
		}
	}
}