package com.ontograph.module.graphiti.vo.imports;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;
import java.util.Map;

/**
 * 添加事实三元组请求 VO
 */
@Data
@Schema(description = "添加事实三元组请求")
public class FactTripleReqVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "目标图谱 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "图谱 ID 不能为空")
    private String graphId;

    @Schema(description = "源节点名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "源节点名称不能为空")
    private String sourceNodeName;

    @Schema(description = "关系类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "关系类型不能为空")
    private String relationType;

    @Schema(description = "目标节点名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标节点名称不能为空")
    private String targetNodeName;

    @Schema(description = "事实描述")
    private String fact;

    @Schema(description = "额外属性")
    private Map<String, Object> properties;
}
