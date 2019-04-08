package project.test;

import project.map.Graph;
import project.map.SMap;

import java.io.File;
import java.io.IOException;

public class CreateResources {
    public static void main(String[] args) throws InterruptedException {

        long startTime, endTime;
        String region = args[0];
        String mapDir = System.getProperty("user.dir").concat("/res/");
        File f = new File(mapDir.concat(region).concat(".osm.pbf"));

        File fregion = new File(System.getProperty("user.dir").concat("/files/" + region + "/"));
        fregion.mkdirs();
        try {
            fregion.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Graph graph;

        try {
            startTime = System.nanoTime();
            SMap map = new SMap(f, region, 1024, false);
            map.draw();
            endTime = System.nanoTime();
            System.out.println("Total map drawing time: " + (((float) endTime - (float) startTime) / 1000000000));


            startTime = System.nanoTime();
            graph = new Graph(f, region);
            endTime = System.nanoTime();
            System.out.println("Making graph time: " + (((float) endTime - (float) startTime) / 1000000000));

        } catch (IOException e) {
            System.out.println("IO Exception on resource creation.");
        }
    }
}