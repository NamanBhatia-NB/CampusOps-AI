package com.campusops.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiInsightDTO {
    private Long id;
    private Long leadId;
    private String leadName;
    private int leadScore;
    private String classification;
    private String summary;
    private List<String> riskFlags;
    private String recommendedAction;
    private String modelVersion;
    private String generatedAt;
    private String updatedAt;
}
