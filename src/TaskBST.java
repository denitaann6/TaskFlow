// Binary Search Tree to organize tasks by Task ID
// Phase 2: Task Hierarchy

public class TaskBST {

    // Node represents one task in the BST
    class Node {
        Task task;
        Node left;
        Node right;

        public Node(Task task) {
            this.task = task;
            this.left = null;
            this.right = null;
        }
    }

    // Root of the Binary Search Tree
    private Node root;

    // =========================
    // INSERT TASK
    // =========================

    public void insert(Task task) {
        root = insertRecursive(root, task);
    }

    private Node insertRecursive(Node root, Task task) {

        // If tree is empty, create a new node
        if (root == null) {
            root = new Node(task);
            return root;
        }

        // Smaller Task ID goes to the left
        if (task.taskId < root.task.taskId) {
            root.left = insertRecursive(root.left, task);
        }

        // Larger Task ID goes to the right
        else if (task.taskId > root.task.taskId) {
            root.right = insertRecursive(root.right, task);
        }

        // If Task ID is same, do not insert duplicate
        else {
            System.out.println("Task ID " + task.taskId + " already exists.");
        }

        return root;
    }

    // =========================
    // SEARCH TASK
    // =========================

    public void search(int taskId) {

        Node result = searchRecursive(root, taskId);

        if (result != null) {
            System.out.println(
                "Task found: ID = " +
                result.task.taskId +
                ", Name = " +
                result.task.taskName
            );
        } else {
            System.out.println("Task ID " + taskId + " not found.");
        }
    }

    private Node searchRecursive(Node root, int taskId) {

        // Task not found
        if (root == null) {
            return null;
        }

        // Task found
        if (root.task.taskId == taskId) {
            return root;
        }

        // Search left side
        if (taskId < root.task.taskId) {
            return searchRecursive(root.left, taskId);
        }

        // Search right side
        return searchRecursive(root.right, taskId);
    }

    // =========================
    // INORDER TRAVERSAL
    // =========================

    public void displayInorder() {

        System.out.print("Inorder (Sorted ID): ");

        inorder(root);

        System.out.println();
    }

    private void inorder(Node root) {

        if (root != null) {

            // Left
            inorder(root.left);

            // Root
            System.out.print(root.task.taskId + " ");

            // Right
            inorder(root.right);
        }
    }

    // =========================
    // PREORDER TRAVERSAL
    // =========================

    public void displayPreorder() {

        System.out.print("Preorder: ");

        preorder(root);

        System.out.println();
    }

    private void preorder(Node root) {

        if (root != null) {

            // Root
            System.out.print(root.task.taskId + " ");

            // Left
            preorder(root.left);

            // Right
            preorder(root.right);
        }
    }

    // =========================
    // POSTORDER TRAVERSAL
    // =========================

    public void displayPostorder() {

        System.out.print("Postorder: ");

        postorder(root);

        System.out.println();
    }

    private void postorder(Node root) {

        if (root != null) {

            // Left
            postorder(root.left);

            // Right
            postorder(root.right);

            // Root
            System.out.print(root.task.taskId + " ");
        }
    }
}