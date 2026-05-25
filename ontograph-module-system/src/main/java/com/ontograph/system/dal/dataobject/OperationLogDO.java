package com.ontograph.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统操作日志 DO
 */
@Data
@TableName("sys_operation_log")
public class OperationLogDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    private Long userId;

    private String username;

    private String operation;

    private String method;

    private String params;

    private String ip;

    private String location;

    private Integer status; // 0-失败 1-成功

    private String errorMsg;

    private Integer duration; // 毫秒

    private LocalDateTime createTime;
}
