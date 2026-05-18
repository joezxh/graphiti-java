package com.graphiti.module.graphiti.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 图谱元数据 DO
 * 对应表：graphiti_graph_metadata
 */
@Data
@TableName("graphiti_graph_metadata")
public class GraphMetadataDO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 图谱ID（UUID）
     */
    @TableField("graph_id")
    private String graphId;
    /**
     * 图谱名称
     */
    private String name;
    /**
     * 图谱描述
     */
    private String description;
    /**
     * 节点数量
     */
    @TableField("node_count")
    private Integer nodeCount;
    /**
     * 边数量
     */
    @TableField("edge_count")
    private Integer edgeCount;
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
    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;

    /**
     * 图谱状态
     */
    private String status;
}
