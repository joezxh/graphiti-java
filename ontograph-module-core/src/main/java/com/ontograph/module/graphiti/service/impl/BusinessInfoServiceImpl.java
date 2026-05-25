package com.ontograph.module.graphiti.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontograph.common.exception.BusinessException;
import com.ontograph.module.graphiti.dal.dataobject.ont.OntDraftDO;
import com.ontograph.module.graphiti.dal.mysql.ont.OntDraftMapper;
import com.ontograph.module.graphiti.service.BusinessInfoService;
import com.ontograph.module.graphiti.service.LlmClientService;
import com.ontograph.module.graphiti.vo.business.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessInfoServiceImpl implements BusinessInfoService {

    private final LlmClientService llmClientService;
    private final OntDraftMapper draftMapper;
    private final ObjectMapper objectMapper;

    private static final String PROMPT_PATH_GENERATE = "prompts/business_info/generate_ontology.txt";
    private static final String PROMPT_PATH_OPTIMIZE = "prompts/business_info/optimize_desc.txt";
    private static final String PROMPT_PATH_DATA = "prompts/business_info/generate_data.txt";

    @Override
    public GenerateOntologyRespVO generateOntology(String graphId, GenerateOntologyReqVO reqVO) {
        try {
            String prompt = buildPrompt(PROMPT_PATH_GENERATE, Map.of(
                "businessScenario", reqVO.getBusinessScenario() != null ? reqVO.getBusinessScenario() : "",
                "domainHint", reqVO.getDomainHint() != null ? reqVO.getDomainHint() : "GENERAL",
                "userInput", reqVO.getUserInput() != null ? reqVO.getUserInput() : ""
            ));

            GenerateOntologyRespVO resp = llmClientService.chat(prompt, GenerateOntologyRespVO.class);
            resp.setGeneratedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            resp.setStatus("GENERATED");

            if (reqVO.isSaveAsDraft()) {
                Long draftId = saveDraft(graphId, reqVO.getDraftName(), "GENERATED",
                    objectMapper.writeValueAsString(reqVO), objectMapper.writeValueAsString(resp));
                resp.setDraftId(draftId);
            }

            return resp;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成本体定义失败: graphId={}", graphId, e);
            throw new BusinessException(2002, "生成本体定义失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void saveAsDraft(String graphId, GenerateOntologyReqVO reqVO) {
        try {
            String sourceJson = objectMapper.writeValueAsString(reqVO);
            saveDraft(graphId, reqVO.getDraftName(), "DRAFT", sourceJson, null);
        } catch (Exception e) {
            log.error("保存草稿失败: graphId={}", graphId, e);
            throw new BusinessException(2005, "保存草稿失败: " + e.getMessage());
        }
    }

    private Long saveDraft(String graphId, String draftName, String draftType,
                           String sourceInfo, String generatedInfo) {
        OntDraftDO draft = new OntDraftDO();
        draft.setGraphId(graphId);
        draft.setDraftName(draftName != null ? draftName : "未命名草稿-" + System.currentTimeMillis());
        draft.setDraftType(draftType);
        draft.setSourceInfo(sourceInfo);
        draft.setGeneratedInfo(generatedInfo);
        draft.setStatus("PENDING");
        draft.setCreatedAt(LocalDateTime.now());
        draft.setUpdatedAt(LocalDateTime.now());
        draftMapper.insert(draft);
        return draft.getId();
    }

    @Override
    public OptimizeDescRespVO optimizeDescription(OptimizeDescReqVO reqVO) {
        try {
            String prompt = buildPrompt(PROMPT_PATH_OPTIMIZE, Map.of(
                "originalDescription", reqVO.getOriginalDescription() != null ? reqVO.getOriginalDescription() : "",
                "context", reqVO.getContext() != null ? reqVO.getContext() : "",
                "language", reqVO.getLanguage() != null ? reqVO.getLanguage() : "zh"
            ));

            OptimizeDescRespVO resp = llmClientService.chat(prompt, OptimizeDescRespVO.class);
            return resp;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("优化描述失败", e);
            throw new BusinessException(2004, "优化描述失败: " + e.getMessage());
        }
    }

    @Override
    public OptimizeDescRespVO optimizeBatch(OptimizeDescReqVO reqVO) {
        if (reqVO.getBatchItems() == null || reqVO.getBatchItems().isEmpty()) {
            return optimizeDescription(reqVO);
        }

        List<OptimizeDescRespVO.BatchOptimizeResult> results = new ArrayList<>();
        for (OptimizeDescReqVO.OptimizeItem item : reqVO.getBatchItems()) {
            try {
                OptimizeDescReqVO singleReq = OptimizeDescReqVO.builder()
                    .originalDescription(item.getOriginalDescription())
                    .context(item.getContext())
                    .language(reqVO.getLanguage())
                    .build();
                OptimizeDescRespVO singleResult = optimizeDescription(singleReq);
                results.add(OptimizeDescRespVO.BatchOptimizeResult.builder()
                    .id(item.getId())
                    .original(item.getOriginalDescription())
                    .optimizations(singleResult.getOptimizations())
                    .build());
            } catch (Exception e) {
                log.warn("批量优化失败，item={}: {}", item.getId(), e.getMessage());
                results.add(OptimizeDescRespVO.BatchOptimizeResult.builder()
                    .id(item.getId())
                    .original(item.getOriginalDescription())
                    .optimizations(List.of())
                    .build());
            }
        }

        return OptimizeDescRespVO.builder()
            .batchResults(results)
            .build();
    }

    @Override
    public GenerateDataRespVO generateMockData(String graphId, GenerateDataReqVO reqVO) {
        try {
            String prompt = buildPrompt(PROMPT_PATH_DATA, Map.of(
                "ontologyDefinition", "根据图谱ID " + graphId + " 的本体定义生成模拟数据",
                "count", String.valueOf(reqVO.getCount()),
                "format", reqVO.getFormat() != null ? reqVO.getFormat() : "JSON"
            ));

            GenerateDataRespVO resp = llmClientService.chat(prompt, GenerateDataRespVO.class);

            // 格式化输出
            if ("CSV".equalsIgnoreCase(reqVO.getFormat())) {
                resp.setFormattedData(formatAsCSV(resp));
            } else if ("N-TRIPLES".equalsIgnoreCase(reqVO.getFormat())
                || "NTRIPLES".equalsIgnoreCase(reqVO.getFormat())) {
                resp.setFormattedData(formatAsNTriples(resp));
            } else {
                resp.setFormattedData(objectMapper.writeValueAsString(resp));
            }

            // 统计
            resp.setStats(GenerateDataRespVO.DataStatsVO.builder()
                .totalEntities(resp.getEntities() != null ? resp.getEntities().size() : 0)
                .totalRelationships(resp.getRelationships() != null ? resp.getRelationships().size() : 0)
                .build());

            return resp;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成模拟数据失败: graphId={}", graphId, e);
            throw new BusinessException(2003, "生成模拟数据失败: " + e.getMessage());
        }
    }

    @Override
    public GenerateDataRespVO generateFromDraft(String graphId, Long draftId, GenerateDataReqVO reqVO) {
        OntDraftDO draft = draftMapper.selectById(draftId);
        if (draft == null) {
            throw new BusinessException(2000, "草稿不存在");
        }
        if (draft.getGeneratedInfo() == null) {
            throw new BusinessException(2005, "草稿中没有本体定义内容");
        }

        try {
            GenerateOntologyRespVO ontology = objectMapper.readValue(
                draft.getGeneratedInfo(), GenerateOntologyRespVO.class);

            String ontologyJson = objectMapper.writeValueAsString(ontology);
            String prompt = buildPrompt(PROMPT_PATH_DATA, Map.of(
                "ontologyDefinition", ontologyJson,
                "count", String.valueOf(reqVO.getCount()),
                "format", reqVO.getFormat() != null ? reqVO.getFormat() : "JSON"
            ));

            GenerateDataRespVO resp = llmClientService.chat(prompt, GenerateDataRespVO.class);
            resp.setDraftId(draftId);

            if ("CSV".equalsIgnoreCase(reqVO.getFormat())) {
                resp.setFormattedData(formatAsCSV(resp));
            } else if ("N-TRIPLES".equalsIgnoreCase(reqVO.getFormat())) {
                resp.setFormattedData(formatAsNTriples(resp));
            } else {
                resp.setFormattedData(objectMapper.writeValueAsString(resp));
            }

            // 保存模拟数据到草稿
            draft.setMockData(objectMapper.writeValueAsString(resp));
            draft.setUpdatedAt(LocalDateTime.now());
            draftMapper.updateById(draft);

            return resp;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("基于草稿生成模拟数据失败: draftId={}", draftId, e);
            throw new BusinessException(2003, "生成模拟数据失败: " + e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    private String buildPrompt(String templatePath, Map<String, String> variables) {
        String template = loadPromptTemplate(templatePath);
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            template = template.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return template;
    }

    private String loadPromptTemplate(String path) {
        try (var is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Prompt file not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("加载提示词模板失败: " + path, e);
        }
    }

    private String formatAsCSV(GenerateDataRespVO data) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Entities\n");
        sb.append("id,name,type\n");
        if (data.getEntities() != null) {
            for (GenerateDataRespVO.EntityVO entity : data.getEntities()) {
                sb.append(escapeCSV(entity.getId())).append(",")
                  .append(escapeCSV(entity.getName())).append(",")
                  .append(escapeCSV(entity.getType())).append("\n");
            }
        }
        sb.append("\n# Relationships\n");
        sb.append("id,source,target,type,fact\n");
        if (data.getRelationships() != null) {
            for (GenerateDataRespVO.RelationshipVO rel : data.getRelationships()) {
                sb.append(escapeCSV(rel.getId())).append(",")
                  .append(escapeCSV(rel.getSource())).append(",")
                  .append(escapeCSV(rel.getTarget())).append(",")
                  .append(escapeCSV(rel.getType())).append(",")
                  .append(escapeCSV(rel.getFact())).append("\n");
            }
        }
        return sb.toString();
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String formatAsNTriples(GenerateDataRespVO data) {
        StringBuilder sb = new StringBuilder();
        if (data.getEntities() != null) {
            for (GenerateDataRespVO.EntityVO entity : data.getEntities()) {
                String subject = "<http://example.org/entity/" + entity.getId() + ">";
                sb.append(subject).append(" <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://example.org/" + entity.getType() + "> .\n");
                if (entity.getProperties() != null) {
                    for (Map.Entry<String, Object> prop : entity.getProperties().entrySet()) {
                        sb.append(subject).append(" <http://example.org/").append(prop.getKey())
                          .append("> \"").append(prop.getValue()).append("\" .\n");
                    }
                }
            }
        }
        return sb.toString();
    }
}
