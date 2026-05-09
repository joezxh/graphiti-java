package com.graphiti.module.graphiti.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 本体定义 DO
 * 对应表：graphiti_ontology
 */
@Data
@TableName("graphiti_ontology")
public class OntologyDO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 图谱ID
     */
    @TableField("graph_id")
    private String graphId;
    /**
     * 实体类型定义（JSON 数组）
     */
    private String entities;
    /**
     * 关系类型定义（JSON 数组）
     */
    private String edges;
    /**
     * 是否默认本体
     */
    @TableField("is_default")
    private Boolean isDefault;
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    /**
     * 删除标志
     */
    @TableLogic
    private Boolean deleted;
}
