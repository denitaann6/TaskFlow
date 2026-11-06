package com.taskflow.taskflow_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a directed dependency relationship between two tasks.
 * prerequisiteId must be executed before dependentId (prerequisiteId -> dependentId).
 */
@Entity
@Table(name = "task_dependencies", uniqueConstraints = {
    @UniqueConstraint(name = "uq_task_dependency", columnNames = {"prerequisite_id", "dependent_id"})
})
public class TaskDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prerequisite_id", nullable = false)
    private Integer prerequisiteId;

    @Column(name = "dependent_id", nullable = false)
    private Integer dependentId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TaskDependency() {
    }

    public TaskDependency(Integer prerequisiteId, Integer dependentId) {
        this.prerequisiteId = prerequisiteId;
        this.dependentId = dependentId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getPrerequisiteId() {
        return prerequisiteId;
    }

    public void setPrerequisiteId(Integer prerequisiteId) {
        this.prerequisiteId = prerequisiteId;
    }

    public Integer getDependentId() {
        return dependentId;
    }

    public void setDependentId(Integer dependentId) {
        this.dependentId = dependentId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

