package com.graphiti.system.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.system.dal.dataobject.UserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {
}
