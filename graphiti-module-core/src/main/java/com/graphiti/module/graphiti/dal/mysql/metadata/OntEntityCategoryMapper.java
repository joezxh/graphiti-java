package com.graphiti.module.graphiti.dal.mysql.metadata;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.metadata.OntEntityCategoryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OntEntityCategoryMapper extends BaseMapper<OntEntityCategoryDO> {

    @Select("SELECT * FROM ont_entity_category WHERE definition_id = #{definitionId} AND status = 'ACTIVE' ORDER BY category_level, sort_order")
    List<OntEntityCategoryDO> selectActiveByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_entity_category WHERE definition_id = #{definitionId} ORDER BY category_level, sort_order")
    List<OntEntityCategoryDO> selectByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_entity_category WHERE definition_id = #{definitionId} AND category_level = #{level} AND status = 'ACTIVE' ORDER BY sort_order")
    List<OntEntityCategoryDO> selectByLevel(@Param("definitionId") Long definitionId, @Param("level") Integer level);

    @Select("SELECT * FROM ont_entity_category WHERE definition_id = #{definitionId} AND parent_category_code IS NULL AND status = 'ACTIVE' ORDER BY sort_order")
    List<OntEntityCategoryDO> selectRootCategories(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_entity_category WHERE definition_id = #{definitionId} AND parent_category_code = #{parentCode} AND status = 'ACTIVE' ORDER BY sort_order")
    List<OntEntityCategoryDO> selectByParentCode(@Param("definitionId") Long definitionId, @Param("parentCode") String parentCode);
}
