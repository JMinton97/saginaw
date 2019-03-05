package project.map;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import project.search.*;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public class ComparisonTest {

    public long startTime, endTime;


    public static void main(String[] args) throws InterruptedException {
        long startTime, endTime;
        String region = "birmingham";
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
        Long src, dst, src1, dst1, src2, dst2;

        try {

            File fe = new File(System.getProperty("user.dir").concat("/draw/"));
            fe.mkdirs();
            fe.createNewFile();


            startTime = System.nanoTime();
            graph = new MyGraph(f, region);
            endTime = System.nanoTime();
            System.out.println("Making graph time: " + (((float) endTime - (float) startTime) / 1000000000));

            ALTPreProcess altPreProcess = new ALTPreProcess(graph);

            float avg = 0, eps = 0;

            int[] srcs = new int[20];
            int[] dsts = new int[20];

            Random generator = new Random(47465346);
            Object[] keys = graph.getFwdGraph().keySet().toArray();

            for (int x = 0; x < 20; x++) {
//                System.out.println(x);
                Integer randomSrc = (Integer) keys[generator.nextInt(keys.length)];
                Integer randomDst = (Integer) keys[generator.nextInt(keys.length)];
                srcs[x] = randomSrc;
                dsts[x] = randomDst;
//                System.out.println(randomSrc + "    " + randomDst);
            }

            System.out.println();

            Searcher searcher = new Dijkstra(graph);
            for (int x = 0; x < 20; x++) {
//                System.out.println(x);
                startTime = System.nanoTime();
                searcher.search(srcs[x], dsts[x]);
                endTime = System.nanoTime();
                avg += ((float) endTime - (float) startTime) / 1000000000;
                eps += searcher.getExplored() / (((float) endTime - (float) startTime) / 1000000000);
                searcher.clear();
            }
            System.out.println("Dijkstra: " + avg / 10);
//            System.out.println("EpS:    " + eps / 10);

            System.out.println();


            searcher = new BiDijkstra(graph);
            avg = 0; eps = 0;
            for (int x = 0; x < 20; x++) {
//                System.out.println(x);
                startTime = System.nanoTime();
                searcher.search(srcs[x], dsts[x]);
                endTime = System.nanoTime();
                avg += ((float) endTime - (float) startTime) / 1000000000;
                eps += searcher.getExplored() / (((float) endTime - (float) startTime) / 1000000000);
                if(x == 10){
                    System.out.println(searcher.getDist());
                }
                searcher.clear();

            }
            System.out.println("BiDijkstra: " + avg / 10);
//            System.out.println("EpS:    " + eps / 10);

            System.out.println();

            searcher = new ConcurrentBiDijkstra(graph);
            avg = 0; eps = 0;
            for (int x = 0; x < 20; x++) {
//                System.out.println(x);
                startTime = System.nanoTime();
                searcher.search(srcs[x], dsts[x]);
                endTime = System.nanoTime();
                avg += ((float) endTime - (float) startTime) / 1000000000;
                eps += searcher.getExplored() / (((float) endTime - (float) startTime) / 1000000000);
                if(x == 10){
                    System.out.println(searcher.getDist());
                }
                searcher.clear();

            }
            System.out.println("ConcurrentBiDijkstra: " + avg / 10);
//            System.out.println("EpS:    " + eps / 10);

            System.out.println();

            searcher = new ALT(graph, altPreProcess);
            avg = 0; eps = 0;
            for (int x = 0; x < 20; x++) {
//                System.out.println(x);
                startTime = System.nanoTime();
                searcher.search(srcs[x], dsts[x]);
                endTime = System.nanoTime();
                avg += ((float) endTime - (float) startTime) / 1000000000;
                eps += searcher.getExplored() / (((float) endTime - (float) startTime) / 1000000000);
                if(x == 10){
                    System.out.println(searcher.getDist());
                }
                searcher.clear();

            }
            System.out.println("ALT: " + avg / 10);
//            System.out.println("EpS:    " + eps / 10);

            System.out.println();

            searcher = new BiALT(graph, altPreProcess);
            avg = 0; eps = 0;
            for (int x = 0; x < 20; x++) {
//                System.out.println(x);
                startTime = System.nanoTime();
                searcher.search(srcs[x], dsts[x]);
                endTime = System.nanoTime();
                avg += ((float) endTime - (float) startTime) / 1000000000;
                eps += searcher.getExplored() / (((float) endTime - (float) startTime) / 1000000000);
                if(x == 10){
                    System.out.println(searcher.getDist());
                }
                searcher.clear();

            }
            System.out.println("BiALT: " + avg / 10);
//            System.out.println("EpS:    " + eps / 10);

            System.out.println();

            searcher = new ConcurrentBiALT(graph, altPreProcess);
            avg = 0; eps = 0;
            for (int x = 1; x < 20; x++) {
//                System.out.println(x);
                startTime = System.nanoTime();
                searcher.search(srcs[x], dsts[x]);
                endTime = System.nanoTime();
//                System.out.println(x + " : " + ((float) endTime - (float) startTime) / 1000000000);
                avg += ((float) endTime - (float) startTime) / 1000000000;
                eps += searcher.getExplored() / (((float) endTime - (float) startTime) / 1000000000);
                if(x == 10){
                    System.out.println(searcher.getDist());
                }
                searcher.clear();

            }
            System.out.println("ConcurrentBiALT: " + avg / 10);
            System.out.println("EpS:    " + eps / 10);

            System.out.println();

            searcher = new ContractionALT(graph, altPreProcess);
            avg = 0; eps = 0;
            for (int x = 1; x < 20; x++) {
                startTime = System.nanoTime();
                searcher.search(srcs[x], dsts[x]);
                endTime = System.nanoTime();
                System.out.println(x + " : " + ((float) endTime - (float) startTime) / 1000000000);
                avg += ((float) endTime - (float) startTime) / 1000000000;
                eps += searcher.getExplored() / (((float) endTime - (float) startTime) / 1000000000);
                if(x == 10){
                    System.out.println(searcher.getDist());
                }
                searcher.clear();

            }
            System.out.println("Contraction ALT: " + avg / 10);
            System.out.println("EpS:    " + eps / 10);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
