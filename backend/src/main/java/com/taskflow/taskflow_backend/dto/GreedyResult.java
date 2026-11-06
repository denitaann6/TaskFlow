package com.taskflow.taskflow_backend.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Result DTO returned by the Greedy Algorithm service.
 */
public class GreedyResult {

    private int timeLimitHours;
    private int totalAllocatedHours;
    private int remainingHours;
    private int totalPriorityScore;
    private List<GreedyTaskItem> selectedTasks = new ArrayList<>();
    private List<GreedyTaskItem> candidateTasks = new ArrayList<>();
    private List<String> decisionTrace = new ArrayList<>();
    private String heuristicExplanation;
    private String optimalityDisclaimer;

    public GreedyResult() {
    }

    public int getTimeLimitHours() {
        return timeLimitHours;
    }

    public void setTimeLimitHours(int timeLimitHours) {
        this.timeLimitHours = timeLimitHours;
    }

    public int getTotalAllocatedHours() {
        return totalAllocatedHours;
    }

    public void setTotalAllocatedHours(int totalAllocatedHours) {
        this.totalAllocatedHours = totalAllocatedHours;
    }

    public int getRemainingHours() {
        return remainingHours;
    }

    public void setRemainingHours(int remainingHours) {
        this.remainingHours = remainingHours;
    }

    public int getTotalPriorityScore() {
        return totalPriorityScore;
    }

    public void setTotalPriorityScore(int totalPriorityScore) {
        this.totalPriorityScore = totalPriorityScore;
    }

    public List<GreedyTaskItem> getSelectedTasks() {
        return selectedTasks;
    }

    public void setSelectedTasks(List<GreedyTaskItem> selectedTasks) {
        this.selectedTasks = selectedTasks;
    }

    public List<GreedyTaskItem> getCandidateTasks() {
        return candidateTasks;
    }

    public void setCandidateTasks(List<GreedyTaskItem> candidateTasks) {
        this.candidateTasks = candidateTasks;
    }

    public List<String> getDecisionTrace() {
        return decisionTrace;
    }

    public void setDecisionTrace(List<String> decisionTrace) {
        this.decisionTrace = decisionTrace;
    }

    public String getHeuristicExplanation() {
        return heuristicExplanation;
    }

    public void setHeuristicExplanation(String heuristicExplanation) {
        this.heuristicExplanation = heuristicExplanation;
    }

    public String getOptimalityDisclaimer() {
        return optimalityDisclaimer;
    }

    public void setOptimalityDisclaimer(String optimalityDisclaimer) {
        this.optimalityDisclaimer = optimalityDisclaimer;
    }
}
