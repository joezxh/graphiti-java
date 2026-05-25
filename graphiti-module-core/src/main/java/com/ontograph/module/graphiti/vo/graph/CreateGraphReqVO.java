package com.graphiti.module.graphiti.vo.graph;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;

/**
 * 创建图谱请求 VO
 */
@Data
public class CreateGraphReqVO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 图谱名称
     */
    @NotBlank(message = "图谱名称不能为空")
    private String name;
    /**
     * 图谱描述
     */
    private String description;
}
