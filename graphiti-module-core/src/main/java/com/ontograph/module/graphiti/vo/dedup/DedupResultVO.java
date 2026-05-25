package com.ontograph.module.graphiti.vo.dedup;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 实体去重结果 VO
 */
@Data
public class DedupResultVO {

    /**
     * 已解析的实体列表（映射到现有节点）
     * 每个元素包含：uuid, originalName, resolvedUuid
     */
    private List<Map<String, Object>> resolvedNodes;

    /**
     * 需要新建的实体列表
     * 每个元素包含：name, type, summary 等原始提取信息
     */
    private List<Map<String, Object>> newNodes;

    /**
     * 名称到UUID的映射
     * key: 实体名称（原始），value: UUID
     */
    private Map<String, String> uuidMapping;

    /**
     * 去重统计信息
     */
    private DedupStatsVO stats;

    @Data
    public static class DedupStatsVO {
        /**
         * 原始实体数量
         */
        private int originalCount;

        /**
         * 精确匹配去重数量
         */
        private int exactMatchCount;

        /**
         * 语义匹配去重数量
         */
        private int semanticMatchCount;

        /**
         * LLM 去重数量
         */
        private int llmDedupCount;

        /**
         * 最终实体数量
         */
        private int finalCount;

        /**
         * 总去重数量
         */
        public int getTotalDedupCount() {
            return exactMatchCount + semanticMatchCount + llmDedupCount;
        }
    }
}
