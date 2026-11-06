package com.taskflow.taskflow_backend.repository;

import com.taskflow.taskflow_backend.entity.PoppedTaskHistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PoppedTaskHistoryRepository extends JpaRepository<PoppedTaskHistoryEntry, Integer> {

    List<PoppedTaskHistoryEntry> findAllByOrderByPoppedAtDesc();

    List<PoppedTaskHistoryEntry> findByTaskIdOrderByPoppedAtDesc(Integer taskId);

    boolean existsByTaskId(Integer taskId);
}
