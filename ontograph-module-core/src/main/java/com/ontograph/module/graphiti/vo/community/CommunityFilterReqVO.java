package com.ontograph.module.graphiti.vo.community;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 社区过滤请求 VO (V3.1.0 通用化)
 */
@Data
@Schema(description = "社区过滤请求 (V3.1.0)")
public class CommunityFilterReqVO implements Serializable {
    private static final long serialVersionUID = 1L;

    // V3.0.0 旧字段（向后兼容）
    @Schema(description = "法律领域过滤 (V3.0.0 旧字段): DOMAIN_CIVIL, DOMAIN_CRIMINAL, etc.")
    private String legalDomain;

    @Schema(description = "司法管辖区过滤 (V3.0.0 旧字段): JURISDICTION_CN, etc.")
    private String jurisdiction;

    @Schema(description = "应用场景过滤 (V3.0.0 旧字段): PRACTICE_JUDICIAL, PRACTICE_MEDIATION, etc.")
    private String practiceType;

    // V3.1.0 通用化新字段
    @Schema(description = "领域类型过滤 (V3.1.0): DOMAIN_LEGAL, DOMAIN_FINANCE, etc.")
    private String domainType;

    @Schema(description = "子领域过滤 (V3.1.0)")
    private String subDomainType;

    @Schema(description = "区域过滤 (V3.1.0): REGION_CN, REGION_US, etc.")
    private String region;

    @Schema(description = "场景类型过滤 (V3.1.0): SCENARIO_JUDICIAL, SCENARIO_COMPLIANCE, etc.")
    private String scenarioType;

    @Schema(description = "父社区 UUID（仅返回指定社区的子节点）")
    private String parentCommunityUuid;

    @Schema(description = "关键词搜索（匹配 name）")
    private String keyword;
}
