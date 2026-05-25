package com.graphiti.module.graphiti.dal.mysql.metadata;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.metadata.OntCommunityTypeDO;
import org.apache.ibatis.annotations.*;

/**
 * 社区元数据专用 Mapper
 * 提供 ont_community_type 表的社区实例级操作：
 * upsert（创建/更新元数据）、按 typeCode 查询/更新、软删除等。
 */
@Mapper
public interface CommunityMetadataMapper extends BaseMapper<OntCommunityTypeDO> {

    /**
     * 根据 definitionId + typeCode 查找一条记录
     */
    @Select("SELECT * FROM ont_community_type WHERE definition_id = #{definitionId} AND type_code = #{typeCode} AND status = 'ACTIVE' LIMIT 1")
    OntCommunityTypeDO findByCode(@Param("definitionId") Long definitionId, @Param("typeCode") String typeCode);

    /**
     * 按 definitionId + typeCode 更新元数据
     */
    @Update("UPDATE ont_community_type " +
            "SET type_name = #{d.typeName}, region = #{d.region}, scenario_type = #{d.scenarioType}, " +
            "    community_uuid = #{d.communityUuid}, graph_id = #{d.graphId}, " +
            "    description = #{d.description}, metadata = #{d.metadata}, " +
            "    updated_at = NOW() " +
            "WHERE definition_id = #{d.definitionId} AND type_code = #{d.typeCode} AND status = 'ACTIVE'")
    int updateByCode(@Param("d") OntCommunityTypeDO d);

    /**
     * 软删除（status → INACTIVE）
     */
    @Update("UPDATE ont_community_type SET status = 'INACTIVE', updated_at = NOW() " +
            "WHERE definition_id = #{definitionId} AND type_code = #{typeCode}")
    int softDeleteByCode(@Param("definitionId") Long definitionId, @Param("typeCode") String typeCode);

    /**
     * 查询某 definitionId 下所有涉及 communityUuid 的活跃类型
     */
    @Select("SELECT * FROM ont_community_type " +
            "WHERE definition_id = #{definitionId} AND community_uuid IS NOT NULL AND status = 'ACTIVE'")
    java.util.List<OntCommunityTypeDO> selectByDefinitionIdWithCommunity(@Param("definitionId") Long definitionId);
}
