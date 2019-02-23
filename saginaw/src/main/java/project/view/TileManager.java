package project.view;

public class TileManager implements Runnable {

    Tile[][] grid;
    Canvas canvas;

    public TileManager(Tile[][] grid, Canvas canvas){
        this.grid = grid;
        this.canvas = canvas;
    }

    public void run() {
        while(!Thread.currentThread().isInterrupted()){
            for(int x = 0; x < grid.length; x++){
                for(int y = 0; y < grid[0].length; y++){
//                    System.out.println(grid[x][y]);
//                    System.out.println(canvas.getBottomRight());
                    if(canvas.getBottomRight() != null){
                        grid[x][y].check(canvas);
                    }
                }
            }

        }
    }
}
