package com.graphiti.module.graphiti.vo.ontology;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class BatchValidationRespVO {
    private int totalNodes;
    private int validNodes;
    private int totalEdges;
    private int validEdges;
    private List<ValidationResultVO> nodeResults;
    private List<ValidationResultVO> edgeResults;
}
