package project.map;

import com.sun.tools.javac.util.Pair;

import java.util.ArrayList;

public class Vertex {
    protected long id;
    protected ArrayList<GraphLink> edges;

    public Vertex(long id){
        this.id = id;
    }
}
