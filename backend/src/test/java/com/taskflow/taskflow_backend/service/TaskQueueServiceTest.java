package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.entity.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskQueueServiceTest {

    private TaskQueueService queueService;

    @BeforeEach
    void setUp() {
        queueService = new TaskQueueService();
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
    void testEnqueueAndDequeueFIFOOrder() {
        Task t1 = createTask(1, "Earliest Task", "Alice", "High", "Pending");
        Task t2 = createTask(2, "Second Task", "Bob", "Medium", "Pending");
        Task t3 = createTask(3, "Latest Task", "Charlie", "Low", "Pending");

        // Enqueue 1, then 2, then 3
        queueService.enqueue(t1);
        queueService.enqueue(t2);
        queueService.enqueue(t3);

        assertEquals(3, queueService.size());
        assertFalse(queueService.isEmpty());

        // First In First Out: t1 must come out first
        assertEquals(1, queueService.peek().getId());
        Task dequeued1 = queueService.dequeue();
        assertEquals(1, dequeued1.getId());
        assertEquals(2, queueService.size());

        // Then t2
        assertEquals(2, queueService.peek().getId());
        Task dequeued2 = queueService.dequeue();
        assertEquals(2, dequeued2.getId());

        // Then t3
        Task dequeued3 = queueService.dequeue();
        assertEquals(3, dequeued3.getId());

        // Queue must now be empty
        assertTrue(queueService.isEmpty());
        assertNull(queueService.peek());
        assertNull(queueService.dequeue());
    }

    @Test
    void testPeekDoesNotRemove() {
        Task t1 = createTask(10, "Task 10", "User", "High", "Pending");
        queueService.enqueue(t1);

        assertEquals(10, queueService.peek().getId());
        assertEquals(10, queueService.peek().getId());
        assertEquals(1, queueService.size()); // Size remains unchanged
    }

    @Test
    void testEmptyQueueHandling() {
        assertTrue(queueService.isEmpty());
        assertNull(queueService.peek());
        assertNull(queueService.dequeue());
        assertEquals(0, queueService.size());
    }

    @Test
    void testSynchronizePendingTasksFiltersAndSorts() {
        Task pending101 = createTask(101, "Pending Task 101", "A", "High", "Pending");
        Task completed102 = createTask(102, "Completed Task", "B", "High", "Completed");
        Task pending103 = createTask(103, "Pending Task 103", "C", "Low", "Pending");
        Task inProgress104 = createTask(104, "In Progress", "D", "Medium", "In Progress");

        queueService.synchronizePendingTasks(List.of(inProgress104, pending103, completed102, pending101));

        // Only Pending tasks (101 and 103) should be enqueued, sorted by ID
        List<Task> pending = queueService.toList();
        assertEquals(2, pending.size());
        assertEquals(101, pending.get(0).getId());
        assertEquals(103, pending.get(1).getId());
    }

    @Test
    void testProcessNextPendingTask() {
        Task pending1 = createTask(101, "First Pending", "A", "High", "Pending");
        Task pending2 = createTask(102, "Second Pending", "B", "Medium", "Pending");

        Task processed = queueService.processNextPendingTask(List.of(pending1, pending2));
        assertNotNull(processed);
        assertEquals(101, processed.getId());

        // Subsequent dequeue gets next
        Task next = queueService.dequeue();
        assertEquals(102, next.getId());
    }

    @Test
    void testRemoveByIdFromFrontAndMiddle() {
        Task t1 = createTask(1, "T1", "A", "High", "Pending");
        Task t2 = createTask(2, "T2", "B", "Medium", "Pending");
        Task t3 = createTask(3, "T3", "C", "Low", "Pending");

        queueService.enqueue(t1);
        queueService.enqueue(t2);
        queueService.enqueue(t3);

        // Remove middle (T2)
        queueService.removeById(2);
        assertEquals(2, queueService.size());
        assertEquals(1, queueService.dequeue().getId());
        assertEquals(3, queueService.dequeue().getId());

        // Re-enqueue and remove front (T1)
        queueService.enqueue(t1);
        queueService.enqueue(t2);
        queueService.removeById(1);
        assertEquals(1, queueService.size());
        assertEquals(2, queueService.peek().getId());
    }

    @Test
    void testQueueVisualOutput() {
        Task t1 = createTask(101, "Database Setup", "John", "High", "Pending");
        queueService.enqueue(t1);

        String visual = queueService.getQueueVisual();
        assertTrue(visual.contains("[FRONT / NEXT TO PROCESS]"));
        assertTrue(visual.contains("#101: \"Database Setup\""));
        assertTrue(visual.contains("[REAR / LATEST ARRIVAL]"));
    }
}
