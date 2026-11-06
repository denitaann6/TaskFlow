package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.entity.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TaskSearchSortServiceTest {

    private TaskSearchSortService searchSortService;
    private List<Task> sampleTasks;

    @BeforeEach
    void setUp() {
        searchSortService = new TaskSearchSortService();
        sampleTasks = new ArrayList<>();

        // Unsorted tasks: IDs 105, 102, 108, 101, 103
        sampleTasks.add(createTask(105, "Report Writing", "Medium"));
        sampleTasks.add(createTask(102, "Database Design", "High"));
        sampleTasks.add(createTask(108, "Testing Module", "Low"));
        sampleTasks.add(createTask(101, "API Development", "High"));
        sampleTasks.add(createTask(103, "Bug Fixing", "Medium"));
    }

    private Task createTask(int id, String name, String priority) {
        Task t = new Task();
        t.setId(id);
        t.setName(name);
        t.setMember("Alex");
        t.setPriority(priority);
        t.setStatus("Pending");
        return t;
    }

    // ==========================================
    // 1. LINEAR SEARCH TESTS
    // ==========================================

    @Test
    void testLinearSearchByIdFound() {
        Map<String, Object> result = searchSortService.linearSearch(sampleTasks, "108");
        assertTrue((Boolean) result.get("found"));
        assertNotNull(result.get("task"));

        Task found = (Task) result.get("task");
        assertEquals(108, found.getId());
        assertEquals("Testing Module", found.getName());
        assertEquals(2, result.get("index")); // index 2 in sampleTasks (105, 102, 108)
        assertEquals(3, result.get("comparisons")); // 3 sequential checks
    }

    @Test
    void testLinearSearchByNameFound() {
        Map<String, Object> result = searchSortService.linearSearch(sampleTasks, "API");
        assertTrue((Boolean) result.get("found"));
        Task found = (Task) result.get("task");
        assertEquals(101, found.getId());
        assertEquals(3, result.get("index")); // index 3 in sampleTasks
    }

    @Test
    void testLinearSearchNotFound() {
        Map<String, Object> result = searchSortService.linearSearch(sampleTasks, "999");
        assertFalse((Boolean) result.get("found"));
        assertNull(result.get("task"));
        assertEquals(-1, result.get("index"));
        assertEquals(5, result.get("comparisons")); // checked all 5 elements
    }

    @Test
    void testLinearSearchEmptyList() {
        Map<String, Object> result = searchSortService.linearSearch(List.of(), "101");
        assertFalse((Boolean) result.get("found"));
        assertEquals(0, result.get("comparisons"));
    }

    // ==========================================
    // 2. BINARY SEARCH TESTS
    // ==========================================

    @Test
    void testBinarySearchFound() {
        // Binary search must first sort the tasks (101, 102, 103, 105, 108)
        Map<String, Object> result = searchSortService.binarySearch(sampleTasks, 103);
        assertTrue((Boolean) result.get("found"));
        assertNotNull(result.get("task"));

        Task found = (Task) result.get("task");
        assertEquals(103, found.getId());
        assertEquals("Bug Fixing", found.getName());

        // In sorted order [101, 102, 103, 105, 108], mid index 2 is 103 -> Found in 1 comparison!
        assertEquals(2, result.get("sortedIndex"));
        assertEquals(1, result.get("comparisons"));

        @SuppressWarnings("unchecked")
        List<Integer> sortedIds = (List<Integer>) result.get("sortedTaskIds");
        assertEquals(List.of(101, 102, 103, 105, 108), sortedIds);
    }

    @Test
    void testBinarySearchLeftBranch() {
        // Search 101: Mid is 103, 101 < 103 goes left, next mid is 101 -> match found!
        Map<String, Object> result = searchSortService.binarySearch(sampleTasks, 101);
        assertTrue((Boolean) result.get("found"));
        Task found = (Task) result.get("task");
        assertEquals(101, found.getId());
        assertEquals(0, result.get("sortedIndex"));
        assertEquals(2, result.get("comparisons"));
    }

    @Test
    void testBinarySearchNotFound() {
        Map<String, Object> result = searchSortService.binarySearch(sampleTasks, 104);
        assertFalse((Boolean) result.get("found"));
        assertNull(result.get("task"));
        assertEquals(-1, result.get("sortedIndex"));
        assertTrue((Integer) result.get("comparisons") > 0);
    }

    @Test
    void testBinarySearchEmptyOrNull() {
        Map<String, Object> r1 = searchSortService.binarySearch(List.of(), 101);
        assertFalse((Boolean) r1.get("found"));

        Map<String, Object> r2 = searchSortService.binarySearch(sampleTasks, null);
        assertFalse((Boolean) r2.get("found"));
    }

    // ==========================================
    // 3. INSERTION SORT TESTS
    // ==========================================

    @Test
    void testInsertionSortById() {
        Map<String, Object> result = searchSortService.insertionSort(sampleTasks, "id");
        @SuppressWarnings("unchecked")
        List<Task> sorted = (List<Task>) result.get("sortedTasks");

        assertEquals(5, sorted.size());
        assertEquals(101, sorted.get(0).getId());
        assertEquals(102, sorted.get(1).getId());
        assertEquals(103, sorted.get(2).getId());
        assertEquals(105, sorted.get(3).getId());
        assertEquals(108, sorted.get(4).getId());

        assertTrue((Integer) result.get("comparisons") > 0);
        assertTrue((Integer) result.get("shifts") > 0);
    }

    @Test
    void testInsertionSortByName() {
        Map<String, Object> result = searchSortService.insertionSort(sampleTasks, "name");
        @SuppressWarnings("unchecked")
        List<Task> sorted = (List<Task>) result.get("sortedTasks");

        assertEquals("API Development", sorted.get(0).getName());
        assertEquals("Bug Fixing", sorted.get(1).getName());
        assertEquals("Database Design", sorted.get(2).getName());
        assertEquals("Report Writing", sorted.get(3).getName());
        assertEquals("Testing Module", sorted.get(4).getName());
    }

    @Test
    void testInsertionSortByPriority() {
        Map<String, Object> result = searchSortService.insertionSort(sampleTasks, "priority");
        @SuppressWarnings("unchecked")
        List<Task> sorted = (List<Task>) result.get("sortedTasks");

        // High priority first (Database Design, API Development)
        assertEquals("High", sorted.get(0).getPriority());
        assertEquals("High", sorted.get(1).getPriority());
        // Medium next
        assertEquals("Medium", sorted.get(2).getPriority());
        assertEquals("Medium", sorted.get(3).getPriority());
        // Low last
        assertEquals("Low", sorted.get(4).getPriority());
    }

    @Test
    void testInsertionSortDoesNotMutateOriginalList() {
        int originalFirstId = sampleTasks.get(0).getId();
        searchSortService.insertionSort(sampleTasks, "id");

        // Original list must remain unsorted / unmutated
        assertEquals(originalFirstId, sampleTasks.get(0).getId());
        assertEquals(105, sampleTasks.get(0).getId());
    }

    // ==========================================
    // 4. QUICK SORT TESTS
    // ==========================================

    @Test
    void testQuickSortById() {
        Map<String, Object> result = searchSortService.quickSort(sampleTasks, "id");
        @SuppressWarnings("unchecked")
        List<Task> sorted = (List<Task>) result.get("sortedTasks");

        assertEquals(5, sorted.size());
        assertEquals(101, sorted.get(0).getId());
        assertEquals(102, sorted.get(1).getId());
        assertEquals(103, sorted.get(2).getId());
        assertEquals(105, sorted.get(3).getId());
        assertEquals(108, sorted.get(4).getId());

        assertTrue((Integer) result.get("comparisons") > 0);
        assertTrue((Integer) result.get("swaps") >= 0);
    }

    @Test
    void testQuickSortByName() {
        Map<String, Object> result = searchSortService.quickSort(sampleTasks, "name");
        @SuppressWarnings("unchecked")
        List<Task> sorted = (List<Task>) result.get("sortedTasks");

        assertEquals("API Development", sorted.get(0).getName());
        assertEquals("Bug Fixing", sorted.get(1).getName());
        assertEquals("Database Design", sorted.get(2).getName());
        assertEquals("Report Writing", sorted.get(3).getName());
        assertEquals("Testing Module", sorted.get(4).getName());
    }

    @Test
    void testQuickSortDoesNotMutateOriginalList() {
        int originalFirstId = sampleTasks.get(0).getId();
        searchSortService.quickSort(sampleTasks, "id");

        assertEquals(originalFirstId, sampleTasks.get(0).getId());
        assertEquals(105, sampleTasks.get(0).getId());
    }

    // ==========================================
    // 5. EXTENDED SEARCH TESTS
    // ==========================================

    @Test
    void testBinarySearchByNameFound() {
        Map<String, Object> result = searchSortService.binarySearch(sampleTasks, "Database Design", "name");
        assertTrue((Boolean) result.get("found"));
        assertNotNull(result.get("task"));
        Task t = (Task) result.get("task");
        assertEquals("Database Design", t.getName());
        assertEquals(102, t.getId());
        assertTrue((Integer) result.get("comparisons") > 0);
    }

    @Test
    void testBinarySearchByNameNotFound() {
        Map<String, Object> result = searchSortService.binarySearch(sampleTasks, "Nonexistent Task", "name");
        assertFalse((Boolean) result.get("found"));
        assertNull(result.get("task"));
        assertEquals(-1, result.get("sortedIndex"));
    }

    @Test
    void testLinearSearchWithExplicitField() {
        Map<String, Object> r1 = searchSortService.linearSearch(sampleTasks, "105", "id");
        assertTrue((Boolean) r1.get("found"));
        assertEquals(105, ((Task) r1.get("task")).getId());

        Map<String, Object> r2 = searchSortService.linearSearch(sampleTasks, "Report", "name");
        assertTrue((Boolean) r2.get("found"));
        assertEquals("Report Writing", ((Task) r2.get("task")).getName());
    }
}
