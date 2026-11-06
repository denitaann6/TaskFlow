package com.taskflow.taskflow_backend.controller;

import com.taskflow.taskflow_backend.dto.BacktrackingResult;
import com.taskflow.taskflow_backend.dto.DPResult;
import com.taskflow.taskflow_backend.dto.DPTaskItem;
import com.taskflow.taskflow_backend.dto.GreedyResult;
import com.taskflow.taskflow_backend.dto.GreedyTaskItem;
import com.taskflow.taskflow_backend.entity.Task;
import com.taskflow.taskflow_backend.repository.TaskRepository;
import com.taskflow.taskflow_backend.service.AuthService;
import com.taskflow.taskflow_backend.service.TaskBacktrackingService;
import com.taskflow.taskflow_backend.service.TaskDPService;
import com.taskflow.taskflow_backend.service.TaskGraphService;
import com.taskflow.taskflow_backend.service.TaskGreedyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Phase 4 Advanced Algorithms:
 * 1. Greedy Algorithm: Priority-Density Pending Task Selection.
 * 2. Dynamic Programming: 0/1 Knapsack Task Allocation within Time Limit.
 * 3. Backtracking: Dependency Planning & Topological Execution Ordering.
 */
@RestController
@RequestMapping("/api/tasks/phase4")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class Phase4Controller {

    private final TaskRepository taskRepository;
    private final TaskGraphService graphService;
    private final TaskGreedyService greedyService;
    private final TaskDPService dpService;
    private final TaskBacktrackingService backtrackingService;
    private final AuthService authService;

    public Phase4Controller(TaskRepository taskRepository,
                            TaskGraphService graphService,
                            TaskGreedyService greedyService,
                            TaskDPService dpService,
                            TaskBacktrackingService backtrackingService,
                            AuthService authService) {
        this.taskRepository = taskRepository;
        this.graphService = graphService;
        this.greedyService = greedyService;
        this.dpService = dpService;
        this.backtrackingService = backtrackingService;
        this.authService = authService;
    }

    // ==========================================
    // 1. GREEDY ALGORITHM ENDPOINTS
    // ==========================================

    @GetMapping("/greedy")
    public GreedyResult getGreedyPendingSchedule(
            @RequestParam(defaultValue = "8") int timeLimit,
            HttpServletRequest httpRequest) {
        authService.requireUser(httpRequest);
        List<Task> tasks = taskRepository.findAll();
        return greedyService.selectPendingTasksGreedy(tasks, timeLimit);
    }

    public static class CustomGreedyRequest {
        public int timeLimit = 8;
        public List<GreedyTaskItem> tasks;
    }

    @PostMapping("/greedy")
    public GreedyResult calculateCustomGreedy(
            @RequestBody CustomGreedyRequest request,
            HttpServletRequest httpRequest) {
        authService.requireUser(httpRequest);
        int limit = request != null ? request.timeLimit : 8;
        List<GreedyTaskItem> items = request != null ? request.tasks : List.of();
        return greedyService.selectCustomTasksGreedy(items, limit);
    }

    // ==========================================
    // 2. DYNAMIC PROGRAMMING ENDPOINTS
    // ==========================================

    @GetMapping("/dp")
    public DPResult getDPAllocation(
            @RequestParam(defaultValue = "8") int timeLimit,
            HttpServletRequest httpRequest) {
        authService.requireUser(httpRequest);
        List<Task> tasks = taskRepository.findAll();
        return dpService.allocateTasksDP(tasks, timeLimit);
    }

    public static class CustomDPRequest {
        public int timeLimit = 8;
        public List<DPTaskItem> tasks;
    }

    @PostMapping("/dp")
    public DPResult calculateCustomDP(
            @RequestBody CustomDPRequest request,
            HttpServletRequest httpRequest) {
        authService.requireUser(httpRequest);
        int limit = request != null ? request.timeLimit : 8;
        List<DPTaskItem> items = request != null ? request.tasks : List.of();
        return dpService.allocateCustomTasksDP(items, limit);
    }

    // ==========================================
    // 3. BACKTRACKING ENDPOINTS
    // ==========================================

    @GetMapping("/backtracking")
    public BacktrackingResult getBacktrackingSchedules(
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest httpRequest) {
        authService.requireUser(httpRequest);
        List<Task> tasks = taskRepository.findAll();
        Map<Integer, List<Integer>> currentGraph = graphService.getGraph();
        return backtrackingService.planSchedules(tasks, currentGraph, limit);
    }

    public static class CustomBacktrackingRequest {
        public List<Integer> taskIds;
        public Map<Integer, List<Integer>> graph;
        public Map<Integer, String> taskNames;
        public int limit = 10;
    }

    @PostMapping("/backtracking")
    public BacktrackingResult calculateCustomBacktracking(
            @RequestBody CustomBacktrackingRequest request,
            HttpServletRequest httpRequest) {
        authService.requireUser(httpRequest);
        List<Integer> ids = request != null ? request.taskIds : List.of();
        Map<Integer, List<Integer>> graph = request != null ? request.graph : Map.of();
        Map<Integer, String> names = request != null ? request.taskNames : Map.of();
        int limit = request != null ? request.limit : 10;
        return backtrackingService.planCustomSchedules(ids, graph, names, limit);
    }
}
