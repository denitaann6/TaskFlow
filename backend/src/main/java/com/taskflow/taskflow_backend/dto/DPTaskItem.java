package com.taskflow.taskflow_backend.dto;

/**
 * DTO representing a task evaluated by the Dynamic Programming 0/1 Knapsack algorithm.
 */
public class DPTaskItem {

    private Integer taskId;
    private String taskName;
    private String priority;
    private String member;
    private String status;
    private int sampleDurationHours;
    private int sampleBenefitValue;

    public DPTaskItem() {
    }

    public DPTaskItem(Integer taskId, String taskName, String priority, String member, String status,
                      int sampleDurationHours, int sampleBenefitValue) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.priority = priority;
        this.member = member;
        this.status = status;
        this.sampleDurationHours = sampleDurationHours;
        this.sampleBenefitValue = sampleBenefitValue;
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

    public int getSampleDurationHours() {
        return sampleDurationHours;
    }

    public void setSampleDurationHours(int sampleDurationHours) {
        this.sampleDurationHours = sampleDurationHours;
    }

    public int getSampleBenefitValue() {
        return sampleBenefitValue;
    }

    public void setSampleBenefitValue(int sampleBenefitValue) {
        this.sampleBenefitValue = sampleBenefitValue;
    }

    public int getEstimatedDuration() {
        return sampleDurationHours;
    }

    public int getBenefit() {
        return sampleBenefitValue;
    }
}
