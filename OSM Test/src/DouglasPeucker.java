import javafx.scene.shape.Polyline;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DouglasPeucker {
//    public void decimate(Points[] points, int threshold) {
//        line = new line(points[0], points[points.length];
//        furthest = new int;
//        distance = new distance;
//        for(int i = 0, i < points.length, i++){
//            if(getDistance(points[i], line) > distance){
//                furthest = i;
//                distance = getDistance(points[i], line);
//            }
//
//            if(distance > threshold){
//                Points[] segment1 = decimate(points[0..i], threshold);
//                Points[] segment2 = decimate(points[i..end], threshold);
//                return(segment1.append(segment2.tail()));
//            } else {
//                return([points(1), points(end)]);
//            }
//        }
//  }
        public static List<Long> decimate(List<Long> nodes, Double threshold, Map<Long, MyNode> dictionary) {
            Point2D first = dictionary.get(nodes.get(0)).getPoint();
            Point2D last = dictionary.get(nodes.get(nodes.size() - 1)).getPoint();
            Line2D line = new Line2D.Double(first, last);
            int furthest = 0;
            Double distance = 0.0;
            for(int i = 0; i < nodes.size(); i++) {
                Double thisDistance = line.ptLineDist(dictionary.get(nodes.get(i)).getPoint());
                if (thisDistance > distance) {
                    furthest = i;
                    distance = thisDistance;
                }
            }
            if(distance > threshold){
                List<Long> segment1 = decimate(nodes.subList(0, furthest), threshold, dictionary);
                List<Long> segment2 = decimate(nodes.subList(furthest, nodes.size()), threshold, dictionary);
                segment1.addAll(segment2.subList(1, segment2.size()));
                return segment1;
            } else {
                ArrayList<Long> returnList = new ArrayList<Long>();
                returnList.add(nodes.get(0));
                returnList.add(nodes.get(nodes.size() - 1));
                return returnList;
            }
    }
}