package com.campusops.controller.web;

import com.campusops.dto.TaskDTO;
import com.campusops.security.CustomUserDetails;
import com.campusops.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.campusops.entity.enums.TaskStatus;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Controller
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskWebController {

    private final TaskService taskService;

    @GetMapping
    public String listTasks(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String filter,
            @AuthenticationPrincipal CustomUserDetails userDetails, 
            Model model) {
        Long userId = userDetails.getUser().getId();
        
        List<TaskDTO> pendingTasks = taskService.getTasksByAssignee(userId, TaskStatus.PENDING, PageRequest.of(0, 100)).getContent();
        
        if ("high".equals(filter)) {
            pendingTasks = pendingTasks.stream()
                .filter(t -> "HIGH".equals(t.getPriority()))
                .toList();
        } else if ("overdue".equals(filter)) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            pendingTasks = pendingTasks.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(now))
                .toList();
        }
                
        model.addAttribute("tasks", pendingTasks);
        return "tasks/list";
    }

    @org.springframework.web.bind.annotation.PostMapping
    public String createTask(
            @org.springframework.web.bind.annotation.RequestParam String title,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String description,
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dueDate,
            @org.springframework.web.bind.annotation.RequestParam String priority,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        
        try {
            Long userId = userDetails.getUser().getId();
            java.time.LocalDateTime dueDateTime = dueDate != null ? dueDate.atTime(23, 59) : null;
            com.campusops.entity.enums.Priority taskPriority = com.campusops.entity.enums.Priority.valueOf(priority);
            
            taskService.createTask(null, userId, userId, title, description, dueDateTime, taskPriority);
            redirectAttributes.addFlashAttribute("success", "Task created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating task: " + e.getMessage());
        }
        
        return "redirect:/tasks";
    }
}
