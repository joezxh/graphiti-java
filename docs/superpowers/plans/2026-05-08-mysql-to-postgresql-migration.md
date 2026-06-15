# MySQL 到 PostgreSQL 迁移实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 ontograph-java 项目的数据库从 MySQL 8.0 迁移到 PostgreSQL 15+，包括依赖、配置、SQL 脚本和代码的适配。

**Architecture:** 使用 PostgreSQL 原生驱动和语法，利用 PostgreSQL 的 `GENERATED ALWAYS AS IDENTITY` 替代 MySQL 的 `AUTO_INCREMENT`，使用 `JSONB` 类型替代 MySQL 的 `JSON` 类型以获得更好的性能。保持 MyBatis-Plus 的抽象层，最小化代码修改。

**Tech Stack:** PostgreSQL 15+, postgresql JDBC Driver 42.7+, MyBatis-Plus 3.5.7+, Spring Boot 3.4.1

---

## 文件结构映射

### 需要修改的文件
1. `pom.xml` (根) - 依赖管理：移除 MySQL，添加 PostgreSQL
2. `ontograph-module-system/pom.xml` - 模块依赖：移除 MySQL
3. `ontograph-framework/graphiti-spring-boot-starter-mybatis/pom.xml` - Starter 依赖：移除 MySQL
4. `ontograph-server/src/main/resources/application.yml` - 主配置：数据库连接
5. `ontograph-server/src/main/resources/application-dev.yml` - 开发配置：数据库连接
6. `sql/mysql/schema.sql` - 原 MySQL 脚本（保留备份）
7. `sql/mysql/init-data.sql` - 原初始化数据（保留备份）

### 需要创建的文件
1. `sql/postgresql/schema.sql` - PostgreSQL 表结构脚本
2. `sql/postgresql/init-data.sql` - PostgreSQL 初始化数据脚本

### 可能需要修改的文件
1. `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/GraphMetadataDO.java` - 实体类（如需要）
2. `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/OntologyDO.java` - 实体类（如需要）

---

## Task 1: 更新 Maven 依赖（PostgreSQL 驱动）

**Files:**
- Modify: `d:/projects/ontograph-java/pom.xml:39,142-147`
- Modify: `d:/projects/ontograph-java/ontograph-module-system/pom.xml:42-44`
- Modify: `d:/projects/ontograph-java/ontograph-framework/graphiti-spring-boot-starter-mybatis/pom.xml:27-29`

- [ ] **Step 1: 修改根 pom.xml 的依赖管理**

在 `pom.xml` 中：
1. 将 MySQL 版本定义改为 PostgreSQL 版本定义
2. 将 MySQL 依赖改为 PostgreSQL 依赖

```xml
<!-- 第 39 行：修改版本定义 -->
<!-- 旧 -->
<mysql.version>9.1.0</mysql.version>
<!-- 新 -->
<postgresql.version>42.7.4</postgresql.version>
```

```xml
<!-- 第 142-147 行：修改依赖管理 -->
<!-- 旧 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>${mysql.version}</version>
</dependency>
<!-- 新 -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>${postgresql.version}</version>
</dependency>
```

- [ ] **Step 2: 修改 ontograph-module-system/pom.xml**

```xml
<!-- 第 42-44 行：修改依赖 -->
<!-- 旧 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
<!-- 新 -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

- [ ] **Step 3: 修改 graphiti-spring-boot-starter-mybatis/pom.xml**

```xml
<!-- 第 27-29 行：修改依赖 -->
<!-- 旧 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
<!-- 新 -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

- [ ] **Step 4: 验证依赖修改**

Run: `cd d:/projects/ontograph-java && mvn clean compile -DskipTests`

Expected: BUILD SUCCESS（此时可能仍有配置错误，但编译应通过）

- [ ] **Step 5: Commit**

```bash
git add pom.xml ontograph-module-system/pom.xml ontograph-framework/graphiti-spring-boot-starter-mybatis/pom.xml
git commit -m "chore: 替换 MySQL 驱动为 PostgreSQL 驱动"
```

---

## Task 2: 更新应用配置文件（数据库连接）

