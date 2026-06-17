package com.ontograph.module.system.controller;

import com.ontograph.common.response.CommonResult;
import com.ontograph.framework.security.util.UserContext;
import com.ontograph.module.system.dal.dataobject.NotificationDO;
import com.ontograph.module.system.dal.dataobject.NotificationSettingsDO;
import com.ontograph.module.system.service.NotificationService;
import com.ontograph.module.system.service.NotificationSettingsService;
import com.ontograph.module.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知管理控制器
 */
@Tag(name = "通知管理", description = "用户通知的查询、管理和设置接口")
@RestController
@RequestMapping("/api/v1/notifications")
@Slf4j
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    @Resource
    private NotificationSettingsService notificationSettingsService;

    @Resource
    private UserContext userContext;

    @Resource
    private UserService userService;

    private Long getCurrentUserId() {
        String username = userContext.getCurrentUsername();
        Long userId = userService.getUserIdByUsername(username);
        if (userId == null) {
            throw new com.ontograph.common.exception.BusinessException(401, "用户不存在");
        }
        return userId;
    }

    @GetMapping("/list")
    @Operation(summary = "获取通知列表", description = "分页获取当前用户的通知列表，支持按类型和已读状态筛选",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Map<String, Object>> listNotifications(
            @RequestParam(required = false) @Parameter(description = "通知类型: 1-系统通知 2-图谱通知 3-检索通知") Integer type,
            @RequestParam(required = false) @Parameter(description = "已读状态: 0-未读 1-已读") Integer isRead,
            @RequestParam(defaultValue = "1") @Parameter(description = "页码", example = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页数量", example = "10") Integer pageSize) {
        Long userId = getCurrentUserId();
        List<NotificationDO> list = notificationService.listNotifications(userId, type, isRead, pageNum, pageSize);
        Long total = notificationService.countNotifications(userId, type, isRead);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return CommonResult.success(result);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读通知数量", description = "获取当前用户的未读通知数量",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Map<String, Long>> getUnreadCount() {
        Long userId = getCurrentUserId();
        Long count = notificationService.countUnread(userId);
        Map<String, Long> result = new HashMap<>();
        result.put("count", count);
        return CommonResult.success(result);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记通知为已读", description = "将指定通知标记为已读",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Void> markAsRead(
            @PathVariable @Parameter(description = "通知ID", required = true) Long id) {
        Long userId = getCurrentUserId();
        notificationService.markAsRead(id, userId);
        return CommonResult.success();
    }

    @PutMapping("/read-all")
    @Operation(summary = "标记所有通知为已读", description = "将当前用户的所有通知标记为已读",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Void> markAllAsRead() {
        Long userId = getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return CommonResult.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知", description = "删除指定的单条通知",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Void> deleteNotification(
            @PathVariable @Parameter(description = "通知ID", required = true) Long id) {
        Long userId = getCurrentUserId();
        notificationService.deleteNotification(id, userId);
        return CommonResult.success();
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清空所有通知", description = "清空当前用户的所有通知",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Void> clearAll() {
        Long userId = getCurrentUserId();
        notificationService.clearAll(userId);
        return CommonResult.success();
    }

    @GetMapping("/settings")
    @Operation(summary = "获取通知设置", description = "获取当前用户的通知偏好设置",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<NotificationSettingsDO> getSettings() {
        Long userId = getCurrentUserId();
        NotificationSettingsDO settings = notificationSettingsService.getSettings(userId);
        return CommonResult.success(settings);
    }

    @PutMapping("/settings")
    @Operation(summary = "保存通知设置", description = "保存当前用户的通知偏好设置",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Void> saveSettings(@RequestBody NotificationSettingsDO settings) {
        Long userId = getCurrentUserId();
        settings.setUserId(userId);
        notificationSettingsService.saveSettings(settings);
        return CommonResult.success();
    }
}
