package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.search.*;

/**
 * 搜索检索服务接口
 *
 * <p>定义了与 Graphiti 服务功能等价的全部操作：
 * <ul>
 *   <li>全局搜索（多 groupId）</li>
 *   <li>图谱级别搜索</li>
 *   <li>获取记忆（基于对话历史重建上下文）</li>
 * </ul>
 */
public interface SearchService {

    /**
     * 全局搜索（多 groupId）
     * @param reqVO 搜索请求
     * @return 搜索结果
     */
    SearchResultsRespVO search(SearchQueryReqVO reqVO);

    /**
     * 图谱级别搜索
     * @param graphId 图谱ID
     * @param reqVO 搜索请求
     * @return 搜索结果
     */
    SearchResultsRespVO searchGraph(String graphId, SearchQueryReqVO reqVO);

    /**
     * 获取记忆（基于对话历史重建上下文）
     * @param reqVO 获取记忆请求
     * @return 记忆响应
     */
    GetMemoryRespVO getMemory(GetMemoryReqVO reqVO);

    /**
     * 检索指定边（fact 格式）
     * @param edgeUuid 边UUID
     * @return 边的事实描述
     */
    FactResultVO getEntityEdge(String edgeUuid);

    /**
     * 获取最近的 Episode 列表
     * @param graphId 图谱ID
     * @param lastN 返回数量
     * @return Episode 列表
     */
    java.util.List<java.util.Map<String, Object>> getRecentEpisodes(String graphId, int lastN);
}
