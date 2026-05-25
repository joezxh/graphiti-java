package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.vo.dedup.DedupResultVO;

import java.util.List;
import java.util.Map;

/**
 * 实体去重服务接口
 *
 * <p>实现三层去重策略（与 Python graphiti 保持一致）：
 * <ol>
 *   <li>Tier 1: Exact Match（规范化字符串匹配）</li>
 *   <li>Tier 2: Semantic Match（MinHash + LSH，Jaccard >= 0.9）</li>
 *   <li>Tier 3: LLM Resolution（LLM 判断是否重复）</li>
 * </ol>
 *
 * <p>阈值配置（参考 graphiti_core/utils/maintenance/dedup_helpers.py）：
 * <ul>
 *   <li>NODE_DEDUP_COSINE_MIN_SCORE = 0.6：向量相似度阈值</li>
 *   <li>NODE_DEDUP_CANDIDATE_LIMIT = 15：每节点最大候选数</li>
 *   <li>_FUZZY_JACCARD_THRESHOLD = 0.9：MinHash Jaccard 阈值</li>
 * </ul>
 */
public interface EntityDedupService {

    /**
     * 对提取的实体列表进行去重
     *
     * @param graphId 图谱ID
     * @param extractedEntities 提取的实体列表（包含 name, type, summary 等）
     * @param existingNodes 现有实体节点（用于匹配）
     * @return 去重结果，包含：
     *         <ul>
     *           <li>resolvedNodes: 已解析的实体（映射到现有节点）</li>
     *           <li>newNodes: 需要新建的实体</li>
     *           <li>uuidMapping: 名称到UUID的映射</li>
     *         </ul>
     */
    DedupResultVO deduplicate(String graphId, List<Map<String, Object>> extractedEntities,
                              List<Map<String, Object>> existingNodes);

    /**
     * 精确匹配去重（Tier 1）
     *
     * @param entities 实体列表
     * @return 合并后的实体列表
     */
    List<Map<String, Object>> exactMatch(List<Map<String, Object>> entities);

    /**
     * 语义匹配去重（Tier 2）- 使用 MinHash + LSH
     *
     * @param entities 实体列表
     * @return 合并后的实体列表
     */
    List<Map<String, Object>> semanticMatch(List<Map<String, Object>> entities);

    /**
     * LLM 去重（Tier 3）
     *
     * @param graphId 图谱ID
     * @param entities 实体列表
     * @param context 上下文信息（用于 LLM 判断）
     * @return 去重后的实体列表
     */
    List<Map<String, Object>> llmDedup(String graphId, List<Map<String, Object>> entities, String context);
}
