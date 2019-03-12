package project.map;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import project.search.*;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public class ComparisonTest {

    public long startTime, endTime;

    private static int LOOPS = 100;


    public static void main(String[] args) throws InterruptedException {
        long startTime, endTime;
        String region = "london";
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


            float avg = 0, eps = 0;

            int[] srcs = new int[LOOPS];
            int[] dsts = new int[LOOPS];

            Random generator = new Random(47465346);
            Object[] keys = graph.getFwdGraph().keySet().toArray();

            for (int x = 0; x < LOOPS; x++) {
//                System.out.println(x);
                Integer randomSrc = (Integer) keys[generator.nextInt(keys.length)];
                Integer randomDst = (Integer) keys[generator.nextInt(keys.length)];
                srcs[x] = randomSrc;
                dsts[x] = randomDst;
//                System.out.println(randomSrc + "    " + randomDst);
            }

            System.out.println();

//            Searcher searcher = new Dijkstra(graph);
//            for (int x = 0; x < LOOPS; x++) {
////                System.out.println(x);
//                startTime = System.nanoTime();
//                searcher.search(srcs[x], dsts[x]);
//                endTime = System.nanoTime();
//                System.out.println(x + " : " + ((float) endTime - (float) startTime) / 1000000000);
//                avg += ((float) endTime - (float) startTime) / 1000000000;
//                eps += searcher.getExplored() / (((float) endTime - (float) startTime) / 1000000000);
////                if(x == 5){
////                    System.out.println(searcher.getDist());
////                }
//                searcher.clear();
//            }
//            System.out.println("Dijkstra: " + avg / LOOPS);
////            System.out.println("EpS:    " + eps / LOOPS);
//
//            System.out.println();
//
//
//            searcher = new BiDijkstra(graph);
//            avg = 0; eps = 0;
//            for (int x = 0; x < LOOPS; x++) {
////                System.out.println(x);
//                startTime = System.nanoTime();
//                searcher.search(srcs[x], dsts[x]);
//                endTime = System.nanoTime();
//                System.out.println(x + " : " + ((float) endTime - (float) startTime) / 1000000000);
//                avg += ((float) endTime - (float) startTime) / 1000000000;
//                eps += searcher.getExplored() / (((float) endTime - (float) startTime) / 1000000000);
////                if(x == 5){
////                    System.out.println(searcher.getDist());
////                }
//                searcher.clear();
//
//            }
//            System.out.println("BiDijkstra: " + avg / LOOPS);
////            System.out.println("EpS:    " + eps / LOOPS);
//
//            System.out.println();
//
//            searcher = new ConcurrentBiDijkstra(graph);
//            avg = 0; eps = 0;
//            for (int x = 0; x < LOOPS; x++) {
////                System.out.println(x);
//                startTime = System.nanoTime();
//                searcher.search(srcs[x], dsts[x]);
//                endTime = System.nanoTime();
//                System.out.println(x + " : " + ((float) endTime - (float) startTime) / 1000000000);
//                avg += ((float) endTime - (float) startTime) / 1000000000;
//                eps += searcher.getExplored() / (((float) endTime - (float) startTime) / 1000000000);
////                if(x == 5){
////                    System.out.println(searcher.getDist());
////                }
//                searcher.clear();
//
//            }
//            System.out.println("ConcurrentBiDijkstra: " + avg / LOOPS);
////            System.out.println("EpS:    " + eps / LOOPS);
//
//            System.out.println();
//
////            searcher = new ALT(graph, altPreProcess);
////            avg = 0; eps = 0;
////            for (int x = 0; x < LOOPS; x++) {
//////                System.out.println(x);
////                startTime = System.nanoTime();
////                searcher.search(srcs[x], dsts[x]);
////                endTime = System.nanoTime();
////                avg += ((float) endTime - (float) startTime) / 1000000000;
////                eps += searcher.getExplored() / (((float) endTime - (float) startTime) / 1000000000);
////                if(x == 5){
////                    System.out.println(searcher.getDist());
////                }
////                searcher.clear();
////
////            }
////            System.out.println("ALT: " + avg / LOOPS);
//////            System.out.println("EpS:    " + eps / LOOPS);
////
////            System.out.println();
//
//            searcher = new BiALT(graph, altPreProcess);
//            avg = 0; eps = 0;
//            for (int x = 0; x < LOOPS; x++) {
////                System.out.println(x);
//                startTime = System.nanoTime();
//                searcher.search(srcs[x], dsts[x]);
//                endTime = System.nanoTime();
//                System.out.println(x + " : " + ((float) endTime - (float) startTime) / 1000000000);
//                avg += ((float) endTime - (float) startTime) / 1000000000;
//                eps += searcher.getExplored() / (((float) endTime - (float) startTime) / 1000000000);
////                if(x == 5){
////                    System.out.println(searcher.getDist());
////                    System.out.println(searcher.getRoute().size());
////                }
//                searcher.clear();
//
//            }
//            System.out.println("BiALT: " + avg / LOOPS);
////            System.out.println("EpS:    " + eps / LOOPS);
//
//            System.out.println();
//
//            searcher = new ConcurrentBiALT(graph, altPreProcess);
//            avg = 0; eps = 0;
//            for (int x = 1; x < LOOPS; x++) {
////                System.out.println(x);
//                startTime = System.nanoTime();
//                searcher.search(srcs[x], dsts[x]);
//                endTime = System.nanoTime();
//                System.out.println(x + " : " + ((float) endTime - (float) startTime) / 1000000000);
//                avg += ((float) endTime - (float) startTime) / 1000000000;
//                eps += searcher.getExplored() / (((float) endTime - (float) startTime) / 1000000000);
////                if(x == 5){
////                    System.out.println(searcher.getDist());
////                    System.out.println(searcher.getRoute().size());
////                }
//                searcher.clear();
//            }
//
//            System.out.println("ConcurrentBiALT: " + avg / LOOPS);
//            System.out.println("EpS:    " + eps / LOOPS);
//
//            System.out.println();

            Searcher searcher = new ContractionALT(graph, altPreProcessCore);
            avg = 0; eps = 0;
            for (int x = 1; x < LOOPS; x++) {
                startTime = System.nanoTime();
                searcher.search(srcs[x], dsts[x]);
                endTime = System.nanoTime();
                System.out.println(startTime + " " + endTime);
                System.out.println(x + " : " + (endTime - startTime) / 1000000);
                avg += (endTime - startTime) / 1000000;
//                eps += searcher.getExplored() / ((endTime - startTime) / 1000000);
//                if(x == 5){
//                    System.out.println(searcher.getDist());
//                }
                searcher.clear();

            }
            System.out.println("Contraction ALT: " + avg / LOOPS);
            System.out.println("EpS:    " + eps / LOOPS);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}