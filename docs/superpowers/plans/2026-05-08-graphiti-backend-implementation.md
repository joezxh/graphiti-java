# Graphiti-Java 后端服务实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 完成 Graphiti-Java 后端服务开发，包括 Maven 多模块项目、数据库设计与初始化、用户认证、图谱管理、本体管理、数据导入、检索服务等核心功能，与前端控制台集成。

**Architecture:** Maven 多模块架构 - graphiti-dependencies (依赖管理) → graphiti-framework (common/security/mybatis/redis/web) → graphiti-module-system (用户权限) → graphiti-module-core (核心业务) → graphiti-server (启动模块)

**Tech Stack:** Java 21, Spring Boot 3.5.5, MyBatis-Plus 3.5.x, Neo4j Driver 5.x, Spring Data Redis, Spring Security 6.x, JWT, Spring AI 1.1.2

---

## 文件结构

```
graphiti-java/
├── pom.xml                           # 父 POM
├── graphiti-dependencies/pom.xml      # 依赖管理
├── graphiti-framework/
│   ├── graphiti-common/             # 公共模块
│   ├── graphiti-spring-boot-starter-security/   # 安全
│   ├── graphiti-spring-boot-starter-mybatis/    # MyBatis
│   ├── graphiti-spring-boot-starter-redis/     # Redis
│   └── graphiti-spring-boot-starter-web/       # Web
├── graphiti-module-system/           # 系统模块
├── graphiti-module-core/             # 核心业务模块
├── graphiti-server/                  # 启动模块
└── sql/                             # 数据库脚本
    ├── mysql/schema.sql
    ├── mysql/init-data.sql
    └── neo4j/init.cypher
```

---

### Task 1: 创建父项目 POM 和依赖管理

**Files:**
- Create: `d:\projects\graphiti-java\pom.xml`
- Create: `d:\projects\graphiti-java\graphiti-dependencies\pom.xml`

- [ ] **Step 1: 创建父项目 POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.graphiti</groupId>
    <artifactId>graphiti-java</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>Graphiti-Java</name>
    
    <modules>
        <module>graphiti-dependencies</module>
        <module>graphiti-framework</module>
        <module>graphiti-module-system</module>
        <module>graphiti-module-core</module>
        <module>graphiti-server</module>
    </modules>
    
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>
</project>
```

- [ ] **Step 2: 创建依赖管理 POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.graphiti</groupId>
        <artifactId>graphiti-java</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>graphiti-dependencies</artifactId>
    <packaging>pom</packaging>
    
    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>3.5.5</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            
            <!-- Spring AI -->
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>1.1.2</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            
            <!-- MyBatis-Plus -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-boot-starter</artifactId>
                <version>3.5.12</version>
            </dependency>
            
            <!-- Neo4j Driver -->
            <dependency>
                <groupId>org.neo4j.driver</groupId>
                <artifactId>neo4j-java-driver</artifactId>
                <version>5.26.0</version>
            </dependency>
            
            <!-- JWT -->
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-api</artifactId>
                <version>0.12.3</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-impl</artifactId>
                <version>0.12.3</version>
                <scope>runtime</scope>
            </dependency>
            
            <!-- Lombok -->
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>1.18.38</version>
                <scope>provided</scope>
            </dependency>
            
            <!-- MapStruct -->
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct</artifactId>
                <version>1.6.0</version>
            </dependency>
            
            <!-- Hutool -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>5.8.37</version>
            </dependency>
            
            <!-- SpringDoc -->
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                <version>2.8.5</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

- [ ] **Step 3: 验证并提交**

```bash
cd d:\projects\graphiti-java
mvn help:effective-pom
git add pom.xml graphiti-dependencies/pom.xml
git commit -m "feat: add parent POM and dependencies management"
```

---

### Task 2: 创建 graphiti-common 公共模块

**Files:**
- Create: `d:\projects\graphiti-java\graphiti-framework\graphiti-common\pom.xml`
- Create: `d:\projects\graphiti-java\graphiti-framework\graphiti-common\src\main\java\com\graphiti\common\constants\ResultCode.java`
- Create: `d:\projects\graphiti-java\graphiti-framework\graphiti-common\src\main\java\com\graphiti\common\response\CommonResult.java`
- Create: `d:\projects\graphiti-java\graphiti-framework\graphiti-common\src\main\java\com\graphiti\common\exception\GlobalExceptionHandler.java`

- [ ] **Step 1: 创建 common 模块 POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.graphiti</groupId>
        <artifactId>graphiti-java</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>graphiti-common</artifactId>
    <name>Graphiti Common</name>
    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建错误码常量**

```java
package com.graphiti.common.constants;

