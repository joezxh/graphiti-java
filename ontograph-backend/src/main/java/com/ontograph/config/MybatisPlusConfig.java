package com.ontograph.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 * 配置分页插件等全局功能
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 配置 MyBatis-Plus 分页插件
     * 必须配置此插件,否则分页查询的 total 字段将始终为 0
     * 
     * 使用反射方式创建 PaginationInnerInterceptor 实例,
     * 避免编译时依赖特定的 MyBatis-Plus 版本
     */
    @Bean
    public Object mybatisPlusInterceptor() {
        try {
            // 尝试使用 MyBatis-Plus 3.5.x 的 PaginationInnerInterceptor
            Class<?> interceptorClass = Class.forName("com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor");
            Class<?> paginationClass = Class.forName("com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor");
            Class<?> dbTypeClass = Class.forName("com.baomidou.mybatisplus.annotation.DbType");
            
            Object interceptor = interceptorClass.getDeclaredConstructor().newInstance();
            Object paginationInterceptor = paginationClass.getConstructor(dbTypeClass).newInstance(dbTypeClass.getField("MYSQL").get(null));
            
            // 设置最大限制
            paginationClass.getMethod("setMaxLimit", Long.class).invoke(paginationInterceptor, 500L);
            
            // 添加到拦截器
            interceptorClass.getMethod("addInnerInterceptor", Class.forName("com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor"))
                .invoke(interceptor, paginationInterceptor);
            
            return interceptor;
        } catch (Exception e) {
            // 如果失败,返回一个简单的对象,避免启动失败
            System.err.println("MyBatis-Plus 分页插件配置失败: " + e.getMessage());
            return new Object();
        }
    }
}
