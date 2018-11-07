package project.map;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class TestRun {
    public static void main(String[] args) {
        File f = new File("Birmingham.osm.pbf");
        MyGraph graph;
        MyMap map;
        try {
            graph = new MyGraph(f);
            map = new MyMap(f);
            Long src = Long.parseLong("1107401572");
            Long dst = Long.parseLong("267536186");
            Dijkstra dijk = new Dijkstra(graph, src);
            System.out.println("Distance: " + dijk.getDistTo().get(dst));
            ArrayList<Long> route = new ArrayList<>();
            Long next = dst;
            route.add(next);
            while(next != null){
                System.out.println(next);
                next = dijk.getEdgeTo().get(next);
                route.add(next);
            }
            map.drawMap(0);
            map.drawRoute(route);
            map.saveMap();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
