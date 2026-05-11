package com.graphiti.module.graphiti.namespace.graph;

import com.graphiti.module.graphiti.service.GraphitiService;
import com.graphiti.module.graphiti.vo.graph.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

/**
 * 图谱元数据命名空间
 * 对应 Python: graphiti.graphs.metadata
 *
 * <p>封装图谱的创建、克隆、导出等元数据操作。
 */
@Slf4j
@RequiredArgsConstructor
public class GraphMetadataNamespace {

    private final GraphitiService graphitiService;

    /**
     * 创建图谱
     */
    public GraphInfoRespVO create(CreateGraphReqVO reqVO) {
        log.debug("GraphMetadataNamespace.create: name={}", reqVO.getName());
        return graphitiService.createGraph(reqVO);
    }

    /**
     * 获取图谱详情
     */
    public GraphInfoRespVO get(String graphId) {
        return graphitiService.getGraph(graphId);
    }

    /**
     * 更新图谱
     */
    public GraphInfoRespVO update(String graphId, UpdateGraphReqVO reqVO) {
        return graphitiService.updateGraph(graphId, reqVO);
    }

    /**
     * 删除图谱
     */
    public void delete(String graphId) {
        graphitiService.deleteGraph(graphId);
    }

    /**
     * 克隆图谱
     */
    public GraphInfoRespVO clone(String graphId) {
        return graphitiService.cloneGraph(graphId);
    }

    /**
     * 清空图谱数据
     */
    public void clear(String graphId) {
        graphitiService.clearGraph(graphId);
    }

    /**
     * 导出图谱数据
     */
    public Map<String, Object> export(String graphId) {
        return graphitiService.exportGraph(graphId);
    }

    /**
     * 获取图谱统计信息
     */
    public GraphStatsRespVO stats() {
        return graphitiService.getGraphStats();
    }
}
