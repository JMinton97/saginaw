package project.view;

import project.controller.Controller;
import project.map.MyNode;
import project.model.Model;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * User: Alan P. Sexton Date: 20/06/13 Time: 18:00
 */
class Canvas extends JPanel
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -7552020673193783095L;
	private Model				model;
	private View				view;

	private CanvasMouseListener	mouseListener;
	private CanvasKeyboardListener keyListener;

	private BufferedImage image;

	private Tile[][] tileGrid;
	private HashMap<Integer, Tile[][]> layers;

	private Point2D centre;
	private Point2D origin;
	private Point2D topLeft, bottomRight;
	private double scale;
	private double zoom;
	private double oX, oY;
	private double imageEdge = 2000;
	private double paneX = 800;
	private double paneY = 800;
	private int xDimension, yDimension;
	private int level, modifier;

	/**
	 * The default constructor should NEVER be called. It has been made private
	 * so that no other class can create a Canvas except by initialising it
	 * properly (i.e. by calling the parameterised constructor)
	 */
	@SuppressWarnings("unused")
	private Canvas()
	{
	}

	/**
	 * Create a <code>Canvas</code> object initialised to the given
	 * <code>View</code> and <code>Model</code>
	 *
	 * @param view
	 *            The View object that encapsulates the whole GUI
	 * @param model
	 *            The Model object that encapsulates the (view-independent) data
	 *            of the application
	 * @param controller
	 *            The Controller object that handles all operations
	 */
	public Canvas(Model model, View view, Controller controller)
	{
		this.view = view;
		this.model = model;
		mouseListener = new CanvasMouseListener(this.model, this.view,
				controller);
		keyListener = new CanvasKeyboardListener(view, controller);
		addMouseListener(mouseListener);
		addKeyListener(keyListener);
		this.setSize((int) paneX, (int) paneY);
		origin = model.getOrigin();
		layers = new HashMap<Integer, Tile[][]>();

		xDimension = 59;
		yDimension = 44;
		this.scale = model.getScale().doubleValue();
		for(int l = 1; l < 512; l *= 2){
			tileGrid = new Tile[(int) Math.ceil(xDimension / (double) l)][(int) Math.ceil(yDimension / (double) l)];
			System.out.println(l + " is " +  (int) Math.ceil(xDimension / (double) l) + ", " + (int) Math.ceil(yDimension / (double) l));
			for(int x = 0; x < tileGrid.length; x++){
				for(int y = 0; y < tileGrid[0].length; y++){
					tileGrid[x][y] = new Tile((l * x) + 1, (l * y) + 1, l, scale, imageEdge, model.getRegion());
					Point2D.Double topLeft = new Point2D.Double(origin.getX() + ((imageEdge / (scale / l)) * x), origin.getY() - ((imageEdge / (scale / l)) * y));
					Point2D.Double bottomRight = new Point2D.Double(topLeft.getX() + (imageEdge / (scale / l)), topLeft.getY() - (imageEdge / (scale / l)));
					tileGrid[x][y].setTopLeftAndBottomRight(topLeft, bottomRight);
				}
			}
			layers.put(l, tileGrid);
			System.out.println();
		}
	}

	/**
	 * The method that is called to paint the contents of this component
	 *
	 * @param g
	 *            The <code>Graphics</code> object used to do the actual drawing
	 */
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		// Using g or g2, draw on the full size "canvas":
		Graphics2D g2 = (Graphics2D) g;

//		Graphics g = this.getGraphics();

//		g.fillRect(0, 0, (int) paneX, (int) paneY);

		System.out.println();

		centre = model.getCentre();
		scale = model.getScale().doubleValue();
		zoom = model.getZ().doubleValue();
		topLeft = new Point2D.Double((centre.getX() - (((paneX / 2) * zoom) / scale)), (centre.getY() + (((paneY / 2) * zoom) / scale)));
		bottomRight = new Point2D.Double((centre.getX() + (((paneX / 2) * zoom) / scale)), (centre.getY() - (((paneY / 2) * zoom) / scale)));
//		System.out.println(topLeft.getX() + ", " + topLeft.getY() + "   " + bottomRight.getX() + ", " + bottomRight.getY());
//		System.out.println("EDGE " + (topLeft.getY() - bottomRight.getY()));

		Tile t;
		Point2D.Double p, o;

