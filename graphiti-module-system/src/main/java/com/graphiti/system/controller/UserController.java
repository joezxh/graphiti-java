package com.graphiti.system.controller;

import com.graphiti.common.response.CommonResult;
import com.graphiti.system.dal.dataobject.UserDO;
import com.graphiti.system.service.UserService;
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
 * 用户管理控制器
 */
@Tag(name = "管理后台 - 系统用户管理")
@RestController
@RequestMapping("/admin/system/user")
@Validated
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/create")
    @Operation(summary = "创建用户", description = "创建新的系统用户", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Long> createUser(@Valid @RequestBody UserDO userDO) {
        Long userId = userService.createUser(userDO);
        return CommonResult.success(userId);
    }

    @PutMapping("/update")
    @Operation(summary = "更新用户", description = "更新现有用户信息", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Boolean> updateUser(@Valid @RequestBody UserDO userDO) {
        userService.updateUser(userDO);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete/{userId}")
    @Operation(summary = "删除用户", description = "根据用户ID删除用户", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Boolean> deleteUser(
            @PathVariable @Parameter(description = "用户ID", required = true, example = "1") Long userId) {
        userService.deleteUser(userId);
        return CommonResult.success(true);
    }

    @GetMapping("/get/{userId}")
    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户详细信息", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<UserDO> getUser(
            @PathVariable @Parameter(description = "用户ID", required = true, example = "1") Long userId) {
        UserDO user = userService.getUser(userId);
        return CommonResult.success(user);
    }

    @GetMapping("/list")
    @Operation(summary = "获取用户列表", description = "分页获取系统用户列表",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<?> listUsers(
            @RequestParam(defaultValue = "1") @Parameter(description = "页码", example = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页数量", example = "10") Integer pageSize,
            @RequestParam(required = false) @Parameter(description = "用户名（模糊匹配）") String username,
            @RequestParam(required = false) @Parameter(description = "昵称（模糊匹配）") String nickname,
            @RequestParam(required = false) @Parameter(description = "状态") Integer status) {
        return CommonResult.success(userService.listUsers(pageNo, pageSize, username, nickname, status));
    }
}
