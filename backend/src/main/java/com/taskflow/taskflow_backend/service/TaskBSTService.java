package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.entity.Task;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service implementing a Binary Search Tree (BST)
 * to organize tasks by Task ID for O(log n) search and Inorder traversal.
 */
@Service
public class TaskBSTService {

    // Node representing one task in the BST
    public static class BSTNode {
        public Task task;
        public BSTNode left;
        public BSTNode right;

        public BSTNode(Task task) {
            this.task = task;
            this.left = null;
            this.right = null;
        }
    }

    /**
     * Builds a BST from a list of tasks.
     */
    public BSTNode buildTree(List<Task> tasks) {
        BSTNode root = null;
        if (tasks != null) {
            for (Task task : tasks) {
                root = insert(root, task);
            }
        }
        return root;
    }

    /**
     * Inserts a task into the BST based on its Task ID.
     */
    public BSTNode insert(BSTNode root, Task task) {
        if (task == null || task.getId() == null) {
            return root;
        }

        if (root == null) {
            return new BSTNode(task);
        }

        if (task.getId() < root.task.getId()) {
            root.left = insert(root.left, task);
        } else if (task.getId() > root.task.getId()) {
            root.right = insert(root.right, task);
        }

        return root;
    }

    /**
     * Searches the BST for a task with the given ID.
     * Returns a Map containing the task (if found), the traversal path, and comparison count.
     */
    public Map<String, Object> searchWithTrace(List<Task> tasks, Integer targetId) {
        Map<String, Object> response = new HashMap<>();
        List<String> path = new ArrayList<>();

        if (targetId == null) {
            response.put("found", false);
            response.put("message", "Please provide a valid Task ID.");
            response.put("path", path);
            response.put("comparisons", 0);
            return response;
        }

        BSTNode root = buildTree(tasks);
        BSTNode current = root;
        int comparisons = 0;
        Task foundTask = null;

        while (current != null) {
            comparisons++;
            int currentId = current.task.getId();

            if (targetId.equals(currentId)) {
                path.add("Visited Node (ID: " + currentId + ") -> Match Found!");
                foundTask = current.task;
                break;
            } else if (targetId < currentId) {
                path.add("Visited Node (ID: " + currentId + ") -> Target " + targetId + " is smaller, went LEFT");
                current = current.left;
            } else {
                path.add("Visited Node (ID: " + currentId + ") -> Target " + targetId + " is larger, went RIGHT");
                current = current.right;
            }
        }

        if (foundTask != null) {
            response.put("found", true);
            response.put("task", foundTask);
            response.put("message", "Task found in BST: " + foundTask.getName());
        } else {
            response.put("found", false);
            response.put("message", "Task ID " + targetId + " not found in BST.");
            path.add("Reached null branch -> Task ID " + targetId + " does not exist in the BST.");
        }

        response.put("path", path);
        response.put("comparisons", comparisons);
        return response;
    }

    /**
     * Inorder Traversal (Left -> Root -> Right).
     * Returns tasks sorted in ascending order of Task ID.
     */
    public List<Task> getInorderTraversal(List<Task> tasks) {
        BSTNode root = buildTree(tasks);
        List<Task> result = new ArrayList<>();
        inorder(root, result);
        return result;
    }

    private void inorder(BSTNode node, List<Task> list) {
        if (node != null) {
            inorder(node.left, list);
            list.add(node.task);
            inorder(node.right, list);
        }
    }

    /**
     * Preorder Traversal (Root -> Left -> Right).
     * Visits root first, then left subtree, then right subtree.
     */
    public List<Task> getPreorderTraversal(List<Task> tasks) {
        BSTNode root = buildTree(tasks);
        List<Task> result = new ArrayList<>();
        preorder(root, result);
        return result;
    }

    private void preorder(BSTNode node, List<Task> list) {
        if (node != null) {
            list.add(node.task);
            preorder(node.left, list);
            preorder(node.right, list);
        }
    }

    /**
     * Postorder Traversal (Left -> Right -> Root).
     * Visits left subtree first, then right subtree, and root last.
     */
    public List<Task> getPostorderTraversal(List<Task> tasks) {
        BSTNode root = buildTree(tasks);
        List<Task> result = new ArrayList<>();
        postorder(root, result);
        return result;
    }

    private void postorder(BSTNode node, List<Task> list) {
        if (node != null) {
            postorder(node.left, list);
            postorder(node.right, list);
            list.add(node.task);
        }
    }
}
