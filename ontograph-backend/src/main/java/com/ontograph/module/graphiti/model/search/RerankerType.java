package com.ontograph.module.graphiti.model.search;

/**
 * 重排策略枚举
 *
 * <p>参考 Python 实现：graphiti_core/search/search_config_recipes.py
 *
 * <p>支持的策略：
 * <ul>
 *   <li>RRF: 倒数排名融合（默认）</li>
 *   <li>MMR: 最大边际相关性</li>
 *   <li>CROSS_ENCODER: LLM 语义重排</li>
 *   <li>NODE_DISTANCE: 基于图距离的重排</li>
 *   <li>EPISODE_MENTIONS: 基于 Episode 提及次数的重排</li>
 * </ul>
 */
public enum RerankerType {
    /** 倒数排名融合（Reciprocal Rank Fusion） */
    rrf,

    /** 最大边际相关性（Maximal Marginal Relevance） */
    mmr,

    /** Cross-Encoder LLM 语义重排 */
    cross_encoder,

    /** 基于图距离的重排 */
    node_distance,

    /** 基于 Episode 提及次数的重排 */
    episode_mentions
}
