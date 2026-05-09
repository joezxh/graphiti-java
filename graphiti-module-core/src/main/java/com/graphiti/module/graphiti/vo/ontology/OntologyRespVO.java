package com.graphiti.module.graphiti.vo.ontology;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 本体定义响应 VO
 */
@Data
public class OntologyRespVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 图谱ID
     */
    private String graphId;
    
    /**
     * 实体类型定义（JSON 数组）
     */
    private String entities;
    
    /**
     * 关系类型定义（JSON 数组）
     */
    private String edges;
    
    /**
     * 是否默认本体
     */
    private Boolean isDefault;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
