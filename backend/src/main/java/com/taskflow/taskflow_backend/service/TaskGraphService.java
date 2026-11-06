package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.entity.TaskDependency;
import com.taskflow.taskflow_backend.repository.TaskDependencyRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service implementing a Directed Graph using an Adjacency List
 * to represent task dependencies and compute execution flow using BFS.
 * Supports persistent storage via MySQL task_dependencies table.
 */
@Service
public class TaskGraphService {

    // Adjacency List: Task ID -> List of Dependent Task IDs
    private final Map<Integer, List<Integer>> adjList = new HashMap<>();
    private final TaskDependencyRepository dependencyRepository;

    public TaskGraphService() {
        this.dependencyRepository = null;
    }

    @Autowired
    public TaskGraphService(TaskDependencyRepository dependencyRepository) {
        this.dependencyRepository = dependencyRepository;
    }

    @PostConstruct
    public synchronized void init() {
        if (dependencyRepository != null) {
            adjList.clear();
            List<TaskDependency> dependencies = dependencyRepository.findAllByOrderByPrerequisiteIdAscDependentIdAsc();
            for (TaskDependency dep : dependencies) {
                adjList.putIfAbsent(dep.getPrerequisiteId(), new ArrayList<>());
                adjList.putIfAbsent(dep.getDependentId(), new ArrayList<>());
                List<Integer> dependents = adjList.get(dep.getPrerequisiteId());
                if (!dependents.contains(dep.getDependentId())) {
                    dependents.add(dep.getDependentId());
                }
            }
        }
    }

    /**
     * Ensures a task ID exists as a node in the graph.
     */
    public synchronized void ensureNode(int taskId) {
        adjList.putIfAbsent(taskId, new ArrayList<>());
    }

    /**
     * Adds a directed dependency: prerequisiteId must be completed before dependentId.
     * Prevents self-loops and duplicate edges.
     */
    public synchronized boolean addDependency(int prerequisiteId, int dependentId) {
        if (prerequisiteId == dependentId) {
            return false; // Prevent self-loop
        }

        adjList.putIfAbsent(prerequisiteId, new ArrayList<>());
        adjList.putIfAbsent(dependentId, new ArrayList<>());

        List<Integer> dependents = adjList.get(prerequisiteId);
        if (dependents.contains(dependentId)) {
            return false; // Prevent duplicate edge
        }

        if (dependencyRepository != null) {
            if (!dependencyRepository.existsByPrerequisiteIdAndDependentId(prerequisiteId, dependentId)) {
                dependencyRepository.save(new TaskDependency(prerequisiteId, dependentId));
            }
        }

        dependents.add(dependentId);
        return true;
    }

    /**
     * Removes a single directed dependency edge (prerequisiteId -> dependentId).
     */
    public synchronized boolean removeDependency(int prerequisiteId, int dependentId) {
        if (dependencyRepository != null) {
            dependencyRepository.deleteDependencyEdge(prerequisiteId, dependentId);
        }
        if (adjList.containsKey(prerequisiteId)) {
            return adjList.get(prerequisiteId).remove(Integer.valueOf(dependentId));
        }
        return false;
    }

    /**
     * Returns a copy of the graph adjacency list.
     */
    public synchronized Map<Integer, List<Integer>> getGraph() {
        Map<Integer, List<Integer>> copy = new HashMap<>();
        for (Map.Entry<Integer, List<Integer>> entry : adjList.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    /**
     * Performs Breadth-First Search (BFS) to determine the execution order
     * of dependent tasks starting from a given task.
     */
    public synchronized List<Integer> bfsTraversal(int startTaskId) {
        List<Integer> executionOrder = new ArrayList<>();

        if (!adjList.containsKey(startTaskId)) {
            return executionOrder;
        }

        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();

        queue.offer(startTaskId);
        visited.add(startTaskId);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            executionOrder.add(current);

            List<Integer> neighbors = adjList.getOrDefault(current, Collections.emptyList());
            for (int neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.offer(neighbor);
                }
            }
        }

        return executionOrder;
    }

    /**
     * Cleans up node from the graph when a task is deleted.
     */
    public synchronized void removeTask(int taskId) {
        if (dependencyRepository != null) {
            dependencyRepository.deleteByTaskId(taskId);
        }
        adjList.remove(taskId);
        for (List<Integer> neighbors : adjList.values()) {
            neighbors.remove(Integer.valueOf(taskId));
        }
    }

    /**
     * Clears all dependencies.
     */
    public synchronized void clear() {
        if (dependencyRepository != null) {
            dependencyRepository.deleteAll();
        }
        adjList.clear();
    }
}
