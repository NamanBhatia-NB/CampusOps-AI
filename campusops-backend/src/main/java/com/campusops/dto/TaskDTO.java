package com.campusops.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {
    private Long id;
    private Long leadId;
    private String leadName;
    private Long assignedToId;
    private String assignedToName;
    private Long createdById;
    private String createdByName;
    private String title;
    private String description;
    private java.time.LocalDateTime dueDate;
    private String status;
    private String priority;
    private java.time.LocalDateTime completedAt;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}
