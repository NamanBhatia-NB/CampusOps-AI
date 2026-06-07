package com.campusops.service;

import com.campusops.dto.ConversationDTO;
import com.campusops.dto.MessageDTO;
import com.campusops.entity.Conversation;
import com.campusops.entity.Lead;
import com.campusops.entity.Message;
import com.campusops.entity.User;
import com.campusops.entity.enums.Channel;
import com.campusops.entity.enums.MessageType;
import com.campusops.entity.enums.SenderType;
import com.campusops.exception.ResourceNotFoundException;
import com.campusops.repository.ConversationRepository;
import com.campusops.repository.LeadRepository;
import com.campusops.repository.MessageRepository;
import com.campusops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ActivityLogService activityLogService;

    public ConversationDTO createConversation(Long leadId, Channel channel, String subject) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead", "id", leadId));

        Conversation conversation = new Conversation();
        conversation.setLead(lead);
        conversation.setChannel(channel);
        conversation.setSubject(subject);
        conversation.setIsActive(true);

        conversation = conversationRepository.save(conversation);
        
        activityLogService.log("CONVERSATION_CREATED", "LEAD", leadId,
                "Started new " + channel.name() + " conversation", null);

        return toDTO(conversation);
    }

    @Transactional(readOnly = true)
    public List<ConversationDTO> getConversationsByLead(Long leadId) {
        return conversationRepository.findByLeadIdOrderByUpdatedAtDesc(leadId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConversationDTO getConversationById(Long id) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", id));
        return toDTO(conversation);
    }

    public MessageDTO sendMessage(Long conversationId, String content, SenderType senderType, 
                                  Long senderId, MessageType messageType) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        Message message = new Message();
        message.setConversation(conversation);
        message.setContent(content);
        message.setSenderType(senderType);
        message.setMessageType(messageType);
        message.setIsRead(senderType == SenderType.USER); // Read if we sent it
        
        if (senderId != null) {
            User sender = userRepository.findById(senderId).orElse(null);
            message.setSender(sender);
        }

        message = messageRepository.save(message);
        
        // Update conversation timestamp
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        
        MessageDTO dto = toMessageDTO(message);
        
        // Push via WebSocket
        try {
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, dto);
        } catch (Exception e) {
            log.warn("Failed to push message via WebSocket", e);
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> getMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream().map(this::toMessageDTO).collect(Collectors.toList());
    }

    public void markMessagesAsRead(Long conversationId) {
        List<Message> unread = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream().filter(m -> !m.getIsRead()).collect(Collectors.toList());
                
        unread.forEach(m -> m.setIsRead(true));
        messageRepository.saveAll(unread);
    }

    private ConversationDTO toDTO(Conversation conversation) {
        Long unreadCount = messageRepository.countByConversationIdAndIsReadFalse(conversation.getId());
        
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        String lastMessage = messages.isEmpty() ? null : messages.get(messages.size() - 1).getContent();
        
        return ConversationDTO.builder()
                .id(conversation.getId())
                .leadId(conversation.getLead() != null ? conversation.getLead().getId() : null)
                .leadName(conversation.getLead() != null ? conversation.getLead().getFullName() : null)
                .channel(conversation.getChannel().name())
                .subject(conversation.getSubject())
                .active(conversation.getIsActive())
                .messageCount(messages.size())
                .unreadCount(unreadCount.intValue())
                .lastMessage(lastMessage)
                .lastMessageAt(conversation.getUpdatedAt() != null ? conversation.getUpdatedAt().toString() : null)
                .createdAt(conversation.getCreatedAt() != null ? conversation.getCreatedAt().toString() : null)
                .updatedAt(conversation.getUpdatedAt() != null ? conversation.getUpdatedAt().toString() : null)
                .build();
    }

    private MessageDTO toMessageDTO(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversation() != null ? message.getConversation().getId() : null)
                .senderType(message.getSenderType().name())
                .senderId(message.getSender() != null ? message.getSender().getId() : null)
                .senderName(message.getSender() != null ? message.getSender().getFullName() : null)
                .content(message.getContent())
                .messageType(message.getMessageType().name())
                .read(message.getIsRead())
                .externalId(message.getExternalId())
                .createdAt(message.getCreatedAt() != null ? message.getCreatedAt().toString() : null)
                .build();
    }
}
