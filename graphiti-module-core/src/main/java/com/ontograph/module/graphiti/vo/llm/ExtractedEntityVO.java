package com.graphiti.module.graphiti.vo.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * LLM 提取的实体 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedEntityVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 实体名称
     */
    private String name;

    /**
     * 实体类型
     */
    private String type;

    /**
     * 实体摘要
     */
    private String summary;

    /**
     * 额外属性
     */
    private Map<String, Object> attributes;
}
