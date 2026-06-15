package com.ontograph.module.graphiti.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontograph.module.graphiti.service.DataExtractService;
import com.ontograph.module.graphiti.service.EdgeExtractorService;
import com.ontograph.module.graphiti.service.EntityExtractorService;
import com.ontograph.module.graphiti.vo.extractor.DataExtractReqVO;
import com.ontograph.module.graphiti.vo.extractor.DataExtractResultVO;
import com.ontograph.module.graphiti.vo.extractor.ExtractedEdgeVO;
import com.ontograph.module.graphiti.vo.extractor.ExtractedEntityVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据提取服务实现
 * 参考 Graphiti 的 EpisodeProcessor.java 和 tianque-ai 的 EntityRelationExtractor.java
 *
 * <p>完整流程：
 * <ol>
 *   <li>根据数据源类型选择合适的提取提示词</li>
 *   <li>调用 LLM 提取实体节点</li>
 *   <li>调用 LLM 提取关系边</li>
 *   <li>解析并返回结构化结果</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataExtractServiceImpl implements DataExtractService {

    private final EntityExtractorService entityExtractorService;
    private final EdgeExtractorService edgeExtractorService;
    private final ObjectMapper objectMapper;

    @Override
    public DataExtractResultVO extract(DataExtractReqVO reqVO) {
        long startTime = System.currentTimeMillis();
        List<String> errors = new ArrayList<>();

        DataExtractResultVO result = new DataExtractResultVO();
        result.setEntities(new ArrayList<>());
        result.setEdges(new ArrayList<>());

        try {
            // 如果是仅关系提取模式
            if (Boolean.TRUE.equals(reqVO.getEdgeOnly()) && reqVO.getExistingEntities() != null) {
                List<ExtractedEdgeVO> edges = extractEdgesInternal(reqVO);
                result.setEdges(edges);
                result.setEdgeCount(edges.size());
            }
            // 如果是仅实体提取模式
            else if (Boolean.TRUE.equals(reqVO.getEntityOnly())) {
                List<ExtractedEntityVO> entities = extractEntitiesInternal(reqVO);
                result.setEntities(entities);
                result.setEntityCount(entities.size());
            }
            // 完整提取（实体 + 关系）
            else {
                // 1. 提取实体
                List<ExtractedEntityVO> entities = extractEntitiesInternal(reqVO);
                result.setEntities(entities);
                result.setEntityCount(entities.size());

                // 2. 基于提取的实体提取关系
                if (!entities.isEmpty()) {
                    reqVO.setExistingEntities(entities);
                    List<ExtractedEdgeVO> edges = extractEdgesInternal(reqVO);
                    result.setEdges(edges);
                    result.setEdgeCount(edges.size());
                }
            }

            // 计算统计信息
            result.setEntityTypeStatistics(calculateEntityTypeStatistics(result.getEntities()));
            result.setEdgeTypeStatistics(calculateEdgeTypeStatistics(result.getEdges()));

        } catch (Exception e) {
            log.error("数据提取失败: graphId={}, error={}", reqVO.getGraphId(), e.getMessage(), e);
            errors.add("数据提取失败: " + e.getMessage());
        }

        result.setErrors(errors);
        result.setElapsedMs(System.currentTimeMillis() - startTime);

        log.info("数据提取完成: graphId={}, entities={}, edges={}, elapsed={}ms",
                reqVO.getGraphId(), result.getEntityCount(), result.getEdgeCount(), result.getElapsedMs());

        return result;
    }

    @Override
    public DataExtractResultVO extractEntities(DataExtractReqVO reqVO) {
        long startTime = System.currentTimeMillis();
        List<String> errors = new ArrayList<>();

        DataExtractResultVO result = new DataExtractResultVO();
        result.setEntities(new ArrayList<>());
        result.setEdges(new ArrayList<>());

        try {
            List<ExtractedEntityVO> entities = extractEntitiesInternal(reqVO);
            result.setEntities(entities);
            result.setEntityCount(entities.size());
            result.setEntityTypeStatistics(calculateEntityTypeStatistics(entities));
        } catch (Exception e) {
            log.error("实体提取失败: graphId={}, error={}", reqVO.getGraphId(), e.getMessage(), e);
            errors.add("实体提取失败: " + e.getMessage());
        }

        result.setErrors(errors);
        result.setElapsedMs(System.currentTimeMillis() - startTime);
        return result;
    }

    @Override
    public DataExtractResultVO extractEdges(DataExtractReqVO reqVO) {
        long startTime = System.currentTimeMillis();
        List<String> errors = new ArrayList<>();

        DataExtractResultVO result = new DataExtractResultVO();
        result.setEntities(reqVO.getExistingEntities() != null ? reqVO.getExistingEntities() : new ArrayList<>());
        result.setEdges(new ArrayList<>());

        try {
            if (reqVO.getExistingEntities() == null || reqVO.getExistingEntities().isEmpty()) {
                throw new IllegalArgumentException("提取关系需要先提供实体列表");
            }

            List<ExtractedEdgeVO> edges = extractEdgesInternal(reqVO);
            result.setEdges(edges);
            result.setEdgeCount(edges.size());
            result.setEdgeTypeStatistics(calculateEdgeTypeStatistics(edges));
        } catch (Exception e) {
            log.error("关系提取失败: graphId={}, error={}", reqVO.getGraphId(), e.getMessage(), e);
            errors.add("关系提取失败: " + e.getMessage());
        }

        result.setErrors(errors);
        result.setElapsedMs(System.currentTimeMillis() - startTime);
        return result;
    }

    // ========== 内部方法 ==========

    private List<ExtractedEntityVO> extractEntitiesInternal(DataExtractReqVO reqVO) {
        String content = reqVO.getContent();
        String sourceType = reqVO.getSourceType() != null ? reqVO.getSourceType() : "text";
        String entityTypesConfig = getEffectiveEntityTypesConfig(reqVO);
        String customInstructions = getEffectiveCustomInstructions(reqVO);
        List<Map<String, Object>> previousEpisodes = reqVO.getPreviousEpisodes() != null ?
                convertEpisodesToMap(reqVO.getPreviousEpisodes()) : new ArrayList<>();

        // 根据数据源类型选择不同的提取方法
        return switch (sourceType) {
            case "message" -> entityExtractorService.extractFromMessage(content, previousEpisodes,
                    entityTypesConfig, customInstructions);
            case "json" -> entityExtractorService.extractFromJson(content, reqVO.getSourceDescription(),
                    entityTypesConfig, customInstructions);
            default -> entityExtractorService.extractFromText(content, entityTypesConfig, customInstructions);
        };
    }

    private List<ExtractedEdgeVO> extractEdgesInternal(DataExtractReqVO reqVO) {
        String content = reqVO.getContent();
        String sourceType = reqVO.getSourceType() != null ? reqVO.getSourceType() : "text";
        List<ExtractedEntityVO> entities = reqVO.getExistingEntities();
        String edgeTypesConfig = getEffectiveEdgeTypesConfig(reqVO);
        String customInstructions = getEffectiveCustomInstructions(reqVO);
        LocalDateTime referenceTime = reqVO.getReferenceTime() != null ? reqVO.getReferenceTime() : LocalDateTime.now();
        List<Map<String, Object>> previousEpisodes = reqVO.getPreviousEpisodes() != null ?
                convertEpisodesToMap(reqVO.getPreviousEpisodes()) : new ArrayList<>();

        // 根据数据源类型选择不同的提取方法
        return switch (sourceType) {
            case "message" -> edgeExtractorService.extractFromMessage(content, entities, previousEpisodes,
                    referenceTime, edgeTypesConfig, customInstructions);
            default -> edgeExtractorService.extractFromText(content, entities, referenceTime,
                    edgeTypesConfig, customInstructions);
        };
    }

    private String getEffectiveEntityTypesConfig(DataExtractReqVO reqVO) {
        if (reqVO.getEntityTypesConfig() != null && !reqVO.getEntityTypesConfig().isBlank()) {
            return reqVO.getEntityTypesConfig();
        }
        return entityExtractorService.getDefaultEntityTypes();
    }

    private String getEffectiveEdgeTypesConfig(DataExtractReqVO reqVO) {
        if (reqVO.getEdgeTypesConfig() != null && !reqVO.getEdgeTypesConfig().isBlank()) {
            return reqVO.getEdgeTypesConfig();
        }
        return edgeExtractorService.getDefaultEdgeTypes();
    }

    private String getEffectiveCustomInstructions(DataExtractReqVO reqVO) {
        StringBuilder instructions = new StringBuilder();

        if (reqVO.getCustomInstructions() != null && !reqVO.getCustomInstructions().isBlank()) {
            instructions.append(reqVO.getCustomInstructions());
        }

        // 添加上下文 episodes 的说明
        if (reqVO.getPreviousEpisodes() != null && !reqVO.getPreviousEpisodes().isEmpty()) {
            if (instructions.length() > 0) {
                instructions.append("\n\n");
            }
            instructions.append("注意：PREVIOUS_MESSAGES 仅用于上下文参考，不要从中提取新的实体。");
        }

        return instructions.toString();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> convertEpisodesToMap(List<DataExtractReqVO.EpisodeContext> episodes) {
        return episodes.stream()
                .map(ep -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("content", ep.getContent());
                    map.put("timestamp", ep.getTimestamp() != null ? ep.getTimestamp().toString() : null);
                    map.put("source_type", ep.getSourceType() != null ? ep.getSourceType() : "text");
                    return map;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Integer> calculateEntityTypeStatistics(List<ExtractedEntityVO> entities) {
        if (entities == null || entities.isEmpty()) {
            return new LinkedHashMap<>();
        }

        return entities.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getEntityType() != null ? e.getEntityType() : "Unknown",
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
    }

    private Map<String, Integer> calculateEdgeTypeStatistics(List<ExtractedEdgeVO> edges) {
        if (edges == null || edges.isEmpty()) {
            return new LinkedHashMap<>();
        }

        return edges.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getRelationType() != null ? e.getRelationType() : "UNKNOWN",
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
    }
}
