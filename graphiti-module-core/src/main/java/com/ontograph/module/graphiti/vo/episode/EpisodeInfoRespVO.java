package com.graphiti.module.graphiti.vo.episode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 事件详情响应 VO
 */
@Data
@Schema(description = "事件（Episode）详情")
public class EpisodeInfoRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "事件 UUID")
    private String uuid;

    @Schema(description = "事件名称")
    private String name;

    @Schema(description = "所属图谱 ID")
    private String groupId;

    @Schema(description = "来源类型")
    private String source;

    @Schema(description = "来源描述")
    private String sourceDescription;

    @Schema(description = "事件内容")
    private String content;

    @Schema(description = "创建时间")
    private String createdAt;

    @Schema(description = "有效时间")
    private String validAt;

    @Schema(description = "失效时间")
    private String invalidAt;

    @Schema(description = "关联实体 UUID 列表")
    private List<String> entityEdges;

    @Schema(description = "元数据")
    private Map<String, Object> metadata;

    @Schema(description = "是否已处理")
    private Boolean processed;

    // ==================== V3.0.0 新增字段 ====================

    @Schema(description = "Episode 类型代码 (V3): EP_TRIAL_1ST, EP_MEDIATION_NEGOTIATION, etc.")
    private String episodeType;

    @Schema(description = "法律程序 (V3): litigation|mediation|arbitration|execution")
    private String legalProcess;

    @Schema(description = "阶段标签 (V3): 立案|庭审|调解|判决|执行")
    private String stageLabel;

    @Schema(description = "审级 (V3): 一审|二审|再审|null")
    private String courtLevel;

    @Schema(description = "是否审判阶段 (V3)")
    private Boolean isTrialStage;

    @Schema(description = "开始时间 (V3), ISO-8601 格式")
    private String startTime;

    @Schema(description = "结束时间 (V3), ISO-8601 格式")
    private String endTime;

    @Schema(description = "关联案件 ID (V3)")
    private String caseId;

    // ==================== V3.1.0 通用化字段（兼容旧字段）====================

    @Schema(description = "通用化流程类型 (V3.1): 替代 legal_process")
    private String processType;

    @Schema(description = "通用化阶段级别 (V3.1): 替代 court_level")
    private String stageLevel;

    @Schema(description = "是否为审核阶段 (V3.1): 替代 is_trial_stage")
    private Boolean isReviewStage;
}
