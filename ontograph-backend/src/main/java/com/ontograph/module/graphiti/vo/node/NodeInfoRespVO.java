package com.ontograph.module.graphiti.vo.node;

import lombok.Data;
import java.io.Serializable;
import java.util.List;
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

    /**
     * 所属图谱 ID
     */
    private String groupId;

    /**
     * 创建时间（毫秒时间戳）
     */
    private Long createdAt;

    /**
     * 有效时间
     */
    private Long validAt;

    /**
     * 失效时间
     */
    private Long invalidAt;

    /**
     * Neo4j 标签列表
     */
    private List<String> labels;

    /**
     * 自定义属性
     */
    private Map<String, Object> attributes;
}
