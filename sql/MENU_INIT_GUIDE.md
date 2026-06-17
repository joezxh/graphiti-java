# 菜单初始化SQL脚本使用指南

## 📅 生成日期
2026-06-16

## 📋 概述

本文档说明如何使用完整菜单初始化SQL脚本，基于前端 `Sidebar.vue` 硬编码的菜单配置生成。

## 🎯 功能特性

### ✅ 包含内容

1. **完整菜单层级结构**
   - 5个顶级目录菜单
   - 24个功能菜单
   - 清晰的父子关系

2. **超级管理员全权限**
   - 自动分配所有菜单权限给 `SUPER_ADMIN` 角色
   - 使用动态查询，不依赖固定ID

3. **数据验证查询**
   - 菜单数量统计
   - 层级结构展示
   - 权限分配验证

### 📊 菜单结构

```
仪表盘 (Dashboard)
├─ 无子菜单

图谱管理 (Graph Management)
├─ 图谱列表
├─ 图谱 IDE
├─ 时序历史
└─ 社区检测

数据管理 (Data Management)
├─ 类管理
├─ 属性管理
├─ 约束管理
├─ 实体管理
├─ 边管理
├─ 社区Episode管理
├─ Episode管理
├─ 数据导入
├─ 数据导出
└─ 法律知识图谱

工具 (Tools)
├─ 混合搜索
├─ 自定义指令
└─ 提示词管理

系统管理 (System Management)
├─ 用户管理
├─ 角色管理
├─ 菜单管理
├─ 系统配置
├─ 操作日志
└─ 系统监控
```

## 📁 文件说明

| 文件 | 数据库 | 路径 |
|------|--------|------|
| MySQL版本 | MySQL 5.7+ | `sql/mysql/init-menu-complete.sql` |
| PostgreSQL版本 | PostgreSQL 12+ | `sql/postgresql/init-menu-complete.sql` |

## 🚀 使用方法

### MySQL

#### 方法1: 命令行执行

```bash
# 连接到MySQL数据库
mysql -u your_username -p your_database

# 执行SQL脚本
source d:/projects/graphiti-java/sql/mysql/init-menu-complete.sql
```

或直接执行：

```bash
mysql -u your_username -p your_database < sql/mysql/init-menu-complete.sql
```

#### 方法2: MySQL Workbench

1. 打开 MySQL Workbench
2. 连接到目标数据库
3. 打开文件：`sql/mysql/init-menu-complete.sql`
4. 点击执行按钮（⚡）

### PostgreSQL

#### 方法1: 命令行执行

```bash
# 连接到PostgreSQL数据库
psql -U your_username -d your_database

# 执行SQL脚本
\i d:/projects/graphiti-java/sql/postgresql/init-menu-complete.sql
```

或直接执行：

```bash
psql -U your_username -d your_database -f sql/postgresql/init-menu-complete.sql
```

#### 方法2: pgAdmin

1. 打开 pgAdmin
2. 连接到目标数据库
3. 打开查询工具
4. 加载文件：`sql/postgresql/init-menu-complete.sql`
5. 点击执行

## 🔧 执行步骤

### 前置条件

1. ✅ 数据库已创建
2. ✅ `sys_menu` 表已创建（通过 schema.sql）
3. ✅ `sys_role` 表已有数据（至少包含 SUPER_ADMIN 角色）
4. ✅ `sys_role_menu` 表已创建

### 执行流程

#### 步骤1: 清理旧数据（可选）

如果数据库中已有菜单数据，需要先清理：

```sql
-- MySQL
DELETE FROM sys_role_menu WHERE role_id = (SELECT id FROM sys_role WHERE code = 'SUPER_ADMIN');
DELETE FROM sys_menu;

-- PostgreSQL
DELETE FROM sys_role_menu WHERE role_id = (SELECT id FROM sys_role WHERE code = 'SUPER_ADMIN');
DELETE FROM sys_menu;
```

#### 步骤2: 执行初始化脚本

按照上述"使用方法"执行对应数据库的SQL脚本。

#### 步骤3: 验证结果

脚本会自动执行验证查询，检查：

1. **菜单数量统计**
   - 目录菜单：5个
   - 功能菜单：24个
   - 总菜单数：29个

2. **层级结构**
   - 查看所有菜单的父子关系
   - 确认 parent_id 正确

3. **权限分配**
   - 超级管理员应该有29个菜单权限

## 📋 菜单数据说明

### 字段定义

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| name | VARCHAR | 菜单名称 | '用户管理' |
| permission | VARCHAR | 权限标识 | 'system:user:list' |
| url | VARCHAR | 路由路径 | '/system/user' |
| parent_id | BIGINT | 父菜单ID，0表示顶级 | 0, 5 |
| sort | INT | 排序号 | 1, 2, 3 |
| type | INT | 菜单类型：1-目录 2-菜单 | 1, 2 |
| icon | VARCHAR | 图标名称 | 'UserOutlined' |
| status | INT | 状态：0-禁用 1-启用 | 1 |
| deleted | BOOLEAN | 逻辑删除标记 | FALSE |

