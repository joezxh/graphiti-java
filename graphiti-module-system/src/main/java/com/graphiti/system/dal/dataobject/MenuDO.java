package com.graphiti.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统菜单 DO
 */
@Data
@TableName("sys_menu")
public class MenuDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    private String name;

    private String permission;

    private String url;

    private Long parentId;

    private Integer sort;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Boolean deleted;

    @TableField(exist = false)
    @JsonIgnore
    private List<MenuDO> children;
}
