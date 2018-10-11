import java.io.IOException;

public class Runner {
    public static void main(String[] args) throws IOException {
        MyMap map = new MyMap("Birmingham.osm.pbf");
        map.drawMap();
    }
}
