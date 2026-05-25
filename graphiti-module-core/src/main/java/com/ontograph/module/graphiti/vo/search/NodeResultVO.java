package com.graphiti.module.graphiti.vo.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 节点结果 VO（对应实体节点）
 */
@Data
@Schema(description = "节点结果（实体节点）")
public class NodeResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "节点 UUID")
    private String uuid;

    @Schema(description = "节点名称")
    private String name;

    @Schema(description = "节点标签列表")
    private List<String> labels;

    @Schema(description = "节点摘要")
    private String summary;

    @Schema(description = "搜索得分")
    private Double score;
}
