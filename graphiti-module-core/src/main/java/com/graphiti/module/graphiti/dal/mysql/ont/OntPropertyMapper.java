package com.graphiti.module.graphiti.dal.mysql.ont;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntPropertyDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Optional;

@Mapper
public interface OntPropertyMapper extends BaseMapper<OntPropertyDO> {

    @Select("SELECT * FROM ont_property WHERE definition_id = #{definitionId} ORDER BY local_name")
    List<OntPropertyDO> selectByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_property WHERE definition_id = #{definitionId} AND domain_class_id = #{classId}")
    List<OntPropertyDO> selectByClassId(@Param("definitionId") Long definitionId, @Param("classId") Long classId);

    @Select("SELECT * FROM ont_property WHERE definition_id = #{definitionId} AND property_uri = #{propertyUri} LIMIT 1")
    Optional<OntPropertyDO> selectByUri(@Param("definitionId") Long definitionId, @Param("propertyUri") String propertyUri);
}