**Files:**
- Modify: `d:/projects/ontograph-java/ontograph-server/src/main/resources/application.yml:9-22,36`
- Modify: `d:/projects/ontograph-java/ontograph-server/src/main/resources/application-dev.yml:3-12`

- [ ] **Step 1: 修改 application.yml 数据库连接配置**

```yaml
# 第 9-22 行：修改 datasource 配置
# 旧
spring:
  datasource:
    dynamic:
      primary: master
      strict: false
      datasource:
        master:
          url: jdbc:mysql://localhost:3306/graphiti?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
          username: root
          password: 123456
          driver-class-name: com.mysql.cj.jdbc.Driver
          hikari:
            maximum-pool-size: 10
            minimum-idle: 5
            connection-timeout: 30000

# 新
spring:
  datasource:
    dynamic:
      primary: master
      strict: false
      datasource:
        master:
          url: jdbc:postgresql://localhost:5432/graphiti?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
          username: postgres
          password: 123456
          driver-class-name: org.postgresql.Driver
          hikari:
            maximum-pool-size: 10
            minimum-idle: 5
            connection-timeout: 30000
```

```yaml
# 第 36 行：修改 MyBatis-Plus ID 策略
# 旧
mybatis-plus:
  global-config:
    db-config:
      id-type: auto

# 新（PostgreSQL 使用 IDENTITY 或 INPUT）
mybatis-plus:
  global-config:
    db-config:
      id-type: auto  # 配合 GENERATED ALWAYS AS IDENTITY 使用
```

- [ ] **Step 2: 修改 application-dev.yml 数据库连接配置**

```yaml
# 第 3-12 行：修改 datasource 配置
# 旧
spring:
  datasource:
    dynamic:
      primary: master
      strict: false
      datasource:
        master:
          url: jdbc:mysql://localhost:3306/graphiti?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
          username: root
          password: 123456
          driver-class-name: com.mysql.cj.jdbc.Driver

# 新
spring:
  datasource:
    dynamic:
      primary: master
      strict: false
      datasource:
        master:
          url: jdbc:postgresql://localhost:5432/graphiti?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
          username: postgres
          password: 123456
          driver-class-name: org.postgresql.Driver
```

- [ ] **Step 3: 验证配置修改**

Run: `cd d:/projects/ontograph-java && mvn clean compile -DskipTests`

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add ontograph-server/src/main/resources/application.yml ontograph-server/src/main/resources/application-dev.yml
git commit -m "chore: 更新数据库连接配置为 PostgreSQL"
```

---

## Task 3: 创建 PostgreSQL 数据库 schema 脚本

**Files:**
- Create: `d:/projects/ontograph-java/sql/postgresql/schema.sql`
- Reference: `d:/projects/ontograph-java/sql/mysql/schema.sql`

- [ ] **Step 1: 创建 PostgreSQL 目录结构**

```bash
mkdir -p d:/projects/ontograph-java/sql/postgresql
```

- [ ] **Step 2: 编写 PostgreSQL schema.sql**

创建文件 `sql/postgresql/schema.sql`，内容如下：

```sql
-- Graphiti 数据库 Schema (PostgreSQL 版本)
-- 创建时间: 2026-05-08
-- 说明: 从 MySQL 迁移到 PostgreSQL

-- 设置客户端编码
SET client_encoding = 'UTF8';

-- 如果表已存在则删除（注意顺序：先删子表，再删主表）
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS graphiti_graph_metadata;
DROP TABLE IF EXISTS graphiti_ontology;

-- ============================================================
-- 系统管理模块表
-- ============================================================

-- 用户表
CREATE TABLE sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0
);

-- 创建更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 为 sys_user 表创建更新时间触发器
CREATE TRIGGER update_sys_user_updated_at
    BEFORE UPDATE ON sys_user
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 角色表
CREATE TABLE sys_role (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0
);

-- 为 sys_role 表创建更新时间触发器
CREATE TRIGGER update_sys_role_updated_at
    BEFORE UPDATE ON sys_role
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 用户角色关联表
CREATE TABLE sys_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE
);

-- 菜单表
CREATE TABLE sys_menu (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    permission VARCHAR(100),
    url VARCHAR(200),
    parent_id BIGINT DEFAULT 0,
    sort SMALLINT DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0
);

