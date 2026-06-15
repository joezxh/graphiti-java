package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.model.search.SearchResults.EdgeResult;
import com.ontograph.module.graphiti.model.search.SearchResults.NodeResult;

import java.util.List;
import java.util.Map;

/**
 * MMR (Maximal Marginal Relevance) 重排服务
 *
 * <p>参考 Python 实现：graphiti_core/search/search_utils.py:maximal_marginal_relevance()
 *
 * <p>公式：MMR(doc) = λ * sim(query, doc) - (1-λ) * max(sim(selected_docs, doc))
 * <p>λ = 1 时纯相关性，λ = 0 时纯多样性
 */
public interface MmrRerankerService {

    /**
     * MMR 重排边（使用向量余弦相似度）
     *
     * @param queryVector 查询向量
     * @param candidateUuids 候选边 UUID 列表
     * @param uuidEmbeddingMap UUID 到嵌入向量的映射
     * @param uuidFactMap UUID 到事实文本的映射（用于降级）
     * @param lambda 平衡参数（0-1）
     * @param limit 返回数量
     * @return 按 MMR 分数降序排列的边 UUID 列表
     */
    List<String> mmrEdges(
            double[] queryVector,
            List<String> candidateUuids,
            Map<String, double[]> uuidEmbeddingMap,
            Map<String, String> uuidFactMap,
            double lambda,
            int limit);

    /**
     * MMR 重排节点（使用向量余弦相似度）
     */
    List<String> mmrNodes(
            double[] queryVector,
            List<String> candidateUuids,
            Map<String, double[]> uuidEmbeddingMap,
            Map<String, String> uuidNameMap,
            double lambda,
            int limit);

    /**
     * MMR 重排（基于文本 Jaccard 相似度，降级方案）
     *
     * <p>当无向量时使用此方法
     */
    List<String> mmrByText(
            List<String> candidateUuids,
            Map<String, String> uuidTextMap,
            double lambda,
            int limit);

    /**
     * 计算余弦相似度
     */
    double cosineSimilarity(double[] a, double[] b);

    /**
     * L2 归一化向量
     */
    double[] normalizeL2(double[] vector);
}
