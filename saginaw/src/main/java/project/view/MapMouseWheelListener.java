package project.view;

import project.controller.Controller;
import project.model.Model;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public class MapMouseWheelListener implements MouseWheelListener
{

    Model model;
    View view;
    Controller controller;

    public MapMouseWheelListener(Model model, View view, Controller controller)
    {
        this.model = model;
        this.view = view;
        this.controller = controller;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if(e.getWheelRotation() > 0){
            model.zoomOut(view.getMapPane().canvasToGeo(e.getX(), e.getY()));
        } else {
//            model.zoomIn();
            model.zoomIn(view.getMapPane().canvasToGeo(e.getX(), e.getY()));
        }
        view.getMapPane().repaint();
    }
}