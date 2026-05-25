package com.ontograph.system.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ontograph.system.dal.dataobject.RoleDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统角色 Mapper
 */
@Mapper
public interface RoleMapper extends BaseMapper<RoleDO> {
}
