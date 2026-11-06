package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.dto.DPResult;
import com.taskflow.taskflow_backend.dto.DPTaskItem;
import com.taskflow.taskflow_backend.entity.Task;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service implementing Phase 4 Dynamic Programming:
 * 0/1 Knapsack-style optimal task selection maximizing total sample benefit within a time limit.
 */
@Service
public class TaskDPService {

    public static final String SAMPLE_METRICS_EXPLANATION =
            "Demonstration Notice: Task durations (hours) and benefit values are sample demonstration metrics " +
            "derived deterministically from task priority (or provided test inputs), as the core schema does not store duration columns. " +
            "0/1 Knapsack Dynamic Programming constructs a 2D state table (DP[i][w]) to find the globally optimal task subset.";

    public static final String DP_RECURRENCE =
            "Recurrence Relation: DP[i][w] = max(DP[i-1][w], DP[i-1][w - duration[i-1]] + benefit[i-1]) when duration[i-1] <= w; " +
            "otherwise DP[i][w] = DP[i-1][w]. Backtracking from DP[N][timeLimit] reconstructs the optimal task set.";

    public static final int MAX_SAFE_TIME_LIMIT = 100;

    /**
     * Solves 0/1 Knapsack task allocation using database task records.
     */
    public DPResult allocateTasksDP(List<Task> allTasks, int timeLimitHours) {
        List<DPTaskItem> candidateItems = new ArrayList<>();

        if (allTasks != null) {
            for (Task task : allTasks) {
                candidateItems.add(convertToDPItem(task));
            }
        }

        return executeDPAllocation(candidateItems, timeLimitHours);
    }

    /**
     * Solves 0/1 Knapsack task allocation using custom task items for unit tests.
     */
    public DPResult allocateCustomTasksDP(List<DPTaskItem> customItems, int timeLimitHours) {
        List<DPTaskItem> candidates = customItems != null ? new ArrayList<>(customItems) : new ArrayList<>();
        return executeDPAllocation(candidates, timeLimitHours);
    }

    private DPResult executeDPAllocation(List<DPTaskItem> candidates, int rawTimeLimit) {
        DPResult result = new DPResult();
        int timeLimit = Math.max(0, Math.min(rawTimeLimit, MAX_SAFE_TIME_LIMIT));
        result.setTimeLimitHours(timeLimit);
        result.setCandidateTasks(candidates);
        result.setSampleMetricsExplanation(SAMPLE_METRICS_EXPLANATION);
        result.setDpRecurrence(DP_RECURRENCE);

        if (timeLimit <= 0 || candidates.isEmpty()) {
            result.setMaxBenefitAchieved(0);
            result.setTotalDurationUsed(0);
            result.setRemainingHours(timeLimit);
            result.setSelectedTasks(new ArrayList<>());
            if (timeLimit <= 0) {
                result.getDpSteps().add("Time limit is 0 or invalid. Optimal benefit is 0.");
            } else {
                result.getDpSteps().add("No candidate tasks available for allocation.");
            }
            return result;
        }

        int n = candidates.size();
        int[][] dp = new int[n + 1][timeLimit + 1];

        // Bottom-up Tabulation
        for (int i = 1; i <= n; i++) {
            DPTaskItem item = candidates.get(i - 1);
            int duration = Math.max(1, item.getSampleDurationHours());
            int benefit = Math.max(0, item.getSampleBenefitValue());

            for (int w = 0; w <= timeLimit; w++) {
                if (duration <= w) {
                    dp[i][w] = Math.max(dp[i - 1][w], dp[i - 1][w - duration] + benefit);
                } else {
                    dp[i][w] = dp[i - 1][w];
                }
            }
        }

        int maxBenefit = dp[n][timeLimit];
        result.setMaxBenefitAchieved(maxBenefit);

        // Backtrack to find exact tasks included in optimal solution
        List<DPTaskItem> selected = new ArrayList<>();
        int w = timeLimit;
        List<String> steps = new ArrayList<>();
        steps.add("Constructed DP table: " + (n + 1) + " task stages x " + (timeLimit + 1) + " time capacities.");

        for (int i = n; i > 0 && w > 0; i--) {
            if (dp[i][w] != dp[i - 1][w]) {
                DPTaskItem chosenItem = candidates.get(i - 1);
                selected.add(chosenItem);
                steps.add("Backtrack Step: Included Task " + chosenItem.getTaskId() + " (\"" + chosenItem.getTaskName() +
                        "\") -> Sample Duration: " + chosenItem.getSampleDurationHours() + "h, Sample Benefit: " +
                        chosenItem.getSampleBenefitValue() + " pts.");
                w -= Math.max(1, chosenItem.getSampleDurationHours());
            }
        }

        Collections.reverse(selected);
        result.setSelectedTasks(selected);

        int totalDurationUsed = timeLimit - w;
        result.setTotalDurationUsed(totalDurationUsed);
        result.setRemainingHours(w);

        steps.add("Optimal Allocation Complete: " + selected.size() + " tasks selected. Total Duration: " +
                totalDurationUsed + "h / " + timeLimit + "h limit. Total Sample Benefit: " + maxBenefit + " pts.");
        result.setDpSteps(steps);

        return result;
    }

    private DPTaskItem convertToDPItem(Task task) {
        String priority = task.getPriority() != null ? task.getPriority() : "Medium";
        int duration = getSampleDuration(priority);
        int benefit = getSampleBenefit(priority);

        return new DPTaskItem(
                task.getId(),
                task.getName(),
                priority,
                task.getMember(),
                task.getStatus(),
                duration,
                benefit
        );
    }

    public static int getSampleDuration(String priority) {
        if ("High".equalsIgnoreCase(priority)) {
            return 3; // 3 hours
        } else if ("Low".equalsIgnoreCase(priority)) {
            return 1; // 1 hour
        }
        return 2; // Medium: 2 hours
    }

    public static int getSampleBenefit(String priority) {
        if ("High".equalsIgnoreCase(priority)) {
            return 30; // 30 sample points
        } else if ("Low".equalsIgnoreCase(priority)) {
            return 10; // 10 sample points
        }
        return 18; // Medium: 18 sample points
    }
}
