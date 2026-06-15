package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.vo.search.SearchResultsRespVO;

import java.util.function.Supplier;

/**
 * 搜索结果缓存服务接口
 *
 * <p>提供搜索结果的二级缓存（L1 Caffeine + L2 Redis）
 * <p>缓存 Key = MD5(query + graphId + config)
 */
public interface SearchResultCacheService {

    /**
     * 获取或计算搜索结果（缓存模式）
     *
     * @param query 查询文本
     * @param graphId 图谱 ID
     * @param computer 计算函数（实际执行搜索）
     * @return 搜索结果
     */
    SearchResultsRespVO getOrCompute(String query, String graphId, Supplier<SearchResultsRespVO> computer);

    /**
     * 获取或计算搜索结果（带配置）
     *
     * @param query 查询文本
     * @param graphId 图谱 ID
     * @param configKey 配置 Key（MD5）
     * @param computer 计算函数
     * @return 搜索结果
     */
    SearchResultsRespVO getOrCompute(String query, String graphId, String configKey,
                                    Supplier<SearchResultsRespVO> computer);

    /**
     * 使指定图谱的缓存失效
     *
     * @param graphId 图谱 ID
     */
    void invalidateByGraphId(String graphId);

    /**
     * 使指定查询的缓存失效
     *
     * @param query 查询文本
     * @param graphId 图谱 ID
     */
    void invalidate(String query, String graphId);

    /**
     * 清空所有搜索结果缓存
     */
    void invalidateAll();

    /**
     * 构建缓存 Key
     *
     * @param query 查询文本
     * @param graphId 图谱 ID
     * @return MD5 缓存 Key
     */
    String buildCacheKey(String query, String graphId);

    /**
     * 构建缓存 Key（带配置）
     *
     * @param query 查询文本
     * @param graphId 图谱 ID
     * @param configKey 配置 Key
     * @return MD5 缓存 Key
     */
    String buildCacheKey(String query, String graphId, String configKey);
}
