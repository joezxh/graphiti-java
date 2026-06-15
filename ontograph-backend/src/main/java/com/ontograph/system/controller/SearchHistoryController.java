package com.ontograph.system.controller;

import com.ontograph.common.response.CommonResult;
import com.ontograph.system.service.SearchHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * 搜索历史控制器
 */
@Tag(name = "搜索历史管理")
@RestController
@RequestMapping("/api/v1/admin/graphiti/search-history")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @GetMapping("/list")
    @Operation(summary = "获取当前用户搜索历史（分页）")
    public CommonResult<Map<String, Object>> listHistory(
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") Integer pageNo,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页数量") Integer pageSize) {
        return CommonResult.success(searchHistoryService.listHistory(pageNo, pageSize));
    }

    @PostMapping("/save")
    @Operation(summary = "保存搜索记录")
    public CommonResult<Long> saveHistory(
            @RequestParam @Parameter(description = "搜索词") String query,
            @RequestParam(required = false) @Parameter(description = "搜索模式") String mode,
            @RequestParam(defaultValue = "0") @Parameter(description = "结果数量") Integer resultCount) {
        return CommonResult.success(searchHistoryService.saveHistory(query, mode, resultCount));
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清空当前用户搜索历史")
    public CommonResult<Void> clearHistory() {
        searchHistoryService.clearHistory();
        return CommonResult.success();
    }
}
