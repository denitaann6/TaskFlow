package com.taskflow.taskflow_backend.repository;

import com.taskflow.taskflow_backend.entity.CompletedTaskStackEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CompletedTaskStackRepository extends JpaRepository<CompletedTaskStackEntry, Integer> {

    List<CompletedTaskStackEntry> findAllByOrderByStackOrderDesc();

    Optional<CompletedTaskStackEntry> findTopByOrderByStackOrderDesc();

    @Query("SELECT s FROM CompletedTaskStackEntry s WHERE s.task.id = :taskId")
    Optional<CompletedTaskStackEntry> findByTaskId(@Param("taskId") Integer taskId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CompletedTaskStackEntry s WHERE s.task.id = :taskId")
    void deleteByTaskId(@Param("taskId") Integer taskId);

    @Query("SELECT COALESCE(MAX(s.stackOrder), 0) FROM CompletedTaskStackEntry s")
    Long findMaxStackOrder();
}
