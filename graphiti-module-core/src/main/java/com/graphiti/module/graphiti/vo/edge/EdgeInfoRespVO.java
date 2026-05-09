package com.graphiti.module.graphiti.vo.edge;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

/**
 * 边详情响应 VO
 */
@Data
public class EdgeInfoRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 边UUID
     */
    private String uuid;
    
    /**
     * 源节点UUID
     */
    private String source;
    
    /**
     * 目标节点UUID
     */
    private String target;
    
    /**
     * 关系类型
     */
    private String type;
    
    /**
     * 边属性
     */
    private Map<String, Object> properties;
}
