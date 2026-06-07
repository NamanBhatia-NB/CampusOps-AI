package com.campusops.service;

import com.campusops.dto.AiInsightDTO;
import com.campusops.entity.AiInsight;
import com.campusops.entity.Lead;
import com.campusops.entity.enums.Classification;
import com.campusops.entity.enums.LeadStatus;
import com.campusops.repository.AiInsightRepository;
import com.campusops.repository.LeadRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "fallback", matchIfMissing = true)
public class FallbackAiService implements AiInsightService {

    private final AiInsightRepository aiInsightRepository;
    private final LeadRepository leadRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AiInsight generateInsight(Lead lead) {
        int score = calculateScore(lead);
        Classification classification = getClassification(score);
        List<String> riskFlags = generateRiskFlags(lead, score);
        String summary = generateSummary(lead, score, classification, riskFlags);
        String action = generateRecommendedAction(lead, score, riskFlags);

        AiInsight insight = aiInsightRepository.findByLeadId(lead.getId())
                .orElse(new AiInsight());

        insight.setLead(lead);
        insight.setLeadScore(score);
        insight.setClassification(classification);
        insight.setSummary(summary);
        try {
            insight.setRiskFlags(objectMapper.writeValueAsString(riskFlags));
        } catch (JsonProcessingException e) {
            insight.setRiskFlags("[]");
            log.error("Failed to serialize risk flags for lead {}", lead.getId(), e);
        }
        insight.setRecommendedAction(action);
        insight.setModelVersion("fallback-v1");
        insight.setGeneratedAt(LocalDateTime.now());

        return aiInsightRepository.save(insight);
    }

    @Override
    @Transactional
    public void refreshAllInsights() {
        log.info("Refreshing AI insights for all active leads");
        List<Lead> leads = leadRepository.findAll().stream()
                .filter(l -> l.getStatus() != LeadStatus.ADMITTED && l.getStatus() != LeadStatus.LOST)
                .toList();

        int count = 0;
        for (Lead lead : leads) {
            generateInsight(lead);
            count++;
            if (count % 50 == 0) {
                log.debug("Processed {}/{} insights", count, leads.size());
            }
        }
        log.info("Completed refreshing {} AI insights", count);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiInsightDTO> getInsightForLead(Long leadId) {
        return aiInsightRepository.findByLeadId(leadId).map(this::toDTO);
    }

    private int calculateScore(Lead lead) {
        int score = 30; // Base score

        // Contact info
        if (lead.getEmail() != null && !lead.getEmail().isBlank()) score += 10;
        if (lead.getPhone() != null && !lead.getPhone().isBlank()) score += 10;

        // Follow-up scheduled
        if (lead.getNextFollowUp() != null && lead.getNextFollowUp().isAfter(LocalDateTime.now())) {
            score += 15;
        }

        // Status progression
        if (lead.getStatus() == LeadStatus.QUALIFIED) score += 20;
        else if (lead.getStatus() == LeadStatus.FOLLOW_UP) score += 10;
        else if (lead.getStatus() == LeadStatus.CONTACTED) score += 5;

        // Activity metrics (approximated via entity state)
        if (lead.getConversations() != null && !lead.getConversations().isEmpty()) {
            score += Math.min(20, lead.getConversations().size() * 5);
        }
        if (lead.getNotes() != null && !lead.getNotes().isEmpty()) {
            score += Math.min(15, lead.getNotes().size() * 3);
        }

        // Recency decay
        if (lead.getLastContactedAt() != null) {
            long daysSinceContact = ChronoUnit.DAYS.between(lead.getLastContactedAt(), LocalDateTime.now());
            if (daysSinceContact > 14) score -= 15;
            else if (daysSinceContact > 7) score -= 5;
            else if (daysSinceContact <= 2) score += 5;
        } else {
            long daysSinceCreated = ChronoUnit.DAYS.between(lead.getCreatedAt(), LocalDateTime.now());
            if (daysSinceCreated > 7) score -= 10;
        }

        return Math.max(0, Math.min(100, score));
    }

    private Classification getClassification(int score) {
        if (score >= 70) return Classification.HOT;
        if (score >= 40) return Classification.WARM;
        return Classification.COLD;
    }

    private List<String> generateRiskFlags(Lead lead, int score) {
        List<String> flags = new ArrayList<>();
        
        long daysSinceContact = lead.getLastContactedAt() != null 
                ? ChronoUnit.DAYS.between(lead.getLastContactedAt(), LocalDateTime.now())
                : ChronoUnit.DAYS.between(lead.getCreatedAt(), LocalDateTime.now());

        if (daysSinceContact >= 7 && lead.getStatus() != LeadStatus.ADMITTED && lead.getStatus() != LeadStatus.LOST) {
            flags.add("inactive");
        }

        if (lead.getStatus() == LeadStatus.NEW && daysSinceContact >= 5) {
            flags.add("no_response");
        }

        if (score < 30) {
            flags.add("low_intent");
        }

        return flags;
    }

    private String generateSummary(Lead lead, int score, Classification classification, List<String> riskFlags) {
        String engagement = classification == Classification.HOT ? "high" : 
                            classification == Classification.WARM ? "moderate" : "low";
        
        int convCount = lead.getConversations() != null ? lead.getConversations().size() : 0;
        
        StringBuilder sb = new StringBuilder();
        sb.append("Lead showed ").append(engagement)
          .append(" engagement with ").append(convCount).append(" conversations. ");
        
        sb.append("Currently in ").append(lead.getStatus().name()).append(" stage. ");

        if (!riskFlags.isEmpty()) {
            sb.append("Risk flags detected: ").append(String.join(", ", riskFlags)).append(".");
        } else if (classification == Classification.HOT) {
            sb.append("High probability of conversion based on recent activity.");
        }

        return sb.toString();
    }

    private String generateRecommendedAction(Lead lead, int score, List<String> riskFlags) {
        if (riskFlags.contains("inactive") || riskFlags.contains("no_response")) {
            return "Send a quick check-in message via WhatsApp to re-engage.";
        }
        if (score >= 70) {
            return "Call immediately to push for final enrollment/admission.";
        }
        if (lead.getNextFollowUp() == null) {
            return "Schedule a follow-up call to maintain momentum.";
        }
        return "Continue nurturing according to standard pipeline.";
    }

    private AiInsightDTO toDTO(AiInsight insight) {
        List<String> flags = new ArrayList<>();
        try {
            if (insight.getRiskFlags() != null) {
                flags = objectMapper.readValue(insight.getRiskFlags(), List.class);
            }
        } catch (JsonProcessingException e) {
            log.warn("Could not parse risk flags for insight {}", insight.getId());
        }

        return AiInsightDTO.builder()
                .id(insight.getId())
                .leadId(insight.getLead() != null ? insight.getLead().getId() : null)
                .leadName(insight.getLead() != null ? insight.getLead().getFullName() : null)
                .leadScore(insight.getLeadScore())
                .classification(insight.getClassification().name())
                .summary(insight.getSummary())
                .riskFlags(flags)
                .recommendedAction(insight.getRecommendedAction())
                .modelVersion(insight.getModelVersion())
                .generatedAt(insight.getGeneratedAt() != null ? insight.getGeneratedAt().toString() : null)
                .updatedAt(insight.getUpdatedAt() != null ? insight.getUpdatedAt().toString() : null)
                .build();
    }
}
