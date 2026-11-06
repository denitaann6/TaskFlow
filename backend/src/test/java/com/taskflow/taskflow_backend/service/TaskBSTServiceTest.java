package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.entity.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TaskBSTServiceTest {

    private TaskBSTService bstService;
    private List<Task> sampleTasks;

    @BeforeEach
    void setUp() {
        bstService = new TaskBSTService();
        sampleTasks = new ArrayList<>();

        // Add tasks with IDs in an unbalanced order: 105 (root), 102 (left), 108 (right), 101, 103
        sampleTasks.add(createTask(105, "Root Task"));
        sampleTasks.add(createTask(102, "Left Child"));
        sampleTasks.add(createTask(108, "Right Child"));
        sampleTasks.add(createTask(101, "Left-Left Leaf"));
        sampleTasks.add(createTask(103, "Left-Right Leaf"));
    }

    private Task createTask(int id, String name) {
        Task t = new Task();
        t.setId(id);
        t.setName(name);
        t.setMember("Dev");
        t.setPriority("Medium");
        t.setStatus("Pending");
        return t;
    }

    @Test
    void testSearchExistingTaskWithTrace() {
        Map<String, Object> result = bstService.searchWithTrace(sampleTasks, 103);
        assertTrue((Boolean) result.get("found"));
        assertNotNull(result.get("task"));

        Task found = (Task) result.get("task");
        assertEquals(103, found.getId());
        assertEquals("Left-Right Leaf", found.getName());

        // Traversal path: 105 -> went left -> 102 -> went right -> 103 (3 comparisons)
        assertEquals(3, result.get("comparisons"));
        @SuppressWarnings("unchecked")
        List<String> path = (List<String>) result.get("path");
        assertEquals(3, path.size());
    }

    @Test
    void testSearchNonExistentTask() {
        Map<String, Object> result = bstService.searchWithTrace(sampleTasks, 999);
        assertFalse((Boolean) result.get("found"));
        assertNull(result.get("task"));
        // 105 -> went right -> 108 -> went right -> null
        assertEquals(2, result.get("comparisons"));
    }

    @Test
    void testInorderTraversalYieldsSortedIDs() {
        List<Task> inorder = bstService.getInorderTraversal(sampleTasks);
        assertEquals(5, inorder.size());

        // Inorder of BST must always be sorted by Key/ID (Left -> Root -> Right)
        assertEquals(101, inorder.get(0).getId());
        assertEquals(102, inorder.get(1).getId());
        assertEquals(103, inorder.get(2).getId());
        assertEquals(105, inorder.get(3).getId());
        assertEquals(108, inorder.get(4).getId());
    }

    @Test
    void testPreorderTraversal() {
        List<Task> preorder = bstService.getPreorderTraversal(sampleTasks);
        assertEquals(5, preorder.size());

        // Preorder: Root -> Left -> Right (105, 102, 101, 103, 108)
        assertEquals(105, preorder.get(0).getId());
        assertEquals(102, preorder.get(1).getId());
        assertEquals(101, preorder.get(2).getId());
        assertEquals(103, preorder.get(3).getId());
        assertEquals(108, preorder.get(4).getId());
    }

    @Test
    void testPostorderTraversal() {
        List<Task> postorder = bstService.getPostorderTraversal(sampleTasks);
        assertEquals(5, postorder.size());

        // Postorder: Left -> Right -> Root (101, 103, 102, 108, 105)
        assertEquals(101, postorder.get(0).getId());
        assertEquals(103, postorder.get(1).getId());
        assertEquals(102, postorder.get(2).getId());
        assertEquals(108, postorder.get(3).getId());
        assertEquals(105, postorder.get(4).getId());
    }

    @Test
    void testTraversalsWithEmptyList() {
        List<Task> empty = List.of();
        assertTrue(bstService.getInorderTraversal(empty).isEmpty());
        assertTrue(bstService.getPreorderTraversal(empty).isEmpty());
        assertTrue(bstService.getPostorderTraversal(empty).isEmpty());
    }
}
