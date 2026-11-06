package com.taskflow.taskflow_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_queue")
public class TaskQueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "task_id", referencedColumnName = "id", nullable = false, unique = true)
    private Task task;

    @Column(name = "sequence_order", nullable = false)
    private Long sequenceOrder;

    @Column(name = "enqueued_at", nullable = false)
    private LocalDateTime enqueuedAt;

    public TaskQueueEntry() {
    }

    public TaskQueueEntry(Task task, Long sequenceOrder, LocalDateTime enqueuedAt) {
        this.task = task;
        this.sequenceOrder = sequenceOrder;
        this.enqueuedAt = enqueuedAt != null ? enqueuedAt : LocalDateTime.now();
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

    public Long getSequenceOrder() {
        return sequenceOrder;
    }

    public void setSequenceOrder(Long sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }

    public LocalDateTime getEnqueuedAt() {
        return enqueuedAt;
    }

    public void setEnqueuedAt(LocalDateTime enqueuedAt) {
        this.enqueuedAt = enqueuedAt;
    }
}
