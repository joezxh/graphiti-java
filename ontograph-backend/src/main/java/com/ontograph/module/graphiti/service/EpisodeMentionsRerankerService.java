package com.ontograph.module.graphiti.service;

import java.util.List;
import java.util.Map;

/**
 * Episode Mentions 重排服务
 *
 * <p>参考 Python 实现：graphiti_core/search/search_utils.py:episode_mentions_reranker()
 *
 * <p>按每个候选在 Episode 中的提及次数重排，提及越多分数越高。
 * 适用于"高频关联事实"优先的场景。
 */
public interface EpisodeMentionsRerankerService {

    /**
     * 按 Episode 提及次数重排边
     *
     * @param candidateUuids 候选边 UUID 列表
     * @param limit 返回数量
     * @return 按提及次数降序排列的 UUID 列表
     */
    List<String> rerankEdgesByMentions(List<String> candidateUuids, int limit);

    /**
     * 按 Episode 提及次数重排节点
     *
     * @param candidateUuids 候选节点 UUID 列表
     * @param limit 返回数量
     * @return 按提及次数降序排列的 UUID 列表
     */
    List<String> rerankNodesByMentions(List<String> candidateUuids, int limit);

    /**
     * 获取提及次数统计
     *
     * @param candidateUuids 候选 UUID 列表
     * @return UUID -> 提及次数
     */
    Map<String, Integer> getMentionCounts(List<String> candidateUuids);
}
