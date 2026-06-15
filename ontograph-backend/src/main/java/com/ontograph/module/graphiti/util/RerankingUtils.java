package com.ontograph.module.graphiti.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 搜索结果重排工具类
 *
 * <p>参考 Python 实现：graphiti_core/search/search_utils.py
 *
 * <p>支持的策略：
 * <ul>
 *   <li>RRF (Reciprocal Rank Fusion): 倒数排名融合</li>
 *   <li>MMR (Maximal Marginal Relevance): 最大边际相关性</li>
 *   <li>Node Distance: 图形距离重排</li>
 *   <li>Episode Mentions: 提及次数重排</li>
 * </ul>
 */
public class RerankingUtils {

    // RRF 默认参数
    public static final int RRF_DEFAULT_K = 60;

    // MMR 默认参数
    public static final double MMR_DEFAULT_LAMBDA = 0.5;

    // 最小相似度阈值
    public static final double MIN_SIMILARITY_SCORE = 0.6;

    private RerankingUtils() {}

    // ==================== RRF (Reciprocal Rank Fusion) ====================

    /**
     * 倒数排名融合 (RRF)
     *
     * <p>公式：score(doc) = Σ 1 / (rank + k)，k 默认值为 60
     *
     * <p>参考 Python 实现：search_utils.py:1779-1795
     *
     * @param resultLists 多个排名列表
     * @param k RRF 参数
     * @param <T> 元素类型
     * @return 按 RRF 分数排序的元素列表
     */
    @SafeVarargs
    public static <T> List<RerankedItem<T>> rrf(List<T>... resultLists) {
        return rrf(Arrays.asList(resultLists), RRF_DEFAULT_K);
    }

