package project.model;

import project.map.MyGraph;
import project.search.*;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class Route{

    private ArrayList<double[]> waypoints;
    private ArrayList<Segment> segments;
    private int pivotSegment;
    private Stack<double[]> addedPoints;
    private boolean pivoting;
    private MyGraph graph;
    private HashMap<double[], Integer> closestNodes;
    private Stack<Searcher> searcherStack;
    private ALTPreProcess preProcess, corePreProcess;

    private final int SEARCHER_COUNT = 4;

    public Route(MyGraph graph) {
        this.graph = graph;
        waypoints = new ArrayList<>();
        segments = new ArrayList<>();
        addedPoints = new Stack<>();
        pivoting = false;
        closestNodes = new HashMap<>();
        searcherStack = new Stack<>();
        try{
            preProcess = new ALTPreProcess(graph, false);
            corePreProcess = new ALTPreProcess(graph, true);
        }catch(IOException e){
            System.out.println("Problem with ALTPreProcess loading.");
        }
        switchSearchers(SearchType.CONTRACTION_ALT);

    }

    public void addToEnd(double[] point){
        System.out.println("added");
        waypoints.add(point);
        if(waypoints.size() > 1){
            segments.add(new Segment(waypoints.get(waypoints.size() - 2), waypoints.get(waypoints.size() - 1)));
        }
        addedPoints.push(point);
        validateSegments();
        calculateRoute();
    }

    public boolean addPivot(double[] pivotPoint, double dragThreshold){
        if(hasRoute()){
            double minDist = Double.MAX_VALUE;
            int minSegment = 0;
            int segmentNum = 0;
            double distFromLine;

            for(Segment segment : segments){
                for(Point2D.Double point : segment.getPoints()){
                    distFromLine = MyGraph.haversineDistance(pivotPoint, new double[]{point.getX(), point.getY()});
                    if(distFromLine < minDist){
                        minDist = distFromLine;
                        minSegment = segmentNum;
                    }
                }
                segmentNum++;
            }
            if(minDist < dragThreshold){
                pivotSegment = minSegment;
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }

    }

    public void alterPivot(double[] pivotPoint){
        if(pivoting){
            waypoints.set(pivotSegment + 1, pivotPoint);
            segments.get(pivotSegment).setResolved(false);
            segments.get(pivotSegment + 1).setResolved(false);
            addedPoints.pop();
        }else{
            pivoting = true;
            waypoints.add(pivotSegment + 1, pivotPoint);
            segments.add(pivotSegment + 1, new Segment(new double[]{}, new double[]{}));
        }
        addedPoints.push(pivotPoint);
        validateSegments();
        calculateRoute();
    }

    public boolean hasRoute(){
        return segments.size() > 0;
    }

    public void endPivot(){
        pivoting = false;
    }

    public ArrayList<double[]> getWaypoints() {
        return waypoints;
    }

    public ArrayList<Segment> getSegments() {
        return segments;
    }

    public void validateSegments(){
        int x = 0;
        for(Segment s : segments){
            if(s.getStartNode() != waypoints.get(x)){
                s.setStartNode(waypoints.get(x));
                s.setResolved(false);
            }
            if(s.getEndNode() != waypoints.get(x + 1)){
                s.setEndNode(waypoints.get(x + 1));
                s.setResolved(false);
            }
            x++;
        }
    }

    public void undo(){
        double[] undoPoint = addedPoints.pop();
        int undoIndex = waypoints.indexOf(undoPoint);
        waypoints.remove(undoIndex);
        segments.remove(undoIndex);
        validateSegments();

    }

    public void loadFullRoute(){
        for(Segment segment : segments) {
            segment.getFullDetailRoute(graph);
        }
        pivoting = false;
    }

    public void calculateRoute() {
        ArrayList<Thread> routeThreads = new ArrayList<>();
        if (waypoints.size() > 1) {
            for(Segment segment : segments){
                if (!segment.isResolved()) {
                    Runnable routeSegmentThread = () -> {
                        int src, dst;
                        if (!closestNodes.containsKey(segment.getStartNode())) {
                            src = graph.findClosest(segment.getStartNode());
                            closestNodes.put(segment.getStartNode(), src);
                        } else {
                            src = closestNodes.get(segment.getStartNode());
                        }
                        if (!closestNodes.containsKey(segment.getEndNode())) {
                            dst = graph.findClosest(segment.getEndNode());
                            closestNodes.put(segment.getEndNode(), dst);
                        } else {
                            dst = closestNodes.get(segment.getEndNode());
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
                        segment.setPoints(graph.refsToNodes(searcher.getRoute()));

//                        routeDistance += searcher.getDist();

                        segment.setWayIds(searcher.getRouteAsWays());

                        segment.setResolved(true);

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

            System.out.println("Thread count: " + routeThreads.size());

            boolean running = true;

            while(running){
				try{
					Thread.sleep(50);
				}catch(InterruptedException e){}
                running = false;
                for(Thread routeThread : routeThreads){
					System.out.println("waiting");
                    running = (running || routeThread.isAlive());
                }
            }

            System.out.println("Finished.");
        }
    }

    public void freshSearch() {
        for(Segment s : segments){
            s.setResolved(false);
        }
        calculateRoute();
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
        freshSearch();
    }


    public void clear(){
        waypoints = new ArrayList<>();
        segments = new ArrayList<>();
        addedPoints = new Stack<>();
        pivoting = false;
    }

}
