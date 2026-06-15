package com.ontograph.module.graphiti.vo.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 剧集类型响应 VO
 */
@Data
@Builder
@Schema(description = "剧集类型响应 V5")
public class OntEpisodeTypeRespVO {
    @Schema(description = "主键ID") private Long id;
    @Schema(description = "本体定义ID") private Long definitionId;
    @Schema(description = "类型代码") private String typeCode;
    @Schema(description = "类型名称") private String typeName;
    @Schema(description = "英文名称") private String typeNameEn;

    // 层级关系
    @Schema(description = "父类型编码") private String parentTypeCode;
    @Schema(description = "层级深度") private Integer level;

    // 通用分类
    @Schema(description = "业务流程类型") private String processType;
    @Schema(description = "阶段标签") private String stageLabel;
    @Schema(description = "阶段级别") private String stageLevel;
    @Schema(description = "是否审查/评议阶段") private Boolean isReviewStage;

    @Schema(description = "描述") private String description;
    @Schema(description = "排序值") private Integer sortOrder;
    @Schema(description = "元数据 JSON") private String metadata;
    @Schema(description = "状态") private String status;

    // 使用统计（查询时动态计算）
    @Schema(description = "引用该类型的实例数量") private Long instanceCount;

    // 子类型列表（树形结构用）
    @Schema(description = "子类型列表") private List<OntEpisodeTypeRespVO> children;

    @Schema(description = "创建时间") private LocalDateTime createdAt;
    @Schema(description = "更新时间") private LocalDateTime updatedAt;
    @Schema(description = "创建人") private String createdBy;
    @Schema(description = "更新人") private String updatedBy;
}
