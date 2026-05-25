package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.vo.OntologyGraphVO;
import java.util.Map;

/**
 * 本体元数据服务接口
 */
public interface OntologyMetadataService {

    OntologyGraphVO getOntologyGraph(String graphId);

    OntologyGraphVO getMockDataGraph(String graphId, Long draftId);

    Map<String, Object> getGraphStats(String graphId);

    Map<String, Object> getMockDataStats(String graphId, Long draftId);
}
