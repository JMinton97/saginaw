package project.map;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Stack;

public class PathHull {
    Deque<Point2D> deque;
    private Stack<Point2D> historyPoints;
    private Stack<Integer> historyOps;
    int top;
    int bot;

    public int getPhTag() {
        return phTag;
    }

    public void setPhTag(int phTag) {
        this.phTag = phTag;
    }

    private int phTag;

    protected PathHull(Point2D e1, Point2D e2){
        deque.add(e1);
        deque.add(e2);
        historyPoints = new Stack<>();
        historyOps = new Stack<>();
        historyPoints.add(e2);
        historyOps.add(0); //pushop
    }

    public void add(Point2D p){
        boolean topFlag, botFlag;
        topFlag = DouglasPeucker.leftOf(deque.getFirst(), getSecond(), p);
        botFlag = DouglasPeucker.leftOf(getSecondLast(), deque.getLast(), p);
        if(topFlag || botFlag){
            while(topFlag){
                popTop();
                topFlag = DouglasPeucker.leftOf(deque.getFirst(), getSecond(), p);
            }
            while(botFlag){
                popBottom();
                botFlag = DouglasPeucker.leftOf(getSecondLast(), deque.getLast(), p);
            }
            push(p);
        }
    }

    private Point2D getSecond(){
        Point2D tempRemove = deque.removeFirst();
        Point2D second = deque.getFirst();
        deque.addFirst(tempRemove);
        return second;
    }

    private Point2D getSecondLast(){
        Point2D tempRemove = deque.removeLast();
        Point2D secondLast = deque.getLast();
        deque.addLast(tempRemove);
        return secondLast;
    }

    private void popTop(){
        historyPoints.add(deque.getFirst());
        historyOps.add(1); //topop
    }

    private void popBottom(){
        historyPoints.add(deque.getLast());
        historyOps.add(2); //botop
    }

    private void push(Point2D p){
        deque.addFirst(p);
        deque.addLast(p);
        historyPoints.add(p);
        historyOps.add(0);
    }

    protected void split(Point2D p){
        Point2D tempPoint;
        int tempOp = historyOps.peek();
        int hp = historyOps.size();
        while(!historyOps.empty() && ((historyPoints.get(hp)) != p || tempOp != 0)){
            tempOp = historyOps.get(hp);
            tempPoint = historyPoints.get(hp);
            hp--;
            if(tempOp == 0){
                deque.removeFirst();
                deque.removeLast();
            }
            if(tempOp == 1){
                deque.addFirst(tempPoint);
            }
            if(tempOp == 1){
                deque.addLast(tempPoint);
            }
        }
    }

    protected ArrayList<Point2D> getQueueAsList(){
        return new ArrayList(deque);
    }
}