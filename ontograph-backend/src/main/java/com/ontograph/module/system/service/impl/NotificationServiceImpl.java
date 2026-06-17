package com.ontograph.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ontograph.module.system.dal.dataobject.NotificationDO;
import com.ontograph.module.system.dal.mysql.NotificationMapper;
import com.ontograph.module.system.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public Long createNotification(NotificationDO notification) {
        notification.setCreateTime(LocalDateTime.now());
        notification.setUpdateTime(LocalDateTime.now());
        notification.setDeleted(false);
        if (notification.getIsRead() == null) {
            notification.setIsRead(0);
        }
        notificationMapper.insert(notification);
        log.info("创建通知成功: id={}, userId={}, title={}",
                 notification.getId(), notification.getUserId(), notification.getTitle());
        return notification.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNotifications(List<NotificationDO> notifications) {
        for (NotificationDO notification : notifications) {
            createNotification(notification);
        }
    }

    @Override
    public List<NotificationDO> listNotifications(Long userId, Integer type, Integer isRead,
                                                   Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<NotificationDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationDO::getUserId, userId)
               .eq(NotificationDO::getDeleted, false);

        if (type != null) {
            wrapper.eq(NotificationDO::getType, type);
        }
        if (isRead != null) {
            wrapper.eq(NotificationDO::getIsRead, isRead);
        }

        wrapper.orderByDesc(NotificationDO::getCreateTime);

        int offset = (pageNum - 1) * pageSize;
        wrapper.last("LIMIT " + pageSize + " OFFSET " + offset);

        return notificationMapper.selectList(wrapper);
    }

    @Override
    public Long countNotifications(Long userId, Integer type, Integer isRead) {
        LambdaQueryWrapper<NotificationDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationDO::getUserId, userId)
               .eq(NotificationDO::getDeleted, false);

        if (type != null) {
            wrapper.eq(NotificationDO::getType, type);
        }
        if (isRead != null) {
            wrapper.eq(NotificationDO::getIsRead, isRead);
        }

        return notificationMapper.selectCount(wrapper);
    }

    @Override
    public Long countUnread(Long userId) {
        return countNotifications(userId, null, 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long id, Long userId) {
        LambdaUpdateWrapper<NotificationDO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(NotificationDO::getId, id)
               .eq(NotificationDO::getUserId, userId)
               .eq(NotificationDO::getDeleted, false)
               .set(NotificationDO::getIsRead, 1)
               .set(NotificationDO::getUpdateTime, LocalDateTime.now());
        notificationMapper.update(null, wrapper);
        log.info("标记通知已读: id={}, userId={}", id, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<NotificationDO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(NotificationDO::getUserId, userId)
               .eq(NotificationDO::getDeleted, false)
               .eq(NotificationDO::getIsRead, 0)
               .set(NotificationDO::getIsRead, 1)
               .set(NotificationDO::getUpdateTime, LocalDateTime.now());
        notificationMapper.update(null, wrapper);
        log.info("标记全部通知已读: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long id, Long userId) {
        LambdaUpdateWrapper<NotificationDO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(NotificationDO::getId, id)
               .eq(NotificationDO::getUserId, userId)
               .set(NotificationDO::getDeleted, true)
               .set(NotificationDO::getUpdateTime, LocalDateTime.now());
        notificationMapper.update(null, wrapper);
        log.info("删除通知: id={}, userId={}", id, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAll(Long userId) {
        LambdaUpdateWrapper<NotificationDO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(NotificationDO::getUserId, userId)
               .eq(NotificationDO::getDeleted, false)
               .set(NotificationDO::getDeleted, true)
               .set(NotificationDO::getUpdateTime, LocalDateTime.now());
        notificationMapper.update(null, wrapper);
        log.info("清空所有通知: userId={}", userId);
    }
}
