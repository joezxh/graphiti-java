package com.graphiti.module.graphiti.dal.mysql.ont;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntDomainRuleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OntDomainRuleMapper extends BaseMapper<OntDomainRuleDO> {
    @Select("SELECT * FROM ont_domain_rule WHERE definition_id = #{defId} AND enabled = true")
    List<OntDomainRuleDO> selectEnabledByDefinitionId(@Param("defId") Long defId);
}
