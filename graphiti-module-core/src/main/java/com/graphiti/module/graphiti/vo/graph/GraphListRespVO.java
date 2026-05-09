package com.graphiti.module.graphiti.vo.graph;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 图谱列表响应 VO
 */
@Data
public class GraphListRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 图谱ID
     */
    private String graphId;
    
    /**
     * 图谱名称
     */
    private String name;
    
    /**
     * 图谱描述
     */
    private String description;
    
    /**
     * 节点数量
     */
    private Integer nodeCount;
    
    /**
     * 边数量
     */
    private Integer edgeCount;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
