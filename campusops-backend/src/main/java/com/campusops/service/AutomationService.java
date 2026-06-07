package com.campusops.service;

import com.campusops.dto.AutomationRuleDTO;
import com.campusops.entity.AutomationRule;
import com.campusops.entity.Lead;
import com.campusops.entity.RuleExecution;
import com.campusops.entity.User;
import com.campusops.entity.enums.*;
import com.campusops.exception.ResourceNotFoundException;
import com.campusops.repository.AutomationRuleRepository;
import com.campusops.repository.LeadRepository;
import com.campusops.repository.RuleExecutionRepository;
import com.campusops.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AutomationService {

    private final AutomationRuleRepository ruleRepository;
    private final RuleExecutionRepository executionRepository;
    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final TaskService taskService;
    private final NotificationService notificationService;
    private final LeadService leadService;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;
    private final ActivityLogService activityLogService;

    public AutomationRuleDTO createRule(String name, String description, TriggerType triggerType,
                                        String triggerConfig, ActionType actionType, String actionConfig,
                                        Long createdById) {
        
        AutomationRule rule = new AutomationRule();
        rule.setName(name);
        rule.setDescription(description);
        rule.setTriggerType(triggerType);
        rule.setTriggerConfig(triggerConfig);
        rule.setActionType(actionType);
        rule.setActionConfig(actionConfig);
        rule.setIsActive(true);
        rule.setRunCount(0);

        if (createdById != null) {
            User creator = userRepository.findById(createdById).orElse(null);
            rule.setCreatedBy(creator);
        }

        rule = ruleRepository.save(rule);
        
        activityLogService.log("RULE_CREATED", "AUTOMATION", rule.getId(), "Created rule: " + name, createdById);
        return toDTO(rule);
    }

    @Transactional(readOnly = true)
    public Page<AutomationRuleDTO> getAllRules(Pageable pageable) {
        return ruleRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<AutomationRuleDTO> getActiveRules() {
        return ruleRepository.findByIsActiveTrue()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AutomationRuleDTO getRuleById(Long id) {
        return toDTO(findRuleOrThrow(id));
    }

    public AutomationRuleDTO toggleRule(Long id, Long toggledById) {
        AutomationRule rule = findRuleOrThrow(id);
        rule.setIsActive(!rule.getIsActive());
        rule = ruleRepository.save(rule);
        
        String state = rule.getIsActive() ? "activated" : "deactivated";
        activityLogService.log("RULE_TOGGLED", "AUTOMATION", id, "Rule " + state + ": " + rule.getName(), toggledById);
        
        return toDTO(rule);
    }

    public void deleteRule(Long id, Long deletedById) {
        AutomationRule rule = findRuleOrThrow(id);
        ruleRepository.delete(rule);
        activityLogService.log("RULE_DELETED", "AUTOMATION", id, "Deleted rule: " + rule.getName(), deletedById);
    }

    @Transactional(readOnly = true)
    public Page<RuleExecution> getRuleExecutions(Long ruleId, Pageable pageable) {
        return executionRepository.findByRuleIdOrderByExecutedAtDesc(ruleId, pageable);
    }

    // Called by scheduler or event listeners
    public void executeRule(Long ruleId) {
        AutomationRule rule = findRuleOrThrow(ruleId);
        if (!rule.getIsActive()) return;

        log.info("Executing automation rule: {}", rule.getName());
        
        try {
            Map<String, Object> triggerConfig = objectMapper.readValue(rule.getTriggerConfig(), Map.class);
            List<Lead> targetLeads = findTargetLeads(rule.getTriggerType(), triggerConfig);
            
            log.info("Rule {} found {} target leads", ruleId, targetLeads.size());
            
            for (Lead lead : targetLeads) {
                executeActionForLead(rule, lead);
            }
            
            rule.setLastRunAt(LocalDateTime.now());
            rule.setRunCount(rule.getRunCount() + 1);
            ruleRepository.save(rule);
            
        } catch (Exception e) {
            log.error("Failed to execute rule {}", ruleId, e);
            recordExecution(rule, null, ExecutionStatus.FAILURE, e.getMessage());
        }
    }

    private List<Lead> findTargetLeads(TriggerType triggerType, Map<String, Object> config) {
        // Simplified trigger logic for demo
        // In a real app, this would use a complex specification builder based on the config
        
        String condition = (String) config.getOrDefault("condition", "ALL");
        
        if ("INACTIVE_7_DAYS".equals(condition)) {
            LocalDateTime threshold = LocalDateTime.now().minusDays(7);
            return leadRepository.findByStatusAndLastContactedAtBefore(LeadStatus.CONTACTED, threshold);
        } else if ("SCORE_DROP".equals(condition)) {
            // Find leads with score < 30 (would normally join with AiInsight)
            // Simplified return empty for now since we don't have the custom repo method here
            return List.of();
        } else if ("FOLLOW_UP_DUE".equals(condition)) {
            return leadRepository.findByNextFollowUpBefore(LocalDateTime.now());
        }
        
        return List.of(); // Empty fallback
    }

    private void executeActionForLead(AutomationRule rule, Lead lead) {
        try {
            Map<String, Object> actionConfig = objectMapper.readValue(rule.getActionConfig(), Map.class);
            
            switch (rule.getActionType()) {
                case CREATE_TASK:
                    taskService.createTask(
                            lead.getId(),
                            lead.getOwner() != null ? lead.getOwner().getId() : 1L, // Fallback to admin
                            1L, // System user
                            (String) actionConfig.getOrDefault("title", "Automated Task"),
                            (String) actionConfig.getOrDefault("description", "Generated by rule: " + rule.getName()),
                            LocalDateTime.now().plusDays(1),
                            Priority.valueOf((String) actionConfig.getOrDefault("priority", "HIGH"))
                    );
                    break;
                    
                case UPDATE_STATUS:
                    String newStatus = (String) actionConfig.get("status");
                    if (newStatus != null) {
                        leadService.updateLeadStatus(lead.getId(), LeadStatus.valueOf(newStatus), 1L);
                    }
                    break;
                    
                case SEND_MESSAGE:
                    conversationService.createConversation(lead.getId(), Channel.SYSTEM, "Automated Message");
                    // Assuming we'd get the newly created conversation ID here and send the message
                    break;
                    
                case SEND_NOTIFICATION:
                    if (lead.getOwner() != null) {
                        notificationService.createAndPush(
                                lead.getOwner().getId(),
                                (String) actionConfig.getOrDefault("title", "Automated Alert"),
                                (String) actionConfig.getOrDefault("message", "Alert for lead " + lead.getFullName()),
                                NotificationType.ALERT,
                                "LEAD",
                                lead.getId()
                        );
                    }
                    break;
            }
            
            recordExecution(rule, lead, ExecutionStatus.SUCCESS, "Action executed successfully");
            
        } catch (Exception e) {
            log.error("Action execution failed for rule {} lead {}", rule.getId(), lead.getId(), e);
            recordExecution(rule, lead, ExecutionStatus.FAILURE, e.getMessage());
        }
    }

    private void recordExecution(AutomationRule rule, Lead lead, ExecutionStatus status, String message) {
        RuleExecution execution = new RuleExecution();
        execution.setRule(rule);
        execution.setLead(lead);
        execution.setStatus(status);
        execution.setResultMessage(message);
        execution.setExecutedAt(LocalDateTime.now());
        executionRepository.save(execution);
    }

    private AutomationRule findRuleOrThrow(Long id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AutomationRule", "id", id));
    }

    private AutomationRuleDTO toDTO(AutomationRule rule) {
        return AutomationRuleDTO.builder()
                .id(rule.getId())
                .name(rule.getName())
                .description(rule.getDescription())
                .triggerType(rule.getTriggerType().name())
                .triggerConfig(rule.getTriggerConfig())
                .actionType(rule.getActionType().name())
                .actionConfig(rule.getActionConfig())
                .active(rule.getIsActive())
                .createdById(rule.getCreatedBy() != null ? rule.getCreatedBy().getId() : null)
                .createdByName(rule.getCreatedBy() != null ? rule.getCreatedBy().getFullName() : "System")
                .lastRunAt(rule.getLastRunAt() != null ? rule.getLastRunAt().toString() : null)
                .runCount(rule.getRunCount())
                .createdAt(rule.getCreatedAt() != null ? rule.getCreatedAt().toString() : null)
                .updatedAt(rule.getUpdatedAt() != null ? rule.getUpdatedAt().toString() : null)
                .build();
    }
}
