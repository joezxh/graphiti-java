# PostgreSQL数据库实现

<!--<cite>
**本文档引用的文件**
- [schema.sql](file://sql/postgresql/schema.sql)
- [init-data.sql](file://sql/postgresql/init-data.sql)
- [prompt_template_postgresql_init.sql](file://docs/sql/prompt_template_postgresql_init.sql)
- [database-migration-guide.md](file://docs/database-migration-guide.md)
- [legal-ontology-migration-guide.md](file://docs/legal-ontology-migration-guide.md)
- [V1__create_ontology_tables.sql](file://sql/postgresql/V1__create_ontology_tables.sql)
- [V2__create_notification_tables.sql](file://sql/postgresql/V2__create_notification_tables.sql)
- [V3__create_legal_ontology.sql](file://sql/postgresql/V3__create_legal_ontology.sql)
- [V4__seed_legal_ontology.sql](file://sql/postgresql/V4__seed_legal_ontology.sql)
- [V5__create_ontology_tables.sql](file://sql/postgresql/V5__create_ontology_tables.sql)
- [application-dev.yml](file://graphiti-server/src/main/resources/application-dev.yml)
- [application.yml](file://graphiti-server/src/main/resources/application.yml)
</cite>-->

## 目录
1. [项目概述](#项目概述)
2. [项目结构](#项目结构)
3. [核心组件](#核心组件)
4. [架构总览](#架构总览)
5. [详细组件分析](#详细组件分析)
6. [依赖关系分析](#依赖关系分析)
7. [性能考虑](#性能考虑)
8. [故障排除指南](#故障排除指南)
9. [结论](#结论)
10. [附录](#附录)

## 项目概述
本项目为Graphiti知识图谱平台的PostgreSQL数据库实现，涵盖系统管理、图谱管理、通知模块、提示词模板、法律知识图谱本体等多个子系统。数据库采用PostgreSQL 15+作为核心存储引擎，通过版本化的迁移脚本实现从MySQL到PostgreSQL的完整迁移，并提供了丰富的索引策略、触发器机制和JSONB字段支持。

## 项目结构
项目采用模块化设计，数据库相关文件主要位于`sql/postgresql/`目录下，文档位于`docs/`目录，应用配置位于`graphiti-server/src/main/resources/`目录。

```mermaid
graph TB
subgraph "数据库脚本"
A[schema.sql<br/>完整数据库结构]
B[V1__create_ontology_tables.sql<br/>本体表结构V1]
C[V2__create_notification_tables.sql<br/>通知表结构]
D[V3__create_legal_ontology.sql<br/>法律本体初始化]
E[V4__seed_legal_ontology.sql<br/>法律本体数据]
F[V5__create_ontology_tables.sql<br/>本体表结构V5]
end
subgraph "初始化数据"
G[init-data.sql<br/>系统基础数据]
H[prompt_template_postgresql_init.sql<br/>提示词模板数据]
end
subgraph "文档指南"
I[database-migration-guide.md<br/>迁移指南]
J[legal-ontology-migration-guide.md<br/>法律本体迁移]
end
A --> G
B --> F
C --> H
D --> E
I --> A
J --> D
```

**图表来源**
- [schema.sql:1-319](file://sql/postgresql/schema.sql#L1-L319)
- [V1__create_ontology_tables.sql:1-126](file://sql/postgresql/V1__create_ontology_tables.sql#L1-L126)
- [V2__create_notification_tables.sql:1-114](file://sql/postgresql/V2__create_notification_tables.sql#L1-L114)
- [V3__create_legal_ontology.sql:1-611](file://sql/postgresql/V3__create_legal_ontology.sql#L1-L611)
- [V4__seed_legal_ontology.sql:1-329](file://sql/postgresql/V4__seed_legal_ontology.sql#L1-L329)
- [V5__create_ontology_tables.sql:1-281](file://sql/postgresql/V5__create_ontology_tables.sql#L1-L281)

**章节来源**
- [schema.sql:1-319](file://sql/postgresql/schema.sql#L1-L319)
- [init-data.sql:1-110](file://sql/postgresql/init-data.sql#L1-L110)

## 核心组件
PostgreSQL数据库实现包含六大核心模块：

### 系统管理模块
- **用户管理表**：sys_user（用户信息管理）
- **角色管理表**：sys_role（权限角色定义）
- **菜单管理表**：sys_menu（系统菜单结构）
- **关联表**：sys_user_role、sys_role_menu（用户-角色、角色-菜单关联）

### 图谱管理模块
- **图谱元数据表**：graphiti_graph_metadata（图谱基本信息）
- **本体定义表**：graphiti_ontology（本体结构定义）

### 通知模块
- **系统通知表**：sys_notification（系统通知消息）
- **用户通知设置表**：sys_user_notification_settings（通知偏好配置）
- **自定义抽取指令表**：custom_instruction（LLM抽取指令）

### 提示词模板模块
- **提示词模板表**：prompt_template（模板定义）
- **提示词变量表**：prompt_variable（模板变量定义）
- **提示词版本表**：prompt_version（模板版本管理）

### 法律知识图谱模块
- **本体定义系统**：ont_definition（本体定义）
- **类定义表**：ont_class（实体类型定义）
- **属性定义表**：ont_property（属性定义）
- **约束定义表**：ont_constraint（数据约束）
- **版本历史表**：ont_version_history（版本追踪）
- **映射表**：ont_mapping（本体映射）

**章节来源**
- [schema.sql:21-144](file://sql/postgresql/schema.sql#L21-L144)
- [V2__create_notification_tables.sql:6-114](file://sql/postgresql/V2__create_notification_tables.sql#L6-L114)
- [prompt_template_postgresql_init.sql:5-127](file://docs/sql/prompt_template_postgresql_init.sql#L5-L127)
- [V5__create_ontology_tables.sql:14-274](file://sql/postgresql/V5__create_ontology_tables.sql#L14-L274)

## 架构总览

```mermaid
erDiagram
SYS_USER {
bigserial id PK
varchar username UK
varchar password
varchar nickname
varchar email
varchar mobile
smallint status
timestamp create_time
timestamp update_time
boolean deleted
}
SYS_ROLE {
bigserial id PK
varchar name
varchar code UK
smallint status
timestamp create_time
timestamp update_time
boolean deleted
}
SYS_MENU {
bigserial id PK
varchar name
varchar permission
varchar url
bigint parent_id
int sort
smallint status
timestamp create_time
timestamp update_time
boolean deleted
}
SYS_USER_ROLE {
bigserial id PK
bigint user_id FK
bigint role_id FK
}
SYS_ROLE_MENU {
bigserial id PK
bigint role_id FK
bigint menu_id FK
}
GRAPHITI_GRAPH_METADATA {
bigserial id PK
varchar graph_id UK
varchar name
text description
integer node_count
integer edge_count
timestamp create_time
timestamp update_time
boolean deleted
}
GRAPHITI_ONTOLOGY {
bigserial id PK
varchar graph_id
text entities
text edges
boolean is_default
timestamp create_time
timestamp update_time
boolean deleted
}
SYS_NOTIFICATION {
bigserial id PK
bigint user_id
varchar title
text content
smallint type
smallint is_read
timestamp create_time
timestamp update_time
boolean deleted
}
SYS_USER_NOTIFICATION_SETTINGS {
bigserial id PK
bigint user_id UK
smallint system_enabled
smallint graph_enabled
smallint search_enabled
smallint email_enabled
timestamp create_time
timestamp update_time
boolean deleted
}
CUSTOM_INSTRUCTION {
bigserial id PK
varchar graph_id
text instruction
boolean enabled
timestamptz created_at
timestamptz updated_at
}
SYS_USER ||--o{ SYS_USER_ROLE : "has"
SYS_ROLE ||--o{ SYS_USER_ROLE : "has"
SYS_MENU ||--o{ SYS_ROLE_MENU : "has"
SYS_ROLE ||--o{ SYS_ROLE_MENU : "has"
GRAPHITI_GRAPH_METADATA ||--o{ GRAPHITI_ONTOLOGY : "defines"
```

**图表来源**
- [schema.sql:21-144](file://sql/postgresql/schema.sql#L21-L144)

## 详细组件分析

### 系统用户表（sys_user）
系统用户表采用PostgreSQL的BIGSERIAL自增主键，支持用户名唯一性约束和逻辑删除机制。

```mermaid
classDiagram
class SysUser {
+bigserial id
+varchar username
+varchar password
+varchar nickname
+varchar email
+varchar mobile
+smallint status
+timestamp create_time
+timestamp update_time
+boolean deleted
}
class TriggerFunction {
+update_updated_at_column()
+returns TRIGGER
+updates update_time
}
SysUser --> TriggerFunction : "触发器"
```

**图表来源**
- [schema.sql:22-48](file://sql/postgresql/schema.sql#L22-L48)

**章节来源**
- [schema.sql:22-48](file://sql/postgresql/schema.sql#L22-L48)

### 通知模块架构
通知模块采用异步消息机制，支持系统通知、图谱通知和检索通知三种类型。

```mermaid
sequenceDiagram
participant User as 用户
participant Notification as 通知服务
participant DB as PostgreSQL数据库
participant Settings as 通知设置
User->>Notification : 发送通知请求
Notification->>Settings : 检查用户通知设置
Settings->>DB : 查询用户通知偏好
DB-->>Settings : 返回设置状态
Settings-->>Notification : 返回通知开关状态
Notification->>DB : 插入通知记录
DB-->>Notification : 确认插入
Notification-->>User : 返回通知结果
```

**图表来源**
- [V2__create_notification_tables.sql:6-54](file://sql/postgresql/V2__create_notification_tables.sql#L6-L54)

**章节来源**
- [V2__create_notification_tables.sql:6-54](file://sql/postgresql/V2__create_notification_tables.sql#L6-L54)

### 提示词模板系统
提示词模板系统支持版本化管理和动态变量替换，采用JSON Schema定义响应格式。

```mermaid
classDiagram
class PromptTemplate {
+bigserial id
+varchar code UK
+varchar name
+varchar description
+varchar type
+text system_prompt
+text user_prompt_template
+text response_format
+boolean enabled
+varchar model
+int sort
+varchar tags
+text extra_config
+bigint created_by
+bigint updated_by
+timestamp created_at
+timestamp updated_at
}
class PromptVariable {
+bigserial id
+bigint template_id FK
+varchar variable_name
+varchar description
+varchar variable_type
+boolean required
+varchar default_value
+varchar source
+varchar validation_rule
+int sort
+varchar remark
+timestamp created_at
+timestamp updated_at
}
class PromptVersion {
+bigserial id
+bigint template_id FK
+int version
+text system_prompt
+text user_prompt_template
+text response_format
+varchar description
+boolean active
+bigint created_by
+timestamp created_at
}
PromptTemplate "1" <-- "many" PromptVariable : "has"
PromptTemplate "1" <-- "many" PromptVersion : "has"
```

**图表来源**
- [prompt_template_postgresql_init.sql:5-102](file://docs/sql/prompt_template_postgresql_init.sql#L5-L102)

**章节来源**
- [prompt_template_postgresql_init.sql:5-127](file://docs/sql/prompt_template_postgresql_init.sql#L5-L127)

### 法律知识图谱本体系统
法律知识图谱采用六表架构，支持复杂的本体定义、类层次、属性约束和版本管理。

```mermaid
erDiagram
ONT_DEFINITION {
bigserial id PK
varchar graph_id
varchar namespace
varchar name
varchar version
varchar status
text description
bigint parent_version_id FK
varchar created_by
timestamp created_at
timestamp updated_at
}
ONT_CLASS {
bigserial id PK
bigint definition_id FK
varchar class_uri UK
varchar local_name UK
bigint parent_class_id FK
text equivalent_to
text disjoint_with
text description
text example
varchar domain_hint
text metadata
timestamp created_at
timestamp updated_at
}
ONT_PROPERTY {
bigserial id PK
bigint definition_id FK
varchar property_uri UK
varchar local_name
varchar property_type
bigint domain_class_id FK
bigint range_class_id FK
varchar range_data_type
int min_cardinality
int max_cardinality
varchar default_value
text allowed_values
bigint parent_property_id FK
text equivalent_to
bigint inverse_of_id FK
boolean is_required
boolean is_multiple
varchar pattern
decimal min_value
decimal max_value
text description
text example
text metadata
timestamp created_at
timestamp updated_at
}
ONT_CONSTRAINT {
bigserial id PK
bigint definition_id FK
bigint class_id FK
bigint property_id FK
varchar constraint_type
text value
varchar error_message
varchar severity
text description
timestamp created_at
timestamp updated_at
}
ONT_VERSION_HISTORY {
bigserial id PK
bigint definition_id FK
varchar version
varchar change_type
varchar entity_type
bigint entity_id
text before_state
text after_state
varchar diff_summary
varchar changed_by
timestamp changed_at
}
ONT_MAPPING {
bigserial id PK
bigint definition_id FK
varchar source_ontology
varchar source_type
varchar mapped_class_uri
varchar mapping_type
decimal confidence
text metadata
timestamp created_at
}
ONT_DEFINITION ||--o{ ONT_CLASS : "contains"
ONT_DEFINITION ||--o{ ONT_PROPERTY : "contains"
ONT_DEFINITION ||--o{ ONT_CONSTRAINT : "contains"
ONT_DEFINITION ||--o{ ONT_VERSION_HISTORY : "history_of"
ONT_DEFINITION ||--o{ ONT_MAPPING : "maps_to"
ONT_CLASS ||--o{ ONT_CLASS : "inherits_from"
ONT_PROPERTY ||--o{ ONT_PROPERTY : "inherits_from"
```

**图表来源**
- [V5__create_ontology_tables.sql:14-274](file://sql/postgresql/V5__create_ontology_tables.sql#L14-L274)

**章节来源**
- [V5__create_ontology_tables.sql:14-274](file://sql/postgresql/V5__create_ontology_tables.sql#L14-L274)

## 依赖关系分析

```mermaid
graph LR
subgraph "数据库层"
A[PostgreSQL 15+]
B[驱动程序]
C[连接池]
end
subgraph "应用层"
D[Spring Boot]
E[MyBatis-Plus]
F[动态数据源]
end
subgraph "业务模块"
G[系统管理]
H[图谱管理]
I[通知模块]
J[提示词模板]
K[法律本体]
end
A --> B
B --> C
C --> D
D --> E
E --> F
F --> G
F --> H
F --> I
F --> J
F --> K
```

**图表来源**
- [application-dev.yml:488-502](file://graphiti-server/src/main/resources/application-dev.yml#L488-L502)
- [application.yml:1-67](file://graphiti-server/src/main/resources/application.yml#L1-L67)

**章节来源**
- [application-dev.yml:488-502](file://graphiti-server/src/main/resources/application-dev.yml#L488-L502)
- [application.yml:1-67](file://graphiti-server/src/main/resources/application.yml#L1-L67)

## 性能考虑

### 索引策略
PostgreSQL数据库采用多层次索引策略：

1. **唯一索引**：确保数据完整性
   - 用户名唯一索引：uk_sys_user_username
   - 角色编码唯一索引：uk_sys_role_code
   - 图谱ID唯一索引：uk_graphiti_graph_metadata_graph_id

2. **普通索引**：优化查询性能
   - 状态过滤索引：idx_sys_user_status, idx_sys_role_status
   - 删除标记索引：idx_sys_user_deleted, idx_sys_role_deleted
   - 时间戳索引：idx_sys_operation_log_create_time

3. **复合索引**：支持复杂查询
   - 通知类型索引：idx_sys_notification_type, idx_sys_notification_is_read
   - 本体查询索引：idx_ont_class_domain, idx_ont_prop_type

### 触发器机制
系统采用触发器自动维护更新时间字段，减少应用层代码复杂度。

```mermaid
flowchart TD
Start([数据更新开始]) --> CheckTrigger{"检查是否存在触发器"}
CheckTrigger --> |是| UpdateTimestamp["更新update_time字段"]
CheckTrigger --> |否| SkipTrigger["跳过触发器"]
UpdateTimestamp --> CascadeDelete{"检查级联删除"}
CascadeDelete --> |是| DeleteRelated["删除关联数据"]
CascadeDelete --> |否| Complete["完成更新"]
DeleteRelated --> Complete
SkipTrigger --> Complete
```

**图表来源**
- [schema.sql:36-48](file://sql/postgresql/schema.sql#L36-L48)

### 连接池配置
推荐的HikariCP配置参数：
- maximum-pool-size: 10-20（根据并发需求调整）
- minimum-idle: 5-10（保持的最小连接数）
- connection-timeout: 30000ms（连接超时时间）
- idle-timeout: 600000ms（空闲超时时间）
- max-lifetime: 1800000ms（连接最大生命周期）

**章节来源**
- [database-migration-guide.md:174-190](file://docs/database-migration-guide.md#L174-L190)

## 故障排除指南

### 常见连接问题
1. **连接被拒绝**
   - 检查PostgreSQL服务状态
   - 验证pg_hba.conf配置
   - 确认防火墙设置

2. **时区配置问题**
   ```sql
   -- 在JDBC URL中添加时区参数
   jdbc:postgresql://localhost:5432/graphiti?serverTimezone=Asia/Shanghai
   ```

3. **JSONB类型支持问题**
   - 确保MyBatis-Plus版本>=3.5.7
   - 添加mybatis-plus-jsonb依赖

### 数据迁移问题
1. **使用pgLoader进行迁移**
   ```bash
   pgloader mysql://root:password@localhost:3306/graphiti \
   postgresql://postgres:password@localhost:5432/graphiti
   ```

2. **手动数据转换**
   - 处理数据类型差异
   - 转换时间戳格式
   - 适配JSON字段

### 性能优化建议
1. **索引优化**
   ```sql
   -- 为常用查询字段创建索引
   CREATE INDEX idx_user_email ON sys_user(email);
   CREATE INDEX idx_graph_name ON graphiti_graph_metadata(name);
   ```

2. **查询优化**
   - 使用EXPLAIN ANALYZE分析慢查询
   - 合理使用LIMIT子句
   - 避免SELECT *查询

**章节来源**
- [database-migration-guide.md:139-162](file://docs/database-migration-guide.md#L139-L162)

## 结论
Graphiti项目的PostgreSQL数据库实现展现了现代化数据库设计的最佳实践。通过合理的表结构设计、完善的索引策略、灵活的触发器机制和丰富的JSONB字段支持，系统能够高效支撑知识图谱应用的各种业务场景。版本化的迁移脚本确保了从MySQL到PostgreSQL的平滑过渡，而详细的文档和配置指南为运维管理提供了有力保障。

## 附录

### 版本迁移脚本说明

| 版本 | 文件名 | 功能描述 |
|------|--------|----------|
| V1 | V1__create_ontology_tables.sql | 本体系统基础表结构 |
| V2 | V2__create_notification_tables.sql | 通知模块表结构 |
| V3 | V3__create_legal_ontology.sql | 法律知识图谱初始化 |
| V4 | V4__seed_legal_ontology.sql | 法律本体数据填充 |
| V5 | V5__create_ontology_tables.sql | 完整本体管理系统 |
| V6 | V6__seed_legal_ontology_v2.sql | 法律本体V2数据 |
| V7 | V7__seed_legal_neo4j_data.sql | Neo4j图谱数据 |

### 字段类型对比（MySQL vs PostgreSQL）

| MySQL类型 | PostgreSQL类型 | 说明 |
|-----------|----------------|------|
| AUTO_INCREMENT | BIGSERIAL | 自增序列 |
| TINYINT(1) | BOOLEAN/SMALLINT | 布尔类型 |
| JSON | JSONB | 二进制JSON格式 |
| DATETIME | TIMESTAMP/TIMESTAMP WITH TIME ZONE | 时间戳类型 |
| ENGINE=InnoDB | 移除 | PostgreSQL无需存储引擎声明 |
| DEFAULT CHARSET=utf8mb4 | 移除 | 默认UTF8编码 |

### 索引优化策略
1. **热点数据索引**：为高频查询字段建立索引
2. **复合索引设计**：针对多条件查询优化
3. **分区表策略**：大数据量表的分区管理
4. **统计信息更新**：定期更新表统计信息

**章节来源**
- [database-migration-guide.md:202-218](file://docs/database-migration-guide.md#L202-L218)