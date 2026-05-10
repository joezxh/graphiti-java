package com.graphiti.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.graphiti.system.dal.dataobject.NotificationSettingsDO;
import com.graphiti.system.dal.mysql.NotificationSettingsMapper;
import com.graphiti.system.service.NotificationSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/**
 * 用户通知设置服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSettingsServiceImpl implements NotificationSettingsService {

    private final NotificationSettingsMapper notificationSettingsMapper;

    @Override
    public NotificationSettingsDO getSettings(Long userId) {
        LambdaQueryWrapper<NotificationSettingsDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationSettingsDO::getUserId, userId)
               .eq(NotificationSettingsDO::getDeleted, false);
        NotificationSettingsDO settings = notificationSettingsMapper.selectOne(wrapper);

        if (settings == null) {
            settings = getDefaultSettings(userId);
        }
        return settings;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSettings(NotificationSettingsDO settings) {
        LambdaQueryWrapper<NotificationSettingsDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationSettingsDO::getUserId, settings.getUserId())
               .eq(NotificationSettingsDO::getDeleted, false);
        NotificationSettingsDO existing = notificationSettingsMapper.selectOne(wrapper);

        if (existing == null) {
            settings.setCreateTime(LocalDateTime.now());
            settings.setUpdateTime(LocalDateTime.now());
            settings.setDeleted(false);
            notificationSettingsMapper.insert(settings);
            log.info("新建用户通知设置: userId={}", settings.getUserId());
        } else {
            LambdaUpdateWrapper<NotificationSettingsDO> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(NotificationSettingsDO::getId, existing.getId())
                         .set(settings.getSystemEnabled() != null,
                              NotificationSettingsDO::getSystemEnabled, settings.getSystemEnabled())
                         .set(settings.getGraphEnabled() != null,
                              NotificationSettingsDO::getGraphEnabled, settings.getGraphEnabled())
                         .set(settings.getSearchEnabled() != null,
                              NotificationSettingsDO::getSearchEnabled, settings.getSearchEnabled())
                         .set(settings.getEmailEnabled() != null,
                              NotificationSettingsDO::getEmailEnabled, settings.getEmailEnabled())
                         .set(NotificationSettingsDO::getUpdateTime, LocalDateTime.now());
            notificationSettingsMapper.update(null, updateWrapper);
            log.info("更新用户通知设置: userId={}", settings.getUserId());
        }
    }

    private NotificationSettingsDO getDefaultSettings(Long userId) {
        NotificationSettingsDO settings = new NotificationSettingsDO();
        settings.setUserId(userId);
        settings.setSystemEnabled(1);
        settings.setGraphEnabled(1);
        settings.setSearchEnabled(1);
        settings.setEmailEnabled(0);
        return settings;
    }
}
