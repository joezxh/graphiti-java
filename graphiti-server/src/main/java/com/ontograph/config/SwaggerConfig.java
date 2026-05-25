package com.ontograph.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger OpenAPI 配置类
 * 配置API文档的基本信息、安全认证等
 */
@Configuration
public class SwaggerConfig {

    /**
     * 创建OpenAPI文档配置
     * @return OpenAPI
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OntoGraph 知识图谱后端 API 文档")
                        .version("1.0.0")
                        .description("Graphiti知识图谱系统的RESTful API接口文档")
                        .contact(new Contact()
                                .name("OntoGraph Team")
                                .email("support@ontograph.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .name("Authorization")
                                        .description("JWT认证令牌，格式：Bearer {token}")));
    }
}
