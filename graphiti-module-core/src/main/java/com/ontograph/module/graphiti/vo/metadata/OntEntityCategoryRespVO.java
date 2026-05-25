package com.graphiti.module.graphiti.vo.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.time.LocalDateTime;

/**
 * 实体分类响应 VO（含树形 children）
 */
@Data
@Builder
@Schema(description = "实体分类响应（含树形结构）")
public class OntEntityCategoryRespVO {
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "本体定义ID")
    private Long definitionId;
    @Schema(description = "分类代码")
    private String categoryCode;
    @Schema(description = "分类名称")
    private String categoryName;
    @Schema(description = "层级")
    private Integer categoryLevel;
    @Schema(description = "父分类代码")
    private String parentCategoryCode;
    @Schema(description = "适用实体类型范围")
    private String entityTypeScope;
    @Schema(description = "默认属性模板")
    private String defaultAttributes;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "排序值")
    private Integer sortOrder;
    @Schema(description = "元数据")
    private String metadata;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "子分类")
    private List<OntEntityCategoryRespVO> children;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
