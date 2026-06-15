package com.ontograph.module.graphiti.service.impl;

import com.ontograph.common.exception.BusinessException;
import com.ontograph.module.graphiti.exception.OntologyValidationException;
import com.ontograph.module.graphiti.service.*;
import com.ontograph.module.graphiti.vo.imports.AddDataBatchReqVO;
import com.ontograph.module.graphiti.vo.imports.AddDataReqVO;
import com.ontograph.module.graphiti.vo.imports.AddMessagesReqVO;
import com.ontograph.module.graphiti.vo.imports.FactTripleReqVO;
import com.ontograph.module.graphiti.vo.llm.ExtractedEntityVO;
import com.ontograph.module.graphiti.vo.llm.ExtractedRelationVO;
import com.ontograph.module.graphiti.vo.ontology.ValidationResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 数据导入服务实现类
 *
 * <p>集成 LLM 实体提取能力，支持从文本/对话中自动提取实体和关系。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataImportServiceImpl implements DataImportService {

    private final GraphNeo4jService graphNeo4jService;
    private final TemporalService temporalService;
    private final OntologyValidationService ontologyValidationService;
    private final EmbedderService embedderService;
    private final LlmClientService llmClientService;

    @Override
    public void addData(AddDataReqVO reqVO) {
        log.info("添加单条数据：graphId={}, content={}", reqVO.getGraphId(), reqVO.getContent());

        // 1. 创建 Episode
        String episodeUuid = UUID.randomUUID().toString().replace("-", "");
        graphNeo4jService.createEpisode(
            reqVO.getGraphId(),
            episodeUuid,
            reqVO.getName() != null ? reqVO.getName() : "Episode-" + System.currentTimeMillis(),
            reqVO.getSourceType(),
            reqVO.getSourceDescription(),
            reqVO.getContent(),
            new HashMap<>()
        );

        // 2. LLM 提取实体和关系
        String content = reqVO.getContent();
        if (content != null && !content.isBlank()) {
            List<ExtractedEntityVO> entities = llmClientService.extractEntities(content);
            List<ExtractedRelationVO> relations = llmClientService.extractRelations(content);

            log.info("LLM 提取结果：entities={}, relations={}", entities.size(), relations.size());

            if (!entities.isEmpty()) {
                // 3. 创建节点
                Map<String, String> entityNameToUuid = new HashMap<>();
                for (ExtractedEntityVO entity : entities) {
                    String nodeUuid = UUID.randomUUID().toString().replace("-", "");
                    String name = entity.getName();
                    String type = entity.getType() != null ? entity.getType() : "Entity";
                    String summary = entity.getSummary() != null ? entity.getSummary() : "";
                    Map<String, Object> props = entity.getAttributes() != null ? entity.getAttributes() : new HashMap<>();

                    // 时序管理：失效同名旧实体
                    temporalService.invalidateFacts(reqVO.getGraphId(), Collections.singletonList(name));

                    // 生成嵌入向量
                    String embedText = name + (summary.isEmpty() ? "" : " " + summary);
                    float[] embedding = embedderService.embed(embedText);

                    // 创建节点
                    graphNeo4jService.createEntityNode(
                        reqVO.getGraphId(), nodeUuid, name, type,
                        summary, embedding, props
                    );
                    entityNameToUuid.put(name, nodeUuid);
                    log.info("  创建节点：name={}, type={}, uuid={}", name, type, nodeUuid);
                }

                // 4. 创建关系
                for (ExtractedRelationVO relation : relations) {
                    String sourceUuid = entityNameToUuid.get(relation.getSource());
                    String targetUuid = entityNameToUuid.get(relation.getTarget());
                    if (sourceUuid != null && targetUuid != null) {
                        String edgeUuid = UUID.randomUUID().toString().replace("-", "");
                        String fact = relation.getFact() != null ? relation.getFact() : "";

                        // 生成关系嵌入
                        float[] embedding = embedderService.embed(fact.isEmpty() ? relation.getType() : fact);

                        graphNeo4jService.createRelationship(
                            reqVO.getGraphId(), edgeUuid, sourceUuid, targetUuid,
                            relation.getType(), fact, embedding, new HashMap<>()
                        );
                        log.info("  创建关系：{} -[{}]-> {}", relation.getSource(), relation.getType(), relation.getTarget());
                    } else {
                        log.warn("  关系节点未找到：source={}, target={}", relation.getSource(), relation.getTarget());
                    }
                }
            }
        }

        log.info("数据添加完成：graphId={}, episodeUuid={}", reqVO.getGraphId(), episodeUuid);
    }

    @Override
    public void addDataBatch(AddDataBatchReqVO reqVO) {
        // TODO: 批量处理，优化性能
        log.info("批量添加数据：graphId={}, count={}", reqVO.getGraphId(), reqVO.getItems().size());
        
        for (var item : reqVO.getItems()) {
            AddDataReqVO singleReq = new AddDataReqVO();
            singleReq.setGraphId(reqVO.getGraphId());
            singleReq.setContent(item.getContent());
            singleReq.setSourceType(item.getSourceType());
            singleReq.setSourceDescription(item.getSourceDescription());
            singleReq.setName(item.getName());
            
            addData(singleReq);
        }
    }

    @Override
    public void addMessages(AddMessagesReqVO reqVO) {
        log.info("添加消息：graphId={}, messageCount={}",
                 reqVO.getGraphId(), reqVO.getMessages().size());

        // 合并对话内容
        StringBuilder conversation = new StringBuilder();
        for (var msg : reqVO.getMessages()) {
            conversation.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        String content = conversation.toString();

        // 创建 Episode（整个对话作为一个 Episode）
        String episodeUuid = UUID.randomUUID().toString().replace("-", "");
        graphNeo4jService.createEpisode(
            reqVO.getGraphId(),
            episodeUuid,
            "Conversation",
            "message",
            "multi-turn conversation",
            content,
            new HashMap<>()
        );

        // LLM 提取实体和关系
        if (!content.isBlank()) {
            List<ExtractedEntityVO> entities = llmClientService.extractEntities(content);
            List<ExtractedRelationVO> relations = llmClientService.extractRelations(content);

            log.info("消息 LLM 提取结果：entities={}, relations={}", entities.size(), relations.size());

            if (!entities.isEmpty()) {
                Map<String, String> entityNameToUuid = new HashMap<>();
                for (ExtractedEntityVO entity : entities) {
                    String nodeUuid = UUID.randomUUID().toString().replace("-", "");
                    String name = entity.getName();
                    String type = entity.getType() != null ? entity.getType() : "Entity";
                    String summary = entity.getSummary() != null ? entity.getSummary() : "";
                    Map<String, Object> props = entity.getAttributes() != null ? entity.getAttributes() : new HashMap<>();

                    temporalService.invalidateFacts(reqVO.getGraphId(), Collections.singletonList(name));

                    String embedText = name + (summary.isEmpty() ? "" : " " + summary);
                    float[] embedding = embedderService.embed(embedText);

                    graphNeo4jService.createEntityNode(
                        reqVO.getGraphId(), nodeUuid, name, type,
                        summary, embedding, props
                    );
                    entityNameToUuid.put(name, nodeUuid);
                }

                for (ExtractedRelationVO relation : relations) {
                    String sourceUuid = entityNameToUuid.get(relation.getSource());
                    String targetUuid = entityNameToUuid.get(relation.getTarget());
                    if (sourceUuid != null && targetUuid != null) {
                        String edgeUuid = UUID.randomUUID().toString().replace("-", "");
                        String fact = relation.getFact() != null ? relation.getFact() : "";
                        float[] embedding = embedderService.embed(fact.isEmpty() ? relation.getType() : fact);

                        graphNeo4jService.createRelationship(
                            reqVO.getGraphId(), edgeUuid, sourceUuid, targetUuid,
                            relation.getType(), fact, embedding, new HashMap<>()
                        );
                    }
                }
            }
        }
    }

    @Override
    public void addFactTriple(FactTripleReqVO reqVO) {
        log.info("添加事实三元组：graphId={}, source={}, relation={}, target={}",
                 reqVO.getGraphId(), reqVO.getSourceNodeName(),
                 reqVO.getRelationType(), reqVO.getTargetNodeName());

        // 1. 查找或创建源节点
        String sourceUuid = findOrCreateNode(reqVO.getGraphId(), reqVO.getSourceNodeName());

        // 2. 查找或创建目标节点
        String targetUuid = findOrCreateNode(reqVO.getGraphId(), reqVO.getTargetNodeName());

        // 3. 创建关系
        String edgeUuid = UUID.randomUUID().toString().replace("-", "");
        String fact = reqVO.getFact() != null ? reqVO.getFact() : "";
        String relationType = reqVO.getRelationType() != null ? reqVO.getRelationType() : "RELATES_TO";
        float[] embedding = embedderService.embed(fact.isEmpty() ? relationType : fact);
        Map<String, Object> properties = reqVO.getProperties() != null ? reqVO.getProperties() : new HashMap<>();

        graphNeo4jService.createRelationship(
            reqVO.getGraphId(), edgeUuid, sourceUuid, targetUuid,
            relationType, fact, embedding, properties
        );

        log.info("事实三元组创建成功：sourceUuid={}, targetUuid={}, edgeUuid={}",
                 sourceUuid, targetUuid, edgeUuid);
    }

    /**
     * 查找或创建节点
     */
    private String findOrCreateNode(String graphId, String nodeName) {
        // 使用 listNodes 分页查询，按名称匹配
        List<Map<String, Object>> nodes = graphNeo4jService.listNodes(graphId, 0, 1000);
        for (Map<String, Object> node : nodes) {
            if (nodeName.equals(node.get("name"))) {
                return (String) node.get("uuid");
            }
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        float[] embedding = embedderService.embed(nodeName);
        graphNeo4jService.createEntityNode(
            graphId, uuid, nodeName, "Entity", "", embedding, new HashMap<>()
        );
        return uuid;
    }

    @Override
    public void addEntityNode(String graphId, Map<String, Object> nodeData) {
        // 直接写入实体节点，不经过 LLM 提取
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String name = (String) nodeData.get("name");
        String type = (String) nodeData.getOrDefault("type", "Entity");
        String summary = (String) nodeData.get("summary");
        Map<String, Object> properties = (Map<String, Object>) nodeData.getOrDefault("properties", new HashMap<>());

        if (ontologyValidationService.hasOntology(graphId)) {
            ValidationResultVO vr = ontologyValidationService.validateNode(
                graphId, type != null ? type : "Entity", properties);
            if (!vr.isPassed()) {
                throw new OntologyValidationException(vr);
            }
            if (vr.getEnrichedProperties() != null && !vr.getEnrichedProperties().isEmpty()) {
                Map<String, Object> merged = new HashMap<>(vr.getEnrichedProperties());
                properties.forEach(merged::putIfAbsent);
                properties = merged;
            }
        }

        if (name == null || name.isEmpty()) {
            throw new BusinessException(1006, "节点名称不能为空");
        }

        // 时序管理：如果同名实体已存在，先失效旧实体
        temporalService.invalidateFacts(graphId, Collections.singletonList(name));

        String embedText = name + (summary != null ? " " + summary : "");
        float[] embedding = embedderService.embed(embedText);

        graphNeo4jService.createEntityNode(
            graphId, uuid, name, type, summary != null ? summary : "", embedding, properties);
        log.info("实体节点创建成功：graphId={}, uuid={}, name={}", graphId, uuid, name);
    }

    @Override
    public void deleteEntityEdge(String edgeUuid) {
        log.info("删除实体边：edgeUuid={}", edgeUuid);
        Map<String, Object> edge = graphNeo4jService.getEdgeByUuidOnly(edgeUuid);
        if (edge == null) {
            throw new BusinessException(404, "边不存在");
        }
        // getEdgeByUuidOnly 不返回 graphId，需要从 edge 的 graph_id 获取
        Object groupId = edge.get("graph_id");
        if (groupId != null) {
            graphNeo4jService.deleteEdge(groupId.toString(), edgeUuid);
        } else {
            // 兜底：直接删除（通过 edgeUuid 全局匹配）
            deleteEdgeByUuidOnly(edgeUuid);
        }
    }

    @Override
    public void deleteGroup(String graphId) {
        log.info("删除图谱数据：graphId={}", graphId);
        graphNeo4jService.clearGraphData(graphId);
    }

    @Override
    public void deleteEpisode(String episodeUuid) {
        log.info("删除 Episode：episodeUuid={}", episodeUuid);
        Map<String, Object> episode = graphNeo4jService.getEpisodeByUuid("", episodeUuid);
        if (episode == null) {
            throw new BusinessException(404, "Episode 不存在");
        }
        String graphId = (String) episode.get("graph_id");
        graphNeo4jService.deleteEpisode(graphId != null ? graphId : "", episodeUuid);
    }

    private void deleteEdgeByUuidOnly(String edgeUuid) {
        String cypher = "MATCH ()-[r {uuid: $uuid}]->() DELETE r";
        try (org.neo4j.driver.Session session = graphNeo4jService.getNeo4jDriver().session()) {
            session.run(cypher, org.neo4j.driver.Values.parameters("uuid", edgeUuid));
        }
    }
}
