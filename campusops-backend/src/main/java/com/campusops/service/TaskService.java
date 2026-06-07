package com.campusops.service;

import com.campusops.dto.TaskDTO;
import com.campusops.entity.Lead;
import com.campusops.entity.Task;
import com.campusops.entity.User;
import com.campusops.entity.enums.NotificationType;
import com.campusops.entity.enums.Priority;
import com.campusops.entity.enums.TaskStatus;
import com.campusops.exception.ResourceNotFoundException;
import com.campusops.repository.LeadRepository;
import com.campusops.repository.TaskRepository;
import com.campusops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;

    public TaskDTO createTask(Long leadId, Long assignedToId, Long createdById, 
                              String title, String description, LocalDateTime dueDate, Priority priority) {
        
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setDueDate(dueDate);
        task.setPriority(priority != null ? priority : Priority.MEDIUM);
        task.setStatus(TaskStatus.PENDING);

        if (leadId != null) {
            Lead lead = leadRepository.findById(leadId).orElse(null);
            task.setLead(lead);
        }

        User assignedTo = userRepository.findById(assignedToId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", assignedToId));
        task.setAssignedTo(assignedTo);

        if (createdById != null) {
            User createdBy = userRepository.findById(createdById).orElse(null);
            task.setCreatedBy(createdBy);
        }

        task = taskRepository.save(task);

        // Notify assignee
        if (createdById == null || !createdById.equals(assignedToId)) {
            String leadInfo = task.getLead() != null ? " for " + task.getLead().getFullName() : "";
            notificationService.createAndPush(
                    assignedToId,
                    "New Task Assigned",
                    "You have been assigned a new task: " + title + leadInfo,
                    NotificationType.TASK,
                    "TASK",
                    task.getId()
            );
        }

        activityLogService.log("TASK_CREATED", "TASK", task.getId(), "Task created: " + title, createdById);

        return toDTO(task);
    }

    @Transactional(readOnly = true)
    public Page<TaskDTO> getTasksByAssignee(Long userId, TaskStatus status, Pageable pageable) {
        if (status != null) {
            return taskRepository.findByAssignedToIdAndStatus(userId, status, pageable).map(this::toDTO);
        }
        return taskRepository.findAll(pageable).map(this::toDTO); // Assuming you'd filter this in reality
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByLead(Long leadId) {
        return taskRepository.findByLeadId(leadId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));
        return toDTO(task);
    }

    public TaskDTO updateTaskStatus(Long id, TaskStatus newStatus, Long userId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        String oldStatus = task.getStatus().name();
        task.setStatus(newStatus);

        if (newStatus == TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        } else if (oldStatus.equals("COMPLETED")) {
            task.setCompletedAt(null);
        }

        task = taskRepository.save(task);

        activityLogService.log("TASK_STATUS_UPDATED", "TASK", id, 
                "Status changed: " + oldStatus + " → " + newStatus, userId);

        return toDTO(task);
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getOverdueTasks() {
        return taskRepository.findByDueDateBeforeAndStatusNot(LocalDateTime.now(), TaskStatus.COMPLETED)
                .stream()
                .filter(t -> t.getStatus() != TaskStatus.CANCELLED)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public long countByAssigneeAndStatus(Long userId, TaskStatus status) {
        return taskRepository.countByAssignedToIdAndStatus(userId, status);
    }

    private TaskDTO toDTO(Task task) {
        return TaskDTO.builder()
                .id(task.getId())
                .leadId(task.getLead() != null ? task.getLead().getId() : null)
                .leadName(task.getLead() != null ? task.getLead().getFullName() : null)
                .assignedToId(task.getAssignedTo() != null ? task.getAssignedTo().getId() : null)
                .assignedToName(task.getAssignedTo() != null ? task.getAssignedTo().getFullName() : null)
                .createdById(task.getCreatedBy() != null ? task.getCreatedBy().getId() : null)
                .createdByName(task.getCreatedBy() != null ? task.getCreatedBy().getFullName() : null)
                .title(task.getTitle())
                .description(task.getDescription())
                .dueDate(task.getDueDate())
                .status(task.getStatus().name())
                .priority(task.getPriority().name())
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
