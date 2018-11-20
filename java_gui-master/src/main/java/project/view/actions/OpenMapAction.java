package project.view.actions;

import project.controller.Controller;
import project.utils.SimpleFileFilter;
import project.utils.UnsupportedImageTypeException;
import project.view.View;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

/**
 * User: Alan P. Sexton Date: 20/06/13 Time: 21:04
 */
public class OpenMapAction extends AbstractAction
{
    /**
     *
     */
    private static final long	serialVersionUID	= 9036684359479464138L;
    private View				view;
    private Controller			controller;

    // Note that once we first create a file chooser object, we keep it and
    // re-use
    // it rather than creating a new one each time that we invoke this action.
    // This has the effect that the chooser dialog always starts in the last
    // directory we opened from, rather than going back to the starting
    // directory.
    private JFileChooser		mapFileChooser	= null;

    {
        putValue(NAME, "Open new project map file...");
        putValue(SMALL_ICON, new ImageIcon(
                getClass().getResource("/project/icons/fileopen.png")));
        putValue(SHORT_DESCRIPTION, "Opens a new map file");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("control O"));
    }

    public OpenMapAction(View view, Controller controller)
    {
        this.view = view;
        this.controller = controller;
    }

    public void actionPerformed(ActionEvent e)
    {
        if (mapFileChooser == null)
        {
            mapFileChooser = new JFileChooser(".");
            // Use the following line instead if you always want the file
            // chooser to
            // start in the user's home directory rather than the current
            // directory
            // imageFileChooser = new
            // JFileChooser(System.getProperty("user.dir"));
            SimpleFileFilter filter = new SimpleFileFilter();
            filter.addExtension(".osm.pbf");
            filter.setDescription("pbf map files");
            mapFileChooser.setFileFilter(filter);
        }
        mapFileChooser.setDialogTitle("Choose a map file to open");
        int result = mapFileChooser.showOpenDialog(view);
        try
        {
            if (result == JFileChooser.APPROVE_OPTION)
            {
                File f;
                f = mapFileChooser.getSelectedFile();
                controller.loadMap(f);
            }
        }
        catch (IOException ioe)
        {
            JOptionPane.showMessageDialog(view, ioe.getMessage(),
                    "Accessing Image File", JOptionPane.ERROR_MESSAGE);
        }
    }

}
