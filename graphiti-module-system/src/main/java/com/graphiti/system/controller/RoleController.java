package com.graphiti.system.controller;

import com.graphiti.common.response.CommonResult;
import com.graphiti.system.dal.dataobject.RoleDO;
import com.graphiti.system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 角色管理控制器
 */
@Tag(name = "管理后台 - 系统角色管理")
@RestController
@RequestMapping("/admin/system/role")
@Validated
@Slf4j
public class RoleController {

    @Resource
    private RoleService roleService;

    @PostMapping("/create")
    @Operation(summary = "创建角色", description = "创建新的系统角色", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Long> createRole(@Valid @RequestBody RoleDO roleDO) {
        Long roleId = roleService.createRole(roleDO);
        return CommonResult.success(roleId);
    }

    @PutMapping("/update")
    @Operation(summary = "更新角色", description = "更新现有角色信息", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Boolean> updateRole(@Valid @RequestBody RoleDO roleDO) {
        roleService.updateRole(roleDO);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete/{roleId}")
    @Operation(summary = "删除角色", description = "根据角色ID删除角色", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Boolean> deleteRole(
            @PathVariable @Parameter(description = "角色ID", required = true, example = "1") Long roleId) {
        roleService.deleteRole(roleId);
        return CommonResult.success(true);
    }

    @GetMapping("/get/{roleId}")
    @Operation(summary = "获取角色详情", description = "根据角色ID获取角色详细信息", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<RoleDO> getRole(
            @PathVariable @Parameter(description = "角色ID", required = true, example = "1") Long roleId) {
        RoleDO role = roleService.getRole(roleId);
        return CommonResult.success(role);
    }

    @GetMapping("/list")
    @Operation(summary = "获取角色列表", description = "获取所有角色列表",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<List<RoleDO>> listRoles() {
        return CommonResult.success(roleService.listRoles());
    }
}
