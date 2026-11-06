package com.taskflow.taskflow_backend.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Result DTO returned by the Dynamic Programming 0/1 Knapsack service.
 */
public class DPResult {

    private int timeLimitHours;
    private int totalDurationUsed;
    private int remainingHours;
    private int maxBenefitAchieved;
    private List<DPTaskItem> selectedTasks = new ArrayList<>();
    private List<DPTaskItem> candidateTasks = new ArrayList<>();
    private String algorithmType = "0/1 Knapsack Tabulation (Dynamic Programming)";
    private String sampleMetricsExplanation;
    private String dpRecurrence;
    private List<String> dpSteps = new ArrayList<>();

    public DPResult() {
    }

    public int getTimeLimitHours() {
        return timeLimitHours;
    }

    public void setTimeLimitHours(int timeLimitHours) {
        this.timeLimitHours = timeLimitHours;
    }

    public int getTotalDurationUsed() {
        return totalDurationUsed;
    }

    public void setTotalDurationUsed(int totalDurationUsed) {
        this.totalDurationUsed = totalDurationUsed;
    }

    public int getRemainingHours() {
        return remainingHours;
    }

    public void setRemainingHours(int remainingHours) {
        this.remainingHours = remainingHours;
    }

    public int getMaxBenefitAchieved() {
        return maxBenefitAchieved;
    }

    public void setMaxBenefitAchieved(int maxBenefitAchieved) {
        this.maxBenefitAchieved = maxBenefitAchieved;
    }

    public List<DPTaskItem> getSelectedTasks() {
        return selectedTasks;
    }

    public void setSelectedTasks(List<DPTaskItem> selectedTasks) {
        this.selectedTasks = selectedTasks;
    }

    public List<DPTaskItem> getCandidateTasks() {
        return candidateTasks;
    }

    public void setCandidateTasks(List<DPTaskItem> candidateTasks) {
        this.candidateTasks = candidateTasks;
    }

    public String getAlgorithmType() {
        return algorithmType;
    }

    public void setAlgorithmType(String algorithmType) {
        this.algorithmType = algorithmType;
    }

    public String getSampleMetricsExplanation() {
        return sampleMetricsExplanation;
    }

    public void setSampleMetricsExplanation(String sampleMetricsExplanation) {
        this.sampleMetricsExplanation = sampleMetricsExplanation;
    }

    public String getDpRecurrence() {
        return dpRecurrence;
    }

    public void setDpRecurrence(String dpRecurrence) {
        this.dpRecurrence = dpRecurrence;
    }

    public List<String> getDpSteps() {
        return dpSteps;
    }

    public void setDpSteps(List<String> dpSteps) {
        this.dpSteps = dpSteps;
    }
}
