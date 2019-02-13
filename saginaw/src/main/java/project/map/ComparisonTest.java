package project.map;

import project.search.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class ComparisonTest {

    public long startTime, endTime;


    public static void main(String[] args) throws InterruptedException {
        long startTime, endTime;
        String region = "england";
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

            ALTPreProcess altPreProcess = new ALTPreProcess(graph, region);

            float avg = 0, eps = 0;

            long[] srcs = new long[20];
            long[] dsts = new long[20];

            Random generator = new Random();
            Object[] keys = graph.getFwdGraph().keySet().toArray();

            for (int x = 0; x < 20; x++) {
                Long randomSrc = (Long) keys[generator.nextInt(keys.length)];
                Long randomDst = (Long) keys[generator.nextInt(keys.length)];
                srcs[x] = randomSrc;
                dsts[x] = randomDst;
                System.out.println(randomSrc + "    " + randomDst);
            }

            Searcher searcher = new Dijkstra(graph);

            for (int x = 0; x < 20; x++) {
                startTime = System.nanoTime();
                searcher.search(srcs[x], dsts[x]);
                endTime = System.nanoTime();
                avg += ((float) endTime - (float) startTime) / 1000000000;
                eps += searcher.getExplored() / (((float) endTime - (float) startTime) / 1000000000);
            }
            System.out.println("Dijkstra: " + avg / 10);
            System.out.println("EpS:    " + eps / 10);


            searcher = new BiDijkstra(graph);
            avg = 0; eps = 0;
            for (int x = 0; x < 20; x++) {
                startTime = System.nanoTime();
                searcher.search(srcs[x], dsts[x]);
                endTime = System.nanoTime();
                avg += ((float) endTime - (float) startTime) / 1000000000;
                eps += searcher.getExplored() / (((float) endTime - (float) startTime) / 1000000000);
            }
            System.out.println("BiDijkstra: " + avg / 10);
            System.out.println("EpS:    " + eps / 10);

            searcher = new ConcurrentBiDijkstra(graph);
            avg = 0; eps = 0;
            for (int x = 0; x < 20; x++) {
                startTime = System.nanoTime();
                searcher.search(srcs[x], dsts[x]);
                endTime = System.nanoTime();
                avg += ((float) endTime - (float) startTime) / 1000000000;
                eps += searcher.getExplored() / (((float) endTime - (float) startTime) / 1000000000);
            }
            System.out.println("ConcurrentBiDijkstra: " + avg / 10);
            System.out.println("EpS:    " + eps / 10);

            searcher = new AStar(graph, altPreProcess);
            avg = 0; eps = 0;
            for (int x = 0; x < 20; x++) {
                startTime = System.nanoTime();
                searcher.search(srcs[x], dsts[x]);
                endTime = System.nanoTime();
                avg += ((float) endTime - (float) startTime) / 1000000000;
                eps += searcher.getExplored() / (((float) endTime - (float) startTime) / 1000000000);
            }
            System.out.println("AStar: " + avg / 10);
            System.out.println("EpS:    " + eps / 10);

            searcher = new BiAStar(graph, altPreProcess);
            avg = 0; eps = 0;
            for (int x = 0; x < 20; x++) {
                startTime = System.nanoTime();
                searcher.search(srcs[x], dsts[x]);
                endTime = System.nanoTime();
                avg += ((float) endTime - (float) startTime) / 1000000000;
                eps += searcher.getExplored() / (((float) endTime - (float) startTime) / 1000000000);
            }
            System.out.println("BiAStar: " + avg / 10);
            System.out.println("EpS:    " + eps / 10);

            searcher = new ConcurrentBiAStar(graph, altPreProcess);
            avg = 0; eps = 0;
            for (int x = 0; x < 20; x++) {
                startTime = System.nanoTime();
                searcher.search(srcs[x], dsts[x]);
                endTime = System.nanoTime();
                avg += ((float) endTime - (float) startTime) / 1000000000;
                eps += searcher.getExplored() / (((float) endTime - (float) startTime) / 1000000000);
            }
            System.out.println("ConcurrentBiAStar: " + avg / 10);
            System.out.println("EpS:    " + eps / 10);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
