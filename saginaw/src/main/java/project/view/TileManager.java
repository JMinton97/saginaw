package project.view;

public class TileManager implements Runnable {

    Tile[][] grid;
    MapPane mapPane;

    public TileManager(Tile[][] grid, MapPane mapPane){
        this.grid = grid;
        this.mapPane = mapPane;
    }

    public void run() {
        while(!Thread.currentThread().isInterrupted()){
            try{
                Thread.sleep(500);
            }catch(InterruptedException e){}
            mapPane.repaint();
            for(int x = 0; x < grid.length; x++){
                for(int y = 0; y < grid[0].length; y++){
//                    System.out.println(grid[x][y]);
//                    System.out.println(mapPane.getBottomRight());
                    if(mapPane.getBottomRight() != null){
                        grid[x][y].check(mapPane);
                    }
                }
            }

        }
    }
}
