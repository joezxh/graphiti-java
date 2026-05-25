package com.graphiti.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户通知设置 DO
 */
@Data
@TableName("sys_user_notification_settings")
public class NotificationSettingsDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    private Long userId;

    private Integer systemEnabled;

    private Integer graphEnabled;

    private Integer searchEnabled;

    private Integer emailEnabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Boolean deleted;
}