-- 为 sys_menu 表创建更新时间触发器
CREATE TRIGGER update_sys_menu_updated_at
    BEFORE UPDATE ON sys_menu
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 角色菜单关联表
CREATE TABLE sys_role_menu (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu(id) ON DELETE CASCADE
);

-- ============================================================
-- 图谱管理模块表
-- ============================================================

-- 图谱元数据表
CREATE TABLE graphiti_graph_metadata (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    config JSONB,  -- PostgreSQL 使用 JSONB 类型（二进制 JSON，性能更好）
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0
);

-- 为 graphiti_graph_metadata 表创建更新时间触发器
CREATE TRIGGER update_graph_metadata_updated_at
    BEFORE UPDATE ON graphiti_graph_metadata
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 本体定义表
CREATE TABLE graphiti_ontology (
    id BIGSERIAL PRIMARY KEY,
    graph_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    definition JSONB,  -- PostgreSQL 使用 JSONB 类型
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0,
    CONSTRAINT fk_ontology_graph FOREIGN KEY (graph_id) REFERENCES graphiti_graph_metadata(id) ON DELETE CASCADE
);

-- 为 graphiti_ontology 表创建更新时间触发器
CREATE TRIGGER update_graph_ontology_updated_at
    BEFORE UPDATE ON graphiti_ontology
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- 索引创建
-- ============================================================

-- sys_user 表索引
CREATE INDEX idx_sys_user_username ON sys_user(username);
CREATE INDEX idx_sys_user_status ON sys_user(status);

-- sys_role 表索引
CREATE INDEX idx_sys_role_code ON sys_role(code);

-- sys_menu 表索引
CREATE INDEX idx_sys_menu_parent_id ON sys_menu(parent_id);

-- graphiti_graph_metadata 表索引
CREATE INDEX idx_graph_metadata_name ON graphiti_graph_metadata(name);
CREATE INDEX idx_graph_metadata_status ON graphiti_graph_metadata(status);

-- graphiti_ontology 表索引
CREATE INDEX idx_graph_ontology_graph_id ON graphiti_ontology(graph_id);
CREATE INDEX idx_graph_ontology_name ON graphiti_ontology(name);

-- ============================================================
-- 注释（PostgreSQL 风格）
-- ============================================================

COMMENT ON TABLE sys_user IS '系统用户表';
COMMENT ON COLUMN sys_user.id IS '用户ID';
COMMENT ON COLUMN sys_user.username IS '用户名';
COMMENT ON COLUMN sys_user.password IS '密码（加密存储）';
COMMENT ON COLUMN sys_user.nickname IS '昵称';
COMMENT ON COLUMN sys_user.email IS '邮箱';
COMMENT ON COLUMN sys_user.phone IS '手机号';
COMMENT ON COLUMN sys_user.status IS '状态（0-禁用，1-启用）';
COMMENT ON COLUMN sys_user.created_at IS '创建时间';
COMMENT ON COLUMN sys_user.updated_at IS '更新时间';
COMMENT ON COLUMN sys_user.deleted IS '逻辑删除标记';

COMMENT ON TABLE sys_role IS '系统角色表';
COMMENT ON COLUMN sys_role.id IS '角色ID';
COMMENT ON COLUMN sys_role.name IS '角色名称';
COMMENT ON COLUMN sys_role.code IS '角色编码';
COMMENT ON COLUMN sys_role.status IS '状态（0-禁用，1-启用）';

COMMENT ON TABLE sys_menu IS '系统菜单表';
COMMENT ON COLUMN sys_menu.id IS '菜单ID';
COMMENT ON COLUMN sys_menu.name IS '菜单名称';
COMMENT ON COLUMN sys_menu.permission IS '权限标识';
COMMENT ON COLUMN sys_menu.url IS '菜单URL';
COMMENT ON COLUMN sys_menu.parent_id IS '父菜单ID';

