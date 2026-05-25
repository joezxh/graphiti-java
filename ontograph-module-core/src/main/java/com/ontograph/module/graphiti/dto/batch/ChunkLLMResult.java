package com.ontograph.module.graphiti.dto.batch;

import com.ontograph.module.graphiti.vo.llm.ExtractedEntityVO;
import com.ontograph.module.graphiti.vo.llm.ExtractedRelationVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * LLM extraction result for a single chunk
 */
@Data
@Builder
public class ChunkLLMResult {

    private int chunkIndex;
    private List<ExtractedEntityVO> entities;
    private List<ExtractedRelationVO> relations;
}
