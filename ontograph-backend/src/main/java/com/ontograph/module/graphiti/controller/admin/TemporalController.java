package com.ontograph.module.graphiti.controller.admin;

import com.ontograph.common.response.CommonResult;
import com.ontograph.module.graphiti.service.TemporalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 时序管理控制器
 */
@Tag(name = "时序管理", description = "管理实体和关系的时间有效性")
@RestController
@RequestMapping("/api/v1/graph/{graphId}/temporal")
@RequiredArgsConstructor
public class TemporalController {

    private final TemporalService temporalService;

    @Operation(summary = "获取当前有效的事实（节点）")
    @GetMapping("/facts/current")
    public CommonResult<List<Map<String, Object>>> getCurrentFacts(@PathVariable String graphId) {
        List<Map<String, Object>> facts = temporalService.getCurrentFacts(graphId);
        return CommonResult.success(facts);
    }

    @Operation(summary = "获取指定时间点的有效事实")
    @GetMapping("/facts/at/{referenceTime}")
    public CommonResult<List<Map<String, Object>>> getFactsAtTime(
            @PathVariable String graphId,
            @PathVariable long referenceTime) {
        List<Map<String, Object>> facts = temporalService.getFactsAtTime(graphId, referenceTime);
        return CommonResult.success(facts);
    }

    @Operation(summary = "获取指定时间点的有效关系")
    @GetMapping("/relationships/at/{referenceTime}")
    public CommonResult<List<Map<String, Object>>> getRelationshipsAtTime(
            @PathVariable String graphId,
            @PathVariable long referenceTime) {
        List<Map<String, Object>> edges = temporalService.getRelationshipsAtTime(graphId, referenceTime);
        return CommonResult.success(edges);
    }

    @Operation(summary = "获取实体的历史版本")
    @GetMapping("/history/{entityName}")
    public CommonResult<List<Map<String, Object>>> getFactHistory(
            @PathVariable String graphId,
            @PathVariable String entityName) {
        List<Map<String, Object>> history = temporalService.getFactHistory(graphId, entityName);
        return CommonResult.success(history);
    }

    @Operation(summary = "批量失效事实")
    @PostMapping("/facts/invalidate")
    public CommonResult<Void> invalidateFacts(
            @PathVariable String graphId,
            @RequestBody List<String> entityNames) {
        temporalService.invalidateFacts(graphId, entityNames);
        return CommonResult.success(null);
    }
}
