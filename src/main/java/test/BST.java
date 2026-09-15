package test;

/**
 * Represents a Binary Search Tree (BST).
 * This implementation uses generics to allow it to store any comparable type.
 */
public class BST<T extends Comparable<T>> {

    // Inner class representing a node in the tree
    private static class Node<T> {
        T data;
        Node<T> left;
        Node<T> right;

        public Node(T data) {
            this.data = data;
            this.left = null;
            this.right = null;
        }
    }

    private Node۳<T> root;

    /**
     * Constructor for BST.
     */
    public BST() {
        this.root = null;
    }

    /**
     * Inserts a new value into the BST.
     * @param data The element to insert.
     */
    public void insert(T data) {
        if (data == null) return;
        root = insertRec(root, data);
    }

    /**
     * Recursive helper function for insertion.
     */
    private Node<T> insertRec(Node<T> root, T data) {
        // If the tree is empty, return a new node
        if (root == null) {
            return new Node<>(data);
        }

        int comparison = data.compareTo(root.data);

        if (comparison < 0) { // Data is smaller than root's data -> go left
            root.left = insertRec(root.left, data);
        } else if (comparison > 0) { // Data is larger than root's data -> go right
            root.right = insertRec(root.right, data);
        } 
        // If comparison == 0, the element already exists, so we do nothing.

        return root;
    }

    /**
     * Searches for a value in the BST.
     * @param data The element to search for.
     * @return true if the element is found, false otherwise.
     */
    public boolean search(T data) {
        if (data == null) return false;
        return searchRec(root, data);
    }

    /**
     * Recursive helper function for searching.
     */
    private boolean searchRec(Node<T> root, T data) {
        // Base case: Tree is empty or element not found
        if (root == null) { 
            return false;
        }

        int comparison = data.compareTo(root.data);

        if (comparison < 0) { // Search in the left subtree
            return searchRec(root.left, data);
        } else if (comparison > 0) { // Search in the right subtree
            return searchRec(root.right, data);
        } else { // Element found
            return true;
        }
    }

    /**
     * Performs an In-order traversal of the BST and prints elements.
     * (This is useful for printing sorted order)
     */
    public void inOrderTraversal() {
        System.out.print("In-Order Traversal: ");
        inOrderRec(root);
        System.out.println();
    }

    private void inOrderRec(Node<T> root) {
        if (root != null) {
            inOrderRec(root.left);
            System.out.print(root.data + " "); // Print current node
            inOrderRec(root.right);
        }
    }
    
    // --- Example usage (main method for testing) ---
    public static void main(String[] args) {
        BST<Integer> bst = new BST<>();
        
        // Test insertion
        bst.insert(50);
        bst.insert(30);
        bst.insert(70);
        bst.insert(20);
        bst.insert(40);
        bst.insert(60);
        bst.insert(80);

        // Test traversal (should be sorted)
        bst.inOrderTraversal(); // Expected: 20 30 40 50 60 70 80 
        
        System.out.println("\n--- Search Tests ---");
        // Test search found
        Integer searchFound = 40;
        System.out.println("Is " + searchFound + " present? " + bst.search(searchFound)); // Expected: true

        // Test search not found
        Integer searchNotFound = 99;
        System.out.println("Is " + searchNotFound + " present? " + bst.search(searchNotFound)); // Expected: false
    }
}