package com.ontograph.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Vue SPA (Single Page Application) Fallback Controller
 *
 * Vue Router 使用 History 模式时，当用户直接访问 /login、/dashboard 等前端路由，
 * 或刷新非根路径页面时，Spring Boot 需要将所有未匹配到 API/静态资源的请求
 * 转发到 index.html，由前端路由处理。
 *
 * 优先级说明：
 * - /api/** 由 @RestController 处理，不会进入此 Controller
 * - /actuator/** 由 Spring Boot Actuator 处理，不会进入此 Controller
 * - /swagger-ui/** 由 SpringDoc 处理，不会进入此 Controller
 * - /assets/**、/*.js、/*.css 等静态资源由 Spring 静态资源处理器处理
 * - 其余请求（如 /login、/dashboard、/graph/detail 等）进入此方法，返回 index.html
 */
@Controller
public class SpaForwardController {

    /**
     * 匹配所有前端路由，返回 index.html
     * 排除已知的后端路径前缀（防误拦截）
     */
    @RequestMapping(value = {
        "/",
        "/login",
        "/dashboard",
        "/graph/**",
        "/episode/**",
        "/ontology/**",
        "/prompt/**",
        "/search/**",
        "/legal-kg/**",
        "/legal-import/**",
        "/legal-extract/**",
        "/communities/**",
        "/system/**",
        "/admin/**",
        "/user/**",
        "/role/**",
        "/menu/**",
        "/notification/**",
        "/dev/**",
        "/{path:[^\\.]*}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
