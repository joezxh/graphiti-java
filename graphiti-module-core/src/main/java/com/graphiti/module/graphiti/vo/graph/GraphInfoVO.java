package com.graphiti.module.graphiti.vo.graph;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 图谱简要信息 VO（用于列表展示）
 */
@Data
public class GraphInfoVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String graphId;
    private String name;
    private String description;
    private Integer nodeCount;
    private Integer edgeCount;
    private LocalDateTime createdAt;
}
