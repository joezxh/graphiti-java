package com.graphiti.module.graphiti.vo.node;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

/**
 * 节点详情响应 VO
 */
@Data
public class NodeInfoRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 节点UUID
     */
    private String uuid;
    
    /**
     * 节点名称
     */
    private String name;
    
    /**
     * 节点类型（实体类型）
     */
    private String type;
    
    /**
     * 节点属性
     */
    private Map<String, Object> properties;
    
    /**
     * 节点摘要（可选）
     */
    private String summary;
}
