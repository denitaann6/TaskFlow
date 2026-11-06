package com.taskflow.taskflow_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "popped_tasks_history")
public class PoppedTaskHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "task_id", nullable = false)
    private Integer taskId;

    @Column(name = "task_name", nullable = false, length = 150)
    private String taskName;

    @Column(name = "member", nullable = false, length = 100)
    private String member;

    @Column(name = "priority", nullable = false, length = 20)
    private String priority;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "Completed";

    @Column(name = "popped_at", nullable = false)
    private LocalDateTime poppedAt;

    @Column(name = "popped_by", length = 100)
    private String poppedBy = "Administrator";

    @Column(name = "original_stack_order")
    private Long originalStackOrder;

    public PoppedTaskHistoryEntry() {
    }

    public PoppedTaskHistoryEntry(Integer taskId, String taskName, String member, String priority, String status, LocalDateTime poppedAt, String poppedBy, Long originalStackOrder) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.member = member;
        this.priority = priority;
        this.status = status != null ? status : "Completed";
        this.poppedAt = poppedAt != null ? poppedAt : LocalDateTime.now();
        this.poppedBy = poppedBy != null ? poppedBy : "Administrator";
        this.originalStackOrder = originalStackOrder;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getMember() {
        return member;
    }

    public void setMember(String member) {
        this.member = member;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPoppedAt() {
        return poppedAt;
    }

    public void setPoppedAt(LocalDateTime poppedAt) {
        this.poppedAt = poppedAt;
    }

    public String getPoppedBy() {
        return poppedBy;
    }

    public void setPoppedBy(String poppedBy) {
        this.poppedBy = poppedBy;
    }

    public Long getOriginalStackOrder() {
        return originalStackOrder;
    }

    public void setOriginalStackOrder(Long originalStackOrder) {
        this.originalStackOrder = originalStackOrder;
    }
}
