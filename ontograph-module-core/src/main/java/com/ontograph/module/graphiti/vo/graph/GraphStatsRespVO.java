package com.ontograph.module.graphiti.vo.graph;

import lombok.Data;
import java.io.Serializable;

/**
 * 图谱统计信息响应 VO
 */
@Data
public class GraphStatsRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 图谱总数
     */
    private Long totalGraphs;
    
    /**
     * 实体节点总数
     */
    private Long totalNodes;
    
    /**
     * 关系边总数
     */
    private Long totalEdges;
    
    /**
     * 推理事件总数
     */
    private Long totalEpisodes;
    
    /**
     * 节点趋势（可选）
     */
    private Integer nodeTrend;
    
    /**
     * 边趋势（可选）
     */
    private Integer edgeTrend;
    
    /**
     * 事件趋势（可选）
     */
    private Integer episodeTrend;
}
