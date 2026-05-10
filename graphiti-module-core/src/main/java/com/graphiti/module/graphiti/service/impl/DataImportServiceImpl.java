package com.graphiti.module.graphiti.service.impl;

import com.graphiti.common.exception.BusinessException;
import com.graphiti.module.graphiti.service.DataImportService;
import com.graphiti.module.graphiti.service.GraphNeo4jService;
import com.graphiti.module.graphiti.service.TemporalService;
import com.graphiti.module.graphiti.vo.imports.AddDataBatchReqVO;
import com.graphiti.module.graphiti.vo.imports.AddDataReqVO;
import com.graphiti.module.graphiti.vo.imports.AddMessagesReqVO;
import com.graphiti.module.graphiti.vo.imports.FactTripleReqVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 数据导入服务实现类（简化版）
 *
 * <p>注意：LLM 实体提取功能标记为 TODO，当前版本仅实现基础数据写入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataImportServiceImpl implements DataImportService {

    private final GraphNeo4jService graphNeo4jService;
    private final TemporalService temporalService;

    @Override
    public void addData(AddDataReqVO reqVO) {
        // TODO: 集成 Spring AI 实现实体提取
        log.info("添加单条数据：graphId={}, content={}", reqVO.getGraphId(), reqVO.getContent());
        
        // 简化实现：直接创建 Episode
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
        
        log.info(" Episode 创建成功：uuid={}", episodeUuid);
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
        // TODO: 处理对话历史，提取实体和关系
        log.info("添加消息：graphId={}, messageCount={}", 
                 reqVO.getGraphId(), reqVO.getMessages().size());
        
        // 简化实现：将每条消息作为单独的 Episode 存储
        for (var msg : reqVO.getMessages()) {
            String episodeUuid = UUID.randomUUID().toString().replace("-", "");
            graphNeo4jService.createEpisode(
                reqVO.getGraphId(),
                episodeUuid,
                "Message-" + msg.getRole(),
                "message",
                "role: " + msg.getRole(),
                msg.getContent(),
                new HashMap<>()
            );
        }
    }

    @Override
    public void addFactTriple(FactTripleReqVO reqVO) {
        // TODO: 检查源节点和目标节点是否存在，不存在则创建
        log.info("添加事实三元组：graphId={}, source={}, relation={}, target={}",
                 reqVO.getGraphId(), reqVO.getSourceNodeName(), 
                 reqVO.getRelationType(), reqVO.getTargetNodeName());
        
        // 简化实现：创建源节点、目标节点和关系
        String sourceUuid = UUID.randomUUID().toString().replace("-", "");
        String targetUuid = UUID.randomUUID().toString().replace("-", "");
        String edgeUuid = UUID.randomUUID().toString().replace("-", "");
        
        // 时序管理：如果同名实体已存在，先失效旧实体
        temporalService.invalidateFacts(reqVO.getGraphId(),
            List.of(reqVO.getSourceNodeName(), reqVO.getTargetNodeName()));
        
        // 创建源节点
        graphNeo4jService.createEntityNode(
            reqVO.getGraphId(),
            sourceUuid,
            reqVO.getSourceNodeName(),
            "Entity",
            "",
            null,
            new HashMap<>()
        );
        
        // 创建目标节点
        graphNeo4jService.createEntityNode(
            reqVO.getGraphId(),
            targetUuid,
            reqVO.getTargetNodeName(),
            "Entity",
            "",
            null,
            new HashMap<>()
        );
        
        // 创建关系
        Map<String, Object> props = reqVO.getProperties() != null ? reqVO.getProperties() : new HashMap<>();
        String fact = reqVO.getFact() != null ? reqVO.getFact() : "";
        
        graphNeo4jService.createRelationship(
            reqVO.getGraphId(),
            edgeUuid,
            sourceUuid,
            targetUuid,
            reqVO.getRelationType(),
            fact,
            null,
            props
        );
        
        log.info("事实三元组创建成功：sourceUuid={}, targetUuid={}, edgeUuid={}",
                 sourceUuid, targetUuid, edgeUuid);
    }

    @Override
    public void addEntityNode(String graphId, Map<String, Object> nodeData) {
        // 直接写入实体节点，不经过 LLM 提取
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String name = (String) nodeData.get("name");
        String type = (String) nodeData.getOrDefault("type", "Entity");
        Map<String, Object> properties = (Map<String, Object>) nodeData.getOrDefault("properties", new HashMap<>());
        
        if (name == null || name.isEmpty()) {
            throw new BusinessException(1006, "节点名称不能为空");
        }
        
        // 时序管理：如果同名实体已存在，先失效旧实体
        temporalService.invalidateFacts(graphId, Collections.singletonList(name));
        
        graphNeo4jService.createEntityNode(graphId, uuid, name, type, "", null, properties);
        log.info("实体节点创建成功：graphId={}, uuid={}, name={}", graphId, uuid, name);
    }
}
