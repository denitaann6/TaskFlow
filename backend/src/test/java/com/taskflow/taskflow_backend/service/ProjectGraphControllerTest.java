package com.taskflow.taskflow_backend.service;

import com.taskflow.taskflow_backend.controller.ProjectGraphController;
import com.taskflow.taskflow_backend.entity.Task;
import com.taskflow.taskflow_backend.entity.User;
import com.taskflow.taskflow_backend.repository.TaskDependencyRepository;
import com.taskflow.taskflow_backend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectGraphControllerTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskDependencyRepository dependencyRepository;

    @Mock
    private AuthService authService;

    private TaskGraphService graphService;
    private ProjectGraphController controller;

    private User adminEntity;
    private Task task1;
    private Task task2;
    private Task task3;

    private Task createTask(int id, String name, String member, String priority, String status) {
        Task t = new Task();
        t.setId(id);
        t.setName(name);
        t.setMember(member);
        t.setPriority(priority);
        t.setStatus(status);
        return t;
    }

    @BeforeEach
    void setUp() {
        graphService = new TaskGraphService();
        controller = new ProjectGraphController(taskRepository, dependencyRepository, graphService, authService);

        adminEntity = new User("admin", "hash", "System Administrator", "ADMIN", true, "Administrator");
        adminEntity.setId(1);

        task1 = createTask(101, "Database Design", "John", "High", "In Progress");
        task2 = createTask(102, "trial", "subu", "High", "Completed");
        task3 = createTask(103, "Bug Checking", "Sheena", "High", "In Progress");
    }

    @Test
    @DisplayName("ADMIN can retrieve project graph with task nodes and adjacency list")
    void testAdminGetProjectGraph() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(authService.requireAdmin(request)).thenReturn(adminEntity);
        when(taskRepository.findAll()).thenReturn(List.of(task1, task2, task3));
        when(dependencyRepository.findAllByOrderByPrerequisiteIdAscDependentIdAsc()).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = controller.getProjectGraph(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().get("totalTasks"));
        assertEquals(0, response.getBody().get("totalDependencies"));
        assertNotNull(response.getBody().get("adjacencyList"));
    }

    @Test
    @DisplayName("ADMIN can add a directed dependency successfully")
    void testAdminAddDependencySuccess() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(authService.requireAdmin(request)).thenReturn(adminEntity);
        when(taskRepository.existsById(101)).thenReturn(true);
        when(taskRepository.existsById(102)).thenReturn(true);
        when(taskRepository.findById(101)).thenReturn(Optional.of(task1));
        when(taskRepository.findById(102)).thenReturn(Optional.of(task2));

        ResponseEntity<Map<String, Object>> response = controller.addDependency(101, 102, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(101, response.getBody().get("fromId"));
        assertEquals(102, response.getBody().get("toId"));
    }

    @Test
    @DisplayName("Self-dependency is rejected with 400 Bad Request")
    void testSelfDependencyRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(authService.requireAdmin(request)).thenReturn(adminEntity);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.addDependency(101, 101, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("self-dependency is not allowed"));
    }

    @Test
    @DisplayName("Duplicate dependency edge is rejected with 400 Bad Request")
    void testDuplicateDependencyRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(authService.requireAdmin(request)).thenReturn(adminEntity);
        when(taskRepository.existsById(101)).thenReturn(true);
        when(taskRepository.existsById(102)).thenReturn(true);

        graphService.addDependency(101, 102);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.addDependency(101, 102, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Dependency edge already exists"));
    }

    @Test
    @DisplayName("Dependency referencing nonexistent task is rejected with 404 Not Found")
    void testNonexistentTaskDependencyRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(authService.requireAdmin(request)).thenReturn(adminEntity);
        when(taskRepository.existsById(101)).thenReturn(true);
        when(taskRepository.existsById(999)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.addDependency(101, 999, request));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Dependent Task ID 999 does not exist"));
    }

    @Test
    @DisplayName("ADMIN can run BFS traversal from valid start node")
    void testAdminRunBFSSuccess() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(authService.requireAdmin(request)).thenReturn(adminEntity);
        when(taskRepository.findById(101)).thenReturn(Optional.of(task1));
        when(taskRepository.findById(102)).thenReturn(Optional.of(task2));

        graphService.addDependency(101, 102);

        ResponseEntity<Map<String, Object>> response = controller.runBFS(101, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(101, response.getBody().get("startId"));
        assertEquals(2, response.getBody().get("count"));

        @SuppressWarnings("unchecked")
        List<Integer> order = (List<Integer>) response.getBody().get("bfsOrder");
        assertEquals(List.of(101, 102), order);
    }

    @Test
    @DisplayName("BFS with nonexistent start node is rejected with 404 Not Found")
    void testBFSNonexistentStartNodeRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(authService.requireAdmin(request)).thenReturn(adminEntity);
        when(taskRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.runBFS(999, request));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("MEMBER request to Project Graph is rejected with 403 Forbidden")
    void testMemberForbidden() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Administrator privileges required."))
                .when(authService).requireAdmin(request);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getProjectGraph(request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(taskRepository, never()).findAll();
    }

    @Test
    @DisplayName("Unauthenticated request to Project Graph is rejected with 401 Unauthorized")
    void testUnauthenticatedUnauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required. Please log in."))
                .when(authService).requireAdmin(request);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getProjectGraph(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(taskRepository, never()).findAll();
    }
}
