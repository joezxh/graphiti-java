package com.graphiti.module.graphiti.service.impl;

import com.graphiti.common.exception.BusinessException;
import com.graphiti.module.graphiti.service.GraphNeo4jService;
import com.graphiti.module.graphiti.service.NodeService;
import com.graphiti.module.graphiti.vo.node.NodeFilterReqVO;
import com.graphiti.module.graphiti.vo.node.NodeInfoRespVO;
import com.graphiti.module.graphiti.vo.node.NodeListRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 节点管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NodeServiceImpl implements NodeService {
    
    private final GraphNeo4jService graphNeo4jService;
    
    @Override
    public List<NodeListRespVO> listNodes(String graphId, NodeFilterReqVO filterReqVO) {
        // 查询节点列表（分页）
        List<Map<String, Object>> nodes = graphNeo4jService.listNodes(
            graphId, 
            filterReqVO.getSkip() != null ? filterReqVO.getSkip() : 0L,
            filterReqVO.getLimit() != null ? filterReqVO.getLimit() : 20L
        );
        
        // 转换为响应 VO
        List<NodeListRespVO> respList = new ArrayList<>();
        for (Map<String, Object> node : nodes) {
            respList.add(convertToNodeListRespVO(node));
        }
        return respList;
    }
    
    @Override
    public NodeInfoRespVO getNodeDetail(String graphId, String nodeUuid) {
        Map<String, Object> node = graphNeo4jService.getEntityNode(graphId, nodeUuid);
        if (node == null) {
            throw new BusinessException(1003, "节点不存在");
        }
        return convertToNodeInfoRespVO(node);
    }
    
    @Override
    public NodeInfoRespVO createNode(String graphId, Map<String, Object> nodeData) {
        // 生成节点 UUID
        String uuid = UUID.randomUUID().toString().replace("-", "");
        
        // 提取节点属性
        String name = (String) nodeData.get("name");
        String type = (String) nodeData.get("type");
        Map<String, Object> properties = (Map<String, Object>) nodeData.getOrDefault("properties", new HashMap<>());
        
        if (name == null || name.isEmpty()) {
            throw new BusinessException(1006, "节点名称不能为空");
        }
        
        // 创建节点
        Map<String, Object> createdNode = graphNeo4jService.createEntityNode(
            graphId, uuid, name, type != null ? type : "Entity", properties);
        
        if (createdNode == null) {
            throw new BusinessException(500, "创建节点失败");
        }
        
        // 更新图谱元数据中的节点数量
        updateGraphNodeCount(graphId, 1);
        
        return convertToNodeInfoRespVO(createdNode);
    }
    
    @Override
    public NodeInfoRespVO updateNode(String graphId, String nodeUuid, Map<String, Object> nodeData) {
        // TODO: 实现节点更新逻辑（Neo4j Cypher: MATCH (n) SET n += $props）
        throw new BusinessException(500, "节点更新功能待实现");
    }
    
    @Override
    public void deleteNode(String graphId, String nodeUuid) {
        graphNeo4jService.deleteEntityNode(graphId, nodeUuid);
        
        // 更新图谱元数据中的节点数量
        updateGraphNodeCount(graphId, -1);
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 转换为 NodeListRespVO
     */
    private NodeListRespVO convertToNodeListRespVO(Map<String, Object> node) {
        NodeListRespVO respVO = new NodeListRespVO();
        respVO.setUuid((String) node.get("uuid"));
        respVO.setName((String) node.get("name"));
        respVO.setType((String) node.get("type"));
        
        // 提取属性（排除系统字段）
        Map<String, Object> props = new HashMap<>(node);
        props.remove("uuid");
        props.remove("name");
        props.remove("type");
        props.remove("group_id");
        respVO.setProperties(props);
        
        return respVO;
    }
    
    /**
     * 转换为 NodeInfoRespVO
     */
    private NodeInfoRespVO convertToNodeInfoRespVO(Map<String, Object> node) {
        NodeInfoRespVO respVO = new NodeInfoRespVO();
        respVO.setUuid((String) node.get("uuid"));
        respVO.setName((String) node.get("name"));
        respVO.setType((String) node.get("type"));
        respVO.setSummary((String) node.get("summary"));
        
        // 提取属性（排除系统字段）
        Map<String, Object> props = new HashMap<>(node);
        props.remove("uuid");
        props.remove("name");
        props.remove("type");
        props.remove("summary");
        props.remove("group_id");
        respVO.setProperties(props);
        
        return respVO;
    }
    
    /**
     * 更新图谱元数据中的节点数量
     * @param graphId 图谱ID
     * @param delta 变化量（+1 或 -1）
     */
    private void updateGraphNodeCount(String graphId, int delta) {
        // TODO: 调用 GraphitiService 更新节点数量
        log.info("更新图谱节点数量：graphId={}, delta={}", graphId, delta);
    }
}
