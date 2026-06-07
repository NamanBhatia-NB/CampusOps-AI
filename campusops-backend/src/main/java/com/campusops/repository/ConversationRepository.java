package com.campusops.repository;

import com.campusops.entity.Conversation;
import com.campusops.entity.enums.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByLeadIdOrderByUpdatedAtDesc(Long leadId);

    List<Conversation> findByLeadIdAndChannel(Long leadId, Channel channel);
}
