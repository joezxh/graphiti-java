package com.ontograph.module.graphiti.service;

import java.util.List;
import java.util.Map;

/**
 * Cross-Encoder 重排服务
 *
 * <p>参考 Python 实现：graphiti_core/cross_encoder/openai_reranker_client.py
 *
 * <p>使用 LLM 判断每个候选项与查询的相关性，输出 0-1 的分数。
 * 支持 OpenAI GPT-4.1-nano 等支持 logprobs 的模型。
 */
public interface CrossEncoderRerankerService {

    /**
     * Cross-Encoder 重排边
     *
     * @param query 查询文本
     * @param facts 边的事实文本列表（uuid -> fact）
     * @param limit 返回数量
     * @return 按 LLM 打分降序排列的 UUID 列表
     */
    List<String> rankEdges(String query, Map<String, String> facts, int limit);

    /**
     * Cross-Encoder 重排节点
     *
     * @param query 查询文本
     * @param nodeNames 节点名称映射（uuid -> name）
     * @param nodeSummaries 节点摘要映射（uuid -> summary）
     * @param limit 返回数量
     * @return 按 LLM 打分降序排列的 UUID 列表
     */
    List<String> rankNodes(String query, Map<String, String> nodeNames,
                            Map<String, String> nodeSummaries, int limit);

    /**
     * 批量 Cross-Encoder 重排（并发控制）
     *
     * @param query 查询文本
     * @param passages 候选项文本列表
     * @param maxConcurrency 最大并发数
     * @return 按分数降序排列的 (passage, score) 列表
     */
    List<Map.Entry<String, Double>> rankBatch(String query, List<String> passages,
                                               int maxConcurrency);

    /**
     * 判断单条文本是否与查询相关（True/False）
     *
     * @param query 查询文本
     * @param passage 待判断文本
     * @return 相关性分数（0-1）
     */
    double scoreSingle(String query, String passage);
}
