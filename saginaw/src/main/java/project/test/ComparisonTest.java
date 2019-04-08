package project.test;

import project.map.Graph;
import project.search.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.ResourceBundle;

import static java.util.ResourceBundle.clearCache;
import static project.map.Graph.haversineDistance;

public class ComparisonTest {

    public long startTime, endTime;

    private static int LOOPS = 250;

    public static void main(String[] args) throws InterruptedException {
        long startTime, endTime;
        String region = "britain";
        String mapDir = System.getProperty("user.dir").concat("/res/");
        File f = new File(mapDir.concat(region).concat(".osm.pbf"));

        File fregion = new File(System.getProperty("user.dir").concat("/files/" + region + "/"));
        fregion.mkdirs();
        try {
            fregion.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Graph graph;

        try {

            File fe = new File(System.getProperty("user.dir").concat("/draw/"));
            fe.mkdirs();
            fe.createNewFile();

            startTime = System.nanoTime();
            graph = new Graph(f, region);
            endTime = System.nanoTime();
            System.out.println("Making graph time: " + (((float) endTime - (float) startTime) / 1000000000));

            System.out.println(graph.getFwdGraph().size());
            System.out.println(graph.getFwdCore().size());
            System.out.println(graph.getDictionary().size());

            ALTPreProcess altPreProcess = new ALTPreProcess(graph, false);
            ALTPreProcess altPreProcessCore = new ALTPreProcess(graph, true);

            ArrayList<Integer> srcs = new ArrayList<>();
            ArrayList<Integer> dsts = new ArrayList<>();

            Random generator = new Random(47465346);

            double avgDistance = 0.0;

            for (int x = 0; x < LOOPS; x++) {
                Integer randomSrc = generator.nextInt(graph.getFwdGraph().size() - 1);
                Integer randomDst = generator.nextInt(graph.getFwdGraph().size() - 1);

//                if(haversineDistance(graph.getGraphNodeLocation(randomSrc), graph.getGraphNodeLocation(randomDst)) > 250000){
                    srcs.add(randomSrc);
                    dsts.add(randomDst);
                    avgDistance += haversineDistance(graph.getGraphNodeLocation(randomSrc), graph.getGraphNodeLocation(randomDst));

//                    System.out.println(haversineDistance(graph.getGraphNodeLocation(randomSrc), graph.getGraphNodeLocation(randomDst)));
//                }
//                else{
//                    x--;
////                    System.out.println(haversineDistance(graph.getGraphNodeLocation(randomSrc), graph.getGraphNodeLocation(randomDst)));
//                }
            }

            System.out.println("Average distance: " + avgDistance / LOOPS);
            LOOPS = srcs.size();

//            runInterTest(new ALT(graph, altPreProcess), new BetterALT(graph, altPreProcess), srcs, dsts);


//            removeUnreachables(new ContractionDijkstra(    graph),                     srcs, dsts);
            ResourceBundle.clearCache();
            clearCache();

//            runTest(new Dijkstra(               graph),                     srcs, dsts);
//            ResourceBundle.clearCache();
//            clearCache();
//            runTest(new BiDijkstra(             graph),                     srcs, dsts);
//            ResourceBundle.clearCache();
//            clearCache();
//            runTest(new ParallelBiDijkstra(   graph),                     srcs, dsts);
//            ResourceBundle.clearCache();
//            clearCache();
//            runTest(new ALT(                    graph, altPreProcess),      srcs, dsts);
//            runTest(new BetterALT(                    graph, altPreProcess),      srcs, dsts);
//            ResourceBundle.clearCache();
//            clearCache();
//            runTest(new BiALT(                  graph, altPreProcess),      srcs, dsts);
//            ResourceBundle.clearCache();
//            clearCache();
//            runTest(new ParallelBiALT(        graph, altPreProcess),      srcs, dsts);
//            ResourceBundle.clearCache();
//            clearCache();
//            runTest(new ContractionDijkstra(    graph),                     srcs, dsts);
//            ResourceBundle.clearCache();
//            clearCache();
            runTest(new ContractionALT(         graph, altPreProcessCore),  srcs, dsts);



        }catch(IOException e){
            System.out.println("IO Exception!");
        }

    }

    private static void removeUnreachables(Searcher searcher, ArrayList<Integer> srcs, ArrayList<Integer> dsts){
        Iterator srcIt = srcs.iterator();
        Iterator dstIt = dsts.iterator();
        while(srcIt.hasNext()){
            int src = (int) srcIt.next();
            int dst = (int) dstIt.next();
            searcher.search(src, dst);
            if(!searcher.routeFound()){
                srcIt.remove();
                dstIt.remove();
//                System.out.println(src);
//                System.out.println(dst);
                System.out.println("Removed.");
            }
            searcher.clear();
        }
        LOOPS = srcs.size();
        System.out.println(LOOPS);
    }

    private static void runTest(Searcher searcher, ArrayList<Integer> srcs, ArrayList<Integer> dsts){
        long avgTime = 0;
        long avgExplored = 0;
        for(int x = 0; x < LOOPS; x++){
//            System.out.println("begun");
            long startTime = System.nanoTime();
            searcher.search(srcs.get(x), dsts.get(x));
            long endTime = System.nanoTime();
//            System.out.println((endTime - startTime) / 1000000);
//            System.out.println((endTime - startTime) / 1000000);
            avgTime += (endTime - startTime);
            avgExplored += searcher.getExplored();
//            System.out.println((avgTime / (x+1) / 1000000) + "ms");
//            System.out.println(searcher.getDist());
            searcher.clear();
//            System.out.println("here we are");
        }
        avgTime = avgTime / LOOPS;
        avgExplored = avgExplored / LOOPS;
        System.out.println("\n" + searcher.getName());
        System.out.println("Average time: " + (avgTime / 1000000) + "ms");
        System.out.println("Average explored: " + (avgExplored));
    }

    private static void runInterTest(Searcher searcherA, Searcher searcherB, ArrayList<Integer> srcs, ArrayList<Integer> dsts){
        long avgTimeA = 0;
        long avgTimeB = 0;
        long avgExploredA = 0;
        long avgExploredB = 0;
        for(int x = 0; x < LOOPS; x++){
//            System.out.println("begun");
            long startTime = System.nanoTime();
            searcherA.search(srcs.get(x), dsts.get(x));
            long endTime = System.nanoTime();
//            System.out.println((endTime - startTime) / 1000000);
            avgTimeA += (endTime - startTime);
            avgExploredA += searcherA.getExplored();
            System.out.println(searcherA.getDist());
            searcherA.clear();

            startTime = System.nanoTime();
            searcherB.search(srcs.get(x), dsts.get(x));
            endTime = System.nanoTime();
//            System.out.println((endTime - startTime) / 1000000);
            avgTimeB += (endTime - startTime);
            avgExploredB += searcherB.getExplored();
            System.out.println(searcherB.getDist());
            searcherB.clear();
//            System.out.println("here we are");
        }
        avgTimeA = avgTimeA / LOOPS;
        avgTimeB = avgTimeB / LOOPS;
        avgExploredA = avgExploredA / LOOPS;
        avgExploredB = avgExploredB / LOOPS;

        System.out.println("\n" + searcherA.getName());
        System.out.println("Average time: " + (avgTimeA / 1000000) + "ms");
        System.out.println("Average explored: " + (avgExploredA));

        System.out.println("\n" + searcherB.getName());
        System.out.println("Average time: " + (avgTimeB / 1000000) + "ms");
        System.out.println("Average explored: " + (avgExploredB));
    }

    public static double haversineDistance(double[] nodeA, double[] nodeB){
        double rad = 6371000; //radius of earth in metres
        double aLatRadians = Math.toRadians(nodeA[1]); //0 = latitude, 1 = longitude
        double bLatRadians = Math.toRadians(nodeB[1]);
        double deltaLatRadians = Math.toRadians(nodeB[1] - nodeA[1]);
        double deltaLongRadians = Math.toRadians(nodeB[0] - nodeA[0]);

        double x = Math.sin(deltaLatRadians/2) * Math.sin(deltaLatRadians/2) +
                Math.cos(aLatRadians) * Math.cos(bLatRadians) *
                        Math.sin(deltaLongRadians/2) * Math.sin(deltaLongRadians/2);
        double y = 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1-x));
        return rad * y;

//        return Math.sqrt(Math.pow((nodeB.getLati() - nodeA.getLati()), 2) + Math.pow((nodeB.getLongi() - nodeA.getLongi()), 2));
    }
}