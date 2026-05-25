package com.graphiti.module.graphiti.dal.dataobject.metadata;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 剧集类型维度表
 * 定义法律过程中事件的分类体系，兼容诉讼审级嵌套和 ADR 扁平化结构
 */
@Data
@TableName("ont_episode_type")
public class OntEpisodeTypeDO implements Serializable {
    private static final long serialVersionUID = 2L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("definition_id")
    private Long definitionId;

    @TableField("type_code")
    private String typeCode;

    @TableField("type_name")
    private String typeName;

    @TableField("type_name_en")
    private String typeNameEn;

    // ========== 层级关系（V5 新增）==========
    @TableField("parent_type_code")
    private String parentTypeCode;

    @TableField("level")
    private Integer level;

    @TableField("stage_label")
    private String stageLabel;

    // ========== 通用化字段（Phase 4 新增）==========
    /**
     * 业务流程类型：business_process|workflow|lifecycle
     */
    @TableField("process_type")
    private String processType;

    /**
     * 阶段级别（通用，可配置。法律领域：一审/二审/再审；其他领域：可自定义）
     */
    @TableField("stage_level")
    private String stageLevel;

    /**
     * 是否审查/评议阶段
     */
    @TableField("is_review_stage")
    private Boolean isReviewStage;

    // ========== 向后兼容旧字段（Phase 3 迁移完成后删除）==========
    /**
     * [向后兼容] 旧字段，对应 processType
     */
    @TableField("legal_process")
    private String legalProcess;

    /**
     * [向后兼容] 旧字段，对应 stageLevel
     */
    @TableField("court_level")
    private String courtLevel;

    /**
     * [向后兼容] 旧字段，对应 isReviewStage
     */
    @TableField("is_trial_stage")
    private Boolean isTrialStage;

    private String description;

    @TableField("sort_order")
    private Integer sortOrder;

    private String metadata;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("created_by")
    private String createdBy;

    @TableField("updated_by")
    private String updatedBy;

    @Version
    @TableField("version")
    private Integer version;
}
