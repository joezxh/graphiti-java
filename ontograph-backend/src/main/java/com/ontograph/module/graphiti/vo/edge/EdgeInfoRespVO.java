package com.ontograph.module.graphiti.vo.edge;

import lombok.Data;
import java.io.Serializable;
import java.util.List;
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
     * 边名称
     */
    private String name;

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
     * 过期时间
     */
    private Long expiredAt;

    /**
     * 关联的 Episode UUID 列表
     */
    private List<String> episodes;

    /**
     * 边属性
     */
    private Map<String, Object> properties;
}
