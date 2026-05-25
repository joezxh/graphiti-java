package com.ontograph.module.graphiti.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainInferenceResult {
    /** 顶层领域代码，如 DOMAIN_LEGAL / DOMAIN_SOCIAL_GOV */
    private String domainType;
    /** 二级子领域代码，如 DOMAIN_SOCIAL_DISPUTE_MARRIAGE */
    private String subDomainType;
    /** 区域代码，如 REGION_CN */
    private String region;
    /** 场景类型代码，如 SCENARIO_LAW_REGULATE */
    private String scenarioType;
    /** 推断置信度 0.0~1.0 */
    private Double confidence;
    /** 推断理由 */
    private String reasoning;
    /** 用户是否手动覆盖了推断结果 */
    private boolean userOverridden;
}