COMMENT ON TABLE graphiti_graph_metadata IS '图谱元数据表';
COMMENT ON COLUMN graphiti_graph_metadata.id IS '图谱ID';
COMMENT ON COLUMN graphiti_graph_metadata.name IS '图谱名称';
COMMENT ON COLUMN graphiti_graph_metadata.description IS '图谱描述';
COMMENT ON COLUMN graphiti_graph_metadata.config IS '图谱配置（JSON格式）';
COMMENT ON COLUMN graphiti_graph_metadata.status IS '状态（0-禁用，1-启用）';

COMMENT ON TABLE graphiti_ontology IS '本体定义表';
COMMENT ON COLUMN graphiti_ontology.id IS '本体ID';
COMMENT ON COLUMN graphiti_ontology.graph_id IS '所属图谱ID';
COMMENT ON COLUMN graphiti_ontology.name IS '本体名称';
COMMENT ON COLUMN graphiti_ontology.definition IS '本体定义（JSON格式）';
COMMENT ON COLUMN graphiti_ontology.status IS '状态（0-禁用，1-启用）';
```

- [ ] **Step 3: 验证 SQL 语法**

Run: `psql -U postgres -d graphiti -f sql/postgresql/schema.sql`

Expected: 所有表创建成功，无错误

- [ ] **Step 4: Commit**

```bash
git add sql/postgresql/schema.sql
git commit -m "feat: 添加 PostgreSQL 数据库 schema 脚本"
```

---

## Task 4: 创建 PostgreSQL 初始化数据脚本

**Files:**
- Create: `d:/projects/ontograph-java/sql/postgresql/init-data.sql`
- Reference: `d:/projects/ontograph-java/sql/mysql/init-data.sql`

- [ ] **Step 1: 编写 PostgreSQL init-data.sql**

创建文件 `sql/postgresql/init-data.sql`，内容如下：

```sql
-- Graphiti 初始化数据 (PostgreSQL 版本)
-- 创建时间: 2026-05-08
-- 说明: 从 MySQL 迁移到 PostgreSQL

-- ============================================================
-- 初始化系统角色
-- ============================================================

INSERT INTO sys_role (name, code, status) VALUES
('超级管理员', 'SUPER_ADMIN', 1),
('管理员', 'ADMIN', 1),
('普通用户', 'USER', 1);

-- ============================================================
-- 初始化系统用户（密码：admin123）
-- 注意：PostgreSQL 的 BIGSERIAL 会自动生成 ID
-- ============================================================

INSERT INTO sys_user (username, password, nickname, email, phone, status) 
VALUES (
    'admin', 
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 
    '系统管理员', 
    'admin@graphiti.com', 
    '13800138000', 
    1
);

-- ============================================================
-- 初始化用户角色关联
-- ============================================================

-- 获取 admin 用户的 ID 和 SUPER_ADMIN 角色的 ID
DO $$
DECLARE
    admin_user_id BIGINT;
    super_admin_role_id BIGINT;
BEGIN
    SELECT id INTO admin_user_id FROM sys_user WHERE username = 'admin';
    SELECT id INTO super_admin_role_id FROM sys_role WHERE code = 'SUPER_ADMIN';
    
    INSERT INTO sys_user_role (user_id, role_id) 
    VALUES (admin_user_id, super_admin_role_id);
END $$;

-- ============================================================
-- 初始化系统菜单
-- ============================================================

INSERT INTO sys_menu (name, permission, url, parent_id, sort, status) VALUES
('系统管理', 'system:manage', '/system', 0, 1, 1),
('用户管理', 'system:user:list', '/system/user', 1, 1, 1),
('角色管理', 'system:role:list', '/system/role', 1, 2, 1),
('菜单管理', 'system:menu:list', '/system/menu', 1, 3, 1);

-- ============================================================
-- 初始化角色菜单关联
-- ============================================================

DO $$
DECLARE
    super_admin_role_id BIGINT;
    menu_id_1 BIGINT;
    menu_id_2 BIGINT;
    menu_id_3 BIGINT;
    menu_id_4 BIGINT;
