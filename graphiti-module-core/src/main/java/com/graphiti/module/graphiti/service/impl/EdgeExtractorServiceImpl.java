package com.graphiti.module.graphiti.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.module.graphiti.service.EdgeExtractorService;
import com.graphiti.module.graphiti.service.LlmClientService;
import com.graphiti.module.graphiti.service.PromptTemplateService;
import com.graphiti.module.graphiti.vo.extractor.ExtractedEdgeVO;
import com.graphiti.module.graphiti.vo.extractor.ExtractedEntityVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 关系提取服务实现
 * 参考 Graphiti 项目的 extract_edges.py 和 edge_operations.py 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EdgeExtractorServiceImpl implements EdgeExtractorService {

    private final LlmClientService llmClientService;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ========== 默认关系类型配置 ==========

    @Override
    public String getDefaultEdgeTypes() {
        return """
            常见关系类型（SCREAMING_SNAKE_CASE 格式）：
            - WORKS_AT: 工作于
            - LIVES_IN: 居住在
            - KNOWS: 认识
            - PARTICIPATES_IN: 参与
            - OWNS: 拥有
            - LOCATED_IN: 位于
            - RELATED_TO: 相关于
            - CREATED_BY: 由...创建
            - BELONGS_TO: 属于
            - MARRIED_TO: 与...结婚
            - PARENT_OF: 是...的父母
            - SIBLING_OF: 与...是兄弟姐妹
            - FRIEND_OF: 与...是朋友
            """;
    }

    // ========== 文本提取 ==========

    @Override
    public List<ExtractedEdgeVO> extractFromText(String content, List<ExtractedEntityVO> entities,
                                                 LocalDateTime referenceTime, String edgeTypesConfig,
                                                 String customInstructions) {
        if (content == null || content.isBlank() || entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }

        String systemPrompt = buildSystemPrompt(edgeTypesConfig, customInstructions);
        String userPrompt = buildTextUserPrompt(content, entities, referenceTime, edgeTypesConfig);

        String llmResponse = llmClientService.chat(systemPrompt, userPrompt);
        return parseEdgesFromResponse(llmResponse, entities);
    }

    // ========== 消息提取 ==========

    @Override
    public List<ExtractedEdgeVO> extractFromMessage(String content, List<ExtractedEntityVO> entities,
                                                    List<Map<String, Object>> previousEpisodes,
                                                    LocalDateTime referenceTime, String edgeTypesConfig,
                                                    String customInstructions) {
        if (content == null || content.isBlank() || entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }

        String systemPrompt = buildSystemPrompt(edgeTypesConfig, customInstructions);
        String userPrompt = buildMessageUserPrompt(content, entities, previousEpisodes, referenceTime, edgeTypesConfig);

        String llmResponse = llmClientService.chat(systemPrompt, userPrompt);
        return parseEdgesFromResponse(llmResponse, entities);
    }

    // ========== 模板提取 ==========

    @Override
    public List<ExtractedEdgeVO> extractWithTemplate(String content, List<ExtractedEntityVO> entities,
                                                    String sourceType, String templateCode,
                                                    Map<String, Object> variables) {
        if (content == null || content.isBlank() || entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }

        // 设置基础变量
        variables.put("episode_content", content);
        variables.put("reference_time", LocalDateTime.now().format(ISO_FORMATTER));
        variables.put("nodes", serializeEntities(entities));

        // 根据数据源类型选择不同的渲染模板
        String promptTemplateCode = switch (sourceType) {
            case "message" -> templateCode + "_message";
            case "json" -> templateCode + "_json";
            default -> templateCode + "_text";
        };

        // 尝试渲染，如果没有特定类型的模板则使用默认
        PromptTemplateService.RenderedPrompt rendered;
        try {
            rendered = promptTemplateService.renderPrompt(promptTemplateCode, variables);
        } catch (Exception e) {
            // 回退到默认模板
            rendered = promptTemplateService.renderPrompt(templateCode, variables);
        }

        String llmResponse = llmClientService.chat(rendered.systemPrompt(), rendered.userPrompt());
        return parseEdgesFromResponse(llmResponse, entities);
    }

    // ========== 提示词构建 ==========

    private String buildSystemPrompt(String edgeTypesConfig, String customInstructions) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个关系提取专家。给定一段文本和已识别的实体列表，请提取实体之间的关系。\n\n");
        prompt.append("提取规则：\n");
        prompt.append("1. source_entity_name 和 target_entity_name 必须使用 ENTITIES 列表中的实体名称\n");
        prompt.append("2. 每个关系必须涉及两个不同的实体\n");
        prompt.append("3. 关系类型使用 SCREAMING_SNAKE_CASE 格式\n");
        prompt.append("4. fact 应该是描述关系的自然语言陈述\n");
        prompt.append("5. 只提取文本中明确表达的关系\n");
        prompt.append("6. 不要虚构或推断关系\n\n");

        if (edgeTypesConfig != null && !edgeTypesConfig.isBlank()) {
            prompt.append("关系类型定义：\n").append(edgeTypesConfig).append("\n\n");
        }

        if (customInstructions != null && !customInstructions.isBlank()) {
            prompt.append("额外指令：\n").append(customInstructions).append("\n\n");
        }

        prompt.append("请严格以如下 JSON 格式返回结果：\n");
        prompt.append("""
            {
              "edges": [
                {
                  "source_entity_name": "源实体名称",
                  "target_entity_name": "目标实体名称",
                  "relation_type": "关系类型",
                  "fact": "描述该关系的事实陈述",
                  "valid_at": "生效时间（ISO 8601格式，可为空）",
                  "invalid_at": "失效时间（ISO 8601格式，可为空）",
                  "episode_indices": [0]
                }
              ]
            }
            """);

        return prompt.toString();
    }

    private String buildTextUserPrompt(String content, List<ExtractedEntityVO> entities,
                                       LocalDateTime referenceTime, String edgeTypesConfig) {
        StringBuilder prompt = new StringBuilder();

        if (referenceTime != null) {
            prompt.append("参考时间：").append(referenceTime.format(ISO_FORMATTER)).append("\n\n");
        }

        prompt.append("实体列表：\n");
        for (ExtractedEntityVO entity : entities) {
            prompt.append("- ").append(entity.getName()).append(" (").append(entity.getEntityType()).append(")\n");
        }

        prompt.append("\n文本内容：\n").append(content);
        return prompt.toString();
    }

    private String buildMessageUserPrompt(String content, List<ExtractedEntityVO> entities,
                                         List<Map<String, Object>> previousEpisodes,
                                         LocalDateTime referenceTime, String edgeTypesConfig) {
        StringBuilder prompt = new StringBuilder();

        if (referenceTime != null) {
            prompt.append("参考时间：").append(referenceTime.format(ISO_FORMATTER)).append("\n\n");
        }

        if (previousEpisodes != null && !previousEpisodes.isEmpty()) {
            prompt.append("历史消息：\n");
            try {
                prompt.append(objectMapper.writeValueAsString(previousEpisodes));
            } catch (Exception e) {
                log.warn("序列化历史消息失败", e);
            }
            prompt.append("\n\n");
        }

        prompt.append("实体列表：\n");
        for (ExtractedEntityVO entity : entities) {
            prompt.append("- ").append(entity.getName()).append(" (").append(entity.getEntityType()).append(")\n");
        }

        prompt.append("\n当前消息：\n").append(content);
        return prompt.toString();
    }

    private String serializeEntities(List<ExtractedEntityVO> entities) {
        try {
            List<Map<String, String>> entityList = entities.stream()
                    .map(e -> {
                        Map<String, String> map = new LinkedHashMap<>();
                        map.put("name", e.getName());
                        map.put("entity_type", e.getEntityType());
                        return map;
                    })
                    .collect(Collectors.toList());
            return objectMapper.writeValueAsString(entityList);
        } catch (Exception e) {
            log.warn("序列化实体列表失败", e);
            return "[]";
        }
    }

    // ========== 响应解析 ==========

    private List<ExtractedEdgeVO> parseEdgesFromResponse(String response, List<ExtractedEntityVO> entities) {
        List<ExtractedEdgeVO> edges = new ArrayList<>();

        // 构建实体名称到实体对象的映射（不区分大小写）
        Map<String, ExtractedEntityVO> nameToEntity = new HashMap<>();
        for (ExtractedEntityVO entity : entities) {
            nameToEntity.put(entity.getName().toLowerCase(), entity);
        }

        try {
            String jsonStr = extractJsonFromResponse(response);
            Map<String, Object> result = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> extractedList = extractEdgesList(result);

            for (Map<String, Object> edge : extractedList) {
                String sourceName = ((String) edge.get("source_entity_name")).toLowerCase();
                String targetName = ((String) edge.get("target_entity_name")).toLowerCase();

                // 验证实体名称
                if (!nameToEntity.containsKey(sourceName) || !nameToEntity.containsKey(targetName)) {
                    log.debug("跳过关系：实体名称不在列表中 source={} target={}", sourceName, targetName);
                    continue;
                }

                // 跳过自环
                if (sourceName.equals(targetName)) {
                    log.debug("跳过自环关系：{}", sourceName);
                    continue;
                }

                ExtractedEdgeVO vo = new ExtractedEdgeVO();
                vo.setSourceEntityName(nameToEntity.get(sourceName).getName());
                vo.setTargetEntityName(nameToEntity.get(targetName).getName());
                vo.setRelationType((String) edge.get("relation_type"));
                vo.setFact((String) edge.getOrDefault("fact", ""));
                vo.setEpisodeIndices(extractEpisodeIndices(edge.get("episode_indices")));
                vo.setConfidence(extractConfidence(edge.get("confidence")));
                vo.setAttributes(extractAttributes(edge));

                // 解析时间
                vo.setValidAt(parseDateTime(edge.get("valid_at")));
                vo.setInvalidAt(parseDateTime(edge.get("invalid_at")));

                edges.add(vo);
            }

        } catch (Exception e) {
            log.warn("解析关系响应失败: {}", e.getMessage());
        }

        return edges;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractEdgesList(Map<String, Object> result) {
        Object edgesObj = result.get("edges");
        if (edgesObj instanceof List) {
            return (List<Map<String, Object>>) edgesObj;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private List<Integer> extractEpisodeIndices(Object indicesObj) {
        if (indicesObj instanceof List) {
            List<?> list = (List<?>) indicesObj;
            List<Integer> indices = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Number) {
                    indices.add(((Number) item).intValue());
                }
            }
            return indices;
        }
        return List.of(0);
    }

    private Double extractConfidence(Object confidenceObj) {
        if (confidenceObj instanceof Number) {
            return ((Number) confidenceObj).doubleValue();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractAttributes(Map<String, Object> edge) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : edge.entrySet()) {
            String key = entry.getKey();
            if (!List.of("source_entity_name", "target_entity_name", "relation_type",
                    "fact", "valid_at", "invalid_at", "episode_indices", "confidence").contains(key)) {
                attributes.put(key, entry.getValue());
            }
        }
        return attributes;
    }

    private LocalDateTime parseDateTime(Object dateTimeObj) {
        if (dateTimeObj == null) {
            return null;
        }
        try {
            String dateTimeStr = dateTimeObj.toString();
            return LocalDateTime.parse(dateTimeStr, ISO_FORMATTER);
        } catch (Exception e) {
            log.debug("解析日期时间失败: {}", dateTimeObj);
            return null;
        }
    }

    private String extractJsonFromResponse(String response) {
        // 尝试找到 JSON 代码块
        int jsonStart = response.indexOf("```json");
        if (jsonStart != -1) {
            int start = jsonStart + 7;
            int end = response.lastIndexOf("```");
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }

        // 尝试找到普通 JSON 对象
        int braceStart = response.indexOf("{");
        int braceEnd = response.lastIndexOf("}");
        if (braceStart != -1 && braceEnd != -1 && braceEnd > braceStart) {
            return response.substring(braceStart, braceEnd + 1);
        }

        return "{}";
    }
}
