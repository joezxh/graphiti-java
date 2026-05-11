package com.graphiti.module.graphiti.namespace.node;

import com.graphiti.module.graphiti.service.EmbedderService;
import com.graphiti.module.graphiti.service.GraphNeo4jService;
import com.graphiti.module.graphiti.service.NodeService;
import com.graphiti.module.graphiti.vo.node.NodeInfoRespVO;
import com.graphiti.module.graphiti.vo.node.NodeListRespVO;
import com.graphiti.module.graphiti.vo.edge.EdgeListRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;

/**
 * 实体节点命名空间
 * 对应 Python: graphiti.nodes.entity
 *
 * <p>职责：
 * <ul>
 *   <li>持有 NodeService、EmbedderService、GraphNeo4jService 引用</li>
 *   <li>业务逻辑在 Namespace 层（如 embedding 生成前的准备）</li>
 *   <li>数据操作委托给底层 Service 层</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class EntityNodeNamespace {

    private final NodeService nodeService;
    private final EmbedderService embedderService;
    private final GraphNeo4jService graphNeo4jService;

    /**
     * 保存实体节点（生成 name embedding）
     */
    public NodeInfoRespVO save(String graphId, Map<String, Object> nodeData) {
        log.debug("EntityNodeNamespace.save: graphId={}", graphId);
        return nodeService.createNode(graphId, nodeData);
    }

    /**
     * 批量保存实体节点
     */
    public List<NodeInfoRespVO> saveBulk(String graphId, List<Map<String, Object>> nodesData) {
        log.debug("EntityNodeNamespace.saveBulk: graphId={}, count={}", graphId, nodesData.size());
        return nodesData.stream()
                .map(data -> nodeService.createNode(graphId, data))
                .toList();
    }

    /**
     * 按图谱ID获取节点列表
     */
    public List<NodeListRespVO> getByGraphId(String graphId, Long skip, Long limit) {
        var filter = new com.graphiti.module.graphiti.vo.node.NodeFilterReqVO();
        filter.setSkip(skip != null ? skip : 0L);
        filter.setLimit(limit != null ? limit : 20L);
        return nodeService.listNodes(graphId, filter);
    }

    /**
     * 按 UUID 获取节点详情
     */
    public NodeInfoRespVO getByUuid(String graphId, String nodeUuid) {
        return nodeService.getNodeDetail(graphId, nodeUuid);
    }

    /**
     * 获取节点关联的边
     */
    public List<EdgeListRespVO> getEdges(String graphId, String nodeUuid, long skip, long limit) {
        return nodeService.getNodeEdges(graphId, nodeUuid, skip, limit);
    }

    /**
     * 删除节点
     */
    public void delete(String graphId, String nodeUuid) {
        nodeService.deleteNode(graphId, nodeUuid);
    }

    /**
     * 重新生成节点的 embedding
     */
    public void refreshEmbedding(String graphId, String nodeUuid) {
        // 1. 获取节点当前数据
        NodeInfoRespVO node = nodeService.getNodeDetail(graphId, nodeUuid);
        // 2. 用 embedder 重新生成向量
        String embedText = node.getName() + (node.getSummary() != null ? " " + node.getSummary() : "");
        float[] embedding = embedderService.embed(embedText);
        // 3. 更新到 Neo4j
        graphNeo4jService.updateNodeEmbedding(graphId, nodeUuid, embedding);
        log.info("EntityNodeNamespace.refreshEmbedding: nodeUuid={}", nodeUuid);
    }
}
