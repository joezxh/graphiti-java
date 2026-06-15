package com.ontograph.module.graphiti.vo.ontology;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 本体定义 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "本体定义信息")
public class OntDefinitionVO {

    @Schema(description = "本体定义ID")
    private Long id;

    @Schema(description = "所属图谱ID")
    private String graphId;

    @Schema(description = "本体命名空间")
    private String namespace;

    @Schema(description = "本体名称")
    private String name;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "状态: ACTIVE/DEPRECATED/ARCHIVED")
    private String status;

    @Schema(description = "本体描述")
    private String description;

    @Schema(description = "父版本ID")
    private Long parentVersionId;

    @Schema(description = "创建者")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "类数量")
    private Integer classCount;

    @Schema(description = "属性数量")
    private Integer propertyCount;

    @Schema(description = "约束数量")
    private Integer constraintCount;
}
