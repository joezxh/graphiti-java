package com.graphiti.module.graphiti.controller.admin;

import com.graphiti.common.response.CommonResult;
import com.graphiti.module.graphiti.service.SearchService;
import com.graphiti.module.graphiti.vo.search.GetMemoryReqVO;
import com.graphiti.module.graphiti.vo.search.GetMemoryRespVO;
import com.graphiti.module.graphiti.vo.search.SearchConfigVO;
import com.graphiti.module.graphiti.vo.search.SearchQueryReqVO;
import com.graphiti.module.graphiti.vo.search.SearchResultsRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;

/**
 * 搜索检索控制器
 */
@Tag(name = "搜索检索", description = "知识图谱搜索和记忆检索接口")
@RestController
@RequestMapping("/admin/graphiti/search")
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
