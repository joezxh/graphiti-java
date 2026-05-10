package com.graphiti.system.service;

import com.graphiti.system.dal.dataobject.NotificationSettingsDO;

/**
 * 用户通知设置服务接口
 */
public interface NotificationSettingsService {

    /**
     * 获取用户的通知设置
     * @param userId 用户ID
     * @return 通知设置，不存在则返回默认设置
     */
    NotificationSettingsDO getSettings(Long userId);

    /**
     * 保存/更新用户的通知设置
     * @param settings 通知设置
     */
    void saveSettings(NotificationSettingsDO settings);
}
