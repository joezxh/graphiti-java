package com.ontograph.module.system.service;

import com.ontograph.module.system.dto.LoginRequest;
import com.ontograph.module.system.dto.LoginResponse;
import com.ontograph.module.system.dal.dataobject.MenuDO;
import java.util.List;

/**
 * 认证服务接口
 */
public interface AuthService {
    /**
     * 用户登录
     * @param request LoginRequest
     * @return LoginResponse
     */
    LoginResponse login(LoginRequest request);
    /**
     * 获取当前登录用户信息
     * @return LoginResponse.UserInfo
     */
    LoginResponse.UserInfo getUserInfo();
    /**
     * 用户退出登录
     */
    void logout();
    /**
     * 获取当前用户的菜单树
     * 根据用户角色返回有权限访问的菜单
     * @return List<MenuDO> 菜单树
     */
    List<MenuDO> getUserMenus();
}
