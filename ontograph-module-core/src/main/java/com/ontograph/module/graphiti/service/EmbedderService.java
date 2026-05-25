package com.ontograph.module.graphiti.service;

import java.util.List;

/**
 * 嵌入向量服务接口
 * 提供统一的文本嵌入能力，屏蔽底层 Provider 差异
 */
public interface EmbedderService {

    /**
     * 对单个文本进行嵌入
     *
     * @param text 输入文本
     * @return 嵌入向量
     */
    float[] embed(String text);

    /**
     * 对批量文本进行嵌入
     *
     * @param texts 输入文本列表
     * @return 嵌入向量列表
     */
    List<float[]> embed(List<String> texts);

    /**
     * 获取嵌入向量维度
     *
     * @return 维度大小
     */
    int getDimensions();

    /**
     * 获取当前使用的 Provider 名称
     *
     * @return Provider 名称
     */
    String getProvider();
}
