package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.vo.ontology.InferredTypeVO;
import java.util.List;
import java.util.Map;

public interface EpisodeTypeInferenceService {

    List<InferredTypeVO> inferEntityTypes(String graphId, String content, String domainHint);

    Map<String, List<InferredTypeVO>> inferBatch(String graphId, List<String> episodeIds);
}
