package com.graphiti.module.graphiti.vo.community;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 社区信息响应 VO (V3.0.0 / V3.1.0 通用化)
 */
@Data
@Accessors(chain = true)
@Schema(description = "社区信息响应 (V3.0.0 / V3.1.0)")
public class CommunityInfoRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "UUID")
    private String uuid;

    @Schema(description = "社区名称")
    private String name;

    @Schema(description = "社区类型")
    private String communityType;

    // ==================== V3.0.0 旧字段（向后兼容）====================
    @Schema(description = "法律领域代码 (V3.0.0 旧字段): DOMAIN_CIVIL, DOMAIN_CRIMINAL, etc.")
    private String legalDomain;

    @Schema(description = "司法管辖区代码 (V3.0.0 旧字段): JURISDICTION_CN, etc.")
    private String jurisdiction;

    @Schema(description = "应用场景代码 (V3.0.0 旧字段): PRACTICE_JUDICIAL, PRACTICE_MEDIATION, etc.")
    private String practiceType;

    // ==================== V3.1.0 通用化字段 ====================
    @Schema(description = "领域类型 (V3.1.0): 替换 legalDomain")
    private String domainType;

    @Schema(description = "子领域类型 (V3.1.0): 更细粒度的领域分类")
    private String subDomainType;

    @Schema(description = "区域 (V3.1.0): 替换 jurisdiction")
    private String region;

    @Schema(description = "场景类型 (V3.1.0): 替换 practiceType")
    private String scenarioType;

    // ==================== 公共字段 ====================
    @Schema(description = "父社区 UUID (PARENT_OF)")
    private String parentCommunityUuid;

    @Schema(description = "摘要（LLM 生成）")
    private String summary;

    @Schema(description = "成员数量")
    private Integer memberCount;

    @Schema(description = "关键法条 ID 列表")
    private List<String> keyProvisions;

    @Schema(description = "元数据: {icon, color, displayPriority}")
    private Map<String, Object> metadata;

    @Schema(description = "创建时间")
    private String createdAt;

    @Schema(description = "更新时间")
    private String updatedAt;
}
