package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.ide.GraphVisualizationRespVO;
import com.graphiti.module.graphiti.vo.node.NodeInfoRespVO;

import java.util.List;
import java.util.Map;

/**
 * 图谱可视化服务接口
 */
public interface GraphVisualizationService {

    /**
     * 获取图谱可视化数据
     */
    GraphVisualizationRespVO getVisualizationData(
            String graphId,
            String layout,
            Integer page,
            Integer pageSize,
            String classType,
            String keyword);

    /**
     * 获取图谱元数据
     */
    Map<String, Object> getGraphMetadata(String graphId);

    /**
     * 获取节点详情
     */
    NodeInfoRespVO getNodeDetail(String graphId, String nodeUuid);

    /**
     * 创建节点
     */
    NodeInfoRespVO createNode(String graphId, Map<String, Object> nodeData);

    /**
     * 更新节点
     */
    NodeInfoRespVO updateNode(String graphId, String nodeUuid, Map<String, Object> nodeData);

    /**
     * 删除节点
     */
    void deleteNode(String graphId, String nodeUuid);

    /**
     * 创建边
     */
    GraphVisualizationRespVO.EdgeVO createEdge(String graphId, Map<String, Object> edgeData);

    /**
     * 展开邻居节点
     */
    GraphVisualizationRespVO expandNeighbors(String graphId, String nodeUuid, Map<String, Object> options);

    /**
     * 按多个类别获取可视化数据
     */
    GraphVisualizationRespVO getVisualizationDataByTypes(
            String graphId,
            String layout,
            Integer page,
            Integer pageSize,
            List<String> classTypes,
            String keyword);

    /**
     * 获取实例数据（按类别过滤）
     */
    GraphVisualizationRespVO getInstances(String graphId, String classType, int page, int pageSize);

    /**
     * 获取所有边数据
     */
    GraphVisualizationRespVO getEdges(String graphId, int limit);

    /**
     * 获取事件流可视化数据
     */
    GraphVisualizationRespVO getEpisodesVisualization(String graphId, int limit);

    /**
     * V5.0: 根据剧集类型获取分页可视化数据（含N跳邻居）
     */
    GraphVisualizationRespVO getEpisodesVisualizationByType(
            String graphId,
            String typeCode,
            Integer page,
            Integer pageSize,
            Integer depth);

    /**
     * 获取社区可视化数据
     */
    GraphVisualizationRespVO getCommunityVisualization(String graphId, int limit);
}
