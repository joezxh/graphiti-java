package com.ontograph.module.graphiti.vo.legal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 批量导入法律图谱请求 VO
 */
@Data
@Schema(description = "批量导入法律图谱请求")
public class ImportLegalKGReqVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "目标图谱 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "图谱 ID 不能为空")
    private String graphId;

    @Schema(description = "节点列表")
    private List<Map<String, Object>> nodes;

    @Schema(description = "边列表")
    private List<Map<String, Object>> edges;
}
