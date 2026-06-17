package com.ontograph.module.system.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ontograph.module.system.dal.dataobject.SearchHistoryDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 搜索历史 Mapper
 */
@Mapper
public interface SearchHistoryMapper extends BaseMapper<SearchHistoryDO> {
}
