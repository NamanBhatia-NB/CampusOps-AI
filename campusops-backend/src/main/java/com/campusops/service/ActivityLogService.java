package com.campusops.service;

import com.campusops.dto.ActivityLogDTO;
import com.campusops.entity.ActivityLog;
import com.campusops.entity.User;
import com.campusops.repository.ActivityLogRepository;
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
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public void log(String action, String entityType, Long entityId, String details, Long userId) {
        try {
            ActivityLog logEntry = new ActivityLog();
            logEntry.setAction(action);
            logEntry.setEntityType(entityType);
            logEntry.setEntityId(entityId);
            
            // Format details as JSON
            java.util.Map<String, String> detailsMap = new java.util.HashMap<>();
            detailsMap.put("message", details);
            logEntry.setDetails(objectMapper.writeValueAsString(detailsMap));
            
            logEntry.setCreatedAt(LocalDateTime.now());

            if (userId != null) {
                userRepository.findById(userId).ifPresent(logEntry::setUser);
            }

            activityLogRepository.save(logEntry);
            log.debug("Activity logged: {} {} {}", action, entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to log activity: {} {} {}", action, entityType, entityId, e);
        }
    }

    public void log(String action, String entityType, Long entityId, String details, Long userId, String ipAddress) {
        try {
            ActivityLog logEntry = new ActivityLog();
            logEntry.setAction(action);
            logEntry.setEntityType(entityType);
            logEntry.setEntityId(entityId);
            
            // Format details as JSON
            java.util.Map<String, String> detailsMap = new java.util.HashMap<>();
            detailsMap.put("message", details);
            logEntry.setDetails(objectMapper.writeValueAsString(detailsMap));
            
            logEntry.setIpAddress(ipAddress);
            logEntry.setCreatedAt(LocalDateTime.now());

            if (userId != null) {
                userRepository.findById(userId).ifPresent(logEntry::setUser);
            }

            activityLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to log activity: {} {} {}", action, entityType, entityId, e);
        }
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogDTO> getRecentLogs(Pageable pageable) {
        return activityLogRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogDTO> getLogsByUser(Long userId, Pageable pageable) {
        return activityLogRepository.findByUserId(userId, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<ActivityLogDTO> getLogsByEntity(String entityType, Long entityId) {
        return activityLogRepository.findByEntityTypeAndEntityId(entityType, entityId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private ActivityLogDTO toDTO(ActivityLog log) {
        String parsedDetails = log.getDetails();
        if (parsedDetails != null && parsedDetails.startsWith("{")) {
            try {
                java.util.Map<String, String> map = objectMapper.readValue(parsedDetails, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String,String>>() {});
                parsedDetails = map.getOrDefault("message", log.getDetails());
            } catch (Exception e) {
                // Ignore parsing errors, keep original string
            }
        }
        
        return ActivityLogDTO.builder()
                .id(log.getId())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .userName(log.getUser() != null ? log.getUser().getFullName() : "System")
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .details(parsedDetails)
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
