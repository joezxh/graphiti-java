package com.ontograph.system.service;

import com.ontograph.system.dal.dataobject.NotificationDO;
import java.util.List;

/**
 * 通知服务接口
 */
public interface NotificationService {

    /**
     * 创建通知
     * @param notification 通知
     * @return 通知ID
     */
    Long createNotification(NotificationDO notification);

    /**
     * 批量创建通知
     * @param notifications 通知列表
     */
    void createNotifications(List<NotificationDO> notifications);

    /**
     * 获取通知列表（分页）
     * @param userId 用户ID
     * @param type 通知类型
     * @param isRead 已读状态
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 通知列表
     */
    List<NotificationDO> listNotifications(Long userId, Integer type, Integer isRead, Integer pageNum, Integer pageSize);

    /**
     * 获取通知总数
     * @param userId 用户ID
     * @param type 通知类型
     * @param isRead 已读状态
     * @return 总数
     */
    Long countNotifications(Long userId, Integer type, Integer isRead);

    /**
     * 获取未读通知数量
     * @param userId 用户ID
     * @return 未读数量
     */
    Long countUnread(Long userId);

    /**
     * 标记通知为已读
     * @param id 通知ID
     * @param userId 用户ID
     */
    void markAsRead(Long id, Long userId);

    /**
     * 标记所有通知为已读
     * @param userId 用户ID
     */
    void markAllAsRead(Long userId);

    /**
     * 删除通知
     * @param id 通知ID
     * @param userId 用户ID
     */
    void deleteNotification(Long id, Long userId);

    /**
     * 清空所有通知
     * @param userId 用户ID
     */
    void clearAll(Long userId);
}
