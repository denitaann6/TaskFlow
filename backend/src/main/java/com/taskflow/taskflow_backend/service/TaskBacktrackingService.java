package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.dto.BacktrackingResult;
import com.taskflow.taskflow_backend.entity.Task;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service implementing Phase 4 Backtracking:
 * Dependency constraint planning that explores valid task execution orders,
 * prunes invalid branches, and detects cycles.
 */
@Service
public class TaskBacktrackingService {

    public static final String DEPENDENCY_EXPLANATION =
            "Dependency Rule: A directed edge (A -> B) defines Task A as a PREREQUISITE for Task B. " +
            "Therefore, Task A must finish BEFORE Task B can begin. In every valid output schedule, " +
            "Task A must appear at a lower index than Task B (index(A) < index(B)).";

    public static final String ALGORITHM_EXPLANATION =
            "State Space Tree Backtracking: Explores possible linear execution permutations level-by-level. " +
            "A candidate task is placed at the current position only if ALL of its prerequisites have already " +
            "been completed in the prefix sequence. If an unmet prerequisite is encountered, the algorithm " +
            "immediately prunes the branch and backtracks to try alternative valid branches.";

    public static final int DEFAULT_MAX_SCHEDULES = 10;
    public static final int ABSOLUTE_MAX_SCHEDULES = 50;

    /**
     * Finds valid execution schedules using database tasks and the active dependency graph.
     */
    public BacktrackingResult planSchedules(List<Task> allTasks, Map<Integer, List<Integer>> graph, int maxSchedules) {
        List<Integer> taskIds = new ArrayList<>();
        Map<Integer, String> names = new HashMap<>();

        if (allTasks != null) {
            for (Task t : allTasks) {
                if (t.getId() != null) {
                    taskIds.add(t.getId());
                    names.put(t.getId(), t.getName() != null ? t.getName() : "Task " + t.getId());
                }
            }
        }

        return executeBacktracking(taskIds, graph, names, maxSchedules);
    }

    /**
     * Finds valid execution schedules for arbitrary task IDs and graph (useful for deterministic tests).
     */
    public BacktrackingResult planCustomSchedules(List<Integer> taskIds, Map<Integer, List<Integer>> graph,
                                                  Map<Integer, String> customNames, int maxSchedules) {
        return executeBacktracking(taskIds, graph, customNames != null ? customNames : new HashMap<>(), maxSchedules);
    }

    private BacktrackingResult executeBacktracking(List<Integer> taskIds, Map<Integer, List<Integer>> rawGraph,
                                                   Map<Integer, String> taskNames, int maxSchedules) {
        BacktrackingResult result = new BacktrackingResult();
        result.setDependencyDirectionExplanation(DEPENDENCY_EXPLANATION);
        result.setAlgorithmExplanation(ALGORITHM_EXPLANATION);

        int limit = maxSchedules <= 0 ? DEFAULT_MAX_SCHEDULES : Math.min(maxSchedules, ABSOLUTE_MAX_SCHEDULES);
        result.setMaxSchedulesRequested(limit);

        if (taskIds == null || taskIds.isEmpty()) {
            result.setTotalTasks(0);
            result.setSchedulesFound(0);
            result.setCycleDetected(false);
            result.setBranchesPruned(0);
            result.setValidOrderings(new ArrayList<>());
            return result;
        }

        // De-duplicate task IDs preserving order
        List<Integer> uniqueTaskIds = new ArrayList<>(new LinkedHashSet<>(taskIds));
        Set<Integer> taskSet = new HashSet<>(uniqueTaskIds);
        result.setTotalTasks(uniqueTaskIds.size());

        // Fill task names fallback if missing
        Map<Integer, String> resolvedNames = new HashMap<>(taskNames);
        for (Integer id : uniqueTaskIds) {
            resolvedNames.putIfAbsent(id, "Task " + id);
        }
        result.setTaskNames(resolvedNames);

        // Sanitize graph: only keep edges between tasks present in taskSet
        Map<Integer, List<Integer>> sanitizedGraph = new HashMap<>();
        Map<Integer, Set<Integer>> prerequisitesOf = new HashMap<>();
        for (Integer id : uniqueTaskIds) {
            prerequisitesOf.put(id, new HashSet<>());
            sanitizedGraph.put(id, new ArrayList<>());
        }

        if (rawGraph != null) {
            for (Map.Entry<Integer, List<Integer>> entry : rawGraph.entrySet()) {
                Integer prereq = entry.getKey();
                if (taskSet.contains(prereq) && entry.getValue() != null) {
                    for (Integer dependent : entry.getValue()) {
                        if (taskSet.contains(dependent) && !prereq.equals(dependent)) {
                            sanitizedGraph.get(prereq).add(dependent);
                            prerequisitesOf.get(dependent).add(prereq);
                        }
                    }
                }
            }
        }
        result.setDependencyGraph(sanitizedGraph);

        // Check for circular dependency using Kahn's in-degree algorithm first
        boolean hasCycle = detectCycle(uniqueTaskIds, prerequisitesOf, sanitizedGraph);
        if (hasCycle) {
            result.setCycleDetected(true);
            result.setSchedulesFound(0);
            result.setBranchesPruned(0);
            result.setValidOrderings(new ArrayList<>());
            result.setCycleMessage("Dependency Conflict Detected: The dependency graph contains a circular cycle. " +
                    "A valid topological execution ordering is mathematically impossible until the cycle is resolved.");
            return result;
        }

        result.setCycleDetected(false);

        // Perform backtracking state space search
        List<List<Integer>> solutions = new ArrayList<>();
        int[] prunedCount = new int[1];
        Set<Integer> completed = new HashSet<>();
        List<Integer> currentOrder = new ArrayList<>();

        backtrack(uniqueTaskIds, prerequisitesOf, currentOrder, completed, solutions, prunedCount, limit);

        result.setValidOrderings(solutions);
        result.setSchedulesFound(solutions.size());
        result.setBranchesPruned(prunedCount[0]);

        if (solutions.isEmpty()) {
            result.setCycleMessage("No valid schedule found due to constraint deadlocks.");
        }

        return result;
    }

