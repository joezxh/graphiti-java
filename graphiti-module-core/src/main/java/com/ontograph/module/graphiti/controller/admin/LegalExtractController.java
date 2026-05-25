package com.ontograph.module.graphiti.controller.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontograph.common.response.CommonResult;
import com.ontograph.module.graphiti.service.LegalExtractService;
import com.ontograph.module.graphiti.vo.legal.LegalExtractResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 法律知识图谱 LLM 提取控制器
 * Legal Knowledge Graph LLM Extraction Controller
 *
 * <p>提供从 JSON 文件中提取法律知识图谱的接口：
 * <ul>
 *   <li>上传 JSON 文件，预览字段结构</li>
 *   <li>根据字段映射配置，调用 LLM 提取法律实体</li>
 *   <li>将提取结果导入知识图谱</li>
 * </ul>
 */
@Tag(name = "法律知识图谱提取", description = "从JSON文件中提取法律知识图谱的接口")
@RestController
@RequestMapping("/api/v1/graph/legal/extract")
@RequiredArgsConstructor
@Slf4j
public class LegalExtractController {

    @Resource
    private LegalExtractService legalExtractService;

    private final ObjectMapper objectMapper;

    /**
     * 上传 JSON 文件，预览字段结构
     *
     * POST /api/v1/graph/legal/extract/preview
     */
    @PostMapping("/preview")
    @Operation(summary = "预览JSON字段结构", description = "上传JSON文件并预览其字段结构",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Map<String, Object>> previewJsonFile(
            @RequestParam("file") MultipartFile file) {
        log.info("预览JSON文件: fileName={}, size={}", file.getOriginalFilename(), file.getSize());

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(content);

            // 构建字段树
            Map<String, Object> fieldTree = buildFieldTree(root, "");
            long nodeCount = countNodes(root);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("fileName", file.getOriginalFilename());
            result.put("fileSize", file.getSize());
            result.put("fieldCount", countFields(root));
            result.put("fieldTree", fieldTree);
            result.put("sampleData", extractSampleData(root, 3));
            result.put("contentPreview", content.substring(0, Math.min(2000, content.length())));

            log.info("JSON预览完成: fileName={}, fields={}", file.getOriginalFilename(), countFields(root));
            return CommonResult.success(result);

        } catch (IOException e) {
            log.error("预览JSON文件失败: fileName={}, error={}", file.getOriginalFilename(), e.getMessage());
            return CommonResult.error(400, "JSON文件解析失败: " + e.getMessage());
        }
    }

    /**
     * 提取法律知识图谱（仅提取，不保存）
     *
     * POST /api/v1/graph/legal/extract
     */
    @PostMapping
    @Operation(summary = "提取法律知识图谱", description = "根据字段映射从JSON中提取法律实体（不保存到图谱）",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<LegalExtractResultVO> extractLegalKG(
            @RequestParam("file") MultipartFile file,
            @RequestParam("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestParam("fieldMapping") @Parameter(description = "字段映射JSON", required = true) String fieldMappingJson,
            @RequestParam(value = "templateCode", required = false) @Parameter(description = "提示词模板编码") String templateCode) {

        log.info("提取法律知识图谱: graphId={}, fileName={}, templateCode={}", graphId, file.getOriginalFilename(), templateCode);

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            Map<String, String> fieldMapping = objectMapper.readValue(fieldMappingJson,
                    objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));

            LegalExtractResultVO result;
            if (templateCode != null && !templateCode.isBlank()) {
                result = legalExtractService.extractWithTemplate(graphId, content, templateCode, file.getOriginalFilename());
            } else {
                result = legalExtractService.extractFromJson(graphId, content, fieldMapping, file.getOriginalFilename());
            }

            return CommonResult.success(result);

        } catch (Exception e) {
            log.error("提取法律知识图谱失败: graphId={}, fileName={}, error={}",
                    graphId, file.getOriginalFilename(), e.getMessage(), e);
            return CommonResult.error(500, "提取失败: " + e.getMessage());
        }
    }

