package com.ontograph.system.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ontograph.system.dal.dataobject.SearchHistoryDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 搜索历史 Mapper
 */
@Mapper
public interface SearchHistoryMapper extends BaseMapper<SearchHistoryDO> {
}
