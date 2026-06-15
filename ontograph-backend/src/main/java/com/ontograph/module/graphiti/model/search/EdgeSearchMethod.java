package com.ontograph.module.graphiti.model.search;

/**
 * 边搜索方法枚举
 *
 * <p>参考 Python 实现：graphiti_core/search/search_config_recipes.py
 */
public enum EdgeSearchMethod {
    /** 余弦相似度搜索 */
    cosine_similarity,
    /** BM25 全文搜索 */
    bm25,
    /** BFS 图遍历搜索 */
    bfs
}
