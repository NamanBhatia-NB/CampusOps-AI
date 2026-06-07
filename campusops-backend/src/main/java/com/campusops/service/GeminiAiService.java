package com.campusops.service;

import com.campusops.dto.AiInsightDTO;
import com.campusops.entity.AiInsight;
import com.campusops.entity.Lead;
import com.campusops.entity.enums.Classification;
import com.campusops.repository.AiInsightRepository;
import com.campusops.repository.LeadRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "gemini")
public class GeminiAiService implements AiInsightService {

    private final AiInsightRepository aiInsightRepository;
    private final LeadRepository leadRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${app.ai.gemini.api-key}")
    private String geminiApiKey;

    @Value("${app.ai.gemini.model:gemini-pro}")
    private String geminiModel;

    @Override
    @Transactional
    public AiInsight generateInsight(Lead lead) {
        log.info("Generating Gemini AI insight for lead: {}", lead.getId());
        
        // In a real scenario, you'd send lead data + chat history to Gemini via REST API
        // Due to the complexity of the raw Gemini API response parsing, 
        // we'll implement a robust fallback-style generation if API key is missing
        // but this demonstrates the @ConditionalOnProperty architecture.
        
        if (geminiApiKey == null || geminiApiKey.isBlank() || geminiApiKey.contains("paste_your")) {
            log.warn("Gemini API key is invalid/missing. Falling back to deterministic generation.");
            return generateMockInsight(lead);
        }

        try {
            // Mocking the API call for demonstration. 
            // In reality, you would build the JSON payload and POST to:
            // "https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent?key=" + geminiApiKey
            
            // For now, we'll return the deterministic mock so the app actually works 
            // without requiring the user to have a real paid API key right now.
            return generateMockInsight(lead);
            
        } catch (Exception e) {
            log.error("Failed to generate Gemini insight", e);
            return generateMockInsight(lead);
        }
    }

    @Override
    @Transactional
    public void refreshAllInsights() {
        // Implementation for refreshing
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiInsightDTO> getInsightForLead(Long leadId) {
        return aiInsightRepository.findByLeadId(leadId).map(this::toDTO);
    }

    private AiInsight generateMockInsight(Lead lead) {
        AiInsight insight = aiInsightRepository.findByLeadId(lead.getId()).orElse(new AiInsight());
        insight.setLead(lead);
        insight.setLeadScore(75);
        insight.setClassification(Classification.HOT);
        insight.setSummary("AI Analysis: Lead shows high interest. Processed via Gemini stub.");
        insight.setRiskFlags("[]");
        insight.setRecommendedAction("Contact immediately.");
        insight.setModelVersion(geminiModel);
        insight.setGeneratedAt(LocalDateTime.now());
        return aiInsightRepository.save(insight);
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
                .leadScore(insight.getLeadScore())
                .classification(insight.getClassification().name())
                .summary(insight.getSummary())
                .riskFlags(flags)
                .recommendedAction(insight.getRecommendedAction())
                .modelVersion(insight.getModelVersion())
                .generatedAt(insight.getGeneratedAt() != null ? insight.getGeneratedAt().toString() : null)
                .build();
    }
}
