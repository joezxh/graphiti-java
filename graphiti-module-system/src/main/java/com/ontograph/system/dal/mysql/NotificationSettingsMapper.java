package com.ontograph.system.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ontograph.system.dal.dataobject.NotificationSettingsDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户通知设置 Mapper
 */
@Mapper
public interface NotificationSettingsMapper extends BaseMapper<NotificationSettingsDO> {
}
