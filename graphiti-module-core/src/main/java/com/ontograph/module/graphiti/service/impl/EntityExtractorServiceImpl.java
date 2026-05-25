package com.graphiti.module.graphiti.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.module.graphiti.service.EntityExtractorService;
import com.graphiti.module.graphiti.service.LlmClientService;
import com.graphiti.module.graphiti.service.PromptTemplateService;
import com.graphiti.module.graphiti.vo.extractor.ExtractedEntityVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 实体提取服务实现
 * 参考 Graphiti 项目的 extract_nodes.py 和 node_operations.py 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntityExtractorServiceImpl implements EntityExtractorService {

    private final LlmClientService llmClientService;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ========== 默认实体类型配置 ==========

    @Override
    public String getDefaultEntityTypes() {
        return """
            1. Person - 人物：具体的人名，如"张三"、"李四"
            2. Organization - 组织机构：公司、政府部门、学校、医院等
            3. Location - 地点：城市、国家、具体地址等
            4. Event - 事件：会议、比赛、事故等具体事件
            5. Concept - 概念：思想、理论、原则等抽象概念
            6. Product - 产品：具体的产品名称
            7. Document - 文档：合同、报告、证书等
            8. Entity - 通用实体：不属于上述类型的实体（默认类型）
            """;
    }

    // ========== 文本提取 ==========

    @Override
    public List<ExtractedEntityVO> extractFromText(String content, String entityTypesConfig, String customInstructions) {
        if (content == null || content.isBlank()) {
            return new ArrayList<>();
        }

        String systemPrompt = buildSystemPrompt("text", entityTypesConfig, customInstructions);
        String userPrompt = buildTextUserPrompt(content, entityTypesConfig);

        String llmResponse = llmClientService.chat(systemPrompt, userPrompt);
        return parseEntitiesFromResponse(llmResponse);
    }

    // ========== JSON 提取 ==========

    @Override
    public List<ExtractedEntityVO> extractFromJson(String jsonContent, String sourceDescription,
                                                   String entityTypesConfig, String customInstructions) {
        if (jsonContent == null || jsonContent.isBlank()) {
            return new ArrayList<>();
        }

        String systemPrompt = buildSystemPrompt("json", entityTypesConfig, customInstructions);
        String userPrompt = buildJsonUserPrompt(jsonContent, sourceDescription, entityTypesConfig);

        String llmResponse = llmClientService.chat(systemPrompt, userPrompt);
        return parseEntitiesFromResponse(llmResponse);
    }

    // ========== 消息提取 ==========

    @Override
    public List<ExtractedEntityVO> extractFromMessage(String content, List<Map<String, Object>> previousEpisodes,
                                                      String entityTypesConfig, String customInstructions) {
        if (content == null || content.isBlank()) {
            return new ArrayList<>();
        }

        String systemPrompt = buildSystemPrompt("message", entityTypesConfig, customInstructions);
        String userPrompt = buildMessageUserPrompt(content, previousEpisodes, entityTypesConfig);

        String llmResponse = llmClientService.chat(systemPrompt, userPrompt);
        return parseEntitiesFromResponse(llmResponse);
    }

    // ========== 模板提取 ==========

    @Override
    public List<ExtractedEntityVO> extractWithTemplate(String content, String sourceType, String templateCode,
                                                       Map<String, Object> variables) {
        if (content == null || content.isBlank()) {
            return new ArrayList<>();
        }

        // 设置基础变量
        variables.put("episode_content", content);
        variables.put("reference_time", LocalDateTime.now().format(ISO_FORMATTER));

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
        return parseEntitiesFromResponse(llmResponse);
    }

    // ========== 提示词构建 ==========

    private String buildSystemPrompt(String sourceType, String entityTypesConfig, String customInstructions) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个实体提取专家。请从给定的文本中提取所有实体（人物、组织、地点、概念、事件等）。\n\n");
        prompt.append("提取规则：\n");
        prompt.append("1. 只提取文本中明确提及的实体\n");
        prompt.append("2. 不要提取抽象概念、日期时间或泛化的词汇\n");
        prompt.append("3. 实体名称要具体明确\n");
        prompt.append("4. 同一实体在一个消息中只提取一次\n");
        prompt.append("5. 排除代词（他、她、它等）和通用词\n\n");

        if (entityTypesConfig != null && !entityTypesConfig.isBlank()) {
            prompt.append("实体类型定义：\n").append(entityTypesConfig).append("\n\n");
        }

        if (customInstructions != null && !customInstructions.isBlank()) {
            prompt.append("额外指令：\n").append(customInstructions).append("\n\n");
        }

        prompt.append("请严格以如下 JSON 格式返回结果：\n");
        prompt.append("""
            {
              "entities": [
                {
                  "name": "实体名称",
                  "entity_type": "实体类型",
                  "summary": "对该实体的简短描述",
                  "episode_indices": [0]
                }
              ]
            }
            """);

        return prompt.toString();
    }

    private String buildTextUserPrompt(String content, String entityTypesConfig) {
        return "请从以下文本中提取实体：\n\n" + content;
    }

    private String buildJsonUserPrompt(String jsonContent, String sourceDescription, String entityTypesConfig) {
        StringBuilder prompt = new StringBuilder();
        if (sourceDescription != null && !sourceDescription.isBlank()) {
            prompt.append("数据源描述：").append(sourceDescription).append("\n\n");
        }
        prompt.append("JSON 数据：\n").append(jsonContent);
        return prompt.toString();
    }

    private String buildMessageUserPrompt(String content, List<Map<String, Object>> previousEpisodes,
                                          String entityTypesConfig) {
        StringBuilder prompt = new StringBuilder();

        if (previousEpisodes != null && !previousEpisodes.isEmpty()) {
            prompt.append("历史消息：\n");
            try {
                prompt.append(objectMapper.writeValueAsString(previousEpisodes));
            } catch (Exception e) {
                log.warn("序列化历史消息失败", e);
            }
            prompt.append("\n\n");
        }

        prompt.append("当前消息：\n").append(content);
        return prompt.toString();
    }

    // ========== 响应解析 ==========

    private List<ExtractedEntityVO> parseEntitiesFromResponse(String response) {
        List<ExtractedEntityVO> entities = new ArrayList<>();

        try {
            String jsonStr = extractJsonFromResponse(response);
            Map<String, Object> result = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> extractedList = extractEntitiesList(result);

            for (Map<String, Object> entity : extractedList) {
                ExtractedEntityVO vo = new ExtractedEntityVO();
                vo.setName((String) entity.get("name"));
                vo.setEntityType((String) entity.get("entity_type"));
                vo.setSummary((String) entity.getOrDefault("summary", ""));
                vo.setEpisodeIndices(extractEpisodeIndices(entity.get("episode_indices")));
                vo.setConfidence(extractConfidence(entity.get("confidence")));
                entities.add(vo);
            }

        } catch (Exception e) {
            log.warn("解析实体响应失败: {}", e.getMessage());
            // 尝试纯文本解析
            entities.addAll(parseEntitiesFromPlainText(response));
        }

        return entities;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractEntitiesList(Map<String, Object> result) {
        Object entitiesObj = result.get("entities");
        if (entitiesObj instanceof List) {
            return (List<Map<String, Object>>) entitiesObj;
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

    private List<ExtractedEntityVO> parseEntitiesFromPlainText(String text) {
        List<ExtractedEntityVO> entities = new ArrayList<>();
        String[] lines = text.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue;

            // 尝试解析 "名称 (类型)" 格式
            if (line.contains("(") && line.contains(")")) {
                int start = line.indexOf("(");
                int end = line.indexOf(")");
                String name = line.substring(0, start).trim();
                String type = line.substring(start + 1, end).trim();

                if (!name.isEmpty()) {
                    ExtractedEntityVO vo = new ExtractedEntityVO();
                    vo.setName(name);
                    vo.setEntityType(type);
                    vo.setEpisodeIndices(List.of(0));
                    entities.add(vo);
                }
            }
        }
        return entities;
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
