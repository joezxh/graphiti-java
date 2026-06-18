package com.ontograph.module.system.service;

import com.ontograph.module.system.dal.dataobject.UserDO;

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

    /**
     * 根据用户名获取用户ID
     * @param username 用户名
     * @return 用户ID，不存在返回 null
     */
    Long getUserIdByUsername(String username);

    /**
     * 分页查询用户列表
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @param username 用户名（模糊匹配，可为 null）
     * @param nickname 昵称（模糊匹配，可为 null）
     * @param status 状态（可为 null）
     * @return 分页结果 {list, total}
     */
    java.util.Map<String, Object> listUsers(Integer pageNo, Integer pageSize, String username, String nickname, Integer status);

    /**
     * 重置用户密码
     * @param userId 用户ID
     * @return 新密码（明文，仅此次返回）
     */
    String resetPassword(Long userId);
}
