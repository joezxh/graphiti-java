package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.model.search.SearchResults.EdgeResult;
import com.ontograph.module.graphiti.model.search.SearchResults.EpisodeResult;
import com.ontograph.module.graphiti.model.search.SearchResults.NodeResult;
import com.ontograph.module.graphiti.model.search.SearchResults.CommunityResult;

import java.util.List;

/**
 * RRF (Reciprocal Rank Fusion) 重排服务
 *
 * <p>参考 Python 实现：graphiti_core/search/search_utils.py:rrf()
 *
 * <p>公式：score(doc) = Σ 1 / (rank + k)
 * <p>将多个排名列表融合为一个综合排名，对每个搜索方法的结果赋予相同的权重。
 */
public interface RrfRerankerService {

    /**
     * RRF 融合边列表
     *
     * @param resultLists 多个边排名列表
     * @param k RRF 参数（通常 1-60，Python 默认 1，Java 旧版默认 60）
     * @return 按 RRF 分数降序排列的结果
     */
    List<EdgeResult> rrfEdges(List<List<EdgeResult>> resultLists, int k);

    /**
     * RRF 融合节点列表
     */
    List<NodeResult> rrfNodes(List<List<NodeResult>> resultLists, int k);

    /**
     * RRF 融合 Episode 列表
     */
    List<EpisodeResult> rrfEpisodes(List<List<EpisodeResult>> resultLists, int k);

    /**
     * RRF 融合 Community 列表
     */
    List<CommunityResult> rrfCommunities(List<List<CommunityResult>> resultLists, int k);

    /**
     * RRF 融合 UUID 字符串列表（纯排名，无类型信息）
     *
     * @param resultLists 多个 UUID 排名列表
     * @param k RRF 参数
     * @return 按 RRF 分数降序排列的 UUID 列表
     */
    List<String> rrfUuids(List<List<String>> resultLists, int k);

    /**
     * 带分数权重的 RRF 融合
     *
     * <p>公式：score(doc) = Σ (method_score * 1 / (rank + k))
     *
     * @param uuidScorePairs 每个列表的 Map（uuid -> 原始分数）
     * @param k RRF 参数
     * @return 按加权 RRF 分数降序排列的 UUID 列表
     */
    List<String> rrfWeightedUuids(List<java.util.Map<String, Double>> uuidScorePairs, int k);
}
