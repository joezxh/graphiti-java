package com.graphiti.module.graphiti.vo.graph;

import lombok.Data;
import java.io.Serializable;

/**
 * 图谱删除预览响应 VO
 * 在执行删除前，返回图谱的完整统计信息供用户确认
 */
@Data
public class GraphDeletePreviewRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 图谱基本信息
     */
    private String graphId;
    private String name;
    private String description;

    /**
     * MySQL 元数据统计
     */
    private Integer nodeCount;
    private Integer edgeCount;

    /**
     * Neo4j 详细统计
     */
    private Long entityNodeCount;
    private Long episodeCount;
    private Long relationshipCount;
    private Long communityNodeCount;

    /**
     * 本体定义统计
     */
    private Long ontDefinitionCount;
    private Long ontClassCount;
    private Long ontPropertyCount;
    private Long ontConstraintCount;
    private Long ontMappingCount;
    private Long ontClassInheritanceCount;
    private Long ontVersionHistoryCount;
    private Long ontEntityCategoryCount;
    private Long ontEpisodeTypeCount;
    private Long ontRelationshipMetaCount;
    private Long ontCommunityTypeCount;
    private Long ontDraftCount;

    /**
     * 是否存在待删除数据
     */
    private Boolean hasData;

    /**
     * Neo4j 总数据量
     */
    private Long neo4jDataCount;

    /**
     * 本体定义总数据量
     */
    private Long ontologyDataCount;

    /**
     * 总数据量（用于前端展示警告）
     */
    private Long totalDataCount;
}
