package com.graphiti.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统通知 DO
 */
@Data
@TableName("sys_notification")
public class NotificationDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    private Long userId;

    private String title;

    private String content;

    private Integer type;

    private Integer isRead;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Boolean deleted;
}
