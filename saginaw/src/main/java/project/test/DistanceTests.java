package project.test;

import project.map.Graph;
import project.search.*;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static java.util.ResourceBundle.clearCache;

public class DistanceTests {

    public long startTime, endTime;

    private static int LOOPS = 250;

    public static void main(String[] args) throws InterruptedException {
        long startTime, endTime;
        String region = "london";
        String mapDir = System.getProperty("user.dir").concat("/res/");
        File f = new File(mapDir.concat(region).concat(".osm.pbf"));
        System.out.println(f.toString());

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
            System.out.println(graph.getDictionary().size());

            ALTPreProcess altPreProcess = new ALTPreProcess(graph, false);
            ALTPreProcess altPreProcessCore = new ALTPreProcess(graph, true);

            ArrayList<Integer> srcs = new ArrayList<>();
            ArrayList<Integer> dsts = new ArrayList<>();

            Random generator = new Random(47465346);

            ArrayList<HashMap<String, Integer>> distances = new ArrayList<>();

            for (int x = 0; x < LOOPS; x++) {
                Integer randomSrc = generator.nextInt(graph.getFwdGraph().size() - 1);
                Integer randomDst = generator.nextInt(graph.getFwdGraph().size() - 1);
                srcs.add(randomSrc);
                dsts.add(randomDst);
            }

//            srcs.remove(srcs.indexOf(2593475));
//            dsts.remove(dsts.indexOf(3158011));
//
//            srcs.remove(srcs.indexOf(423106));
//            dsts.remove(dsts.indexOf(3797640));
//
//            srcs.remove(srcs.indexOf(3123186));
//            dsts.remove(dsts.indexOf(1489875));
//
//            srcs.remove(srcs.indexOf(2401307));
//            dsts.remove(dsts.indexOf(4806072));
//
//            srcs.remove(srcs.indexOf(3472113));
//            dsts.remove(dsts.indexOf(3502252));
//
//            srcs.remove(srcs.indexOf(12653));
//            dsts.remove(dsts.indexOf(2643593));
//
//            srcs.remove(srcs.indexOf(3567705));
//            dsts.remove(dsts.indexOf(657223));
//
//            srcs.remove(srcs.indexOf(3319235));
//            dsts.remove(dsts.indexOf(2103326));
//
//            srcs.remove(srcs.indexOf(2363622));
//            dsts.remove(dsts.indexOf(3846742));
//
//            srcs.remove(srcs.indexOf(2144664));
//            dsts.remove(dsts.indexOf(1464613));
//
//            srcs.remove(srcs.indexOf(140411));
//            dsts.remove(dsts.indexOf(2672748));
//
//            srcs.remove(srcs.indexOf(1894615));
//            dsts.remove(dsts.indexOf(1689425));
//
//            srcs.remove(srcs.indexOf(1717843));
//            dsts.remove(dsts.indexOf(3912546));
//
//            srcs.remove(srcs.indexOf(3061550));
//            dsts.remove(dsts.indexOf(2033514));
//
//            srcs.remove(srcs.indexOf(393961));
//            dsts.remove(dsts.indexOf(251760));
//
//            srcs.remove(srcs.indexOf(3923695));
//            dsts.remove(dsts.indexOf(1770267));
//
//            srcs.remove(srcs.indexOf(272500));
//            dsts.remove(dsts.indexOf(96806));
//
//            srcs.remove(srcs.indexOf(3739555));
//            dsts.remove(dsts.indexOf(2093143));
//
//            srcs.remove(srcs.indexOf(4793826));
//            dsts.remove(dsts.indexOf(223962));
//
//            srcs.remove(srcs.indexOf(866056));
//            dsts.remove(dsts.indexOf(4058010));
//
//            srcs.remove(srcs.indexOf(3965931));
//            dsts.remove(dsts.indexOf(4770273));
//
//            srcs.remove(srcs.indexOf(4597873));
//            dsts.remove(dsts.indexOf(3374321));
//
//            srcs.remove(srcs.indexOf(31888));
//            dsts.remove(dsts.indexOf(2416648));
//
//            srcs.remove(srcs.indexOf(3146069));
//            dsts.remove(dsts.indexOf(2301095));
//
//            srcs.remove(srcs.indexOf(4408625));
//            dsts.remove(dsts.indexOf(2353287));
//
//            srcs.remove(srcs.indexOf(2291159));
//            dsts.remove(dsts.indexOf(1596116));

            LOOPS = srcs.size();

            removeUnreachables(new Dijkstra(    graph),                     srcs, dsts);
            ResourceBundle.clearCache();
            clearCache();

            ArrayList<Searcher> searchers = new ArrayList<>();
            searchers.add(new Dijkstra(               graph));
//            searchers.add(new BiDijkstra(             graph));
//            searchers.add(new ParallelBiDijkstra(   graph));
//            searchers.add(new ContractionDijkstra(    graph));
//            searchers.add(new ALT(                    graph, altPreProcess));
//            searchers.add(new BiALT(                  graph, altPreProcess));
//            searchers.add(new ParallelBiALT(        graph, altPreProcess));
            searchers.add(new ContractionALT(         graph, altPreProcessCore));

            runTest(searchers, srcs, dsts);


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

//    @Test
    private static void runTest(ArrayList<Searcher> searchers, ArrayList<Integer> srcs, ArrayList<Integer> dsts) {
        int numError = 0;
        double avgError = 0.0;
        for (int x = 0; x < LOOPS; x++) {
            double dijkstraDist = 0;
            double dijkstraWays = 0;
            for (Searcher searcher : searchers) {
                searcher.search(srcs.get(x), dsts.get(x));
                if(searcher.getName().equals("dijkstra")){
                    if(searcher.routeFound()){
                        dijkstraDist = round(searcher.getDist(), 2);
//                        dijkstraWays = searcher.getRouteAsWays().size();
                    } else {
                        continue;
                    }
                } else {
//                    Assert.assertEquals(round(searcher.getDist(), 2), dijkstraDist, 0.1);
                    if(round(round(searcher.getDist(), 2) - dijkstraDist, 2) != 0){
                        numError++;
                        avgError += round(round(searcher.getDist(), 2) - dijkstraDist, 2);
                    }
                    System.out.print(round(round(searcher.getDist(), 2) - dijkstraDist, 2) + "         ");

                }
                searcher.clear();
            }
            System.out.println();
        }
        System.out.println(avgError);
        System.out.println(numError);
        System.out.println(numError + " errors. (" + ((float) numError/LOOPS) + "%. Average deviance: " + (avgError / numError) + "m");
    }

    //rounding procedure source: https://stackoverflow.com/a/2808648/3032936
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

}