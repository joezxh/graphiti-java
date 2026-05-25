package com.ontograph.module.graphiti.controller.admin;

import com.ontograph.common.response.CommonResult;
import com.ontograph.module.graphiti.service.BusinessInfoService;
import com.ontograph.module.graphiti.service.OntologyDraftService;
import com.ontograph.module.graphiti.service.OntologyMetadataService;
import com.ontograph.module.graphiti.vo.OntologyGraphVO;
import com.ontograph.module.graphiti.vo.business.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 业务信息管理控制器
 * 提供本体定义生成、描述优化、数据模拟生成、元数据查看等 API
 */
@Tag(name = "业务信息管理", description = "业务信息本体定义生成、描述优化、数据模拟生成")
@RestController
@RequestMapping("/api/v1/business-info")
@RequiredArgsConstructor
public class BusinessInfoController {

    private final BusinessInfoService businessInfoService;
    private final OntologyDraftService ontologyDraftService;
    private final OntologyMetadataService ontologyMetadataService;

    // ==================== Feature 1: 本体定义生成 ====================

    @Operation(summary = "生成本体定义", description = "根据用户输入的业务信息，使用 LLM 生成符合项目规范的本体定义",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}/generate")
    public CommonResult<GenerateOntologyRespVO> generateOntology(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestBody @Valid GenerateOntologyReqVO reqVO) {
        return CommonResult.success(businessInfoService.generateOntology(graphId, reqVO));
    }

    @Operation(summary = "保存草稿", description = "将业务信息保存为草稿",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}/drafts")
    public CommonResult<Void> saveDraft(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestBody @Valid GenerateOntologyReqVO reqVO) {
        businessInfoService.saveAsDraft(graphId, reqVO);
        return CommonResult.success(null);
    }

    // ==================== Feature 2: 描述优化 ====================

    @Operation(summary = "优化描述", description = "使用 LLM 优化业务描述的准确性、完整性和规范性",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/optimize")
    public CommonResult<OptimizeDescRespVO> optimizeDescription(
            @RequestBody @Valid OptimizeDescReqVO reqVO) {
        return CommonResult.success(businessInfoService.optimizeDescription(reqVO));
    }

    @Operation(summary = "批量优化描述", description = "批量使用 LLM 优化多条业务描述",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/optimize/batch")
    public CommonResult<OptimizeDescRespVO> optimizeBatch(
            @RequestBody @Valid OptimizeDescReqVO reqVO) {
        return CommonResult.success(businessInfoService.optimizeBatch(reqVO));
    }

    // ==================== Feature 3: 数据模拟生成 ====================

    @Operation(summary = "生成模拟数据", description = "基于图谱本体生成符合业务逻辑的模拟数据",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}/mock-data")
    public CommonResult<GenerateDataRespVO> generateMockData(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestBody @Valid GenerateDataReqVO reqVO) {
        return CommonResult.success(businessInfoService.generateMockData(graphId, reqVO));
    }

    @Operation(summary = "基于草稿生成模拟数据", description = "基于指定草稿中的本体定义生成模拟数据",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}/mock-data/from-draft/{draftId}")
    public CommonResult<GenerateDataRespVO> generateMockDataFromDraft(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "草稿ID") Long draftId,
            @RequestBody @Valid GenerateDataReqVO reqVO) {
        return CommonResult.success(businessInfoService.generateFromDraft(graphId, draftId, reqVO));
    }

    // ==================== 草稿管理 ====================

    @Operation(summary = "列出草稿", description = "获取图谱下所有本体草稿",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/drafts")
    public CommonResult<List<OntDraftVO>> listDrafts(
            @PathVariable @Parameter(description = "图谱ID") String graphId) {
        return CommonResult.success(ontologyDraftService.listDrafts(graphId));
    }

    @Operation(summary = "获取草稿内容", description = "获取草稿的本体定义内容",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/drafts/{draftId}")
    public CommonResult<GenerateOntologyRespVO> getDraftContent(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "草稿ID") Long draftId) {
        return CommonResult.success(ontologyDraftService.getDraftContent(draftId));
    }

    @Operation(summary = "应用草稿", description = "将草稿中的本体定义应用到图谱",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}/drafts/{draftId}/apply")
    public CommonResult<Void> applyDraft(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "草稿ID") Long draftId) {
        ontologyDraftService.applyDraft(graphId, draftId);
        return CommonResult.success(null);
    }

    @Operation(summary = "审核通过草稿", description = "将草稿状态改为 APPROVED",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/drafts/{draftId}/approve")
    public CommonResult<Void> approveDraft(
            @PathVariable @Parameter(description = "草稿ID") Long draftId) {
        ontologyDraftService.approveDraft(draftId);
        return CommonResult.success(null);
    }

    @Operation(summary = "审核拒绝草稿", description = "将草稿状态改为 REJECTED",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/drafts/{draftId}/reject")
    public CommonResult<Void> rejectDraft(
            @PathVariable @Parameter(description = "草稿ID") Long draftId) {
        ontologyDraftService.rejectDraft(draftId);
        return CommonResult.success(null);
    }

    @Operation(summary = "删除草稿", description = "删除指定的本体草稿",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @DeleteMapping("/{graphId}/drafts/{draftId}")
    public CommonResult<Void> deleteDraft(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "草稿ID") Long draftId) {
        ontologyDraftService.deleteDraft(graphId, draftId);
        return CommonResult.success(null);
    }

    // ==================== 元数据查看 ====================

    @Operation(summary = "获取本体元数据图", description = "获取本体定义的图可视化数据",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/metadata/graph")
    public CommonResult<OntologyGraphVO> getOntologyGraph(
            @PathVariable @Parameter(description = "图谱ID") String graphId) {
        return CommonResult.success(ontologyMetadataService.getOntologyGraph(graphId));
    }

    @Operation(summary = "获取模拟数据图", description = "获取模拟数据的图可视化数据",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/mock-graph/{draftId}")
    public CommonResult<OntologyGraphVO> getMockDataGraph(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "草稿ID") Long draftId) {
        return CommonResult.success(ontologyMetadataService.getMockDataGraph(graphId, draftId));
    }

    @Operation(summary = "获取图统计", description = "获取本体图的统计信息",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/stats")
    public CommonResult<Map<String, Object>> getGraphStats(
            @PathVariable @Parameter(description = "图谱ID") String graphId) {
        return CommonResult.success(ontologyMetadataService.getGraphStats(graphId));
    }

    @Operation(summary = "获取模拟数据统计", description = "获取模拟数据图的统计信息",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/mock-stats/{draftId}")
    public CommonResult<Map<String, Object>> getMockDataStats(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "草稿ID") Long draftId) {
        return CommonResult.success(ontologyMetadataService.getMockDataStats(graphId, draftId));
    }
}
