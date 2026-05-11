package com.graphiti.module.graphiti.namespace;

import com.graphiti.module.graphiti.service.DataImportService;
import com.graphiti.module.graphiti.service.EdgeService;
import com.graphiti.module.graphiti.service.EpisodeService;
import com.graphiti.module.graphiti.service.GraphNeo4jService;
import com.graphiti.module.graphiti.service.NodeService;
import com.graphiti.module.graphiti.vo.imports.AddDataReqVO;
import com.graphiti.module.graphiti.vo.imports.AddMessagesReqVO;
import com.graphiti.module.graphiti.vo.imports.FactTripleReqVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 摄取命名空间
 * 对应 Python: graphiti.ingest
 *
 * <p>封装数据的批量摄取接口：
 * <ul>
 *   <li>消息批量摄取（经过 LLM 实体/关系抽取）</li>
 *   <li>直接节点/边写入（绕过 LLM）</li>
 *   <li>三元组-fact 直接导入</li>
 *   <li>图谱清空</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class IngestNamespace {

    private final DataImportService dataImportService;
    private final NodeService nodeService;
    private final EdgeService edgeService;
    private final EpisodeService episodeService;
    private final GraphNeo4jService graphNeo4jService;

    /**
     * 批量摄取消息（经过 LLM 实体/关系抽取）
     */
    public void addMessages(AddMessagesReqVO reqVO) {
        log.info("IngestNamespace.addMessages: graphId={}, count={}",
                reqVO.getGraphId(), reqVO.getMessages().size());
        dataImportService.addMessages(reqVO);
    }

    /**
     * 直接添加实体节点（绕过 LLM 抽取）
     */
    public void addEntityNode(String graphId, java.util.Map<String, Object> nodeData) {
        log.debug("IngestNamespace.addEntityNode: graphId={}", graphId);
        dataImportService.addEntityNode(graphId, nodeData);
    }

    /**
     * 添加单条数据（LLM 抽取）
     */
    public void addData(AddDataReqVO reqVO) {
        log.info("IngestNamespace.addData: graphId={}", reqVO.getGraphId());
        dataImportService.addData(reqVO);
    }

    /**
     * 添加事实三元组
     */
    public void addFactTriple(FactTripleReqVO reqVO) {
        log.info("IngestNamespace.addFactTriple: graphId={}", reqVO.getGraphId());
        dataImportService.addFactTriple(reqVO);
    }

    /**
     * 删除实体边
     */
    public void deleteEntityEdge(String graphId, String edgeUuid) {
        log.info("IngestNamespace.deleteEntityEdge: graphId={}, edgeUuid={}", graphId, edgeUuid);
        edgeService.deleteEdge(graphId, edgeUuid);
    }

    /**
     * 删除 Episode
     */
    public void deleteEpisode(String graphId, String episodeUuid) {
        log.info("IngestNamespace.deleteEpisode: graphId={}, episodeUuid={}", graphId, episodeUuid);
        episodeService.deleteEpisode(graphId, episodeUuid);
    }

    /**
     * 清空图谱数据
     */
    public void clear(String graphId) {
        log.info("IngestNamespace.clear: graphId={}", graphId);
        graphNeo4jService.clearGraphData(graphId);
    }
}
