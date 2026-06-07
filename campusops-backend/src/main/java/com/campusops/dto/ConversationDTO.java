package com.campusops.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {
    private Long id;
    private Long leadId;
    private String leadName;
    private String channel;
    private String subject;
    private boolean active;
    private int messageCount;
    private int unreadCount;
    private String lastMessage;
    private String lastMessageAt;
    private String createdAt;
    private String updatedAt;
}
