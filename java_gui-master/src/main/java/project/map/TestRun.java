package project.map;

import sun.tools.java.Environment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class TestRun {
    public static void main(String[] args) {
        String mapDir = System.getProperty("user.dir").concat("/res/");
        File f = new File(mapDir.concat("England-latest.osm.pbf"));
        MyGraph graph;
        try {


            File fe = new File(System.getProperty("user.dir").concat("/draw/"));
            fe.mkdirs();
            fe.createNewFile();



//            MyMap map = new MyMap(f, 7.0);

            graph = new MyGraph(f);

//            Long src = Long.parseLong("1349207723"); //wales
//            Long dst = Long.parseLong("707151082");

//            Long src = Long.parseLong("27144564"); //london
//            Long dst = Long.parseLong("59838278");

//            Long src = Long.parseLong("1107401572"); //brum
//            Long dst = Long.parseLong("1635424953");

//            Long src = Long.parseLong("548050322"); //england
//            Long dst = Long.parseLong("14775001");

            Long src = Long.parseLong("548050322"); //brum
            Long dst = Long.parseLong("280150290");

            Dijkstra dijk = new Dijkstra(graph, src, dst);
            System.out.println("Distance: " + dijk.getDistTo().get(dst));

            BiDijkstra biDijk = new BiDijkstra(graph, src, dst);
            System.out.println("Distance: " + biDijk.getDist());


            ArrayList<Long> route = new ArrayList<>();
            Long next = dst;
//            route.add(next);
            while(next != null){
//                System.out.println(next);
                route.add(next);
                next = dijk.getEdgeTo().get(next);
            }
            System.out.println("Route is " + route.size());
            long startTime = System.nanoTime();
            ArrayList<MyNode> newNodes = DouglasPeucker.simplify(graph.refsToNodes(route), 0.0001);
            long endTime = System.nanoTime();
            System.out.println("Douglas time: " + (((float) endTime - (float)startTime) / 1000000000));

//            map = new MyMap(f);
//            map.drawMap(0);
//            map.drawRoute(graph.nodesToRefs(newNodes));
//            map.drawRoute(route);
//            map.saveMap();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
