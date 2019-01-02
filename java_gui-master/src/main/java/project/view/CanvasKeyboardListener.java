package project.view;

import project.controller.Controller;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class CanvasKeyboardListener implements KeyListener {

    Controller controller;

    public CanvasKeyboardListener(Controller controller){
        this.controller = controller;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if(e.getKeyCode() == (KeyEvent.VK_W)){
            controller.moveMap(0, 10);
        } else if(e.getKeyCode() == (KeyEvent.VK_S)){
            controller.moveMap(0, -10);
        } else if(e.getKeyCode() == (KeyEvent.VK_A)){
            controller.moveMap(-10, 0);
        } else if(e.getKeyCode() == (KeyEvent.VK_D)){
            controller.moveMap(10, 0);
        } else if(e.getKeyCode() == (KeyEvent.VK_Z)){
            controller.zoomOut();
        } else if(e.getKeyCode() == (KeyEvent.VK_C)){
            controller.zoomIn();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
