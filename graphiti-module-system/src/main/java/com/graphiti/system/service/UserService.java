package com.graphiti.system.service;

import com.graphiti.system.dal.dataobject.UserDO;

/**
 * 用户管理服务接口
 */
public interface UserService {

    /**
     * 创建用户
     * @param userDO 用户信息
     * @return 用户ID
     */
    Long createUser(UserDO userDO);

    /**
     * 更新用户
     * @param userDO 用户信息
     */
    void updateUser(UserDO userDO);

    /**
     * 删除用户
     * @param userId 用户ID
     */
    void deleteUser(Long userId);

    /**
     * 获取用户详情
     * @param userId 用户ID
     * @return 用户信息
     */
    UserDO getUser(Long userId);

    /**
     * 根据用户名获取用户
     * @param username 用户名
     * @return 用户信息
     */
    UserDO getUserByUsername(String username);
}
