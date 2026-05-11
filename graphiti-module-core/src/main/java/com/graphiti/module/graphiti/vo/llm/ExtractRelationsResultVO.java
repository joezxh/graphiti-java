package com.graphiti.module.graphiti.vo.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * LLM 关系提取响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractRelationsResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<ExtractedRelationVO> relations;
}
