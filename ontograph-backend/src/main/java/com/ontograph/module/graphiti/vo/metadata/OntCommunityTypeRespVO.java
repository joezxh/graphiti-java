package com.ontograph.module.graphiti.vo.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * 社区类型响应 VO
 */
@Data
@Builder
@Schema(description = "社区类型响应")
public class OntCommunityTypeRespVO {
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "本体定义ID")
    private Long definitionId;
    @Schema(description = "类型代码")
    private String typeCode;
    @Schema(description = "类型名称")
    private String typeName;
    @Schema(description = "英文名称")
    private String typeNameEn;
    @Schema(description = "分类维度: domain|region|scenario")
    private String category;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "父类型代码")
    private String parentTypeCode;
    @Schema(description = "排序值")
    private Integer sortOrder;
    @Schema(description = "区域/管辖区")
    private String region;
    @Schema(description = "应用场景")
    private String scenarioType;
    @Schema(description = "关联的图数据库社区节点 uuid")
    private String communityUuid;
    @Schema(description = "图谱 ID")
    private String graphId;
    @Schema(description = "元数据")
    private String metadata;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
