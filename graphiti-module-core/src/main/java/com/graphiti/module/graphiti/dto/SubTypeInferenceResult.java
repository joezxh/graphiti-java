package com.graphiti.module.graphiti.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubTypeInferenceResult {
    private String domainType;
    private String region;
    private String scenarioType;
    private Double confidence;
    private String reasoning;
}
