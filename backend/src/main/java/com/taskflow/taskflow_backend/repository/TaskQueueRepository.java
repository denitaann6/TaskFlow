package com.taskflow.taskflow_backend.repository;

import com.taskflow.taskflow_backend.entity.TaskQueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface TaskQueueRepository extends JpaRepository<TaskQueueEntry, Integer> {

    List<TaskQueueEntry> findAllByOrderBySequenceOrderAsc();

    @Query("SELECT q FROM TaskQueueEntry q WHERE q.task.id = :taskId")
    Optional<TaskQueueEntry> findByTaskId(@Param("taskId") Integer taskId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TaskQueueEntry q WHERE q.task.id = :taskId")
    void deleteByTaskId(@Param("taskId") Integer taskId);

    @Query("SELECT COALESCE(MAX(q.sequenceOrder), 0) FROM TaskQueueEntry q")
    Long findMaxSequenceOrder();
}
