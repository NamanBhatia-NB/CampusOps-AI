package com.campusops.service;

import com.campusops.dto.ActivityLogDTO;
import com.campusops.dto.DashboardDTO;
import com.campusops.entity.AiInsight;
import com.campusops.entity.Lead;
import com.campusops.entity.enums.Classification;
import com.campusops.entity.enums.LeadStatus;
import com.campusops.entity.enums.Role;
import com.campusops.entity.enums.TaskStatus;
import com.campusops.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final LeadRepository leadRepository;
    private final TaskRepository taskRepository;
    private final ConversationRepository conversationRepository;
    private final NotificationRepository notificationRepository;
    private final AiInsightRepository aiInsightRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public DashboardDTO getDashboardData(Long userId, Role role) {
        boolean isCounselor = role == Role.COUNSELOR;

        long totalLeads = isCounselor ? leadRepository.countByOwnerId(userId) : leadRepository.count();
        
        long newLeads = countLeadsByStatus(LeadStatus.NEW, userId, isCounselor);
        long contactedLeads = countLeadsByStatus(LeadStatus.CONTACTED, userId, isCounselor);
        long followUpLeads = countLeadsByStatus(LeadStatus.FOLLOW_UP, userId, isCounselor);
        long qualifiedLeads = countLeadsByStatus(LeadStatus.QUALIFIED, userId, isCounselor);
        long admittedLeads = countLeadsByStatus(LeadStatus.ADMITTED, userId, isCounselor);
        long lostLeads = countLeadsByStatus(LeadStatus.LOST, userId, isCounselor);

        double conversionRate = totalLeads > 0 ? ((double) admittedLeads / totalLeads) * 100 : 0.0;

        // Pending follow-ups: Leads with nextFollowUp < now
        long pendingFollowUps = leadRepository.findByNextFollowUpBefore(LocalDateTime.now())
                .stream()
                .filter(l -> !isCounselor || (l.getOwner() != null && l.getOwner().getId().equals(userId)))
                .count();

        // Overdue tasks
        long overdueTasks = taskRepository.findByDueDateBeforeAndStatusNot(LocalDateTime.now(), TaskStatus.COMPLETED)
                .stream()
                .filter(t -> t.getStatus() != TaskStatus.CANCELLED)
                .filter(t -> !isCounselor || (t.getAssignedTo() != null && t.getAssignedTo().getId().equals(userId)))
                .count();

        // Active conversations
        long activeConversations = conversationRepository.count(); // Simplified for now

        long unreadNotifications = notificationRepository.countByUserIdAndIsReadFalse(userId);
        long totalCounselors = userRepository.findByRole(Role.COUNSELOR).size();

        // AI Metrics
        List<AiInsight> insights = aiInsightRepository.findAll();
        if (isCounselor) {
            List<Long> leadIds = leadRepository.findByOwnerId(userId, Pageable.unpaged())
                    .stream().map(Lead::getId).collect(Collectors.toList());
            insights = insights.stream().filter(i -> leadIds.contains(i.getLead().getId())).collect(Collectors.toList());
        }

        double avgLeadScore = insights.isEmpty() ? 0 : insights.stream().mapToInt(AiInsight::getLeadScore).average().orElse(0);
        long hotLeads = insights.stream().filter(i -> i.getClassification() == Classification.HOT).count();
        long warmLeads = insights.stream().filter(i -> i.getClassification() == Classification.WARM).count();
        long coldLeads = insights.stream().filter(i -> i.getClassification() == Classification.COLD).count();

        return DashboardDTO.builder()
                .totalLeads(totalLeads)
                .newLeads(newLeads)
                .contactedLeads(contactedLeads)
                .followUpLeads(followUpLeads)
                .qualifiedLeads(qualifiedLeads)
                .admittedLeads(admittedLeads)
                .lostLeads(lostLeads)
                .conversionRate(Math.round(conversionRate * 10.0) / 10.0) // 1 decimal place
                .pendingFollowUps(pendingFollowUps)
                .overdueTasks(overdueTasks)
                .activeConversations(activeConversations)
                .unreadNotifications(unreadNotifications)
                .totalCounselors(totalCounselors)
                .avgLeadScore(Math.round(avgLeadScore * 10.0) / 10.0)
                .hotLeads(hotLeads)
                .warmLeads(warmLeads)
                .coldLeads(coldLeads)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ActivityLogDTO> getRecentActivity(Pageable pageable) {
        return activityLogService.getRecentLogs(pageable);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getLeadsByStatusMap(Long userId, Role role) {
        boolean isCounselor = role == Role.COUNSELOR;
        Map<String, Long> map = new HashMap<>();
        for (LeadStatus status : LeadStatus.values()) {
            map.put(status.name(), countLeadsByStatus(status, userId, isCounselor));
        }
        return map;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getLeadsBySourceMap(Long userId, Role role) {
        boolean isCounselor = role == Role.COUNSELOR;
        List<Lead> leads = isCounselor ? 
                leadRepository.findByOwnerId(userId, Pageable.unpaged()).getContent() : 
                leadRepository.findAll();
                
        return leads.stream()
                .filter(l -> l.getSource() != null && !l.getSource().isBlank())
                .collect(Collectors.groupingBy(Lead::getSource, Collectors.counting()));
    }

    private long countLeadsByStatus(LeadStatus status, Long userId, boolean isCounselor) {
        return isCounselor ? 
                leadRepository.countByOwnerIdAndStatus(userId, status) : 
                leadRepository.countByStatus(status);
    }
}
