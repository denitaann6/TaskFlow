package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.controller.TaskController;
import com.taskflow.taskflow_backend.entity.Task;
import com.taskflow.taskflow_backend.entity.User;
import com.taskflow.taskflow_backend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskControllerAuthTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskLinkedListService linkedListService;
    @Mock
    private TaskQueueService queueService;
    @Mock
    private TaskStackService stackService;
    @Mock
    private TaskBSTService bstService;
    @Mock
    private TaskGraphService graphService;
    @Mock
    private TaskSearchSortService searchSortService;
    @Mock
    private AuthService authService;

    private TaskController taskController;

    private User adminUser;
    private User memberJohn;
    private User memberSubu;
    private Task taskJohn;
    private Task taskSubu;

    @BeforeEach
    void setUp() {
        taskController = new TaskController(
                taskRepository,
                linkedListService,
                queueService,
                stackService,
                bstService,
                graphService,
                searchSortService,
                authService
        );

        adminUser = new User("admin", "hash", "Administrator", "ADMIN", true, "Administrator");
        adminUser.setId(1);

        memberJohn = new User("john", "hash", "John", "MEMBER", true, "John");
        memberJohn.setId(2);

        memberSubu = new User("subu", "hash", "Subu", "MEMBER", true, "subu");
        memberSubu.setId(3);

        taskJohn = new Task();
        taskJohn.setId(101);
        taskJohn.setName("Database Design");
        taskJohn.setMember("John");
        taskJohn.setStatus("Pending");

        taskSubu = new Task();
        taskSubu.setId(102);
        taskSubu.setName("trial");
        taskSubu.setMember("subu");
        taskSubu.setStatus("In Progress");
    }

    @Test
    @DisplayName("Admin sees all tasks across entire organization")
    void testAdminSeesAllTasks() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(authService.requireUser(request)).thenReturn(adminUser);
        when(taskRepository.findAll()).thenReturn(List.of(taskJohn, taskSubu));
        when(linkedListService.arrangeTasksAsLinkedList(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Task> result = taskController.getAllTasks(null, request);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Member sees ONLY tasks assigned to their account")
    void testMemberSeesOnlyOwnTasks() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(authService.requireUser(request)).thenReturn(memberJohn);
        when(taskRepository.findAll()).thenReturn(List.of(taskJohn, taskSubu));
        when(linkedListService.arrangeTasksAsLinkedList(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Task> result = taskController.getAllTasks(null, request);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getMember());
        assertEquals(101, result.get(0).getId());
    }

    @Test
    @DisplayName("Member attempting to create a task receives HTTP 403 Forbidden")
    void testMemberCannotCreateTask() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Admin required"))
                .when(authService).requireAdmin(request);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                taskController.addTask(999, "New Task", "John", "Medium", "Pending", request));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Member attempting to delete a task receives HTTP 403 Forbidden")
    void testMemberCannotDeleteTask() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Admin required"))
                .when(authService).requireAdmin(request);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                taskController.deleteTask(101, request));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("IDOR Prevention: Member attempting to update another member's task receives HTTP 403 Forbidden")
    void testIdorPreventedOnUpdateStatus() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // John tries to update Subu's task 102
        when(authService.requireUser(request)).thenReturn(memberJohn);
        when(taskRepository.findById(102)).thenReturn(Optional.of(taskSubu));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                taskController.updateTaskStatus(102, "Completed", request));

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Access denied"));
    }

    @Test
    @DisplayName("Member can successfully update status of their own task")
    void testMemberCanUpdateOwnTaskStatus() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(authService.requireUser(request)).thenReturn(memberJohn);
        when(taskRepository.findById(101)).thenReturn(Optional.of(taskJohn));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task updated = taskController.updateTaskStatus(101, "In Progress", request);
        assertEquals("In Progress", updated.getStatus());
        verify(taskRepository).save(taskJohn);
        verify(stackService).removeById(101);
        verify(queueService).removeById(101);
    }

    @Test
    @DisplayName("Member updating own task to Completed pushes to stack and removes from queue")
    void testMemberCanUpdateOwnTaskToCompleted() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(authService.requireUser(request)).thenReturn(memberJohn);
        when(taskRepository.findById(101)).thenReturn(Optional.of(taskJohn));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task updated = taskController.updateTaskStatus(101, "Completed", request);
        assertEquals("Completed", updated.getStatus());
        verify(taskRepository).save(taskJohn);
        verify(queueService).removeById(101);
        verify(stackService).push(updated);
    }

    @Test
    @DisplayName("IDOR Prevention: Member attempting to complete another member's task receives HTTP 403 Forbidden")
    void testIdorPreventedOnCompleteTask() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(authService.requireUser(request)).thenReturn(memberJohn);
        when(taskRepository.findById(102)).thenReturn(Optional.of(taskSubu));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                taskController.completeTask(102, request));

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Access denied"));
    }

    @Test
    @DisplayName("Unauthenticated request receives HTTP 401 Unauthorized")
    void testUnauthenticatedThrowsUnauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(authService.requireUser(request)).thenThrow(
                new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                taskController.getAllTasks(null, request));

        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Admin can access system-wide Pending Queue")
    void testAdminCanAccessQueue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(authService.requireAdmin(request)).thenReturn(adminUser);
        when(taskRepository.findAll()).thenReturn(List.of(taskJohn));
        when(queueService.getPendingQueue(anyList())).thenReturn(List.of(taskJohn));

        List<Task> result = taskController.getPendingQueue(request);
        assertEquals(1, result.size());
        assertEquals(101, result.get(0).getId());
    }

    @Test
    @DisplayName("Member attempting to access Pending Queue receives HTTP 403 Forbidden")
    void testMemberCannotAccessQueueForbidden() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Admin required"))
                .when(authService).requireAdmin(request);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                taskController.getPendingQueue(request));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Member attempting to peek Pending Queue receives HTTP 403 Forbidden")
    void testMemberCannotPeekQueueForbidden() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Admin required"))
                .when(authService).requireAdmin(request);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                taskController.peekQueue(request));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Admin can access Completed Tasks Stack")
    void testAdminCanAccessStack() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(authService.requireAdmin(request)).thenReturn(adminUser);
        when(taskRepository.findAll()).thenReturn(List.of(taskSubu));
        when(stackService.getCompletedStack(anyList())).thenReturn(List.of(taskSubu));

        List<Task> result = taskController.getCompletedStack(request);
        assertEquals(1, result.size());
        assertEquals(102, result.get(0).getId());
    }

    @Test
    @DisplayName("Member attempting to access Completed Tasks Stack receives HTTP 403 Forbidden")
    void testMemberCannotAccessStackForbidden() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Admin required"))
                .when(authService).requireAdmin(request);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                taskController.getCompletedStack(request));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Member attempting to peek Completed Tasks Stack receives HTTP 403 Forbidden")
    void testMemberCannotPeekStackForbidden() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Admin required"))
                .when(authService).requireAdmin(request);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                taskController.peekStack(request));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Admin can access Popped Tasks History audit logs")
    void testAdminCanAccessStackHistory() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(authService.requireAdmin(request)).thenReturn(adminUser);
        when(stackService.getPoppedHistory()).thenReturn(List.of());

        var result = taskController.getPoppedTasksHistory(request);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Member attempting to access Popped Tasks History receives HTTP 403 Forbidden")
    void testMemberCannotAccessStackHistoryForbidden() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Admin required"))
                .when(authService).requireAdmin(request);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                taskController.getPoppedTasksHistory(request));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Unauthenticated request to Queue receives HTTP 401 Unauthorized")
    void testUnauthenticatedQueueReceivesUnauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required"))
                .when(authService).requireAdmin(request);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                taskController.getPendingQueue(request));

        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Unauthenticated request to Stack receives HTTP 401 Unauthorized")
    void testUnauthenticatedStackReceivesUnauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required"))
                .when(authService).requireAdmin(request);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                taskController.getCompletedStack(request));

        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("Linked List traversal for Member is strictly scoped to member's assigned tasks")
    void testTraverseLinkedListMemberScoped() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(authService.requireUser(request)).thenReturn(memberJohn);
        when(linkedListService.isEmpty()).thenReturn(false);
        when(linkedListService.traverse()).thenReturn(List.of(taskJohn, taskSubu));

        var result = taskController.traverseLinkedList(request);
        assertEquals(1, result.get("totalNodes"));
        @SuppressWarnings("unchecked")
        List<Task> tasks = (List<Task>) result.get("tasks");
        assertEquals(1, tasks.size());
        assertEquals("John", tasks.get(0).getMember());
        assertEquals(101, tasks.get(0).getId());
        assertTrue(result.get("visual").toString().contains("#101"));
        assertFalse(result.get("visual").toString().contains("#102"));
    }
}
