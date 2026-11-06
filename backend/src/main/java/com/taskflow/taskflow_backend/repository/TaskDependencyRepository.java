package com.taskflow.taskflow_backend.repository;

import com.taskflow.taskflow_backend.entity.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {

    boolean existsByPrerequisiteIdAndDependentId(Integer prerequisiteId, Integer dependentId);

    Optional<TaskDependency> findByPrerequisiteIdAndDependentId(Integer prerequisiteId, Integer dependentId);

    List<TaskDependency> findAllByOrderByPrerequisiteIdAscDependentIdAsc();

    List<TaskDependency> findByPrerequisiteId(Integer prerequisiteId);

    List<TaskDependency> findByDependentId(Integer dependentId);

    @Transactional
    @Modifying
    @Query("DELETE FROM TaskDependency td WHERE td.prerequisiteId = :fromId AND td.dependentId = :toId")
    void deleteDependencyEdge(@Param("fromId") Integer fromId, @Param("toId") Integer toId);

    @Transactional
    @Modifying
    @Query("DELETE FROM TaskDependency td WHERE td.prerequisiteId = :taskId OR td.dependentId = :taskId")
    void deleteByTaskId(@Param("taskId") Integer taskId);
}

