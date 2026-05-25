package com.graphiti.module.graphiti.vo.ide;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Schema 变更验证响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Schema 变更验证响应")
public class SchemaChangeValidateRespVO {

    @Schema(description = "是否兼容 (true 表示可以执行变更)")
    private boolean compatible;

    @Schema(description = "影响的节点数量")
    private int affectedNodes;

    @Schema(description = "冲突详情列表")
    private List<Violation> violations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "冲突详情")
    public static class Violation {
        @Schema(description = "节点UUID")
        private String nodeUuid;

        @Schema(description = "节点名称")
        private String nodeName;

        @Schema(description = "冲突类型")
        private String violationType;

        @Schema(description = "冲突原因")
        private String reason;

        @Schema(description = "当前值")
        private Object currentValue;

        @Schema(description = "期望值")
        private Object expectedValue;
    }

    /**
     * 创建兼容的结果
     */
    public static SchemaChangeValidateRespVO compatible() {
        return SchemaChangeValidateRespVO.builder()
                .compatible(true)
                .affectedNodes(0)
                .violations(new ArrayList<>())
                .build();
    }

    /**
     * 创建不兼容的结果
     */
    public static SchemaChangeValidateRespVO incompatible(int affectedNodes, List<Violation> violations) {
        return SchemaChangeValidateRespVO.builder()
                .compatible(false)
                .affectedNodes(affectedNodes)
                .violations(violations)
                .build();
    }
}
