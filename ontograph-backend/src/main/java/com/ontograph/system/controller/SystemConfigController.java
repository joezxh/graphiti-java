package com.ontograph.system.controller;

import com.ontograph.common.response.CommonResult;
import com.ontograph.system.dal.dataobject.SystemConfigDO;
import com.ontograph.system.service.SystemConfigService;
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
 * 系统配置控制器
 */
@Tag(name = "管理后台 - 系统配置")
@RestController
@RequestMapping("/api/v1/admin/system/config")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping("/list")
    @Operation(summary = "分页查询系统配置")
    public CommonResult<Map<String, Object>> listConfigs(
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") Integer pageNo,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页数量") Integer pageSize,
            @RequestParam(required = false) @Parameter(description = "配置键") String configKey,
            @RequestParam(required = false) @Parameter(description = "配置名称") String configName,
            @RequestParam(required = false) @Parameter(description = "分组") String groupName,
            @RequestParam(required = false) @Parameter(description = "状态") Integer status) {
        return CommonResult.success(
            systemConfigService.listConfigs(pageNo, pageSize, configKey, configName, groupName, status));
    }

    @GetMapping("/all")
    @Operation(summary = "获取所有配置（全量）")
    public CommonResult<List<SystemConfigDO>> listAllConfigs() {
        return CommonResult.success(systemConfigService.listAllConfigs());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取配置详情")
    public CommonResult<SystemConfigDO> getConfig(@PathVariable @Parameter(description = "配置ID") Long id) {
        return CommonResult.success(systemConfigService.getConfig(id));
    }

    @GetMapping("/key/{key}")
    @Operation(summary = "根据key获取配置")
    public CommonResult<SystemConfigDO> getConfigByKey(
            @PathVariable @Parameter(description = "配置键") String key) {
        return CommonResult.success(systemConfigService.getConfigByKey(key));
    }

    @PostMapping("/create")
    @Operation(summary = "创建系统配置")
    public CommonResult<Long> createConfig(@Valid @RequestBody SystemConfigDO configDO) {
        return CommonResult.success(systemConfigService.createConfig(configDO));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新系统配置")
    public CommonResult<Void> updateConfig(
            @PathVariable @Parameter(description = "配置ID") Long id,
            @RequestBody SystemConfigDO configDO) {
        configDO.setId(id);
        systemConfigService.updateConfig(configDO);
        return CommonResult.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除系统配置")
    public CommonResult<Void> deleteConfig(@PathVariable @Parameter(description = "配置ID") Long id) {
        systemConfigService.deleteConfig(id);
        return CommonResult.success();
    }
}
