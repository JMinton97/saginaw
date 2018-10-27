import java.io.IOException;

public class Runner {
    public static void main(String[] args) throws IOException {
        MyMap map = new MyMap("greater-london-latest.osm.pbf");
        map.drawMap();
    }
}