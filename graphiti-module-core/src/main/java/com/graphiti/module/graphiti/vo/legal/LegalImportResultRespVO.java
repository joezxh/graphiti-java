package com.graphiti.module.graphiti.vo.legal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 法律图谱导入结果响应 VO
 */
@Data
@Schema(description = "法律图谱导入结果")
public class LegalImportResultRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "图谱 ID")
    private String graphId;

    @Schema(description = "成功导入的节点数量")
    private int nodeCount;

    @Schema(description = "成功导入的边数量")
    private int edgeCount;

    @Schema(description = "节点导入错误信息")
    private List<String> nodeErrors;

    @Schema(description = "边导入错误信息")
    private List<String> edgeErrors;

    @Schema(description = "是否完全成功")
    public boolean isAllSuccess() {
        return nodeErrors != null && nodeErrors.isEmpty()
                && edgeErrors != null && edgeErrors.isEmpty();
    }

    @Schema(description = "成功导入的总数")
    public int getTotalImported() {
        return nodeCount + edgeCount;
    }
}
