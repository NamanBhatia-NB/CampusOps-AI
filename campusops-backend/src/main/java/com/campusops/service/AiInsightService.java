package com.campusops.service;

import com.campusops.dto.AiInsightDTO;
import com.campusops.entity.AiInsight;
import com.campusops.entity.Lead;

import java.util.Optional;

public interface AiInsightService {
    AiInsight generateInsight(Lead lead);
    void refreshAllInsights();
    Optional<AiInsightDTO> getInsightForLead(Long leadId);
}