    /**
     * 提取并保存法律知识图谱
     *
     * POST /api/v1/graph/legal/extract/save
     */
    @PostMapping("/save")
    @Operation(summary = "提取并保存法律知识图谱", description = "根据字段映射从JSON中提取法律实体并保存到图谱",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<LegalExtractResultVO> extractAndSaveLegalKG(
            @RequestParam("file") MultipartFile file,
            @RequestParam("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestParam("fieldMapping") @Parameter(description = "字段映射JSON", required = true) String fieldMappingJson) {

        log.info("提取并保存法律知识图谱: graphId={}, fileName={}", graphId, file.getOriginalFilename());

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            Map<String, String> fieldMapping = objectMapper.readValue(fieldMappingJson,
                    objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));

            LegalExtractResultVO result = legalExtractService.extractAndImport(
                    graphId, content, fieldMapping, file.getOriginalFilename());

            return CommonResult.success(result);

        } catch (Exception e) {
            log.error("提取并保存失败: graphId={}, fileName={}, error={}",
                    graphId, file.getOriginalFilename(), e.getMessage(), e);
            return CommonResult.error(500, "提取并保存失败: " + e.getMessage());
        }
    }

    /**
     * 获取本体字段列表（用于前端映射）
     */
    @GetMapping("/ontology-fields")
    @Operation(summary = "获取本体字段列表", description = "获取所有法律实体的本体字段定义，用于字段映射",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Map<String, Object>> getOntologyFields() {
        Map<String, Object> result = new LinkedHashMap<>();

        // Case 字段
        result.put("Case", Map.of(
                "displayName", "案件",
                "fields", Map.of(
                        "caseName", "案件名称",
                        "caseNumber", "案号",
                        "caseType", "案件类型",
                        "caseStatus", "案件状态",
                        "filingDate", "立案日期",
                        "closedDate", "结案日期",
                        "amountInDispute", "争议金额",
                        "summary", "案件摘要",
                        "description", "完整描述"
                )
        ));

        // Party 字段
        result.put("Party", Map.of(
                "displayName", "当事人",
                "fields", Map.of(
                        "name", "当事人名称",
                        "partyType", "当事人类型",
                        "idNumber", "证件号码",
                        "role", "诉讼角色",
                        "address", "住所地",
                        "contact", "联系方式",
                        "isEnterprise", "是否企业"
                )
        ));

        // Court 字段
        result.put("Court", Map.of(
                "displayName", "法院",
                "fields", Map.of(
                        "name", "法院名称",
                        "level", "法院级别",
                        "location", "所在地",
                        "jurisdiction", "管辖范围",
                        "parentCourt", "上级法院"
                )
        ));

        // Judge 字段
        result.put("Judge", Map.of(
                "displayName", "法官",
                "fields", Map.of(
                        "name", "法官姓名",
                        "title", "职务",
                        "courtName", "所属法院",
                        "specialty", "专业领域"
                )
        ));

        // LegalProvision 字段
        result.put("LegalProvision", Map.of(
                "displayName", "法律条文",
                "fields", Map.of(
                        "provisionId", "条文编号",
                        "articleNumber", "条款序号",
                        "content", "条文内容",
                        "lawName", "法律名称",
                        "lawType", "法律类型",
                        "effectiveDate", "生效日期",
                        "keywords", "关键词"
                )
        ));

        // Lawyer 字段
        result.put("Lawyer", Map.of(
                "displayName", "律师",
                "fields", Map.of(
                        "name", "律师姓名",
                        "licenseNumber", "执业证号",
                        "firmName", "所属律所",
                        "specialty", "专业领域",
                        "contact", "联系方式"
                )
        ));

        // Evidence 字段
        result.put("Evidence", Map.of(
                "displayName", "证据",
                "fields", Map.of(
                        "evidenceNumber", "证据编号",
                        "evidenceType", "证据类型",
                        "content", "证据内容",
                        "submittedBy", "提交方",
                        "submissionDate", "提交日期",
                        "purpose", "证明目的"
                )
        ));

        // JudgmentDocument 字段
        result.put("JudgmentDocument", Map.of(
                "displayName", "裁判文书",
                "fields", Map.of(
                        "documentNumber", "文书编号",
                        "documentType", "文书类型",
                        "issueDate", "作出日期",
                        "mainContent", "主要内容",
                        "judgmentResult", "判决结果",
                        "legalBasis", "法律依据"
                )
        ));

        return CommonResult.success(result);
    }

    /**
     * 获取可用的法律提取模板列表
     */
    @GetMapping("/templates")
    @Operation(summary = "获取可用的法律提取模板", description = "获取所有可用的法律提取提示词模板",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Map<String, Object>> getAvailableTemplates() {
        List<String> templates = legalExtractService.getAvailableTemplates();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templates", templates);
        result.put("defaultTemplate", "LEGAL_CASE_EXTRACT");
        result.put("defaultPrompt", legalExtractService.getDefaultPrompt());
        return CommonResult.success(result);
    }

    // ============ 私有辅助方法 ============

    /**
     * 构建字段树
     */
    private Map<String, Object> buildFieldTree(JsonNode node, String prefix) {
        Map<String, Object> tree = new LinkedHashMap<>();

        if (node.isObject()) {
            node.fieldNames().forEachRemaining(fieldName -> {
                String path = prefix.isEmpty() ? fieldName : prefix + "." + fieldName;
                JsonNode child = node.get(fieldName);
                tree.put(path, buildFieldNode(child, path));
            });
        } else if (node.isArray() && node.size() > 0) {
            tree.put(prefix, buildFieldNode(node.get(0), prefix + "[0]"));
        }

        return tree;
    }

    private Object buildFieldNode(JsonNode node, String path) {
        if (node.isObject()) {
            Map<String, Object> children = new LinkedHashMap<>();
            node.fieldNames().forEachRemaining(fieldName -> {
                String childPath = path + "." + fieldName;
                children.put(childPath, buildFieldNode(node.get(fieldName), childPath));
            });
            return Map.of(
                    "type", getJsonType(node),
                    "children", children
            );
        } else if (node.isArray()) {
            if (node.size() > 0) {
                return Map.of(
                        "type", "array[" + node.size() + "]",
                        "sample", getSampleValue(node.get(0)),
                        "children", buildFieldTree(node.get(0), path + "[0]")
                );
            } else {
                return Map.of("type", "array[0]", "children", Map.of());
            }
        } else {
            return Map.of(
                    "type", getJsonType(node),
                    "value", getSampleValue(node)
            );
        }
    }

    private String getJsonType(JsonNode node) {
        if (node.isTextual()) return "string";
        if (node.isNumber()) return node.isIntegralNumber() ? "integer" : "number";
        if (node.isBoolean()) return "boolean";
        if (node.isNull()) return "null";
        if (node.isArray()) return "array";
        if (node.isObject()) return "object";
        return "unknown";
    }

    private String getSampleValue(JsonNode node) {
        if (node == null || node.isNull()) return "null";
        if (node.isTextual()) {
            String text = node.asText();
            return text.length() > 100 ? text.substring(0, 100) + "..." : text;
        }
        return node.toString();
    }

    private int countFields(JsonNode node) {
        if (node.isObject()) {
            int count = 0;
            for (JsonNode child : node) {
                count += countFields(child);
            }
            return node.size() + count;
        } else if (node.isArray() && node.size() > 0) {
            return countFields(node.get(0));
        }
        return 0;
    }

    private long countNodes(JsonNode node) {
        if (node.isObject()) {
            long count = 1;
            for (JsonNode child : node) {
                count += countNodes(child);
            }
            return count;
        } else if (node.isArray()) {
            long count = 1;
            for (JsonNode item : node) {
                count += countNodes(item);
            }
            return count;
        }
        return 1;
    }

    private Map<String, Object> extractSampleData(JsonNode root, int maxItems) {
        Map<String, Object> samples = new LinkedHashMap<>();
        if (root.isObject()) {
            root.fieldNames().forEachRemaining(fieldName -> {
                JsonNode field = root.get(fieldName);
                if (field.isArray() && field.size() > 0) {
                    List<Object> items = new java.util.ArrayList<>();
                    for (int i = 0; i < Math.min(maxItems, field.size()); i++) {
                        try {
                            items.add(objectMapper.treeToValue(field.get(i), Object.class));
                        } catch (Exception e) {
                            items.add(field.get(i).toString());
                        }
                    }
                    samples.put(fieldName, items);
                } else if (!field.isObject() && !field.isArray()) {
                    samples.put(fieldName, getSampleValue(field));
                }
            });
        }
        return samples;
    }
}
