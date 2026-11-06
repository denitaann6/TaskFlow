package com.taskflow.taskflow_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TaskGraphServiceTest {

    private TaskGraphService graphService;

    @BeforeEach
    void setUp() {
        graphService = new TaskGraphService();
    }

    @Test
    void testAddDependency() {
        assertTrue(graphService.addDependency(101, 102));
        assertTrue(graphService.addDependency(101, 103));
        assertFalse(graphService.addDependency(101, 102)); // duplicate edge
        assertFalse(graphService.addDependency(101, 101)); // self-loop

        Map<Integer, List<Integer>> graph = graphService.getGraph();
        assertEquals(2, graph.get(101).size());
        assertTrue(graph.get(101).contains(102));
        assertTrue(graph.get(101).contains(103));
    }

    @Test
    void testBFSTraversalOrder() {
        // Build dependency chain:
        // 101 -> 102, 101 -> 103
        // 102 -> 104
        // 103 -> 105
        graphService.addDependency(101, 102);
        graphService.addDependency(101, 103);
        graphService.addDependency(102, 104);
        graphService.addDependency(103, 105);

        List<Integer> bfsOrder = graphService.bfsTraversal(101);

        // BFS level 0: 101
        assertEquals(101, bfsOrder.get(0));
        // BFS level 1: 102 and 103
        assertTrue(bfsOrder.subList(1, 3).contains(102));
        assertTrue(bfsOrder.subList(1, 3).contains(103));
        // BFS level 2: 104 and 105
        assertTrue(bfsOrder.subList(3, 5).contains(104));
        assertTrue(bfsOrder.subList(3, 5).contains(105));
        assertEquals(5, bfsOrder.size());
    }

    @Test
    void testRemoveTask() {
        graphService.addDependency(101, 102);
        graphService.addDependency(102, 103);

        graphService.removeTask(102);

        Map<Integer, List<Integer>> graph = graphService.getGraph();
        assertFalse(graph.containsKey(102));
        assertFalse(graph.get(101).contains(102));
    }
}
