package com.ontograph.module.graphiti.vo.node;

import lombok.Data;
import java.io.Serializable;

/**
 * 节点过滤请求 VO
 */
@Data
public class NodeFilterReqVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 节点名称（模糊匹配）
     */
    private String name;
    
    /**
     * 节点类型（精确匹配）
     */
    private String type;
    
    /**
     * 跳过数量（分页）
     */
    private Long skip = 0L;
    
    /**
     * 限制数量（分页）
     */
    private Long limit = 20L;
}
