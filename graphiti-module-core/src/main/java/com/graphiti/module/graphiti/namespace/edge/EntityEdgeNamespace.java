package com.graphiti.module.graphiti.namespace.edge;

import com.graphiti.module.graphiti.service.EdgeService;
import com.graphiti.module.graphiti.service.EmbedderService;
import com.graphiti.module.graphiti.service.GraphNeo4jService;
import com.graphiti.module.graphiti.vo.edge.EdgeInfoRespVO;
import com.graphiti.module.graphiti.vo.edge.EdgeListRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;

/**
 * 实体边命名空间
 * 对应 Python: graphiti.edges.entity
 *
 * <p>实体边连接两个 Entity 节点，表示实体间的关系。
 * 保存时自动生成 fact embedding。
 */
@Slf4j
@RequiredArgsConstructor
public class EntityEdgeNamespace {

    private final EdgeService edgeService;
    private final EmbedderService embedderService;
    private final GraphNeo4jService graphNeo4jService;

    /**
     * 保存实体边（生成 fact embedding）
     */
    public EdgeInfoRespVO save(String graphId, Map<String, Object> edgeData) {
        log.debug("EntityEdgeNamespace.save: graphId={}", graphId);
        return edgeService.createEdge(graphId, edgeData);
    }

    /**
     * 批量保存实体边
     */
    public List<EdgeInfoRespVO> saveBulk(String graphId, List<Map<String, Object>> edgesData) {
        log.debug("EntityEdgeNamespace.saveBulk: graphId={}, count={}", graphId, edgesData.size());
        return edgesData.stream()
                .map(data -> edgeService.createEdge(graphId, data))
                .toList();
    }

    /**
     * 按图谱ID获取边列表
     */
    public List<EdgeListRespVO> getByGraphId(String graphId, Long skip, Long limit) {
        var filter = new com.graphiti.module.graphiti.vo.edge.EdgeFilterReqVO();
        filter.setSkip(skip != null ? skip : 0L);
        filter.setLimit(limit != null ? limit : 20L);
        return edgeService.listEdges(graphId, filter);
    }

    /**
     * 按 UUID 获取边详情
     */
    public EdgeInfoRespVO getByUuid(String graphId, String edgeUuid) {
        return edgeService.getEdgeDetail(graphId, edgeUuid);
    }

    /**
     * 获取两节点间的所有边
     */
    public List<EdgeListRespVO> getBetweenNodes(String sourceUuid, String targetUuid) {
        return edgeService.getEdgesBetweenNodes(sourceUuid, targetUuid);
    }

    /**
     * 删除边
     */
    public void delete(String graphId, String edgeUuid) {
        edgeService.deleteEdge(graphId, edgeUuid);
    }

    /**
     * 重新生成边的 fact embedding
     */
    public void refreshEmbedding(String graphId, String edgeUuid) {
        EdgeInfoRespVO edge = edgeService.getEdgeDetail(graphId, edgeUuid);
        String embedText = edge.getType() + " " + (edge.getProperties() != null ? edge.getProperties().getOrDefault("fact", "").toString() : "");
        float[] embedding = embedderService.embed(embedText);
        graphNeo4jService.updateEdgeEmbedding(graphId, edgeUuid, embedding);
        log.info("EntityEdgeNamespace.refreshEmbedding: edgeUuid={}", edgeUuid);
    }
}
