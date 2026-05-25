package com.ontograph.module.graphiti.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 本体图可视化 VO
 * 用于前端图可视化组件的图数据格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "本体图可视化数据")
public class OntologyGraphVO {

    @Schema(description = "节点列表")
    private List<NodeVO> nodes;

    @Schema(description = "边列表")
    private List<EdgeVO> edges;

    @Schema(description = "图元信息")
    private GraphMetaVO meta;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "图节点")
    public static class NodeVO {
        @Schema(description = "节点ID")
        private String id;

        @Schema(description = "节点标签")
        private String label;

        @Schema(description = "节点类型: CLASS | PROPERTY | ENTITY")
        private String type;

        @Schema(description = "领域分类")
        private String category;

        @Schema(description = "附加数据")
        private Map<String, Object> data;

        @Schema(description = "节点颜色（十六进制）")
        private String color;

        @Schema(description = "描述")
        private String description;

        @Schema(description = "示例")
        private String example;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "图边")
    public static class EdgeVO {
        @Schema(description = "边ID")
        private String id;

        @Schema(description = "源节点ID")
        private String source;

        @Schema(description = "目标节点ID")
        private String target;

        @Schema(description = "边标签")
        private String label;

        @Schema(description = "边类型: INHERITS | HAS_PROPERTY | RELATES_TO | INSTANCE_OF")
        private String type;

        @Schema(description = "附加数据")
        private Map<String, Object> data;

        @Schema(description = "边颜色")
        private String color;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "图元信息")
    public static class GraphMetaVO {
        @Schema(description = "节点总数")
        private int nodeCount;

        @Schema(description = "边总数")
        private int edgeCount;

        @Schema(description = "实体类型数量")
        private int entityTypeCount;

        @Schema(description = "关系类型数量")
        private int relationTypeCount;

        @Schema(description = "实体类型列表")
        private List<String> entityTypes;

        @Schema(description = "关系类型列表")
        private List<String> relationTypes;

        @Schema(description = "图谱ID")
        private String graphId;

        @Schema(description = "本体定义名称")
        private String ontologyName;

        @Schema(description = "本体版本")
        private String ontologyVersion;
    }
}
