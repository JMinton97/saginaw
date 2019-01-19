package project.kdtree;

import java.io.Serializable;
import java.util.Arrays;

public class Tree implements Serializable{
    private Node root;
    private double[] nearestPoint;
    private long nearestId;
    private double minDist;

    public void insert(long id, double[] addPoint){
        if(root == null){
            root = new Node(id, addPoint);
        } else {
            add(addPoint, id, root, true);
        }
    }

//    public void left(){
//        System.out.println(root.getLeft().getPoint()[0]);
//    }


    private void add(double[] addPoint, long id, Node point, boolean vertical){
        if(vertical){
            if(addPoint[1] >= point.getPoint()[1]){
                if(point.getLeft() == null){
                    point.setLeft(new Node(id, addPoint));
                } else {
                    add(addPoint, id, point.getLeft(), false);
                }
            } else {
                if(point.getRight() == null){
                    point.setRight(new Node(id, addPoint));
                } else {
                    add(addPoint, id, point.getRight(), false);
                }
            }
        } else {
            if(addPoint[0] >= point.getPoint()[0]){
                if(point.getLeft() == null){
                    point.setLeft(new Node(id, addPoint));
                } else {
                    add(addPoint, id, point.getLeft(), true);
                }
            } else {
                if(point.getRight() == null){
                    point.setRight(new Node(id, addPoint));
                } else {
                    add(addPoint, id, point.getRight(), true);
                }
            }
        }
    }

    public boolean contains(double[] point){
        if(root == null){
            return false;
        } else {
            return search(root, point, true);
        }
    }

    private boolean search(Node node, double[] point, boolean vertical){
        if(node == null){
            return false;
        } else {
//            System.out.println(node.getPoint()[0] + " " + node.getPoint()[1]);
            if(vertical){
                if(Arrays.equals(node.getPoint(), point)){
                    return true;
                }
                if(point[1] >= node.getPoint()[1]){
                    return search(node.getLeft(), point, false);
                } else {
                    return search(node.getRight(), point, false);
                }
            } else {
                if(Arrays.equals(node.getPoint(), point)){
                    return true;
                }
                if(point[0] >= node.getPoint()[0]){
                    return search(node.getLeft(), point, true);
                } else {
                    return search(node.getRight(), point, true);
                }
            }

        }
    }

    public long nearest(double[] point){
        nearestId = 0;
        if(root == null){
            return -1;                      //reserved?
        } else {
            minDist = Double.MAX_VALUE;
            find(point, root, true);
            return nearestId;
        }
    }

    public void find(double[] p, Node node, boolean vertical){
        if(node != null){
//            System.out.println("Trying " + node.getPoint()[0] + "," + node.getPoint()[1]);
            if(node.isLeaf()){
//                System.out.println("true");
                double d = distance(p, node.getPoint());
//                System.out.println(d);
                if (minDist > d) {
//                    System.out.println("yeah!");
                    minDist = d;
                    nearestPoint = node.getPoint();
                    nearestId = node.getId();
                }
            } else {
                int x;
                if (vertical) {
                    x = 1;
                } else {
                    x = 0;
                }
                if (p[x] >= node.getPoint()[x]) { //search left
                    find(p, node.getLeft(), !vertical);
                    double d = distance(p, node.getPoint());
//                    System.out.println(d);
                    if (minDist > d) {
//                        System.out.println("yeah!");
                        minDist = d;
                        nearestPoint = node.getPoint();
                        nearestId = node.getId();
                    }
                    if (p[x] - minDist >= node.getPoint()[x]) {
                        find(p, node.getRight(), !vertical);
                    }
                } else {
                    find(p, node.getRight(), !vertical);
                    double d = distance(p, node.getPoint());
//                    System.out.println(d);
                    if (minDist > d) {
//                        System.out.println("yeah!");
                        minDist = d;
                        nearestPoint = node.getPoint();
                        nearestId = node.getId();
                    }
                    if (p[x] + minDist < node.getPoint()[x]) {
                        find(p, node.getRight(), !vertical);
                    }
                }
            }
        }
    }


    public double distance(double[] a, double[] b){
        return (((b[0] - a[0]) * (b[0] - a[0])) + ((b[1] - a[1]) * (b[1] - a[1])));
    }

    public void print(){
        print(root);
    }

    private void print(Node node){
        if(node != null){
            System.out.println(node.getPoint()[0] + " " + node.getPoint()[1]);
            print(node.getLeft());
            print(node.getRight());
        } else {
            System.out.println("0");
        }
    }
}