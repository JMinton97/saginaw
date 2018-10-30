package project.map;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Stack;

public class PathHull {
    Deque<Integer> deque;
    private Stack<Integer> historyPoints;
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

    protected PathHull(Integer e1, Integer e2){
        deque = new LinkedList<>();
        deque.add(e1);
        deque.addFirst(e2);
        deque.addLast(e2);
        historyPoints = new Stack<>();
        historyOps = new Stack<>();
        historyPoints.add(e2);
        historyOps.add(0); //pushop
        System.out.println(deque.toString());
    }

    public void add(Integer p, ArrayList<Point2D> nodes){
        boolean topFlag, botFlag;
        topFlag = DouglasPeucker.leftOf(nodes.get(deque.getFirst()), nodes.get(getSecond()), nodes.get(p));
        botFlag = DouglasPeucker.leftOf(nodes.get(getSecondLast()), nodes.get(deque.getLast()), nodes.get(p));
        if(topFlag || botFlag){
            while(topFlag){
                popTop();
                topFlag = DouglasPeucker.leftOf(nodes.get(deque.getFirst()), nodes.get(getSecond()), nodes.get(p));
            }
            while(botFlag){
                popBottom();
                botFlag = DouglasPeucker.leftOf(nodes.get(getSecondLast()), nodes.get(deque.getLast()), nodes.get(p));
            }
            push(p);
        }
//        System.out.println(getQueueAsList());
    }

    private int getSecond(){
        int tempRemove = deque.removeFirst();
        int second = deque.getFirst();
        deque.addFirst(tempRemove);
        return second;
    }

    private int getSecondLast(){
        int tempRemove = deque.removeLast();
        int secondLast = deque.getLast();
        deque.addLast(tempRemove);
        return secondLast;
    }

    private void popTop(){
        historyPoints.add(deque.getFirst());
        deque.removeFirst();
        historyOps.add(1); //topop
    }

    private void popBottom(){
        historyPoints.add(deque.getLast());
        deque.removeLast();
        historyOps.add(2); //botop
    }

    private void push(int p){
        deque.addFirst(p);
        deque.addLast(p);
        historyPoints.add(p);
        historyOps.add(0);
    }

    protected void split(Integer p){
        int tempPoint;
        int tempOp = historyOps.peek();
//        System.out.println(getQueueAsList());
        while(!historyOps.empty() && ((historyPoints.peek()) != p || historyOps.peek() != 0)){
//            System.out.println("loop");
            tempOp = historyOps.pop();
            tempPoint = historyPoints.pop();
//            System.out.println(tempOp + " " + tempPoint);
            if(tempOp == 0){
                deque.removeFirst();
                deque.removeLast();
            }
            if(tempOp == 1){
                deque.addFirst(tempPoint);
            }
            if(tempOp == 2){
                deque.addLast(tempPoint);
            }
//            System.out.println(getQueueAsList());
//            System.out.println("Next: " history);
        }
    }

    protected ArrayList<Integer> getQueueAsList(){
        return new ArrayList(deque);
    }
}