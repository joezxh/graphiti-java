package com.graphiti.module.graphiti.vo.edge;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

/**
 * 边列表响应 VO
 */
@Data
public class EdgeListRespVO implements Serializable {
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
     * 边属性（简要信息）
     */
    private Map<String, Object> properties;
}
