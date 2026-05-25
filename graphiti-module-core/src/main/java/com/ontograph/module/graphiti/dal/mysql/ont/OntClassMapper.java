package com.graphiti.module.graphiti.dal.mysql.ont;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntClassDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OntClassMapper extends BaseMapper<OntClassDO> {

    @Select("SELECT * FROM ont_class WHERE definition_id = #{definitionId} ORDER BY local_name")
    List<OntClassDO> selectByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_class WHERE definition_id = #{definitionId} AND parent_class_id IS NULL")
    List<OntClassDO> selectRootClasses(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_class WHERE definition_id = #{definitionId} AND parent_class_id = #{parentId}")
    List<OntClassDO> selectByParentId(@Param("definitionId") Long definitionId, @Param("parentId") Long parentId);

    @Select("SELECT COUNT(*) FROM ont_class WHERE definition_id = #{definitionId}")
    long countByDefinitionId(@Param("definitionId") Long definitionId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM ont_class WHERE definition_id = #{definitionId}")
    int deleteByDefinitionId(@Param("definitionId") Long definitionId);
}
