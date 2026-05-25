package com.graphiti.module.graphiti.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.graphiti.common.exception.BusinessException;
import com.graphiti.module.graphiti.dal.dataobject.GraphMetadataDO;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntDefinitionDO;
import com.graphiti.module.graphiti.dal.mysql.GraphMetadataMapper;
import com.graphiti.module.graphiti.dal.mysql.metadata.OntCommunityTypeMapper;
import com.graphiti.module.graphiti.dal.mysql.metadata.OntEntityCategoryMapper;
import com.graphiti.module.graphiti.dal.mysql.metadata.OntEpisodeTypeMapper;
import com.graphiti.module.graphiti.dal.mysql.metadata.OntRelationshipMetaMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntClassInheritanceMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntClassMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntConstraintMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntDefinitionMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntDraftMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntMappingMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntPropertyMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntVersionHistoryMapper;
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
    // 本体定义相关 Mapper
    private final OntDefinitionMapper ontDefinitionMapper;
    private final OntClassMapper ontClassMapper;
    private final OntPropertyMapper ontPropertyMapper;
    private final OntConstraintMapper ontConstraintMapper;
    private final OntMappingMapper ontMappingMapper;
    private final OntClassInheritanceMapper ontClassInheritanceMapper;
    private final OntVersionHistoryMapper ontVersionHistoryMapper;
    private final OntEntityCategoryMapper ontEntityCategoryMapper;
    private final OntEpisodeTypeMapper ontEpisodeTypeMapper;
    private final OntRelationshipMetaMapper ontRelationshipMetaMapper;
    private final OntCommunityTypeMapper ontCommunityTypeMapper;
    private final OntDraftMapper ontDraftMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GraphInfoRespVO createGraph(CreateGraphReqVO reqVO) {
        GraphMetadataDO entity = new GraphMetadataDO();
        entity.setGraphId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(reqVO.getName());
        entity.setDescription(reqVO.getDescription());
        entity.setNodeCount(0);
        entity.setEdgeCount(0);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setDeleted(false);
        graphMetadataMapper.insert(entity);
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
        // 4. 删除本体定义及所有子数据（先查 definitionId，因为子表按 definitionId 删除）
        deleteOntologyData(graphId);
        // 5. 删除本体草稿（直接按 graphId）
        ontDraftMapper.deleteByGraphId(graphId);
        // 6. 逻辑删除 MySQL 元数据
        LambdaUpdateWrapper<GraphMetadataDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(GraphMetadataDO::getId, entity.getId())
                .set(GraphMetadataDO::getDeleted, true)
                .set(GraphMetadataDO::getUpdateTime, LocalDateTime.now());
        graphMetadataMapper.update(null, updateWrapper);
        log.info("图谱已完整删除：graphId={}, name={}", graphId, entity.getName());
    }

    @Override
    public GraphDeletePreviewRespVO getGraphDeletePreview(String graphId) {
        GraphMetadataDO entity = getGraphMetadataByGraphId(graphId);
        // Neo4j 统计
        Map<String, Long> neo4jStats = graphNeo4jService.getGraphStats(graphId);
        long entityNodeCount = neo4jStats.getOrDefault("nodeCount", 0L);
        long episodeCount = neo4jStats.getOrDefault("episodeCount", 0L);
        long relationshipCount = neo4jStats.getOrDefault("edgeCount", 0L);
        long communityNodeCount = graphNeo4jService.countCommunitiesByGraphId(graphId);
        long neo4jDataCount = entityNodeCount + episodeCount + relationshipCount + communityNodeCount;

        // 本体定义统计
        Map<String, Long> ontStats = getOntologyStats(graphId);
        long ontDefinitionCount = ontStats.getOrDefault("definition", 0L);
        long ontClassCount = ontStats.getOrDefault("class", 0L);
        long ontPropertyCount = ontStats.getOrDefault("property", 0L);
        long ontConstraintCount = ontStats.getOrDefault("constraint", 0L);
        long ontMappingCount = ontStats.getOrDefault("mapping", 0L);
        long ontClassInheritanceCount = ontStats.getOrDefault("classInheritance", 0L);
        long ontVersionHistoryCount = ontStats.getOrDefault("versionHistory", 0L);
        long ontEntityCategoryCount = ontStats.getOrDefault("entityCategory", 0L);
        long ontEpisodeTypeCount = ontStats.getOrDefault("episodeType", 0L);
        long ontRelationshipMetaCount = ontStats.getOrDefault("relationshipMeta", 0L);
        long ontCommunityTypeCount = ontStats.getOrDefault("communityType", 0L);
        long ontDraftCount = ontDraftMapper.countByGraphId(graphId);

        long ontologyDataCount = ontDefinitionCount + ontClassCount + ontPropertyCount
            + ontConstraintCount + ontMappingCount + ontClassInheritanceCount
            + ontVersionHistoryCount + ontEntityCategoryCount + ontEpisodeTypeCount
            + ontRelationshipMetaCount + ontCommunityTypeCount + ontDraftCount;

        long totalDataCount = neo4jDataCount + ontologyDataCount;

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
        vo.setOntDefinitionCount(ontDefinitionCount);
        vo.setOntClassCount(ontClassCount);
        vo.setOntPropertyCount(ontPropertyCount);
        vo.setOntConstraintCount(ontConstraintCount);
        vo.setOntMappingCount(ontMappingCount);
        vo.setOntClassInheritanceCount(ontClassInheritanceCount);
        vo.setOntVersionHistoryCount(ontVersionHistoryCount);
        vo.setOntEntityCategoryCount(ontEntityCategoryCount);
        vo.setOntEpisodeTypeCount(ontEpisodeTypeCount);
        vo.setOntRelationshipMetaCount(ontRelationshipMetaCount);
        vo.setOntCommunityTypeCount(ontCommunityTypeCount);
        vo.setOntDraftCount(ontDraftCount);
        vo.setHasData(totalDataCount > 0);
        vo.setNeo4jDataCount(neo4jDataCount);
        vo.setOntologyDataCount(ontologyDataCount);
        vo.setTotalDataCount(totalDataCount);
        return vo;
    }

    @Override
    public void clearGraph(String graphId) {
        log.info("已清空图谱数据：graphId={}", graphId);
        GraphMetadataDO entity = getGraphMetadataByGraphId(graphId);
        entity.setNodeCount(0);
        entity.setEdgeCount(0);
        graphMetadataMapper.updateById(entity);
    }

    @Override
    public GraphStatsRespVO getGraphStats() {
        GraphStatsRespVO statsVO = new GraphStatsRespVO();
        LambdaQueryWrapper<GraphMetadataDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GraphMetadataDO::getDeleted, false);
        Long totalGraphs = graphMetadataMapper.selectCount(wrapper);
        statsVO.setTotalGraphs(totalGraphs);

        List<GraphMetadataDO> allGraphs = graphMetadataMapper.selectList(wrapper);
        long totalNodes = allGraphs.stream()
            .mapToLong(g -> g.getNodeCount() != null ? g.getNodeCount() : 0)
            .sum();
        long totalEdges = allGraphs.stream()
            .mapToLong(g -> g.getEdgeCount() != null ? g.getEdgeCount() : 0)
            .sum();
        statsVO.setTotalNodes(totalNodes);
        statsVO.setTotalEdges(totalEdges);

        long totalEpisodes = 0L;
        for (GraphMetadataDO graph : allGraphs) {
            try {
                totalEpisodes += graphNeo4jService.countEpisodesByGraphId(graph.getGraphId());
            } catch (Exception e) {
                log.warn("查询图谱 {} 的 Episode 数量失败: {}", graph.getGraphId(), e.getMessage());
            }
        }
        statsVO.setTotalEpisodes(totalEpisodes);
        statsVO.setNodeTrend(0);
        statsVO.setEdgeTrend(0);
        statsVO.setEpisodeTrend(0);
        return statsVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GraphInfoRespVO cloneGraph(String graphId) {
        GraphMetadataDO sourceEntity = getGraphMetadataByGraphId(graphId);
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
        graphNeo4jService.cloneGraphData(graphId, newGraphId);
        log.info("图谱克隆成功：source={}, target={}", graphId, newGraphId);
        return convertToGraphInfoRespVO(newEntity);
    }

    @Override
    public Map<String, Object> exportGraph(String graphId) {
        Map<String, Object> result = new HashMap<>();
        GraphMetadataDO entity = getGraphMetadataByGraphId(graphId);
        result.put("graphId", entity.getGraphId());
        result.put("name", entity.getName());
        result.put("description", entity.getDescription());
        result.put("nodeCount", entity.getNodeCount());
        result.put("edgeCount", entity.getEdgeCount());
        result.put("createdAt", entity.getCreateTime());
        result.put("nodes", graphNeo4jService.getNodesByGraphId(graphId));
        result.put("edges", graphNeo4jService.getEdgesByGraphId(graphId));
        result.put("episodes", graphNeo4jService.getEpisodesByGraphId(graphId, 1000, 0));
        return result;
    }

    // ==================== 私有方法 ====================
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

    /**
     * 获取本体定义的所有子表统计
     */
    private Map<String, Long> getOntologyStats(String graphId) {
        Map<String, Long> stats = new HashMap<>();
        OntDefinitionDO definition = ontDefinitionMapper.selectByGraphId(graphId);
        if (definition == null) {
            return stats;
        }
        Long definitionId = definition.getId();
        stats.put("definition", 1L);
        stats.put("class", ontClassMapper.countByDefinitionId(definitionId));
        stats.put("property", ontPropertyMapper.countByDefinitionId(definitionId));
        stats.put("constraint", ontConstraintMapper.countByDefinitionId(definitionId));
        stats.put("mapping", ontMappingMapper.countByDefinitionId(definitionId));
        stats.put("classInheritance", ontClassInheritanceMapper.countByDefinitionId(definitionId));
        stats.put("versionHistory", ontVersionHistoryMapper.countByDefinitionId(definitionId));
        stats.put("entityCategory", ontEntityCategoryMapper.countByDefinitionId(definitionId));
        stats.put("episodeType", ontEpisodeTypeMapper.countByDefinitionId(definitionId));
        stats.put("relationshipMeta", ontRelationshipMetaMapper.countByDefinitionId(definitionId));
        stats.put("communityType", ontCommunityTypeMapper.countByDefinitionId(definitionId));
        return stats;
    }

    /**
     * 删除本体定义及所有子表数据（按 definitionId）
     */
    private void deleteOntologyData(String graphId) {
        OntDefinitionDO definition = ontDefinitionMapper.selectByGraphId(graphId);
        if (definition == null) {
            return;
        }
        Long definitionId = definition.getId();
        // 按依赖顺序删除子表（先删叶子表，再删主表）
        ontClassInheritanceMapper.deleteByDefinitionId(definitionId);
        ontConstraintMapper.deleteByDefinitionId(definitionId);
        ontPropertyMapper.deleteByDefinitionId(definitionId);
        ontClassMapper.deleteByDefinitionId(definitionId);
        ontMappingMapper.deleteByDefinitionId(definitionId);
        ontVersionHistoryMapper.deleteByDefinitionId(definitionId);
        ontEntityCategoryMapper.deleteByDefinitionId(definitionId);
        ontEpisodeTypeMapper.deleteByDefinitionId(definitionId);
        ontRelationshipMetaMapper.deleteByDefinitionId(definitionId);
        ontCommunityTypeMapper.deleteByDefinitionId(definitionId);
        // 最后删除本体定义本身
        ontDefinitionMapper.deleteByGraphId(graphId);
        log.info("已删除本体定义及子表数据：graphId={}, definitionId={}", graphId, definitionId);
    }
}
