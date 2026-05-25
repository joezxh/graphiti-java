package com.graphiti.module.graphiti.dal.mysql.metadata;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.metadata.OntCommunityTypeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OntCommunityTypeMapper extends BaseMapper<OntCommunityTypeDO> {

    @Select("SELECT * FROM ont_community_type WHERE definition_id = #{definitionId} AND status = 'ACTIVE' ORDER BY sort_order")
    List<OntCommunityTypeDO> selectActiveByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_community_type WHERE definition_id = #{definitionId} ORDER BY sort_order")
    List<OntCommunityTypeDO> selectByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_community_type WHERE definition_id = #{definitionId} AND category = #{category} AND status = 'ACTIVE' ORDER BY sort_order")
    List<OntCommunityTypeDO> selectByCategory(@Param("definitionId") Long definitionId, @Param("category") String category);

    @Select("SELECT * FROM ont_community_type WHERE definition_id = #{definitionId} AND parent_type_code IS NULL AND status = 'ACTIVE' ORDER BY sort_order")
    List<OntCommunityTypeDO> selectRootTypes(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_community_type WHERE definition_id = #{definitionId} AND parent_type_code = #{parentTypeCode} AND status = 'ACTIVE' ORDER BY sort_order")
    List<OntCommunityTypeDO> selectByParentType(@Param("definitionId") Long definitionId, @Param("parentTypeCode") String parentTypeCode);

    /**
     * 根据 definitionId 和 typeCode 查询社区类型
     * @param definitionId 定义ID
     * @param typeCode 类型代码
     * @return 社区类型对象
     */
    @Select("SELECT * FROM ont_community_type WHERE definition_id = #{definitionId} AND type_code = #{typeCode} LIMIT 1")
    OntCommunityTypeDO findByCode(@Param("definitionId") Long definitionId, @Param("typeCode") String typeCode);

    @Select("SELECT COUNT(*) FROM ont_community_type WHERE definition_id = #{definitionId}")
    long countByDefinitionId(@Param("definitionId") Long definitionId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM ont_community_type WHERE definition_id = #{definitionId}")
    int deleteByDefinitionId(@Param("definitionId") Long definitionId);
}
