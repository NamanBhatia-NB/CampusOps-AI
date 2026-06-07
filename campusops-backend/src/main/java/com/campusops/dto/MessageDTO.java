package com.campusops.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private Long id;
    private Long conversationId;
    private String senderType;
    private Long senderId;
    private String senderName;
    private String content;
    private String messageType;
    private boolean read;
    private String externalId;
    private String createdAt;
}
