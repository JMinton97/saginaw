package project.map;

import sun.tools.java.Environment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class TestRun {


    public static void main(String[] args) {
        long startTime, endTime;
        String mapDir = System.getProperty("user.dir").concat("/res/");
        File f = new File(mapDir.concat("Stratford.osm.pbf"));
        MyGraph graph;
        Long src, dst;

        try {

            File fe = new File(System.getProperty("user.dir").concat("/draw/"));
            fe.mkdirs();
            fe.createNewFile();



//            MyMap map = new MyMap(f, 7.0);

             startTime = System.nanoTime();
            graph = new MyGraph(f);
             endTime = System.nanoTime();
            System.out.println("Making graph time: " + (((float) endTime - (float)startTime) / 1000000000));


//            src = Long.parseLong("1349207723"); //wales
//            dst = Long.parseLong("707151082");

            src = Long.parseLong("312711672"); //
            dst = Long.parseLong("2940631595");

//            src = Long.parseLong("27144564"); //london
//            dst = Long.parseLong("59838278");


//            src = Long.parseLong("1107401572"); //brum
//            dst = Long.parseLong("1635424953");

//            src = Long.parseLong("548050322"); //england
//            dst = Long.parseLong("513499");
//
//
//            src = Long.parseLong("548050322"); //exeter to spalding
//            dst = Long.parseLong("550385409");

//            src = Long.parseLong("548050322"); //brum
//            dst = Long.parseLong("280150290");
            ArrayList<Long> route = new ArrayList<>();
            Long next;

//            startTime = System.nanoTime();
//            Dijkstra dijk = new Dijkstra(graph, src, dst);
//            endTime = System.nanoTime();
//            System.out.println("Dijkstra full time: " + (((float) endTime - (float)startTime) / 1000000000));
//            System.out.println("Distance: " + dijk.getDistTo().get(dst) / 1000);
//
//            next = dst;
////            route.add(next);
//            while(next != null){
////                System.out.println(next);
//                route.add(next);
//                next = dijk.getEdgeTo().get(next);
//            }
//            System.out.println("Route is " + route.size());
            for(int x = 0; x < 10; x++){
                System.out.println();
                startTime = System.nanoTime();
                BiDijkstra biDijk = new BiDijkstra(graph, src, dst, graph.getDictionary());
                endTime = System.nanoTime();
                System.out.println("Bi-dijkstra full time: " + (((float) endTime - (float)startTime) / 1000000000));
                System.out.println("Distance: " + biDijk.getDist());
            }

//            route = new ArrayList<>();
//            next = biDijk.overlapNode;
//            while(next != null){
////                System.out.println(next);
//                route.add(next);
//                next = biDijk.uEdgeTo.get(next);
//            }
//            next = biDijk.overlapNode;
//            while(next != null){
////                System.out.println(next);
//                route.add(next);
//                next = biDijk.vEdgeTo.get(next);
//            }
//            System.out.println("Overlap route is  " + biDijk.getDist() / 1000);
//            System.out.println("Absolute route is " + biDijk.bestSeen / 1000);
//            System.out.println("Explored " + biDijk.explored);
//
//            System.out.println("Putting/add time: " + ((float) biDijk.totalRelaxTime / 1000000000));
//            System.out.println("Contains time:    " + ((float) biDijk.totalContainsTime / 1000000000));
//            System.out.println("Total relax time: " + ((float) biDijk.atotalRelaxTime / 1000000000));


//             startTime = System.nanoTime();
//            ArrayList<MyNode> newNodes = DouglasPeucker.simplify(graph.refsToNodes(route), 0.0001);
//             endTime = System.nanoTime();
//            System.out.println("Douglas time: " + (((float) endTime - (float)startTime) / 1000000000));
//
//            MyMap map = new MyMap(f);
//            map.drawMap(0);
//            map.drawRoute(graph.nodesToRefs(newNodes));
//            map.drawRoute(route);
//            map.saveMap();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
