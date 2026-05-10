package com.graphiti.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统配置 DO
 */
@Data
@TableName("sys_system_config")
public class SystemConfigDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    private String configKey;

    private String configValue;

    private String configName;

    private String configDescription;

    private Integer configType; // 1-文本 2-数字 3-布尔 4-JSON

    private String groupName;

    private Integer sortNum;

    private Integer status; // 0-禁用 1-启用

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Boolean deleted;
}
