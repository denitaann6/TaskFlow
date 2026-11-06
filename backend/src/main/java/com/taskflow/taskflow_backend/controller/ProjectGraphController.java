package com.taskflow.taskflow_backend.controller;

import com.taskflow.taskflow_backend.entity.Task;
import com.taskflow.taskflow_backend.entity.TaskDependency;
import com.taskflow.taskflow_backend.repository.TaskDependencyRepository;
import com.taskflow.taskflow_backend.repository.TaskRepository;
import com.taskflow.taskflow_backend.service.AuthService;
import com.taskflow.taskflow_backend.service.TaskGraphService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Dedicated REST Controller for Phase 2.2: Project Graph & Task Dependencies.
 * Strictly restricts graph representation and BFS traversal operations to authenticated Administrators.
 */
@RestController
@RequestMapping("/api/admin/tasks/graph")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ProjectGraphController {

    private final TaskRepository taskRepository;
    private final TaskDependencyRepository dependencyRepository;
    private final TaskGraphService graphService;
    private final AuthService authService;

    public ProjectGraphController(TaskRepository taskRepository,
                                  TaskDependencyRepository dependencyRepository,
                                  TaskGraphService graphService,
                                  AuthService authService) {
        this.taskRepository = taskRepository;
        this.dependencyRepository = dependencyRepository;
        this.graphService = graphService;
        this.authService = authService;
    }

    /**
     * Get complete graph state: adjacency list, dependencies list, and task nodes.
     * Accessible to ADMIN only.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getProjectGraph(HttpServletRequest httpRequest) {
        authService.requireAdmin(httpRequest);

        List<Task> allTasks = taskRepository.findAll();
        Map<Integer, Task> taskMap = new HashMap<>();
        for (Task t : allTasks) {
            taskMap.put(t.getId(), t);
            graphService.ensureNode(t.getId());
        }

        Map<Integer, List<Integer>> adjList = graphService.getGraph();

        // Build list of rich dependency objects
        List<Map<String, Object>> dependencyList = new ArrayList<>();
        List<TaskDependency> storedDeps = dependencyRepository.findAllByOrderByPrerequisiteIdAscDependentIdAsc();

        for (TaskDependency dep : storedDeps) {
            Task fromTask = taskMap.get(dep.getPrerequisiteId());
            Task toTask = taskMap.get(dep.getDependentId());

            Map<String, Object> edge = new LinkedHashMap<>();
            edge.put("id", dep.getId());
            edge.put("prerequisiteId", dep.getPrerequisiteId());
            edge.put("prerequisiteName", fromTask != null ? fromTask.getName() : "Task #" + dep.getPrerequisiteId());
            edge.put("dependentId", dep.getDependentId());
            edge.put("dependentName", toTask != null ? toTask.getName() : "Task #" + dep.getDependentId());
            edge.put("createdAt", dep.getCreatedAt());
            dependencyList.add(edge);
        }

        // Summary metrics
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalTasks", allTasks.size());
        response.put("totalDependencies", dependencyList.size());
        response.put("adjacencyList", adjList);
        response.put("dependencies", dependencyList);
        response.put("tasks", allTasks);

        return ResponseEntity.ok(response);
    }

    /**
     * Add a directed dependency edge: fromId (prerequisite) -> toId (dependent).
     * Prevents self-loops, missing tasks, and duplicate dependencies.
     * Accessible to ADMIN only.
     */
    @PostMapping("/dependency")
    public ResponseEntity<Map<String, Object>> addDependency(
            @RequestParam(required = false) Integer fromId,
            @RequestParam(required = false) Integer toId,
            HttpServletRequest httpRequest) {
        authService.requireAdmin(httpRequest);

        if (fromId == null || toId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both fromId (prerequisite) and toId (dependent) are required");
        }

        if (fromId.equals(toId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A task cannot depend on itself (self-dependency is not allowed)");
        }

        boolean fromExists = taskRepository.existsById(fromId);
        boolean toExists = taskRepository.existsById(toId);

        if (!fromExists && !toExists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Neither Task ID " + fromId + " nor Task ID " + toId + " exists in the database");
        }
        if (!fromExists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prerequisite Task ID " + fromId + " does not exist in the database");
        }
        if (!toExists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dependent Task ID " + toId + " does not exist in the database");
        }

        boolean added = graphService.addDependency(fromId, toId);
        if (!added) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dependency edge already exists: Task #" + fromId + " -> Task #" + toId);
        }

        Task fromTask = taskRepository.findById(fromId).orElse(null);
        Task toTask = taskRepository.findById(toId).orElse(null);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", String.format("Dependency created: Task #%d (\"%s\") must precede Task #%d (\"%s\").",
                fromId, fromTask != null ? fromTask.getName() : "", toId, toTask != null ? toTask.getName() : ""));
        response.put("fromId", fromId);
        response.put("toId", toId);

        return ResponseEntity.ok(response);
    }

    /**
     * Remove a single directed dependency edge.
     * Accessible to ADMIN only.
     */
    @DeleteMapping("/dependency")
    public ResponseEntity<Map<String, Object>> removeDependency(
            @RequestParam(required = false) Integer fromId,
            @RequestParam(required = false) Integer toId,
            HttpServletRequest httpRequest) {
        authService.requireAdmin(httpRequest);

        if (fromId == null || toId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both fromId and toId are required");
        }

        boolean removed = graphService.removeDependency(fromId, toId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", removed);
        response.put("message", removed ?
                String.format("Dependency Task #%d -> Task #%d removed.", fromId, toId) :
                "Dependency edge not found.");

        return ResponseEntity.ok(response);
    }

    /**
     * Run Breadth-First Search (BFS) from a chosen start task.
     * Accessible to ADMIN only.
     */
    @GetMapping("/bfs")
    public ResponseEntity<Map<String, Object>> runBFS(
            @RequestParam(required = false) Integer startId,
            HttpServletRequest httpRequest) {
        authService.requireAdmin(httpRequest);

        if (startId == null || startId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please specify a valid startId for BFS traversal");
        }

        Optional<Task> startOpt = taskRepository.findById(startId);
        if (startOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Start Task ID #" + startId + " does not exist in the database");
        }

        Task startTask = startOpt.get();
        List<Integer> bfsOrder = graphService.bfsTraversal(startId);

        // Fetch full task details for each visited node in order
        List<Map<String, Object>> visitedTasks = new ArrayList<>();
        for (Integer taskId : bfsOrder) {
            Optional<Task> tOpt = taskRepository.findById(taskId);
            if (tOpt.isPresent()) {
                Task t = tOpt.get();
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", t.getId());
                item.put("name", t.getName());
                item.put("member", t.getMember());
                item.put("priority", t.getPriority());
                item.put("status", t.getStatus());
                visitedTasks.add(item);
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("startId", startId);
        response.put("startTaskName", startTask.getName());
        response.put("bfsOrder", bfsOrder);
        response.put("visitedTasks", visitedTasks);
        response.put("count", bfsOrder.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Clear all task dependencies.
     * Accessible to ADMIN only.
     */
    @PostMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearGraph(HttpServletRequest httpRequest) {
        authService.requireAdmin(httpRequest);
        graphService.clear();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "All task dependencies have been cleared successfully.");

        return ResponseEntity.ok(response);
    }
}

