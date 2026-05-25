package com.ontograph.system.service;

import com.ontograph.system.dto.LoginRequest;
import com.ontograph.system.dto.LoginResponse;

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
}
