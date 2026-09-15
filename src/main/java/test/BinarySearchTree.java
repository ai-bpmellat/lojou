package com.aiagent.plugin.test;
/**
 * Implements a Binary Search Tree (BST) structure.
 * BSTs store data in a sorted manner, allowing for efficient search, insertion, and deletion operations.
 */
public class BinarySearchTree {

    private Node root;

    // Inner static class representing a node in the BST
    private static class Node {
        int key;
        Node left;
        Node right;

        public Node(int key) {
            this.key = key;
            this.left = null;
            this.right = null;
        }
    }

    public BinarySearchTree() {
        this.root = null;
    }

    /**
     * Inserts a new key into the BST.
     * @param key The integer value to insert.
     */
    public void insert(int key) {
        root = insertRec(root, key);
    }

    // Recursive helper function for insertion
    private Node insertRec(Node root, int key) {
        if (root == null) {
            return new Node(key);
        }
        
        if (key < root.key) {
            root.left = insertRec(root.left, key);
        } else if (key > root.key) {
            root.right = insertRec(root.right, key);
        } 
        // If key == root.key, we ignore it or handle duplicates as required.

        return root;
    }

    /**
     * Searches for a key in the BST.
     * @param key The integer value to search for.
     * @return true if the key is found, false otherwise.
     */
    public boolean search(int key) {
        return searchRec(root, key);
    }

    // Recursive helper function for searching
    private boolean searchRec(Node root, int key) {
        if (root == null || root.key == key) {
            return root != null;
        }
        
        if (key < root.key) {
            return searchRec(root.left, key);
        } else {
            return searchRec(root.right, key);
        }
    }

    /**
     * Performs an in-order traversal of the BST (prints keys in sorted order).
     */
    public void inorderTraversal() {
        System.out.print("Inorder Traversal: ");
        inorderRec(root);
        System.out.println();
    }

    // Recursive helper function for traversal
    private void inorderRec(Node root) {
        if (root != null) {
            inorderRec(root.left);
            System.out.print(root.key + " ");
            inorderRec(root.right);
        }
    }

    public static void main(String[] args) {
        BinarySearchTree tree = new BinarySearchTree();
        int[] keys = {50, 30, 70, 20, 40, 60, 80};

        // Insert elements
        for (int key : keys) {
            tree.insert(key);
        }

        // Test functionality
        System.out.println("BST created and populated.");
        tree.inorderTraversal(); // Should print sorted list: 20 30 40 50 60 70 80 

        // Test search
        int searchKey1 = 40;
        int searchKey2 = 99;
        System.out.println("Searching for " + searchKey1 + ": " + (tree.search(searchKey1) ? "Found" : "Not Found"));
        System.out.println("Searching for " + searchKey2 + ": " + (tree.search(searchKey2) ? "Found" : "Not Found"));
    }
}