    /**
     * 倒数排名融合 (RRF)
     *
     * @param resultLists 多个排名列表
     * @param k RRF 参数
     * @param <T> 元素类型
     * @return 按 RRF 分数排序的元素列表
     */
    public static <T> List<RerankedItem<T>> rrf(List<List<T>> resultLists, int k) {
        Map<T, Double> scores = new HashMap<>();
        Map<T, Integer> ranks = new HashMap<>();

        // 计算每个列表中每个元素的排名
        for (List<T> result : resultLists) {
            for (int i = 0; i < result.size(); i++) {
                T item = result.get(i);
                if (!ranks.containsKey(item) || ranks.get(item) > i) {
                    ranks.put(item, i);
                }
            }
        }

        // 计算 RRF 分数
        for (Map.Entry<T, Integer> entry : ranks.entrySet()) {
            T item = entry.getKey();
            int rank = entry.getValue();
            double rrfScore = 1.0 / (k + rank + 1);
            scores.merge(item, rrfScore, Double::sum);
        }

        // 排序并返回结果
        return scores.entrySet().stream()
                .sorted(Map.Entry.<T, Double>comparingByValue().reversed())
                .map(e -> new RerankedItem<>(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    // ==================== MMR (Maximal Marginal Relevance) ====================

    /**
     * 最大边际相关性 (MMR)
     *
     * <p>公式：MMR(doc) = λ * sim(query, doc) - (1-λ) * max(sim(selected_docs, doc))
     *
     * <p>λ = 1 表示纯相关性，λ = 0 表示纯多样性
     *
     * <p>参考 Python 实现：search_utils.py:1901-1939
     *
     * @param query 查询向量
     * @param candidates 候选文档及其向量
     * @param lambda 平衡参数 (0-1)
     * @param limit 返回数量
     * @param <T> 元素类型
     * @return 按 MMR 分数排序的元素列表
     */
    public static <T> List<RerankedItem<T>> mmr(
            double[] query,
            Map<T, double[]> candidates,
            double lambda,
            int limit) {
        return mmr(query, candidates, lambda, limit, MIN_SIMILARITY_SCORE);
    }

    /**
     * 最大边际相关性 (MMR)
     *
     * @param query 查询向量
     * @param candidates 候选文档及其向量
     * @param lambda 平衡参数 (0-1)
     * @param limit 返回数量
     * @param minScore 最小分数阈值
     * @param <T> 元素类型
     * @return 按 MMR 分数排序的元素列表
     */
    public static <T> List<RerankedItem<T>> mmr(
            double[] query,
            Map<T, double[]> candidates,
            double lambda,
            int limit,
            double minScore) {

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<RerankedItem<T>> selected = new ArrayList<>();
        Set<T> remaining = new HashSet<>(candidates.keySet());

        while (!remaining.isEmpty() && selected.size() < limit) {
            RerankedItem<T> best = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (T candidate : remaining) {
                double relevance = cosineSimilarity(query, candidates.get(candidate));

                if (relevance < minScore) {
                    continue;
                }

                double maxSimToSelected = 0;
                for (RerankedItem<T> s : selected) {
                    double sim = cosineSimilarity(candidates.get(s.item), candidates.get(candidate));
                    maxSimToSelected = Math.max(maxSimToSelected, sim);
                }

                // MMR 公式
                double mmrScore = lambda * relevance - (1 - lambda) * maxSimToSelected;

                if (mmrScore > bestScore) {
                    bestScore = mmrScore;
                    best = new RerankedItem<>(candidate, mmrScore);
                }
            }

            if (best != null) {
                selected.add(best);
                remaining.remove(best.item);
            } else {
                break;
            }
        }

        return selected;
    }

    /**
     * MMR 重排（基于分数和文本相似度）
     *
     * @param query 查询文本
     * @param items 候选项目
     * @param scoreExtractor 分数提取器
     * @param textExtractor 文本提取器
     * @param lambda 平衡参数
     * @param limit 返回数量
     * @param <T> 元素类型
     * @return 按 MMR 分数排序的元素列表
     */
    public static <T> List<RerankedItem<T>> mmrByText(
            String query,
            List<T> items,
            java.util.function.Function<T, Double> scoreExtractor,
            java.util.function.Function<T, String> textExtractor,
            double lambda,
            int limit) {

        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        List<RerankedItem<T>> selected = new ArrayList<>();
        List<T> remaining = new ArrayList<>(items);

        // 按初始分数排序
        remaining.sort((a, b) -> Double.compare(
                scoreExtractor.apply(b) != null ? scoreExtractor.apply(b) : 0,
                scoreExtractor.apply(a) != null ? scoreExtractor.apply(a) : 0));

        // 选择第一个
        if (!remaining.isEmpty()) {
            T first = remaining.remove(0);
            double score = scoreExtractor.apply(first) != null ? scoreExtractor.apply(first) : 0;
            selected.add(new RerankedItem<>(first, score));
        }

        // 选择其余的
        while (!remaining.isEmpty() && selected.size() < limit) {
            RerankedItem<T> best = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            Set<String> selectedTexts = selected.stream()
                    .map(s -> normalizeText(textExtractor.apply(s.item)))
                    .collect(Collectors.toSet());

            String queryNorm = normalizeText(query);

            for (T candidate : remaining) {
                double relevance = scoreExtractor.apply(candidate) != null ? scoreExtractor.apply(candidate) : 0;
                String candidateText = normalizeText(textExtractor.apply(candidate));

                // 计算与已选文本的最大相似度
                double maxSim = selectedTexts.stream()
                        .mapToDouble(s -> textSimilarity(queryNorm, s, candidateText))
                        .max()
                        .orElse(0);

                // MMR 公式
                double mmrScore = lambda * relevance - (1 - lambda) * maxSim;

                if (mmrScore > bestScore) {
                    bestScore = mmrScore;
                    best = new RerankedItem<>(candidate, mmrScore);
                }
            }

            if (best != null) {
                selected.add(best);
                remaining.remove(best.item);
            } else {
                break;
            }
        }

        return selected;
    }

    // ==================== Node Distance Reranking ====================

    /**
     * 基于图形距离的重排
     *
     * <p>参考 Python 实现：search_utils.py:1798-1857
     *
     * @param nodeUuids 候选节点 UUID
     * @param centerNodeUuid 中心节点 UUID
     * @param distanceFunction 距离计算函数（返回节点到中心节点的距离）
     * @param limit 返回数量
     * @param <T> 元素类型
     * @return 按距离排序的元素列表
     */
    public static <T> List<RerankedItem<T>> byNodeDistance(
            List<T> nodeUuids,
            String centerNodeUuid,
            java.util.function.Function<T, Integer> distanceFunction,
            int limit) {

        return nodeUuids.stream()
                .map(item -> {
                    int distance = distanceFunction.apply(item);
                    // 距离越小，分数越高
                    double score = distance == Integer.MAX_VALUE ? 0 : 1.0 / (distance + 1);
                    return new RerankedItem<>(item, score);
                })
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ==================== Episode Mentions Reranking ====================

    /**
     * 基于提及次数的重排
     *
     * <p>参考 Python 实现：search_utils.py:1860-1898
     *
     * @param items 候选项目
     * @param mentionCountExtractor 提及次数提取器
     * @param limit 返回数量
     * @param <T> 元素类型
     * @return 按提及次数排序的元素列表
     */
    public static <T> List<RerankedItem<T>> byEpisodeMentions(
            List<T> items,
            java.util.function.Function<T, Integer> mentionCountExtractor,
            int limit) {

        return items.stream()
                .map(item -> {
                    int mentions = mentionCountExtractor.apply(item);
                    return new RerankedItem<>(item, mentions);
                })
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ==================== Utility Methods ====================

    /**
     * 计算余弦相似度
     */
    public static double cosineSimilarity(double[] a, double[] b) {
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

    /**
     * 计算文本相似度（Jaccard 风格）
     */
    private static double textSimilarity(String query, String selected, String candidate) {
        Set<String> querySet = new HashSet<>(Arrays.asList(query.toLowerCase().split("\\s+")));
        Set<String> selectedSet = new HashSet<>(Arrays.asList(selected.toLowerCase().split("\\s+")));
        Set<String> candidateSet = new HashSet<>(Arrays.asList(candidate.toLowerCase().split("\\s+")));

        if (querySet.isEmpty() || candidateSet.isEmpty()) {
            return 0;
        }

        Set<String> intersection = new HashSet<>(querySet);
        intersection.retainAll(candidateSet);

        return (double) intersection.size() / Math.sqrt(querySet.size() * candidateSet.size());
    }

    /**
     * 规范化文本
     */
    private static String normalizeText(String text) {
        return text != null ? text.toLowerCase().trim() : "";
    }

    /**
     * 重排结果项
     */
    public static class RerankedItem<T> {
        public final T item;
        public final double score;

        public RerankedItem(T item, double score) {
            this.item = item;
            this.score = score;
        }

        @Override
        public String toString() {
            return String.format("RerankedItem{item=%s, score=%.4f}", item, score);
        }
    }
}
