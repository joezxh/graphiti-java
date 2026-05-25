package com.graphiti.module.graphiti.vo.node;

import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 节点列表响应 VO
 */
@Data
public class NodeListRespVO implements Serializable {
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
     * Neo4j 标签（Entity 或 Episode）
     */
    private String label;

    /**
     * 所属图谱 ID
     */
    private String groupId;

    /**
     * 创建时间（毫秒时间戳）
     */
    private Long createdAt;

    /**
     * 节点摘要
     */
    private String summary;

    /**
     * 自定义属性
     */
    private Map<String, Object> attributes;

    /**
     * 节点属性（简要信息）
     */
    private Map<String, Object> properties;
}
