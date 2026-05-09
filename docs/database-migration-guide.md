# Graphiti-Java 数据库迁移指南

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

确保 `graphiti-server/src/main/resources/application.yml` 中的数据库连接配置正确：

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
cd graphiti-java
mvn clean spring-boot:run -pl graphiti-server
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
