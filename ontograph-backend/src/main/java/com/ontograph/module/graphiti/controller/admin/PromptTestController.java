package com.ontograph.module.graphiti.controller.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontograph.common.response.CommonResult;
import com.ontograph.module.graphiti.service.DataExtractService;
import com.ontograph.module.graphiti.service.PromptTemplateService;
import com.ontograph.module.graphiti.vo.extractor.DataExtractReqVO;
import com.ontograph.module.graphiti.vo.extractor.DataExtractResultVO;
import com.ontograph.module.graphiti.vo.extractor.ExtractedEntityVO;
import com.ontograph.module.graphiti.vo.prompt.GenerateSampleDataReqVO;
import com.ontograph.module.graphiti.vo.prompt.GenerateSampleDataRespVO;
import com.ontograph.module.graphiti.vo.prompt.PromptTestReqVO;
import com.ontograph.module.graphiti.vo.prompt.PromptTestRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 提示词测试控制器
 * 提供提示词测试和生成范例数据的功能
 */
@Tag(name = "提示词测试", description = "提示词模板测试和范例数据生成")
@RestController
@RequestMapping("/api/v1/prompt/test")
@RequiredArgsConstructor
@Slf4j
public class PromptTestController {

    @Resource
    private PromptTemplateService promptTemplateService;

    @Resource
    private DataExtractService dataExtractService;

    private final ObjectMapper objectMapper;

