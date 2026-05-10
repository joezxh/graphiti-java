package com.graphiti.system.controller;

import com.graphiti.common.response.CommonResult;
import com.graphiti.system.dal.dataobject.MenuDO;
import com.graphiti.system.service.MenuService;
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
 * 菜单管理控制器
 */
@Tag(name = "管理后台 - 系统菜单管理")
@RestController
@RequestMapping("/admin/system/menu")
@Validated
@Slf4j
public class MenuController {

    @Resource
    private MenuService menuService;

    @PostMapping("/create")
    @Operation(summary = "创建菜单", description = "创建新的系统菜单", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Long> createMenu(@Valid @RequestBody MenuDO menuDO) {
        Long menuId = menuService.createMenu(menuDO);
        return CommonResult.success(menuId);
    }

    @PutMapping("/update")
    @Operation(summary = "更新菜单", description = "更新现有菜单信息", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Boolean> updateMenu(@Valid @RequestBody MenuDO menuDO) {
        menuService.updateMenu(menuDO);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete/{menuId}")
    @Operation(summary = "删除菜单", description = "根据菜单ID删除菜单", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Boolean> deleteMenu(
            @PathVariable @Parameter(description = "菜单ID", required = true, example = "1") Long menuId) {
        menuService.deleteMenu(menuId);
        return CommonResult.success(true);
    }

    @GetMapping("/get/{menuId}")
    @Operation(summary = "获取菜单详情", description = "根据菜单ID获取菜单详细信息", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<MenuDO> getMenu(
            @PathVariable @Parameter(description = "菜单ID", required = true, example = "1") Long menuId) {
        MenuDO menu = menuService.getMenu(menuId);
        return CommonResult.success(menu);
    }

    @GetMapping("/list")
    @Operation(summary = "获取菜单列表", description = "获取所有菜单的树形列表",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<List<MenuDO>> listMenus() {
        List<MenuDO> allMenus = menuService.listMenus();
        return CommonResult.success(buildMenuTree(allMenus, 0L));
    }

    private List<MenuDO> buildMenuTree(List<MenuDO> allMenus, Long parentId) {
        return allMenus.stream()
            .filter(m -> m.getParentId() != null && m.getParentId().equals(parentId))
            .peek(m -> m.setChildren(buildMenuTree(allMenus, m.getId())))
            .toList();
    }
}
