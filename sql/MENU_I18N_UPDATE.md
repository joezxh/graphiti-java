# 菜单国际化改造说明

## 📅 更新日期
2026-06-16

## 🎯 改造目标

将菜单表（`sys_menu`）的 `name` 字段从硬编码的中文文本改为国际化key，使前端能够通过i18n资源文件获取多语言显示文本。

## ✅ 已完成的修改

### 修改的文件

| 文件 | 数据库 | 状态 |
|------|--------|------|
| [sql/mysql/init-menu-complete.sql](file:///d:/projects/graphiti-java/sql/mysql/init-menu-complete.sql) | MySQL | ✅ 已更新 |
| [sql/postgresql/init-menu-complete.sql](file:///d:/projects/graphiti-java/sql/postgresql/init-menu-complete.sql) | PostgreSQL | ✅ 已更新 |

### 国际化Key映射表

#### 顶级目录菜单（5个）

| 原中文名称 | 国际化Key | 路由路径 | 图标 |
|-----------|----------|---------|------|
| 仪表盘 | `nav.dashboard` | /dashboard | DashboardOutlined |
| 图谱管理 | `nav.graphManagement` | /graph | ShareAltOutlined |
| 数据管理 | `nav.dataManagement` | /data | DatabaseOutlined |
| 工具 | `nav.tools` | /tools | ToolOutlined |
| 系统管理 | `nav.systemManagement` | /system | SettingOutlined |

#### 图谱管理子菜单（4个）

| 原中文名称 | 国际化Key | 路由路径 | 图标 |
|-----------|----------|---------|------|
| 图谱列表 | `nav.graphList` | /graph/list | UnorderedListOutlined |
| 图谱 IDE | `nav.graphIDE` | /graph/ide | CodeOutlined |
| 时序历史 | `nav.temporalHistory` | /graph/temporal | HistoryOutlined |
| 社区检测 | `nav.communityDetection` | /data/communities | ClusterOutlined |

#### 数据管理子菜单（10个）

| 原中文名称 | 国际化Key | 路由路径 | 图标 |
|-----------|----------|---------|------|
| 类管理 | `nav.classManagement` | /data/classes | AppstoreOutlined |
| 属性管理 | `nav.propertyManagement` | /data/properties | TagOutlined |
| 约束管理 | `nav.constraintManagement` | /data/constraints | SafetyCertificateOutlined |
| 实体管理 | `nav.entityManagement` | /data/entities | NodeIndexOutlined |
| 边管理 | `nav.edgeManagement` | /data/edges | LinkOutlined |
| 社区Episode管理 | `nav.communityEpisodeManagement` | /data/community-episode | ClusterOutlined |
| Episode管理 | `nav.episodeManagement` | /data/episodes | FileTextOutlined |
| 数据导入 | `nav.dataImport` | /data/import | ImportOutlined |
| 数据导出 | `nav.dataExport` | /data/export | ExportOutlined |
| 法律知识图谱 | `nav.legalKnowledgeGraph` | /legal-kg | AuditOutlined |

#### 工具子菜单（3个）

| 原中文名称 | 国际化Key | 路由路径 | 图标 |
|-----------|----------|---------|------|
| 混合搜索 | `nav.hybridSearch` | /search | SearchOutlined |
| 自定义指令 | `nav.customInstructions` | /custom-instructions | EditOutlined |
| 提示词管理 | `nav.promptManagement` | /prompt | MessageOutlined |

#### 系统管理子菜单（6个）

| 原中文名称 | 国际化Key | 路由路径 | 图标 |
|-----------|----------|---------|------|
| 用户管理 | `nav.userManagement` | /system/user | UserOutlined |
| 角色管理 | `nav.roleManagement` | /system/role | TeamOutlined |
| 菜单管理 | `nav.menuManagement` | /system/menu | MenuOutlined |
| 系统配置 | `nav.systemConfig` | /system/config | ToolOutlined |
| 操作日志 | `nav.operationLog` | /system/log | FileTextOutlined |
| 系统监控 | `nav.systemMonitor` | /monitor | MonitorOutlined |

## 🔧 前端需要的改动

### 1. 菜单渲染组件

需要修改前端菜单渲染逻辑，使用国际化key获取显示文本。

#### 示例代码（Vue 3）

```vue
<template>
  <a-menu-item :key="menu.url">
    <template #icon>
      <component :is="menu.icon" />
    </template>
    {{ $t(menu.name) }}
  </a-menu-item>
</template>

<script setup>
// menu.name 现在是 'nav.dashboard' 这样的key
// $t() 函数会自动从i18n资源文件中获取对应语言的文本
</script>
```

### 2. i18n资源文件

需要在各语言资源文件中添加对应的翻译。

#### zh-CN.ts（中文）

```typescript
export default {
  nav: {
    navigation: '导航',
    dashboard: '仪表盘',
    graphManagement: '图谱管理',
    graphList: '图谱列表',
    graphIDE: '图谱 IDE',
    temporalHistory: '时序历史',
    communityDetection: '社区检测',
    dataManagement: '数据管理',
    classManagement: '类管理',
    propertyManagement: '属性管理',
    constraintManagement: '约束管理',
    entityManagement: '实体管理',
    edgeManagement: '边管理',
    communityEpisodeManagement: '社区Episode管理',
    episodeManagement: 'Episode管理',
    dataImport: '数据导入',
    dataExport: '数据导出',
    legalKnowledgeGraph: '法律知识图谱',
    tools: '工具',
    hybridSearch: '混合搜索',
    customInstructions: '自定义指令',
    promptManagement: '提示词管理',
    systemManagement: '系统管理',
    userManagement: '用户管理',
    roleManagement: '角色管理',
    menuManagement: '菜单管理',
    systemConfig: '系统配置',
    operationLog: '操作日志',
    systemMonitor: '系统监控'
  }
}
```

#### en-US.ts（英文）

```typescript
export default {
  nav: {
    navigation: 'Navigation',
    dashboard: 'Dashboard',
    graphManagement: 'Graph Management',
    graphList: 'Graph List',
    graphIDE: 'Graph IDE',
    temporalHistory: 'Temporal History',
    communityDetection: 'Community Detection',
    dataManagement: 'Data Management',
    classManagement: 'Class Management',
    propertyManagement: 'Property Management',
    constraintManagement: 'Constraint Management',
    entityManagement: 'Entity Management',
    edgeManagement: 'Edge Management',
    communityEpisodeManagement: 'Community Episode Management',
    episodeManagement: 'Episode Management',
    dataImport: 'Data Import',
    dataExport: 'Data Export',
    legalKnowledgeGraph: 'Legal Knowledge Graph',
    tools: 'Tools',
    hybridSearch: 'Hybrid Search',
    customInstructions: 'Custom Instructions',
    promptManagement: 'Prompt Management',
    systemManagement: 'System Management',
    userManagement: 'User Management',
    roleManagement: 'Role Management',
    menuManagement: 'Menu Management',
    systemConfig: 'System Config',
    operationLog: 'Operation Log',
    systemMonitor: 'System Monitor'
  }
}
```

## 📊 数据库示例

### 修改前

```sql
INSERT INTO sys_menu (name, permission, url, ...) VALUES
('仪表盘', 'dashboard:view', '/dashboard', ...),
('图谱管理', 'graph:manage', '/graph', ...);
```

### 修改后

```sql
INSERT INTO sys_menu (name, permission, url, ...) VALUES
('nav.dashboard', 'dashboard:view', '/dashboard', ...),
('nav.graphManagement', 'graph:manage', '/graph', ...);
```

## ⚠️ 注意事项

### 1. 现有数据迁移

如果数据库中已有菜单数据，需要执行更新脚本：

```sql
-- MySQL
UPDATE sys_menu SET name = 'nav.dashboard' WHERE name = '仪表盘';
UPDATE sys_menu SET name = 'nav.graphManagement' WHERE name = '图谱管理';
UPDATE sys_menu SET name = 'nav.dataManagement' WHERE name = '数据管理';
-- ... 其他菜单

-- PostgreSQL
UPDATE sys_menu SET name = 'nav.dashboard' WHERE name = '仪表盘';
UPDATE sys_menu SET name = 'nav.graphManagement' WHERE name = '图谱管理';
UPDATE sys_menu SET name = 'nav.dataManagement' WHERE name = '数据管理';
-- ... 其他菜单
```

### 2. 前端兼容性

确保前端菜单渲染组件使用 `$t(menu.name)` 或类似的国际化函数，而不是直接显示 `menu.name`。

### 3. 权限树显示

角色管理页面的权限树组件也需要使用国际化key显示菜单名称：

```vue
<a-tree
  :tree-data="menuTreeData"
  :field-names="{ title: 'name', key: 'id', children: 'children' }"
>
  <template #title="{ name }">
    {{ $t(name) }}
  </template>
</a-tree>
```

## 🎯 优势

### 1. 多语言支持
- ✅ 菜单名称支持任意语言
- ✅ 切换语言时菜单自动更新
- ✅ 无需修改数据库数据

### 2. 维护性
- ✅ 翻译集中管理在i18n文件中
- ✅ 新增语言只需添加翻译文件
- ✅ 修改文本无需执行SQL

### 3. 一致性
- ✅ 与前端Sidebar.vue的i18n调用方式一致
- ✅ 遵循项目的国际化规范
- ✅ 便于后续功能扩展

## 📝 后续步骤

1. ✅ **更新SQL脚本** - 已完成
2. ⏳ **更新i18n资源文件** - 需要添加所有nav.*的翻译
3. ⏳ **修改前端组件** - 确保使用$t()渲染菜单名称
4. ⏳ **测试多语言切换** - 验证菜单显示正确
5. ⏳ **更新现有数据** - 如有需要，执行数据迁移脚本

## 📎 相关文件

| 文件 | 路径 |
|------|------|
| MySQL初始化脚本 | [sql/mysql/init-menu-complete.sql](file:///d:/projects/graphiti-java/sql/mysql/init-menu-complete.sql) |
| PostgreSQL初始化脚本 | [sql/postgresql/init-menu-complete.sql](file:///d:/projects/graphiti-java/sql/postgresql/init-menu-complete.sql) |
| 前端Sidebar组件 | [ontograph-frontend/src/components/Layout/Sidebar.vue](file:///d:/projects/graphiti-java/ontograph-frontend/src/components/Layout/Sidebar.vue) |
| 中文i18n文件 | [ontograph-frontend/src/i18n/locales/zh-CN.ts](file:///d:/projects/graphiti-java/ontograph-frontend/src/i18n/locales/zh-CN.ts) |
| 英文i18n文件 | [ontograph-frontend/src/i18n/locales/en-US.ts](file:///d:/projects/graphiti-java/ontograph-frontend/src/i18n/locales/en-US.ts) |

---

**更新日期**: 2026-06-16  
**版本**: 1.0.0  
**状态**: ✅ SQL脚本已完成，前端改动待执行
