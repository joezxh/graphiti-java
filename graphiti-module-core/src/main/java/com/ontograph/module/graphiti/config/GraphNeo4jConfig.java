package com.ontograph.module.graphiti.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Neo4j 配置类
 * 读取配置文件中的 Neo4j 连接信息，创建 Driver Bean
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "neo4j")
public class GraphNeo4jConfig {
    /**
     * Neo4j 连接 URI（例如：bolt://localhost:7687）
     */
    @NotBlank
    private String uri;
    /**
     * 用户名
     */
    @NotBlank
    private String username;
    /**
     * 密码
     */
    @NotBlank
    private String password;
    /**
     * 创建 Neo4j Driver Bean
     * @return Driver
     */
    @Bean
    public Driver neo4jDriver() {
        return GraphDatabase.driver(uri, 
                                   AuthTokens.basic(username, password));
    }
}
