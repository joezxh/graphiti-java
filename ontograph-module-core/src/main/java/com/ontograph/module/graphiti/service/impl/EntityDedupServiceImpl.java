package com.ontograph.module.graphiti.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontograph.module.graphiti.service.EmbedderService;
import com.ontograph.module.graphiti.service.EntityDedupService;
import com.ontograph.module.graphiti.service.GraphNeo4jService;
import com.ontograph.module.graphiti.service.LlmClientService;
import com.ontograph.module.graphiti.util.MinHashLSH;
import com.ontograph.module.graphiti.util.StringNormalizer;
import com.ontograph.module.graphiti.util.UnionFind;
import com.ontograph.module.graphiti.vo.dedup.DedupResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 实体去重服务实现
 *
 * <p>三层去重策略（与 Python graphiti 保持一致）：
 * <ol>
 *   <li>Tier 1: Exact Match - 规范化字符串匹配</li>
 *   <li>Tier 2: Semantic Match - MinHash + LSH，Jaccard >= 0.9</li>
 *   <li>Tier 3: LLM Resolution - LLM 判断是否重复</li>
 * </ol>
 *
 * <p>阈值配置（参考 graphiti_core/utils/maintenance/dedup_helpers.py）：
 * <ul>
 *   <li>NODE_DEDUP_COSINE_MIN_SCORE = 0.6</li>
 *   <li>NODE_DEDUP_CANDIDATE_LIMIT = 15</li>
 *   <li>_FUZZY_JACCARD_THRESHOLD = 0.9</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntityDedupServiceImpl implements EntityDedupService {

    private final GraphNeo4jService graphNeo4jService;
    private final EmbedderService embedderService;
    private final LlmClientService llmClientService;

    // 阈值配置
    private static final double NODE_DEDUP_COSINE_MIN_SCORE = 0.6;
    private static final int NODE_DEDUP_CANDIDATE_LIMIT = 15;
    private static final double FUZZY_JACCARD_THRESHOLD = 0.9;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public DedupResultVO deduplicate(String graphId, List<Map<String, Object>> extractedEntities,
                                     List<Map<String, Object>> existingNodes) {
        log.info("开始实体去重：extracted={}, existing={}",
                extractedEntities.size(), existingNodes.size());

        DedupResultVO result = new DedupResultVO();
        DedupResultVO.DedupStatsVO stats = new DedupResultVO.DedupStatsVO();
        stats.setOriginalCount(extractedEntities.size());

        // 构建现有实体的规范化索引
        Map<String, Map<String, Object>> existingIndex = buildExistingIndex(existingNodes);
        Map<String, String> uuidMapping = new HashMap<>();
        List<Map<String, Object>> resolvedNodes = new ArrayList<>();
        List<Map<String, Object>> newNodes = new ArrayList<>();
        List<Map<String, Object>> remainingEntities = new ArrayList<>();

        // ===== Tier 1: Exact Match =====
        log.debug("Tier 1: 精确匹配去重");
        Set<String> processedExact = new HashSet<>();

        for (Map<String, Object> entity : extractedEntities) {
            String name = (String) entity.get("name");
            if (name == null || name.isBlank()) continue;

            String normalizedName = StringNormalizer.normalizeExact(name);
            Map<String, Object> existing = existingIndex.get(normalizedName);

            if (existing != null) {
                // 精确匹配到现有实体
                String existingUuid = (String) existing.get("uuid");
                resolvedNodes.add(buildResolvedNode(entity, existingUuid));
                uuidMapping.put(name, existingUuid);
                processedExact.add(name);
                stats.setExactMatchCount(stats.getExactMatchCount() + 1);
                log.debug("Tier 1 精确匹配：{} -> {}", name, existingUuid);
            } else {
                remainingEntities.add(entity);
            }
        }

        // ===== Tier 2: Semantic Match (MinHash + LSH) =====
        log.debug("Tier 2: 语义匹配去重 (MinHash + LSH)");
        MinHashLSH lsh = new MinHashLSH();

        // 为现有实体构建 LSH 索引
        for (Map.Entry<String, Map<String, Object>> entry : existingIndex.entrySet()) {
            Map<String, Object> existing = entry.getValue();
            String existingName = (String) existing.get("name");
            if (existingName != null) {
                lsh.add((String) existing.get("uuid"), existingName);
            }
        }

        // 为剩余实体构建 LSH 并查询相似
        Set<String> processedSemantic = new HashSet<>();
        for (Map<String, Object> entity : remainingEntities) {
            String name = (String) entity.get("name");
            if (name == null) continue;

            // 检查是否适合模糊匹配
            if (!StringNormalizer.hasEnoughEntropy(name)) {
                continue;
            }

            // 查询 LSH 候选
            Set<String> candidates = new HashSet<>();
            for (Map<String, Object> existing : existingNodes) {
                String existingName = (String) existing.get("name");
                if (existingName != null && lsh.getSimilarity((String) existing.get("uuid"), name)
                        >= FUZZY_JACCARD_THRESHOLD) {
                    candidates.add((String) existing.get("uuid"));
                }
            }

            if (!candidates.isEmpty()) {
                // 取第一个候选（通常是最佳匹配）
                String bestCandidate = candidates.iterator().next();
                resolvedNodes.add(buildResolvedNode(entity, bestCandidate));
                uuidMapping.put(name, bestCandidate);
                processedSemantic.add(name);
                stats.setSemanticMatchCount(stats.getSemanticMatchCount() + 1);
                log.debug("Tier 2 语义匹配：{} -> {}", name, bestCandidate);
            }
        }

        // 剩余实体需要 LLM 判断或新建
        for (Map<String, Object> entity : remainingEntities) {
            String name = (String) entity.get("name");
            if (name != null && !processedExact.contains(name) && !processedSemantic.contains(name)) {
                // 检查向量相似度
                Map<String, Object> matchedByVector = findByVectorSimilarity(
                        entity, existingIndex.values().stream().collect(Collectors.toList()));

                if (matchedByVector != null) {
                    String existingUuid = (String) matchedByVector.get("uuid");
                    resolvedNodes.add(buildResolvedNode(entity, existingUuid));
                    uuidMapping.put(name, existingUuid);
                    log.debug("向量相似度匹配：{} -> {}", name, existingUuid);
                } else {
                    // 需要新建
                    newNodes.add(entity);
                    uuidMapping.put(name, "NEW:" + name);  // 标记为新建
                }
            }
        }

        stats.setFinalCount(resolvedNodes.size() + newNodes.size());
        result.setResolvedNodes(resolvedNodes);
        result.setNewNodes(newNodes);
        result.setUuidMapping(uuidMapping);
        result.setStats(stats);

        log.info("去重完成：原始={}, 精确匹配={}, 语义匹配={}, 新建={}",
                stats.getOriginalCount(), stats.getExactMatchCount(),
                stats.getSemanticMatchCount(), newNodes.size());

        return result;
    }

    @Override
    public List<Map<String, Object>> exactMatch(List<Map<String, Object>> entities) {
        // 按规范化名称分组
        Map<String, List<Map<String, Object>>> groups = entities.stream()
                .filter(e -> e.get("name") != null)
                .collect(Collectors.groupingBy(e ->
                        StringNormalizer.normalizeExact((String) e.get("name"))));

        // 取每组第一个元素
        return groups.values().stream()
                .map(group -> group.get(0))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> semanticMatch(List<Map<String, Object>> entities) {
        // 使用 MinHash + LSH 进行语义匹配
        MinHashLSH lsh = new MinHashLSH();
        UnionFind<String> unionFind = new UnionFind<>(
                entities.stream()
                        .map(e -> (String) e.get("name"))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
        );

        // 构建 LSH 索引
        for (Map<String, Object> entity : entities) {
            String name = (String) entity.get("name");
            String id = name;  // 使用名称作为 ID
            lsh.add(id, name);
        }

        // 查找相似对并合并
        List<String> names = entities.stream()
                .map(e -> (String) e.get("name"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        for (int i = 0; i < names.size(); i++) {
            for (int j = i + 1; j < names.size(); j++) {
                String name1 = names.get(i);
                String name2 = names.get(j);

                if (lsh.getSimilarity(name1, name2) >= FUZZY_JACCARD_THRESHOLD) {
                    unionFind.union(name1, name2);
                }
            }
        }

        // 获取合并后的结果
        Map<String, List<Map<String, Object>>> groups = new HashMap<>();
        for (Map<String, Object> entity : entities) {
            String name = (String) entity.get("name");
            if (name != null) {
                String root = unionFind.find(name);
                groups.computeIfAbsent(root, k -> new ArrayList<>()).add(entity);
            }
        }

        // 取每组第一个元素
        return groups.values().stream()
                .map(group -> group.get(0))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> llmDedup(String graphId, List<Map<String, Object>> entities, String context) {
        if (entities.size() <= 1) {
            return entities;
        }

        // 构建 LLM 去重 prompt
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请判断以下实体列表中哪些是重复的。\n\n");
        promptBuilder.append("实体列表：\n");

        for (int i = 0; i < entities.size(); i++) {
            Map<String, Object> entity = entities.get(i);
            String name = (String) entity.get("name");
            String type = (String) entity.getOrDefault("type", "Entity");
            String summary = (String) entity.getOrDefault("summary", "");
            promptBuilder.append(String.format("[%d] %s (%s): %s\n", i, name, type, summary));
        }

        if (context != null && !context.isBlank()) {
            promptBuilder.append("\n上下文信息：\n").append(context).append("\n");
        }

        promptBuilder.append("\n请返回 JSON 格式的结果，格式如下：\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"duplicates\": [[0, 1], [3, 4]],  // 每组重复实体的索引\n");
        promptBuilder.append("  \"reason\": \"原因说明\"\n");
        promptBuilder.append("}\n");

        try {
            String response = llmClientService.chat(promptBuilder.toString());

            // 解析 JSON 响应
            Map<String, Object> llmResult = objectMapper.readValue(response,
                    new TypeReference<Map<String, Object>>() {});

            @SuppressWarnings("unchecked")
            List<List<Integer>> duplicates = (List<List<Integer>>) llmResult.get("duplicates");

            if (duplicates == null || duplicates.isEmpty()) {
                return entities;
            }

            // 使用 UnionFind 合并重复组
            List<String> names = entities.stream()
                    .map(e -> (String) e.get("name"))
                    .collect(Collectors.toList());
            UnionFind<Integer> unionFind = new UnionFind<>(
                    IntStream.range(0, entities.size()).boxed().collect(Collectors.toList())
            );

            for (List<Integer> dupGroup : duplicates) {
                for (int i = 1; i < dupGroup.size(); i++) {
                    unionFind.union(dupGroup.get(0), dupGroup.get(i));
                }
            }

            // 获取合并后的结果
            Map<Integer, List<Map<String, Object>>> groups = new HashMap<>();
            for (int i = 0; i < entities.size(); i++) {
                int root = unionFind.find(i);
                groups.computeIfAbsent(root, k -> new ArrayList<>()).add(entities.get(i));
            }

            // 取每组第一个元素
            return groups.values().stream()
                    .map(group -> group.get(0))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("LLM 去重失败：{}", e.getMessage());
            return entities;  // 失败时返回原始列表
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建现有实体的索引
     */
    private Map<String, Map<String, Object>> buildExistingIndex(List<Map<String, Object>> existingNodes) {
        Map<String, Map<String, Object>> index = new HashMap<>();
        for (Map<String, Object> node : existingNodes) {
            String name = (String) node.get("name");
            if (name != null) {
                String normalized = StringNormalizer.normalizeExact(name);
                index.put(normalized, node);
            }
        }
        return index;
    }

    /**
     * 构建已解析节点的 Map
     */
    private Map<String, Object> buildResolvedNode(Map<String, Object> original, String resolvedUuid) {
        Map<String, Object> resolved = new HashMap<>(original);
        resolved.put("resolvedUuid", resolvedUuid);
        resolved.put("originalName", original.get("name"));
        return resolved;
    }

    /**
     * 通过向量相似度查找匹配实体
     */
    private Map<String, Object> findByVectorSimilarity(Map<String, Object> entity,
                                                     List<Map<String, Object>> candidates) {
        String name = (String) entity.get("name");
        if (name == null || candidates.isEmpty()) {
            return null;
        }

        float[] entityEmbedding = embedderService.embed(name);

        Map<String, Object> bestMatch = null;
        double bestScore = NODE_DEDUP_COSINE_MIN_SCORE;

        for (Map<String, Object> candidate : candidates) {
            if (candidate.get("embedding") == null) continue;

            double similarity = calculateCosineSimilarity(
                    entityEmbedding,
                    (float[]) candidate.get("embedding")
            );

            if (similarity > bestScore) {
                bestScore = similarity;
                bestMatch = candidate;
            }
        }

        return bestMatch;
    }

    /**
     * 计算余弦相似度
     */
    private double calculateCosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
