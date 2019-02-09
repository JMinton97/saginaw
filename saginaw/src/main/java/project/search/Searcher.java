package project.search;

import java.util.ArrayList;

public interface Searcher {
    ArrayList<Long> search(long src, long dst);
    double getDist();
}
