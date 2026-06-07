package com.campusops.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadDTO {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String source;
    private String status;
    private String priority;
    private String programInterest;
    private Long ownerId;
    private String ownerName;
    private String tags;
    private String city;
    private String state;
    private String country;
    private String dateOfBirth;
    private String parentName;
    private String parentPhone;
    private String qualification;
    private String lastContactedAt;
    private String nextFollowUp;
    private String enrolledAt;
    private String lostReason;
    private String createdAt;
    private String updatedAt;

    // AI Insight summary (optional, populated when needed)
    private Integer leadScore;
    private String classification;
    private String aiSummary;
    private String recommendedAction;
}
