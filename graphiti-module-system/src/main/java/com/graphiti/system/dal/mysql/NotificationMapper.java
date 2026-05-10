package com.graphiti.system.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.system.dal.dataobject.NotificationDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知 Mapper
 */
@Mapper
public interface NotificationMapper extends BaseMapper<NotificationDO> {
}
