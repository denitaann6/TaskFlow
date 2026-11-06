package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.dto.BacktrackingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TaskBacktrackingServiceTest {

    private TaskBacktrackingService backtrackingService;

    @BeforeEach
    void setUp() {
        backtrackingService = new TaskBacktrackingService();
    }

    @Test
    void testDiamondDAGGeneratesValidTopologicalOrders() {
        // Dependency graph:
        // 101 must precede 102
        // 101 must precede 103
        // 102 must precede 104
        // 103 must precede 104
        List<Integer> taskIds = List.of(101, 102, 103, 104);
        Map<Integer, List<Integer>> graph = new HashMap<>();
        graph.put(101, List.of(102, 103));
        graph.put(102, List.of(104));
        graph.put(103, List.of(104));

        BacktrackingResult result = backtrackingService.planCustomSchedules(taskIds, graph, Map.of(), 10);

        assertFalse(result.isCycleDetected());
        assertEquals(2, result.getSchedulesFound());
        assertEquals(2, result.getValidOrderings().size());

        // The two valid orders are [101, 102, 103, 104] and [101, 103, 102, 104]
        List<Integer> order1 = result.getValidOrderings().get(0);
        List<Integer> order2 = result.getValidOrderings().get(1);

        // Verify strictly that 101 is always first, and 104 is always last
        assertEquals(101, order1.get(0));
        assertEquals(104, order1.get(3));
        assertEquals(101, order2.get(0));
        assertEquals(104, order2.get(3));

        // In both orders, prerequisite index must be smaller than dependent index
        for (List<Integer> order : result.getValidOrderings()) {
            assertTrue(order.indexOf(101) < order.indexOf(102));
            assertTrue(order.indexOf(101) < order.indexOf(103));
            assertTrue(order.indexOf(102) < order.indexOf(104));
            assertTrue(order.indexOf(103) < order.indexOf(104));
        }

        assertTrue(result.getBranchesPruned() > 0, "Backtracking must have pruned invalid candidate branches");
    }

    @Test
    void testLinearChainYieldsSingleDeterministicOrder() {
        // 101 -> 102 -> 103
        List<Integer> taskIds = List.of(103, 101, 102); // deliberately unsorted input
        Map<Integer, List<Integer>> graph = new HashMap<>();
        graph.put(101, List.of(102));
        graph.put(102, List.of(103));

        BacktrackingResult result = backtrackingService.planCustomSchedules(taskIds, graph, Map.of(), 10);

        assertFalse(result.isCycleDetected());
        assertEquals(1, result.getSchedulesFound());
        assertEquals(List.of(101, 102, 103), result.getValidOrderings().get(0));
    }

    @Test
    void testCycleDetectionFailsGracefully() {
        // Circular dependency: 101 -> 102 and 102 -> 101
        List<Integer> taskIds = List.of(101, 102);
        Map<Integer, List<Integer>> graph = new HashMap<>();
        graph.put(101, List.of(102));
        graph.put(102, List.of(101));

        BacktrackingResult result = backtrackingService.planCustomSchedules(taskIds, graph, Map.of(), 10);

        assertTrue(result.isCycleDetected());
        assertEquals(0, result.getSchedulesFound());
        assertNotNull(result.getCycleMessage());
        assertTrue(result.getCycleMessage().contains("circular cycle"));
    }

    @Test
    void testScheduleLimitEnforced() {
        // 4 completely disconnected tasks -> 4! = 24 valid schedules
        List<Integer> taskIds = List.of(1, 2, 3, 4);
        Map<Integer, List<Integer>> graph = Map.of();

        // Request at most 5 schedules
        BacktrackingResult result = backtrackingService.planCustomSchedules(taskIds, graph, Map.of(), 5);

        assertFalse(result.isCycleDetected());
        assertEquals(5, result.getSchedulesFound());
        assertEquals(5, result.getValidOrderings().size());
    }

    @Test
    void testEmptyAndNullGraph() {
        BacktrackingResult r1 = backtrackingService.planCustomSchedules(null, null, null, 10);
        assertEquals(0, r1.getTotalTasks());
        assertEquals(0, r1.getSchedulesFound());

        BacktrackingResult r2 = backtrackingService.planCustomSchedules(List.of(), Map.of(), Map.of(), 10);
        assertEquals(0, r2.getTotalTasks());
        assertEquals(0, r2.getSchedulesFound());
    }
}
