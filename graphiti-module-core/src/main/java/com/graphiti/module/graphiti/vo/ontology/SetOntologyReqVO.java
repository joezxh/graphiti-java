package com.graphiti.module.graphiti.vo.ontology;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;

/**
 * 设置本体请求 VO
 */
@Data
public class SetOntologyReqVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 实体类型定义（JSON 数组字符串）
     */
    @NotBlank(message = "实体类型定义不能为空")
    private String entities;
    
    /**
     * 关系类型定义（JSON 数组字符串）
     */
    @NotBlank(message = "关系类型定义不能为空")
    private String edges;
    
    /**
     * 是否设为默认本体
     */
    private Boolean isDefault = false;
}
