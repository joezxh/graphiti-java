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
    public EpisodeListRespVO listEpisodesByType(String graphId, String typeCode, int page, int pageSize) {
        long total = graphNeo4jService.countEpisodesByType(graphId, typeCode);
        int offset = (page - 1) * pageSize;
        List<Map<String, Object>> rows = graphNeo4jService.getEpisodesByType(graphId, typeCode, pageSize, offset);
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

        // V3.0.0 新增字段提取
        String episodeType = (String) episodeData.get("episode_type");
        String legalProcess = (String) episodeData.get("legal_process");
        String stageLabel = (String) episodeData.get("stage_label");
        String courtLevel = (String) episodeData.get("court_level");
        Boolean isTrialStage = episodeData.get("is_trial_stage") != null
            ? (Boolean) episodeData.get("is_trial_stage") : false;
        String startTime = (String) episodeData.get("start_time");
        String endTime = (String) episodeData.get("end_time");
        String caseId = (String) episodeData.get("case_id");

        // V3.1.0: 将 V3 字段注入 properties Map（GraphNeo4jService.createEpisode 使用 SET e += $props，会自动写入）
        Map<String, Object> v3Props = new HashMap<>(properties);
        v3Props.put("episode_type", episodeType);
        // 优先使用新字段，兼容旧字段作为 fallback
        v3Props.put("process_type", episodeData.getOrDefault("process_type",
            episodeData.getOrDefault("legal_process", "business_process")));
        v3Props.put("stage_label", stageLabel);
        v3Props.put("stage_level", episodeData.getOrDefault("stage_level",
            episodeData.get("court_level")));
        v3Props.put("is_review_stage", episodeData.getOrDefault("is_review_stage",
            episodeData.getOrDefault("is_trial_stage", false)));
        v3Props.put("start_time", startTime);
        v3Props.put("end_time", endTime);
        v3Props.put("case_id", caseId);

        // 创建事件
        Map<String, Object> createdEpisode = graphNeo4jService.createEpisode(
            graphId, uuid, name != null ? name : "", source, sourceDescription, content, v3Props);
        
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
        respVO.setGroupId((String) row.get("graph_id"));
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

        // V3.0.0 字段映射
        respVO.setEpisodeType((String) row.get("episode_type"));
        respVO.setLegalProcess((String) row.get("legal_process"));
        respVO.setStageLabel((String) row.get("stage_label"));
        respVO.setCourtLevel((String) row.get("court_level"));
        Object isTrialStage = row.get("is_trial_stage");
        if (isTrialStage != null) {
            respVO.setIsTrialStage((Boolean) isTrialStage);
        }
        Object startTime = row.get("start_time");
        if (startTime != null) {
            respVO.setStartTime(startTime.toString());
        }
        Object endTime = row.get("end_time");
        if (endTime != null) {
            respVO.setEndTime(endTime.toString());
        }
        respVO.setCaseId((String) row.get("case_id"));

        // V3.1.0 通用化字段映射
        respVO.setProcessType((String) row.get("process_type"));
        respVO.setStageLevel((String) row.get("stage_level"));
        Object isReviewStage = row.get("is_review_stage");
        if (isReviewStage != null) {
            respVO.setIsReviewStage((Boolean) isReviewStage);
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
