package project.test;

import org.junit.Assert;
import org.junit.Test;
import project.map.Graph;
import project.search.Dijkstra;
import project.search.RemovalDijkstra;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class DijkstraTest {

    @Test
    public void dijkstraTest() {
        String region = "london";
        String mapDir = System.getProperty("user.dir").concat("/res/");
        File f = new File("/Users/joshua/Desktop/FYP/jdm638/saginaw/res/london.osm.pbf");
        Graph graph;

        File fregion = new File("/Users/joshua/Desktop/FYP/jdm638/saginaw/files/london/");
        fregion.mkdirs();
        try {
            fregion.createNewFile();
            graph = new Graph(f, region);

            ArrayList<Integer> srcs = new ArrayList<>();
            ArrayList<Integer> dsts = new ArrayList<>();

            Random generator = new Random(47465346);

            ArrayList<HashMap<String, Integer>> distances = new ArrayList<>();

            int LOOPS = 2000;

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


            Dijkstra dijkstra = new Dijkstra(graph);
            RemovalDijkstra removalDijkstra = new RemovalDijkstra(graph);

            for(int x = 0; x< LOOPS; x++){
                dijkstra.search(srcs.get(x), dsts.get(x));
                removalDijkstra.search(srcs.get(x), dsts.get(x));
                Assert.assertEquals(removalDijkstra.getDist(), dijkstra.getDist(), 0);
                dijkstra.clear();
                removalDijkstra.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
