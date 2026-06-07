package com.campusops.repository;

import com.campusops.entity.Task;
import com.campusops.entity.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByAssignedToIdAndStatus(Long assignedToId, TaskStatus status);

    org.springframework.data.domain.Page<Task> findByAssignedToIdAndStatus(Long assignedToId, TaskStatus status, org.springframework.data.domain.Pageable pageable);

    List<Task> findByLeadId(Long leadId);

    List<Task> findByDueDateBeforeAndStatusNot(LocalDateTime dueDate, TaskStatus status);

    long countByAssignedToIdAndStatus(Long assignedToId, TaskStatus status);
}
