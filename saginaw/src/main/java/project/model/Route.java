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
    private int segmentToPivot;
    private int waypointToMove;
    private Stack<double[]> addedPoints;
    private boolean pivoting;
    private boolean startPivot;
    private boolean startMoveWaypoint;
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
            segments.add(new Segment(waypoints.get(waypoints.size() - 2), waypoints.get(waypoints.size() - 1), graph));
        }
        addedPoints.push(point);
        validateSegments();
        calculateRoute();
    }

    public boolean adjustRoute(double[] pivotPoint, double dragThreshold){

        double minDist = Double.MAX_VALUE;
        int minSegment = 0;
        int segmentNum = 0;
        int minWayPoint = 0;
        double distFromLine, distFromPoint;

        for(double[] waypoint : waypoints){
            distFromPoint = MyGraph.haversineDistance(pivotPoint, waypoint);
            if(distFromPoint < minDist){
                minDist = distFromPoint;
                minWayPoint = waypoints.indexOf(waypoint);
            }
        }

        if(minDist < dragThreshold * 2){
            waypointToMove = minWayPoint;
            startMoveWaypoint = true;
            return true;
        } else if(hasRoute()){
            minDist = Double.MAX_VALUE;
            for(Segment segment : segments){
                if(segment.hasRoute()){
                    for(Point2D.Double point : segment.getPoints()){
                        distFromLine = MyGraph.haversineDistance(pivotPoint, new double[]{point.getX(), point.getY()});
                        if(distFromLine < minDist){
                            minDist = distFromLine;
                            minSegment = segmentNum;
                        }
                    }
                }
                segmentNum++;
            }
            if(minDist < dragThreshold){
                segmentToPivot = minSegment;
                startPivot = true;
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void makeAdjustment(double[] point){
        if(startMoveWaypoint){
            alterWayPoint(point);
        } else if (startPivot){
            alterPivot(point);
        }
    }


    private void alterPivot(double[] pivotPoint){
        if(pivoting){
            waypoints.set(segmentToPivot + 1, pivotPoint);
            segments.get(segmentToPivot).setResolved(false);
            segments.get(segmentToPivot + 1).setResolved(false);
            addedPoints.pop();
        }else{
            pivoting = true;
            waypoints.add(segmentToPivot + 1, pivotPoint);
            segments.add(segmentToPivot + 1, new Segment(new double[]{}, new double[]{}, graph));
        }
        addedPoints.push(pivotPoint);
        validateSegments();
        calculateRoute();
    }

    private void alterWayPoint(double[] alterPoint){
        waypoints.set(waypointToMove, alterPoint);
        validateSegments();
        calculateRoute();
    }

    public boolean hasRoute(){
        return(segments.stream().anyMatch(seg -> seg.hasRoute()));

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
        if(!segments.isEmpty()){
            segments.remove(undoIndex - 1);
        }
        validateSegments();
        calculateRoute();
    }

    public void loadFullRoute(){
        for(Segment segment : segments) {
            segment.getFullDetailRoute();
        }
        pivoting = false;
    }

    private void calculateRoute() {
        ArrayList<Thread> routeThreads = new ArrayList<>();
        if (waypoints.size() > 1) {
            for(Segment segment : segments){
                if (!segment.isResolved()) {
                    System.out.println("UNRESOLVED....");
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

                        if(searcher.routeFound()){
                            segment.setPoints(graph.refsToNodes(searcher.getRoute()));
                            segment.setDistance(searcher.getDist());
                            segment.setHasRoute(true);
                        }else{
                            segment.setHasRoute(false);
                        }

                        System.out.println(searcher.getExplored());

                        segment.setWayIds(searcher.getRouteAsWays());
                        segment.setResolved(true);
                        searcher.clear();
                        searcherStack.push(searcher);
                    };

                    Thread searchThread = new Thread(routeSegmentThread);
                    routeThreads.add(searchThread);
                    searchThread.start();
                }
            }

            boolean running = true;

            while(running){
				try{
					Thread.sleep(50);
				}catch(InterruptedException e){}
                running = false;
                for(Thread routeThread : routeThreads){
                    running = (running || routeThread.isAlive());
                }
            }
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
				searcherStack.clear();
                for(int x = 0; x < SEARCHER_COUNT; x++){
                    searcherStack.add(new ContractionDijkstra(graph));
                }
                break;

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

    public double getDistance(){
        double distance = 0;
        for(Segment s : segments){
            distance += s.getDistance();
        }
        return distance;
    }

    public void endAlteration(){
        pivoting = false;
        startMoveWaypoint = false;
        startPivot = false;
        waypointToMove = 0;
        segmentToPivot = 0;
    }
}
