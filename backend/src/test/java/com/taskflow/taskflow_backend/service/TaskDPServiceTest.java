package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.dto.DPResult;
import com.taskflow.taskflow_backend.dto.DPTaskItem;
import com.taskflow.taskflow_backend.entity.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskDPServiceTest {

    private TaskDPService dpService;

    @BeforeEach
    void setUp() {
        dpService = new TaskDPService();
    }

    private Task createTask(int id, String name, String priority) {
        Task t = new Task();
        t.setId(id);
        t.setName(name);
        t.setMember("Sam");
        t.setPriority(priority);
        t.setStatus("Pending");
        return t;
    }

    @Test
    void testStandardDPOptimalAllocation() {
        // High: 3h, 30 benefit
        // Medium: 2h, 18 benefit
        // Low: 1h, 10 benefit
        List<Task> tasks = List.of(
                createTask(101, "Architecture", "High"),
                createTask(102, "Security Audit", "Medium"),
                createTask(103, "Docs", "Low")
        );

        // Time limit: 4 hours
        // Options:
        // High (3h, 30) + Low (1h, 10) = 4h, 40 pts
        // Medium (2h, 18) + Low (1h, 10) = 3h, 28 pts
        // Optimal: 101 and 103 (total 40 pts)
        DPResult result = dpService.allocateTasksDP(tasks, 4);

        assertEquals(40, result.getMaxBenefitAchieved());
        assertEquals(4, result.getTotalDurationUsed());
        assertEquals(0, result.getRemainingHours());
        assertEquals(2, result.getSelectedTasks().size());
        assertEquals(101, result.getSelectedTasks().get(0).getTaskId());
        assertEquals(103, result.getSelectedTasks().get(1).getTaskId());
        assertNotNull(result.getSampleMetricsExplanation());
    }

    @Test
    void testCustomKnapsackCaseWhereDPFindsTrueOptimum() {
        // Custom scenario where greedy density heuristic fails, but DP finds global optimum:
        // Capacity: 5 hours.
        // Task 1: 3h, 20 pts (density 6.67)
        // Task 2: 2h, 12 pts (density 6.00)
        // Task 3: 3h, 20 pts (density 6.67)
        // Greedy would pick Task 1 (3h, 20pts), then only 2h left so it picks Task 2 (2h, 12pts) = 32 pts total.
        // But if Task 2 (2h, 12pts) + Task 1 (3h, 20pts) = 32 pts,
        // Wait, consider:
        // Item 1: 1h, 12 pts (density 12.0)
        // Item 2: 3h, 28 pts (density 9.33)
        // Item 3: 3h, 28 pts (density 9.33)
        // Limit: 6 hours.
        // Greedy takes Item 1 (1h, 12), then can only fit ONE 3h item (28) = 40 pts.
        // DP takes Item 2 + Item 3 (3h+3h = 6h, 28+28 = 56 pts)!
        List<DPTaskItem> custom = List.of(
                new DPTaskItem(1, "Small Fast Task", "Low", "Sam", "Pending", 1, 12),
                new DPTaskItem(2, "Heavy Core Module", "High", "Sam", "Pending", 3, 28),
                new DPTaskItem(3, "Heavy Integration", "High", "Sam", "Pending", 3, 28)
        );

        DPResult result = dpService.allocateCustomTasksDP(custom, 6);

        assertEquals(56, result.getMaxBenefitAchieved());
        assertEquals(6, result.getTotalDurationUsed());
        assertEquals(2, result.getSelectedTasks().size());
        assertEquals(2, result.getSelectedTasks().get(0).getTaskId());
        assertEquals(3, result.getSelectedTasks().get(1).getTaskId());
    }

    @Test
    void testZeroAndNegativeTimeLimit() {
        List<Task> tasks = List.of(createTask(1, "Test", "High"));

        DPResult r1 = dpService.allocateTasksDP(tasks, 0);
        assertEquals(0, r1.getMaxBenefitAchieved());
        assertEquals(0, r1.getSelectedTasks().size());

        DPResult r2 = dpService.allocateTasksDP(tasks, -10);
        assertEquals(0, r2.getMaxBenefitAchieved());
        assertEquals(0, r2.getSelectedTasks().size());
    }

    @Test
    void testEmptyAndNullTasks() {
        DPResult r1 = dpService.allocateTasksDP(null, 5);
        assertEquals(0, r1.getMaxBenefitAchieved());
        assertEquals(0, r1.getSelectedTasks().size());

        DPResult r2 = dpService.allocateTasksDP(new ArrayList<>(), 5);
        assertEquals(0, r2.getMaxBenefitAchieved());
        assertEquals(0, r2.getSelectedTasks().size());
    }

    @Test
    void testCapacityTooSmallForAnyTask() {
        List<DPTaskItem> items = List.of(
                new DPTaskItem(1, "Big Task", "High", "Sam", "Pending", 5, 50)
        );

        DPResult result = dpService.allocateCustomTasksDP(items, 3);
        assertEquals(0, result.getMaxBenefitAchieved());
        assertEquals(0, result.getSelectedTasks().size());
        assertEquals(3, result.getRemainingHours());
    }
}
