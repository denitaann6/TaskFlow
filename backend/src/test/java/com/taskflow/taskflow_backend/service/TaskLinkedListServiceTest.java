package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.entity.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskLinkedListServiceTest {

    private TaskLinkedListService linkedListService;

    @BeforeEach
    void setUp() {
        linkedListService = new TaskLinkedListService();
    }

    private Task createTask(int id, String name, String member, String priority, String status) {
        Task t = new Task();
        t.setId(id);
        t.setName(name);
        t.setMember(member);
        t.setPriority(priority);
        t.setStatus(status);
        return t;
    }

    @Test
    void testInsertHeadAndTraverse() {
        Task t1 = createTask(101, "First Task", "Alice", "High", "Pending");
        Task t2 = createTask(102, "Second Task", "Bob", "Medium", "In Progress");

        linkedListService.insertHead(t1);
        assertEquals(1, linkedListService.size());
        assertEquals(101, linkedListService.getHead().getTask().getId());

        linkedListService.insertHead(t2); // Insert at head should make t2 the new head
        assertEquals(2, linkedListService.size());
        assertEquals(102, linkedListService.getHead().getTask().getId());

        List<Task> traversal = linkedListService.traverse();
        assertEquals(2, traversal.size());
        assertEquals(102, traversal.get(0).getId()); // t2 first
        assertEquals(101, traversal.get(1).getId()); // t1 second
    }

    @Test
    void testInsertTailAndTraverse() {
        Task t1 = createTask(101, "First Task", "Alice", "High", "Pending");
        Task t2 = createTask(102, "Second Task", "Bob", "Medium", "In Progress");
        Task t3 = createTask(103, "Third Task", "Charlie", "Low", "Completed");

        linkedListService.insertTail(t1);
        linkedListService.insertTail(t2);
        linkedListService.insertTail(t3);

        assertEquals(3, linkedListService.size());

        List<Task> traversal = linkedListService.traverse();
        assertEquals(3, traversal.size());
        assertEquals(101, traversal.get(0).getId());
        assertEquals(102, traversal.get(1).getId());
        assertEquals(103, traversal.get(2).getId());
    }

    @Test
    void testInsertAtIndex() {
        Task t1 = createTask(101, "First", "Alice", "High", "Pending");
        Task t2 = createTask(102, "Second", "Bob", "Medium", "Pending");
        Task tMiddle = createTask(999, "Middle Task", "Admin", "High", "Pending");

        linkedListService.insertTail(t1);
        linkedListService.insertTail(t2);

        // Insert at index 1 (between t1 and t2)
        linkedListService.insertAt(1, tMiddle);

        List<Task> traversal = linkedListService.traverse();
        assertEquals(3, traversal.size());
        assertEquals(101, traversal.get(0).getId());
        assertEquals(999, traversal.get(1).getId());
        assertEquals(102, traversal.get(2).getId());
    }

    @Test
    void testDeleteById() {
        Task t1 = createTask(101, "First", "Alice", "High", "Pending");
        Task t2 = createTask(102, "Second", "Bob", "Medium", "Pending");
        Task t3 = createTask(103, "Third", "Charlie", "Low", "Pending");

        linkedListService.insertTail(t1);
        linkedListService.insertTail(t2);
        linkedListService.insertTail(t3);

        // Case 1: Delete middle node (102)
        Task deletedMiddle = linkedListService.deleteById(102);
        assertNotNull(deletedMiddle);
        assertEquals(102, deletedMiddle.getId());
        assertEquals(2, linkedListService.size());
        assertNull(linkedListService.searchById(102));

        // Case 2: Delete head node (101)
        Task deletedHead = linkedListService.deleteById(101);
        assertNotNull(deletedHead);
        assertEquals(101, deletedHead.getId());
        assertEquals(1, linkedListService.size());
        assertEquals(103, linkedListService.getHead().getTask().getId());

        // Case 3: Delete remaining node (103)
        Task deletedTail = linkedListService.deleteById(103);
        assertNotNull(deletedTail);
        assertEquals(103, deletedTail.getId());
        assertEquals(0, linkedListService.size());
        assertTrue(linkedListService.isEmpty());

        // Case 4: Delete non-existent ID
        assertNull(linkedListService.deleteById(9999));
    }

    @Test
    void testDeleteHeadOnEmptyList() {
        assertTrue(linkedListService.isEmpty());
        assertNull(linkedListService.deleteHead());
    }

    @Test
    void testSearchById() {
        Task t1 = createTask(101, "First", "Alice", "High", "Pending");
        linkedListService.insertTail(t1);

        assertNotNull(linkedListService.searchById(101));
        assertEquals("First", linkedListService.searchById(101).getName());
        assertNull(linkedListService.searchById(999));
        assertNull(linkedListService.searchById(null));
    }

    @Test
    void testLoadFromTasksAndClear() {
        List<Task> tasks = List.of(
                createTask(1, "A", "Alice", "Low", "Pending"),
                createTask(2, "B", "Bob", "Medium", "Completed"),
                createTask(3, "C", "Charlie", "High", "Pending")
        );

        linkedListService.loadFromTasks(tasks);
        assertEquals(3, linkedListService.size());
        assertEquals(1, linkedListService.getHead().getTask().getId());

        linkedListService.clear();
        assertEquals(0, linkedListService.size());
        assertTrue(linkedListService.isEmpty());
    }

    @Test
    void testTraversalVisualAndTrace() {
        Task t1 = createTask(101, "Database Design", "John", "High", "Pending");
        linkedListService.insertTail(t1);

        String visual = linkedListService.getTraversalVisual();
        assertTrue(visual.contains("[HEAD] -> [#101: \"Database Design\"] -> NULL"));

        List<String> trace = linkedListService.getTraversalTrace();
        assertEquals(1, trace.size());
        assertTrue(trace.get(0).contains("Step 1"));
        assertTrue(trace.get(0).contains("Database Design"));
    }

    @Test
    void testArrangeTasksAsLinkedList() {
        List<Task> tasks = List.of(
                createTask(10, "Task 10", "User", "Medium", "Pending"),
                createTask(20, "Task 20", "User", "High", "Completed")
        );

        List<Task> arranged = linkedListService.arrangeTasksAsLinkedList(tasks);
        assertEquals(2, arranged.size());
        assertEquals(10, arranged.get(0).getId());
        assertEquals(20, arranged.get(1).getId());
    }
}
