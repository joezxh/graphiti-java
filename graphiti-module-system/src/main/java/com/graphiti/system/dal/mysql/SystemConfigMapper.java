package com.graphiti.system.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.system.dal.dataobject.SystemConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统配置 Mapper
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfigDO> {
}
