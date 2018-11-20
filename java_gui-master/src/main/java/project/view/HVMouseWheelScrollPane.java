package project.view;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;

/**
 * User: Alan P. Sexton
 * Date: 24/06/13
 * Time: 09:48
 */

/**
 * A modified version of the standard <code>JScrollPane</code> that lets the
 * wheel mouse scroll vertically as before but, when combined with the shift
 * modifier, scrolls horizontally
 */
class HVMouseWheelScrollPane extends JScrollPane
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3913787275122888204L;

	public HVMouseWheelScrollPane()
	{
		final JScrollBar horizontalScrollBar = getHorizontalScrollBar();
		final JScrollBar verticalScrollBar = getVerticalScrollBar();
		setWheelScrollingEnabled(false);
		addMouseWheelListener(new MouseAdapter()
		{
			public void mouseWheelMoved(MouseWheelEvent evt)
			{
				JScrollBar scrollBar = verticalScrollBar;
				if (evt.isShiftDown())
					scrollBar = horizontalScrollBar;

				int newValue = scrollBar.getValue() + (evt.getWheelRotation()
						* scrollBar.getBlockIncrement()
						* evt.getScrollAmount());
				if (newValue >= 0 && newValue <= scrollBar.getMaximum())
					scrollBar.setValue(newValue);
			}
		});
	}
}