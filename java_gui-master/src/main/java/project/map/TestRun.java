package project.map;

import sun.tools.java.Environment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class TestRun {


    public static void main(String[] args) throws InterruptedException{
        long startTime, endTime;
        String region = "wales";
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


//            startTime = System.nanoTime();
//            MyMap2 map2 = new MyMap2(f, region, 1024, false);
//            map2.draw();
//            endTime = System.nanoTime();
//            System.out.println("Total map drawing time: " + (((float) endTime - (float)startTime) / 1000000000));
//
//            System.exit(0);


            startTime = System.nanoTime();
            graph = new MyGraph(f, region);
            endTime = System.nanoTime();
            System.out.println("Making graph time: " + (((float) endTime - (float)startTime) / 1000000000));

            src = Long.parseLong("1349207723"); //wales
            dst = Long.parseLong("707151082");

            src = Long.parseLong("154401978"); //wales middle
            dst = Long.parseLong("411081397");

//            src = Long.parseLong("312711672"); //
//            dst = Long.parseLong("2940631595");

//            src = Long.parseLong("27144564"); //london
//            dst = Long.parseLong("59838278");


            src = Long.parseLong("1107401572"); //brum
            dst = Long.parseLong("1635424953");

//            src = Long.parseLong("548050322"); //england
//            dst = Long.parseLong("513499");
//
//
//            src = Long.parseLong("548050322"); //exeter to spalding
//            dst = Long.parseLong("550385409");

//            src = Long.parseLong("548050322"); //brum
//            dst = Long.parseLong("280150290");

            src = Long.parseLong("370459811"); //wolverton to sheffield
            dst = Long.parseLong("1014466202");

//            src = Long.parseLong("1014654504"); //north to south
//            dst = Long.parseLong("1620423227");

            src = Long.parseLong("1802487895"); //france
            dst = Long.parseLong("1338882013");

            src1 = Long.parseLong("3734850968"); //france
            dst1 = Long.parseLong("774690360");

            src2 = Long.parseLong("266864590"); //france
            dst2 = Long.parseLong("427050694");

//
//            System.out.println("Distance: " + astar.getDistTo().get(dst));
//            System.out.println("Explored: " + astar.explored);
//
//            System.out.println();


            BiAStar biastar = new BiAStar(graph);
            startTime = System.nanoTime();
            biastar.search(src, dst);
            endTime = System.nanoTime();
            System.out.println("BiAStar 1 full time: " + (((float) endTime - (float)startTime) / 1000000000));
            System.out.println("Distance: " + biastar.getDist());
            System.out.println("Explored: " + biastar.explored);

            startTime = System.nanoTime();
            biastar.search(src1, dst1);
            endTime = System.nanoTime();
            System.out.println("BiAStar 2 full time: " + (((float) endTime - (float)startTime) / 1000000000));
            System.out.println("Distance: " + biastar.getDist());
            System.out.println("Explored: " + biastar.explored);

            startTime = System.nanoTime();
            biastar.search(src2, dst2);
            endTime = System.nanoTime();
            System.out.println("BiAStar 3 full time: " + (((float) endTime - (float)startTime) / 1000000000));
            System.out.println("Distance: " + biastar.getDist());
            System.out.println("Explored: " + biastar.explored);




            biastar.clear();
            biastar = null;
            System.out.println("----------------------");
//
//            AStar astar = new AStar(graph);
//            startTime = System.nanoTime();
//            astar.search(src, dst);
//            endTime = System.nanoTime();
//            System.out.println("AStar full time: " + (((float) endTime - (float)startTime) / 1000000000));
//            System.out.println("Distance: " + astar.getDistTo(dst));
//            System.out.println("Explored: " + astar.explored);
//            astar.clear();
//            astar = null;
//            System.out.println("----------------------");


            startTime = System.nanoTime();
            Dijkstra dijk = new Dijkstra(graph, src, dst);
            endTime = System.nanoTime();
//            System.out.println("Poll time: " + ((float) dijk.totalPollTime / 1000000000));
//            System.out.println("Add time: " + ((float) dijk.totalAddTime / 1000000000));
//            System.out.println("Put time: " + ((float) dijk.totalPutTime / 1000000000));
//            System.out.println("Relax time: " + ((float) dijk.totalRelaxTime / 1000000000));
            System.out.println("Dijkstra full time: " + (((float) endTime - (float)startTime) / 1000000000));
            System.out.println("Distance: " + dijk.getDistTo().get(dst));
            System.out.println("Explored: " + dijk.explored);
            ArrayList<Long> droute = dijk.getRoute();
            dijk.clear();
            dijk = null;
            System.out.println("----------------------");


//            System.out.println();
//            startTime = System.nanoTime();
////            BiDijkstra biDijk = new BiDijkstra(graph, src, dst, graph.getDictionary());
//            endTime = System.nanoTime();
//            System.out.println("Bi-dijkstra full time: " + (((float) endTime - (float)startTime) / 1000000000));
////            System.out.println("Poll time:    " + ((float) biDijk.totalPollTime / 1000000000));
//////                System.out.println("Priority queue time:    " + ((float) biDijk.totalContainsTime / 1000000000));
////            System.out.println("Total relax time: " + ((float) biDijk.totalRelaxTime / 1000000000));
////            System.out.println("Contains time: " + ((float) biDijk.totalContainsTime / 1000000000));
////            System.out.println("Relax-queue-add time: " + ((float) biDijk.atotalRelaxTime / 1000000000));
////            System.out.println("Relax-put time: " + ((float) biDijk.totalRelaxPutTime / 1000000000));
//            System.out.println("Distance: " + biDijk.getDist());
//            System.out.println("Explored: " + biDijk.explored);
//            ArrayList<Long> route = biDijk.getRoute();
//            biDijk.clear();
//            biDijk = null;
//            System.out.println("----------------------");


//            System.out.println("Overlap route is  " + biDijk.getDist() / 1000);
//            System.out.println("Absolute route is " + biDijk.bestSeen / 1000);
//            System.out.println("Explored " + biDijk.explored);



//            for(int j = 0; j < droute.size(); j++){
//                if(!route.get(j).equals(droute.get(j))){
//                    System.out.println("Error at " + j);
//                    System.out.println(route.get(j));                         //ERROR CHECKING
//                    System.out.println(droute.get(j));
//                }
//            }
//            System.out.println();

//            System.out.println(route == droute);
//            System.out.println(route.size());
//            System.out.println(droute.size());
//
//
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