public interface ResultCode {
    int SUCCESS = 200;
    int BAD_REQUEST = 400;
    int UNAUTHORIZED = 401;
    int FORBIDDEN = 403;
    int NOT_FOUND = 404;
    int INTERNAL_SERVER_ERROR = 500;
    int GRAPH_NOT_FOUND = 1001;
    int ONTOLOGY_NOT_DEFINED = 1002;
    int NODE_NOT_FOUND = 1003;
    int EDGE_NOT_FOUND = 1004;
}
```

- [ ] **Step 3: 创建统一响应类**

```java
package com.graphiti.common.response;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class CommonResult<T> implements Serializable {
    private int code;
    private String message;
    private T data;
    private String timestamp;
    
    public CommonResult() {
        this.timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    
    public static <T> CommonResult<T> success(T data) {
        CommonResult<T> result = new CommonResult<>();
        result.setCode(ResultCode.SUCCESS);
        result.setMessage("success");
        result.setData(data);
        return result;
    }
    
    public static <T> CommonResult<T> error(int code, String message) {
        CommonResult<T> result = new CommonResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
```

- [ ] **Step 4: 创建全局异常处理器**

```java
package com.graphiti.common.exception;

import com.graphiti.common.response.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public CommonResult<?> handleBusinessException(BusinessException e) {
        log.error("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return CommonResult.error(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<?> handleValidationException(
            MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return CommonResult.error(400, message);
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResult<?> handleException(Exception e) {
        log.error("系统异常", e);
        return CommonResult.error(500, "系统内部错误");
    }
}
```

- [ ] **Step 5: 提交**

```bash
git add graphiti-framework/graphiti-common/
git commit -m "feat: add graphiti-common module"
```

---

### Task 3: 创建安全模块 (JWT + Spring Security)

**Files:**
- Create: `d:\projects\graphiti-java\graphiti-framework\graphiti-spring-boot-starter-security\pom.xml`
- Create: `d:\projects\graphiti-java\graphiti-framework\graphiti-spring-boot-starter-security\src\main\java\com\graphiti\framework\security\jwt\JwtTokenProvider.java`
- Create: `d:\projects\graphiti-java\graphiti-framework\graphiti-spring-boot-starter-security\src\main\java\com\graphiti\framework\security\config\SecurityConfig.java`

- [ ] **Step 1: 创建安全模块 POM** (参考 Task 2 格式，添加 spring-boot-starter-security 和 jjwt 依赖)

- [ ] **Step 2: 创建 JWT Token 提供器**

```java
package com.graphiti.framework.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {
    
    @Value("${graphiti.security.jwt.secret}")
    private String jwtSecret;
    
    @Value("${graphiti.security.jwt.expiration:86400}")
    private int jwtExpirationInSeconds;
    
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 
                               jwtExpirationInSeconds * 1000L);
        
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact();
    }
    
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token", e);
            return false;
        }
    }
}
```

- [ ] **Step 3: 创建 Security 配置**

```java
package com.graphiti.framework.security.config;

