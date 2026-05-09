package com.graphiti.module.graphiti.service.impl;

import com.graphiti.common.exception.BusinessException;
import com.graphiti.module.graphiti.service.EpisodeService;
import com.graphiti.module.graphiti.service.GraphNeo4jService;
import com.graphiti.module.graphiti.vo.episode.EpisodeInfoRespVO;
import com.graphiti.module.graphiti.vo.episode.EpisodeListRespVO;
import com.graphiti.module.graphiti.vo.episode.EpisodeMentionsRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 事件管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeServiceImpl implements EpisodeService {
    
    private final GraphNeo4jService graphNeo4jService;
    
    @Override
    public EpisodeListRespVO listEpisodes(String graphId, int limit, int offset) {
        long total = graphNeo4jService.countEpisodesByGraphId(graphId);
        List<Map<String, Object>> rows = graphNeo4jService.getEpisodesByGraphId(graphId, limit, offset);
        List<EpisodeInfoRespVO> episodes = rows.stream()
                .map(this::convertToEpisodeInfo)
                .collect(Collectors.toList());
        EpisodeListRespVO respVO = new EpisodeListRespVO();
        respVO.setEpisodes(episodes);
        respVO.setTotalCount(total);
        respVO.setRowCount(episodes.size());
        return respVO;
    }
    
    @Override
    public EpisodeInfoRespVO getEpisodeDetail(String graphId, String episodeUuid) {
        Map<String, Object> episode = graphNeo4jService.getEpisodeByUuid(graphId, episodeUuid);
        if (episode == null) {
            throw new BusinessException(1010, "事件不存在");
        }
        return convertToEpisodeInfo(episode);
    }
    
    @Override
    public EpisodeMentionsRespVO getEpisodeMentions(String graphId, String episodeUuid) {
        Map<String, List<Map<String, Object>>> mentions = graphNeo4jService.getEpisodeMentions(episodeUuid);
        
        EpisodeMentionsRespVO respVO = new EpisodeMentionsRespVO();
        
        List<EpisodeMentionsRespVO.EpisodeNodeVO> nodes = mentions.get("nodes").stream()
                .map(this::convertToEpisodeNode)
                .collect(Collectors.toList());
        respVO.setNodes(nodes);
        
        List<EpisodeMentionsRespVO.EpisodeEdgeVO> edges = mentions.get("edges").stream()
                .map(this::convertToEpisodeEdge)
                .collect(Collectors.toList());
        respVO.setEdges(edges);
        
        return respVO;
    }
    
    @Override
    public EpisodeInfoRespVO createEpisode(String graphId, Map<String, Object> episodeData) {
        // 生成事件 UUID
        String uuid = UUID.randomUUID().toString().replace("-", "");
        
        // 提取事件属性
        String name = (String) episodeData.get("name");
        String source = (String) episodeData.get("source");
        String sourceDescription = (String) episodeData.get("sourceDescription");
        String content = (String) episodeData.get("content");
        Map<String, Object> properties = (Map<String, Object>) episodeData.getOrDefault("properties", new HashMap<>());
        
        if (content == null || content.isEmpty()) {
            throw new BusinessException(1011, "事件内容不能为空");
        }
        
        // 创建事件
        Map<String, Object> createdEpisode = graphNeo4jService.createEpisode(
            graphId, uuid, name != null ? name : "", source, sourceDescription, content, properties);
        
        if (createdEpisode == null) {
            throw new BusinessException(500, "创建事件失败");
        }
        
        return convertToEpisodeInfo(createdEpisode);
    }
    
    @Override
    public void deleteEpisode(String graphId, String episodeUuid) {
        graphNeo4jService.deleteEpisode(graphId, episodeUuid);
        log.info("删除事件：graphId={}, episodeUuid={}", graphId, episodeUuid);
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 转换为 EpisodeInfoRespVO
     */
    private EpisodeInfoRespVO convertToEpisodeInfo(Map<String, Object> row) {
        EpisodeInfoRespVO respVO = new EpisodeInfoRespVO();
        respVO.setUuid((String) row.get("uuid"));
        respVO.setName((String) row.get("name"));
        respVO.setGroupId((String) row.get("group_id"));
        respVO.setSource((String) row.get("source"));
        respVO.setSourceDescription((String) row.get("source_description"));
        respVO.setContent((String) row.get("content"));
        
        Object createdAt = row.get("created_at");
        if (createdAt != null) {
            respVO.setCreatedAt(createdAt.toString());
        }
        
        Object validAt = row.get("valid_at");
        if (validAt != null) {
            respVO.setValidAt(validAt.toString());
        }
        
        Object processed = row.get("processed");
        if (processed != null) {
            respVO.setProcessed((Boolean) processed);
        }
        
        return respVO;
    }
    
    /**
     * 转换为 EpisodeNodeVO
     */
    private EpisodeMentionsRespVO.EpisodeNodeVO convertToEpisodeNode(Map<String, Object> row) {
        EpisodeMentionsRespVO.EpisodeNodeVO vo = new EpisodeMentionsRespVO.EpisodeNodeVO();
        vo.setUuid((String) row.get("uuid"));
        vo.setName((String) row.get("name"));
        vo.setType((String) row.get("type"));
        vo.setSummary((String) row.get("summary"));
        return vo;
    }
    
    /**
     * 转换为 EpisodeEdgeVO
     */
    private EpisodeMentionsRespVO.EpisodeEdgeVO convertToEpisodeEdge(Map<String, Object> row) {
        EpisodeMentionsRespVO.EpisodeEdgeVO vo = new EpisodeMentionsRespVO.EpisodeEdgeVO();
        vo.setUuid((String) row.get("uuid"));
        vo.setSourceNodeUuid((String) row.get("source_node_uuid"));
        vo.setTargetNodeUuid((String) row.get("target_node_uuid"));
        vo.setType((String) row.get("type"));
        vo.setFact((String) row.get("fact"));
        return vo;
    }
}
