package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.entity.CompletedTaskStackEntry;
import com.taskflow.taskflow_backend.entity.PoppedTaskHistoryEntry;
import com.taskflow.taskflow_backend.entity.Task;
import com.taskflow.taskflow_backend.repository.CompletedTaskStackRepository;
import com.taskflow.taskflow_backend.repository.PoppedTaskHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskStackServiceTest {

    private TaskStackService stackService;

    @BeforeEach
    void setUp() {
        stackService = new TaskStackService();
    }

    private Task createTask(int id, String name, String status) {
        Task t = new Task();
        t.setId(id);
        t.setName(name);
        t.setMember("Alice");
        t.setPriority("High");
        t.setStatus(status);
        return t;
    }

    @Test
    void testPushAndPopLIFOOrder() {
        Task t1 = createTask(1, "Task One", "Completed");
        Task t2 = createTask(2, "Task Two", "Completed");

        stackService.push(t1);
        stackService.push(t2);

        // LIFO: t2 should be on top
        assertEquals(2, stackService.peek().getId());
        Task popped = stackService.pop();
        assertEquals(2, popped.getId());

        // Now t1 should be on top
        assertEquals(1, stackService.pop().getId());
        assertNull(stackService.pop());
    }

    @Test
    void testGetCompletedStackOrder() {
        Task t1 = createTask(101, "Setup Database", "Completed");
        Task t2 = createTask(102, "Design UI", "Completed");

        stackService.push(t1);
        stackService.push(t2);

        List<Task> stackList = stackService.getCompletedStack(new ArrayList<>());
        assertEquals(2, stackList.size());
        assertEquals(102, stackList.get(0).getId()); // Top of stack is first in list
        assertEquals(101, stackList.get(1).getId());
    }

    @Test
    void testRemoveById() {
        Task t1 = createTask(1, "Task 1", "Completed");
        Task t2 = createTask(2, "Task 2", "Completed");

        stackService.push(t1);
        stackService.push(t2);

        stackService.removeById(2);
        assertEquals(1, stackService.peek().getId());
    }

    @Test
    void testIsBalancedWithBalancedExpressions() {
        assertTrue(stackService.isBalanced(""));
        assertTrue(stackService.isBalanced("()"));
        assertTrue(stackService.isBalanced("()[]{}"));
        assertTrue(stackService.isBalanced("{[()]}"));
        assertTrue(stackService.isBalanced("(a + [b * {c - d}])"));
    }

    @Test
    void testIsBalancedWithUnbalancedExpressions() {
        assertFalse(stackService.isBalanced("(]"));
        assertFalse(stackService.isBalanced("([)]"));
        assertFalse(stackService.isBalanced("{[(])}"));
        assertFalse(stackService.isBalanced("((()"));
        assertFalse(stackService.isBalanced(")("));
        assertFalse(stackService.isBalanced("]"));
    }

    @Test
    void testIsBalancedWithNullOrEmpty() {
        assertTrue(stackService.isBalanced(null));
        assertTrue(stackService.isBalanced(""));
    }

    @Test
    void testPoppingAllItemsKeepsStackEmptyWithoutRepopulating() {
        Task t1 = createTask(101, "Task 1", "Completed");
        Task t2 = createTask(102, "Task 2", "Completed");

        List<Task> dbTasks = List.of(t1, t2);

        // Initial fetch initializes stack from completed tasks
        List<Task> initialStack = stackService.getCompletedStack(dbTasks);
        assertEquals(2, initialStack.size());

        // LIFO pop: first t2, then t1
        Task pop1 = stackService.pop();
        assertEquals(102, pop1.getId());

        Task pop2 = stackService.pop();
        assertEquals(101, pop2.getId());

        // Now stack is empty
        assertTrue(stackService.isEmpty());
        assertNull(stackService.pop());
        assertNull(stackService.peek());
        assertEquals(0, stackService.size());

        // Subsequent getCompletedStack must NOT repopulate from dbTasks
        List<Task> emptyStack = stackService.getCompletedStack(dbTasks);
        assertTrue(emptyStack.isEmpty());
        assertEquals(0, emptyStack.size());
    }

    @Test
    void testValidateWithTraceBalancedAndUnbalanced() {
        // Balanced
        var res1 = stackService.validateWithTrace("TaskFlow{Phase[1(Queue)+Stack]}");
        assertTrue((Boolean) res1.get("balanced"));
        assertEquals("Balanced", res1.get("message"));
        assertNull(res1.get("errorDetail"));
        assertNotNull(res1.get("trace"));
        assertFalse(((List<?>) res1.get("trace")).isEmpty());

        // Unbalanced: mismatched
        var res2 = stackService.validateWithTrace("([)]");
        assertFalse((Boolean) res2.get("balanced"));
        assertEquals("Not Balanced", res2.get("message"));
        assertNotNull(res2.get("errorDetail"));
        assertTrue(res2.get("errorDetail").toString().contains("Mismatch"));

        // Unbalanced: unclosed
        var res3 = stackService.validateWithTrace("((()");
        assertFalse((Boolean) res3.get("balanced"));
        assertTrue(res3.get("errorDetail").toString().contains("Unclosed brackets"));

        // Unbalanced: premature closing
        var res4 = stackService.validateWithTrace("]");
        assertFalse((Boolean) res4.get("balanced"));
        assertTrue(res4.get("errorDetail").toString().contains("Syntax Error"));
    }

    @Test
    void testStackVisualAndDuplicatePush() {
        Task t1 = createTask(101, "Setup Database", "Completed");
        stackService.push(t1);
        assertEquals(1, stackService.size());

        // Duplicate push with same ID should not increase size
        stackService.push(t1);
        assertEquals(1, stackService.size());

        String visual = stackService.getStackVisual();
        assertTrue(visual.contains("[TOP] #101: \"Setup Database\""));
        assertTrue(visual.contains("Total Elements on Stack: 1"));

        stackService.clear();
        assertTrue(stackService.isEmpty());
        assertEquals(0, stackService.size());
        assertTrue(stackService.getStackVisual().contains("[STACK IS EMPTY]"));
    }

    @Test
    void testPeekDoesNotModifyStackOrWriteHistory() {
        CompletedTaskStackRepository mockStackRepo = mock(CompletedTaskStackRepository.class);
        PoppedTaskHistoryRepository mockPoppedRepo = mock(PoppedTaskHistoryRepository.class);

        TaskStackService svc = new TaskStackService(mockStackRepo, mockPoppedRepo);

        Task t = createTask(108, "Integration Tests", "Completed");
        CompletedTaskStackEntry entry = new CompletedTaskStackEntry(t, 3L, LocalDateTime.now());
        when(mockStackRepo.findTopByOrderByStackOrderDesc()).thenReturn(Optional.of(entry));

        Task peeked = svc.peek();
        assertNotNull(peeked);
        assertEquals(108, peeked.getId());
        assertEquals("Integration Tests", peeked.getName());

        // Verify read-only: no deletions, no saves
        verify(mockStackRepo, never()).delete(any());
        verify(mockPoppedRepo, never()).save(any());
    }

    @Test
    void testPopRemovesActiveEntryAndWritesHistory() {
        CompletedTaskStackRepository mockStackRepo = mock(CompletedTaskStackRepository.class);
        PoppedTaskHistoryRepository mockPoppedRepo = mock(PoppedTaskHistoryRepository.class);

        TaskStackService svc = new TaskStackService(mockStackRepo, mockPoppedRepo);

        Task t = createTask(108, "Integration Tests", "Completed");
        CompletedTaskStackEntry entry = new CompletedTaskStackEntry(t, 3L, LocalDateTime.now());
        when(mockStackRepo.findTopByOrderByStackOrderDesc()).thenReturn(Optional.of(entry));

        Task popped = svc.pop("AdminUser");
        assertNotNull(popped);
        assertEquals(108, popped.getId());

        // Verify active stack entry is deleted
        verify(mockStackRepo).delete(entry);

        // Verify history entry is saved with correct properties
        ArgumentCaptor<PoppedTaskHistoryEntry> historyCaptor = ArgumentCaptor.forClass(PoppedTaskHistoryEntry.class);
        verify(mockPoppedRepo).save(historyCaptor.capture());
        PoppedTaskHistoryEntry savedHistory = historyCaptor.getValue();
        assertEquals(108, savedHistory.getTaskId());
        assertEquals("Integration Tests", savedHistory.getTaskName());
        assertEquals("AdminUser", savedHistory.getPoppedBy());
        assertEquals(3L, savedHistory.getOriginalStackOrder());
    }

    @Test
    void testPopWhenEmptyReturnsNullAndCreatesNoHistory() {
        CompletedTaskStackRepository mockStackRepo = mock(CompletedTaskStackRepository.class);
        PoppedTaskHistoryRepository mockPoppedRepo = mock(PoppedTaskHistoryRepository.class);

        TaskStackService svc = new TaskStackService(mockStackRepo, mockPoppedRepo);

        when(mockStackRepo.findTopByOrderByStackOrderDesc()).thenReturn(Optional.empty());

        Task popped = svc.pop("AdminUser");
        assertNull(popped);

        // Verify no deletes and no history created
        verify(mockStackRepo, never()).delete(any());
        verify(mockPoppedRepo, never()).save(any());
    }

    @Test
    void testPoppedTasksAreNeverReseededToActiveStack() {
        CompletedTaskStackRepository mockStackRepo = mock(CompletedTaskStackRepository.class);
        PoppedTaskHistoryRepository mockPoppedRepo = mock(PoppedTaskHistoryRepository.class);

        TaskStackService svc = new TaskStackService(mockStackRepo, mockPoppedRepo);

        Task t1 = createTask(110, "backend testing", "Completed");
        Task t2 = createTask(108, "Integration Tests", "Completed");

        // t1 has already been popped in the past (exists in history)
        when(mockPoppedRepo.existsByTaskId(110)).thenReturn(true);
        when(mockPoppedRepo.existsByTaskId(108)).thenReturn(false);

        List<CompletedTaskStackEntry> savedEntries = new ArrayList<>();
        when(mockStackRepo.save(any(CompletedTaskStackEntry.class))).thenAnswer(inv -> {
            CompletedTaskStackEntry e = inv.getArgument(0);
            savedEntries.add(e);
            return e;
        });
        when(mockStackRepo.findAllByOrderByStackOrderDesc()).thenAnswer(inv -> new ArrayList<>(savedEntries));

        List<Task> activeStack = svc.getCompletedStack(List.of(t1, t2));

        // Only t2 should be pushed; t1 must NOT be reseeded
        assertEquals(1, activeStack.size());
        assertEquals(108, activeStack.get(0).getId());
        verify(mockStackRepo, never()).save(argThat(entry -> entry.getTask() != null && entry.getTask().getId().equals(110)));
        verify(mockStackRepo, atLeastOnce()).save(argThat(entry -> entry.getTask() != null && entry.getTask().getId().equals(108)));
    }
}