import com.graphiti.framework.security.jwt.JwtAuthenticationFilter;
import com.graphiti.framework.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> 
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            );
        
        JwtAuthenticationFilter jwtFilter = 
            new JwtAuthenticationFilter(jwtTokenProvider);
        http.addFilterBefore(jwtFilter, 
                            UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add graphiti-framework/graphiti-spring-boot-starter-security/
git commit -m "feat: add security module with JWT support"
```

---

### Task 4: 创建 MySQL 数据库脚本

**Files:**
- Create: `d:\projects\graphiti-java\sql\mysql\schema.sql`
- Create: `d:\projects\graphiti-java\sql\mysql\init-data.sql`
- Create: `d:\projects\graphiti-java\sql\neo4j\init.cypher`

- [ ] **Step 1: 创建 MySQL 建表脚本**

```sql
CREATE DATABASE IF NOT EXISTS graphiti 
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE graphiti;

CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(128) NOT NULL,
  `nickname` VARCHAR(64),
  `email` VARCHAR(128),
  `mobile` VARCHAR(32),
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP 
                      ON UPDATE CURRENT_TIMESTAMP,
  `deleted` BIT(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `code` VARCHAR(64) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP 
                      ON UPDATE CURRENT_TIMESTAMP,
  `deleted` BIT(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `graphiti_graph_metadata` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `graph_id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `description` TEXT,
  `node_count` INT NOT NULL DEFAULT 0,
  `edge_count` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP 
                      ON UPDATE CURRENT_TIMESTAMP,
  `deleted` BIT(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_graph_id` (`graph_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `graphiti_ontology` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `graph_id` VARCHAR(64) NOT NULL,
  `entities` JSON,
  `edges` JSON,
  `is_default` BIT(1) NOT NULL DEFAULT b'0',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP 
                      ON UPDATE CURRENT_TIMESTAMP,
  `deleted` BIT(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_graph_id` (`graph_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: 创建初始数据**

```sql
-- 密码: admin123 (BCrypt)
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `status`) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 
        '系统管理员', 1);

INSERT INTO `sys_role` (`name`, `code`, `status`) VALUES
('超级管理员', 'SUPER_ADMIN', 1),
('管理员', 'ADMIN', 1);
```

- [ ] **Step 3: 创建 Neo4j 初始化脚本**

```cypher
CREATE CONSTRAINT entity_uuid IF NOT EXISTS 
FOR (n:Entity) REQUIRE n.uuid IS UNIQUE;

CREATE CONSTRAINT episode_uuid IF NOT EXISTS 
FOR (n:Episode) REQUIRE n.uuid IS UNIQUE;

CREATE INDEX entity_group_id IF NOT EXISTS 
FOR (n:Entity) ON (n.group_id);

CREATE INDEX entity_name IF NOT EXISTS 
FOR (n:Entity) ON (n.name);

CREATE FULLTEXT INDEX entity_search IF NOT EXISTS 
FOR (n:Entity) ON EACH [n.name, n.summary];
```

- [ ] **Step 4: 提交**

```bash
git add sql/
git commit -m "feat: add database schema and initialization scripts"
```

---

### Task 5: 创建 graphiti-module-system 系统模块

**Files:**
- Create: `d:\projects\graphiti-java\graphiti-module-system\pom.xml`
- Create: `d:\projects\graphiti-java\graphiti-module-system\src\main\java\com\graphiti\system\controller\AuthController.java`
- Create: `d:\projects\graphiti-java\graphiti-module-system\src\main\java\com\graphiti\system\service\AuthService.java`
- Create: `d:\projects\graphiti-java\graphiti-module-system\src\main\java\com\graphiti\system\service\impl\AuthServiceImpl.java`

- [ ] **Step 1: 创建系统模块 POM** (添加 graphiti-common, starter-mybatis, starter-security, starter-redis 依赖)

- [ ] **Step 2: 创建登录控制器**

```java
package com.graphiti.system.controller;

import com.graphiti.common.response.CommonResult;
import com.graphiti.system.dto.LoginRequest;
import com.graphiti.system.dto.LoginResponse;
import com.graphiti.system.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/login")
    public CommonResult<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return CommonResult.success(authService.login(request));
    }
    
    @GetMapping("/info")
    public CommonResult<LoginResponse.UserInfo> getUserInfo() {
        return CommonResult.success(authService.getUserInfo());
    }
}
```

- [ ] **Step 3: 创建认证服务**

```java
package com.graphiti.system.service.impl;

import com.graphiti.common.exception.BusinessException;
import com.graphiti.framework.security.jwt.JwtTokenProvider;
import com.graphiti.system.dto.LoginRequest;
import com.graphiti.system.dto.LoginResponse;
import com.graphiti.system.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    
    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(), 
                request.getPassword())
        );
        
        String token = jwtTokenProvider.generateToken(auth);
        
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setExpiresIn(86400L);
        return response;
    }
    
    @Override
    public LoginResponse.UserInfo getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext()
                                  .getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(401, "未认证");
        }
        LoginResponse.UserInfo info = new LoginResponse.UserInfo();
        info.setUsername(auth.getName());
        return info;
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add graphiti-module-system/
git commit -m "feat: add system module with auth controller and service"
```

---

### Task 6: 创建 graphiti-server 启动模块

**Files:**
- Create: `d:\projects\graphiti-java\graphiti-server\pom.xml`
- Create: `d:\projects\graphiti-java\graphiti-server\src\main\java\com\graphiti\GraphitiApplication.java`
- Create: `d:\projects\graphiti-java\graphiti-server\src\main\resources\application.yml`

- [ ] **Step 1: 创建启动模块 POM** (添加 graphiti-module-system, graphiti-module-core, spring-boot-starter-web 依赖)

- [ ] **Step 2: 创建启动类**

```java
package com.graphiti;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.graphiti")
public class GraphitiApplication {
    public static void main(String[] args) {
        SpringApplication.run(GraphitiApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建配置文件**

```yaml
# application.yml
spring:
  profiles:
    active: dev
  datasource:
    url: jdbc:mysql://localhost:3306/graphiti?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      host: localhost
      port: 6379

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

graphiti:
  security:
    jwt:
      secret: mySecretKeyForJWTTokenGenerationWhichShouldBeAtLeast512BitsLong
      expiration: 86400

logging:
  level:
    com.graphiti: debug
```

- [ ] **Step 4: 构建并测试**

```bash
cd d:\projects\graphiti-java
mvn clean install -DskipTests
```

- [ ] **Step 5: 提交**

```bash
git add graphiti-server/
git commit -m "feat: add graphiti-server startup module"
```

---

## 后续任务 (Tasks 7-15)

由于篇幅限制，后续任务概要如下，详细步骤将在执行时展开：

### Task 7: 图谱管理模块
- 创建 `GraphMetadata` 实体类和 Mapper
- 创建 `GraphService` 和 `GraphController`
- 实现创建/列表/详情/更新/删除图谱接口

### Task 8: 本体管理模块
- 创建 `OntologyService` 和 `OntologyController`
- 实现设置/获取本体接口

### Task 9: Neo4j 数据访问层
- 配置 Neo4j Driver
- 创建 `Neo4jService` 实现节点/关系 CRUD

### Task 10: 节点管理模块
- 创建 `NodeService` 和 `NodeController`
- 实现节点列表/详情/关系/删除接口

### Task 11: 边管理模块
- 创建 `EdgeService` 和 `EdgeController`

### Task 12: 事件管理模块
- 创建 `EpisodeService` 和 `EpisodeController`

### Task 13: 检索服务模块
- 创建 `SearchService` 和 `SearchController`
- 实现全文检索/向量检索/图遍历

### Task 14: 数据导入模块
- 创建 `DataImportService` 和 `DataImportController`
- 集成 Spring AI 实现实体提取

### Task 15: 系统管理接口
- 用户管理/角色管理/菜单管理接口

---

## 执行计划

Plan complete and saved to `docs/superpowers/plans/2026-05-08-graphiti-backend-implementation.md`.

**Two execution options:**

1. **Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

2. **Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