//		System.out.println("ayyy");

		boolean flag = true;											//would it be more efficient to declare this outside the method? Ask generally!!!


		level = model.getLevel();
		modifier = (int) Math.pow(2, level - 1);

		tileGrid = layers.get(modifier);
		System.out.println("Level = " + level);

		LOOP: for(int x = 0; x < tileGrid[0].length; x++){
			for(int y = 0; y < tileGrid[0].length; y++){
				if(tileGrid[x][y].overlaps(topLeft, bottomRight)){
					System.out.println("VISIBLE");
					flag = true;
					t = tileGrid[x][y];
					System.out.println(topLeft + " " + bottomRight);
					p = geoToCanvas(t.getTopLeft());
					o = geoToCanvas(topLeft);
//					System.out.println(p);
					g.drawImage(t.getImage(), (int) ((p.getX() - o.getX()) / zoom), (int) ((p.getY() - o.getY()) / zoom), (int) (imageEdge / (zoom / modifier)), (int) (imageEdge / (zoom / modifier)), null);
					g.setColor(Color.RED);
					g.drawRect((int) ((p.getX() - o.getX()) / zoom), (int) ((p.getY() - o.getY())/zoom), (int) (imageEdge / (zoom / modifier)), (int) (imageEdge / (zoom / modifier)));
//					System.out.println(o.getX() + ", " + o.getY() + "    " + p.getX() + ", " + p.getY());
				}
			}
		}

		g.drawString(String.valueOf(zoom), 50, 50);

		drawRoute(model.getRoute(), (Graphics2D) g);


		//
		// The ViewPort is the part of the canvas that is displayed.
		// By scrolling the ViewPort, you move it across the full size canvas,
		// showing only the ViewPort sized window of the canvas at any one time.

		if (model.isActive())
		{
			// Draw the display image on the full size canvas
//			g2.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), null);

			// In case there is some animation going on (e.g. mouse dragging),
			// call this to
			// paint the intermediate images
			mouseListener.paint(g);
		}
	}

	/**
	 * Get the preferred size of the canvas.
	 * 
	 * @return The <code>Dimension</code> object containing the size of the
	 *         underlying image in the model, if one exits, or
	 *         <code>(0,0)</code> if it does not.
	 */
	public Dimension getPreferredSize()
	{
		return model.getDimensions();
	}

	public void updateRegion(){
		int x = model.getX();
		int y = model.getY();
		int z = 1;
//		BufferedImage bi = ImageIO.read(new File("draw/".concat(model.getRegion()).concat("/").concat(z).concat("/").concat()))
		try {
			System.out.println("draw/" + model.getRegion() + "/" + z + "/" + x + "-" + y + ".png");
			long startTime = System.nanoTime();
			image = ImageIO.read(new File("draw/" + model.getRegion() + "/" + z + "/" + x + "-" + y));
			long endTime = System.nanoTime();
			System.out.println("Load: " + (((float) endTime - (float)startTime) / 1000000000));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	public void update() {
//		System.out.println("UPDATE!");

		//use a grid of Tile objects - iterate through on each update, loading in any needed or 'nearly' needed, removing those not needed.



//		System.out.println("ahh");

////		System.out.println(topLeft.getX() + ", " + x + "  |  " + topLeft.getY() + ", " + y);
//
//
//
//

//
//		double imageOrigX = origin.getX() + (500 * (x + 1));
//		double imageOrigY = origin.getY() + (500 * (y + 1));
//		double paneOrigX = (topLeft.getX() - origin.getX()) * scale;
//		double paneOrigY = (origin.getY() - topLeft.getY()) * scale;
//
////		System.out.println(imageOrigX + " " + imageOrigY);
//		g.drawImage(tileGrid[x][y], (int) (paneOrigX - imageOrigX), (int) (paneOrigY - imageOrigY), 500, 500, null);
//
//		System.out.println((paneOrigX - imageOrigX) + "    " + (paneOrigY - imageOrigY));

	}

	public void drawRoute(ArrayList<Point2D.Double> route, Graphics2D g){
		Point2D o = geoToCanvas(topLeft);
		Path2D path = new Path2D.Double();
		Point2D first = geoToCanvas(route.get(0));
		path.moveTo((int) Math.round((first.getX() - o.getX()) / zoom), (int) Math.round((first.getY()  - o.getY()) / zoom));
		for(Point2D.Double point : route){
			point = geoToCanvas(point);
			path.lineTo((int) Math.round((point.getX() - o.getX()) / zoom), (int) Math.round((point.getY()  - o.getY()) / zoom));
			System.out.println((int) Math.round((point.getX() - o.getX()) / zoom) + " " + (int) Math.round((point.getY()  - o.getY()) / zoom));
		}
		g.setColor(Color.RED.darker());
		g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(path);
		g.setColor(Color.RED);
		g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(path);

	}

	public Point2D.Double geoToCanvas(Point2D geoCoord){
//		System.out.println(scale);
		Double x = origin.getX() + (Math.abs(geoCoord.getX() - origin.getX()) * scale);
		Double y = origin.getY() + (Math.abs(geoCoord.getY() - origin.getY()) * scale);
//		System.out.println("Here" + (origin.getY() + Math.abs(geoCoord.getY() - origin.getY()) * scale));
		return new Point2D.Double(x, y);
	}

	public Point2D.Double canvasToGeo(Point2D canvasCoord){
		return new Point2D.Double(-1, -1);
	}




}
