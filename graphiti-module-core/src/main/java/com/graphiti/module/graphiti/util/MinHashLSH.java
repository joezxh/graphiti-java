package com.graphiti.module.graphiti.util;

import java.util.*;

/**
 * MinHash + LSH (Locality-Sensitive Hashing) 实现
 * 用于实体去重的 Tier 2 语义匹配
 *
 * <p>参考 Python 实现：graphiti_core/utils/maintenance/dedup_helpers.py
 *
 * <p>配置参数（与 Python 保持一致）：
 * <ul>
 *   <li>PERMUTATIONS = 32：MinHash 哈希函数数量</li>
 *   <li>BAND_SIZE = 4：LSH 分段大小（8 bands = 32/4）</li>
 *   <li>JACCARD_THRESHOLD = 0.9：Jaccard 相似度阈值</li>
 * </ul>
 */
public class MinHashLSH {

    // MinHash 配置
    private static final int PERMUTATIONS = 32;
    private static final int BAND_SIZE = 4;
    private static final int NUM_BANDS = PERMUTATIONS / BAND_SIZE;

    // Jaccard 相似度阈值
    private static final double JACCARD_THRESHOLD = 0.9;

    // LSH 桶索引
    private final List<Map<String, Set<String>>> lshIndex;

    // 存储每个文档的签名
    private final Map<String, int[]> signatures;

    public MinHashLSH() {
        this.lshIndex = new ArrayList<>();
        for (int i = 0; i < NUM_BANDS; i++) {
            lshIndex.add(new HashMap<>());
        }
        this.signatures = new HashMap<>();
    }

    /**
     * 添加字符串到索引
     *
     * @param id 字符串唯一标识
     * @param text 字符串文本
     */
    public void add(String id, String text) {
        int[] signature = computeSignature(text);
        signatures.put(id, signature);

        // 插入 LSH 桶
        for (int band = 0; band < NUM_BANDS; band++) {
            int start = band * BAND_SIZE;
            int[] bandHash = new int[BAND_SIZE];
            System.arraycopy(signature, start, bandHash, 0, BAND_SIZE);

            String bucketKey = Arrays.hashCode(bandHash) + ":" + band;
            lshIndex.get(band).computeIfAbsent(bucketKey, k -> new HashSet<>()).add(id);
        }
    }

    /**
     * 查找与给定文本相似的所有 ID
     *
     * @param id 给定 ID
     * @return 相似 ID 集合
     */
    public Set<String> findSimilar(String id) {
        int[] querySignature = signatures.get(id);
        if (querySignature == null) {
            return Collections.emptySet();
        }

        Set<String> candidates = new HashSet<>();

        // 从 LSH 桶中收集候选
        for (int band = 0; band < NUM_BANDS; band++) {
            int start = band * BAND_SIZE;
            int[] bandHash = new int[BAND_SIZE];
            System.arraycopy(querySignature, start, bandHash, 0, BAND_SIZE);

            String bucketKey = Arrays.hashCode(bandHash) + ":" + band;
            Set<String> bucket = lshIndex.get(band).get(bucketKey);
            if (bucket != null) {
                candidates.addAll(bucket);
            }
        }

        // 移除自身
        candidates.remove(id);

        return candidates;
    }

    /**
     * 计算两个 ID 之间的 MinHash Jaccard 相似度
     *
     * @param id1 ID1
     * @param id2 ID2
     * @return Jaccard 相似度 (0-1)
     */
    public double getSimilarity(String id1, String id2) {
        int[] sig1 = signatures.get(id1);
        int[] sig2 = signatures.get(id2);

        if (sig1 == null || sig2 == null) {
            return 0.0;
        }

        int matches = 0;
        for (int i = 0; i < PERMUTATIONS; i++) {
            if (sig1[i] == sig2[i]) {
                matches++;
            }
        }

        return (double) matches / PERMUTATIONS;
    }

    /**
     * 获取 Jaccard 相似度阈值
     *
     * @return 阈值
     */
    public static double getJaccardThreshold() {
        return JACCARD_THRESHOLD;
    }

    /**
     * 计算 MinHash 签名
     */
    private int[] computeSignature(String text) {
        Set<String> shingles = generateShingles(text, 3);
        int[] signature = new int[PERMUTATIONS];

        // 使用多个哈希函数
        for (int i = 0; i < PERMUTATIONS; i++) {
            int minHash = Integer.MAX_VALUE;

            for (String shingle : shingles) {
                int hash = hashShingle(shingle, i);
                minHash = Math.min(minHash, hash);
            }

            signature[i] = minHash;
        }

        return signature;
    }

    /**
     * 生成 3-gram shingle
     */
    private Set<String> generateShingles(String text, int k) {
        Set<String> shingles = new HashSet<>();
        String normalized = text.toLowerCase().trim();

        if (normalized.length() < k) {
            shingles.add(normalized);
            return shingles;
        }

        for (int i = 0; i <= normalized.length() - k; i++) {
            shingles.add(normalized.substring(i, i + k));
        }

        return shingles;
    }

    /**
     * 带盐值的哈希函数
     */
    private int hashShingle(String shingle, int seed) {
        // 使用 Java 的 hashCode 结合种子值
        int h = shingle.hashCode();
        // 简单的线性同余生成器
        return Math.abs((h ^ (0x9e3779b9 + (seed << 6) + (seed >> 2))) * 0x85ebca6b);
    }

    /**
     * 清空索引
     */
    public void clear() {
        lshIndex.forEach(Map::clear);
        signatures.clear();
    }

    /**
     * 获取已索引的 ID 数量
     */
    public int size() {
        return signatures.size();
    }
}
