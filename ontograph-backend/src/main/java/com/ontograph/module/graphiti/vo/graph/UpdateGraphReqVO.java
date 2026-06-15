package com.ontograph.module.graphiti.vo.graph;

import lombok.Data;
import java.io.Serializable;

/**
 * 更新图谱请求 VO
 */
@Data
public class UpdateGraphReqVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 图谱名称
     */
    private String name;
    
    /**
     * 图谱描述
     */
    private String description;
}
