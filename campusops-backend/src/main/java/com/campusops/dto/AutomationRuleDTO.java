package com.campusops.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationRuleDTO {
    private Long id;
    private String name;
    private String description;
    private String triggerType;
    private String triggerConfig;
    private String actionType;
    private String actionConfig;
    private boolean active;
    private String createdByName;
    private Long createdById;
    private String lastRunAt;
    private int runCount;
    private String createdAt;
    private String updatedAt;
}
