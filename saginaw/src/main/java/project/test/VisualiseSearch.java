package project.test;

import project.map.MyGraph;
import project.search.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class VisualiseSearch {

    static int LOOPS = 10;

    public static void main(String[] args) {
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


            for (int x = 0; x < LOOPS; x++) {
                Integer randomSrc = generator.nextInt(graph.getFwdGraph().size() - 1);
                Integer randomDst = generator.nextInt(graph.getFwdGraph().size() - 1);
                srcs.add(randomSrc);
                dsts.add(randomDst);
            }

            DrawGraph draw = new DrawGraph(region);

            ArrayList<Searcher> searchers = new ArrayList<>();

            searchers.add(new Dijkstra(             graph));
            searchers.add(new BiDijkstra(           graph));
            searchers.add(new ContractionDijkstra(  graph));
            searchers.add(new ALT(                  graph, altPreProcess));
            searchers.add(new BiALT(                graph, altPreProcess));
            searchers.add(new ContractionALT(       graph, altPreProcessCore));

            Iterator srcIt = srcs.iterator();
            Iterator dstIt = dsts.iterator();
            int x = 0;
            while(srcIt.hasNext()){
                int src = (int) srcIt.next();
                int dst = (int) dstIt.next();
                int level = 0;
                for(Searcher searcher : searchers){
                    searcher.search(src, dst);
                    draw.drawSearch(searcher, graph, x, level, src, dst);
                    searcher.clear();
                    level++;
                }
                x++;

            }
        }catch(IOException e){
            System.out.println("IO Exception!");
        }
    }
}
