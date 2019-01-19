package project.map;

import java.awt.geom.Point2D;
import java.util.*;

public class PathHull {
//    Deque<Integer> deque;
    private Stack<Integer> historyPoints;
    private Stack<Integer> historyOps;
    private int[] deque;
    private int top;
    private int bot;

    public int getPhTag() {
        return phTag;
    }

    public void setPhTag(int phTag) {
        this.phTag = phTag;
    }

    private int phTag;

    PathHull(Integer e1, Integer e2){
        top = 5000;
        bot = 5000;
        deque = new int[10000];
        deque[top] = e1;
        top++;
        bot--;
        deque[bot] = e2;
        deque[top] = e2;
        historyPoints = new Stack<>();
        historyOps = new Stack<>();
        historyPoints.add(e2);
        historyOps.add(0); //pushop
//        System.out.println(deque.toString());
    }

    public void add(int p, ArrayList<Point2D.Double> nodes){
        boolean topFlag, botFlag;
        topFlag = DouglasPeucker.leftOf(nodes.get(deque[top]), nodes.get(deque[top - 1]), nodes.get(p));
        botFlag = DouglasPeucker.leftOf(nodes.get(deque[bot + 1]), nodes.get(deque[bot]), nodes.get(p));
        if(topFlag || botFlag){
            while(topFlag){
                popTop();
                topFlag = DouglasPeucker.leftOf(nodes.get(deque[top]), nodes.get(deque[top - 1]), nodes.get(p));
            }
            while(botFlag){
                popBottom();
                botFlag = DouglasPeucker.leftOf(nodes.get(deque[bot + 1]), nodes.get(deque[bot]), nodes.get(p));
            }
            push(p);
        }
//        System.out.println(getQueueAsList());
    }

//    private int getSecond(){
//        int tempRemove = deque.removeFirst();
//        int second = deque.getFirst();
//        deque.addFirst(tempRemove);
//        return second;
//    }
//
//    private int getSecondLast(){
//        int tempRemove = deque.removeLast();
//        int secondLast = deque.getLast();
//        deque.addLast(tempRemove);
//        return secondLast;
//    }

    private void popTop(){
        historyPoints.add(deque[top]);
        top--;
        historyOps.add(1); //topop
    }

    private void popBottom(){
        historyPoints.add(deque[bot]);
        bot++;
        historyOps.add(2); //botop
    }

    private void push(int p){
        deque[top + 1] = p;
        deque[bot - 1] = p;
        top++;
        bot--;
        historyPoints.add(p);
        historyOps.add(0);
    }

    protected void split(Integer p){
        int tempPoint;
        int tempOp;
        while(!historyOps.empty() && ((!(historyPoints.peek()).equals(p)) || historyOps.peek() != 0)){
            tempOp = historyOps.pop();
            tempPoint = historyPoints.pop();
            if(tempOp == 0){
                top--;
                bot++;
            }
            if(tempOp == 1){
                top++;
                deque[top] = tempPoint;
            }
            if(tempOp == 2){
                bot--;
                deque[bot] = tempPoint;
            }
        }
    }

    protected int[] getQueueAsList(){
//        return new ArrayList<Integer>(Arrays.asList(Arrays.copyOfRange(deque, bot, top)));
//        System.out.println(top + " " + bot);
        return Arrays.copyOfRange(deque, bot, top);
//        return new ArrayList<Integer>(Arrays.asList(sub));
//        return new ArrayList(deque);
    }
}