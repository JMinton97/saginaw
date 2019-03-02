package project.search;

import java.util.ArrayList;

public interface Searcher {
    void search(int src, int dst);
    void clear();
    double getDist();
    int getExplored();
    ArrayList<Integer> getRoute();
    ArrayList<Long> getRouteAsWays();
}
