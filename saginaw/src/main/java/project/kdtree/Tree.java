package project.kdtree;

import javafx.util.Pair;
import project.map.Graph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class Tree implements Serializable{
    private Node root;
    private double[] nearestPoint;
    private int nearestId;
    private double minDist;
    private double maxDepth;

    public void insert(int id, double[] addPoint){
        if(root == null){
            root = new Node(id, addPoint, 0, false);
        } else {
            add(addPoint, id, root, true);
        }
    }

//    public void left(){
//        System.out.println(root.getLeft().getPoint()[0]);
//    }

    public Tree(int maxDepth){
        this.maxDepth = maxDepth;
    }


    private void add(double[] addPoint, int id, Node point, boolean vertical){
        if(point.isLeaf()){
            point.setId(id);
        }
        if(vertical){
            if(addPoint[1] >= point.getPoint()[1]){
                if(point.getLeft() == null){
                    point.setLeft(new Node(id, addPoint, point.getDepth() + 1, point.getDepth() >= maxDepth));
                } else {
                    add(addPoint, id, point.getLeft(), false);
                }
            } else {
                if(point.getRight() == null){
                    point.setRight(new Node(id, addPoint, point.getDepth() + 1, point.getDepth() >= maxDepth));
                } else {
                    add(addPoint, id, point.getRight(), false);
                }
            }
        } else {
            if(addPoint[0] >= point.getPoint()[0]){
                if(point.getLeft() == null){
                    point.setLeft(new Node(id, addPoint, point.getDepth() + 1, point.getDepth() >= maxDepth));
                } else {
                    add(addPoint, id, point.getLeft(), true);
                }
            } else {
                if(point.getRight() == null){
                    point.setRight(new Node(id, addPoint, point.getDepth() + 1, point.getDepth() >= maxDepth));
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

    public Pair<Integer, Double> nearest(double[] point, ArrayList<double[]> dictionary){
//        System.out.println("Nearest to " + point[0] + " " + point[1]);
        nearestId = 0;
        if(root == null){
            return new Pair<>(-1, null);     //reserved?
        } else {
            minDist = Double.MAX_VALUE;
            find(point, root, true, dictionary);
            return new Pair<>(nearestId, minDist);
        }
    }

    public void find(double[] p, Node node, boolean vertical, ArrayList<double[]> dictionary){
        if(node != null){
//            System.out.println("Trying " + node.getPoint()[0] + "," + node.getPoint()[1]);
            if(node.isLeaf()){
//                System.out.println("LEAF SIZE: " + node.getIds().size());
                Pair<Integer, Double> leafClosest = node.findClosest(p, dictionary);
//                System.out.println(d);
                if (minDist > leafClosest.getValue()) {
//                    System.out.println("yeah!");
                    minDist = leafClosest.getValue();
//                    nearestPoint = node.getPoint();
                    nearestId = leafClosest.getKey();
                }
            } else {
                int x;
                if (vertical) {
                    x = 1;
                } else {
                    x = 0;
                }
                if (p[x] >= node.getPoint()[x]) { //search left
                    find(p, node.getLeft(), !vertical, dictionary);
                    double d = Graph.haversineDistance(p, node.getPoint());
//                    System.out.println(d);
                    if (minDist > d) {
//                        System.out.println("yeah!");
                        minDist = d;
                        nearestPoint = node.getPoint();
                        nearestId = node.getId();
                    }
                    if (p[x] - minDist >= node.getPoint()[x]) {
//                        System.out.println("hmmm");
                        find(p, node.getRight(), !vertical, dictionary);
                    }

                } else {
                    find(p, node.getRight(), !vertical, dictionary);
                    double d = Graph.haversineDistance(p, node.getPoint());
//                    System.out.println(d);
                    if (minDist > d) {
//                        System.out.println("yeah!");
                        minDist = d;
                        nearestPoint = node.getPoint();
                        nearestId = node.getId();
                    }
                    if (p[x] + minDist < node.getPoint()[x]) {
//                        System.out.println("hmmm");
                        find(p, node.getRight(), !vertical, dictionary);
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

    public int size(){
        if(root != null){
            return root.size();
        } else {
            return 0;
        }
    }
}