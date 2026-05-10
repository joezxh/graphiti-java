package com.graphiti.system.controller;

import com.graphiti.common.response.CommonResult;
import com.graphiti.system.dal.dataobject.OperationLogDO;
import com.graphiti.system.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 操作日志控制器
 */
@Tag(name = "管理后台 - 操作日志")
@RestController
@RequestMapping("/admin/system/log")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping("/list")
    @Operation(summary = "分页查询操作日志")
    public CommonResult<Map<String, Object>> listLogs(
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") Integer pageNo,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页数量") Integer pageSize,
            @RequestParam(required = false) @Parameter(description = "用户名") String username,
            @RequestParam(required = false) @Parameter(description = "操作名称") String operation,
            @RequestParam(required = false) @Parameter(description = "状态") Integer status,
            @RequestParam(required = false) @Parameter(description = "开始时间") String startTime,
            @RequestParam(required = false) @Parameter(description = "结束时间") String endTime) {
        return CommonResult.success(
            operationLogService.listLogs(pageNo, pageSize, username, operation, status, startTime, endTime));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取日志详情")
    public CommonResult<OperationLogDO> getLog(@PathVariable @Parameter(description = "日志ID") Long id) {
        return CommonResult.success(operationLogService.getLog(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除单条日志")
    public CommonResult<Void> deleteLog(@PathVariable @Parameter(description = "日志ID") Long id) {
        operationLogService.deleteLog(id);
        return CommonResult.success();
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清空所有日志")
    public CommonResult<Void> clearLogs() {
        operationLogService.clearLogs();
        return CommonResult.success();
    }

    @GetMapping("/export")
    @Operation(summary = "导出日志")
    public CommonResult<List<OperationLogDO>> exportLogs(
            @RequestParam(required = false) @Parameter(description = "用户名") String username,
            @RequestParam(required = false) @Parameter(description = "操作名称") String operation,
            @RequestParam(required = false) @Parameter(description = "状态") Integer status,
            @RequestParam(required = false) @Parameter(description = "开始时间") String startTime,
            @RequestParam(required = false) @Parameter(description = "结束时间") String endTime) {
        return CommonResult.success(
            operationLogService.exportLogs(username, operation, status, startTime, endTime));
    }
}
