package com.graphiti.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户 DO
 */
@Data
@TableName("sys_user")
public class UserDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String email;

    private String mobile;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Boolean deleted;
}
