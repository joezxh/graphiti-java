package com.ontograph.module.system.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 登录响应 DTO
 */
@Data
public class LoginResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /** JWT Token */
    private String token;
    
    /** 过期时间（秒） */
    private Long expiresIn;
    
    /** 用户信息 */
    private UserInfo userInfo;
    
    /**
     * 用户信息内部类
     */
    @Data
    public static class UserInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /** 用户名 */
        private String username;
        
        /** 昵称 */
        private String nickname;
        
        /** 邮箱 */
        private String email;
    }
}
