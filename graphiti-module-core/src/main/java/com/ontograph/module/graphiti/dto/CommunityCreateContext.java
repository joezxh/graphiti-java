package com.ontograph.module.graphiti.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityCreateContext {
    private String communityName;
    private String communityType;
    /** 领域类型（LLM 推断 + 用户覆盖），如 DOMAIN_SOCIAL_GOV */
    private String domainType;
    /** 二级子领域，如 DOMAIN_SOCIAL_DISPUTE_MARRIAGE */
    private String subDomainType;
    /** 区域，如 REGION_CN */
    private String region;
    /** 场景类型，如 SCENARIO_LAW_REGULATE */
    private String scenarioType;
    /** LLM 推断置信度 */
    private Double inferenceConfidence;
    /** 用户是否手动覆盖了推断结果 */
    private boolean userOverridden;
    private String summary;
    private String description;
    private Integer memberCount;
    private List<String> memberUuids;
    private String parentCommunityUuid;
}
