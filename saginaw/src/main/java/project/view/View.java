package project.view;

import com.google.common.math.DoubleMath;
import project.controller.Controller;
import project.model.GPXExporter;
import project.model.Model;
import project.search.SearchType;
import project.view.actions.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

/**
 * User: Alan P. Sexton Date: 21/06/13 Time: 13:42
 */
public class View extends JFrame
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= -6963519874728205328L;
	private MapPane mapPane;
	private JPanel infoPanel = null;
	private JLabel distance;
	private JFrame frame;
	private Model model;
	private Controller controller;

	public View(Model model, Controller controller) {
		super("Saginaw v0.1");
		controller.addView(this);
		setBounds(100, 10, 1200, 800);
//		setBackground(Color.MAGENTA);
		frame = this;

//		try{
//			final BufferedImage splashImg = ImageIO.read(new File(System.getProperty("user.dir").concat("/res/icon/splash.png")));
//			SplashPanel splashPanel = new SplashPanel(splashImg);
//			getContentPane().add(splashPanel);
//			System.out.println(splashPanel.getWidth());
//			splashPanel.repaint();
//			splashPanel.updateUI();
//			splashPanel.paintImmediately(0, 0, 1200, 800);
//		}catch(IOException e){
//			e.printStackTrace();
//		}

		this.model = model;
		this.controller = controller;

		validate();

	}


	public void startMap(){


		// We will use the default BorderLayout, with a panel in
		// the centre area, a tool bar in the NORTH area and a menu bar

		mapPane = new MapPane(model, this, controller);
		getContentPane().add(mapPane, BorderLayout.CENTER);

		// exitAction has to be final because we reference it from within
		// an inner class

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		addWindowListener(new WindowAdapter()
//		{
//			public void windowClosing(WindowEvent we)
//			{
//				exitAction.actionPerformed(null);
//			}
//		});

		// Set up the menu bar
		JMenu fileMenu;
        fileMenu = new JMenu("File");
		fileMenu.addSeparator();

        JMenu searchMenu;
        searchMenu = new JMenu("Search method");

		ArrayList<JRadioButtonMenuItem> searchMethods = new ArrayList<>();

		AbstractAction clearRouteAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				controller.clearMarkers();
				mapPane.repaint();
			}
		};
		clearRouteAction.putValue(Action.SMALL_ICON, new ImageIcon(
				getClass().getResource("/project/icons/cancel.png")));


		AbstractAction showGridAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mapPane.toggleGrid();
			}
		};
		showGridAction.putValue(Action.SMALL_ICON, new ImageIcon(
				getClass().getResource("/project/icons/grid.png")));


		AbstractAction repaintMapAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mapPane.repaint();
			}
		};
		repaintMapAction.putValue(Action.SMALL_ICON, new ImageIcon(
				getClass().getResource("/project/icons/redraw.png")));

		AbstractAction undoAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				controller.undoLastMarker();
			}
		};
		undoAction.putValue(Action.SMALL_ICON, new ImageIcon(
				getClass().getResource("/project/icons/undo.png")));

		AbstractAction freshSearchAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				controller.freshSearch();
			}
		};
		freshSearchAction.putValue(Action.SMALL_ICON, new ImageIcon(
				getClass().getResource("/project/icons/search.png")));

		AbstractAction saveRouteAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveRoute();
			}
		};
		saveRouteAction.putValue(Action.SMALL_ICON, new ImageIcon(
				getClass().getResource("/project/icons/export.png")));



        ButtonGroup searchMethodGroup = new ButtonGroup();
		searchMethods.add(new JRadioButtonMenuItem(new ChangeSearchAction(this, controller, SearchType.DIJKSTRA, "Dijkstra")));
		searchMethods.add(new JRadioButtonMenuItem(new ChangeSearchAction(this, controller, SearchType.BIDIJKSTRA, "Bidirectional Dijkstra")));
		searchMethods.add(new JRadioButtonMenuItem(new ChangeSearchAction(this, controller, SearchType.CONCURRENT_BIDIJKSTRA, "Concurrent Bidirectional Dijkstra")));
		searchMethods.add(new JRadioButtonMenuItem(new ChangeSearchAction(this, controller, SearchType.ALT, "ALT")));
		searchMethods.add(new JRadioButtonMenuItem(new ChangeSearchAction(this, controller, SearchType.BIALT, "Bidirectional ALT")));
		searchMethods.add(new JRadioButtonMenuItem(new ChangeSearchAction(this, controller, SearchType.CONCURRENT_BIALT, "Concurrent Bidirectional ALT")));
		searchMethods.add(new JRadioButtonMenuItem(new ChangeSearchAction(this, controller, SearchType.CONTRACTION_ALT, "CALT (Contraction-ALT)")));

		for(JRadioButtonMenuItem item : searchMethods){
			searchMethodGroup.add(item);
			searchMenu.add(item);
			item.setSelected(true);
		}

		JMenuBar menuBar;

		menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		menuBar.add(searchMenu);

		setJMenuBar(menuBar);

