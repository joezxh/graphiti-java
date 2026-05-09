package com.graphiti.module.graphiti.service.impl;

import com.graphiti.common.exception.BusinessException;
import com.graphiti.module.graphiti.service.EdgeService;
import com.graphiti.module.graphiti.service.GraphNeo4jService;
import com.graphiti.module.graphiti.vo.edge.EdgeFilterReqVO;
import com.graphiti.module.graphiti.vo.edge.EdgeInfoRespVO;
import com.graphiti.module.graphiti.vo.edge.EdgeListRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 边管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EdgeServiceImpl implements EdgeService {
    
    private final GraphNeo4jService graphNeo4jService;
    
    @Override
    public List<EdgeListRespVO> listEdges(String graphId, EdgeFilterReqVO filterReqVO) {
        // 查询关系列表（分页）
        List<Map<String, Object>> edges = graphNeo4jService.listEdges(
            graphId,
            filterReqVO.getType(),
            filterReqVO.getSource(),
            filterReqVO.getTarget(),
            filterReqVO.getSkip() != null ? filterReqVO.getSkip() : 0L,
            filterReqVO.getLimit() != null ? filterReqVO.getLimit() : 20L
        );
        
        // 转换为响应 VO
        List<EdgeListRespVO> respList = new ArrayList<>();
        for (Map<String, Object> edge : edges) {
            respList.add(convertToEdgeListRespVO(edge));
        }
        return respList;
    }
    
    @Override
    public EdgeInfoRespVO getEdgeDetail(String graphId, String edgeUuid) {
        Map<String, Object> edge = graphNeo4jService.getEdgeByUuid(graphId, edgeUuid);
        if (edge == null) {
            throw new BusinessException(1004, "边不存在");
        }
        return convertToEdgeInfoRespVO(edge);
    }
    
    @Override
    public EdgeInfoRespVO createEdge(String graphId, Map<String, Object> edgeData) {
        // 生成边 UUID
        String uuid = UUID.randomUUID().toString().replace("-", "");
        
        // 提取边属性
        String source = (String) edgeData.get("source");
        String target = (String) edgeData.get("target");
        String type = (String) edgeData.get("type");
        Map<String, Object> properties = (Map<String, Object>) edgeData.getOrDefault("properties", new HashMap<>());
        
        if (source == null || source.isEmpty()) {
            throw new BusinessException(1007, "源节点UUID不能为空");
        }
        if (target == null || target.isEmpty()) {
            throw new BusinessException(1008, "目标节点UUID不能为空");
        }
        if (type == null || type.isEmpty()) {
            throw new BusinessException(1009, "关系类型不能为空");
        }
        
        // 创建关系
        Map<String, Object> createdEdge = graphNeo4jService.createRelationship(
            graphId, uuid, source, target, type, properties);
        
        if (createdEdge == null) {
            throw new BusinessException(500, "创建边失败");
        }
        
        return convertToEdgeInfoRespVO(createdEdge);
    }
    
    @Override
    public EdgeInfoRespVO updateEdge(String graphId, String edgeUuid, Map<String, Object> edgeData) {
        // TODO: 实现边更新逻辑（Neo4j Cypher: MATCH ()-[r]->() SET r += $props）
        throw new BusinessException(500, "边更新功能待实现");
    }
    
    @Override
    public void deleteEdge(String graphId, String edgeUuid) {
        graphNeo4jService.deleteEdge(graphId, edgeUuid);
        log.info("删除边：graphId={}, edgeUuid={}", graphId, edgeUuid);
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 转换为 EdgeListRespVO
     */
    private EdgeListRespVO convertToEdgeListRespVO(Map<String, Object> edge) {
        EdgeListRespVO respVO = new EdgeListRespVO();
        respVO.setUuid((String) edge.get("uuid"));
        respVO.setSource((String) edge.get("source"));
        respVO.setTarget((String) edge.get("target"));
        respVO.setType((String) edge.get("type"));
        
        // 提取属性（排除系统字段）
        Map<String, Object> props = new java.util.HashMap<>(edge);
        props.remove("uuid");
        props.remove("source");
        props.remove("target");
        props.remove("type");
        props.remove("group_id");
        respVO.setProperties(props);
        
        return respVO;
    }
    
    /**
     * 转换为 EdgeInfoRespVO
     */
    private EdgeInfoRespVO convertToEdgeInfoRespVO(Map<String, Object> edge) {
        EdgeInfoRespVO respVO = new EdgeInfoRespVO();
        respVO.setUuid((String) edge.get("uuid"));
        respVO.setSource((String) edge.get("source"));
        respVO.setTarget((String) edge.get("target"));
        respVO.setType((String) edge.get("type"));
        
        // 提取属性（排除系统字段）
        Map<String, Object> props = new java.util.HashMap<>(edge);
        props.remove("uuid");
        props.remove("source");
        props.remove("target");
        props.remove("type");
        props.remove("group_id");
        respVO.setProperties(props);
        
        return respVO;
    }
}
