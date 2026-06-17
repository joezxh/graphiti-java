package com.ontograph.module.system.service;

import com.ontograph.module.system.dal.dataobject.RoleDO;

/**
 * 角色管理服务接口
 */
public interface RoleService {

    /**
     * 创建角色
     * @param roleDO 角色信息
     * @return 角色ID
     */
    Long createRole(RoleDO roleDO);

    /**
     * 更新角色
     * @param roleDO 角色信息
     */
    void updateRole(RoleDO roleDO);

    /**
     * 删除角色
     * @param roleId 角色ID
     */
    void deleteRole(Long roleId);

    /**
     * 获取角色详情
     * @param roleId 角色ID
     * @return 角色信息
     */
    RoleDO getRole(Long roleId);

    /**
     * 根据角色编码获取角色
     * @param code 角色编码
     * @return 角色信息
     */
    RoleDO getRoleByCode(String code);

    /**
     * 获取所有角色列表（不分页）
     * @return 角色列表
     */
    java.util.List<RoleDO> listRoles();
}
