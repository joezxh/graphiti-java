package com.graphiti.module.graphiti.vo.community;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 社区过滤请求 VO (V3.0.0)
 */
@Data
@Schema(description = "社区过滤请求 (V3.0.0)")
public class CommunityFilterReqVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "法律领域过滤: DOMAIN_CIVIL, DOMAIN_CRIMINAL, etc.")
    private String legalDomain;

    @Schema(description = "司法管辖区过滤: JURISDICTION_CN, etc.")
    private String jurisdiction;

    @Schema(description = "应用场景过滤: PRACTICE_JUDICIAL, PRACTICE_MEDIATION, etc.")
    private String practiceType;

    @Schema(description = "父社区 UUID（仅返回指定社区的子节点）")
    private String parentCommunityUuid;

    @Schema(description = "关键词搜索（匹配 name）")
    private String keyword;
}
