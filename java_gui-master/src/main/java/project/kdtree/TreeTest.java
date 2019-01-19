package project.kdtree;

public class TreeTest {
    public static void main(String[] args) {
        Tree tree = new Tree();
        tree.insert(74, new double[]{5, 6});
        tree.insert(63, new double[]{7, 4});
        tree.insert(65, new double[]{8, 1});
        tree.insert(66, new double[]{4, 3});

        System.out.println(tree.nearest(new double[]{-5, -5}));

//        tree.print();

//        System.out.println(tree.contains(new double[]{4, 3}));
    }
}
