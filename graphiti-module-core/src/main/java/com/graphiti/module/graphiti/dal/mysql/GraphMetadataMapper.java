package com.graphiti.module.graphiti.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.GraphMetadataDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 图谱元数据 Mapper
 * 对应表：graphiti_graph_metadata
 */
@Mapper
public interface GraphMetadataMapper extends BaseMapper<GraphMetadataDO> {
    // 可以在这里定义自定义的 SQL 查询方法
}
