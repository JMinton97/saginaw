package project.view;

import project.controller.Controller;
import project.model.Model;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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

	private Point2D centre;
	private Point2D origin;
	private Point2D topLeft;
	private double scale;
	private double oX, oY;

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
		keyListener = new CanvasKeyboardListener(controller);
		addMouseListener(mouseListener);
		addKeyListener(keyListener);
		this.setSize(500, 500);
		origin = model.getOrigin();
		oX = origin.getX() / model.getScale();
		oY = origin.getY() / model.getScale();
		tileGrid = new Tile[20][20];
		for(int x = 0; x < tileGrid[0].length; x++){
			for(int y = 0; y < tileGrid[0].length; y++){
				tileGrid[x][y] = new Tile(x, y, 1, model.getRegion());
				Double tileX = origin.getX() + (0.125 * x);
				Double tileY = origin.getY() - (0.125 * y);
				tileGrid[x][y].setTopLeft(new Point2D.Double(tileX, tileY));
			}
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
		//
		// The ViewPort is the part of the canvas that is displayed.
		// By scrolling the ViewPort, you move it across the full size canvas,
		// showing only the ViewPort sized window of the canvas at any one time.

		if (model.isActive())
		{
			// Draw the display image on the full size canvas
			g2.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), null);

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
		int z = model.getZ();
		int x = model.getX();
		int y = model.getY();
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

		//use a grid of Tile objects - iterate through on each update, loading in any needed or 'nearly' needed, removing those not needed.

		Graphics g = this.getGraphics();

		g.fillRect(0, 0, 500, 500);

		centre = model.getCentre();
		scale = model.getScale();
		Point2D topLeft = new Point2D.Double((centre.getX() - (250 / scale)), (centre.getY() - (250 / scale)));
		Point2D bottomRight = new Point2D.Double((centre.getX() + (250 / scale)), (centre.getY() + (250 / scale)));

		Tile t;
		Point2D.Double p, o;

//		System.out.println("ayyy");

		for(int x = 0; x < tileGrid[0].length; x++){
			for(int y = 0; y < tileGrid[0].length; y++){
				if(tileGrid[x][y].overlaps(topLeft, bottomRight)){
					System.out.println("yup");
					t = tileGrid[x][y];
					p = geoToCanvas(t.getTopLeft());
					o = geoToCanvas(topLeft);
//					System.out.println(p);
					g.drawImage(t.getImage(), (int) (p.getX() - o.getX()), (int) (p.getY() - o.getY()), 500, 500, null);
				}
			}
		}

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

	public Point2D.Double geoToCanvas(Point2D geoCoord){
		Double x = origin.getX() + Math.abs(geoCoord.getX() - origin.getX()) * 4000;
		Double y = origin.getY() + Math.abs(geoCoord.getY() - origin.getY()) * 4000;
		return new Point2D.Double(x, y);
	}

	public Point2D.Double canvasToGeo(Point2D canvasCoord){
		return new Point2D.Double(-1, -1);
	}




}
