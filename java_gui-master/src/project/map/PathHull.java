package project.map;

import java.awt.geom.Point2D;
import java.util.*;

public class PathHull {
//    Deque<Integer> deque;
    private Stack<Integer> historyPoints;
    private Stack<Integer> historyOps;
    private int[] deque;
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
//        deque = new LinkedList<>();
//        deque.add(e1);
//        deque.addFirst(e2);
//        deque.addLast(e2);
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

    public void add(int p, Point2D[] nodes){
        boolean topFlag, botFlag;
        topFlag = DouglasPeucker.leftOf(nodes[deque[top]], nodes[deque[top - 1]], nodes[p]);
        botFlag = DouglasPeucker.leftOf(nodes[deque[bot + 1]], nodes[deque[bot]], nodes[p]);
        if(topFlag || botFlag){
            while(topFlag){
//                System.out.println(top + " " + bot);
                popTop();
                topFlag = DouglasPeucker.leftOf(nodes[deque[top]], nodes[deque[top - 1]], nodes[p]);
//                System.out.println("comparing " + nodes[deque[top]].toString() + " " + nodes[deque[top - 1]].toString());
            }
            while(botFlag){
                popBottom();
                botFlag = DouglasPeucker.leftOf(nodes[deque[bot + 1]], nodes[deque[bot]], nodes[p]);
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
//        System.out.println("split");
//        System.out.println("split");
        int tempPoint;
        int tempOp;
//        System.out.println(getQueueAsList());
        while(!historyOps.empty() && ((historyPoints.peek()) != p || historyOps.peek() != 0)){
//            System.out.println("loop");
            tempOp = historyOps.pop();
            tempPoint = historyPoints.pop();
//            System.out.println(tempOp + " " + tempPoint);
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
//            System.out.println(getQueueAsList());
//            System.out.println("Next: " history);
        }
    }

    protected int[] getQueueAsList(){
//        return new ArrayList<Integer>(Arrays.asList(Arrays.copyOfRange(deque, bot, top)));
//        System.out.println(top + " " + bot);
        int[] sub = Arrays.copyOfRange(deque, bot, top);
        return sub;
//        return new ArrayList<Integer>(Arrays.asList(sub));
//        return new ArrayList(deque);
    }
}