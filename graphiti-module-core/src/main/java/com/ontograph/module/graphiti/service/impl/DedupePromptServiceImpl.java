package com.graphiti.module.graphiti.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.module.graphiti.service.DedupePromptService;
import com.graphiti.module.graphiti.service.LlmClientService;
import com.graphiti.module.graphiti.service.PromptTemplateService;
import com.graphiti.module.graphiti.vo.dedup.EdgeDedupeResultVO;
import com.graphiti.module.graphiti.vo.dedup.NodeDedupeResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 去重提示词服务实现
 *
 * <p>实现基于 LLM 的实体去重和边去重功能，从数据库中的提示词模板获取提示词。
 *
 * <p>严格遵循 Python 原版逻辑：
 * <ul>
 *   <li>dedupe_nodes.py - 节点去重（node, nodes, node_list 函数）</li>
 *   <li>dedupe_edges.py - 边去重（resolve_edge 函数）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DedupePromptServiceImpl implements DedupePromptService {

    private final PromptTemplateService promptTemplateService;
    private final LlmClientService llmClientService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String DEDUPE_NODES_SINGLE_CODE = "DEDUPE_NODES_SINGLE";
    private static final String DEDUPE_NODES_BATCH_CODE = "DEDUPE_NODES_BATCH";
    private static final String DEDUPE_NODES_GROUP_CODE = "DEDUPE_NODES_GROUP";
    private static final String DEDUPE_EDGES_CODE = "DEDUPE_EDGES";

    // ========== 节点去重（单实体 vs 现有实体）==========

    @Override
    public NodeDedupeResultVO.NodeDuplicate deduplicateSingleNode(Map<String, Object> context) {
        log.debug("执行单实体去重");

        // 准备变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("previous_episodes", toJson(context.get("previousEpisodes")));
        variables.put("episode_content", context.getOrDefault("episodeContent", ""));
        variables.put("extracted_node", toJson(context.get("extractedNode")));
        variables.put("entity_type_description", context.getOrDefault("entityTypeDescription", "通用实体类型"));
        variables.put("existing_nodes", toJson(context.get("existingNodes")));

        // 渲染提示词
        PromptTemplateService.RenderedPrompt rendered;
        try {
            rendered = promptTemplateService.renderPrompt(DEDUPE_NODES_SINGLE_CODE, variables);
        } catch (Exception e) {
            log.error("获取单实体去重提示词失败: {}", e.getMessage());
            throw new RuntimeException("获取去重提示词失败", e);
        }

        // 调用 LLM
        String response = llmClientService.chat(rendered.systemPrompt(), rendered.userPrompt());

        // 解析响应
        return parseSingleNodeResponse(response);
    }

    // ========== 节点去重（批量实体 vs 现有实体）==========

    @Override
    public NodeDedupeResultVO deduplicateNodes(Map<String, Object> context) {
        log.debug("执行批量实体去重");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> extractedNodes = (List<Map<String, Object>>) context.get("extractedNodes");
        int entityCount = extractedNodes != null ? extractedNodes.size() : 0;

        // 准备变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("previous_episodes", toJson(context.get("previousEpisodes")));
        variables.put("episode_content", context.getOrDefault("episodeContent", ""));
        variables.put("extracted_nodes", toJson(extractedNodes));
        variables.put("existing_nodes", toJson(context.get("existingNodes")));
        variables.put("entity_count", String.valueOf(entityCount));
        variables.put("entity_count_minus_1", String.valueOf(entityCount - 1));

        // 渲染提示词
        PromptTemplateService.RenderedPrompt rendered;
        try {
            rendered = promptTemplateService.renderPrompt(DEDUPE_NODES_BATCH_CODE, variables);
        } catch (Exception e) {
            log.error("获取批量实体去重提示词失败: {}", e.getMessage());
            throw new RuntimeException("获取去重提示词失败", e);
        }

        // 调用 LLM
        String response = llmClientService.chat(rendered.systemPrompt(), rendered.userPrompt());

        // 解析响应
        return parseNodesResponse(response);
    }

    // ========== 节点分组去重 ==========

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> groupDuplicateNodes(List<Map<String, Object>> nodes) {
        log.debug("执行节点分组去重: {} 个节点", nodes.size());

        // 准备变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("nodes", toJson(nodes));

        // 渲染提示词
        PromptTemplateService.RenderedPrompt rendered;
        try {
            rendered = promptTemplateService.renderPrompt(DEDUPE_NODES_GROUP_CODE, variables);
        } catch (Exception e) {
            log.error("获取节点分组去重提示词失败: {}", e.getMessage());
            throw new RuntimeException("获取去重提示词失败", e);
        }

        // 调用 LLM
        String response = llmClientService.chat(rendered.systemPrompt(), rendered.userPrompt());

        // 解析响应 - 返回分组列表
        try {
            return objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("解析节点分组响应失败: {}", e.getMessage());
            throw new RuntimeException("解析分组结果失败", e);
        }
    }

    // ========== 边去重 ==========

    @Override
    public EdgeDedupeResultVO deduplicateEdge(Map<String, Object> context) {
        log.debug("执行边去重");

        // 准备变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("existing_edges", toJson(context.get("existingEdges")));
        variables.put("edge_invalidation_candidates", toJson(context.get("edgeInvalidationCandidates")));
        variables.put("new_edge", toJson(context.get("newEdge")));

        // 渲染提示词
        PromptTemplateService.RenderedPrompt rendered;
        try {
            rendered = promptTemplateService.renderPrompt(DEDUPE_EDGES_CODE, variables);
        } catch (Exception e) {
            log.error("获取边去重提示词失败: {}", e.getMessage());
            throw new RuntimeException("获取去重提示词失败", e);
        }

        // 调用 LLM
        String response = llmClientService.chat(rendered.systemPrompt(), rendered.userPrompt());

        // 解析响应
        return parseEdgeResponse(response);
    }

    // ========== 辅助方法 ==========

    @Override
    public List<Map<String, Object>> convertToNodeCandidates(List<Map<String, Object>> nodes) {
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            Map<String, Object> node = nodes.get(i);
            Map<String, Object> candidate = new LinkedHashMap<>();
            candidate.put("candidate_id", i);
            candidate.put("name", node.getOrDefault("name", ""));
            candidate.put("entity_type", node.getOrDefault("entity_type", node.getOrDefault("type", "")));
            candidate.put("summary", node.getOrDefault("summary", ""));
            candidates.add(candidate);
        }
        return candidates;
    }

    @Override
    public List<Map<String, Object>> convertToEdgeFacts(List<Map<String, Object>> edges) {
        List<Map<String, Object>> facts = new ArrayList<>();
        for (Map<String, Object> edge : edges) {
            Map<String, Object> fact = new LinkedHashMap<>();
            fact.put("source_entity", edge.getOrDefault("source_entity", edge.getOrDefault("source", "")));
            fact.put("target_entity", edge.getOrDefault("target_entity", edge.getOrDefault("target", "")));
            fact.put("relation_type", edge.getOrDefault("relation_type", edge.getOrDefault("type", "")));
            fact.put("fact", edge.getOrDefault("fact", ""));
            fact.put("valid_at", edge.getOrDefault("valid_at", edge.getOrDefault("validAt", "")));
            fact.put("invalid_at", edge.getOrDefault("invalid_at", edge.getOrDefault("invalidAt", "")));
            facts.add(fact);
        }
        return facts;
    }

    // ========== 私有解析方法 ==========

    private NodeDedupeResultVO.NodeDuplicate parseSingleNodeResponse(String response) {
        try {
            // 尝试解析为单对象
            Map<String, Object> result = parseJsonResponse(response);
            NodeDedupeResultVO.NodeDuplicate duplicate = new NodeDedupeResultVO.NodeDuplicate();
            duplicate.setId(getIntValue(result.get("id"), 0));
            duplicate.setName((String) result.getOrDefault("name", ""));
            duplicate.setDuplicateCandidateId(getIntValue(result.get("duplicate_candidate_id"), -1));
            return duplicate;
        } catch (Exception e) {
            log.error("解析单实体去重响应失败: {}", e.getMessage());
            throw new RuntimeException("解析去重结果失败", e);
        }
    }

    private NodeDedupeResultVO parseNodesResponse(String response) {
        try {
            Map<String, Object> result = parseJsonResponse(response);
            NodeDedupeResultVO vo = new NodeDedupeResultVO();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> resolutions = (List<Map<String, Object>>) result.get("entity_resolutions");
            if (resolutions == null) {
                vo.setEntityResolutions(Collections.emptyList());
                return vo;
            }

            List<NodeDedupeResultVO.NodeDuplicate> duplicates = new ArrayList<>();
            for (Map<String, Object> r : resolutions) {
                NodeDedupeResultVO.NodeDuplicate duplicate = new NodeDedupeResultVO.NodeDuplicate();
                duplicate.setId(getIntValue(r.get("id"), 0));
                duplicate.setName((String) r.getOrDefault("name", ""));
                duplicate.setDuplicateCandidateId(getIntValue(r.get("duplicate_candidate_id"), -1));
                duplicates.add(duplicate);
            }
            vo.setEntityResolutions(duplicates);
            return vo;
        } catch (Exception e) {
            log.error("解析批量实体去重响应失败: {}", e.getMessage());
            throw new RuntimeException("解析去重结果失败", e);
        }
    }

    private EdgeDedupeResultVO parseEdgeResponse(String response) {
        try {
            Map<String, Object> result = parseJsonResponse(response);
            EdgeDedupeResultVO vo = new EdgeDedupeResultVO();

            vo.setDuplicateFacts(convertToIntList(result.get("duplicate_facts")));
            vo.setContradictedFacts(convertToIntList(result.get("contradicted_facts")));

            return vo;
        } catch (Exception e) {
            log.error("解析边去重响应失败: {}", e.getMessage());
            throw new RuntimeException("解析去重结果失败", e);
        }
    }

    private Map<String, Object> parseJsonResponse(String response) {
        try {
            // 尝试提取 JSON
            String jsonStr = extractJson(response);
            return objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("JSON 解析失败，尝试直接解析: {}", response);
            throw new RuntimeException("无效的 JSON 响应", e);
        }
    }

    private String extractJson(String text) {
        // 尝试找到 JSON 代码块
        int jsonStart = text.indexOf("```json");
        if (jsonStart != -1) {
            int start = jsonStart + 7;
            int end = text.lastIndexOf("```");
            if (end > start) {
                return text.substring(start, end).trim();
            }
        }

        // 尝试找到普通 JSON 对象
        int braceStart = text.indexOf("{");
        int braceEnd = text.lastIndexOf("}");
        if (braceStart != -1 && braceEnd != -1 && braceEnd > braceStart) {
            return text.substring(braceStart, braceEnd + 1);
        }

        // 尝试找到普通 JSON 数组
        int bracketStart = text.indexOf("[");
        int bracketEnd = text.lastIndexOf("]");
        if (bracketStart != -1 && bracketEnd != -1 && bracketEnd > bracketStart) {
            return text.substring(bracketStart, bracketEnd + 1);
        }

        return "{}";
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("序列化对象失败: {}", e.getMessage());
            return "[]";
        }
    }

    private Integer getIntValue(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Integer> convertToIntList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.stream()
                    .map(item -> getIntValue(item, 0))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
