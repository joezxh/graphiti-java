package com.graphiti.module.graphiti.vo.edge;

import lombok.Data;
import java.io.Serializable;

/**
 * 边过滤请求 VO
 */
@Data
public class EdgeFilterReqVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 关系类型（精确匹配）
     */
    private String type;
    
    /**
     * 源节点UUID
     */
    private String source;
    
    /**
     * 目标节点UUID
     */
    private String target;
    
    /**
     * 跳过数量（分页）
     */
    private Long skip = 0L;
    
    /**
     * 限制数量（分页）
     */
    private Long limit = 20L;
}
