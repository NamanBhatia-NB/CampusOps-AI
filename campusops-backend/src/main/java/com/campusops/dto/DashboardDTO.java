package com.campusops.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {
    private long totalLeads;
    private long newLeads;
    private long contactedLeads;
    private long followUpLeads;
    private long qualifiedLeads;
    private long admittedLeads;
    private long lostLeads;
    private double conversionRate;
    private long pendingFollowUps;
    private long overdueTasks;
    private long activeConversations;
    private long unreadNotifications;
    private long totalCounselors;
    private double avgLeadScore;
    private long hotLeads;
    private long warmLeads;
    private long coldLeads;
}
