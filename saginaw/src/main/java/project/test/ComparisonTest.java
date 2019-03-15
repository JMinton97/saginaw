package project.test;

import project.map.MyGraph;
import project.search.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import static java.util.ResourceBundle.clearCache;

public class ComparisonTest {

    public long startTime, endTime;

    private static int LOOPS = 100;

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

        MyGraph graph;

        try {

            File fe = new File(System.getProperty("user.dir").concat("/draw/"));
            fe.mkdirs();
            fe.createNewFile();

            startTime = System.nanoTime();
            graph = new MyGraph(f, region);
            endTime = System.nanoTime();
            System.out.println("Making graph time: " + (((float) endTime - (float) startTime) / 1000000000));

            ALTPreProcess altPreProcess = new ALTPreProcess(graph, false);
            ALTPreProcess altPreProcessCore = new ALTPreProcess(graph, true);

            ArrayList<Integer> srcs = new ArrayList<>();
            ArrayList<Integer> dsts = new ArrayList<>();

            Random generator = new Random(47465346);
            Object[] keys = graph.getFwdGraph().keySet().toArray();

            for (int x = 0; x < LOOPS; x++) {
                Integer randomSrc = (Integer) keys[generator.nextInt(keys.length)];
                Integer randomDst = (Integer) keys[generator.nextInt(keys.length)];
                srcs.add(randomSrc);
                dsts.add(randomDst);
            }

            removeUnreachables(new Dijkstra(    graph),                     srcs, dsts);
            clearCache();

            runTest(new Dijkstra(               graph),                     srcs, dsts);
            clearCache();
            runTest(new BiDijkstra(             graph),                     srcs, dsts);
            clearCache();
            runTest(new ConcurrentBiDijkstra(   graph),                     srcs, dsts);
            clearCache();
            runTest(new ALT(                    graph, altPreProcess),      srcs, dsts);
            clearCache();
            runTest(new BiALT(                  graph, altPreProcess),      srcs, dsts);
            clearCache();
            runTest(new ConcurrentBiALT(        graph, altPreProcess),      srcs, dsts);
            clearCache();
            runTest(new ContractionDijkstra(    graph),                     srcs, dsts);
            clearCache();
            runTest(new ContractionALT(         graph, altPreProcessCore),  srcs, dsts);



        }catch(IOException e){
            System.out.println("IO Exception!");
        }

    }

    private static void removeUnreachables(Searcher searcher, ArrayList<Integer> srcs, ArrayList<Integer> dsts){
        Iterator srcIt = srcs.iterator();
        Iterator dstIt = dsts.iterator();
        while(srcIt.hasNext()){
            searcher.search((int) srcIt.next(), (int) dstIt.next());
            if(!searcher.routeFound()){
                srcIt.remove();
                dstIt.remove();
                System.out.println("Removed.");
            }
            searcher.clear();
        }
        LOOPS = srcs.size();
    }

    private static void runTest(Searcher searcher, ArrayList<Integer> srcs, ArrayList<Integer> dsts){
        long avgTime = 0;
        long avgExplored = 0;
        for(int x = 0; x < LOOPS; x++){
            long startTime = System.nanoTime();
            searcher.search(srcs.get(x), dsts.get(x));
            long endTime = System.nanoTime();
//            System.out.println((endTime - startTime) / 1000000);
            avgTime += (endTime - startTime);
            avgExplored += searcher.getExplored();
            searcher.clear();
        }
        avgTime = avgTime / LOOPS;
        avgExplored = avgExplored / LOOPS;
        System.out.println("\n" + searcher.getName());
        System.out.println("Average time: " + (avgTime / 1000000) + "ms");
        System.out.println("Average explored: " + (avgExplored));
    }
}