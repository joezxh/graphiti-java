package com.graphiti.module.graphiti.vo.search;

/**
 * 搜索配方枚举
 * 预置常用的检索策略组合
 */
public enum SearchRecipe {

    /**
     * 边混合检索：BM25 + 向量 + RRF
     */
    EDGE_HYBRID_SEARCH_RRF("边混合检索", "bm25", "vector", true, false),

    /**
     * 边混合 + 节点距离
     */
    EDGE_HYBRID_SEARCH_NODE_DISTANCE("边混合+节点距离", "bm25", "vector", true, false),

    /**
     * 组合混合 + Cross Encoder
     */
    COMBINED_HYBRID_SEARCH_CROSS_ENCODER("组合混合+重排序", "bm25", "vector", true, true),

    /**
     * 节点语义搜索
     */
    NODE_SEMANTIC_SEARCH("节点语义搜索", null, "vector", false, false),

    /**
     * Episode 搜索
     */
    EPISODE_SEARCH("Episode搜索", "bm25", null, false, false),

    /**
     * 社区搜索
     */
    COMMUNITY_SEARCH("社区搜索", "bm25", "vector", true, false);

    private final String description;
    private final String bm25Mode;
    private final String vectorMode;
    private final boolean useRrf;
    private final boolean useMmr;

    SearchRecipe(String description, String bm25Mode, String vectorMode, boolean useRrf, boolean useMmr) {
        this.description = description;
        this.bm25Mode = bm25Mode;
        this.vectorMode = vectorMode;
        this.useRrf = useRrf;
        this.useMmr = useMmr;
    }

    public String getDescription() { return description; }
    public String getBm25Mode() { return bm25Mode; }
    public String getVectorMode() { return vectorMode; }
    public boolean isUseRrf() { return useRrf; }
    public boolean isUseMmr() { return useMmr; }
}
