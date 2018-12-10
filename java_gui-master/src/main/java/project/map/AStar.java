package project.map;


import gnu.trove.map.hash.THashMap;
import javafx.util.Pair;
import org.mapdb.BTreeMap;
import gnu.trove.map.hash.TLongDoubleHashMap;
import java.util.*;

public class AStar {
    long pollTimeStart, pollTimeEnd, totalPollTime, addTimeStart, addTimeEnd, totalAddTime, relaxTimeStart, relaxTimeEnd, totalRelaxTime, putTimeStart, putTimeEnd, totalPutTime;
    THashMap<Long, Double> distTo;
    THashMap<Long, Long> edgeTo;
    PriorityQueue<DijkstraEntry> pq;
    long startNode, endNode;
    public int explored;
    MyGraph myGraph;
    ArrayList<Long> landmarks;
    THashMap<Long, double[]> distancesTo;
    THashMap<Long, double[]> distancesFrom;

//    public AStar(project.map.MyGraph graph, Long startNode){
//        distTo = new HashMap<>();
//        edgeTo = new HashMap<>();
//        pq = new PriorityQueue();
//
//        for(Long vert : graph.getGraph().keySet()){
//            distTo.put(vert, Double.MAX_VALUE);
//        }
//        distTo.put(startNode, 0.0);
//
//        Comparator<DijkstraEntry> comparator = new DistanceComparator();
//        pq = new PriorityQueue<DijkstraEntry>(comparator);
//
//        pq.add(new DijkstraEntry(startNode, 0.0));
//
//        while(!pq.isEmpty()){
//            long v = pq.poll().getNode();
//            for (double[] e : graph.adj(v)){
//                relax(v, e,);
//            }
//        }
//    }

    public AStar(MyGraph graph){
        this.myGraph = graph;
        landmarks = new ArrayList<>();
        Precomputation();
    }

    public ArrayList<Long> search(long start, long end){
        System.out.println("search");
        distTo = new THashMap<Long, Double>();
        edgeTo = new THashMap<Long, Long>();
        pq = new PriorityQueue();

//        HashMap<Long, Double> nodeWeights = MakeNodeWeights(graph.getGraph());

        for(Long vert : myGraph.getGraph().keySet()){
            distTo.put(vert, Double.MAX_VALUE);
        }
        distTo.put(start, 0.0);

        Comparator<DijkstraEntry> comparator = new DistanceComparator();
        pq = new PriorityQueue<>(comparator);

        pq.add(new DijkstraEntry(start, 0.0));

        System.out.println("and here");

        long startTime = System.nanoTime();
        while(!pq.isEmpty()){
            pollTimeStart = System.nanoTime();
            long v = pq.poll().getNode();
            pollTimeEnd = System.nanoTime();
            totalPollTime += (pollTimeEnd - pollTimeStart);
            for (double[] e : myGraph.adj(v)){
                relax(v, e, end);
                if(v == endNode){
                    break;
                }
            }
        }
        long endTime = System.nanoTime();
        System.out.println("done");
        System.out.println("Inner AStar time: " + (((float) endTime - (float)startTime) / 1000000000));
        return getRoute();
    }

    private void relax(Long v, double[] edge, long t){
        explored++;
        relaxTimeStart = System.nanoTime();
        long w = (long) edge[0];
        double weight = edge[1];
        double distToV = distTo.get(v);
        if (distTo.get(w) > (distToV + weight)){
            putTimeStart = System.nanoTime();
            distTo.put(w, distToV + weight);
            edgeTo.put(w, v); //should be 'nodeBefore'
            putTimeEnd = System.nanoTime();
            totalPutTime += (putTimeEnd - putTimeStart);
            addTimeStart = System.nanoTime();
//            System.out.println("distTo " + distTo.get(w) + " lower bound " + lowerBound(w, t));
            pq.add(new DijkstraEntry(w, distTo.get(w) + lowerBound(w, t))); //inefficient?
            addTimeEnd = System.nanoTime();
            totalAddTime += (addTimeEnd - addTimeStart);
        }
        relaxTimeEnd = System.nanoTime();
        totalRelaxTime += (relaxTimeEnd - relaxTimeStart);
    }

    public void Precomputation(){
        Map<Long, Set<double[]>> graph = myGraph.getGraph();
        BTreeMap<Long, double[]> dictionary = myGraph.getDictionary();
        distancesTo = new THashMap<>(); //need to compute
        distancesFrom = new THashMap<>();
        GenerateLandmarks();
        DijkstraLandmarks dj;
                                                            //fix this bit - don't store entire Dijkstra's computation.
        dj = new DijkstraLandmarks(this.myGraph, landmarks);
        distancesFrom = dj.getDistTo();
        dj.clear();
        dj = new DijkstraLandmarks(this.myGraph, landmarks);                             // <-- need reverse graph here
        distancesTo = dj.getDistTo();
        dj.clear();

//        HashMap<Long, double[]> nodeWeights = new HashMap<Long, double[]>();
//        for(Long node : graph.keySet()){
//            double[] distances = new double[landmarks.size()];
//            for(int x = 0; x < landmarks.size(); x++){
//                distances[x] =
//                System.out.print(distances[x] + " ");
//            }
//            nodeWeights.put(node, distances);
//            System.out.println();
//        }
    }

    public void GenerateLandmarks(){
        Map<Long, Set<double[]>> graph = myGraph.getGraph();
        int size = graph.size();
        Random random = new Random();
        List<Long> nodes = new ArrayList<>(graph.keySet());

        for(int x = 0; x < 10; x++){
            landmarks.add(nodes.get(random.nextInt(size)));
        }

        landmarks.clear();

        landmarks.add(Long.parseLong("27103812"));
        landmarks.add(Long.parseLong("299818750"));
        landmarks.add(Long.parseLong("312674444"));
        landmarks.add(Long.parseLong("273662"));
        landmarks.add(Long.parseLong("14644591"));
        landmarks.add(Long.parseLong("27210725"));
        landmarks.add(Long.parseLong("817576914"));
        landmarks.add(Long.parseLong("262840382"));
        landmarks.add(Long.parseLong("344881575"));
        landmarks.add(Long.parseLong("1795462073"));

    }

    public double lowerBound(long u, long v){
        double max = 0;
        double[] dTU, dFU, dTV, dFV;
        dTU = distancesTo.get(u);
        dFU = distancesFrom.get(u);
        dTV = distancesTo.get(v);
        dFV = distancesFrom.get(v);
//        System.out.println(dFV[0]);

        for(int l = 0; l < landmarks.size(); l++){
            max = Math.max(max, Math.max(dTU[l] - dTV[l], dFV[l] - dFU[l]));
        }
        return max;
    }

    public THashMap<Long, Double> getDistTo() {
        return distTo;
    }

    public class DistanceComparator implements Comparator<DijkstraEntry>{
        public int compare(DijkstraEntry x, DijkstraEntry y){
            if(x.getDistance() < y.getDistance()){
                return -1;
            }
            if(x.getDistance() > y.getDistance()){
                return 1;
            }
            else return 0;
        }
    }

    public ArrayList<Long> getRoute(){
        ArrayList<Long> route = new ArrayList<>();
        long node = endNode;
        route.add(node);
        while(node != startNode){
            node = edgeTo.get(node);
            route.add(node);
        }
        Collections.reverse(route);
        return route;
    }

    public void clear(){
        distTo.clear();
        edgeTo.clear();
        pq.clear();
        landmarks.clear();
        distancesTo.clear();
        distancesFrom.clear();
    }

}



