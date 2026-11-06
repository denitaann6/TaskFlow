package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.dto.GreedyResult;
import com.taskflow.taskflow_backend.dto.GreedyTaskItem;
import com.taskflow.taskflow_backend.entity.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskGreedyServiceTest {

    private TaskGreedyService greedyService;

    @BeforeEach
    void setUp() {
        greedyService = new TaskGreedyService();
    }

    private Task createTask(int id, String name, String priority, String status) {
        Task t = new Task();
        t.setId(id);
        t.setName(name);
        t.setMember("Alex");
        t.setPriority(priority);
        t.setStatus(status);
        return t;
    }

    @Test
    void testStandardGreedySelectionWithDefaultMetrics() {
        List<Task> tasks = new ArrayList<>();
        // High: 30 pts, 2h -> ratio 15.0
        tasks.add(createTask(101, "Critical Bug", "High", "Pending"));
        // Medium: 20 pts, 3h -> ratio 6.67
        tasks.add(createTask(102, "API Refactor", "Medium", "Pending"));
        // Low: 10 pts, 2h -> ratio 5.0
        tasks.add(createTask(103, "Update Docs", "Low", "Pending"));

        // Capacity: 4 hours
        // Task 101 (2h) fits -> 2h remaining.
        // Task 102 (3h) does not fit into 2h.
        // Task 103 (2h) fits into 2h -> 0h remaining.
        GreedyResult result = greedyService.selectPendingTasksGreedy(tasks, 4);

        assertEquals(4, result.getTimeLimitHours());
        assertEquals(4, result.getTotalAllocatedHours());
        assertEquals(0, result.getRemainingHours());
        assertEquals(40, result.getTotalPriorityScore()); // 30 + 10 = 40
        assertEquals(2, result.getSelectedTasks().size());
        assertEquals(101, result.getSelectedTasks().get(0).getTaskId());
        assertEquals(103, result.getSelectedTasks().get(1).getTaskId());
        assertNotNull(result.getOptimalityDisclaimer());
    }

    @Test
    void testIgnoresCompletedTasks() {
        List<Task> tasks = new ArrayList<>();
        tasks.add(createTask(101, "Old Task", "High", "Completed"));
        tasks.add(createTask(102, "Active Task", "Medium", "Pending"));

        GreedyResult result = greedyService.selectPendingTasksGreedy(tasks, 5);
        assertEquals(1, result.getCandidateTasks().size());
        assertEquals(102, result.getCandidateTasks().get(0).getTaskId());
    }

    @Test
    void testEmptyAndNullInputs() {
        GreedyResult r1 = greedyService.selectPendingTasksGreedy(null, 8);
        assertEquals(0, r1.getSelectedTasks().size());
        assertEquals(0, r1.getTotalPriorityScore());

        GreedyResult r2 = greedyService.selectPendingTasksGreedy(new ArrayList<>(), 8);
        assertEquals(0, r2.getSelectedTasks().size());
    }

    @Test
    void testZeroAndNegativeTimeLimit() {
        List<Task> tasks = List.of(createTask(1, "Test", "High", "Pending"));

        GreedyResult r1 = greedyService.selectPendingTasksGreedy(tasks, 0);
        assertEquals(0, r1.getSelectedTasks().size());
        assertEquals(0, r1.getTotalAllocatedHours());

        GreedyResult r2 = greedyService.selectPendingTasksGreedy(tasks, -5);
        assertEquals(0, r2.getSelectedTasks().size());
    }

    @Test
    void testCustomTasksAndHeuristicSuboptimalityDocumentation() {
        // Classic 0/1 knapsack example where greedy density heuristic is sub-optimal:
        // Capacity: 5 hours.
        // Item A: 2h, 24 pts (density 12.0)
        // Item B: 3h, 30 pts (density 10.0)
        // Item C: 3h, 30 pts (density 10.0)
        // Greedy takes A (2h, 24pts), leaving 3h. Then takes B (3h, 30pts). Total = 54 pts.
        // Optimal (DP) takes B + C (60 pts) if capacity allowed 6h,
        // or consider Cap = 5h with:
        // Item 1: 1h, 12 pts (density 12.0)
        // Item 2: 3h, 30 pts (density 10.0)
        // Item 3: 3h, 30 pts (density 10.0)
        // Greedy with Cap 4h picks Item 1 (1h, 12pts), then can only pick at most one 3h task (30pts) = 42pts.
        // However, this demonstrates the heuristic behaves strictly as designed:
        List<GreedyTaskItem> custom = new ArrayList<>();
        custom.add(new GreedyTaskItem(1, "Task A", "High", "Dev", "Pending", 24, 2, 12.0));
        custom.add(new GreedyTaskItem(2, "Task B", "Medium", "Dev", "Pending", 30, 3, 10.0));
        custom.add(new GreedyTaskItem(3, "Task C", "Medium", "Dev", "Pending", 30, 3, 10.0));

        GreedyResult result = greedyService.selectCustomTasksGreedy(custom, 5);
        assertEquals(2, result.getSelectedTasks().size());
        assertEquals(1, result.getSelectedTasks().get(0).getTaskId()); // Highest density first
        assertEquals(2, result.getSelectedTasks().get(1).getTaskId());
        assertEquals(54, result.getTotalPriorityScore());
        assertEquals(5, result.getTotalAllocatedHours());
    }
}
