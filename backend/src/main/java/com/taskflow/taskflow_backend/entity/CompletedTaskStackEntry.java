package com.taskflow.taskflow_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "completed_task_stack")
public class CompletedTaskStackEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "task_id", referencedColumnName = "id", nullable = false, unique = true)
    private Task task;

    @Column(name = "stack_order", nullable = false)
    private Long stackOrder;

    @Column(name = "pushed_at", nullable = false)
    private LocalDateTime pushedAt;

    public CompletedTaskStackEntry() {
    }

    public CompletedTaskStackEntry(Task task, Long stackOrder, LocalDateTime pushedAt) {
        this.task = task;
        this.stackOrder = stackOrder;
        this.pushedAt = pushedAt != null ? pushedAt : LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Long getStackOrder() {
        return stackOrder;
    }

    public void setStackOrder(Long stackOrder) {
        this.stackOrder = stackOrder;
    }

    public LocalDateTime getPushedAt() {
        return pushedAt;
    }

    public void setPushedAt(LocalDateTime pushedAt) {
        this.pushedAt = pushedAt;
    }
}
