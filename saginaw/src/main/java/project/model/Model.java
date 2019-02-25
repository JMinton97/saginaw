package project.model;

import javafx.util.Pair;
import project.map.*;
import project.search.*;
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
	public double[] pivot;
	private double modZoom;

	private ContractionALT c1, c2;
	private MyGraph graph;
	private ArrayList<Long> routeWays;
	private ArrayList<Point2D.Double> routeNodes;
	private HashMap<double[], Long> closestNodes;
	private ALTPreProcess preProcess;

	public boolean hasRoute;
	public boolean pivoted;

	public Model() {
		x = 1;
		y = 1;
		baseScale = BigDecimal.valueOf(40000);
		zoom = BigDecimal.valueOf(2);
		modZoom = Math.pow(2, zoom.doubleValue());
		level = (int) Math.pow(2, Math.floor(zoom.doubleValue()));
		markers = new ArrayList<>();

		File f = new File(mapDir.concat(region).concat(".osm.pbf"));
		try {
			graph = new MyGraph(f, region);
			map = new MyMap2(f, region, imageEdge, false);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try{
			preProcess = new ALTPreProcess(graph, region);
		} catch(IOException e){
			System.out.println("IO error in ALTPreProcess.");
			System.exit(0);
		}


		c1 = new ContractionALT(graph, preProcess);
		c2 = new ContractionALT(graph, preProcess);

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

	public void findRoute(long src, long dst){
		routeWays = c1.search(src, dst);
		System.out.println("Distance: " + c1.getDist());
		routeNodes = new ArrayList<>();
		for(Long w : routeWays){
//			System.out.println(w);
//			System.out.println(graph.wayToNodes(w));
			routeNodes.addAll(graph.refsToNodes(graph.wayToRefs(w)));
//			System.out.println();
		}
	}

	public void findRandomRoute(){
		Random generator = new Random();
		Object[] keys = graph.getFwdGraph().keySet().toArray();
		Object randomSrc = keys[generator.nextInt(keys.length)];
		Object randomDst = keys[generator.nextInt(keys.length)];
		System.out.println(randomSrc + "    " + randomDst);
		routeWays = c1.search((Long) randomSrc, (Long) randomDst);
		System.out.println("Distance: " + c1.getDist());
		routeNodes = new ArrayList<>();
		for(Long w : routeWays){
//			System.out.println(w);
//			System.out.println(graph.wayToNodes(w));
			routeNodes.addAll(graph.refsToNodes(graph.wayToRefs(w)));
//			System.out.println();
		}
	}

	public void zoomIn() {
//        System.out.println(zoom);
		if(zoom.compareTo(BigDecimal.valueOf(0.1)) > 0){
			zoom = zoom.subtract(BigDecimal.valueOf(0.05));
		}else{
//			System.out.println(false);
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
		}else{
			System.out.println(false);
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
		if(zoom.multiply(BigDecimal.valueOf(1.1)).compareTo(BigDecimal.valueOf(9)) < 0){
			zoom = zoom.add(BigDecimal.valueOf(0.05));
		}

		modZoom = Math.pow(2, zoom.doubleValue());
		level = (int) Math.pow(2, Math.floor(zoom.doubleValue()));
	}

	public void zoomOut(double[] zoomPoint) {
		double xDif = zoomPoint[0] - centreCoord.getX();
		double yDif = zoomPoint[1] - centreCoord.getY();

		double oldZoom = modZoom;
		if(zoom.multiply(BigDecimal.valueOf(1.1)).compareTo(BigDecimal.valueOf(9)) < 0){
			zoom = zoom.add(BigDecimal.valueOf(0.05));
		}

		modZoom = Math.pow(2, zoom.doubleValue());
		level = (int) Math.pow(2, Math.floor(zoom.doubleValue()));

        if(level < 1){
            level = 1;
        }

		double scaleDif = oldZoom - modZoom;

//		System.out.println(centreCoord.x);
		centreCoord.x += (xDif * (scaleDif / oldZoom));
//		System.out.println(centreCoord.x);
		centreCoord.y += (yDif * (scaleDif / oldZoom));
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
		findRoute();
	}

	public void addPivotAlternate(double[] location){
		pivot = location;
		pivoted = true;
//		findRouteAlternate();
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


//	public void findRouteAlternate() {
//		if(markers.size() > 1){
//			if(pivoted){
//
//				ConcurrentBiALT searcherA = new ConcurrentBiALT(graph, preProcess, (ConcurrentBiALT) searcher, true);
//				ConcurrentBiALT searcherB = new ConcurrentBiALT(graph, preProcess, (ConcurrentBiALT) searcher, false);
//				long src, dst, pvt;
//				if(closestNodes.get(markers.get(0)) == null){
//					src = graph.findClosest(markers.get(0));
//				}else{
//					src = closestNodes.get(markers.get(0));
//				}
//				if(closestNodes.get(markers.get(1)) == null){
//					dst = graph.findClosest(markers.get(1));
//				}else{
//					dst = closestNodes.get(markers.get(1));
//				}
//				if(closestNodes.get(pivot) == null){
//					pvt = graph.findClosest(pivot);
//				}else{
//					pvt = closestNodes.get(pivot);
//				}
//				routeWays.clear();
//				routeWays.addAll(searcherA.continueSearch(src, pvt));
//				routeWays.addAll(searcherB.continueSearch(pvt, dst));
//				for (Long w : routeWays) {
//					System.out.println("add");
//					ArrayList<Point2D.Double> p = graph.wayToFirstNodes(w);
//					routeNodes.addAll(p);
//				}
//				System.out.println(routeNodes.size());
//				hasRoute = true;
//			}else{
//				System.out.println("search");
//				long src, dst;
//				if(closestNodes.get(markers.get(0)) == null){
//					src = graph.findClosest(markers.get(0));
//				}else{
//					src = closestNodes.get(markers.get(0));
//				}
//				if(closestNodes.get(markers.get(1)) == null){
//					dst = graph.findClosest(markers.get(1));
//				}else{
//					dst = closestNodes.get(markers.get(1));
//				}
//				routeWays = searcher.search(src, dst);
//				for (Long w : routeWays) {
//					System.out.println("add");
//					ArrayList<Point2D.Double> p = graph.wayToFirstNodes(w);
//					routeNodes.addAll(p);
//				}
//				System.out.println(routeNodes.size());
//				hasRoute = true;
//			}
//		}
//	}

	public void findRoute() {
		long startTime, endTime;
		ArrayList<Thread> routeThreads = new ArrayList<>();
		ArrayList<ContractionALT> routeFinders = new ArrayList<>();
		routeWays = new ArrayList<>();
		startTime = System.nanoTime();
		if (markers.size() > 1) {
			hasRoute = true;
			routeNodes = new ArrayList<>();
			for (int x = 0; x < markers.size() - 1; x++) {
				if (!flags.get(x)) {
//					System.out.println(markers.get(x)[0] + " " + markers.get(x)[1]);
//					startTime = System.nanoTime();
					long src, dst;
					if (closestNodes.containsKey(markers.get(x))) {
						src = graph.findClosest(markers.get(x));
						closestNodes.put(markers.get(x), src);
					} else {
						src = closestNodes.get(markers.get(x));
					}
					if (closestNodes.containsKey(markers.get(x + 1))) {
						dst = graph.findClosest(markers.get(x + 1));
						closestNodes.put(markers.get(x + 1), dst);
					} else {
						dst = closestNodes.get(markers.get(x + 1));
					}
//					endTime = System.nanoTime();
//					System.out.println("Find nodes: " + (((float) endTime - (float) startTime) / 1000000000));
//					startTime = System.nanoTime();
//					System.out.println(src);
//					System.out.println(dst);
//					System.out.println("Found src and dst.");
					endTime = System.nanoTime();
//					System.out.println("	closest time: " + (((float) endTime - (float) startTime) / 1000000000));
//					startTime = System.nanoTime();

					c1.clear();
					routeWays.addAll(c1.search(src, dst));
				}
			}

			endTime = System.nanoTime();
			System.out.println("Find route: " + (((float) endTime - (float) startTime) / 1000000000));
//			startTime = System.nanoTime();

			for (Long w : routeWays) {
//				System.out.println(w);
				ArrayList<Point2D.Double> p = graph.wayToNodes(w);
				routeNodes.addAll(p);
			}

			endTime = System.nanoTime();
			System.out.println("Get points: " + (((float) endTime - (float) startTime) / 1000000000));
			System.out.println();
		}

	}

	public void findRouteThreads() {
		long startTime, endTime;
		ArrayList<Thread> routeThreads = new ArrayList<>();
		ArrayList<ContractionALT> routeFinders = new ArrayList<>();
		routeWays = new ArrayList<>();
		startTime = System.nanoTime();
		if (markers.size() > 1) {
			hasRoute = true;
			routeNodes = new ArrayList<>();
			for (int x = 0; x < markers.size() - 1; x++) {
				if (!flags.get(x)) {
//					System.out.println(markers.get(x)[0] + " " + markers.get(x)[1]);
					startTime = System.nanoTime();
					long src, dst;
					if (closestNodes.get(markers.get(x)) == null) {
						src = graph.findClosest(markers.get(x));
						closestNodes.put(markers.get(x), src);
					} else {
						src = closestNodes.get(markers.get(x));
					}
					if (closestNodes.get(markers.get(x + 1)) == null) {
						dst = graph.findClosest(markers.get(x + 1));
						closestNodes.put(markers.get(x + 1), dst);
					} else {
						dst = closestNodes.get(markers.get(x + 1));
					}
//					System.out.println(src);
//					System.out.println(dst);
//					System.out.println("Found src and dst.");
					endTime = System.nanoTime();
//					System.out.println("	closest time: " + (((float) endTime - (float) startTime) / 1000000000));
//					startTime = System.nanoTime();

					routeFinders.add(new ContractionALT(graph, preProcess));
					Pair<Thread, Thread> threads = routeFinders.get(x).searchWithThreads(src, dst);
					threads.getValue().start();
					threads.getKey().start();
//					System.out.println("Started threads.");
					routeThreads.add(threads.getKey());
					routeThreads.add(threads.getValue());
				}
			}
//			System.out.println("Started all threads.");
			boolean done;
			do {
				done = true;
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				for(int x = 0; x < routeThreads.size(); x += 2){
					done = done && !routeThreads.get(x).isAlive();
					if(!routeThreads.get(x).isAlive()){
						routeThreads.get(x + 1).interrupt();
					}
					done = done && !routeThreads.get(x + 1).isAlive();
					if(!routeThreads.get(x + 1).isAlive()){
						routeThreads.get(x).interrupt();
					}
				}
			}while(!done);

			endTime = System.nanoTime();
			System.out.println("Find route: " + (((float) endTime - (float) startTime) / 1000000000));
			startTime = System.nanoTime();

			int j = 0;

			for (ContractionALT c : routeFinders) {
				ArrayList<Long> ways = c.getRouteAsWays();
				c = null;
				if(ways == null){
					System.out.println("Error at segment " + j);
				} else {
					routeWays.addAll(ways);
				}
				j++;
			}

			for (Long w : routeWays) {
//				System.out.println(w);
				ArrayList<Point2D.Double> p = graph.wayToNodes(w);
				routeNodes.addAll(p);
			}

			endTime = System.nanoTime();
			System.out.println("Get points: " + (((float) endTime - (float) startTime) / 1000000000));
			System.out.println();
		}

	}
}