package com.graphiti.module.graphiti.service;

import java.util.List;
import java.util.Map;

/**
 * 重排序服务接口
 */
public interface RerankerService {

    /**
     * RRF (Reciprocal Rank Fusion) 重排序
     */
    List<Map<String, Object>> rrfRerank(List<List<Map<String, Object>>> resultLists, int k);

    /**
     * MMR (Maximal Marginal Relevance) 重排序
     */
    List<Map<String, Object>> mmrRerank(List<Map<String, Object>> results, float[] queryEmbedding,
                                        double lambda, EmbedderService embedderService);
}
