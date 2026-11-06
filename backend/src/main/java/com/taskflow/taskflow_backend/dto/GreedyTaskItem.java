package com.taskflow.taskflow_backend.dto;

/**
 * DTO representing a task evaluated by the Greedy algorithm.
 */
public class GreedyTaskItem {

    private Integer taskId;
    private String taskName;
    private String priority;
    private String member;
    private String status;
    private int priorityWeight;
    private int sampleEffortHours;
    private double densityRatio;

    public GreedyTaskItem() {
    }

    public GreedyTaskItem(Integer taskId, String taskName, String priority, String member, String status,
                          int priorityWeight, int sampleEffortHours, double densityRatio) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.priority = priority;
        this.member = member;
        this.status = status;
        this.priorityWeight = priorityWeight;
        this.sampleEffortHours = sampleEffortHours;
        this.densityRatio = densityRatio;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getMember() {
        return member;
    }

    public void setMember(String member) {
        this.member = member;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPriorityWeight() {
        return priorityWeight;
    }

    public void setPriorityWeight(int priorityWeight) {
        this.priorityWeight = priorityWeight;
    }

    public int getSampleEffortHours() {
        return sampleEffortHours;
    }

    public void setSampleEffortHours(int sampleEffortHours) {
        this.sampleEffortHours = sampleEffortHours;
    }

    public double getDensityRatio() {
        return densityRatio;
    }

    public void setDensityRatio(double densityRatio) {
        this.densityRatio = densityRatio;
    }

    public int getEstimatedDuration() {
        return sampleEffortHours;
    }

    public int getBenefit() {
        return priorityWeight;
    }

    public double getBenefitToTimeRatio() {
        return densityRatio;
    }
}
