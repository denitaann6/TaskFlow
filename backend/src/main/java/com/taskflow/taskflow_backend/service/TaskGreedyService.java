package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.dto.GreedyResult;
import com.taskflow.taskflow_backend.dto.GreedyTaskItem;
import com.taskflow.taskflow_backend.entity.Task;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service implementing the Phase 4 Greedy Algorithm:
 * Priority-based pending task selection within an allocated time limit.
 */
@Service
public class TaskGreedyService {

    public static final String HEURISTIC_EXPLANATION =
            "The Greedy Algorithm computes a priority-to-effort density ratio (Priority Weight / Sample Effort Hours) " +
            "for each pending task. Tasks are sorted in descending order of this ratio, and the algorithm greedily commits " +
            "to the highest-density available task that fits within the remaining time limit.";

    public static final String OPTIMALITY_DISCLAIMER =
            "Note on Optimality: The greedy choice heuristic makes locally optimal decisions at each step. " +
            "While computationally efficient (O(N log N)), this heuristic does not guarantee the global optimum " +
            "in all 0/1 integer task selection scenarios because it cannot fractionally divide tasks to fill leftover time.";

    /**
     * Selects pending tasks from database entities using standard priority-derived sample metrics.
     */
    public GreedyResult selectPendingTasksGreedy(List<Task> allTasks, int timeLimitHours) {
        List<GreedyTaskItem> candidateItems = new ArrayList<>();

        if (allTasks != null) {
            for (Task task : allTasks) {
                // Focus on pending or in-progress tasks
                if (task.getStatus() == null || !"Completed".equalsIgnoreCase(task.getStatus())) {
                    candidateItems.add(convertToGreedyItem(task));
                }
            }
        }

        return executeGreedySelection(candidateItems, timeLimitHours);
    }

    /**
     * Selects tasks from custom task items (useful for unit testing and deterministic demonstrations).
     */
    public GreedyResult selectCustomTasksGreedy(List<GreedyTaskItem> customItems, int timeLimitHours) {
        List<GreedyTaskItem> candidates = customItems != null ? new ArrayList<>(customItems) : new ArrayList<>();
        return executeGreedySelection(candidates, timeLimitHours);
    }

    private GreedyResult executeGreedySelection(List<GreedyTaskItem> candidates, int timeLimitHours) {
        GreedyResult result = new GreedyResult();
        result.setTimeLimitHours(Math.max(0, timeLimitHours));
        result.setHeuristicExplanation(HEURISTIC_EXPLANATION);
        result.setOptimalityDisclaimer(OPTIMALITY_DISCLAIMER);

        if (timeLimitHours <= 0 || candidates.isEmpty()) {
            result.setTotalAllocatedHours(0);
            result.setRemainingHours(Math.max(0, timeLimitHours));
            result.setTotalPriorityScore(0);
            result.setCandidateTasks(candidates);
            result.setSelectedTasks(new ArrayList<>());
            if (timeLimitHours <= 0) {
                result.getDecisionTrace().add("Time limit is 0 or negative. No tasks could be allocated.");
            } else {
                result.getDecisionTrace().add("No candidate pending tasks available for selection.");
            }
            return result;
        }

        // Sort candidates by density ratio descending, tie-breaking by priority weight DESC, then taskId ASC
        List<GreedyTaskItem> sortedCandidates = new ArrayList<>(candidates);
        sortedCandidates.sort(
                Comparator.comparingDouble(GreedyTaskItem::getDensityRatio).reversed()
                        .thenComparing(Comparator.comparingInt(GreedyTaskItem::getPriorityWeight).reversed())
                        .thenComparing(Comparator.comparing(GreedyTaskItem::getTaskId, Comparator.nullsLast(Integer::compareTo)))
        );
        result.setCandidateTasks(sortedCandidates);

        int remaining = timeLimitHours;
        int totalScore = 0;
        int totalAllocated = 0;
        List<GreedyTaskItem> selected = new ArrayList<>();
        List<String> trace = new ArrayList<>();

        for (GreedyTaskItem item : sortedCandidates) {
            int effort = item.getSampleEffortHours();
            String label = "Task " + item.getTaskId() + " (\"" + item.getTaskName() + "\", " +
                    item.getPriority() + ", Ratio: " + String.format("%.2f", item.getDensityRatio()) + ")";

            if (effort <= remaining) {
                selected.add(item);
                totalAllocated += effort;
                remaining -= effort;
                totalScore += item.getPriorityWeight();
                trace.add("ACCEPTED " + label + ": Effort " + effort + "h fits in remaining " +
                        (remaining + effort) + "h. New remaining: " + remaining + "h, score: " + totalScore + ".");
            } else {
                trace.add("SKIPPED " + label + ": Effort " + effort + "h exceeds available remaining time of " +
                        remaining + "h.");
            }
        }

        result.setSelectedTasks(selected);
        result.setTotalAllocatedHours(totalAllocated);
        result.setRemainingHours(remaining);
        result.setTotalPriorityScore(totalScore);
        result.setDecisionTrace(trace);
        return result;
    }

    private GreedyTaskItem convertToGreedyItem(Task task) {
        String priority = task.getPriority() != null ? task.getPriority() : "Medium";
        int weight = getPriorityWeight(priority);
        int effort = getSampleEffort(priority);
        double ratio = (double) weight / effort;

        return new GreedyTaskItem(
                task.getId(),
                task.getName(),
                priority,
                task.getMember(),
                task.getStatus(),
                weight,
                effort,
                ratio
        );
    }

    public static int getPriorityWeight(String priority) {
        if ("High".equalsIgnoreCase(priority)) {
            return 30;
        } else if ("Low".equalsIgnoreCase(priority)) {
            return 10;
        }
        return 20; // Medium
    }

    public static int getSampleEffort(String priority) {
        if ("High".equalsIgnoreCase(priority)) {
            return 2; // High priority: 2h effort -> Ratio 15.0
        } else if ("Low".equalsIgnoreCase(priority)) {
            return 2; // Low priority: 2h effort -> Ratio 5.0
        }
        return 3; // Medium priority: 3h effort -> Ratio 6.67
    }
}
