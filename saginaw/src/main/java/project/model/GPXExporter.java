package project.model;

import project.map.MyGraph;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class GPXExporter {

    public void makeGPXFile(File f, Route route, String routeName){
        String gpxText =    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                            "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\" creator=\"Saginaw\">\n" +
                            "    <metadata>\n" +
                            "        <name>" + routeName + "</name>\n" +
                            "    </metadata>\n" +
                            "    <rte>\n" +
                            "        <name>" + routeName + "</name>\n";

        int segCtr = 0;
        for(Segment segment : route.getSegments()){
            segment.getFullDetailRoute();
            int ptCtr = 0;
            for(Point2D.Double point : segment.getPoints()){

                gpxText +=  "        <rtept lon=\"" + point.getX() + "\" lat=\"" + point.getY() + "\">\n" +
                            "            <ele>0.0</ele>\n" +
                            "            <name>Segment " + segCtr + ", Point " + ptCtr + "</name>\n" +
                            "        </rtept>\n";
                ptCtr++;
            }
            segCtr++;
        }

        gpxText +=          "    </rte>\n" +
                            "</gpx>";

        try {
            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(f), "utf-8");
            osw.write(gpxText);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
