package com.graphiti.module.graphiti.controller.admin;

import com.graphiti.common.response.CommonResult;
import com.graphiti.module.graphiti.dal.dataobject.PromptTemplateDO;
import com.graphiti.module.graphiti.dal.dataobject.PromptVersionDO;
import com.graphiti.module.graphiti.service.PromptTemplateService;
import com.graphiti.module.graphiti.vo.prompt.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 提示词模板管理控制器
 */
@Tag(name = "提示词管理", description = "提示词模板的增删改查管理")
@RestController
@RequestMapping("/api/v1/prompt")
@RequiredArgsConstructor
@Slf4j
public class PromptController {

    @Resource
    private PromptTemplateService promptTemplateService;

    // ========== 模板管理 ==========

    /**
     * 创建提示词模板
     *
     * POST /api/v1/prompt/templates
     */
    @PostMapping("/templates")
    @Operation(summary = "创建提示词模板", description = "创建新的提示词模板",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<PromptTemplateVO> createTemplate(
            @Valid @RequestBody CreatePromptTemplateReqVO reqVO) {
        log.info("创建提示词模板: code={}", reqVO.getCode());
        PromptTemplateVO result = promptTemplateService.createTemplate(reqVO);
        return CommonResult.success(result);
    }

    /**
     * 更新提示词模板
     *
     * PUT /api/v1/prompt/templates/{id}
     */
    @PutMapping("/templates/{id}")
    @Operation(summary = "更新提示词模板", description = "更新指定的提示词模板",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<PromptTemplateVO> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody CreatePromptTemplateReqVO reqVO) {
        log.info("更新提示词模板: id={}", id);
        reqVO.setId(id);
        PromptTemplateVO result = promptTemplateService.updateTemplate(reqVO);
        return CommonResult.success(result);
    }

    /**
     * 删除提示词模板
     *
     * DELETE /api/v1/prompt/templates/{id}
     */
    @DeleteMapping("/templates/{id}")
    @Operation(summary = "删除提示词模板", description = "删除指定的提示词模板",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Void> deleteTemplate(@PathVariable Long id) {
        log.info("删除提示词模板: id={}", id);
        promptTemplateService.deleteTemplate(id);
        return CommonResult.success();
    }

    /**
     * 获取提示词模板详情
     *
     * GET /api/v1/prompt/templates/{id}
     */
    @GetMapping("/templates/{id}")
    @Operation(summary = "获取提示词模板详情", description = "获取指定提示词模板的详细信息",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<PromptTemplateVO> getTemplate(@PathVariable Long id) {
        Optional<PromptTemplateVO> result = promptTemplateService.getTemplate(id);
        return result.map(CommonResult::success)
                .orElse(CommonResult.error(404, "模板不存在"));
    }

    /**
     * 根据编码获取提示词模板
     *
     * GET /api/v1/prompt/templates/code/{code}
     */
    @GetMapping("/templates/code/{code}")
    @Operation(summary = "根据编码获取提示词模板", description = "根据模板编码获取提示词模板",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<PromptTemplateVO> getTemplateByCode(@PathVariable String code) {
        Optional<PromptTemplateVO> result = promptTemplateService.getTemplateByCode(code);
        return result.map(CommonResult::success)
                .orElse(CommonResult.error(404, "模板不存在"));
    }

    /**
     * 获取所有提示词模板
     *
     * GET /api/v1/prompt/templates
     */
    @GetMapping("/templates")
    @Operation(summary = "获取所有提示词模板", description = "获取所有启用的提示词模板",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<List<PromptTemplateVO>> listAllTemplates() {
        List<PromptTemplateVO> result = promptTemplateService.listAllTemplates();
        return CommonResult.success(result);
    }

    /**
     * 根据类型获取提示词模板列表
     *
     * GET /api/v1/prompt/templates/type/{type}
     */
    @GetMapping("/templates/type/{type}")
    @Operation(summary = "根据类型获取提示词模板", description = "根据模板类型获取提示词模板列表",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<List<PromptTemplateVO>> listTemplatesByType(@PathVariable String type) {
        List<PromptTemplateVO> result = promptTemplateService.listTemplatesByType(type);
        return CommonResult.success(result);
    }

    /**
     * 启用/禁用提示词模板
     *
     * PUT /api/v1/prompt/templates/{id}/toggle
     */
    @PutMapping("/templates/{id}/toggle")
    @Operation(summary = "启用/禁用模板", description = "切换提示词模板的启用状态",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Void> toggleTemplate(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        log.info("切换模板状态: id={}, enabled={}", id, enabled);
        promptTemplateService.toggleTemplate(id, enabled);
        return CommonResult.success();
    }

    // ========== 版本管理 ==========

    /**
     * 创建新版本
     *
     * POST /api/v1/prompt/templates/{id}/versions
     */
    @PostMapping("/templates/{id}/versions")
    @Operation(summary = "创建新版本", description = "为提示词模板创建新版本",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Void> createVersion(
            @PathVariable Long id,
            @RequestParam(required = false) String description) {
        log.info("创建提示词模板版本: templateId={}", id);
        promptTemplateService.createVersion(id, description != null ? description : "新版本");
        return CommonResult.success();
    }

    /**
     * 获取版本历史
     *
     * GET /api/v1/prompt/templates/{id}/versions
     */
    @GetMapping("/templates/{id}/versions")
    @Operation(summary = "获取版本历史", description = "获取提示词模板的版本历史",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<List<PromptVersionDO>> getVersionHistory(@PathVariable Long id) {
        List<PromptVersionDO> versions = promptTemplateService.getVersionHistory(id);
        return CommonResult.success(versions);
    }

    /**
     * 回滚到指定版本
     *
     * POST /api/v1/prompt/templates/{id}/rollback
     */
    @PostMapping("/templates/{id}/rollback")
    @Operation(summary = "回滚到指定版本", description = "将提示词模板回滚到指定版本",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Void> rollbackToVersion(
            @PathVariable Long id,
            @RequestParam Integer version) {
        log.info("回滚提示词模板: templateId={}, version={}", id, version);
        promptTemplateService.rollbackToVersion(id, version);
        return CommonResult.success();
    }

    // ========== 提示词渲染测试 ==========

    /**
     * 渲染提示词（预览）
     *
     * POST /api/v1/prompt/templates/{id}/render
     */
    @PostMapping("/templates/{id}/render")
    @Operation(summary = "渲染提示词", description = "使用指定变量渲染提示词模板（用于预览）",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Map<String, String>> renderPrompt(
            @PathVariable Long id,
            @RequestBody Map<String, Object> variables) {
        PromptTemplateService.RenderedPrompt rendered = promptTemplateService.getRenderedPrompt(id, variables);
        Map<String, String> result = Map.of(
                "systemPrompt", rendered.systemPrompt(),
                "userPrompt", rendered.userPrompt(),
                "responseFormat", rendered.responseFormat() != null ? rendered.responseFormat() : ""
        );
        return CommonResult.success(result);
    }

    /**
     * 获取模板类型列表
     *
     * GET /api/v1/prompt/types
     */
    @GetMapping("/types")
    @Operation(summary = "获取模板类型列表", description = "获取所有可用的模板类型",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<List<Map<String, String>>> getTemplateTypes() {
        List<Map<String, String>> types = List.of(
                Map.of("value", "entity_extract", "label", "实体抽取"),
                Map.of("value", "edge_extract", "label", "关系抽取"),
                Map.of("value", "dedupe", "label", "去重处理"),
                Map.of("value", "summary", "label", "摘要生成"),
                Map.of("value", "classify", "label", "分类"),
                Map.of("value", "attribute", "label", "属性提取")
        );
        return CommonResult.success(types);
    }
}
