package com.graphiti.module.graphiti.controller.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.common.response.CommonResult;
import com.graphiti.module.graphiti.service.DataExtractService;
import com.graphiti.module.graphiti.vo.extractor.DataExtractReqVO;
import com.graphiti.module.graphiti.vo.extractor.DataExtractResultVO;
import com.graphiti.module.graphiti.vo.extractor.ExtractedEntityVO;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据提取控制器
 * 提供通用的数据提取接口，支持实体和关系的提取
 */
@Tag(name = "数据提取", description = "通用数据提取接口，支持实体和关系的提取")
@RestController
@RequestMapping("/api/v1/graph/extract")
@RequiredArgsConstructor
@Slf4j
public class DataExtractController {

    @Resource
    private DataExtractService dataExtractService;

    private final ObjectMapper objectMapper;

    /**
     * 从文本内容提取实体和关系
     *
     * POST /api/v1/graph/extract/text
     */
    @PostMapping("/text")
    @Operation(summary = "从文本提取实体和关系", description = "从文本内容中提取实体和关系",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<DataExtractResultVO> extractFromText(
            @RequestBody DataExtractReqVO reqVO) {
        log.info("从文本提取数据: graphId={}, contentLength={}", reqVO.getGraphId(),
                reqVO.getContent() != null ? reqVO.getContent().length() : 0);

        DataExtractResultVO result = dataExtractService.extract(reqVO);
        return CommonResult.success(result);
    }

    /**
     * 从 JSON 文件提取实体和关系
     *
     * POST /api/v1/graph/extract/json
     */
    @PostMapping("/json")
    @Operation(summary = "从JSON文件提取实体和关系", description = "从JSON文件中提取实体和关系",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<DataExtractResultVO> extractFromJson(
            @RequestParam("file") MultipartFile file,
            @RequestParam("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestParam(value = "entityTypesConfig", required = false) String entityTypesConfig,
            @RequestParam(value = "edgeTypesConfig", required = false) String edgeTypesConfig,
            @RequestParam(value = "customInstructions", required = false) String customInstructions) {

        log.info("从JSON文件提取数据: graphId={}, fileName={}", graphId, file.getOriginalFilename());

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            DataExtractReqVO reqVO = new DataExtractReqVO();
            reqVO.setGraphId(graphId);
            reqVO.setContent(content);
            reqVO.setSourceType("json");
            reqVO.setSourceDescription(file.getOriginalFilename());
            reqVO.setEntityTypesConfig(entityTypesConfig);
            reqVO.setEdgeTypesConfig(edgeTypesConfig);
            reqVO.setCustomInstructions(customInstructions);

            DataExtractResultVO result = dataExtractService.extract(reqVO);
            return CommonResult.success(result);

        } catch (IOException e) {
            log.error("读取JSON文件失败: graphId={}, error={}", graphId, e.getMessage());
            return CommonResult.error(400, "读取JSON文件失败: " + e.getMessage());
        }
    }

    /**
     * 仅提取实体
     *
     * POST /api/v1/graph/extract/entities
     */
    @PostMapping("/entities")
    @Operation(summary = "仅提取实体", description = "从内容中仅提取实体节点",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<DataExtractResultVO> extractEntities(
            @RequestBody DataExtractReqVO reqVO) {
        log.info("提取实体: graphId={}, contentLength={}", reqVO.getGraphId(),
                reqVO.getContent() != null ? reqVO.getContent().length() : 0);

        reqVO.setEntityOnly(true);
        DataExtractResultVO result = dataExtractService.extractEntities(reqVO);
        return CommonResult.success(result);
    }

    /**
     * 仅提取关系（需要先提供实体列表）
     *
     * POST /api/v1/graph/extract/edges
     */
    @PostMapping("/edges")
    @Operation(summary = "仅提取关系", description = "基于已有实体列表提取关系（需要先调用 /entities 接口）",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<DataExtractResultVO> extractEdges(
            @RequestBody DataExtractReqVO reqVO) {
        log.info("提取关系: graphId={}, entityCount={}", reqVO.getGraphId(),
                reqVO.getExistingEntities() != null ? reqVO.getExistingEntities().size() : 0);

        if (reqVO.getExistingEntities() == null || reqVO.getExistingEntities().isEmpty()) {
            return CommonResult.error(400, "请先提供实体列表（existingEntities）");
        }

        reqVO.setEdgeOnly(true);
        DataExtractResultVO result = dataExtractService.extractEdges(reqVO);
        return CommonResult.success(result);
    }

    /**
     * 预览 JSON 文件结构
     *
     * POST /api/v1/graph/extract/preview
     */
    @PostMapping("/preview")
    @Operation(summary = "预览JSON文件结构", description = "上传JSON文件并预览其结构",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Map<String, Object>> previewJsonFile(
            @RequestParam("file") MultipartFile file) {
        log.info("预览JSON文件: fileName={}, size={}", file.getOriginalFilename(), file.getSize());

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            return CommonResult.success(parseJsonPreview(content, file.getOriginalFilename()));
        } catch (Exception e) {
            log.error("预览JSON文件失败: fileName={}, error={}", file.getOriginalFilename(), e.getMessage());
            return CommonResult.error(400, "JSON文件解析失败: " + e.getMessage());
        }
    }

    /**
     * 获取默认的实体类型配置
     *
     * GET /api/v1/graph/extract/entity-types
     */
    @GetMapping("/entity-types")
    @Operation(summary = "获取默认实体类型配置", description = "获取系统默认的实体类型定义",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Map<String, Object>> getDefaultEntityTypes() {
        Map<String, Object> result = new HashMap<>();
        result.put("types", getDefaultEntityTypesList());
        result.put("format", "entity_type_id | entity_type_name | description");
        return CommonResult.success(result);
    }

    /**
     * 获取默认的关系类型配置
     *
     * GET /api/v1/graph/extract/edge-types
     */
    @GetMapping("/edge-types")
    @Operation(summary = "获取默认关系类型配置", description = "获取系统默认的关系类型定义",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Map<String, Object>> getDefaultEdgeTypes() {
        Map<String, Object> result = new HashMap<>();
        result.put("types", getDefaultEdgeTypesList());
        return CommonResult.success(result);
    }

    // ========== 辅助方法 ==========

    private Map<String, Object> parseJsonPreview(String content, String fileName) {
        Map<String, Object> preview = new HashMap<>();
        preview.put("fileName", fileName);
        preview.put("contentLength", content.length());

        try {
            Map<String, Object> json = objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});
            preview.put("fieldCount", countFields(json));
            preview.put("sampleData", getSampleData(json));
        } catch (Exception e) {
            preview.put("parseError", e.getMessage());
        }

        // 截取前2000字符作为预览
        preview.put("contentPreview", content.substring(0, Math.min(2000, content.length())));

        return preview;
    }

    private int countFields(Map<String, Object> map) {
        int count = map.size();
        for (Object value : map.values()) {
            if (value instanceof Map) {
                count += countFields((Map<String, Object>) value);
            } else if (value instanceof List && !((List<?>) value).isEmpty()) {
                Object first = ((List<?>) value).get(0);
                if (first instanceof Map) {
                    count += countFields((Map<String, Object>) first);
                }
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getSampleData(Map<String, Object> json) {
        Map<String, Object> samples = new HashMap<>();
        for (Map.Entry<String, Object> entry : json.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                String str = (String) value;
                samples.put(entry.getKey(), str.length() > 100 ? str.substring(0, 100) + "..." : str);
            } else if (value instanceof Number || value instanceof Boolean) {
                samples.put(entry.getKey(), value);
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                if (!list.isEmpty()) {
                    Object first = list.get(0);
                    if (first instanceof Map) {
                        samples.put(entry.getKey() + "[]", "Array of " + ((Map<String, Object>) first).size() + " fields");
                    } else {
                        samples.put(entry.getKey() + "[]", list.size() + " items");
                    }
                }
            }
        }
        return samples;
    }

    private List<Map<String, String>> getDefaultEntityTypesList() {
        return List.of(
                Map.of("id", "1", "name", "Person", "description", "人物"),
                Map.of("id", "2", "name", "Organization", "description", "组织机构"),
                Map.of("id", "3", "name", "Location", "description", "地点"),
                Map.of("id", "4", "name", "Event", "description", "事件"),
                Map.of("id", "5", "name", "Concept", "description", "概念"),
                Map.of("id", "6", "name", "Product", "description", "产品"),
                Map.of("id", "7", "name", "Document", "description", "文档"),
                Map.of("id", "0", "name", "Entity", "description", "通用实体（默认）")
        );
    }

    private List<Map<String, String>> getDefaultEdgeTypesList() {
        return List.of(
                Map.of("type", "WORKS_AT", "description", "工作于"),
                Map.of("type", "LIVES_IN", "description", "居住在"),
                Map.of("type", "KNOWS", "description", "认识"),
                Map.of("type", "PARTICIPATES_IN", "description", "参与"),
                Map.of("type", "OWNS", "description", "拥有"),
                Map.of("type", "LOCATED_IN", "description", "位于"),
                Map.of("type", "RELATED_TO", "description", "相关于"),
                Map.of("type", "CREATED_BY", "description", "由...创建"),
                Map.of("type", "BELONGS_TO", "description", "属于"),
                Map.of("type", "MARRIED_TO", "description", "与...结婚"),
                Map.of("type", "PARENT_OF", "description", "是...的父母"),
                Map.of("type", "FRIEND_OF", "description", "与...是朋友")
        );
    }
}
