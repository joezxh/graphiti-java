# 后端缺失接口实现：日志、监控、配置、搜索历史

**日期**: 2026-05-11
**状态**: 已确认，待实现

---

## 背景

前端 `log.ts`、`monitor.ts`、`system.ts`、`search.ts` 存在 Mock 实现，后端对应接口缺失。本次实现为四个模块补充完整的后端 API。

---

## 1. 操作日志模块

### 数据库表: `sys_operation_log`

```sql
CREATE TABLE sys_operation_log (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id     BIGINT COMMENT '用户ID',
    username    VARCHAR(50) COMMENT '用户名',
    operation   VARCHAR(100) COMMENT '操作名称',
    method      VARCHAR(200) COMMENT '请求方法和路径',
    params      TEXT COMMENT '请求参数(JSON)',
    ip          VARCHAR(50) COMMENT 'IP地址',
    location    VARCHAR(100) COMMENT '地理位置',
    status      TINYINT COMMENT '0-失败 1-成功',
    error_msg   VARCHAR(500) COMMENT '错误信息',
    duration    INT COMMENT '耗时(毫秒)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_username (username),
    INDEX idx_operation (operation),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志';
```

### API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/system/log/list` | 分页查询日志 |
| GET | `/admin/system/log/{id}` | 获取日志详情 |
| DELETE | `/admin/system/log/{id}` | 删除单条日志 |
| DELETE | `/admin/system/log/clear` | 清空所有日志 |
| GET | `/admin/system/log/export` | 导出日志(JSON) |

---

## 2. 系统配置模块

### 数据库表: `sys_system_config`

```sql
CREATE TABLE sys_system_config (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    config_key          VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value        TEXT COMMENT '配置值',
    config_name         VARCHAR(100) COMMENT '配置名称',
    config_description  VARCHAR(500) COMMENT '配置描述',
    config_type         TINYINT DEFAULT 1 COMMENT '1-文本 2-数字 3-布尔 4-JSON',
    group_name          VARCHAR(50) COMMENT '分组名称',
    sort_num            INT DEFAULT 0 COMMENT '排序',
    status              TINYINT DEFAULT 1 COMMENT '0-禁用 1-启用',
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_config_key (config_key),
    INDEX idx_group_name (group_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';
```

### API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/system/config/list` | 分页查询配置 |
| GET | `/admin/system/config/all` | 获取所有配置 |
| GET | `/admin/system/config/{id}` | 获取配置详情 |
| GET | `/admin/system/config/key/{key}` | 根据key获取值 |
| POST | `/admin/system/config/create` | 创建配置 |
| PUT | `/admin/system/config/{id}` | 更新配置 |
| DELETE | `/admin/system/config/{id}` | 删除配置 |

---

## 3. 搜索历史模块

### 数据库表: `sys_search_history`

```sql
CREATE TABLE sys_search_history (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id       BIGINT COMMENT '用户ID',
    query         VARCHAR(500) NOT NULL COMMENT '搜索词',
    mode          VARCHAR(20) COMMENT '搜索模式',
    result_count  INT DEFAULT 0 COMMENT '结果数量',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索历史记录';
```

### API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/graphiti/search-history/list` | 获取当前用户搜索历史(分页) |
| POST | `/admin/graphiti/search-history/save` | 保存搜索记录 |
| DELETE | `/admin/graphiti/search-history/clear` | 清空当前用户历史 |

---

## 4. 系统监控模块

采用 **Spring Boot Actuator**，不新建 Controller。

### 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 暴露端点

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, metrics, info
  endpoint:
    health:
      show-details: always
```

### 前端调用映射

| 前端需求 | Actuator 端点 |
|---------|-------------|
| 系统状态 | GET /actuator/health |
| Neo4j/MySQL 健康 | GET /actuator/health/components |
| JVM 内存 | GET /actuator/metrics/jvm.memory.used |
| CPU 使用率 | GET /actuator/metrics/process.cpu.usage |
| 数据库连接池 | GET /actuator/metrics/hikaricp.connections.active |

---

## 5. 前端 API 文件修改

| 文件 | 修改方式 |
|------|---------|
| `monitor.ts` | 调用 `/actuator/*` 端点 |
| `log.ts` | 调用 `/admin/system/log/*` |
| `system.ts` | 调用 `/admin/system/config/*` |
| `search.ts` | 调用 `/admin/graphiti/search-history/*` |

---

## 6. 文件清单

### 后端新增 (16 个文件)

- `ontograph-module-core/pom.xml` — Actuator 依赖
- `sql/mysql/schema.sql` — 3 张表 DDL
- `sql/postgresql/schema.sql` — 3 张表 DDL (PostgreSQL 版)
- `OperationLogDO.java`, `OperationLogMapper.java`, `OperationLogService.java`, `OperationLogServiceImpl.java`, `OperationLogController.java`
- `SystemConfigDO.java`, `SystemConfigMapper.java`, `SystemConfigService.java`, `SystemConfigServiceImpl.java`, `SystemConfigController.java`
- `SearchHistoryDO.java`, `SearchHistoryMapper.java`, `SearchHistoryService.java`, `SearchHistoryServiceImpl.java`, `SearchHistoryController.java`

### 前端修改 (4 个文件)

- `ontograph-web/src/api/monitor.ts`
- `ontograph-web/src/api/log.ts`
- `ontograph-web/src/api/system.ts`
- `ontograph-web/src/api/search.ts`

---

## 7. 安全说明

- Actuator 端点通过 Spring Security 配置放行（`/actuator/**`）
- 所有 CRUD 接口均需登录认证（Bearer Token）
- 搜索历史按当前登录用户隔离查询
