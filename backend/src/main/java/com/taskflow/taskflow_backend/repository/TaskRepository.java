
package com.taskflow.taskflow_backend.repository;

import com.taskflow.taskflow_backend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Integer> {
}
