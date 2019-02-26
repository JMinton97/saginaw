package project.search;

import java.util.ArrayList;

public interface Searcher {
    ArrayList<Long> search(int src, int dst);
    double getDist();
    int getExplored();
}
