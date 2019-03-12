package project.model;

import project.kdtree.Tree;
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
	private String region = "london";
	String mapDir = System.getProperty("user.dir").concat("/res/");
	private int x, y, level;
	private BigDecimal zoom, baseScale;
	private Point2D.Double centreCoord;
//	private Point2D.Double centreCoord = new Point2D.Double(-5.425, 53.425);
	private Point2D.Double origin;
	private double geomXD, geomYD;
	private int imageEdge = 1024;
	private ArrayList<double[]> markers;
	private ArrayList<Boolean> segmentFlags;
	private int pivotOnSegment;
	private double modZoom;

	private ContractionALT c1, c2;
	private MyGraph graph;
	private ArrayList<ArrayList<Long>> segments;
	private ArrayList<ArrayList<Point2D.Double>> routeNodes;
	private ArrayList<Double> segmentDistances;
	private HashMap<double[], Integer> closestNodes;
	private ALTPreProcess preProcess;
	private ALTPreProcess corePreProcess;
	private final int SEARCHER_COUNT = 4;

	private Stack<double[]> markerStack;

	public boolean hasRoute;
	public boolean pivotMode;

	private double routeDistance;

	private ArrayList<Searcher> searcherList;
	private Stack<Searcher> searcherStack;

	private Tree routeTree;

	public Model() {

	}

	public void startUp(){
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

//		try{
//			preProcess = new ALTPreProcess(graph, false);
//		} catch(IOException e){
//			System.out.println("IO error in ALTPreProcess.");
//			System.exit(0);
//		}

		try{
			corePreProcess = new ALTPreProcess(graph, true);
		} catch(IOException e){
			System.out.println("IO error in ALTPreProcess.");
			System.exit(0);
		}

		routeNodes = new ArrayList<>();
		segments = new ArrayList<>();
		segmentDistances = new ArrayList<>();
		segmentFlags = new ArrayList<>();
		pivotMode = false;
		markerStack = new Stack<>();

		searcherStack = new Stack<>();
		switchSearchers(SearchType.CONTRACTION_ALT);

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

		routeDistance = 0;

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

	public ArrayList<ArrayList<Point2D.Double>> getRoute() {
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

	public void switchSearchers(SearchType s){
		switch(s){
			case DIJKSTRA:
				searcherStack.clear();
				for(int x = 0; x < SEARCHER_COUNT; x++){
					searcherStack.add(new Dijkstra(graph));
				}
				System.out.println("switched");
				break;

			case BIDIJKSTRA:
				searcherStack.clear();
				for(int x = 0; x < SEARCHER_COUNT; x++){
					searcherStack.add(new BiDijkstra(graph));
				}
				break;

			case CONCURRENT_BIDIJKSTRA:
				searcherStack.clear();
				for(int x = 0; x < SEARCHER_COUNT; x++){
					searcherStack.add(new ConcurrentBiDijkstra(graph));
				}
				break;

			case ALT:
				searcherStack.clear();
				for(int x = 0; x < SEARCHER_COUNT; x++){
					searcherStack.add(new ALT(graph, preProcess));
				}
				break;

			case BIALT:
				searcherStack.clear();
				for(int x = 0; x < SEARCHER_COUNT; x++){
					searcherStack.add(new BiALT(graph, preProcess));
				}
				break;

			case CONCURRENT_BIALT:
				searcherStack.clear();
				for(int x = 0; x < SEARCHER_COUNT; x++){
					searcherStack.add(new ConcurrentBiALT(graph, preProcess));
				}
				break;

			case CONTRACTION_DIJKSTRA:
//				searcherList.add(new Con)

			case CONTRACTION_ALT:
				searcherStack.clear();
				for(int x = 0; x < SEARCHER_COUNT; x++){
					searcherStack.add(new ContractionALT(graph, corePreProcess));
				}
				break;
		}
		if(hasRoute){
			freshSearch();
		}

	}

//	public void findRandomRoute(){
//		Random generator = new Random();
//		Object[] keys = graph.getFwdGraph().keySet().toArray();
//		Object randomSrc = keys[generator.nextInt(keys.length)];
//		Object randomDst = keys[generator.nextInt(keys.length)];
//		System.out.println(randomSrc + "    " + randomDst);
//		c1.search((int) randomSrc, (int) randomDst);
//		System.out.println("Distance: " + c1.getDist());
//		routeNodes = new ArrayList<>();
//		for(Long w : routeWays){
////			System.out.println(w);
////			System.out.println(graph.wayToNodes(w));
//			routeNodes.addAll(graph.refsToNodes(graph.wayToRefs(w)));
////			System.out.println();
//		}
//	}

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
		if(zoom.multiply(BigDecimal.valueOf(1.1)).compareTo(BigDecimal.valueOf(10)) < 0){
			zoom = zoom.add(BigDecimal.valueOf(0.05));
		}

		modZoom = Math.pow(2, zoom.doubleValue());
		level = (int) Math.pow(2, Math.floor(zoom.doubleValue()));
	}

	public void zoomOut(double[] zoomPoint) {
		double xDif = zoomPoint[0] - centreCoord.getX();
		double yDif = zoomPoint[1] - centreCoord.getY();

		double oldZoom = modZoom;
		if(zoom.multiply(BigDecimal.valueOf(1.1)).compareTo(BigDecimal.valueOf(10)) < 0){
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
		markerStack.add(location);
		routeNodes.add(new ArrayList<>());
		segments.add(new ArrayList<>());
		if(markers.size() > 1){
			segmentFlags.add(false);
		}
		betterFindRoutes();
	}

	public void addPivot(double[] location){
		if(pivotMode){
			markers.set(pivotOnSegment + 1, location);
			segmentFlags.set(pivotOnSegment, false);
			segmentFlags.set(pivotOnSegment + 1, false);
		} else {
			markers.add(pivotOnSegment + 1, location);
			segments.add(pivotOnSegment + 1, new ArrayList<>());
			segmentFlags.add(pivotOnSegment, false);
			segmentFlags.set(pivotOnSegment + 1, false);
			routeNodes.add(pivotOnSegment, new ArrayList<>());
		}
		markerStack.pop();
		markerStack.add(location);
		pivotMode = true;
		betterFindRoutes();
	}

	public void clearMarkers(){
		routeTree = null;
		routeDistance = 0;
		markers = new ArrayList<>();
		segmentDistances = new ArrayList<>();
		routeNodes= new ArrayList<>();
		segmentFlags = new ArrayList<>();
		pivotMode = false;
		hasRoute = false;
		markerStack = new Stack<>();
		segments = new ArrayList<>();
	}

	public void undoLastMarker(){

		int index = markers.indexOf(markerStack.peek());
		markers.remove(markerStack.pop());
		segmentFlags.set(index - 1, false);
		segmentFlags.remove(index);
		segments.remove(index);
		routeNodes.remove(index);
		if(markers.size() < 2){
			double[] firstMarker = markerStack.pop();
			clearMarkers();
			addMarker(firstMarker);
		}
		betterFindRoutes();

	}

	public ArrayList<double[]> getMarkers() {
		return markers;
	}

	public double getRouteDistance() {
		double distance = 0.0;
		for(double d : segmentDistances){
			distance += d;
		}
		return distance;
	}

	public void betterFindRoutes() {
		System.out.println();
		ArrayList<Thread> routeThreads = new ArrayList<>();
		if (markers.size() > 1) {
			hasRoute = true;
			for (int x = 0; x < markers.size() - 1; x++) {
//                System.out.println(x);
				final int z = x;
				if (!segmentFlags.get(x)) {
					Runnable routeSegmentThread = () -> {
						long startTime = System.nanoTime();
						int src, dst;
						if (!closestNodes.containsKey(markers.get(z))) {
							src = graph.findClosest(markers.get(z));
							closestNodes.put(markers.get(z), src);
						} else {
							src = closestNodes.get(markers.get(z));
						}
						if (!closestNodes.containsKey(markers.get(z + 1))) {
							dst = graph.findClosest(markers.get(z + 1));
							closestNodes.put(markers.get(z + 1), dst);
						} else {
							dst = closestNodes.get(markers.get(z + 1));
						}
						while(searcherStack.isEmpty()){
							try {
								Thread.sleep(10);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						Searcher searcher = searcherStack.pop();
						searcher.search(src, dst);
						if(routeNodes.size() > z){
							routeNodes.set(z, graph.refsToNodes(searcher.getRoute()));
//							segmentDistances.set(z, searcher.getDist());
                        }

						routeDistance += searcher.getDist();

						ArrayList<Long> segmentWays = searcher.getRouteAsWays();

						if(segments.size() > z){
							segments.set(z, segmentWays);
							System.out.println("setting segment " + z);
						} else {
							segments.add(segmentWays);
							System.out.println("adding segment " + z);
						}

						segmentFlags.set(z, true);
						searcher.clear();
						searcherStack.push(searcher);
					};

					Thread searchThread = new Thread(routeSegmentThread);
					routeThreads.add(searchThread);
					searchThread.start();
				} else {
					System.out.println("UNTOUCHED");
				}
			}

			for(int x = 0; x < routeNodes.size(); x++){
				if(x >= markers.size() - 1){
					routeNodes.remove(x);
					if(segmentDistances.size() > x){
						segmentDistances.remove(x);
					}
					x--;
				}
			}


			System.out.println("Thread count: " + routeThreads.size());

			boolean running = true;

			while(running){
//				try{
//					Thread.sleep(50);
//				}catch(InterruptedException e){}
				running = false;
				for(Thread routeThread : routeThreads){
//					System.out.println("waiting");
					running = (running || routeThread.isAlive());
				}
			}

            System.out.println("Finished.");
		}
		System.out.println(segmentFlags);
		System.out.println(routeNodes.size());
	}

	public void freshSearch() {
		for(int x = 0; x < segmentFlags.size(); x++){
			segmentFlags.set(x, false);
		}
		betterFindRoutes();
	}

	public void loadFullRoute(){
		int z = 0;
		for(ArrayList<Long> segment : segments) {
			if (routeNodes.size() > z) {
				routeNodes.set(z, graph.wayListToNodes(segment));
			} else {
				routeNodes.add(graph.wayListToNodes(segment));
			}
			z++;
		}
		pivotMode = false;
	}

	public boolean clickedRoute(double[] clickPoint, double dragThreshold){
		if(hasRoute){
			System.out.println(clickPoint[0] + "," + clickPoint[1]);

			double minDist = Double.MAX_VALUE;
			int minSegment = 0;
			int segmentNum = 0;
			double distFromLine;

			for(ArrayList<Point2D.Double> segment : routeNodes){
				for(Point2D.Double point : segment){
					distFromLine = MyGraph.haversineDistance(clickPoint, new double[]{point.getX(), point.getY()});
					if(distFromLine < minDist){
						minDist = distFromLine;
						minSegment = segmentNum;
					}
				}
				segmentNum++;
			}
			if(minDist < dragThreshold){
				pivotOnSegment = minSegment;
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}

	}
}