package com.ontograph.module.system.controller;

import com.ontograph.common.response.CommonResult;
import com.ontograph.module.system.dto.LoginRequest;
import com.ontograph.module.system.dto.LoginResponse;
import com.ontograph.module.system.service.AuthService;
import com.ontograph.module.system.dal.dataobject.MenuDO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 认证控制器
 * 提供用户登录、获取用户信息等接口
 */
@Tag(name = "认证管理", description = "用户登录、登出、获取用户信息等接口")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    /**
     * 用户登录
     * @param request LoginRequest
     * @return CommonResult<LoginResponse>
     */
    @Operation(summary = "用户登录", description = "用户通过用户名和密码登录系统，返回JWT令牌")
    @PostMapping("/login")
    public CommonResult<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return CommonResult.success(authService.login(request));
    }
    /**
     * 获取当前登录用户信息
     * @return CommonResult<LoginResponse.UserInfo>
     */
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的详细信息", 
               security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/info")
    public CommonResult<LoginResponse.UserInfo> getUserInfo() {
        return CommonResult.success(authService.getUserInfo());
    }
    /**
     * 用户退出登录
     * @return CommonResult<Void>
     */
    @Operation(summary = "退出登录", description = "用户退出登录，清除会话", 
               security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/logout")
    public CommonResult<Void> logout() {
        authService.logout();
        return CommonResult.success();
    }

    /**
     * 获取当前用户的菜单树
     * 根据用户角色返回有权限访问的菜单
     * @return CommonResult<List<MenuDO>>
     */
    @Operation(summary = "获取用户菜单", description = "获取当前登录用户有权限访问的菜单树",
               security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/menus")
    public CommonResult<List<MenuDO>> getUserMenus() {
        return CommonResult.success(authService.getUserMenus());
    }
}
