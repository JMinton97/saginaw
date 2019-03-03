package project.view.actions;

import project.controller.Controller;
import project.search.SearchType;
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
public class ChangeSearchAction extends AbstractAction
{
    /**
     *
     */
    private static final long	serialVersionUID	= 9036684359479464138L;
    private View				view;
    private Controller			controller;
    private SearchType          searchType;
    private String              name;

    public ChangeSearchAction(View view, Controller controller, SearchType searchType, String name)
    {
        this.view = view;
        this.controller = controller;
        this.searchType = searchType;
        this.name = name;
        putValue(NAME, name);
    }

    public void actionPerformed(ActionEvent e)
    {
        controller.switchSearchers(searchType);
    }

}

