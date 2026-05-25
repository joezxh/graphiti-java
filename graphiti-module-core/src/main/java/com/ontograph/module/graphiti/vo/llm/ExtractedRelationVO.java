package com.graphiti.module.graphiti.vo.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * LLM 提取的关系 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedRelationVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 源实体名称
     */
    private String source;

    /**
     * 目标实体名称
     */
    private String target;

    /**
     * 关系类型
     */
    private String type;

    /**
     * 事实描述
     */
    private String fact;
}