    private void backtrack(List<Integer> remainingTasks,
                           Map<Integer, Set<Integer>> prerequisitesOf,
                           List<Integer> currentOrder,
                           Set<Integer> completed,
                           List<List<Integer>> solutions,
                           int[] prunedCount,
                           int maxLimit) {

        if (solutions.size() >= maxLimit) {
            return;
        }

        if (remainingTasks.isEmpty()) {
            solutions.add(new ArrayList<>(currentOrder));
            return;
        }

        for (int i = 0; i < remainingTasks.size(); i++) {
            Integer candidate = remainingTasks.get(i);

            // Bounding function: Check if all prerequisites of candidate are already completed
            Set<Integer> requiredPrereqs = prerequisitesOf.getOrDefault(candidate, Collections.emptySet());
            if (!completed.containsAll(requiredPrereqs)) {
                // Prune branch because dependency constraint is violated
                prunedCount[0]++;
                continue;
            }

            // Choose candidate
            completed.add(candidate);
            currentOrder.add(candidate);

            List<Integer> nextRemaining = new ArrayList<>(remainingTasks);
            nextRemaining.remove(i);

            // Explore
            backtrack(nextRemaining, prerequisitesOf, currentOrder, completed, solutions, prunedCount, maxLimit);

            // Backtrack
            currentOrder.remove(currentOrder.size() - 1);
            completed.remove(candidate);

            if (solutions.size() >= maxLimit) {
                break;
            }
        }
    }

    private boolean detectCycle(List<Integer> nodes, Map<Integer, Set<Integer>> prerequisitesOf,
                                Map<Integer, List<Integer>> adj) {
        Map<Integer, Integer> inDegree = new HashMap<>();
        for (Integer node : nodes) {
            inDegree.put(node, prerequisitesOf.getOrDefault(node, Collections.emptySet()).size());
        }

        Queue<Integer> queue = new LinkedList<>();
        for (Integer node : nodes) {
            if (inDegree.get(node) == 0) {
                queue.offer(node);
            }
        }

        int visitedCount = 0;
        while (!queue.isEmpty()) {
            int current = queue.poll();
            visitedCount++;

            List<Integer> dependents = adj.getOrDefault(current, Collections.emptyList());
            for (Integer dep : dependents) {
                int newDegree = inDegree.get(dep) - 1;
                inDegree.put(dep, newDegree);
                if (newDegree == 0) {
                    queue.offer(dep);
                }
            }
        }

        // If visitedCount != total nodes, there is a cycle!
        return visitedCount != nodes.size();
    }
}
