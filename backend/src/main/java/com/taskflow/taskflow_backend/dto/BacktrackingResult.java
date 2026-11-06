package com.taskflow.taskflow_backend.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result DTO returned by the Backtracking Dependency Planning service.
 */
public class BacktrackingResult {

    private int totalTasks;
    private int maxSchedulesRequested;
    private int schedulesFound;
    private boolean cycleDetected;
    private int branchesPruned;
    private List<List<Integer>> validOrderings = new ArrayList<>();
    private Map<Integer, String> taskNames = new HashMap<>();
    private Map<Integer, List<Integer>> dependencyGraph = new HashMap<>();
    private String dependencyDirectionExplanation;
    private String algorithmExplanation;
    private String cycleMessage;

    public BacktrackingResult() {
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }

    public int getMaxSchedulesRequested() {
        return maxSchedulesRequested;
    }

    public void setMaxSchedulesRequested(int maxSchedulesRequested) {
        this.maxSchedulesRequested = maxSchedulesRequested;
    }

    public int getSchedulesFound() {
        return schedulesFound;
    }

    public void setSchedulesFound(int schedulesFound) {
        this.schedulesFound = schedulesFound;
    }

    public boolean isCycleDetected() {
        return cycleDetected;
    }

    public void setCycleDetected(boolean cycleDetected) {
        this.cycleDetected = cycleDetected;
    }

    public int getBranchesPruned() {
        return branchesPruned;
    }

    public void setBranchesPruned(int branchesPruned) {
        this.branchesPruned = branchesPruned;
    }

    public List<List<Integer>> getValidOrderings() {
        return validOrderings;
    }

    public void setValidOrderings(List<List<Integer>> validOrderings) {
        this.validOrderings = validOrderings;
    }

    public Map<Integer, String> getTaskNames() {
        return taskNames;
    }

    public void setTaskNames(Map<Integer, String> taskNames) {
        this.taskNames = taskNames;
    }

    public Map<Integer, List<Integer>> getDependencyGraph() {
        return dependencyGraph;
    }

    public void setDependencyGraph(Map<Integer, List<Integer>> dependencyGraph) {
        this.dependencyGraph = dependencyGraph;
    }

    public String getDependencyDirectionExplanation() {
        return dependencyDirectionExplanation;
    }

    public void setDependencyDirectionExplanation(String dependencyDirectionExplanation) {
        this.dependencyDirectionExplanation = dependencyDirectionExplanation;
    }

    public String getAlgorithmExplanation() {
        return algorithmExplanation;
    }

    public void setAlgorithmExplanation(String algorithmExplanation) {
        this.algorithmExplanation = algorithmExplanation;
    }

    public String getCycleMessage() {
        return cycleMessage;
    }

    public void setCycleMessage(String cycleMessage) {
        this.cycleMessage = cycleMessage;
    }
}
