package com.graphiti.module.graphiti.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graphiti.common.exception.BusinessException;
import com.graphiti.module.graphiti.dal.dataobject.GraphMetadataDO;
import com.graphiti.module.graphiti.dal.mysql.GraphMetadataMapper;
import com.graphiti.module.graphiti.service.GraphitiService;
import com.graphiti.module.graphiti.vo.graph.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 图谱管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphitiServiceImpl implements GraphitiService {
    private final GraphMetadataMapper graphMetadataMapper;
    private final com.graphiti.module.graphiti.service.GraphNeo4jService graphNeo4jService;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GraphInfoRespVO createGraph(CreateGraphReqVO reqVO) {
        // 构造图谱元数据
        GraphMetadataDO entity = new GraphMetadataDO();
        entity.setGraphId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(reqVO.getName());
        entity.setDescription(reqVO.getDescription());
        entity.setNodeCount(0);
        entity.setEdgeCount(0);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setDeleted(false);
        // 保存到数据库
        graphMetadataMapper.insert(entity);
        // 构造响应
        return convertToGraphInfoRespVO(entity);
    }
    @Override
    public GraphListRespVO listGraphs(Long limit, Long offset) {
        if (limit == null) limit = 100L;
        if (offset == null) offset = 0L;
        LambdaQueryWrapper<GraphMetadataDO> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(GraphMetadataDO::getDeleted, false);
        long total = graphMetadataMapper.selectCount(countWrapper);

        LambdaQueryWrapper<GraphMetadataDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GraphMetadataDO::getDeleted, false);
        wrapper.orderByDesc(GraphMetadataDO::getCreateTime);
        wrapper.last("LIMIT " + limit + " OFFSET " + offset);
        List<GraphMetadataDO> list = graphMetadataMapper.selectList(wrapper);

        List<GraphInfoVO> graphs = list.stream()
            .map(this::convertToGraphInfoVO)
            .collect(Collectors.toList());

        GraphListRespVO respVO = new GraphListRespVO();
        respVO.setGraphs(graphs);
        respVO.setTotalCount(total);
        respVO.setRowCount(graphs.size());
        return respVO;
    }

    @Override
    public GraphListRespVO listGraphs() {
        return listGraphs(null, null);
    }
    @Override
    public GraphInfoRespVO getGraph(String graphId) {
        GraphMetadataDO entity = getGraphMetadataByGraphId(graphId);
        return convertToGraphInfoRespVO(entity);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GraphInfoRespVO updateGraph(String graphId, UpdateGraphReqVO reqVO) {
        GraphMetadataDO entity = getGraphMetadataByGraphId(graphId);
        // 更新字段
        if (reqVO.getName() != null) {
            entity.setName(reqVO.getName());
        }
        if (reqVO.getDescription() != null) {
            entity.setDescription(reqVO.getDescription());
        }
        entity.setUpdateTime(LocalDateTime.now());
        graphMetadataMapper.updateById(entity);
        return convertToGraphInfoRespVO(entity);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGraph(String graphId) {
        GraphMetadataDO entity = getGraphMetadataByGraphId(graphId);
        // 1. 清除 Neo4j 中所有关系（RELATES_TO, MENTIONS, HAS_COMMUNITY）
        graphNeo4jService.clearAllRelationships(graphId);
        // 2. 删除 Neo4j 中所有社区节点
        graphNeo4jService.deleteAllCommunities(graphId);
        // 3. 删除 Neo4j 中所有实体节点和事件节点
        graphNeo4jService.clearGraphData(graphId);
        // 4. 逻辑删除 MySQL 元数据
        entity.setDeleted(true);
        entity.setUpdateTime(LocalDateTime.now());
        graphMetadataMapper.updateById(entity);
        log.info("图谱已完整删除：graphId={}, name={}", graphId, entity.getName());
    }

    @Override
    public GraphDeletePreviewRespVO getGraphDeletePreview(String graphId) {
        GraphMetadataDO entity = getGraphMetadataByGraphId(graphId);
        Map<String, Long> neo4jStats = graphNeo4jService.getGraphStats(graphId);
        long entityNodeCount = neo4jStats.getOrDefault("nodeCount", 0L);
        long episodeCount = neo4jStats.getOrDefault("episodeCount", 0L);
        long relationshipCount = neo4jStats.getOrDefault("edgeCount", 0L);
        long communityNodeCount = graphNeo4jService.countCommunitiesByGraphId(graphId);

        long totalDataCount = entityNodeCount + episodeCount + relationshipCount + communityNodeCount;

        GraphDeletePreviewRespVO vo = new GraphDeletePreviewRespVO();
        vo.setGraphId(entity.getGraphId());
        vo.setName(entity.getName());
        vo.setDescription(entity.getDescription());
        vo.setNodeCount(entity.getNodeCount());
        vo.setEdgeCount(entity.getEdgeCount());
        vo.setEntityNodeCount(entityNodeCount);
        vo.setEpisodeCount(episodeCount);
        vo.setRelationshipCount(relationshipCount);
        vo.setCommunityNodeCount(communityNodeCount);
        vo.setHasData(totalDataCount > 0);
        vo.setTotalDataCount(totalDataCount);
        return vo;
    }
    @Override
    public void clearGraph(String graphId) {
        // TODO: 清空 Neo4j 中对应的图谱数据（根据 graph_id），但保留 MySQL 元数据
        log.info("已清空图谱数据：graphId={}", graphId);
        // 同时重置节点和边数量
        GraphMetadataDO entity = getGraphMetadataByGraphId(graphId);
        entity.setNodeCount(0);
        entity.setEdgeCount(0);
        graphMetadataMapper.updateById(entity);
    }
    @Override
    public GraphStatsRespVO getGraphStats() {
        GraphStatsRespVO statsVO = new GraphStatsRespVO();
        
        // 1. 查询图谱总数
        LambdaQueryWrapper<GraphMetadataDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GraphMetadataDO::getDeleted, false);
        Long totalGraphs = graphMetadataMapper.selectCount(wrapper);
        statsVO.setTotalGraphs(totalGraphs);
        
        // 2. 查询所有图谱的节点和边总数（从 MySQL 元数据）
        List<GraphMetadataDO> allGraphs = graphMetadataMapper.selectList(wrapper);
        long totalNodes = allGraphs.stream()
            .mapToLong(g -> g.getNodeCount() != null ? g.getNodeCount() : 0)
            .sum();
        long totalEdges = allGraphs.stream()
            .mapToLong(g -> g.getEdgeCount() != null ? g.getEdgeCount() : 0)
            .sum();
        statsVO.setTotalNodes(totalNodes);
        statsVO.setTotalEdges(totalEdges);
        
        // 3. 查询 Episode 总数（从 Neo4j）
        // 注意：这里需要遍历所有图谱统计 Episode，或者在 Neo4j 中直接查询所有 Episode
        // 暂时设置为 0，后续可以优化
        long totalEpisodes = 0L;
        for (GraphMetadataDO graph : allGraphs) {
            try {
                long episodeCount = graphNeo4jService.countEpisodesByGraphId(graph.getGraphId());
                totalEpisodes += episodeCount;
            } catch (Exception e) {
                log.warn("查询图谱 {} 的 Episode 数量失败: {}", graph.getGraphId(), e.getMessage());
            }
        }
        statsVO.setTotalEpisodes(totalEpisodes);
        
        // 4. 趋势数据暂时设置为默认值（后续可以从历史数据中计算）
        statsVO.setNodeTrend(0);
        statsVO.setEdgeTrend(0);
        statsVO.setEpisodeTrend(0);
        
        return statsVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GraphInfoRespVO cloneGraph(String graphId) {
        // 1. 获取原图谱元数据
        GraphMetadataDO sourceEntity = getGraphMetadataByGraphId(graphId);

        // 2. 创建新图谱元数据
        GraphMetadataDO newEntity = new GraphMetadataDO();
        String newGraphId = UUID.randomUUID().toString().replace("-", "");
        newEntity.setGraphId(newGraphId);
        newEntity.setName(sourceEntity.getName() + " (副本)");
        newEntity.setDescription(sourceEntity.getDescription());
        newEntity.setNodeCount(sourceEntity.getNodeCount());
        newEntity.setEdgeCount(sourceEntity.getEdgeCount());
        newEntity.setCreateTime(LocalDateTime.now());
        newEntity.setUpdateTime(LocalDateTime.now());
        newEntity.setDeleted(false);
        graphMetadataMapper.insert(newEntity);

        // 3. 克隆 Neo4j 数据（将原 graph_id 的节点/边复制到新 graph_id）
        graphNeo4jService.cloneGraphData(graphId, newGraphId);

        log.info("图谱克隆成功：source={}, target={}", graphId, newGraphId);
        return convertToGraphInfoRespVO(newEntity);
    }

    @Override
    public Map<String, Object> exportGraph(String graphId) {
        Map<String, Object> result = new HashMap<>();

        // 1. 图谱元数据
        GraphMetadataDO entity = getGraphMetadataByGraphId(graphId);
        result.put("graphId", entity.getGraphId());
        result.put("name", entity.getName());
        result.put("description", entity.getDescription());
        result.put("nodeCount", entity.getNodeCount());
        result.put("edgeCount", entity.getEdgeCount());
        result.put("createdAt", entity.getCreateTime());

        // 2. Neo4j 数据
        result.put("nodes", graphNeo4jService.getNodesByGraphId(graphId));
        result.put("edges", graphNeo4jService.getEdgesByGraphId(graphId));
        result.put("episodes", graphNeo4jService.getEpisodesByGraphId(graphId, 1000, 0));

        return result;
    }

    // ==================== 私有方法 ====================
    /**
     * 根据 graphId 查询图谱元数据
     * @param graphId 图谱ID
     * @return GraphMetadataDO
     * @throws BusinessException 如果图谱不存在或已删除
     */
    private GraphMetadataDO getGraphMetadataByGraphId(String graphId) {
        LambdaQueryWrapper<GraphMetadataDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GraphMetadataDO::getGraphId, graphId);
        wrapper.eq(GraphMetadataDO::getDeleted, false);
        GraphMetadataDO entity = graphMetadataMapper.selectOne(wrapper);
        if (entity == null) {
            throw new BusinessException(1001, "图谱不存在或已删除");
        }
        return entity;
    }
    /**
     * 转换为 GraphInfoRespVO
     * @param entity GraphMetadataDO
     * @return GraphInfoRespVO
     */
    private GraphInfoRespVO convertToGraphInfoRespVO(GraphMetadataDO entity) {
        GraphInfoRespVO respVO = new GraphInfoRespVO();
        respVO.setGraphId(entity.getGraphId());
        respVO.setName(entity.getName());
        respVO.setDescription(entity.getDescription());
        respVO.setNodeCount(entity.getNodeCount());
        respVO.setEdgeCount(entity.getEdgeCount());
        respVO.setCreatedAt(entity.getCreateTime());
        respVO.setUpdatedAt(entity.getUpdateTime());
        return respVO;
    }
    /**
     * 转换为 GraphListRespVO
     * @param entity GraphMetadataDO
     * @return GraphListRespVO
     */
    private GraphInfoVO convertToGraphInfoVO(GraphMetadataDO entity) {
        GraphInfoVO vo = new GraphInfoVO();
        vo.setGraphId(entity.getGraphId());
        vo.setName(entity.getName());
        vo.setDescription(entity.getDescription());
        vo.setNodeCount(entity.getNodeCount());
        vo.setEdgeCount(entity.getEdgeCount());
        vo.setCreatedAt(entity.getCreateTime());
        return vo;
    }
}
