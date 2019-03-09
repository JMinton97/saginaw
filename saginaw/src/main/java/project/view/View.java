package project.view;

import com.google.common.math.DoubleMath;
import project.controller.Controller;
import project.model.Model;
import project.search.SearchType;
import project.view.actions.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
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
	private MapPane mapPane = null;
	private JPanel infoPane = null;
	private JLabel distance;
	private JFrame frame;
	private JToolBar toolBar;
	private Model model;

	public View(Model model, Controller controller)
	{
		super("Saginaw v0.1");
		controller.addView(this);

		frame = this;

		this.model = model;

		// We will use the default BorderLayout, with a panel in
		// the centre area, a tool bar in the NORTH area and a menu bar

		mapPane = new MapPane(model, this, controller);
		getContentPane().add(mapPane, BorderLayout.CENTER);

		infoPane = new InfoPane();

		// exitAction has to be final because we reference it from within
		// an inner class

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
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
				getClass().getResource("/project/icons/exit.png")));

		AbstractAction showGridAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mapPane.toggleGrid();
			}
		};
		showGridAction.putValue(Action.SMALL_ICON, new ImageIcon(
				getClass().getResource("/project/icons/exit.png")));

		AbstractAction repaintMapAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mapPane.repaint();
			}
		};
		repaintMapAction.putValue(Action.SMALL_ICON, new ImageIcon(
				getClass().getResource("/project/icons/exit.png")));

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

		// Set up the tool bar
		toolBar = new JToolBar();
//		toolBar.setMargin(new Insets(10, 10, 10, 10));
		toolBar.setFloatable(true);
		toolBar.setRollover(true);
		toolBar.addSeparator();
		distance = new JLabel();
		updateInfo();
		toolBar.add("distance", distance);
		toolBar.addSeparator(new Dimension(100, 10));
		toolBar.add(clearRouteAction);
		toolBar.add(showGridAction);
		toolBar.add(repaintMapAction);

		getContentPane().add(toolBar, BorderLayout.NORTH);

		getContentPane().add(makeInfoPane(), BorderLayout.SOUTH);

		pack();
		setBounds(0, 0, 1200, 800);

		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent componentEvent) {
				mapPane.changeMapSize(frame.getWidth(), frame.getHeight());
			}
		});

		mapPane.grabFocus();
		updateInfo();
		toolBar.updateUI();
	}

	private JPanel makeInfoPane(){
		SpringLayout spr = new SpringLayout();
		JPanel routeInfo = new JPanel(spr);
		routeInfo.setPreferredSize(new Dimension(1200,100));
		distance = new JLabel("Distance: ");
		routeInfo.add(distance);
		spr.putConstraint(SpringLayout.NORTH, distance, 20,
				SpringLayout.NORTH, routeInfo);
		spr.putConstraint(SpringLayout.WEST, distance, 20,
				SpringLayout.WEST, routeInfo);
		return routeInfo;
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

	public double[] getClickCoordinate(int x, int y){
		return mapPane.getClickCoordinate(x, y);
	}

	public void updateInfo(){
		double distance  = model.getRouteDistance();
		if(distance < 1200){
			this.distance.setText("Distance: " + (int) Math.floor(distance) + "m");
		} else {
			distance /= 1000;
			this.distance.setText("Distance: " + new BigDecimal(distance).setScale(2, RoundingMode.HALF_UP).doubleValue() + "km");
		}
		toolBar.repaint();
	}

	public void repaint(){}

}