    /**
     * 测试提示词模板
     *
     * POST /api/v1/prompt/test/execute
     */
    @PostMapping("/execute")
    @Operation(summary = "测试提示词模板", description = "使用指定内容测试提示词模板的提取效果",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<PromptTestRespVO> testPrompt(
            @RequestBody PromptTestReqVO reqVO) {
        log.info("测试提示词: templateId={}, contentLength={}",
                reqVO.getTemplateId(), reqVO.getInputContent() != null ? reqVO.getInputContent().length() : 0);

        PromptTestRespVO resp = new PromptTestRespVO();
        long startTime = System.currentTimeMillis();

        try {
            // 解析变量
            Map<String, Object> variables = parseVariables(reqVO);

            // 获取渲染后的提示词
            PromptTemplateService.RenderedPrompt rendered;
            try {
                Long templateId = Long.parseLong(reqVO.getTemplateId());
                rendered = promptTemplateService.getRenderedPrompt(templateId, variables);
            } catch (NumberFormatException e) {
                // 尝试按编码获取
                rendered = promptTemplateService.renderPrompt(reqVO.getTemplateId(), variables);
            }

            resp.setSuccess(true);
            resp.setRawResponse(rendered.systemPrompt() + "\n\n---\n\n" + rendered.userPrompt());

        } catch (Exception e) {
            log.error("测试提示词失败: templateId={}, error={}", reqVO.getTemplateId(), e.getMessage(), e);
            resp.setSuccess(false);
            resp.setErrorMessage(e.getMessage());
        }

        resp.setElapsedMs(System.currentTimeMillis() - startTime);
        resp.setResponseTime(LocalDateTime.now());

        return CommonResult.success(resp);
    }

    /**
     * 测试提示词并提取数据
     *
     * POST /api/v1/prompt/test/extract
     */
    @PostMapping("/extract")
    @Operation(summary = "测试提取效果", description = "使用提示词模板对内容进行实体和关系提取",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<PromptTestRespVO> testExtract(
            @RequestBody PromptTestReqVO reqVO) {
        log.info("测试提取效果: templateId={}, contentLength={}",
                reqVO.getTemplateId(), reqVO.getInputContent() != null ? reqVO.getInputContent().length() : 0);

        PromptTestRespVO resp = new PromptTestRespVO();
        long startTime = System.currentTimeMillis();

        try {
            // 构建提取请求
            DataExtractReqVO extractReq = new DataExtractReqVO();
            extractReq.setGraphId("test-graph");
            extractReq.setContent(reqVO.getInputContent());
            extractReq.setSourceType(reqVO.getSourceType() != null ? reqVO.getSourceType() : "text");
            extractReq.setPromptTemplateId(parseTemplateId(reqVO.getTemplateId()));

            // 解析额外变量
            if (reqVO.getCustomVariables() != null && !reqVO.getCustomVariables().isBlank()) {
                Map<String, String> extraVars = objectMapper.readValue(reqVO.getCustomVariables(),
                        new TypeReference<Map<String, String>>() {});
                extractReq.setExtraVariables(extraVars);
            }

            // 执行提取
            DataExtractResultVO result = dataExtractService.extract(extractReq);

            // 构建响应
            resp.setSuccess(true);
            resp.setEntityCount(result.getEntityCount());
            resp.setEdgeCount(result.getEdgeCount());

            // 序列化提取结果
            try {
                Map<String, Object> parsedData = new LinkedHashMap<>();
                parsedData.put("entities", result.getEntities());
                parsedData.put("edges", result.getEdges());
                parsedData.put("statistics", Map.of(
                        "entityTypes", result.getEntityTypeStatistics(),
                        "edgeTypes", result.getEdgeTypeStatistics()
                ));
                resp.setParsedData(objectMapper.writeValueAsString(parsedData));
            } catch (Exception e) {
                log.warn("序列化提取结果失败", e);
            }

            if (result.getErrors() != null && !result.getErrors().isEmpty()) {
                resp.setErrorMessage(String.join("; ", result.getErrors()));
            }

        } catch (Exception e) {
            log.error("测试提取失败: templateId={}, error={}", reqVO.getTemplateId(), e.getMessage(), e);
            resp.setSuccess(false);
            resp.setErrorMessage(e.getMessage());
        }

        resp.setElapsedMs(System.currentTimeMillis() - startTime);
        resp.setResponseTime(LocalDateTime.now());

        return CommonResult.success(resp);
    }

    /**
     * 生成范例数据
     *
     * POST /api/v1/prompt/test/generate-sample
     */
    @PostMapping("/generate-sample")
    @Operation(summary = "生成范例数据", description = "使用AI生成符合提示词格式的测试数据",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<GenerateSampleDataRespVO> generateSampleData(
            @RequestBody GenerateSampleDataReqVO reqVO) {
        log.info("生成范例数据: templateId={}, dataType={}", reqVO.getTemplateId(), reqVO.getDataType());

        GenerateSampleDataRespVO resp = new GenerateSampleDataRespVO();
        resp.setSamples(new ArrayList<>());

        try {
            // 获取模板信息
            Long templateId = parseTemplateId(reqVO.getTemplateId());
            var templateOpt = promptTemplateService.getTemplate(templateId);
            if (templateOpt.isEmpty()) {
                throw new IllegalArgumentException("模板不存在: " + reqVO.getTemplateId());
            }
            var template = templateOpt.get();

            // 构建生成提示词
            String generatePrompt = buildSampleDataPrompt(template.getName(), template.getType(),
                    reqVO.getDataType(), reqVO.getCount(), reqVO.getScenario(), reqVO.getFormat(),
                    reqVO.getAdditionalInstructions());

            // 调用 LLM 生成数据
            String generatedText = callLLMForSampleData(generatePrompt);

            // 解析生成的数据
            List<GenerateSampleDataRespVO.SampleData> samples = parseGeneratedSamples(
                    generatedText, reqVO.getFormat(), reqVO.getDataType());

            resp.setSuccess(true);
            resp.setSamples(samples);

        } catch (Exception e) {
            log.error("生成范例数据失败: templateId={}, error={}", reqVO.getTemplateId(), e.getMessage(), e);
            resp.setSuccess(false);
            resp.setErrorMessage(e.getMessage());
        }

        return CommonResult.success(resp);
    }

    // ========== 辅助方法 ==========

    private Map<String, Object> parseVariables(PromptTestReqVO reqVO) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("episode_content", reqVO.getInputContent());

        if (reqVO.getContextContent() != null && !reqVO.getContextContent().isBlank()) {
            variables.put("context_content", reqVO.getContextContent());
        }

        if (reqVO.getCustomVariables() != null && !reqVO.getCustomVariables().isBlank()) {
            try {
                Map<String, Object> customVars = objectMapper.readValue(reqVO.getCustomVariables(),
                        new TypeReference<Map<String, Object>>() {});
                variables.putAll(customVars);
            } catch (Exception e) {
                log.warn("解析自定义变量失败: {}", e.getMessage());
            }
        }

        return variables;
    }

    private Long parseTemplateId(String templateId) {
        if (templateId == null || templateId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(templateId);
        } catch (NumberFormatException e) {
            // 如果是编码，返回 null 交给 service 处理
            return null;
        }
    }

    private String buildSampleDataPrompt(String templateName, String templateType,
                                        String dataType, Integer count, String scenario,
                                        String format, String additionalInstructions) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个测试数据生成专家。请为以下提示词模板生成测试数据。\n\n");

        prompt.append("模板信息：\n");
        prompt.append("- 模板名称：").append(templateName).append("\n");
        prompt.append("- 模板类型：").append(templateType).append("\n\n");

        prompt.append("数据要求：\n");
        prompt.append("- 数据类型：").append(dataType).append("\n");
        prompt.append("- 生成数量：").append(count != null ? count : 3).append("\n");

        if (scenario != null && !scenario.isBlank()) {
            prompt.append("- 具体场景：").append(scenario).append("\n");
        }

        if (format != null && !format.isBlank()) {
            prompt.append("- 格式要求：").append(format.equals("xml") ? "XML格式" : "JSON格式").append("\n");
        } else {
            prompt.append("- 格式要求：JSON格式\n");
        }

        if (additionalInstructions != null && !additionalInstructions.isBlank()) {
            prompt.append("- 额外说明：").append(additionalInstructions).append("\n");
        }

        prompt.append("\n请生成真实、可用的测试数据，数据内容要符合中国的").append(dataType).append("领域特点。");
        prompt.append("\n\n返回格式：\n");
        prompt.append("```json\n");
        prompt.append("[\n");
        prompt.append("  {\"content\": \"测试数据内容1\", \"type\": \"").append(dataType).append("\"},");
        prompt.append("  {\"content\": \"测试数据内容2\", \"type\": \"").append(dataType).append("\"}");
        prompt.append("]\n");
        prompt.append("```");

        return prompt.toString();
    }

    private String callLLMForSampleData(String prompt) {
        // 这里应该调用实际的 LLM 服务
        // 暂时使用简化实现
        return "{\n  \"samples\": [\n    {\"content\": \"示例数据1\", \"type\": \"legal\"},\n    {\"content\": \"示例数据2\", \"type\": \"legal\"}\n  ]\n}";
    }

    @SuppressWarnings("unchecked")
    private List<GenerateSampleDataRespVO.SampleData> parseGeneratedSamples(String generatedText,
                                                                           String format,
                                                                           String dataType) {
        List<GenerateSampleDataRespVO.SampleData> samples = new ArrayList<>();

        try {
            // 提取 JSON 内容
            String jsonStr = extractJsonFromText(generatedText);
            Map<String, Object> result = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});

            Object samplesObj = result.get("samples");
            if (samplesObj instanceof List) {
                List<Map<String, Object>> samplesList = (List<Map<String, Object>>) samplesObj;
                int index = 0;
                for (Map<String, Object> sampleMap : samplesList) {
                    GenerateSampleDataRespVO.SampleData sample = new GenerateSampleDataRespVO.SampleData();
                    sample.setIndex(index++);
                    sample.setContent((String) sampleMap.get("content"));
                    sample.setType((String) sampleMap.getOrDefault("type", dataType));
                    sample.setDomain((String) sampleMap.get("domain"));
                    sample.setMetadata(objectMapper.writeValueAsString(sampleMap.get("metadata")));
                    samples.add(sample);
                }
            }
        } catch (Exception e) {
            log.warn("解析生成样本失败: {}", e.getMessage());
        }

        return samples;
    }

    private String extractJsonFromText(String text) {
        // 尝试找到 JSON 代码块
        int jsonStart = text.indexOf("```json");
        if (jsonStart != -1) {
            int start = jsonStart + 7;
            int end = text.lastIndexOf("```");
            if (end > start) {
                return text.substring(start, end).trim();
            }
        }

        // 尝试找到普通 JSON 数组
        int bracketStart = text.indexOf("[");
        int bracketEnd = text.lastIndexOf("]");
        if (bracketStart != -1 && bracketEnd != -1 && bracketEnd > bracketStart) {
            return text.substring(bracketStart, bracketEnd + 1);
        }

        // 尝试找到普通 JSON 对象
        int braceStart = text.indexOf("{");
        int braceEnd = text.lastIndexOf("}");
        if (braceStart != -1 && braceEnd != -1 && braceEnd > braceStart) {
            return text.substring(braceStart, braceEnd + 1);
        }

        return "{}";
    }
}
