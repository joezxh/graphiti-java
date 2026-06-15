package com.ontograph.module.graphiti.controller.admin;

import com.ontograph.common.response.CommonResult;
import com.ontograph.common.exception.BusinessException;
import com.ontograph.module.graphiti.service.SearchService;
import com.ontograph.module.graphiti.vo.search.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 搜索检索控制器
 */
@Tag(name = "搜索检索", description = "知识图谱搜索和记忆检索接口")
@RestController
@RequestMapping("/api/v1/graph/search")
@Validated
@Slf4j
public class SearchController {

    @Resource
    private SearchService searchService;

    @PostMapping("/global")
    @Operation(summary = "全局搜索", description = "在多个图谱中进行全局搜索", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<SearchResultsRespVO> search(@Valid @RequestBody SearchQueryReqVO reqVO) {
        return CommonResult.success(searchService.search(reqVO));
    }

    @PostMapping("/graph/{graphId}")
    @Operation(summary = "图谱搜索", description = "在指定图谱中进行搜索", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<SearchResultsRespVO> searchGraph(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @Valid @RequestBody SearchQueryReqVO reqVO) {
        return CommonResult.success(searchService.searchGraph(graphId, reqVO));
    }

    @PostMapping("/memory")
    @Operation(summary = "获取记忆", description = "基于对话历史重建上下文获取记忆", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<GetMemoryRespVO> getMemory(@Valid @RequestBody GetMemoryReqVO reqVO) {
        return CommonResult.success(searchService.getMemory(reqVO));
    }

    // ==================== retrieve 命名空间对齐 ====================

    @PostMapping("/retrieve/search")
    @Operation(summary = "检索搜索", description = "独立检索入口，返回事实列表",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<SearchResultsRespVO> retrieveSearch(@Valid @RequestBody SearchQueryReqVO reqVO) {
        return CommonResult.success(searchService.search(reqVO));
    }

    @GetMapping("/retrieve/entity-edge/{uuid}")
    @Operation(summary = "获取指定边的事实", description = "检索指定边，返回 fact 格式",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<FactResultVO> getEntityEdge(
            @PathVariable("uuid") @Parameter(description = "边UUID", required = true) String uuid) {
        FactResultVO result = searchService.getEntityEdge(uuid);
        if (result == null) {
            throw new com.ontograph.common.exception.BusinessException(404, "边不存在");
        }
        return CommonResult.success(result);
    }

    @GetMapping("/retrieve/episodes/{graphId}")
    @Operation(summary = "获取最近的 Episode", description = "获取指定图谱最近的 N 个 Episode",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<List<Map<String, Object>>> getRecentEpisodes(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestParam(value = "last_n", defaultValue = "10") @Parameter(description = "返回数量") int lastN) {
        return CommonResult.success(searchService.getRecentEpisodes(graphId, lastN));
    }

    // ==================== 便捷检索接口 ====================

    @PostMapping("/hybrid/{graphId}")
    @Operation(summary = "混合检索", description = "执行语义+全文+图遍历的混合检索",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<SearchResultsRespVO> hybridSearch(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestParam("query") @Parameter(description = "搜索关键词") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        SearchQueryReqVO reqVO = new SearchQueryReqVO();
        reqVO.setQuery(query);
        reqVO.setMaxFacts(limit);
        SearchConfigVO config = new SearchConfigVO();
        config.setMode("hybrid");
        reqVO.setConfig(config);
        return CommonResult.success(searchService.searchGraph(graphId, reqVO));
    }

    @PostMapping("/semantic/{graphId}")
    @Operation(summary = "语义搜索", description = "基于向量相似度的语义搜索",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<SearchResultsRespVO> semanticSearch(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestParam("query") @Parameter(description = "搜索关键词") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        SearchQueryReqVO reqVO = new SearchQueryReqVO();
        reqVO.setQuery(query);
        reqVO.setMaxFacts(limit);
        SearchConfigVO config = new SearchConfigVO();
        config.setMode("vector");
        reqVO.setConfig(config);
        return CommonResult.success(searchService.searchGraph(graphId, reqVO));
    }

    @PostMapping("/bfs/{graphId}")
    @Operation(summary = "BFS搜索", description = "从指定节点开始BFS图遍历搜索",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<SearchResultsRespVO> bfsSearch(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestParam("query") @Parameter(description = "搜索关键词（用于找到种子节点）") String query,
            @RequestParam(value = "depth", defaultValue = "2") int depth,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        SearchQueryReqVO reqVO = new SearchQueryReqVO();
        reqVO.setQuery(query);
        reqVO.setMaxFacts(limit);
        SearchConfigVO config = new SearchConfigVO();
        config.setMode("bfs");
        config.setBfsDepth(depth);
        reqVO.setConfig(config);
        return CommonResult.success(searchService.searchGraph(graphId, reqVO));
    }
}
