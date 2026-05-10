package com.graphiti.module.graphiti.service;

import java.util.List;

/**
 * 嵌入向量服务接口
 */
public interface EmbedderService {

    /**
     * 生成文本嵌入向量
     */
    float[] embed(String text);

    /**
     * 批量生成嵌入向量
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * 计算余弦相似度
     */
    double cosineSimilarity(float[] a, float[] b);

    /**
     * 获取向量维度
     */
    int getDimensions();
}
