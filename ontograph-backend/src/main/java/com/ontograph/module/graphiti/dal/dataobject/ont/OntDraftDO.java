package com.ontograph.module.graphiti.dal.dataobject.ont;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 本体草稿数据对象
 * 存储 LLM 生成的本体定义草稿和模拟数据
 */
@Data
@TableName("ont_draft")
public class OntDraftDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("graph_id")
    private String graphId;

    @TableField("draft_name")
    private String draftName;

    @TableField("draft_type")
    private String draftType;  // DRAFT | OPTIMIZED | GENERATED

    @TableField("source_info")
    private String sourceInfo;  // JSON: 原始业务信息

    @TableField("generated_info")
    private String generatedInfo;  // JSON: LLM 生成的本体定义

    @TableField("mock_data")
    private String mockData;  // JSON: 生成的模拟数据（节点+边）

    private String status;  // PENDING | APPROVED | REJECTED | APPLIED

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private String createdBy;
}
