package project.view;

import project.controller.Controller;
import project.model.Model;
import project.view.actions.ExitAction;
import project.view.actions.LongRunningAction;
import project.view.actions.OpenAction;
import project.view.actions.OpenMapAction;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * User: Alan P. Sexton Date: 21/06/13 Time: 13:42
 */
public class View extends JFrame
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= -6963519874728205328L;
	private Canvas				canvas				= null;
	private JScrollPane			canvasScrollPane;

	public View(Model model, Controller controller)
	{
		super("Basic Java GUI Application");
		controller.addView(this);

		// We will use the default BorderLayout, with a scrolled panel in
		// the centre area, a tool bar in the NORTH area and a menu bar

		canvasScrollPane = new HVMouseWheelScrollPane();

		// The default when scrolling is very slow. Set the increment as
		// follows:
		canvasScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		canvasScrollPane.getHorizontalScrollBar().setUnitIncrement(16);

		canvas = new Canvas(model, this, controller);
		canvasScrollPane.setViewportView(canvas);
		getContentPane().add(canvasScrollPane, BorderLayout.CENTER);

		// exitAction has to be final because we reference it from within
		// an inner class

		final AbstractAction exitAction = new ExitAction(this, controller);
		AbstractAction openAction = new OpenAction(this, controller);
		AbstractAction openMapAction = new OpenMapAction(this, controller);
		AbstractAction longRunningAction = new LongRunningAction(this,
				controller);

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent we)
			{
				exitAction.actionPerformed(null);
			}
		});

		// Set up the menu bar
		JMenu fileMenu;
		fileMenu = new JMenu("File");
		fileMenu.add(openAction);
		fileMenu.add(longRunningAction);
		fileMenu.add(openMapAction);
		fileMenu.addSeparator();
		fileMenu.add(exitAction);

		JMenuBar menuBar;

		menuBar = new JMenuBar();
		menuBar.add(fileMenu);

		setJMenuBar(menuBar);

		// Set up the tool bar
		JToolBar toolBar;
		toolBar = new JToolBar();
		toolBar.setFloatable(true);
		toolBar.setRollover(true);
		toolBar.add(exitAction);
		toolBar.add(openMapAction);
		toolBar.addSeparator();
		toolBar.add(openAction);
		toolBar.add(longRunningAction);

		getContentPane().add(toolBar, BorderLayout.NORTH);

		pack();
		setBounds(0, 0, 700, 800);

		updateRegion();

		canvas.grabFocus();
	}

	public void adaptToNewImage()
	{
//		setCanvasSize();
	}

	public void updateRegion() {
		canvas.update();
//		repaint();
	}

	/**
	 * Adapt the settings for the ViewPort and scroll bars to the dimensions
	 * required. This needs to be called any time the image changes size.
	 */
	protected void setCanvasSize()
	{
		canvas.setSize(canvas.getPreferredSize());

		// need this so that the scroll bars knows the size of the canvas that
		// has to be
		// scrolled over
		canvas.validate();
	}

	protected Canvas getCanvas()
	{
		return canvas;
	}

	protected JScrollPane getCanvasScrollPane()
	{
		return canvasScrollPane;
	}

	public void upDoug(){
		canvas.upDoug();
	}

	public void downDoug(){
		canvas.downDoug();
	}
}
