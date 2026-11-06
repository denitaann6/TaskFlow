import java.util.*;

public class TaskGraph {

    // Adjacency List
    // Task ID -> Tasks that depend on it
    private Map<Integer, List<Integer>> adjList;

    // Constructor
    public TaskGraph() {
        adjList = new HashMap<>();
    }

    // ==========================================
    // ADD TASK / NODE
    // ==========================================

    public void addTask(int taskId) {

        if (!adjList.containsKey(taskId)) {
            adjList.put(taskId, new ArrayList<>());
            System.out.println("Task " + taskId + " added to graph.");
        } else {
            System.out.println("Task " + taskId + " already exists in graph.");
        }
    }

    // ==========================================
    // ADD DEPENDENCY / EDGE
    // ==========================================

    public void addDependency(int u, int v) {

        // Make sure both tasks exist
        adjList.putIfAbsent(u, new ArrayList<>());
        adjList.putIfAbsent(v, new ArrayList<>());

        // Add dependency
        adjList.get(u).add(v);

        System.out.println(
            "Dependency set: Task " + u +
            " must be done before Task " + v
        );
    }

    // ==========================================
    // DISPLAY GRAPH
    // ==========================================

    public void displayGraph() {

        if (adjList.isEmpty()) {
            System.out.println("Graph is empty.");
            return;
        }

        System.out.println("\n--- TASK DEPENDENCY GRAPH ---");

        for (Map.Entry<Integer, List<Integer>> entry : adjList.entrySet()) {

            System.out.print("Task " + entry.getKey() + " -> ");

            if (entry.getValue().isEmpty()) {
                System.out.println("No dependencies");
            } else {
                for (int taskId : entry.getValue()) {
                    System.out.print(taskId + " ");
                }
                System.out.println();
            }
        }
    }

    // ==========================================
    // BFS TRAVERSAL
    // ==========================================

    public void bfsTraversal(int startId) {

        if (!adjList.containsKey(startId)) {
            System.out.println(
                "Start task " + startId +
                " not found in graph."
            );
            return;
        }

        System.out.println(
            "BFS Traversal (Task Dependency Flow):"
        );

        Queue<Integer> queue = new LinkedList<>();

        Set<Integer> visited = new HashSet<>();

        // Start from selected task
        queue.add(startId);
        visited.add(startId);

        while (!queue.isEmpty()) {

            int current = queue.poll();

            System.out.print(current + " -> ");

            for (int neighbor : adjList.get(current)) {

                if (!visited.contains(neighbor)) {

                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        System.out.println("END");
    }
}