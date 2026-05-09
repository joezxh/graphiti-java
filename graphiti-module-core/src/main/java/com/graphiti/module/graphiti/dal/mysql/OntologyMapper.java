package com.graphiti.module.graphiti.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.OntologyDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 本体定义 Mapper
 * 对应表：graphiti_ontology
 */
@Mapper
public interface OntologyMapper extends BaseMapper<OntologyDO> {
    // 可以在这里定义自定义的 SQL 查询方法
}
