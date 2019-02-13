package project.view;

import project.controller.Controller;
import project.model.Model;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public class CanvasMouseWheelListener implements MouseWheelListener
{

    Model model;
    View view;
    Controller controller;

    public CanvasMouseWheelListener(Model model, View view, Controller controller)
    {
        this.model = model;
        this.view = view;
        this.controller = controller;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if(e.getWheelRotation() > 0){
            System.out.println(true);
            model.zoomOut();
        } else {
//            model.zoomIn();
            model.zoomIn(view.getCanvas().canvasToGeo(e.getX(), e.getY()));
        }
        view.getCanvas().repaint();
        System.out.println(e.getWheelRotation());
    }
}
