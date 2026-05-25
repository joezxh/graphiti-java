package com.ontograph.module.graphiti.vo.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 搜索结果响应 VO（返回事实和节点列表）
 */
@Data
@Schema(description = "搜索结果")
public class SearchResultsRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "事实列表（边）")
    private List<FactResultVO> facts;

    @Schema(description = "总事实数")
    private int totalCount;

    @Schema(description = "节点列表（实体节点）")
    private List<NodeResultVO> nodes;

    @Schema(description = "总节点数")
    private int nodeCount;
}
