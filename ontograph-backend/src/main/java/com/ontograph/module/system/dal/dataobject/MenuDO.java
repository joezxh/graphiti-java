package com.ontograph.module.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
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

    private String component;

    private String icon;

    private Long parentId;

    private Integer sort;

    /**
     * 菜单类型：1-目录 2-菜单 3-按钮
     */
    private Integer type;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;

    @TableField(exist = false)
    private List<MenuDO> children;
}
