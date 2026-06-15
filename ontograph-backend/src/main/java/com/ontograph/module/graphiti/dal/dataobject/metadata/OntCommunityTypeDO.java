package com.ontograph.module.graphiti.dal.dataobject.metadata;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 社区类型维度表
 * 定义知识图谱中社区的分类体系，支持多领域通用分类（领域、区域、场景三个正交维度）。
 *
 * <p>分类维度（category 字段）：
 * <ul>
 *   <li>domain：领域分类，如 DOMAIN_LEGAL、DOMAIN_FINANCE、DOMAIN_SOCIAL_GOV</li>
 *   <li>region：区域/管辖区，如 REGION_CN、REGION_US、REGION_ROOT</li>
 *   <li>scenario：应用场景，如 SCENARIO_JUDICIAL、SCENARIO_COMPLIANCE、SCENARIO_RISK</li>
 * </ul>
 *
 * <p>communityUuid / graphId 字段用于实例级关联：一个 typeCode 对应多个 Community 节点实例。
 */
@Data
@TableName("ont_community_type")
public class OntCommunityTypeDO implements Serializable {
    private static final long serialVersionUID = 1L;

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

    /**
     * 分类维度：domain(领域)|region(区域)|scenario(场景)
     */
    private String category;

    private String description;

    @TableField("parent_type_code")
    private String parentTypeCode;

    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 区域/管辖区代码（对应 Neo4j Community 节点的 region 属性）
     * 典型值：REGION_CN | REGION_US | REGION_EU | REGION_ROOT
     */
    @TableField("region")
    private String region;

    /**
     * 应用场景类型（对应 Neo4j Community 节点的 scenario_type 属性）
     * 典型值：SCENARIO_JUDICIAL | SCENARIO_COMPLIANCE | SCENARIO_RISK | SCENARIO_ROOT
     */
    @TableField("scenario_type")
    private String scenarioType;

    /**
     * 关联的图数据库社区节点 uuid（实例级关联键）
     * 当一个 typeCode 对应多个 Community 实例时，此字段记录最后一次关联的实例
     */
    @TableField("community_uuid")
    private String communityUuid;

    /**
     * 图谱 ID（用于多图谱场景下元数据隔离）
     */
    @TableField("graph_id")
    private String graphId;

    private String metadata;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