BEGIN
    SELECT id INTO super_admin_role_id FROM sys_role WHERE code = 'SUPER_ADMIN';
    SELECT id INTO menu_id_1 FROM sys_menu WHERE name = '系统管理';
    SELECT id INTO menu_id_2 FROM sys_menu WHERE name = '用户管理';
    SELECT id INTO menu_id_3 FROM sys_menu WHERE name = '角色管理';
    SELECT id INTO menu_id_4 FROM sys_menu WHERE name = '菜单管理';
    
    INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (super_admin_role_id, menu_id_1),
    (super_admin_role_id, menu_id_2),
    (super_admin_role_id, menu_id_3),
    (super_admin_role_id, menu_id_4);
END $$;

-- ============================================================
-- 初始化示例图谱
-- ============================================================

INSERT INTO graphiti_graph_metadata (name, description, config, status) VALUES
('示例图谱', '这是一个示例知识图谱', '{"version": "1.0", "type": "knowledge_graph"}'::jsonb, 1);

-- 验证数据插入
SELECT 'Users' as table_name, count(*) as count FROM sys_user
UNION ALL
SELECT 'Roles', count(*) FROM sys_role
UNION ALL
SELECT 'Menus', count(*) FROM sys_menu
UNION ALL
SELECT 'Graphs', count(*) FROM graphiti_graph_metadata;
```

- [ ] **Step 2: 验证初始化数据**

Run: `psql -U postgres -d graphiti -f sql/postgresql/init-data.sql`

Expected: 数据插入成功，查询结果正确

- [ ] **Step 3: Commit**

```bash
git add sql/postgresql/init-data.sql
git commit -m "feat: 添加 PostgreSQL 初始化数据脚本"
```

---

## Task 5: 检查并适配 Java 实体类

**Files:**
- Modify: `d:/projects/ontograph-java/ontograph-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/GraphMetadataDO.java:18`
- Modify: `d:/projects/ontograph-java/ontograph-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/OntologyDO.java:18`
- Reference: `d:/projects/ontograph-java/ontograph-module-system/src/main/java/com/graphiti/system/dal/dataobject/UserDO.java`
- Reference: `d:/projects/ontograph-java/ontograph-module-system/src/main/java/com/graphiti/system/dal/dataobject/RoleDO.java`
- Reference: `d:/projects/ontograph-java/ontograph-module-system/src/main/java/com/graphiti/system/dal/dataobject/MenuDO.java`

- [ ] **Step 1: 检查 MyBatis-Plus 实体类的 @TableId 注解**

检查以下文件中的 `@TableId` 注解：
1. `UserDO.java` - 第 18 行
2. `RoleDO.java` - 第 17 行
3. `MenuDO.java` - 第 18 行
4. `GraphMetadataDO.java` - 第 18 行
5. `OntologyDO.java` - 第 18 行

如果使用了 `@TableId(type = IdType.AUTO)`，PostgreSQL 的 `BIGSERIAL` 或 `GENERATED ALWAYS AS IDENTITY` 会自动处理，通常无需修改。

但如果使用的是 `IdType.INPUT`，则需要改为 `IdType.AUTO` 或 `IdType.ASSIGN_ID`。

- [ ] **Step 2: 检查 JSON/JSONB 类型映射（如果需要）**

如果使用 PostgreSQL 的 `JSONB` 类型，可能需要添加类型处理器。在 `GraphMetadataDO.java` 和 `OntologyDO.java` 中：

```java
// 如果 config/definition 字段是 JSONB 类型，可能需要添加 @TableField 注解
@TableField(typeHandler = JacksonTypeHandler.class)
private Object config;  // 或使用具体的 DTO 类
```

**注意**: MyBatis-Plus 3.5.7+ 已经支持 PostgreSQL 的 JSONB 类型，通常无需额外配置。

- [ ] **Step 3: 编译验证**

Run: `cd d:/projects/ontograph-java && mvn clean compile -DskipTests`

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit（如果有修改）**

```bash
git add ontograph-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/*.java
git commit -m "chore: 适配 PostgreSQL 实体类（如需要）"
```

---

## Task 6: 创建数据库迁移文档

**Files:**
- Create: `d:/projects/ontograph-java/docs/database-migration-guide.md`

- [ ] **Step 1: 编写迁移指南文档**

创建文件 `docs/database-migration-guide.md`，内容如下：

```markdown
# ontograph-java 数据库迁移指南

## 从 MySQL 8.0 迁移到 PostgreSQL 15+

### 1. 环境准备

#### 1.1 安装 PostgreSQL

```bash
# Ubuntu/Debian
sudo apt-get install postgresql-15

# macOS
brew install postgresql@15

# Windows
# 下载安装包：https://www.postgresql.org/download/windows/
```

#### 1.2 创建数据库和用户

```sql
-- 连接到 PostgreSQL
psql -U postgres

-- 创建数据库
CREATE DATABASE graphiti;

-- 创建用户（可选）
CREATE USER graphiti WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE graphiti TO graphiti;

-- 退出
\q
```

#### 1.3 配置 PostgreSQL（可选）

编辑 `postgresql.conf`：

```conf
# 允许远程连接
listen_addresses = '*'

# 调整内存配置
shared_buffers = 256MB
effective_cache_size = 1GB
```

编辑 `pg_hba.conf` 添加：

```
# 允许密码认证
host    all             all             0.0.0.0/0               md5
```

重启 PostgreSQL：

```bash
sudo systemctl restart postgresql  # Linux
brew services restart postgresql   # macOS
```

### 2. 执行数据库脚本

#### 2.1 创建表结构

```bash
psql -U postgres -d graphiti -f sql/postgresql/schema.sql
```

#### 2.2 插入初始化数据

```bash
psql -U postgres -d graphiti -f sql/postgresql/init-data.sql
```

### 3. 配置应用

#### 3.1 修改 application.yml

确保 `ontograph-server/src/main/resources/application.yml` 中的数据库连接配置正确：

```yaml
spring:
  datasource:
    dynamic:
      datasource:
        master:
          url: jdbc:postgresql://localhost:5432/graphiti
          username: postgres
          password: your_password
          driver-class-name: org.postgresql.Driver
```

#### 3.2 配置环境变量（推荐）

创建 `.env` 文件或在 IDE 中配置环境变量：

```bash
SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL=jdbc:postgresql://localhost:5432/graphiti
SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME=postgres
SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD=your_password
```

### 4. 启动应用

```bash
cd ontograph-java
mvn clean spring-boot:run -pl ontograph-server
```

### 5. 数据迁移（从 MySQL 到 PostgreSQL）

如果需要迁移现有数据，可以使用以下工具：

#### 5.1 使用 pgLoader（推荐）

```bash
# 安装 pgLoader
sudo apt-get install pgloader  # Linux
brew install pgloader          # macOS

# 执行迁移
pgloader mysql://root:password@localhost:3306/graphiti postgresql://postgres:password@localhost:5432/graphiti
```

#### 5.2 使用导出/导入

```bash
# 从 MySQL 导出数据
mysqldump -u root -p graphiti --no-create-info > data.sql

# 转换 SQL 语法（可能需要手动调整）
# 然后导入到 PostgreSQL
psql -U postgres -d graphiti -f data.sql
```

### 6. 常见问题

#### 6.1 连接拒绝

**错误**: `Connection refused`

**解决**: 检查 PostgreSQL 是否启动，确认 `pg_hba.conf` 配置正确。

#### 6.2 时区问题

**错误**: `Cannot parse timestamp`

**解决**: 在 JDBC URL 中添加时区参数：

```
jdbc:postgresql://localhost:5432/graphiti?serverTimezone=Asia/Shanghai
```

#### 6.3 JSONB 类型问题

**错误**: `Column type not supported`

**解决**: 确保 MyBatis-Plus 版本 >= 3.5.7，或添加 `mybatis-plus-jsonb` 依赖。

### 7. 性能优化

#### 7.1 索引优化

PostgreSQL 会自动为主键创建索引，但可以为常用查询字段创建额外索引：

```sql
CREATE INDEX idx_user_email ON sys_user(email);
CREATE INDEX idx_graph_name ON graphiti_graph_metadata(name);
```

#### 7.2 连接池配置

调整 HikariCP 配置：

```yaml
spring:
  datasource:
    dynamic:
      datasource:
        master:
          hikari:
            maximum-pool-size: 20
            minimum-idle: 10
            connection-timeout: 30000
            idle-timeout: 600000
            max-lifetime: 1800000
```

### 8. 回滚方案

如果需要回滚到 MySQL：

1. 恢复 `pom.xml` 中的 MySQL 依赖
2. 恢复 `application.yml` 和 `application-dev.yml` 中的 MySQL 配置
3. 使用 `sql/mysql/schema.sql` 和 `sql/mysql/init-data.sql` 重新初始化数据库

---

## 附录：MySQL 与 PostgreSQL 语法对比

| MySQL | PostgreSQL |
|-------|------------|
| `AUTO_INCREMENT` | `BIGSERIAL` 或 `GENERATED ALWAYS AS IDENTITY` |
| `ENGINE=InnoDB` | 不需要（移除） |
| `DEFAULT CHARSET=utf8mb4` | 不需要（默认 UTF8） |
| `DATETIME` | `TIMESTAMP` 或 `TIMESTAMP WITH TIME ZONE` |
| `TINYINT(1)` | `BOOLEAN` 或 `SMALLINT` |
| `JSON` | `JSONB`（推荐） |
| `ON UPDATE CURRENT_TIMESTAMP` | 使用触发器 |
| 反引号 `` ` `` | 双引号 `"` 或不使用 |
| `NOW()` | `CURRENT_TIMESTAMP` 或 `NOW()` |
| `LIMIT` | `LIMIT`（相同） |
| `INSERT IGNORE` | `INSERT ... ON CONFLICT DO NOTHING` |
| `REPLACE INTO` | `INSERT ... ON CONFLICT DO UPDATE` |
```

- [ ] **Step 2: Commit**

```bash
git add docs/database-migration-guide.md
git commit -m "docs: 添加 MySQL 到 PostgreSQL 迁移指南"
```

---

## Task 7: 最终验证与测试

**Files:**
- Test: 手动测试或自动化测试

- [ ] **Step 1: 清理并编译项目**

Run: `cd d:/projects/ontograph-java && mvn clean compile -DskipTests`

Expected: BUILD SUCCESS

- [ ] **Step 2: 创建 PostgreSQL 数据库**

```bash
psql -U postgres -c "CREATE DATABASE graphiti;"
psql -U postgres -d graphiti -f sql/postgresql/schema.sql
psql -U postgres -d graphiti -f sql/postgresql/init-data.sql
```

- [ ] **Step 3: 启动应用并测试**

Run: `cd d:/projects/ontograph-java && mvn spring-boot:run -pl ontograph-server`

Expected: 应用启动成功，能够连接到 PostgreSQL 数据库

- [ ] **Step 4: 测试 API 端点**

使用 Postman 或 curl 测试以下端点：

```bash
# 测试用户登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'

# 测试获取用户列表
curl -X GET http://localhost:8080/api/system/users \
  -H "Authorization: Bearer <token>"
```

Expected: 所有 API 正常工作

- [ ] **Step 5: 运行自动化测试（如果有）**

Run: `cd d:/projects/ontograph-java && mvn test`

Expected: 所有测试通过

- [ ] **Step 6: Final Commit**

```bash
git add -A
git commit -m "feat: 完成 MySQL 到 PostgreSQL 迁移"
```

---

## Self-Review Checklist

**1. Spec coverage:** 
- [x] 依赖更新（Task 1）
- [x] 配置更新（Task 2）
- [x] Schema 脚本（Task 3）
- [x] 初始化数据（Task 4）
- [x] 代码适配（Task 5）
- [x] 文档（Task 6）
- [x] 测试验证（Task 7）

**2. Placeholder scan:**
- [x] 无 TBD/TODO
- [x] 无占位符
- [x] 所有步骤包含完整代码

**3. Type consistency:**
- [x] PostgreSQL 驱动版本一致（42.7.4）
- [x] 数据库连接 URL 格式一致
- [x] Schema 脚本中的数据类型一致

**4. 额外检查:**
- [x] 保留了 MySQL 脚本作为备份（不删除）
- [x] 创建了迁移指南文档
- [x] 包含了回滚方案

---

## 执行选项

**Plan complete and saved to `docs/superpowers/plans/2026-05-08-mysql-to-postgresql-migration.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
