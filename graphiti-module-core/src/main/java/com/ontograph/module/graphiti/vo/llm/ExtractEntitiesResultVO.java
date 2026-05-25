package com.ontograph.module.graphiti.vo.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * LLM 实体提取响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractEntitiesResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<ExtractedEntityVO> entities;
}
