package project.map;

import crosby.binary.file.BlockInputStream;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class TestRun {
    public static void main(String[] args) {
        File f = new File("wales.osm.pbf");
        MyGraph graph;
        MyMap map;
        try {
            graph = new MyGraph(f);

            Long src = Long.parseLong("1349207723");
            Long dst = Long.parseLong("707151082");
            long startTime = System.nanoTime();
            Dijkstra dijk = new Dijkstra(graph, src);
            long endTime = System.nanoTime();
            System.out.println("Dijkstra time in ds: " + ((endTime - startTime) / 100000000));
            System.out.println("Distance: " + dijk.getDistTo().get(dst));
            ArrayList<Long> route = new ArrayList<>();
            Long next = dst;
//            route.add(next);
            while(next != null){
//                System.out.println(next);
                route.add(next);
                next = dijk.getEdgeTo().get(next);
            }
            System.out.println(graph.refsToNodes(route).get(0).getNodeId());
            System.out.println(graph.refsToNodes(route).get(graph.refsToNodes(route).size() - 1).getNodeId());
            ArrayList<MyNode> newNodes = DouglasPeucker.simplify(graph.refsToNodes(route), 0.000001);
            map = new MyMap(f);
            map.drawMap(0);
            map.drawRoute(graph.nodesToRefs(newNodes));
            map.saveMap();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
