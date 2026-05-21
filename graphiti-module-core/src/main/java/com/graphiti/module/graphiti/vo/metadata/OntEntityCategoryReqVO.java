package com.graphiti.module.graphiti.vo.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
import java.time.LocalDateTime;

/**
 * 实体分类请求 VO（创建/更新共用）
 */
@Data
@Schema(description = "创建/更新实体分类请求")
public class OntEntityCategoryReqVO {

    @Schema(description = "主键ID（更新时必需）")
    private Long id;

    @Schema(description = "本体定义ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long definitionId;

    @Schema(description = "分类代码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分类代码不能为空")
    private String categoryCode;

    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;

    @Schema(description = "层级（1=一级, 2=二级, 3=三级）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "层级不能为空")
    private Integer categoryLevel = 1;

    @Schema(description = "父分类代码（层级嵌套）")
    private String parentCategoryCode;

    @Schema(description = "适用实体类型 JSON 数组，如 [\"Case\", \"Court\"]")
    private String entityTypeScope;

    @Schema(description = "默认属性模板 JSON")
    private String defaultAttributes;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "排序值")
    private Integer sortOrder = 0;

    @Schema(description = "元数据 JSON")
    private String metadata;

    @Schema(description = "状态: ACTIVE|INACTIVE")
    private String status = "ACTIVE";
}
