package com.graphiti.module.graphiti.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.module.graphiti.service.*;
import com.graphiti.module.graphiti.service.PromptTemplateService.RenderedPrompt;
import com.graphiti.module.graphiti.vo.legal.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 法律知识图谱 LLM 提取服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LegalExtractServiceImpl implements LegalExtractService {

    private final LlmClientService llmClientService;
    private final NodeService nodeService;
    private final GraphNeo4jService graphNeo4jService;
    private final EmbedderService embedderService;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    /**
     * 法律实体提取提示词模板编码
     */
    private static final String LEGAL_ENTITY_TEMPLATE_CODE = "LEGAL_CASE_EXTRACT";

    /**
     * 法律关系提取提示词模板编码
     */
    private static final String LEGAL_EDGE_TEMPLATE_CODE = "LEGAL_RELATION_EXTRACT";

    /**
     * 系统默认的法律提取提示词（当数据库中未配置时使用）
     */
    private static final String SYSTEM_PROMPT = """
        你是一个专业的法律知识图谱构建助手。你的任务是从给定的JSON数据中提取结构化的法律实体信息。
        
        请严格遵循以下实体类型定义进行提取：
        
        1. Case（案件）：包含 caseName（案件名称）、caseNumber（案号）、caseType（案件类型，民事/商事/刑事/行政）、caseStatus（案件状态）、filingDate（立案日期）、closedDate（结案日期）、amountInDispute（争议金额）、summary（案件摘要）、description（完整描述）
        
        2. Party（当事人）：包含 name（名称）、partyType（自然人/法人/非法人组织）、idNumber（身份证号/统一社会信用代码）、role（原告/被告/第三人/上诉人/被上诉人）、address（住所地）、contact（联系方式）、isEnterprise（是否企业）
        
        3. Court（法院）：包含 name（法院名称）、level（最高人民法院/高级人民法院/中级人民法院/基层人民法院）、location（所在地）、jurisdiction（管辖范围）、parentCourt（上级法院）
        
        4. Judge（法官）：包含 name（姓名）、title（职务）、courtName（所属法院）、specialty（专业领域）
        
        5. LegalProvision（法律条文）：包含 provisionId（条文编号）、articleNumber（条款序号）、content（条文内容）、lawName（法律名称）、lawType（法律/行政法规/司法解释）、effectiveDate（生效日期）、keywords（关键词）
        
        6. Lawyer（律师）：包含 name（姓名）、licenseNumber（执业证号）、firmName（所属律所）、specialty（专业领域）、contact（联系方式）
        
        7. Evidence（证据）：包含 evidenceNumber（证据编号）、evidenceType（证据类型）、content（内容摘要）、submittedBy（提交方）、submissionDate（提交日期）、purpose（证明目的）
        
        8. JudgmentDocument（裁判文书）：包含 documentNumber（文书编号）、documentType（文书类型）、issueDate（作出日期）、mainContent（主要内容）、judgmentResult（判决结果）、legalBasis（法律依据）
        
        提取规则：
        - 严格按照JSON字段映射关系，将JSON中的字段值提取到对应本体字段
        - 如果某个字段在JSON中不存在，标记为 null
        - 日期格式统一使用 YYYY-MM-DD
        - 金额统一为数字类型（单位：元）
        - 只提取在JSON中能找到对应值的字段，不要虚构数据
        - 尽可能提取所有能找到的法律实体
        - 返回格式必须是合法的JSON对象
        """;

    @Override
    public LegalExtractResultVO extractFromJson(String graphId, String jsonContent,
                                               Map<String, String> fieldMapping, String sourceFileName) {
        log.info("开始从JSON提取法律知识图谱: graphId={}, file={}, fieldMapping={}",
                graphId, sourceFileName, fieldMapping);

        try {
            // 1. 尝试使用配置化的提示词模板
            RenderedPrompt renderedPrompt = tryGetRenderedPrompt(jsonContent, fieldMapping);

            String systemPrompt;
            String userPrompt;

            if (renderedPrompt != null) {
                // 使用配置化的提示词
                systemPrompt = renderedPrompt.systemPrompt();
                userPrompt = renderedPrompt.userPrompt();
                log.info("使用配置化的提示词模板: systemPrompt长度={}, userPrompt长度={}",
                        systemPrompt.length(), userPrompt.length());
            } else {
                // 回退到默认提示词
                log.info("使用默认提示词（模板未配置）");
                String extractedContent = buildExtractedContent(jsonContent, fieldMapping);
                systemPrompt = SYSTEM_PROMPT;
                userPrompt = buildUserPrompt(extractedContent, fieldMapping);
            }

            // 2. 调用 LLM 提取
            String llmResponse = llmClientService.chat(systemPrompt, userPrompt);

            // 3. 解析 LLM 返回结果
            LegalExtractResultVO result = parseLlmResponse(llmResponse, sourceFileName);

            log.info("JSON提取完成: cases={}, parties={}, courts={}, provisions={}",
                    safeSize(result.getCases()), safeSize(result.getParties()),
                    safeSize(result.getCourts()), safeSize(result.getProvisions()));

            return result;

        } catch (Exception e) {
            log.error("JSON提取失败: graphId={}, error={}", graphId, e.getMessage(), e);
            LegalExtractResultVO errorResult = LegalExtractResultVO.builder()
                    .sourceFileName(sourceFileName)
                    .errors(List.of("提取失败: " + e.getMessage()))
                    .build();
            return errorResult;
        }
    }

    /**
     * 尝试获取配置化的提示词模板
     */
    private RenderedPrompt tryGetRenderedPrompt(String jsonContent, Map<String, String> fieldMapping) {
        try {
            // 尝试从数据库获取法律实体提取模板
            var templateOpt = promptTemplateService.getTemplateByCode(LEGAL_ENTITY_TEMPLATE_CODE);
            if (templateOpt.isEmpty()) {
                log.debug("未找到法律实体提取模板: {}", LEGAL_ENTITY_TEMPLATE_CODE);
                return null;
            }

            var template = templateOpt.get();
            if (!template.getEnabled()) {
                log.debug("法律实体提取模板已禁用: {}", LEGAL_ENTITY_TEMPLATE_CODE);
                return null;
            }

            // 构建变量映射
            Map<String, Object> variables = new LinkedHashMap<>();
            variables.put("episode_content", jsonContent);
            variables.put("source_description", "法律案件数据");

            // 如果有字段映射，添加到提示词中
            if (fieldMapping != null && !fieldMapping.isEmpty()) {
                StringBuilder fieldMappingStr = new StringBuilder("字段映射关系：\n");
                for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
                    fieldMappingStr.append("- JSON字段 '").append(entry.getKey())
                            .append("' 对应本体字段 '").append(entry.getValue()).append("'\n");
                }
                variables.put("field_mapping", fieldMappingStr.toString());
            }

            // 通过模板ID渲染提示词
            return promptTemplateService.getRenderedPrompt(template.getId(), variables);

        } catch (Exception e) {
            log.warn("获取配置化提示词失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LegalExtractResultVO extractAndImport(String graphId, String jsonContent,
                                                 Map<String, String> fieldMapping, String sourceFileName) {
        // 1. 先提取
        LegalExtractResultVO result = extractFromJson(graphId, jsonContent, fieldMapping, sourceFileName);

        if (result.getErrors() != null && !result.getErrors().isEmpty()) {
            return result;
        }

        // 2. 导入节点
        int nodeCount = 0;
        int edgeCount = 0;
        List<String> errors = result.getErrors() != null ? new ArrayList<>(result.getErrors()) : new ArrayList<>();

        try {
            nodeCount += importCases(graphId, result.getCases());
            nodeCount += importParties(graphId, result.getParties());
            nodeCount += importCourts(graphId, result.getCourts());
            nodeCount += importJudges(graphId, result.getJudges());
            nodeCount += importProvisions(graphId, result.getProvisions());
            nodeCount += importLawyers(graphId, result.getLawyers());
            nodeCount += importEvidences(graphId, result.getEvidences());
            nodeCount += importJudgments(graphId, result.getJudgments());

            // 3. 构建并导入边
            edgeCount = buildAndImportEdges(graphId, result);

        } catch (Exception e) {
            log.error("导入失败: graphId={}, error={}", graphId, e.getMessage(), e);
            errors.add("导入失败: " + e.getMessage());
        }

        result.setTotalNodes(nodeCount);
        result.setTotalEdges(edgeCount);
        result.setErrors(errors);

        log.info("提取并导入完成: graphId={}, nodes={}, edges={}", graphId, nodeCount, edgeCount);
        return result;
    }

    /**
     * 根据字段映射从 JSON 中提取内容文本
     */
    private String buildExtractedContent(String jsonContent, Map<String, String> fieldMapping) throws Exception {
        JsonNode root = objectMapper.readTree(jsonContent);
        StringBuilder sb = new StringBuilder();

        // 提取映射字段的值
        for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
            String jsonPath = entry.getKey();
            String ontField = entry.getValue();

            Object value = getJsonValue(root, jsonPath);
            if (value != null) {
                sb.append(ontField).append(": ").append(value.toString()).append("\n");
            }
        }

        // 如果没有映射字段，尝试提取整个 JSON 的关键字段
        if (sb.isEmpty()) {
            sb.append("原始JSON内容:\n").append(jsonContent);
        }

        return sb.toString();
    }

    /**
     * 从 JSON 节点中按路径获取值
     */
    private Object getJsonValue(JsonNode root, String jsonPath) {
        try {
            String[] parts = jsonPath.split("\\.");
            JsonNode current = root;
            for (String part : parts) {
                if (current == null) return null;
                // 处理数组索引，如 items[0]
                if (part.contains("[")) {
                    int idx = Integer.parseInt(part.replaceAll("[^0-9]", ""));
                    String fieldName = part.replaceAll("\\[.*\\]", "");
                    current = current.get(fieldName);
                    if (current != null && current.isArray()) {
                        current = current.get(idx);
                    } else {
                        return null;
                    }
                } else {
                    current = current.get(part);
                }
            }
            if (current != null) {
                if (current.isTextual()) return current.asText();
                if (current.isNumber()) return current.asText();
                if (current.isArray() || current.isObject()) return current.toString();
                return current.asText();
            }
        } catch (Exception e) {
            log.warn("解析JSON路径失败: path={}, error={}", jsonPath, e.getMessage());
        }
        return null;
    }

    /**
     * 构建用户 prompt
     */
    private String buildUserPrompt(String extractedContent, Map<String, String> fieldMapping) {
        StringBuilder sb = new StringBuilder();
        sb.append("请从以下JSON数据中提取法律知识图谱实体。\n\n");
        sb.append("字段映射关系:\n");
        for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
            sb.append("- JSON字段 '").append(entry.getKey())
              .append("' 对应本体字段 '").append(entry.getValue()).append("'\n");
        }
        sb.append("\n提取的数据内容:\n").append(extractedContent);
        sb.append("\n\n请以JSON格式返回提取结果，格式如下:\n");
        sb.append("""
                {
                  "cases": [...],
                  "parties": [...],
                  "courts": [...],
                  "judges": [...],
                  "provisions": [...],
                  "lawyers": [...],
                  "evidences": [...],
                  "judgments": [...]
                }
                """);
        sb.append("\n只返回JSON，不要有其他文字。");
        return sb.toString();
    }

    /**
     * 解析 LLM 返回结果
     */
    private LegalExtractResultVO parseLlmResponse(String llmResponse, String sourceFileName) {
        try {
            // 清理 LLM 返回，尝试提取 JSON 部分
            String jsonStr = extractJsonFromResponse(llmResponse);
            Map<String, Object> responseMap = objectMapper.readValue(jsonStr,
                    new TypeReference<Map<String, Object>>() {});

            LegalExtractResultVO result = LegalExtractResultVO.builder()
                    .sourceFileName(sourceFileName)
                    .cases(parseList(responseMap.get("cases"), ExtractedCaseVO.class))
                    .parties(parseList(responseMap.get("parties"), ExtractedPartyVO.class))
                    .courts(parseList(responseMap.get("courts"), ExtractedCourtVO.class))
                    .judges(parseList(responseMap.get("judges"), ExtractedJudgeVO.class))
                    .provisions(parseList(responseMap.get("provisions"), ExtractedProvisionVO.class))
                    .lawyers(parseList(responseMap.get("lawyers"), ExtractedLawyerVO.class))
                    .evidences(parseList(responseMap.get("evidences"), ExtractedEvidenceVO.class))
                    .judgments(parseList(responseMap.get("judgments"), ExtractedJudgmentVO.class))
                    .errors(new ArrayList<>())
                    .build();

            // 计算总节点数
            int totalNodes = safeSize(result.getCases()) + safeSize(result.getParties())
                    + safeSize(result.getCourts()) + safeSize(result.getJudges())
                    + safeSize(result.getProvisions()) + safeSize(result.getLawyers())
                    + safeSize(result.getEvidences()) + safeSize(result.getJudgments());
            result.setTotalNodes(totalNodes);

            return result;

        } catch (Exception e) {
            log.error("解析LLM返回失败: error={}, response={}", e.getMessage(), llmResponse);
            return LegalExtractResultVO.builder()
                    .sourceFileName(sourceFileName)
                    .errors(List.of("解析LLM返回失败: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * 从 LLM 返回中提取 JSON 字符串
     */
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

        throw new RuntimeException("无法从LLM返回中提取JSON内容");
    }

    /**
     * 解析列表
     */
    private <T> List<T> parseList(Object obj, Class<T> elementType) {
        if (obj == null) return new ArrayList<>();
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            List<T> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map) {
                    try {
                        T entity = objectMapper.convertValue(item, elementType);
                        result.add(entity);
                    } catch (Exception e) {
                        log.warn("转换实体失败: type={}, error={}", elementType.getSimpleName(), e.getMessage());
                    }
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    private int safeSize(List<?> list) {
        return list != null ? list.size() : 0;
    }

    // ============ 导入节点的方法 ============

    private int importCases(String graphId, List<ExtractedCaseVO> cases) {
        if (cases == null || cases.isEmpty()) return 0;
        int count = 0;
        for (ExtractedCaseVO c : cases) {
            try {
                Map<String, Object> nodeData = new HashMap<>();
                nodeData.put("name", c.getCaseName() != null ? c.getCaseName() : c.getCaseNumber());
                nodeData.put("type", "Case");
                nodeData.put("summary", c.getSummary() != null ? c.getSummary() : c.getDescription());

                Map<String, Object> props = new HashMap<>();
                if (c.getCaseName() != null) props.put("caseName", c.getCaseName());
                if (c.getCaseNumber() != null) props.put("caseNumber", c.getCaseNumber());
                if (c.getCaseType() != null) props.put("caseType", c.getCaseType());
                if (c.getCaseStatus() != null) props.put("caseStatus", c.getCaseStatus());
                if (c.getFilingDate() != null) props.put("filingDate", c.getFilingDate());
                if (c.getClosedDate() != null) props.put("closedDate", c.getClosedDate());
                if (c.getAmountInDispute() != null) props.put("amountInDispute", c.getAmountInDispute());
                if (c.getSummary() != null) props.put("summary", c.getSummary());
                if (c.getDescription() != null) props.put("description", c.getDescription());
                if (c.getUuid() != null) props.put("sourceUuid", c.getUuid());

                nodeData.put("properties", props);
                nodeService.createNode(graphId, nodeData);
                count++;
            } catch (Exception e) {
                log.warn("导入Case失败: caseName={}, error={}", c.getCaseName(), e.getMessage());
            }
        }
        return count;
    }

    private int importParties(String graphId, List<ExtractedPartyVO> parties) {
        if (parties == null || parties.isEmpty()) return 0;
        int count = 0;
        for (ExtractedPartyVO p : parties) {
            try {
                Map<String, Object> nodeData = new HashMap<>();
                nodeData.put("name", p.getName());
                nodeData.put("type", "Party");
                nodeData.put("summary", p.getName() + "，" + p.getRole());

                Map<String, Object> props = new HashMap<>();
                if (p.getName() != null) props.put("name", p.getName());
                if (p.getPartyType() != null) props.put("partyType", p.getPartyType());
                if (p.getIdNumber() != null) props.put("idNumber", p.getIdNumber());
                if (p.getRole() != null) props.put("role", p.getRole());
                if (p.getAddress() != null) props.put("address", p.getAddress());
                if (p.getContact() != null) props.put("contact", p.getContact());
                if (p.getIsEnterprise() != null) props.put("isEnterprise", p.getIsEnterprise());
                if (p.getUuid() != null) props.put("sourceUuid", p.getUuid());

                nodeData.put("properties", props);
                nodeService.createNode(graphId, nodeData);
                count++;
            } catch (Exception e) {
                log.warn("导入Party失败: name={}, error={}", p.getName(), e.getMessage());
            }
        }
        return count;
    }

    private int importCourts(String graphId, List<ExtractedCourtVO> courts) {
        if (courts == null || courts.isEmpty()) return 0;
        int count = 0;
        for (ExtractedCourtVO c : courts) {
            try {
                Map<String, Object> nodeData = new HashMap<>();
                nodeData.put("name", c.getName());
                nodeData.put("type", "Court");
                nodeData.put("summary", c.getName() + "，" + c.getLevel());

                Map<String, Object> props = new HashMap<>();
                if (c.getName() != null) props.put("name", c.getName());
                if (c.getLevel() != null) props.put("level", c.getLevel());
                if (c.getLocation() != null) props.put("location", c.getLocation());
                if (c.getJurisdiction() != null) props.put("jurisdiction", c.getJurisdiction());
                if (c.getParentCourt() != null) props.put("parentCourt", c.getParentCourt());
                if (c.getUuid() != null) props.put("sourceUuid", c.getUuid());

                nodeData.put("properties", props);
                nodeService.createNode(graphId, nodeData);
                count++;
            } catch (Exception e) {
                log.warn("导入Court失败: name={}, error={}", c.getName(), e.getMessage());
            }
        }
        return count;
    }

    private int importJudges(String graphId, List<ExtractedJudgeVO> judges) {
        if (judges == null || judges.isEmpty()) return 0;
        int count = 0;
        for (ExtractedJudgeVO j : judges) {
            try {
                Map<String, Object> nodeData = new HashMap<>();
                nodeData.put("name", j.getName());
                nodeData.put("type", "Judge");
                nodeData.put("summary", j.getName() + "，" + j.getTitle());

                Map<String, Object> props = new HashMap<>();
                if (j.getName() != null) props.put("name", j.getName());
                if (j.getTitle() != null) props.put("title", j.getTitle());
                if (j.getCourtName() != null) props.put("courtName", j.getCourtName());
                if (j.getSpecialty() != null) props.put("specialty", j.getSpecialty());
                if (j.getUuid() != null) props.put("sourceUuid", j.getUuid());

                nodeData.put("properties", props);
                nodeService.createNode(graphId, nodeData);
                count++;
            } catch (Exception e) {
                log.warn("导入Judge失败: name={}, error={}", j.getName(), e.getMessage());
            }
        }
        return count;
    }

    private int importProvisions(String graphId, List<ExtractedProvisionVO> provisions) {
        if (provisions == null || provisions.isEmpty()) return 0;
        int count = 0;
        for (ExtractedProvisionVO p : provisions) {
            try {
                String name = p.getProvisionId() != null ? p.getProvisionId()
                        : (p.getLawName() != null ? p.getLawName() + " " + p.getArticleNumber() : p.getArticleNumber());
                Map<String, Object> nodeData = new HashMap<>();
                nodeData.put("name", name);
                nodeData.put("type", "LegalProvision");
                nodeData.put("summary", name + "：" + (p.getContent() != null ? p.getContent().substring(0, Math.min(100, p.getContent().length())) : ""));

                Map<String, Object> props = new HashMap<>();
                if (p.getProvisionId() != null) props.put("provisionId", p.getProvisionId());
                if (p.getArticleNumber() != null) props.put("articleNumber", p.getArticleNumber());
                if (p.getContent() != null) props.put("content", p.getContent());
                if (p.getLawName() != null) props.put("lawName", p.getLawName());
                if (p.getLawType() != null) props.put("lawType", p.getLawType());
                if (p.getEffectiveDate() != null) props.put("effectiveDate", p.getEffectiveDate());
                if (p.getKeywords() != null) props.put("keywords", p.getKeywords());
                if (p.getUuid() != null) props.put("sourceUuid", p.getUuid());

                nodeData.put("properties", props);
                nodeService.createNode(graphId, nodeData);
                count++;
            } catch (Exception e) {
                log.warn("导入LegalProvision失败: provisionId={}, error={}", p.getProvisionId(), e.getMessage());
            }
        }
        return count;
    }

    private int importLawyers(String graphId, List<ExtractedLawyerVO> lawyers) {
        if (lawyers == null || lawyers.isEmpty()) return 0;
        int count = 0;
        for (ExtractedLawyerVO l : lawyers) {
            try {
                Map<String, Object> nodeData = new HashMap<>();
                nodeData.put("name", l.getName());
                nodeData.put("type", "Lawyer");
                nodeData.put("summary", l.getName() + "，" + l.getFirmName());

                Map<String, Object> props = new HashMap<>();
                if (l.getName() != null) props.put("name", l.getName());
                if (l.getLicenseNumber() != null) props.put("licenseNumber", l.getLicenseNumber());
                if (l.getFirmName() != null) props.put("firmName", l.getFirmName());
                if (l.getSpecialty() != null) props.put("specialty", l.getSpecialty());
                if (l.getContact() != null) props.put("contact", l.getContact());
                if (l.getUuid() != null) props.put("sourceUuid", l.getUuid());

                nodeData.put("properties", props);
                nodeService.createNode(graphId, nodeData);
                count++;
            } catch (Exception e) {
                log.warn("导入Lawyer失败: name={}, error={}", l.getName(), e.getMessage());
            }
        }
        return count;
    }

    private int importEvidences(String graphId, List<ExtractedEvidenceVO> evidences) {
        if (evidences == null || evidences.isEmpty()) return 0;
        int count = 0;
        for (ExtractedEvidenceVO e : evidences) {
            try {
                String name = e.getEvidenceNumber() != null ? e.getEvidenceNumber()
                        : (e.getContent() != null ? e.getContent().substring(0, Math.min(50, e.getContent().length())) : "证据");
                Map<String, Object> nodeData = new HashMap<>();
                nodeData.put("name", name);
                nodeData.put("type", "Evidence");
                nodeData.put("summary", name + "，" + e.getEvidenceType());

                Map<String, Object> props = new HashMap<>();
                if (e.getEvidenceNumber() != null) props.put("evidenceNumber", e.getEvidenceNumber());
                if (e.getEvidenceType() != null) props.put("evidenceType", e.getEvidenceType());
                if (e.getContent() != null) props.put("content", e.getContent());
                if (e.getSubmittedBy() != null) props.put("submittedBy", e.getSubmittedBy());
                if (e.getSubmissionDate() != null) props.put("submissionDate", e.getSubmissionDate());
                if (e.getPurpose() != null) props.put("purpose", e.getPurpose());
                if (e.getUuid() != null) props.put("sourceUuid", e.getUuid());

                nodeData.put("properties", props);
                nodeService.createNode(graphId, nodeData);
                count++;
            } catch (Exception ex) {
                log.warn("导入Evidence失败: evidenceNumber={}, error={}", e.getEvidenceNumber(), ex.getMessage());
            }
        }
        return count;
    }

    private int importJudgments(String graphId, List<ExtractedJudgmentVO> judgments) {
        if (judgments == null || judgments.isEmpty()) return 0;
        int count = 0;
        for (ExtractedJudgmentVO j : judgments) {
            try {
                String name = j.getDocumentNumber() != null ? j.getDocumentNumber()
                        : (j.getDocumentType() != null ? j.getDocumentType() : "裁判文书");
                Map<String, Object> nodeData = new HashMap<>();
                nodeData.put("name", name);
                nodeData.put("type", "JudgmentDocument");
                nodeData.put("summary", name + "，" + j.getJudgmentResult());

                Map<String, Object> props = new HashMap<>();
                if (j.getDocumentNumber() != null) props.put("documentNumber", j.getDocumentNumber());
                if (j.getDocumentType() != null) props.put("documentType", j.getDocumentType());
                if (j.getIssueDate() != null) props.put("issueDate", j.getIssueDate());
                if (j.getMainContent() != null) props.put("mainContent", j.getMainContent());
                if (j.getJudgmentResult() != null) props.put("judgmentResult", j.getJudgmentResult());
                if (j.getLegalBasis() != null) props.put("legalBasis", j.getLegalBasis());
                if (j.getUuid() != null) props.put("sourceUuid", j.getUuid());

                nodeData.put("properties", props);
                nodeService.createNode(graphId, nodeData);
                count++;
            } catch (Exception ex) {
                log.warn("导入JudgmentDocument失败: documentNumber={}, error={}", j.getDocumentNumber(), ex.getMessage());
            }
        }
        return count;
    }

    /**
     * 构建并导入边
     */
    private int buildAndImportEdges(String graphId, LegalExtractResultVO result) {
        List<Map<String, Object>> edges = new ArrayList<>();
        Map<String, String> nameToUuid = buildNameToUuidMap(graphId);

        // 案件-当事人边
        if (result.getCases() != null && result.getParties() != null) {
            for (ExtractedCaseVO c : result.getCases()) {
                String caseName = c.getCaseName() != null ? c.getCaseName() : c.getCaseNumber();
                for (ExtractedPartyVO p : result.getParties()) {
                    if (p.getRole() != null) {
                        edges.add(buildEdge(caseName, p.getName(), "CASE_PARTY",
                                Map.of("role", p.getRole())));
                    }
                }
            }
        }

        // 案件-法院边
        if (result.getCases() != null && result.getCourts() != null) {
            for (ExtractedCaseVO c : result.getCases()) {
                String caseName = c.getCaseName() != null ? c.getCaseName() : c.getCaseNumber();
                for (ExtractedCourtVO court : result.getCourts()) {
                    edges.add(buildEdge(caseName, court.getName(), "CASE_COURT",
                            Map.of("courtRole", "立案法院")));
                }
            }
        }

        // 案件-法官边
        if (result.getCases() != null && result.getJudges() != null) {
            for (ExtractedCaseVO c : result.getCases()) {
                String caseName = c.getCaseName() != null ? c.getCaseName() : c.getCaseNumber();
                for (ExtractedJudgeVO j : result.getJudges()) {
                    edges.add(buildEdge(caseName, j.getName(), "CASE_JUDGE",
                            Map.of("role", j.getTitle() != null ? j.getTitle() : "审判员")));
                }
            }
        }

        // 导入边
        return importEdges(graphId, edges, nameToUuid);
    }

    private Map<String, String> buildNameToUuidMap(String graphId) {
        Map<String, String> map = new HashMap<>();
        try {
            List<Map<String, Object>> nodes = graphNeo4jService.listNodes(graphId, 0, 10000);
            for (Map<String, Object> node : nodes) {
                String name = (String) node.get("name");
                String uuid = (String) node.get("uuid");
                if (name != null && uuid != null) {
                    map.put(name, uuid);
                }
            }
        } catch (Exception e) {
            log.warn("构建名称->UUID映射失败: graphId={}, error={}", graphId, e.getMessage());
        }
        return map;
    }

    private Map<String, Object> buildEdge(String sourceName, String targetName, String type, Map<String, Object> props) {
        Map<String, Object> edge = new HashMap<>();
        edge.put("sourceName", sourceName);
        edge.put("targetName", targetName);
        edge.put("type", type);
        edge.put("properties", props);
        edge.put("fact", type + ": " + sourceName + " -> " + targetName);
        return edge;
    }

    private int importEdges(String graphId, List<Map<String, Object>> edges, Map<String, String> nameToUuid) {
        int count = 0;
        for (Map<String, Object> edgeData : edges) {
            try {
                String sourceName = (String) edgeData.get("sourceName");
                String targetName = (String) edgeData.get("targetName");
                String sourceUuid = nameToUuid.get(sourceName);
                String targetUuid = nameToUuid.get(targetName);

                if (sourceUuid == null || targetUuid == null) {
                    log.debug("跳过边（节点未找到）: {} -> {}", sourceName, targetName);
                    continue;
                }

                String edgeType = (String) edgeData.get("type");
                String fact = (String) edgeData.getOrDefault("fact", edgeType + ": " + sourceName + " -> " + targetName);

                float[] embedding = embedderService.embed(fact);
                String edgeUuid = UUID.randomUUID().toString().replace("-", "");

                @SuppressWarnings("unchecked")
                Map<String, Object> props = (Map<String, Object>) edgeData.get("properties");

                graphNeo4jService.createRelationship(
                        graphId, edgeUuid, sourceUuid, targetUuid,
                        edgeType, fact, embedding, props);
                count++;
            } catch (Exception e) {
                log.warn("导入边失败: error={}", e.getMessage());
            }
        }
        return count;
    }

    // ========== 增强方法 ==========

    @Override
    public LegalExtractResultVO extractWithTemplate(String graphId, String jsonContent,
                                                     String templateCode, String sourceFileName) {
        log.info("使用模板提取法律知识图谱: graphId={}, templateCode={}, file={}",
                graphId, templateCode, sourceFileName);

        try {
            // 尝试获取指定的模板
            var templateOpt = promptTemplateService.getTemplateByCode(templateCode);
            if (templateOpt.isEmpty()) {
                log.warn("模板不存在: {}，使用默认提示词", templateCode);
                return extractFromJson(graphId, jsonContent, null, sourceFileName);
            }

            var template = templateOpt.get();
            if (!template.getEnabled()) {
                log.warn("模板已禁用: {}，使用默认提示词", templateCode);
                return extractFromJson(graphId, jsonContent, null, sourceFileName);
            }

            // 构建变量
            Map<String, Object> variables = new LinkedHashMap<>();
            variables.put("episode_content", jsonContent);
            variables.put("source_description", sourceFileName != null ? sourceFileName : "法律案件数据");

            // 渲染提示词
            RenderedPrompt rendered = promptTemplateService.getRenderedPrompt(template.getId(), variables);

            // 调用 LLM
            String llmResponse = llmClientService.chat(rendered.systemPrompt(), rendered.userPrompt());

            // 解析结果
            LegalExtractResultVO result = parseLlmResponse(llmResponse, sourceFileName);

            log.info("模板提取完成: templateCode={}, cases={}, parties={}",
                    templateCode, safeSize(result.getCases()), safeSize(result.getParties()));

            return result;

        } catch (Exception e) {
            log.error("模板提取失败: graphId={}, templateCode={}, error={}",
                    graphId, templateCode, e.getMessage(), e);
            return LegalExtractResultVO.builder()
                    .sourceFileName(sourceFileName)
                    .errors(List.of("模板提取失败: " + e.getMessage()))
                    .build();
        }
    }

    @Override
    public LegalExtractResultVO extractOnly(String jsonContent, String templateCode) {
        String template = templateCode != null ? templateCode : LEGAL_ENTITY_TEMPLATE_CODE;
        return extractWithTemplate(null, jsonContent, template, null);
    }

    @Override
    public List<String> getAvailableTemplates() {
        List<String> templates = new ArrayList<>();

        // 查询所有法律相关的模板
        var entityTemplates = promptTemplateService.listTemplatesByType("entity_extract");
        for (var template : entityTemplates) {
            if (template.getCode().toUpperCase().contains("LEGAL")) {
                templates.add(template.getCode());
            }
        }

        // 如果没有配置的法律模板，添加默认模板编码
        if (templates.isEmpty()) {
            templates.add(LEGAL_ENTITY_TEMPLATE_CODE);
        }

        return templates;
    }

    @Override
    public String getDefaultPrompt() {
        return SYSTEM_PROMPT;
    }
}
