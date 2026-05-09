package com.graphiti.module.graphiti.controller.admin;

import com.graphiti.common.response.CommonResult;
import com.graphiti.module.graphiti.service.SearchService;
import com.graphiti.module.graphiti.vo.search.GetMemoryReqVO;
import com.graphiti.module.graphiti.vo.search.GetMemoryRespVO;
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
}