### 权限代码规范

权限代码遵循以下格式：

```
模块:功能:操作
```

示例：
- `system:user:list` - 系统模块的用户列表功能
- `graph:ide` - 图谱模块的IDE功能
- `data:import` - 数据模块的导入功能
- `dashboard:view` - 仪表盘的查看功能

## ⚠️ 注意事项

### 1. ID依赖问题

**问题**: 子菜单使用硬编码的 parent_id（2, 3, 4, 5），这些ID依赖于顶级目录菜单的插入顺序。

**解决方案**:
- 确保顶级目录菜单的 INSERT 语句不改变顺序
- 如果数据库自增ID不是从2开始，需要调整 parent_id

**验证方法**:
```sql
-- 检查顶级目录菜单的ID
SELECT id, name FROM sys_menu WHERE parent_id = 0 ORDER BY sort;
```

期望结果：
```
id | name
---|--------
2  | 图谱管理
3  | 数据管理
4  | 工具
5  | 系统管理
```

### 2. 重复执行

**问题**: 多次执行会导致菜单数据重复。

**解决方案**:
- 执行前先清理旧数据
- 或使用事务包装执行过程

### 3. 数据库兼容性

**MySQL vs PostgreSQL 差异**:
- Boolean值：MySQL使用 `0/1`，PostgreSQL使用 `FALSE/TRUE`
- 变量声明：PostgreSQL需要使用 `DO $$ ... END $$` 块
- 字符串引号：两者都支持单引号

## 🔍 故障排查

### 问题1: 权限树仍然为空

**可能原因**:
1. 菜单数据未插入成功
2. 后端服务未重启
3. 数据库连接配置错误

**解决方法**:
```sql
-- 检查菜单数据是否存在
SELECT COUNT(*) FROM sys_menu WHERE deleted = FALSE;
-- 期望结果：29

-- 检查超级管理员权限
SELECT COUNT(*) FROM sys_role_menu rm
INNER JOIN sys_role r ON rm.role_id = r.id
WHERE r.code = 'SUPER_ADMIN';
-- 期望结果：29
```

### 问题2: 菜单层级关系错误

**可能原因**:
- parent_id 不正确
- 顶级菜单ID不是预期的2,3,4,5

**解决方法**:
```sql
-- 查看所有菜单的父子关系
SELECT 
    m.id,
    m.name,
    m.parent_id,
    p.name AS parent_name
FROM sys_menu m
LEFT JOIN sys_menu p ON m.parent_id = p.id
ORDER BY m.parent_id, m.sort;
```

### 问题3: 权限分配失败

**可能原因**:
- SUPER_ADMIN 角色不存在
- sys_role_menu 表结构不匹配

**解决方法**:
```sql
-- 检查角色是否存在
SELECT id, code, name FROM sys_role WHERE code = 'SUPER_ADMIN';

-- 如果不存在，先创建角色
INSERT INTO sys_role (name, code, status) VALUES ('超级管理员', 'SUPER_ADMIN', 1);
```

## 📝 更新菜单

### 添加新菜单

1. 在对应的 INSERT 语句中添加新菜单记录
2. 确保 parent_id 指向正确的父菜单
3. 重新执行脚本或手动 INSERT

### 修改菜单

```sql
-- 修改菜单名称
UPDATE sys_menu SET name = '新名称' WHERE id = 菜单ID;

-- 修改权限代码
UPDATE sys_menu SET permission = '新权限' WHERE id = 菜单ID;

-- 修改排序
UPDATE sys_menu SET sort = 新排序号 WHERE id = 菜单ID;
```

### 删除菜单

```sql
-- 逻辑删除（推荐）
UPDATE sys_menu SET deleted = TRUE WHERE id = 菜单ID;

-- 物理删除（谨慎使用）
DELETE FROM sys_role_menu WHERE menu_id = 菜单ID;
DELETE FROM sys_menu WHERE id = 菜单ID;
```

## 🎓 最佳实践

1. **执行前备份**: 始终在执行数据初始化脚本前备份数据库
2. **测试环境验证**: 先在测试环境执行，确认无误后再应用于生产环境
3. **版本控制**: 将SQL脚本纳入Git版本控制
4. **事务执行**: 使用事务确保数据一致性
5. **日志记录**: 记录执行结果和可能的错误信息

## 📞 技术支持

如有问题，请检查：
- 数据库版本是否符合要求
- 表结构是否与脚本匹配
- 是否有足够的执行权限

## 📄 相关文档

- [数据库Schema定义](file:///d:/projects/graphiti-java/sql/mysql/schema.sql)
- [前端Sidebar配置](file:///d:/projects/graphiti-java/ontograph-frontend/src/components/Layout/Sidebar.vue)
- [后端MenuController](file:///d:/projects/graphiti-java/ontograph-backend/src/main/java/com/ontograph/system/controller/MenuController.java)

---

**最后更新**: 2026-06-16  
**版本**: 1.0.0  
**维护者**: OntoGraph Team