//		// Set up the tool bar
//		toolBar = new JToolBar();
////		toolBar.setMargin(new Insets(10, 10, 10, 10));
//		toolBar.setFloatable(true);
//		toolBar.setRollover(true);
//		toolBar.addSeparator();
//
//		toolBar.addSeparator(new Dimension(100, 10));
//		toolBar.add(clearRouteAction);
//		toolBar.add(showGridAction);
//		toolBar.add(repaintMapAction);
//
//		getContentPane().add(toolBar, BorderLayout.NORTH);

		infoPanel = new JPanel(new FlowLayout());
		infoPanel.setPreferredSize(new Dimension(1200,60));
		distance = new JLabel("Distance: ");

        try {
            GraphicsEnvironment ge =
                    GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("/res/fonts/Montserrat-Regular.ttf")));
        } catch (IOException|FontFormatException e) {
        }
		distance.setFont(new Font("Montserrat-Regular", Font.BOLD, 20));
		infoPanel.add(distance);
        infoPanel.add(new JButton(undoAction));
		infoPanel.add(new JButton(clearRouteAction));
		infoPanel.add(new JButton(freshSearchAction));
		infoPanel.add(new JButton(showGridAction));
		infoPanel.add(new JButton(repaintMapAction));
		infoPanel.add(new JButton(saveRouteAction));


		getContentPane().add(infoPanel, BorderLayout.SOUTH);

//		pack();

		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent componentEvent) {
				mapPane.changeMapSize(frame.getWidth(), frame.getHeight());
			}
		});

		mapPane.grabFocus();
		updateInfo();
	}



	public void adaptToNewImage()
	{
//		setCanvasSize();
	}

	protected MapPane getMapPane()
	{
		return mapPane;
	}

	public void toggleDoug(){
		mapPane.toggleDoug();
	}

	public void downDoug(){
		mapPane.downDoug();
	}

	public void upDoug(){
		mapPane.upDoug();
	}

	public double[] getClickCoordinate(int x, int y){
		return mapPane.getClickCoordinate(x, y);
	}

	public void updateInfo(){
		double distance  = model.getRoute().getDistance();
		if(distance < 1200){
			this.distance.setText("Distance: " + (int) Math.floor(distance) + "m");
		} else {
			distance /= 1000;
			this.distance.setText("Distance: " + new BigDecimal(distance).setScale(2, RoundingMode.HALF_UP).doubleValue() + "km");
		}
		infoPanel.repaint();
	}

	protected void saveRoute() {
		String name = (String)JOptionPane.showInputDialog(frame, "Enter a name for your route: ", "Route Export",
				JOptionPane.PLAIN_MESSAGE,
				new ImageIcon(
						getClass().getResource("/project/icons/export.png")),
				null,
				"");
		JFileChooser fileChooser = new JFileChooser();
		int choice = fileChooser.showSaveDialog(this);
		if (choice == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			if (file == null) {
				return;
			}
			if (!file.getName().toLowerCase().endsWith(".gpx")) {
				file = new File(file.getParentFile(), file.getName() + ".gpx");
			}
			new GPXExporter().makeGPXFile(file, model.getRoute(), name);
		}
	}
}









class SplashPanel extends JPanel {

	private BufferedImage image;

	public SplashPanel(BufferedImage image){
		this.image = image;
		setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
		System.out.println(image.getWidth());
	}

	@Override
	protected void paintComponent(Graphics g) {
		System.out.println("PAINT");
		Thread.dumpStack();
		super.paintComponent(g);
		g.drawImage(image, 0, 0, null);
	}
}