# Graphiti(知识图谱)+ System(系统管理)服务 — 端到端浏览器自动化测试 Skill

> 本文档为 AI 浏览器测试 Agent 提供完整的 Graphiti(知识图谱)服务 + System(系统管理)服务的测试提示词。
> 使用 `/browser` 启动浏览器自动化测试,或使用 MCP Playwright 执行测试。
> 适用于 gstack `/qa` 和 `/qa-only` Skill,通过 `/open-gstack-browser` 导入认证 Cookie 后执行端到端测试。
> 文档同时包含问题自动定位与修复建议机制,支持端到端回归测试。
>
> 重点提示:
> 1. **Graphiti 服务** 20 个核心模块的测试提示词 + 开发提示词必须全部完成,覆盖图谱管理、Graph IDE、本体、数据导入/导出/抽取、搜索、提示词工程、时序、Neo4j 集成、Prompt Engineering 等全部业务域。
> 2. **System 服务** 6 个核心模块(SYS-00 登录注销与首页、SYS-01 参数配置、SYS-02 用户管理、SYS-03 角色管理、SYS-04 菜单管理、SYS-05 操作日志)同样需要完成测试提示词 + 开发提示词的编写,其中 SYS-00 必须优先完成以验证系统入口正常。

---

## 目录

- [1.测试前置条件](#测试前置条件)
- [2.全局测试策略](#全局测试策略)
- [3.Skill 调用方式](#skill-调用方式)
- [4.问题发现与自动修复流程](#问题发现与自动修复流程)
- [5.Graphiti 服务测试模块](#graphiti-服务测试模块)
  - [G-01 图谱管理(CRUD + 社区 + 克隆导出)](#g-01-图谱管理)
  - [G-02 Graph IDE — 可视化与画布](#g-02-graph-ide--可视化与画布)
  - [G-03 Graph IDE — Schema 编辑(类/属性/约束)](#g-03-graph-ide--schema-编辑)
  - [G-04 Graph IDE — 节点/边 CRUD](#g-04-graph-ide--节点边-crud)
  - [G-05 Graph IDE — 级联编辑与 Schema 变更校验](#g-05-graph-ide--级联编辑)
  - [G-06 本体管理 — 类/属性/约束 CRUD](#g-06-本体管理--类属性约束)
  - [G-07 本体管理 — 版本历史与回滚](#g-07-本体管理--版本回滚)
  - [G-08 本体管理 — 推理机与一致性检查](#g-08-本体管理--推理机)
  - [G-09 本体管理 — 域规则管理(SpEL)](#g-09-本体管理--域规则)
  - [G-10 元数据管理 — EpisodeType/CommunityType/EntityCategory/RelationshipMeta](#g-10-元数据管理)
  - [G-11 数据导入 — 文本/JSON/批量/消息/事实三元组](#g-11-数据导入)
  - [G-12 数据抽取 — 实体/关系/JSON 预览](#g-12-数据抽取)
  - [G-13 数据导出 — 图谱导出](#g-13-数据导出)
  - [G-14 搜索 — 全局/图谱/混合/BFS/语义](#g-14-搜索)
  - [G-15 搜索管线 — Pipeline/Rerank/Parallel](#g-15-搜索管线)
  - [G-16 Prompt 模板 — CRUD + 版本 + 回滚 + 渲染](#g-16-prompt-模板)
  - [G-17 Prompt 测试 — 执行/抽取/生成样本](#g-17-prompt-测试)
  - [G-18 自定义指令 — 增删查](#g-18-自定义指令)
  - [G-19 时序数据 — Facts/Relationships/History](#g-19-时序数据)
  - [G-20 法律知识图谱 — 抽取/导入/导出](#g-20-法律知识图谱)
  - [SYS-00 登录注销与首页](#sys-00-登录注销与首页)
  - [SYS-01 参数配置](#sys-01-参数配置)
  - [SYS-02 用户管理](#sys-02-用户管理)
  - [SYS-03 角色管理](#sys-03-角色管理)
  - [SYS-04 菜单管理](#sys-04-菜单管理)
  - [SYS-05 操作日志](#sys-05-操作日志)
- [6.测试结果报告模板](#测试结果报告模板)
- [7.模块开发对照与补全清单](#模块开发对照与补全清单)
- [8.文档版本]

---

## 1.测试前置条件

### 1.1 环境要求

| 项目 | 值 |
|------|------|
| 前端地址 | `http://localhost:5173` |
| 后端服务名 | `ontograph-java` |
| 后端端口 | `9090` |
| Neo4j | `bolt://localhost:7687` (user: `neo4j` / password: `password123`) |
| PostgreSQL | `localhost:5432/graphiti` (user: `postgres` / password: `postgres@2026!`) |
| Redis | `localhost:6379` |
| LM Studio (LLM/Embed) | `http://localhost:1234/v1` (OpenAI 兼容) |
| 超级管理员账号 | `admin` |
| 超级管理员密码 | `admin123` |
| 默认租户 ID | `1` |

### 1.2 服务依赖检查

测试前需确认以下服务已启动:

1. **Backend(:9090)** — Spring Boot 主服务
2. **Frontend(:5173)** — Vite 开发服务器
3. **PostgreSQL(:5432)** — 业务元数据
4. **Neo4j(:7687)** — 图数据库
5. **Redis(:6379)** — 缓存 + 嵌入缓存
6. **LM Studio(:1234)** — LLM/Embedding 服务
7. **Spring AI** — Chat/Embedding/Rerank 模型配置

### 1.3 浏览器环境要求

- 浏览器:Chromium / Chrome(headless 模式或带 UI 模式均可)
- Cookie 导入:通过 `/open-gstack-browser` 导入已登录 Cookie,避免重复登录
- 如需手动登录:账号 `admin`,密码 `admin123`,租户 ID `1`
- 注意:Graphiti 服务涉及大量 WebSocket/长连接,Graph IDE 模块需保证 SSE/WebSocket 通道畅通

---

## 2.全局测试策略

### 2.1 模块列表:

| 模块 ID | 模块名称 | 测试场景数 | 说明 |
|---------|---------|-----------|------|
| G-01 | 图谱管理(CRUD + 社区 + 克隆导出) | 12 | 列表/CRUD/删除预览/克隆/导出/搜索/历史 |
| G-02 | Graph IDE — 可视化与画布 | 10 | 可视化加载/类树/实例/邻居展开/布局 |
| G-03 | Graph IDE — Schema 编辑 | 8 | 类/属性/约束 CRUD |
| G-04 | Graph IDE — 节点/边 CRUD | 8 | 节点/边 创建/编辑/删除 |
| G-05 | Graph IDE — 级联编辑 + 变更校验 | 6 | 预览/执行/Schema 变更影响分析 |
| G-06 | 本体管理 — 类/属性/约束 | 10 | CRUD + 层次树 |
| G-07 | 本体管理 — 版本历史与回滚 | 6 | 历史列表/对比/回滚 |
| G-08 | 本体管理 — 推理机 | 5 | 状态/预热/一致性 |
| G-09 | 本体管理 — 域规则(SpEL) | 8 | CRUD/启用切换/表达式测试 |
| G-10 | 元数据管理 | 12 | EpisodeType/CommunityType/EntityCategory/RelationshipMeta CRUD |
| G-11 | 数据导入 | 9 | 文本/JSON/批量/消息/事实三元组 |
| G-12 | 数据抽取 | 8 | 文本/JSON/实体/关系/预览 |
| G-13 | 数据导出 | 4 | 图谱导出/JSON/CSV |
| G-14 | 搜索 | 9 | 全局/图谱/混合/BFS/语义 |
| G-15 | 搜索管线 | 6 | Pipeline/Rerank/Parallel |
| G-16 | Prompt 模板 | 10 | CRUD/版本/回滚/渲染 |
| G-17 | Prompt 测试 | 5 | 执行/抽取/生成样本 |
| G-18 | 自定义指令 | 4 | 增/删/查 |
| G-19 | 时序数据 | 5 | Facts/Relationships/History |
| G-20 | 法律知识图谱 | 12 | 抽取预览/保存/导入/导出 |
| SYS-00 | 登录注销与首页 | 8 | 登录/注销/首页/菜单/重定向 |
| SYS-01 | 参数配置 | 6 | CRUD + 唯一性校验 |
| SYS-02 | 用户管理 | 9 | CRUD + 角色分配 + 状态 |
| SYS-03 | 角色管理 | 8 | CRUD + 菜单权限分配 |
| SYS-04 | 菜单管理 | 9 | 树形 CRUD + 删除保护 |
| SYS-05 | 操作日志 | 10 | 查询 + 详情 + 导出 + 清空 |
| **合计** | **26 个模块** | **205 个场景** | **~13h 测试时间** |

### 2.2 每个测试用例的验证清单

1. **页面加载**:页面是否正常渲染,无白屏、无 JS 错误
2. **数据加载**:表格/图谱数据是否成功加载,loading 状态是否正确消失
3. **交互响应**:按钮点击是否有响应,弹窗/抽屉是否正常弹出
4. **画布渲染**:Graph IDE 页面节点/边是否正确渲染,布局是否合理
5. **可视化效果**:节点 hover/click、边箭头、缩放/平移
6. **表单验证**:必填字段校验是否生效,错误提示是否显示
7. **数据回填**:编辑时已有数据是否正确回填到表单
8. **操作反馈**:成功/失败是否有 message 提示
9. **状态更新**:操作后列表/画布数据是否自动刷新
10. **权限控制**:`v-has-permi` 指令是否正确控制按钮显隐
11. **Neo4j 集成**:节点/边增删后 Neo4j 同步状态
12. **LLM 调用**:涉及 LLM 的接口是否正确返回结果(可能耗时较长)
13. **控制台检查**:浏览器控制台是否有报错或警告

### 2.3 通用测试模式

每个模块遵循 **CRUD 测试循环**:

```
1. 打开页面 → 验证列表/画布加载
2. 搜索/筛选 → 验证条件过滤
3. 点击「新增」→ 验证弹窗/表单初始化 → 填写并提交 → 验证列表/画布刷新
4. 点击「编辑」→ 验证数据回填 → 修改并提交 → 验证更新生效
5. 点击「删除」→ 验证确认弹窗 → 确认删除 → 验证列表/画布更新
6. 边界测试 → 空数据、超长文本、特殊字符、并发操作
```

Graph IDE 类模块额外遵循 **可视化测试循环**:

```
1. 打开 IDE → 验证画布初始化
2. 加载图谱数据 → 验证节点/边渲染
3. 缩放/平移 → 验证画布交互
4. 点击节点 → 验证详情面板
5. 双击节点 → 验证编辑弹窗
6. 拖拽节点 → 验证位置更新
7. 添加邻居 → 验证图谱扩展
```

---

## 3.Skill 调用方式

### 3.1 方式一:使用 gstack /qa Skill(推荐)

```bash
/qa --tier standard --url http://localhost:5173
```

### 3.2 方式二:使用 gstack /open-gstack-browser

```
/open-gstack-browser
启动 GStack 浏览器后,执行以下测试流程...

# 或导入 Cookie 跳过登录
/setup-browser-cookies
选择已登录的 ontograph 会话
执行测试...
```

### 3.3 方式三:直接使用 MCP Playwright

```bash
# 启动浏览器
$B goto http://localhost:5173/login
$B screenshot "login-page.png"

# 登录
$B fill "#username" "admin"
$B fill "#password" "admin123"
$B click "button[type=submit]"

# 验证跳转
$B wait-for-url "**/dashboard"
$B screenshot "after-login.png"

# 进入 Graphiti 模块
$B goto http://localhost:5173/graph/list
$B screenshot "graph-list.png"
```

### 3.4 方式四:使用 /qa-only(仅发现问题不修复)

```
/qa-only --tier exhaustive --scope "Graphiti服务-Graph IDE"
```

---

## 4.问题发现与自动修复流程

### 4.1 三阶段闭环

```
阶段 1:问题发现(Detect)
  → 浏览器测试发现异常
  → 截图记录当前状态
  → 记录控制台错误日志
  → 标记失败测试用例

阶段 2:问题定位(Diagnose)
  → 根据错误信息判断问题层级:
    - 前端渲染问题 → 检查 Vue 组件
    - API 请求失败 → 检查 Network 面板(状态码、响应体)
    - 后端逻辑错误 → 检查后端日志
    - Neo4j 集成问题 → 检查 Neo4j Browser (http://localhost:7474)
    - LLM 调用问题 → 检查 LM Studio 日志
    - 数据问题 → 检查 PostgreSQL/Neo4j 状态
  → 定位到具体文件和行号

阶段 3:自动修复建议(Fix)
  → 输出修复方案(代码 diff)
  → 常见修复模式:
    - 组件未导入 → 补充 import
    - API 路径错误 → 对齐后端路由(/api/v1/...)
    - v-model 绑定问题 → 检查响应式
    - 画布渲染失败 → 检查 ECharts/G6 配置
    - Neo4j 节点未创建 → 检查事务回滚
    - LLM 超时 → 调整超时配置
  → 修复后自动重新执行失败的测试用例
```

### 4.2 常见错误自动匹配表

| 浏览器现象 | 可能原因 | 检查文件 |
|-----------|---------|---------|
| 白屏 | 路由组件未注册 / import 路径错误 | `route-helper.ts`, `views/` |
| 表格无数据 | API 返回 code≠0 / 后端未启动 | `request.ts`, Network 面板 |
| 画布不渲染 | ECharts/G6 初始化失败 | `GraphCanvas.vue`, 控制台 |
| 节点不显示 | Neo4j 节点未创建 | Neo4j Browser, 后端日志 |
| 弹窗打不开 | `v-model:open` / `v-model:visible` 绑定问题 | 对应 FormModal.vue |
| 表单不回填 | watch 未监听 / props 延迟 | FormModal.vue 的 watch |
| 按钮不显示 | 权限码不匹配 | `access.ts`, 后端权限配置 |
| 401 错误 | Token 过期 / 刷新 Token 逻辑失败 | `request.ts`, `auth.ts` |
| 删除失败 | 外键约束 / 关联数据未清理 | 后端 Service |
| 分页异常 | pageNo/pageSize 参数不对 | API 请求参数 |
| LLM 超时 | LM Studio 未启动 / 模型未加载 | LM Studio 界面,后端日志 |
| Neo4j 连接失败 | Neo4j 未启动 / 凭据错误 | `application-dev.yml` 的 neo4j 配置 |
| 嵌入失败 | Embedding 模型未加载 | LM Studio 模式切换 |
| 搜索无结果 | 嵌入缓存失效 / Embedding 模型变更 | Redis, EmbeddingCacheService |
| Schema 校验失败 | 类/属性定义不匹配 | 后端 SchemaManagementService |
| 级联编辑失败 | 筛选条件无匹配 | 后端 CascadeEditService |
| 画布拖拽卡顿 | 节点数过多 / 布局算法耗时 | `GraphCanvas.vue` 的 layout 配置 |
| DictTag 不显示 | 字典类型未注册 / 值为空 | `DictTag.vue`, 后端字典表 |
| 用户名重复 | 后端 UserDO 唯一性约束冲突 | `UserController` create |
| 角色 code 重复 | 后端 RoleDO 唯一性约束冲突 | `RoleController` create |
| configKey 重复 | 后端 SystemConfigDO 唯一性约束冲突 | `SystemConfigController` create |
| 删除有子菜单的菜单 | 后端 MenuService 保护 | `MenuController` delete |
| 删除有用户的角色 | 后端 RoleService 保护 | `RoleController` delete |
| 密码强度不足 | 前端校验提示 + 后端 BCrypt 验证 | `UserFormModal.vue` |
| 角色下拉不显示 | roleApi.getAllRoles() 失败 | `role.ts` 映射 |
| 菜单树不渲染 | menuApi 返回结构异常 | `menu.ts` mapMenuDO |
| 操作日志状态颜色错误 | status 字段映射 | `log.ts` 映射 |
| 导出文件下载失败 | responseType: 'blob' 设置 | `logApi.exportLogs` |
| 权限指令 v-has-permi 不生效 | permission 字段与前端定义不匹配 | `access.ts`, MenuDO.permission |

---

## 5.Graphiti 服务测试模块

---

### G-01 图谱管理

**页面路径**: 左侧菜单「图谱管理」→「图谱列表」
**源码文件**:
- `ontograph-frontend/src/views/graph/list.vue` — 图谱列表
- `ontograph-frontend/src/views/graph/create.vue` — 创建图谱
- `ontograph-frontend/src/views/graph/temporal.vue` — 时序历史
**后端控制器**: `ontograph-backend/.../controller/admin/GraphitiController.java`
**API 文件**: `ontograph-frontend/src/api/graph.ts` (`graphApi`)
**权限标识**: `graphiti:graph:create`, `graphiti:graph:update`, `graphiti:graph:delete`, `graphiti:graph:query`

#### G-01.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| G-01-01 | 图谱列表加载 | 验证分页/搜索/筛选 |
| G-01-02 | 创建图谱 | 表单校验 + 提交 |
| G-01-03 | 编辑图谱 | 数据回填 + 修改提交 |
| G-01-04 | 删除图谱预览 | 显示节点/边/社区统计 |
| G-01-05 | 删除图谱(级联) | Neo4j + MySQL 同步删除 |
| G-01-06 | 清空图谱数据 | 保留元数据,清空数据 |
| G-01-07 | 克隆图谱 | 创建副本 |
| G-01-08 | 导出图谱 | JSON 格式导出 |
| G-01-09 | 构建社区 | 触发社区发现算法 |
| G-01-10 | 社区列表 | 列表/搜索/过滤 |
| G-01-11 | 图谱内搜索 | POST body 形式 |
| G-01-12 | 历史状态查询 | 按时间点查询图谱状态 |

#### G-01.2 测试提示词

```
/browser 或 /open-gstack-browser
打开图谱列表页面,执行完整的图谱管理 CRUD 测试。

【前置操作】
1. 使用 admin/admin123 登录系统
2. 在左侧菜单点击「图谱管理」→「图谱列表」
3. 等待页面加载完成

---

【测试场景 1:图谱列表加载与基础显示】
1. 验证图谱列表正常加载
2. 检查列:图谱ID、图谱名称、描述、节点数、边数、剧集数、创建时间、操作
3. 验证分页功能正常
4. 验证操作列按钮(编辑/克隆/删除预览/删除/导出)
5. 验证顶部「创建图谱」按钮可点击

预期结果:
✅ 表格多列完整
✅ 分页正常
✅ 顶部「创建图谱」按钮存在

---

【测试场景 2:创建图谱】
1. 点击「创建图谱」按钮
2. 验证跳转到 /graph/create 或弹出创建弹窗
3. 验证表单字段:
   - 图谱名称(必填)
   - 描述(可选)
4. 填写:
   - 图谱名称:测试图谱_001
   - 描述:自动化测试创建
5. 提交并验证:
   - API POST /api/v1/graph/create
   - 跳转到 /graph/ide/{graphId} 或返回列表
   - 列表中显示新创建的图谱

预期结果:
✅ 表单字段完整
✅ 提交成功后跳转
✅ 列表中出现新图谱

---

【测试场景 3:编辑图谱】
1. 在列表中点击某个图谱的「编辑」按钮
2. 验证弹窗/抽屉弹出
3. 验证数据回填(图谱名称、描述)
4. 修改图谱名称为「测试图谱_001_modified」
5. 提交并验证:
   - API PUT /api/v1/graph/{graphId}
   - 列表中数据更新

预期结果:
✅ 编辑表单正确回填
✅ 修改后列表更新

---

【测试场景 4:删除图谱预览】
1. 点击某个图谱的「删除预览」按钮
2. 验证弹出删除预览弹窗
3. 验证显示统计信息:
   - 实体节点数(Neo4j)
   - 边数(Neo4j)
   - 剧集数
   - 社区节点数
   - 本体元数据(类/属性/约束数)
4. 验证 hasData 标识

预期结果:
✅ 预览弹窗统计信息完整
✅ 数据从 Neo4j + MySQL 双向统计

---

【测试场景 5:删除图谱(级联)】
1. 在删除预览弹窗中点击「确认删除」
2. 验证二次确认弹窗
3. 确认后:
   - API DELETE /api/v1/graph/{graphId}
   - 显示「删除成功」
   - 列表中该图谱消失
   - Neo4j 中节点/边同步删除
   - MySQL 中元数据逻辑删除

预期结果:
✅ 二次确认正常
✅ Neo4j + MySQL 同步删除
✅ 列表自动刷新

---

【测试场景 6:清空图谱数据】
1. 在图谱详情或列表中点击「清空数据」按钮
2. 验证确认弹窗
3. 确认后:
   - API POST /api/v1/graph/{graphId}/clear
   - 弹窗提示成功
   - 画布/列表中数据清空,但元数据保留

预期结果:
✅ 数据清空成功
✅ 元数据保留

---

【测试场景 7:克隆图谱】
1. 点击某个图谱的「克隆」按钮
2. 验证确认弹窗
3. 确认后:
   - API POST /api/v1/graph/{graphId}/clone
   - 列表中出现新图谱(命名为「xxx_copy」)
   - 跳转到新图谱的 IDE

预期结果:
✅ 克隆成功
✅ 新图谱包含原图谱所有数据

---

【测试场景 8:导出图谱】
1. 点击「导出」按钮
2. 验证触发下载 JSON 文件
3. 验证 JSON 内容包含 nodes、edges、metadata

预期结果:
✅ JSON 文件下载成功
✅ 文件结构完整

---

【测试场景 9:构建社区】
1. 在图谱详情中点击「构建社区」按钮
2. 验证二次确认
3. 触发:
   - API POST /api/v1/graph/{graphId}/communities/build
   - 耗时较长(算法执行)
   - 完成后显示「社区构建成功,共 N 个社区」

预期结果:
✅ 社区构建成功
✅ 返回社区数量

---

【测试场景 10:社区列表】
1. 在图谱详情中点击「社区管理」标签
2. 验证社区列表加载
3. 验证支持搜索、按领域/类型过滤
4. 验证分页(skip/limit)

预期结果:
✅ 社区列表加载完整
✅ 过滤/搜索生效

---

【测试场景 11:图谱内搜索】
1. 进入图谱 IDE
2. 在搜索框输入关键词
3. 触发:
   - API POST /api/v1/graph/{graphId}/search
4. 验证返回节点/边/事实结果
5. 验证高亮显示

预期结果:
✅ 搜索结果正确返回
✅ 结果按相关度排序

---

【测试场景 12:历史状态查询】
1. 访问 /graph/temporal 页面
2. 选择图谱和时间点
3. 触发:
   - API GET /api/v1/graph/{graphId}/history?time=...
4. 验证返回该时间点的节点和边

预期结果:
✅ 历史快照正确返回
✅ 时间选择器工作正常

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 创建图谱 | POST | /api/v1/graph/create |
| 图谱列表 | GET | /api/v1/graph/list |
| 图谱详情 | GET | /api/v1/graph/{graphId} |
| 更新图谱 | PUT | /api/v1/graph/{graphId} |
| 删除预览 | GET | /api/v1/graph/{graphId}/delete-preview |
| 删除图谱 | DELETE | /api/v1/graph/{graphId} |
| 清空数据 | POST | /api/v1/graph/{graphId}/clear |
| 图谱统计(全) | GET | /api/v1/graph/stats |
| 图谱统计(单) | GET | /api/v1/graph/{graphId}/stats |
| 节点列表 | GET | /api/v1/graph/{graphId}/nodes |
| 边列表 | GET | /api/v1/graph/{graphId}/edges |
| 构建社区 | POST | /api/v1/graph/{graphId}/communities/build |
| 社区列表 | GET | /api/v1/graph/{graphId}/communities |
| 社区搜索 | GET | /api/v1/graph/{graphId}/communities/search |
| 删除社区 | DELETE | /api/v1/graph/{graphId}/communities/{communityUuid} |
| 社区列表(分页) | GET | /api/v1/graph/{graphId}/communities/list |
| 创建社区 | POST | /api/v1/graph/{graphId}/communities |
| 更新社区 | PUT | /api/v1/graph/{graphId}/communities/{communityUuid} |
| 克隆图谱 | POST | /api/v1/graph/{graphId}/clone |
| 导出图谱 | GET | /api/v1/graph/{graphId}/export |
| 图谱内搜索 | POST | /api/v1/graph/{graphId}/search |
| 历史查询 | GET | /api/v1/graph/{graphId}/history |

---

【问题诊断】
- 列表不加载 → 检查 graphApi.getList() 是否成功
- 创建后未跳转 → 检查 create.vue 的 router.push
- 删除预览不显示 → 检查 Neo4j 连接
- 级联删除失败 → 检查 Neo4j 事务
- 克隆失败 → 检查 graphId 唯一性
- 社区构建超时 → 检查 Neo4j 数据量 / 算法配置
```

#### G-01.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【图谱管理】模块(若已存在则增强)。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: 左侧菜单「图谱管理」→「图谱列表」
- 路由: /graph/list, /graph/create, /graph/ide/:id, /graph/temporal
- 权限前缀: graphiti:graph:
- 涉及权限码:
  - graphiti:graph:create(创建图谱)
  - graphiti:graph:update(编辑图谱)
  - graphiti:graph:delete(删除图谱)
  - graphiti:graph:query(查询图谱)
  - graphiti:graph:export(导出图谱)
  - graphiti:graph:clone(克隆图谱)

【前端文件清单】
- 列表页: ontograph-frontend/src/views/graph/list.vue
- 创建页: ontograph-frontend/src/views/graph/create.vue
- IDE 页: ontograph-frontend/src/views/graph/ide.vue(已在 G-02 处理)
- 时序页: ontograph-frontend/src/views/graph/temporal.vue
- API 封装: ontograph-frontend/src/api/graph.ts (graphApi 对象)

【后端文件清单】
- Controller: ontograph-backend/src/main/java/com/ontograph/module/graphiti/controller/admin/GraphitiController.java
- Service: .../service/GraphitiService.java + impl
- Service: .../service/GraphNeo4jService.java + impl
- Service: .../service/CommunityService.java + impl
- DO: .../dal/dataobject/GraphMetadataDO.java
- Mapper: .../dal/mysql/GraphMetadataMapper.java

【API 端点】
| 操作 | 方法 | 路径 |
|------|------|------|
| 创建图谱 | POST | /api/v1/graph/create |
| 图谱列表 | GET | /api/v1/graph/list |
| 图谱详情 | GET | /api/v1/graph/{graphId} |
| 更新图谱 | PUT | /api/v1/graph/{graphId} |
| 删除预览 | GET | /api/v1/graph/{graphId}/delete-preview |
| 删除图谱 | DELETE | /api/v1/graph/{graphId} |
| 清空数据 | POST | /api/v1/graph/{graphId}/clear |
| 图谱统计(全) | GET | /api/v1/graph/stats |
| 节点列表 | GET | /api/v1/graph/{graphId}/nodes |
| 边列表 | GET | /api/v1/graph/{graphId}/edges |
| 构建社区 | POST | /api/v1/graph/{graphId}/communities/build |
| 社区列表 | GET | /api/v1/graph/{graphId}/communities |
| 克隆图谱 | POST | /api/v1/graph/{graphId}/clone |
| 导出图谱 | GET | /api/v1/graph/{graphId}/export |
| 图谱内搜索 | POST | /api/v1/graph/{graphId}/search |
| 历史查询 | GET | /api/v1/graph/{graphId}/history |

【功能需求】
1. 支持图谱的创建、查询、更新、删除
2. 删除时执行级联删除(Neo4j 节点/边/社区 + MySQL 元数据逻辑删除)
3. 克隆图谱时创建 graphId 唯一副本,深拷贝所有数据
4. 导出图谱为 JSON 格式(含 nodes/edges/metadata)
5. 图谱内搜索支持语义搜索(LLM Embedding)
6. 历史状态查询基于时序数据

【UI 规范】
- UI 库: ant-design-vue
- 列表字段:图谱ID、图谱名称、描述、节点数、边数、剧集数、创建时间、操作
- 弹窗组件:AntdModal
- 操作按钮:编辑、克隆、删除预览、删除、导出
- 创建图谱按钮:页面顶部

【Neo4j 集成】
- 使用 Neo4j Java Driver
- 节点 Label:`Graph`,`Entity`,`Edge`,`Episode`,`Community`
- 关系 Type:`RELATES_TO`,`MENTIONS`,`MEMBER_OF`,`PARENT_OF`
- 所有 Neo4j 操作需在事务中执行

【参考实现】
- 参考 ontograph-frontend/src/views/system/user/index.vue 的列表+弹窗模式
- 参考 ontograph-frontend/src/views/graph/list.vue 的现有图谱列表
- 复用 ontograph-frontend/src/api/graph.ts 的 graphApi
- Neo4j 操作参考 ontograph-backend/.../service/impl/GraphNeo4jServiceImpl.java

【测试验证】
实现完成后,使用 G-01.2 测试提示词中的测试场景验证,重点验证:
1. 列表 6 列完整渲染
2. 创建/编辑/删除 CRUD 流程
3. 删除预览统计正确(Neo4j + MySQL)
4. 克隆图谱数据完整
5. 导出 JSON 结构完整
6. Neo4j 与 MySQL 同步

【交付物清单】
- [ ] 前端列表页 .vue(list.vue)
- [ ] 前端创建页 .vue(create.vue)
- [ ] 前端 API 封装 .ts(已存在,需增强)
- [ ] 后端 Controller(已存在)
- [ ] 后端 Service 接口 + 实现(已存在)
- [ ] 后端 DO / Mapper(已存在)
- [ ] 路由注册(/graph/list, /graph/create, /graph/temporal)
- [ ] 菜单注册(左侧菜单「图谱管理」)
- [ ] 权限码注册(graphiti:graph:*)
- [ ] Neo4j 集成测试
- [ ] 通过 G-01.2 所有测试场景
```

---

### G-02 Graph IDE — 可视化与画布

**页面路径**: `/graph/ide/:id` 或 `/graph/ide`
**源码文件**:
- `ontograph-frontend/src/views/graph/ide.vue` (主 IDE)
- `ontograph-frontend/src/components/Graph/GraphCanvas.vue` (画布组件)
- `ontograph-frontend/src/components/Graph/NodeEditModal.vue` (节点编辑)
**后端控制器**: `GraphIDEController.java`
**API 文件**: `ontograph-frontend/src/api/graph.ts` (`graphApi.getVisualization*`)
**权限标识**: `graphiti:ide:view`, `graphiti:ide:edit`

#### G-02.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| G-02-01 | IDE 加载 | 选择图谱进入 IDE |
| G-02-02 | 可视化数据加载 | 节点/边渲染 |
| G-02-03 | 画布交互 | 缩放/平移/拖拽 |
| G-02-04 | 类树导航 | 左侧类树点击过滤 |
| G-02-05 | 实例数据查看 | 按类获取实例 |
| G-02-06 | 邻居展开 | 节点右键 → 展开邻居 |
| G-02-07 | 节点详情面板 | 点击节点查看详情 |
| G-02-08 | 布局切换 | 力导向/层次/网格 |
| G-02-09 | 边数据查看 | 单独查看边 |
| G-02-10 | 剧集可视化 | 事件流节点 |

#### G-02.2 测试提示词

```
/browser 或 /open-gstack-browser
打开 Graph IDE 页面,执行完整的可视化与画布交互测试。

【前置操作】
1. 使用 admin/admin123 登录系统
2. 在「图谱列表」中点击某个图谱的「IDE」按钮
3. 等待 IDE 加载完成(可能需要 5-10 秒)

---

【测试场景 1:IDE 加载与画布初始化】
1. 验证 IDE 页面布局:
   - 顶部工具栏(布局切换、保存、布局适配)
   - 左侧类树(Schema Classes)
   - 中央画布
   - 右侧属性面板(选中节点/边时显示)
2. 验证画布加载状态 loading 消失
3. 验证类树正确显示所有类

预期结果:
✅ IDE 四象限布局正常
✅ 类树加载完整
✅ 画布初始化成功

---

【测试场景 2:可视化数据加载】
1. 验证画布自动加载图谱数据
2. 触发 API:
   - GET /api/v1/graph/{graphId}/visualization
3. 验证节点渲染(圆形/不同颜色按类)
4. 验证边渲染(带箭头/标签)
5. 验证聚合信息显示(右下角)

预期结果:
✅ 节点/边正确渲染
✅ 不同类不同颜色
✅ 边带关系类型标签

---

【测试场景 3:画布交互】
1. 测试鼠标滚轮缩放(放大/缩小)
2. 测试鼠标拖拽平移画布
3. 测试点击空白处取消选中
4. 测试拖拽节点改变位置
5. 验证画布适配按钮(适应屏幕/100%/200%)

预期结果:
✅ 缩放/平移流畅
✅ 拖拽节点成功

---

【测试场景 4:类树导航】
1. 在左侧类树中点击某个类
2. 验证画布过滤显示该类节点
3. 验证触发 API:
   - GET /api/v1/graph/{graphId}/visualization/instances?classType=...
4. 验证点击父类展开子类
5. 验证多类过滤(支持逗号分隔)

预期结果:
✅ 类树过滤生效
✅ 画布只显示选中类节点

---

【测试场景 5:实例数据查看】
1. 在类树中选择一个具体类
2. 点击「查看实例」或自动加载
3. 验证分页加载实例数据
4. 验证实例列表与画布同步

预期结果:
✅ 实例数据正确分页
✅ 实例可点击定位到画布

---

【测试场景 6:邻居展开】
1. 在画布中右键点击某个节点
2. 在右键菜单中点击「展开邻居」
3. 验证触发 API:
   - POST /api/v1/graph/{graphId}/nodes/{nodeUuid}/expand
4. 验证画布新增邻居节点和边
5. 验证展开深度(depth)参数生效

预期结果:
✅ 邻居节点正确展开
✅ 展开深度可控

---

【测试场景 7:节点详情面板】
1. 点击画布中的某个节点
2. 验证右侧属性面板显示:
   - 节点名称
   - 节点类型
   - 属性列表(properties)
   - 关联关系列表
3. 验证触发 API:
   - GET /api/v1/graph/{graphId}/nodes/{nodeUuid}
4. 验证双击节点弹出编辑弹窗

预期结果:
✅ 节点详情完整显示
✅ 关联关系列表正确

---

【测试场景 8:布局切换】
1. 在顶部工具栏点击布局下拉框
2. 测试切换:力导向、层次布局、网格布局、圆形布局
3. 验证画布节点位置重新计算
4. 验证切换过程不卡顿(< 3 秒)

预期结果:
✅ 布局切换正常
✅ 节点位置正确重排

---

【测试场景 9:边数据查看】
1. 点击「仅显示边」按钮或选择边模式
2. 验证触发 API:
   - GET /api/v1/graph/{graphId}/visualization/edges
3. 验证画布只显示边和关联实体
4. 测试点击边查看详情

预期结果:
✅ 边数据正确显示
✅ 边详情面板正常

---

【测试场景 10:剧集可视化】
1. 在类树中切换到「剧集」视图
2. 验证触发 API:
   - GET /api/v1/graph/{graphId}/visualization/episodes
3. 验证剧集节点渲染(方形/特殊颜色)
4. 验证剧集与实体的 MENTIONS 关系

预期结果:
✅ 剧集节点正确渲染
✅ MENTIONS 关系显示

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 可视化数据 | GET | /api/v1/graph/{graphId}/visualization |
| 实例数据 | GET | /api/v1/graph/{graphId}/visualization/instances |
| 按类获取实体 | GET | /api/v1/graph/{graphId}/visualization/entities/by-class |
| 按多类获取 | GET | /api/v1/graph/{graphId}/visualization/by-types |
| 边数据 | GET | /api/v1/graph/{graphId}/visualization/edges |
| 剧集可视化 | GET | /api/v1/graph/{graphId}/visualization/episodes |
| 剧集按类型 | GET | /api/v1/graph/{graphId}/visualization/episodes/by-type |
| 社区可视化 | GET | /api/v1/graph/{graphId}/visualization/communities |
| 图谱元数据 | GET | /api/v1/graph/{graphId}/metadata |
| 节点详情 | GET | /api/v1/graph/{graphId}/nodes/{nodeUuid} |
| 展开邻居 | POST | /api/v1/graph/{graphId}/nodes/{nodeUuid}/expand |

---

【问题诊断】
- 画布不渲染 → 检查 ECharts/G6 初始化 / 控制台错误
- 节点加载缓慢 → 检查 Neo4j 索引 / 数据量
- 类树空白 → 检查 Schema API 返回
- 邻居展开失败 → 检查 expand API / Neo4j 查询
```

#### G-02.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【Graph IDE 可视化】模块。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: /graph/ide/:id
- 权限前缀: graphiti:ide:
- 涉及权限码:
  - graphiti:ide:view(查看 IDE)
  - graphiti:ide:edit(编辑节点/边)

【前端文件清单】
- 主页面: ontograph-frontend/src/views/graph/ide.vue
- 画布组件: ontograph-frontend/src/components/Graph/GraphCanvas.vue
- 节点编辑弹窗: ontograph-frontend/src/components/Graph/NodeEditModal.vue
- API 封装: ontograph-frontend/src/api/graph.ts (graphApi)

【后端文件清单】
- Controller: .../controller/admin/GraphIDEController.java
- Service: .../service/GraphVisualizationService.java + impl
- Service: .../service/GraphNeo4jService.java + impl
- Service: .../service/CommunityService.java + impl

【API 端点】
| 操作 | 方法 | 路径 |
|------|------|------|
| 可视化数据 | GET | /api/v1/graph/{graphId}/visualization |
| 实例数据 | GET | /api/v1/graph/{graphId}/visualization/instances |
| 按类获取实体 | GET | /api/v1/graph/{graphId}/visualization/entities/by-class |
| 边数据 | GET | /api/v1/graph/{graphId}/visualization/edges |
| 剧集可视化 | GET | /api/v1/graph/{graphId}/visualization/episodes |
| 剧集按类型 | GET | /api/v1/graph/{graphId}/visualization/episodes/by-type |
| 社区可视化 | GET | /api/v1/graph/{graphId}/visualization/communities |
| 图谱元数据 | GET | /api/v1/graph/{graphId}/metadata |
| 节点详情 | GET | /api/v1/graph/{graphId}/nodes/{nodeUuid} |
| 展开邻居 | POST | /api/v1/graph/{graphId}/nodes/{nodeUuid}/expand |

【功能需求】
1. 支持图谱可视化(节点/边渲染)
2. 支持多种布局(力导向/层次/网格/圆形)
3. 支持类树导航与过滤
4. 支持邻居展开(N 跳)
5. 支持节点详情面板
6. 支持剧集可视化(事件流)
7. 支持分页加载(避免一次性加载过多节点)

【UI 规范】
- 可视化库: ECharts / AntV G6
- 节点渲染:不同类不同颜色/形状
- 边渲染:带箭头/标签
- 画布交互:缩放/平移/拖拽
- 性能要求:1000+ 节点流畅渲染

【Neo4j 集成】
- 使用 Cypher 查询获取节点/边
- 限制返回数量(分页/limit)
- 支持按类、按关键词过滤

【测试验证】
实现完成后,使用 G-02.2 测试提示词中的测试场景验证。

【交付物清单】
- [ ] 前端主页面 .vue(ide.vue)
- [ ] 前端画布组件 .vue(GraphCanvas.vue)
- [ ] 前端 API 封装 .ts(已存在)
- [ ] 后端 Controller(已存在)
- [ ] 后端 Service 接口 + 实现(已存在)
- [ ] 通过 G-02.2 所有测试场景
```

---

### G-03 Graph IDE — Schema 编辑

**页面路径**: `/graph/ide/:id`(Schema 标签)
**源码文件**:
- `ontograph-frontend/src/views/graph/ide.vue`
- `ontograph-frontend/src/components/Graph/SchemaEditPanel.vue`(可能)
**后端控制器**: `GraphIDEController.java` (`/api/v1/graph/{graphId}/ontology/...`)
**API 文件**: `ontograph-frontend/src/api/graph.ts` (`graphApi.getSchemaClasses*`)

#### G-03.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| G-03-01 | Schema 类列表 | 显示所有本体类 |
| G-03-02 | 创建类 | 新增本体类 |
| G-03-03 | 编辑类 | 修改类名/描述/父类 |
| G-03-04 | 删除类 | 删除本体类 |
| G-03-05 | 类属性列表 | 查看类的属性 |
| G-03-06 | 创建属性 | 为类添加属性 |
| G-03-07 | 编辑属性 | 修改属性 |
| G-03-08 | 删除属性 | 删除属性 |

#### G-03.2 测试提示词

```
/browser 或 /open-gstack-browser
打开 Graph IDE,切换到 Schema 标签,执行完整的 Schema 编辑测试。

【前置操作】
1. 使用 admin/admin123 登录系统
2. 进入某个图谱的 IDE
3. 切换到「Schema」标签

---

【测试场景 1:Schema 类列表】
1. 验证类列表加载
2. 触发 API:
   - GET /api/v1/graph/{graphId}/ontology/classes
3. 验证列:类ID、类URI、本地名、描述、父类数、属性数、操作
4. 验证支持创建顶级类/子类

预期结果:
✅ 类列表加载完整
✅ 类继承关系显示

---

【测试场景 2:创建类】
1. 点击「创建类」按钮
2. 验证弹窗字段:
   - 本地名(必填,英文)
   - 描述(可选)
   - 父类(可多选)
3. 填写:
   - 本地名:TestClass
   - 描述:自动化测试类
4. 提交:
   - POST /api/v1/graph/{graphId}/ontology/classes
5. 验证列表刷新

预期结果:
✅ 类创建成功
✅ 列表中出现新类

---

【测试场景 3:编辑类】
1. 点击某个类的「编辑」
2. 验证数据回填
3. 修改描述
4. 提交:
   - PUT /api/v1/graph/{graphId}/ontology/classes/{classId}

预期结果:
✅ 编辑成功
✅ 列表更新

---

【测试场景 4:删除类】
1. 点击「删除」
2. 验证确认弹窗
3. 确认后:
   - DELETE /api/v1/graph/{graphId}/ontology/classes/{classId}
4. 验证列表移除

预期结果:
✅ 删除成功
✅ 若类有实例则拒绝删除(后端保护)

---

【测试场景 5:类属性列表】
1. 点击某个类的「查看属性」或展开
2. 触发 API:
   - GET /api/v1/graph/{graphId}/ontology/classes/{classId}/properties
3. 验证属性列表

预期结果:
✅ 属性列表加载

---

【测试场景 6:创建属性】
1. 在类详情中点击「添加属性」
2. 验证弹窗字段:
   - 本地名(必填)
   - 属性类型(DATATYPE/OBJECT/ANNOTATION)
   - 范围数据类型(string/int/date/...)
   - 是否必填
   - 是否多值
   - 默认值/允许值/正则
3. 填写并提交:
   - POST /api/v1/graph/{graphId}/ontology/classes/{classId}/properties

预期结果:
✅ 属性创建成功

---

【测试场景 7:编辑属性】
1. 点击属性「编辑」
2. 验证回填
3. 修改后提交:
   - PUT /api/v1/graph/{graphId}/ontology/classes/{classId}/properties/{propertyId}

预期结果:
✅ 编辑成功

---

【测试场景 8:删除属性】
1. 点击「删除」属性
2. 确认后:
   - DELETE /api/v1/graph/{graphId}/ontology/classes/{classId}/properties/{propertyId}

预期结果:
✅ 删除成功

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 类列表 | GET | /api/v1/graph/{graphId}/ontology/classes |
| 类详情 | GET | /api/v1/graph/{graphId}/ontology/classes/{classId} |
| 创建类 | POST | /api/v1/graph/{graphId}/ontology/classes |
| 更新类 | PUT | /api/v1/graph/{graphId}/ontology/classes/{classId} |
| 删除类 | DELETE | /api/v1/graph/{graphId}/ontology/classes/{classId} |
| 属性列表 | GET | /api/v1/graph/{graphId}/ontology/classes/{classId}/properties |
| 创建属性 | POST | /api/v1/graph/{graphId}/ontology/classes/{classId}/properties |
| 更新属性 | PUT | /api/v1/graph/{graphId}/ontology/classes/{classId}/properties/{propertyId} |
| 删除属性 | DELETE | /api/v1/graph/{graphId}/ontology/classes/{classId}/properties/{propertyId} |

---

【问题诊断】
- 类列表空白 → 检查 Neo4j 连接 / Schema 数据
- 创建类失败 → 检查 OntologyClassService
- 删除类失败(有实例) → 后端保护,提示「该类存在实例,无法删除」
```

#### G-03.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【Graph IDE Schema 编辑】模块。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: /graph/ide/:id(Schema 标签)
- 权限前缀: graphiti:schema:

【前端文件清单】
- 主页面: ontograph-frontend/src/views/graph/ide.vue(Schema Tab)
- API 封装: ontograph-frontend/src/api/graph.ts

【后端文件清单】
- Controller: .../controller/admin/GraphIDEController.java
- Service: .../service/SchemaManagementService.java + impl

【API 端点】
| 操作 | 方法 | 路径 |
|------|------|------|
| 类列表 | GET | /api/v1/graph/{graphId}/ontology/classes |
| 类详情 | GET | /api/v1/graph/{graphId}/ontology/classes/{classId} |
| 创建类 | POST | /api/v1/graph/{graphId}/ontology/classes |
| 更新类 | PUT | /api/v1/graph/{graphId}/ontology/classes/{classId} |
| 删除类 | DELETE | /api/v1/graph/{graphId}/ontology/classes/{classId} |
| 属性列表 | GET | /api/v1/graph/{graphId}/ontology/classes/{classId}/properties |
| 创建属性 | POST | /api/v1/graph/{graphId}/ontology/classes/{classId}/properties |
| 更新属性 | PUT | /api/v1/graph/{graphId}/ontology/classes/{classId}/properties/{propertyId} |
| 删除属性 | DELETE | /api/v1/graph/{graphId}/ontology/classes/{classId}/properties/{propertyId} |

【功能需求】
1. Schema 类 CRUD(含继承关系)
2. 类属性 CRUD(含数据类型/范围)
3. 删除类时检查是否存在实例
4. 支持必填/多值/默认值/正则约束

【UI 规范】
- UI 库: ant-design-vue
- 列表使用 Table 组件
- 表单使用 Modal + Form

【Neo4j 集成】
- 使用 Neo4j 节点存储类与属性定义
- 类继承关系使用 PARENT_OF 边
- 触发 Schema 变更校验(影响现有数据)

【测试验证】
实现完成后,使用 G-03.2 测试提示词中的测试场景验证。

【交付物清单】
- [ ] 前端 Schema Tab(已在 ide.vue 内)
- [ ] 后端 Controller(已存在)
- [ ] 后端 Service 接口 + 实现(已存在)
- [ ] 通过 G-03.2 所有测试场景
```

---

### G-04 Graph IDE — 节点/边 CRUD

**页面路径**: `/graph/ide/:id`
**源码文件**:
- `ontograph-frontend/src/views/graph/ide.vue`
- `ontograph-frontend/src/components/Graph/NodeEditModal.vue`
- `ontograph-frontend/src/components/Graph/EdgeEditModal.vue`
**后端控制器**: `GraphIDEController.java`
**API 文件**: `ontograph-frontend/src/api/graph.ts`

#### G-04.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| G-04-01 | 创建节点 | 通过工具栏创建 |
| G-04-02 | 编辑节点 | 双击节点编辑 |
| G-04-03 | 删除节点 | 右键删除 |
| G-04-04 | 创建边 | 拖拽创建 |
| G-04-05 | 编辑边 | 双击边编辑 |
| G-04-06 | 删除边 | 右键删除 |
| G-04-07 | 节点位置持久化 | 拖拽后位置保存 |
| G-04-08 | 节点属性编辑 | 动态属性 CRUD |

#### G-04.2 测试提示词

```
/browser 或 /open-gstack-browser
打开 Graph IDE,执行节点/边 CRUD 测试。

【前置操作】
1. 使用 admin/admin123 登录
2. 进入某个图谱的 IDE

---

【测试场景 1:创建节点】
1. 在工具栏点击「添加节点」按钮
2. 验证弹窗字段:
   - 节点名称(必填)
   - 节点类型(类下拉)
   - 属性(properties)
3. 填写:
   - 名称:TestNode_001
   - 类型:TestClass(G-03 创建)
4. 提交:
   - POST /api/v1/graph/{graphId}/nodes
5. 验证画布出现新节点

预期结果:
✅ 节点创建成功
✅ 画布自动更新

---

【测试场景 2:编辑节点】
1. 双击画布中的某个节点
2. 验证 NodeEditModal 弹出
3. 验证数据回填(名称/类型/属性)
4. 修改名称为 TestNode_001_modified
5. 提交:
   - PUT /api/v1/graph/{graphId}/nodes/{nodeUuid}

预期结果:
✅ 编辑成功
✅ 画布节点名称更新

---

【测试场景 3:删除节点】
1. 右键画布中的某个节点
2. 在右键菜单点击「删除」
3. 验证确认弹窗
4. 确认后:
   - DELETE /api/v1/graph/{graphId}/nodes/{nodeUuid}
5. 验证画布节点消失,关联边也删除

预期结果:
✅ 节点删除成功
✅ 级联删除关联边

---

【测试场景 4:创建边】
1. 在工具栏选择「添加边」模式
2. 拖拽从一个节点到另一个节点
3. 验证边编辑弹窗:
   - 源节点(自动)
   - 目标节点(自动)
   - 关系类型
   - 事实(fact)
4. 填写关系类型:RELATES_TO
5. 提交:
   - POST /api/v1/graph/{graphId}/edges

预期结果:
✅ 边创建成功
✅ 画布出现新边

---

【测试场景 5:编辑边】
1. 双击画布中的边
2. 验证 EdgeEditModal 弹出
3. 修改关系类型或 fact
4. 提交:
   - PUT(通过 graphApi 或自定义)

预期结果:
✅ 边编辑成功

---

【测试场景 6:删除边】
1. 右键画布中的边
2. 点击「删除」
3. 确认后删除

预期结果:
✅ 边删除成功
✅ 节点保留

---

【测试场景 7:节点位置持久化】
1. 拖拽画布中的节点到新位置
2. 刷新页面
3. 验证节点位置保持

预期结果:
✅ 位置持久化

---

【测试场景 8:节点属性编辑】
1. 在 NodeEditModal 中点击「添加属性」
2. 填写 key-value
3. 提交后验证属性保存

预期结果:
✅ 属性动态编辑

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 创建节点 | POST | /api/v1/graph/{graphId}/nodes |
| 更新节点 | PUT | /api/v1/graph/{graphId}/nodes/{nodeUuid} |
| 删除节点 | DELETE | /api/v1/graph/{graphId}/nodes/{nodeUuid} |
| 创建边 | POST | /api/v1/graph/{graphId}/edges |
| 删除节点(级联) | DELETE | /api/v1/graph/{graphId}/nodes/{nodeUuid}(自动级联边) |

---

【问题诊断】
- 创建失败 → 检查 Neo4j 事务
- 编辑不回填 → 检查 NodeEditModal 的 watch
- 删除未级联 → 检查 Neo4j DETACH DELETE
```

#### G-04.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【Graph IDE 节点/边 CRUD】模块。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: /graph/ide/:id
- 权限前缀: graphiti:node:, graphiti:edge:

【前端文件清单】
- 主页面: ontograph-frontend/src/views/graph/ide.vue
- 节点编辑: ontograph-frontend/src/components/Graph/NodeEditModal.vue
- 边编辑: ontograph-frontend/src/components/Graph/EdgeEditModal.vue
- API 封装: ontograph-frontend/src/api/graph.ts

【后端文件清单】
- Controller: .../controller/admin/GraphIDEController.java
- Service: .../service/GraphVisualizationService.java + impl

【API 端点】
| 操作 | 方法 | 路径 |
|------|------|------|
| 创建节点 | POST | /api/v1/graph/{graphId}/nodes |
| 更新节点 | PUT | /api/v1/graph/{graphId}/nodes/{nodeUuid} |
| 删除节点 | DELETE | /api/v1/graph/{graphId}/nodes/{nodeUuid} |
| 创建边 | POST | /api/v1/graph/{graphId}/edges |

【功能需求】
1. 节点 CRUD(创建/编辑/删除)
2. 边 CRUD
3. 删除节点时级联删除关联边
4. 节点位置持久化(x/y 坐标)
5. 动态属性编辑

【UI 规范】
- 编辑弹窗:AntdModal
- 拖拽:HTML5 drag/drop
- 画布:G6/ECharts

【Neo4j 集成】
- 节点创建:CREATE (n:Entity {uuid, name, type, properties, x, y})
- 边创建:MATCH (a), (b) CREATE (a)-[r:RELATES_TO {uuid, type, fact}]->(b)
- 删除节点:DETACH DELETE(n)

【测试验证】
实现完成后,使用 G-04.2 测试提示词中的测试场景验证。

【交付物清单】
- [ ] 前端主页面 .vue(ide.vue)
- [ ] 前端节点编辑弹窗 .vue(NodeEditModal.vue)
- [ ] 前端边编辑弹窗 .vue(EdgeEditModal.vue)
- [ ] 后端 Controller(已存在)
- [ ] 通过 G-04.2 所有测试场景
```

---

### G-05 Graph IDE — 级联编辑

**页面路径**: `/graph/ide/:id` (级联编辑标签)
**源码文件**: `ontograph-frontend/src/views/graph/ide.vue`
**后端控制器**: `GraphIDEController.java` (`/api/v1/graph/{graphId}/cascade/...`)
**API 文件**: `ontograph-frontend/src/api/graph.ts`

#### G-05.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| G-05-01 | 级联筛选器构建 | 选择类/属性/操作符 |
| G-05-02 | 预览级联影响 | 显示匹配节点数和分布 |
| G-05-03 | 执行级联编辑 | 批量更新 |
| G-05-04 | Schema 变更校验 | UPDATE_CLASS |
| G-05-05 | Schema 变更校验 | DELETE_PROPERTY |
| G-05-06 | Schema 变更校验 | ADD_REQUIRED_PROPERTY |

#### G-05.2 测试提示词

```
/browser 或 /open-gstack-browser
打开 Graph IDE,执行级联编辑测试。

【前置操作】
1. 使用 admin/admin123 登录
2. 进入某个图谱的 IDE(确保有数据)
3. 切换到「级联编辑」标签

---

【测试场景 1:级联筛选器构建】
1. 验证筛选器字段:
   - 类(ClassType 下拉)
   - 条件列表(属性名/操作符/值)
   - 逻辑(AND/OR)
2. 添加条件:propertyName=age, operator=gte, value=18
3. 添加第二个条件:propertyName=country, operator=eq, value=CN
4. 选择逻辑 AND
5. 验证 UI 状态

预期结果:
✅ 筛选器字段完整
✅ 条件可动态增删

---

【测试场景 2:预览级联影响】
1. 点击「预览」按钮
2. 验证触发 API:
   - POST /api/v1/graph/{graphId}/cascade/preview
3. 验证返回:
   - totalMatch(总匹配数)
   - distribution(分布,按 groupBy 统计)
4. 验证 UI 显示匹配数和分布图

预期结果:
✅ 预览返回正确
✅ 分布图渲染正常

---

【测试场景 3:执行级联编辑】
1. 在筛选器下方填写 updates(要更新的属性)
2. 例如:update propertyName=status, value=active
3. 点击「执行」按钮
4. 验证二次确认
5. 确认后:
   - POST /api/v1/graph/{graphId}/cascade/execute
6. 验证返回:
   - success(是否成功)
   - affectedCount(影响数)
   - failedCount(失败数)
   - errors(错误列表)
7. 验证画布节点属性已更新

预期结果:
✅ 级联执行成功
✅ 节点属性批量更新

---

【测试场景 4:Schema 变更校验 - UPDATE_CLASS】
1. 在 Schema 编辑器中修改某个类的 localName
2. 点击「校验变更影响」按钮
3. 验证触发 API:
   - POST /api/v1/graph/{graphId}/ontology/validate-change
4. 验证返回:
   - compatible(是否兼容)
   - affectedNodes(受影响节点数)
   - violations(违规列表)
5. 验证 UI 显示校验结果

预期结果:
✅ 校验成功
✅ 影响范围显示

---

【测试场景 5:Schema 变更校验 - DELETE_PROPERTY】
1. 在 Schema 编辑器中删除某个属性
2. 触发校验
3. 验证返回 violations 包含被影响节点

预期结果:
✅ 校验成功
✅ 显示受影响的节点

---

【测试场景 6:Schema 变更校验 - ADD_REQUIRED_PROPERTY】
1. 在 Schema 编辑器中新增一个必填属性
2. 触发校验
3. 验证返回:已有实例缺失该必填属性,列出违规

预期结果:
✅ 校验成功
✅ 显示缺失必填属性的实例

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 预览级联 | POST | /api/v1/graph/{graphId}/cascade/preview |
| 执行级联 | POST | /api/v1/graph/{graphId}/cascade/execute |
| 校验变更 | POST | /api/v1/graph/{graphId}/ontology/validate-change |

---

【问题诊断】
- 预览失败 → 检查 Neo4j 查询
- 执行失败 → 检查事务回滚
- 校验耗时 → 检查数据量
```

#### G-05.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【Graph IDE 级联编辑】模块。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: /graph/ide/:id
- 权限前缀: graphiti:cascade:, graphiti:schema:validate

【前端文件清单】
- 主页面: ontograph-frontend/src/views/graph/ide.vue
- API 封装: ontograph-frontend/src/api/graph.ts

【后端文件清单】
- Controller: .../controller/admin/GraphIDEController.java
- Service: .../service/CascadeEditService.java + impl
- Service: .../service/SchemaManagementService.java + impl

【API 端点】
| 操作 | 方法 | 路径 |
|------|------|------|
| 预览级联 | POST | /api/v1/graph/{graphId}/cascade/preview |
| 执行级联 | POST | /api/v1/graph/{graphId}/cascade/execute |
| 校验变更 | POST | /api/v1/graph/{graphId}/ontology/validate-change |

【功能需求】
1. 筛选器构建(类/条件/逻辑)
2. 预览匹配节点数和分布
3. 批量更新匹配节点属性
4. Schema 变更影响校验
5. 操作符支持:eq, ne, gt, lt, gte, lte, contains, in, is_null 等

【Neo4j 集成】
- 预览:使用 OPTIONAL MATCH 查询匹配节点
- 执行:UNWIND 批量更新
- 校验:查询现有数据是否满足新 Schema

【测试验证】
实现完成后,使用 G-05.2 测试提示词中的测试场景验证。

【交付物清单】
- [ ] 前端级联编辑 Tab(已在 ide.vue 内)
- [ ] 后端 Service(已存在)
- [ ] 通过 G-05.2 所有测试场景
```

---

### G-06 本体管理 — 类/属性/约束

**页面路径**:
- `/data/classes` — 类管理
- `/data/properties` — 属性管理
- `/data/constraints` — 约束管理
**源码文件**:
- `ontograph-frontend/src/views/data/classes.vue`
- `ontograph-frontend/src/views/data/properties.vue`
- `ontograph-frontend/src/views/data/constraints.vue`
**后端控制器**: `OntologyController.java`
**API 文件**: `ontograph-frontend/src/api/ontology.ts`

#### G-06.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| G-06-01 | 本体定义查看 | 查看图谱本体定义 |
| G-06-02 | 本体定义创建 | 创建新版本 |
| G-06-03 | 本体完整信息 | 一次性获取类/属性/约束 |
| G-06-04 | 批量验证 | 对节点/边批量本体验证 |
| G-06-05 | 类列表 | 平铺所有类 |
| G-06-06 | 类层次树 | 树形结构 |
| G-06-07 | 类 CRUD | 创建/更新/删除 |
| G-06-08 | 属性 CRUD | 创建/更新/删除 |
| G-06-09 | 约束 CRUD | 创建/更新/删除 |
| G-06-10 | Schema.org 导入 | 从 Schema.org 导入 |

#### G-06.2 测试提示词

```
/browser 或 /open-gstack-browser
打开本体管理,执行 CRUD 测试。

【前置操作】
1. 使用 admin/admin123 登录
2. 进入「数据管理」→「类管理」

---

【测试场景 1:本体定义查看】
1. 在「类管理」页面选择某个图谱
2. 验证触发 API:
   - GET /api/v1/ontology/{graphId}/definition
3. 验证显示:定义ID、版本号、状态、创建时间

预期结果:
✅ 本体定义加载

---

【测试场景 2:本体定义创建】
1. 点击「创建新版本」
2. 填写版本号
3. 提交:
   - POST /api/v1/ontology/{graphId}/definition
4. 验证新版本出现

预期结果:
✅ 新版本创建

---

【测试场景 3:本体完整信息】
1. 验证触发 API:
   - GET /api/v1/ontology/{graphId}
2. 验证返回 OntologyFullVO(类/属性/约束全量)

预期结果:
✅ 完整信息返回

---

【测试场景 4:批量验证】
1. 上传或选择一批节点/边
2. 点击「批量本体验证」
3. 触发 API:
   - POST /api/v1/ontology/{graphId}/validate/batch
4. 验证返回 violations 列表

预期结果:
✅ 批量验证执行

---

【测试场景 5:类列表】
1. 验证 GET /api/v1/ontology/{graphId}/classes
2. 验证列:类ID、URI、本地名、父类、属性数、约束数

预期结果:
✅ 类列表加载

---

【测试场景 6:类层次树】
1. 验证 GET /api/v1/ontology/{graphId}/classes/hierarchy
2. 验证树形结构(父子关系)

预期结果:
✅ 层次树渲染

---

【测试场景 7:类 CRUD】
1. 创建类:POST /api/v1/ontology/{graphId}/classes
2. 更新类:PUT /api/v1/ontology/{graphId}/classes/{classId}
3. 删除类:DELETE /api/v1/ontology/{graphId}/classes/{classId}(若有子类则拒绝)

预期结果:
✅ CRUD 完整

---

【测试场景 8:属性 CRUD】
1. 列出属性:GET /api/v1/ontology/{graphId}/properties
2. 创建:POST
3. 更新:PUT
4. 删除:DELETE

预期结果:
✅ 属性 CRUD 完整

---

【测试场景 9:约束 CRUD】
1. 列出约束:GET /api/v1/ontology/{graphId}/constraints
2. 创建/更新/删除约束

预期结果:
✅ 约束 CRUD 完整

---

【测试场景 10:Schema.org 导入】
1. 在「本体管理」中点击「从 Schema.org 导入」
2. 输入领域(如 Person, Organization)
3. 触发:
   - POST /api/v1/ontology/{graphId}/import/schema-org
4. 验证返回 imported counts

预期结果:
✅ Schema.org 导入成功

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 获取本体定义 | GET | /api/v1/ontology/{graphId}/definition |
| 创建本体定义 | POST | /api/v1/ontology/{graphId}/definition |
| 完整本体 | GET | /api/v1/ontology/{graphId} |
| 批量验证 | POST | /api/v1/ontology/{graphId}/validate/batch |
| 类列表 | GET | /api/v1/ontology/{graphId}/classes |
| 类层次树 | GET | /api/v1/ontology/{graphId}/classes/hierarchy |
| 创建类 | POST | /api/v1/ontology/{graphId}/classes |
| 更新类 | PUT | /api/v1/ontology/{graphId}/classes/{classId} |
| 删除类 | DELETE | /api/v1/ontology/{graphId}/classes/{classId} |
| 属性列表 | GET | /api/v1/ontology/{graphId}/properties |
| 创建属性 | POST | /api/v1/ontology/{graphId}/properties |
| 更新属性 | PUT | /api/v1/ontology/{graphId}/properties/{propertyId} |
| 删除属性 | DELETE | /api/v1/ontology/{graphId}/properties/{propertyId} |
| 约束列表 | GET | /api/v1/ontology/{graphId}/constraints |
| 创建约束 | POST | /api/v1/ontology/{graphId}/constraints |
| 更新约束 | PUT | /api/v1/ontology/{graphId}/constraints/{constraintId} |
| 删除约束 | DELETE | /api/v1/ontology/{graphId}/constraints/{constraintId} |
| Schema.org 导入 | POST | /api/v1/ontology/{graphId}/import/schema-org |

---

【问题诊断】
- 类列表空白 → 检查 ontologyApi / 后端
- 删除类失败 → 后端保护
- 层次树渲染失败 → 检查 tree 组件数据
```

#### G-06.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【本体管理 — 类/属性/约束】模块。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: /data/classes, /data/properties, /data/constraints
- 权限前缀: graphiti:ontology:

【前端文件清单】
- 类管理: ontograph-frontend/src/views/data/classes.vue
- 属性管理: ontograph-frontend/src/views/data/properties.vue
- 约束管理: ontograph-frontend/src/views/data/constraints.vue
- API 封装: ontograph-frontend/src/api/ontology.ts

【后端文件清单】
- Controller: .../controller/admin/OntologyController.java
- Service: .../service/OntologyClassService.java + impl
- Service: .../service/OntologyPropertyService.java + impl
- DO/Mapper: 见 dataobject/ont 与 mysql/ont

【API 端点】
(见 G-06.2 测试提示词)

【功能需求】
1. 本体定义版本管理
2. 类/属性/约束 完整 CRUD
3. 层次树结构
4. 批量本体验证
5. Schema.org 自动导入

【UI 规范】
- 列表:Antd Table
- 树形:Antd Tree
- 表单:Antd Form

【MySQL 集成】
- 数据表:ont_class, ont_property, ont_constraint, ont_definition, ont_class_inheritance
- 使用 MyBatis-Plus

【测试验证】
实现完成后,使用 G-06.2 测试提示词中的测试场景验证。

【交付物清单】
- [ ] 前端类管理 .vue(classes.vue)
- [ ] 前端属性管理 .vue(properties.vue)
- [ ] 前端约束管理 .vue(constraints.vue)
- [ ] 前端 API 封装 .ts
- [ ] 后端 Controller(已存在)
- [ ] 后端 Service(已存在)
- [ ] MySQL 表结构(已存在)
- [ ] 通过 G-06.2 所有测试场景
```

---

### G-07 本体管理 — 版本回滚

**页面路径**: `/data/classes` (版本历史标签)
**源码文件**: `ontograph-frontend/src/views/data/classes.vue` (可能包含版本历史 Tab)
**后端控制器**: `OntologyController.java`
**API 文件**: `ontograph-frontend/src/api/ontology.ts`

#### G-07.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| G-07-01 | 版本历史列表 | 显示所有版本 |
| G-07-02 | 版本对比 | 对比两个版本 |
| G-07-03 | 回滚到指定版本 | 恢复数据 |
| G-07-04 | 回滚后验证 | 数据一致性 |
| G-07-05 | 回滚操作日志 | 记录回滚事件 |
| G-07-06 | 跨图谱版本 | 多图谱版本独立 |

#### G-07.2 测试提示词

```
/browser 或 /open-gstack-browser
打开本体管理,执行版本回滚测试。

【前置操作】
1. 使用 admin/admin123 登录
2. 进入「数据管理」→「类管理」
3. 切换到「版本历史」标签

---

【测试场景 1:版本历史列表】
1. 验证 GET /api/v1/ontology/{graphId}/history
2. 验证列:历史ID、版本号、操作类型(创建/更新/回滚)、操作人、操作时间、变更摘要
3. 验证按时间倒序

预期结果:
✅ 版本历史列表加载

---

【测试场景 2:版本对比】
1. 选择两个历史记录
2. 点击「对比」
3. 验证 UI 显示 diff(添加/删除/修改的类、属性、约束)

预期结果:
✅ 版本对比显示

---

【测试场景 3:回滚到指定版本】
1. 选择某个历史记录
2. 点击「回滚到此版本」
3. 验证确认弹窗
4. 确认后:
   - POST /api/v1/ontology/{graphId}/history/{historyId}/rollback
5. 验证回滚成功提示
6. 验证本体列表与目标版本一致

预期结果:
✅ 回滚成功
✅ 列表数据更新

---

【测试场景 4:回滚后验证】
1. 回滚后检查类/属性/约束列表
2. 验证数据与目标版本完全一致
3. 验证新创建的历史记录(记录回滚事件)

预期结果:
✅ 数据一致性
✅ 新历史记录生成

---

【测试场景 5:回滚操作日志】
1. 在「系统日志」中查找回滚记录
2. 验证显示:操作人、回滚时间、回滚目标版本

预期结果:
✅ 操作日志记录

---

【测试场景 6:跨图谱版本】
1. 切换不同图谱
2. 验证版本历史独立

预期结果:
✅ 跨图谱隔离

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 版本历史 | GET | /api/v1/ontology/{graphId}/history |
| 回滚版本 | POST | /api/v1/ontology/{graphId}/history/{historyId}/rollback |

---

【问题诊断】
- 回滚失败 → 检查事务 / 数据完整性
- 数据不一致 → 检查 history snapshot
```

#### G-07.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【本体版本回滚】模块。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: /data/classes(版本历史 Tab)
- 权限前缀: graphiti:ontology:rollback

【前端文件清单】
- 类管理: ontograph-frontend/src/views/data/classes.vue
- API 封装: ontograph-frontend/src/api/ontology.ts

【后端文件清单】
- Controller: .../controller/admin/OntologyController.java
- Service: .../service/OntologyClassService.java(rollbackVersion)
- DO/Mapper: OntVersionHistoryDO / OntVersionHistoryMapper

【API 端点】
| 操作 | 方法 | 路径 |
|------|------|------|
| 版本历史 | GET | /api/v1/ontology/{graphId}/history |
| 回滚 | POST | /api/v1/ontology/{graphId}/history/{historyId}/rollback |

【功能需求】
1. 记录每次本体变更(类/属性/约束增删改)
2. 支持回滚到任意历史版本
3. 回滚后生成新历史记录
4. 版本对比(diff)

【UI 规范】
- 时间线:Antd Timeline
- 对比:自定义 diff 视图

【MySQL 集成】
- 表:ont_version_history
- 快照存储:JSON 字段

【测试验证】
实现完成后,使用 G-07.2 测试提示词中的测试场景验证。

【交付物清单】
- [ ] 前端版本历史 Tab
- [ ] 后端 Service(已存在)
- [ ] MySQL 表(已存在)
- [ ] 通过 G-07.2 所有测试场景
```

---

### G-08 本体管理 — 推理机

**页面路径**: `/data/classes` (推理机状态)
**源码文件**: `ontograph-frontend/src/views/data/classes.vue`
**后端控制器**: `OntologyController.java`
**依赖**: Apache Jena(OWL 2 RL 推理)

#### G-08.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| G-08-01 | 查看推理机状态 | 是否已预热 |
| G-08-02 | 预热推理机 | 加载本体到 Jena |
| G-08-03 | 一致性检查 | OWL 2 RL 约束 |
| G-08-04 | 推理结果查看 | 隐式关系 |
| G-08-05 | 推理机状态轮询 | 长时间任务监控 |

#### G-08.2 测试提示词

```
/browser 或 /open-gstack-browser
打开本体管理,执行推理机测试。

【前置操作】
1. 使用 admin/admin123 登录
2. 进入「数据管理」→「类管理」
3. 切换到「推理机」标签

---

【测试场景 1:查看推理机状态】
1. 验证 GET /api/v1/ontology/{graphId}/reasoners/status
2. 验证显示 warmedUp(boolean)、graphId

预期结果:
✅ 状态查询成功

---

【测试场景 2:预热推理机】
1. 点击「预热推理机」按钮
2. 验证确认
3. 触发:
   - POST /api/v1/ontology/{graphId}/reasoners/warmup
4. 验证进度提示
5. 验证完成后 warmedUp=true

预期结果:
✅ 推理机预热成功

---

【测试场景 3:一致性检查】
1. 预热完成后,点击「一致性检查」
2. 触发:
   - GET /api/v1/ontology/{graphId}/consistency
3. 验证返回:consistent(boolean)、violations(违规列表)
4. 验证 UI 显示检查结果

预期结果:
✅ 一致性检查完成

---

【测试场景 4:推理结果查看】
1. 一致性检查通过后,查看推理结果
2. 验证显示隐式关系(由 OWL 2 RL 推导)

预期结果:
✅ 推理结果展示

---

【测试场景 5:推理机状态轮询】
1. 预热推理机时,UI 显示进度条
2. 轮询状态接口直到 warmedUp=true

预期结果:
✅ 进度实时更新

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 推理机状态 | GET | /api/v1/ontology/{graphId}/reasoners/status |
| 预热推理机 | POST | /api/v1/ontology/{graphId}/reasoners/warmup |
| 一致性检查 | GET | /api/v1/ontology/{graphId}/consistency |

---

【问题诊断】
- 预热失败 → 检查 Jena 库 / OWL 解析
- 一致性违规 → 后端日志查看
```

#### G-08.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【本体推理机】模块。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: /data/classes(推理机 Tab)
- 依赖: Apache Jena(OWL 2 RL)

【前端文件清单】
- 类管理: ontograph-frontend/src/views/data/classes.vue
- API 封装: ontograph-frontend/src/api/ontology.ts

【后端文件清单】
- Controller: .../controller/admin/OntologyController.java
- Service: .../service/OntologyReasoner.java + impl
- 依赖:org.apache.jena:jena-arq, jena-ontology, jena-infgraph

【API 端点】
| 操作 | 方法 | 路径 |
|------|------|------|
| 推理机状态 | GET | /api/v1/ontology/{graphId}/reasoners/status |
| 预热 | POST | /api/v1/ontology/{graphId}/reasoners/warmup |
| 一致性 | GET | /api/v1/ontology/{graphId}/consistency |

【功能需求】
1. 推理机状态查询
2. 预热推理机(加载本体到 Jena InfGraph)
3. OWL 2 RL 一致性检查
4. 推理结果展示

【UI 规范】
- 状态卡片:Antd Card
- 进度条:Antd Progress

【Jena 集成】
- 使用 ReasonerRegistry 获取 OWL 2 RL 推理机
- 预热:OntModelSpec.OWL_MEM_RDFS_INF
- 一致性:Model consistency check

【测试验证】
实现完成后,使用 G-08.2 测试提示词中的测试场景验证。

【交付物清单】
- [ ] 前端推理机 Tab
- [ ] 后端 Service(已存在)
- [ ] Jena 依赖(pom.xml)
- [ ] 通过 G-08.2 所有测试场景
```

---

### G-09 本体管理 — 域规则

**页面路径**: `/data/classes` (域规则标签)
**后端控制器**: `OntologyController.java`
**特性**: SpEL 表达式

#### G-09.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| G-09-01 | 域规则列表 | 显示所有规则 |
| G-09-02 | 创建域规则 | SpEL 表达式 |
| G-09-03 | 更新域规则 | |
| G-09-04 | 删除域规则 | |
| G-09-05 | 启用/禁用切换 | toggle |
| G-09-06 | 测试 SpEL 表达式 | 输入测试数据 |
| G-09-07 | 规则触发 | 在数据导入时生效 |
| G-09-08 | 规则执行日志 | 失败记录 |

#### G-09.2 测试提示词

```
/browser 或 /open-gstack-browser
打开域规则管理,执行 CRUD 测试。

【前置操作】
1. 使用 admin/admin123 登录
2. 进入「数据管理」→「类管理」→「域规则」

---

【测试场景 1:域规则列表】
1. 验证 GET /api/v1/ontology/{graphId}/domain-rules
2. 验证列:规则ID、名称、SpEL 表达式、作用类、状态、创建时间

预期结果:
✅ 域规则列表加载

---

【测试场景 2:创建域规则】
1. 点击「创建规则」
2. 验证弹窗:
   - 规则名称
   - SpEL 表达式(如 #age >= 18)
   - 作用类
   - 描述
3. 填写:
   - 名称:成人校验
   - 表达式:#age >= 18
   - 作用类:Person
4. 提交:
   - POST /api/v1/ontology/{graphId}/domain-rules

预期结果:
✅ 规则创建成功

---

【测试场景 3:更新域规则】
1. 编辑规则
2. 提交:
   - PUT /api/v1/ontology/{graphId}/domain-rules/{ruleId}

预期结果:
✅ 更新成功

---

【测试场景 4:删除域规则】
1. 点击「删除」
2. 确认后:
   - DELETE /api/v1/ontology/{graphId}/domain-rules/{ruleId}

预期结果:
✅ 删除成功

---

【测试场景 5:启用/禁用切换】
1. 点击「禁用」或「启用」开关
2. 触发:
   - PATCH /api/v1/ontology/{graphId}/domain-rules/{ruleId}/toggle?enabled=...

预期结果:
✅ 切换成功
✅ 状态变化

---

【测试场景 6:测试 SpEL 表达式】
1. 点击「测试表达式」
2. 填写:
   - 表达式:#age >= 18
   - 测试数据:{age: 20}
3. 触发:
   - POST /api/v1/ontology/{graphId}/domain-rules/test
4. 验证返回结果(true/false)

预期结果:
✅ 测试执行
✅ 返回正确结果

---

【测试场景 7:规则触发】
1. 创建规则后,导入数据
2. 验证规则自动生效(数据违反规则时拒绝)

预期结果:
✅ 规则自动触发

---

【测试场景 8:规则执行日志】
1. 查看规则执行历史
2. 验证失败记录

预期结果:
✅ 日志记录完整

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 域规则列表 | GET | /api/v1/ontology/{graphId}/domain-rules |
| 创建 | POST | /api/v1/ontology/{graphId}/domain-rules |
| 更新 | PUT | /api/v1/ontology/{graphId}/domain-rules/{ruleId} |
| 删除 | DELETE | /api/v1/ontology/{graphId}/domain-rules/{ruleId} |
| 切换 | PATCH | /api/v1/ontology/{graphId}/domain-rules/{ruleId}/toggle |
| 测试 | POST | /api/v1/ontology/{graphId}/domain-rules/test |

---

【问题诊断】
- 表达式错误 → SpEL 解析异常
- 规则未触发 → 检查 enabled 状态 / 作用类
```

#### G-09.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【本体域规则(SpEL)】模块。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: /data/classes(域规则 Tab)
- 依赖:Spring Expression Language (SpEL)

【前端文件清单】
- 类管理: ontograph-frontend/src/views/data/classes.vue
- API 封装: ontograph-frontend/src/api/ontology.ts

【后端文件清单】
- Controller: .../controller/admin/OntologyController.java
- Service: .../service/DomainRuleService.java + impl
- Validator: .../service/validator/DomainRuleValidator.java
- DO/Mapper:DomainRuleDO / DomainRuleMapper

【API 端点】
| 操作 | 方法 | 路径 |
|------|------|------|
| 列表 | GET | /api/v1/ontology/{graphId}/domain-rules |
| CRUD | POST/PUT/DELETE | /api/v1/ontology/{graphId}/domain-rules[/{ruleId}] |
| 切换 | PATCH | /api/v1/ontology/{graphId}/domain-rules/{ruleId}/toggle |
| 测试 | POST | /api/v1/ontology/{graphId}/domain-rules/test |

【功能需求】
1. SpEL 表达式定义域规则
2. 规则在数据导入/更新时触发
3. 规则支持启用/禁用
4. 测试表达式功能

【SpEL 集成】
- 使用 SpelExpressionParser
- 上下文:节点属性作为变量

【测试验证】
实现完成后,使用 G-09.2 测试提示词中的测试场景验证。

【交付物清单】
- [ ] 前端域规则 Tab
- [ ] 后端 Service(已存在)
- [ ] 通过 G-09.2 所有测试场景
```

---

### G-10 元数据管理

**页面路径**: `/data/community-episode` (元数据管理)
**子模块**:
- EpisodeType 剧集类型
- CommunityType 社区类型
- EntityCategory 实体分类
- RelationshipMeta 关系元数据
**后端控制器**: `OntMetadataController.java`
**API 文件**: `ontograph-frontend/src/api/ontology.ts` 或 `metadata.ts`

#### G-10.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| G-10-01 | EpisodeType 列表 | 剧集类型管理 |
| G-10-02 | EpisodeType 树 | 树形结构 |
| G-10-03 | EpisodeType CRUD | |
| G-10-04 | EpisodeType 批量创建 | |
| G-10-05 | EpisodeType 删除前检查 | |
| G-10-06 | EpisodeType 重新排序 | |
| G-10-07 | CommunityType 列表/CRUD | |
| G-10-08 | CommunityType 树 | |
| G-10-09 | EntityCategory 列表/CRUD | |
| G-10-10 | EntityCategory 树 | |
| G-10-11 | RelationshipMeta 列表/CRUD | |
| G-10-12 | RelationshipMeta 批量 | |

#### G-10.2 测试提示词

```
/browser 或 /open-gstack-browser
打开元数据管理,执行 4 个子模块的 CRUD 测试。

【前置操作】
1. 使用 admin/admin123 登录
2. 进入「数据管理」→「社区与剧集元数据」

---

【测试场景 1:EpisodeType 列表】
1. 选择某个图谱
2. 触发:
   - GET /api/v1/ontology/{graphId}/episode-types
3. 验证列:类型ID、类型编码、名称、描述、颜色、图标、排序

预期结果:
✅ EpisodeType 列表加载

---

【测试场景 2:EpisodeType 树】
1. 触发:
   - GET /api/v1/ontology/{graphId}/episode-types/tree
2. 验证树形结构

预期结果:
✅ 树形结构渲染

---

【测试场景 3:EpisodeType CRUD】
1. 创建:POST /api/v1/ontology/{graphId}/episode-types
2. 更新:PUT /api/v1/ontology/{graphId}/episode-types/{id}
3. 删除:DELETE /api/v1/ontology/{graphId}/episode-types/{id}
4. 详情:GET /api/v1/ontology/{graphId}/episode-types/{id}

预期结果:
✅ CRUD 完整

---

【测试场景 4:EpisodeType 批量创建】
1. 点击「批量导入」
2. 上传 JSON/CSV
3. 触发:
   - POST /api/v1/ontology/{graphId}/episode-types/batch
4. 验证返回成功数

预期结果:
✅ 批量创建成功

---

【测试场景 5:EpisodeType 删除前检查】
1. 点击「删除」
2. 触发:
   - GET /api/v1/ontology/{graphId}/episode-types/{id}/delete-check
3. 验证返回:hasInstances(是否有实例)、instanceCount(实例数)
4. 验证 UI 提示

预期结果:
✅ 删除前检查生效

---

【测试场景 6:EpisodeType 重新排序】
1. 拖拽调整顺序
2. 触发:
   - POST /api/v1/ontology/{graphId}/episode-types/reorder
3. 验证顺序持久化

预期结果:
✅ 排序生效

---

【测试场景 7-8:CommunityType】
1. 列表:GET /api/v1/ontology/{graphId}/community-types
2. 树:GET /api/v1/ontology/{graphId}/community-types/tree
3. CRUD:同 EpisodeType

预期结果:
✅ CommunityType 完整

---

【测试场景 9-10:EntityCategory】
1. 列表:GET /api/v1/ontology/{graphId}/entity-categories
2. 树:GET /api/v1/ontology/{graphId}/entity-categories/tree
3. CRUD:同 EpisodeType

预期结果:
✅ EntityCategory 完整

---

【测试场景 11-12:RelationshipMeta】
1. 列表:GET /api/v1/ontology/{graphId}/relationship-meta
2. 详情:GET /api/v1/ontology/{graphId}/relationship-meta/{id}
3. CRUD
4. 批量:POST /api/v1/ontology/{graphId}/relationship-meta/batch

预期结果:
✅ RelationshipMeta 完整

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| EpisodeType 列表 | GET | /api/v1/ontology/{graphId}/episode-types |
| EpisodeType 树 | GET | /api/v1/ontology/{graphId}/episode-types/tree |
| EpisodeType CRUD | GET/POST/PUT/DELETE | /api/v1/ontology/{graphId}/episode-types[/{id}] |
| EpisodeType 批量 | POST | /api/v1/ontology/{graphId}/episode-types/batch |
| EpisodeType 删除检查 | GET | /api/v1/ontology/{graphId}/episode-types/{id}/delete-check |
| EpisodeType 重排序 | POST | /api/v1/ontology/{graphId}/episode-types/reorder |
| CommunityType 列表/树 | GET | /api/v1/ontology/{graphId}/community-types[ /tree] |
| CommunityType CRUD | GET/POST/PUT/DELETE | /api/v1/ontology/{graphId}/community-types[/{id}] |
| EntityCategory 列表/树 | GET | /api/v1/ontology/{graphId}/entity-categories[ /tree] |
| EntityCategory CRUD | GET/POST/PUT/DELETE | /api/v1/ontology/{graphId}/entity-categories[/{id}] |
| RelationshipMeta 列表 | GET | /api/v1/ontology/{graphId}/relationship-meta |
| RelationshipMeta CRUD | GET/POST/PUT/DELETE | /api/v1/ontology/{graphId}/relationship-meta[/{id}] |
| RelationshipMeta 批量 | POST | /api/v1/ontology/{graphId}/relationship-meta/batch |

---

【问题诊断】
- 树形渲染失败 → 检查 tree 数据格式
- 批量失败 → 检查 JSON 格式
- 排序不生效 → 检查 reorder API
```

#### G-10.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【元数据管理】模块。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: /data/community-episode
- 权限前缀: graphiti:metadata:

【前端文件清单】
- 元数据管理: ontograph-frontend/src/views/data/community-episode.vue
- API 封装: ontograph-frontend/src/api/metadata.ts

【后端文件清单】
- Controller: .../controller/admin/OntMetadataController.java
- Service: .../service/metadata/OntMetadataService.java + impl
- DO/Mapper:
  - OntEpisodeTypeDO/Mapper
  - OntCommunityTypeDO/Mapper
  - OntEntityCategoryDO/Mapper
  - OntRelationshipMetaDO/Mapper

【API 端点】
(见 G-10.2 测试提示词)

【功能需求】
1. 4 个元数据子模块完整 CRUD
2. 树形结构展示
3. 批量导入
4. 删除前检查(是否被引用)
5. 拖拽排序

【MySQL 集成】
- 4 张元数据表
- 使用 MyBatis-Plus

【测试验证】
实现完成后,使用 G-10.2 测试提示词中的测试场景验证。

【交付物清单】
- [ ] 前端元数据管理 .vue
- [ ] 后端 Controller(已存在)
- [ ] 后端 Service(已存在)
- [ ] MySQL 表(已存在)
- [ ] 通过 G-10.2 所有测试场景
```

---

### G-11 数据导入

**页面路径**: `/data/import`
**源码文件**: `ontograph-frontend/src/views/data/import.vue`
**后端控制器**: `DataImportController.java`
**API 文件**: `ontograph-frontend/src/api/data.ts`

#### G-11.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| G-11-01 | 选择图谱 | 导入前的图谱选择 |
| G-11-02 | 文本导入 | 直接输入文本 |
| G-11-03 | 批量导入 | 批量数据 |
| G-11-04 | 消息导入 | 消息流 |
| G-11-05 | 事实三元组 | (subject, predicate, object) |
| G-11-06 | 实体节点导入 | |
| G-11-07 | 删除边 | |
| G-11-08 | 删除组(按 graphId) | |
| G-11-09 | 删除剧集 | |
| G-11-10 | 清空图谱数据 | |

#### G-11.2 测试提示词

```
/browser 或 /open-gstack-browser
打开数据导入页面,执行 9 个测试场景。

【前置操作】
1. 使用 admin/admin123 登录
2. 进入「数据管理」→「数据导入」

---

【测试场景 1:选择图谱】
1. 在页面顶部选择目标图谱
2. 验证图谱下拉框数据加载

预期结果:
✅ 图谱选择正常

---

【测试场景 2:文本导入】
1. 在文本框输入:「张三是 ACME 公司的 CEO,公司位于北京」
2. 验证触发:
   - POST /api/v1/graph/data/add
3. 验证 LLM 抽取结果(可能耗时 5-30 秒)
4. 验证节点/边出现在画布

预期结果:
✅ 文本导入成功
✅ LLM 抽取实体/关系

---

【测试场景 3:批量导入】
1. 上传 JSON 文件(包含多条数据)
2. 触发:
   - POST /api/v1/graph/data/batch
3. 验证返回批次结果

预期结果:
✅ 批量导入成功

---

【测试场景 4:消息导入】
1. 填写消息:
   - role: user
   - content: ...
   - timestamp: ...
2. 触发:
   - POST /api/v1/graph/data/messages
3. 验证消息被处理为剧集

预期结果:
✅ 消息导入成功

---

【测试场景 5:事实三元组】
1. 填写三元组:
   - subject: 张三
   - predicate: 工作于
   - object: ACME
2. 触发:
   - POST /api/v1/graph/data/fact-triple
3. 验证事实被添加为边

预期结果:
✅ 三元组导入成功

---

【测试场景 6:实体节点导入】
1. 填写实体信息
2. 触发:
   - POST /api/v1/graph/data/entity-node
3. 验证节点创建

预期结果:
✅ 实体节点导入

---

【测试场景 7:删除边】
1. 触发:
   - DELETE /api/v1/graph/data/entity-edge/{uuid}

预期结果:
✅ 边删除

---

【测试场景 8:删除组(按 graphId)】
1. 触发:
   - DELETE /api/v1/graph/data/group/{graphId}
2. 验证清空图谱数据

预期结果:
✅ 数据清空

---

【测试场景 9:删除剧集 + 清空】
1. 删除剧集:DELETE /api/v1/graph/data/episode/{uuid}
2. 清空:POST /api/v1/graph/data/clear

预期结果:
✅ 剧集/数据清空

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 添加数据 | POST | /api/v1/graph/data/add |
| 批量 | POST | /api/v1/graph/data/batch |
| 消息 | POST | /api/v1/graph/data/messages |
| 事实三元组 | POST | /api/v1/graph/data/fact-triple |
| 实体节点 | POST | /api/v1/graph/data/entity-node |
| 删除边 | DELETE | /api/v1/graph/data/entity-edge/{uuid} |
| 删除组 | DELETE | /api/v1/graph/data/group/{graphId} |
| 删除剧集 | DELETE | /api/v1/graph/data/episode/{uuid} |
| 清空 | POST | /api/v1/graph/data/clear |

---

【问题诊断】
- LLM 超时 → 检查 LM Studio
- 抽取失败 → 检查 Prompt 模板
- Neo4j 写入失败 → 检查事务
```

#### G-11.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【数据导入】模块。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: /data/import
- 权限前缀: graphiti:data:import

【前端文件清单】
- 导入页: ontograph-frontend/src/views/data/import.vue
- API 封装: ontograph-frontend/src/api/data.ts

【后端文件清单】
- Controller: .../controller/admin/DataImportController.java
- Service: .../service/DataImportService.java + impl
- Service: .../service/DataExtractService.java + impl
- Service: .../service/GraphitiService.java + impl

【API 端点】
(见 G-11.2 测试提示词)

【功能需求】
1. 支持多种数据源(文本/JSON/批量/消息/三元组)
2. LLM 抽取实体和关系
3. 写入 Neo4j 图数据库
4. 异步任务跟踪(返回 taskId)

【LLM 集成】
- 调用 LlmClientService
- 使用 entity-extraction 提示词模板

【Neo4j 集成】
- 节点:CREATE (n:Entity {...})
- 边:MATCH (a), (b) CREATE (a)-[r:RELATES_TO]->(b)

【测试验证】
实现完成后,使用 G-11.2 测试提示词中的测试场景验证。

【交付物清单】
- [ ] 前端导入页 .vue
- [ ] 后端 Controller(已存在)
- [ ] 后端 Service(已存在)
- [ ] 通过 G-11.2 所有测试场景
```

---

### G-12 数据抽取

**页面路径**: `/data/import` (抽取 Tab) 或独立页面
**后端控制器**: `DataExtractController.java`
**API 文件**: `ontograph-frontend/src/api/data.ts`

#### G-12.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| G-12-01 | 文本抽取 | 实体+关系 |
| G-12-02 | JSON 文件抽取 | 上传 JSON |
| G-12-03 | 仅抽取实体 | |
| G-12-04 | 仅抽取关系 | 需先提供实体 |
| G-12-05 | JSON 结构预览 | |
| G-12-06 | 默认实体类型 | |
| G-12-07 | 默认关系类型 | |
| G-12-08 | 自定义指令抽取 | |

#### G-12.2 测试提示词

```
/browser 或 /open-gstack-browser
打开数据抽取页面,执行测试。

【前置操作】
1. 使用 admin/admin123 登录
2. 进入数据抽取页面

---

【测试场景 1:文本抽取】
1. 输入文本:「马云创立了阿里巴巴,公司位于杭州」
2. 触发:
   - POST /api/v1/graph/extract/text
3. 验证返回 entities(马云, 阿里巴巴)、relations(创立了, 位于)

预期结果:
✅ 实体/关系抽取正确

---

【测试场景 2:JSON 文件抽取】
1. 上传 JSON 文件
2. 填写:
   - graphId
   - entityTypesConfig(可选)
   - edgeTypesConfig(可选)
   - customInstructions(可选)
3. 触发:
   - POST /api/v1/graph/extract/json(multipart)
4. 验证抽取结果

预期结果:
✅ JSON 抽取成功

---

【测试场景 3:仅抽取实体】
1. 触发:
   - POST /api/v1/graph/extract/entities
2. 验证只返回 entities

预期结果:
✅ 仅实体返回

---

【测试场景 4:仅抽取关系】
1. 先调用 /entities 获取实体列表
2. 在 existingEntities 中提供
3. 触发:
   - POST /api/v1/graph/extract/edges
4. 验证返回 relations

预期结果:
✅ 关系抽取

---

【测试场景 5:JSON 结构预览】
1. 上传 JSON 文件
2. 触发:
   - POST /api/v1/graph/extract/preview
3. 验证返回:
   - fileName
   - fieldCount
   - sampleData
   - contentPreview

预期结果:
✅ 预览返回

---

【测试场景 6:默认实体类型】
1. 触发:
   - GET /api/v1/graph/extract/entity-types
2. 验证返回 Person, Organization, Location 等

预期结果:
✅ 默认类型返回

---

【测试场景 7:默认关系类型】
1. 触发:
   - GET /api/v1/graph/extract/edge-types
2. 验证返回 WORKS_AT, LIVES_IN 等

预期结果:
✅ 默认关系返回

---

【测试场景 8:自定义指令抽取】
1. 填写 customInstructions:「重点关注人物和组织」
2. 触发抽取
3. 验证抽取结果按指令优化

预期结果:
✅ 自定义指令生效

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 文本抽取 | POST | /api/v1/graph/extract/text |
| JSON 抽取 | POST | /api/v1/graph/extract/json |
| 仅实体 | POST | /api/v1/graph/extract/entities |
| 仅关系 | POST | /api/v1/graph/extract/edges |
| 预览 | POST | /api/v1/graph/extract/preview |
| 默认实体类型 | GET | /api/v1/graph/extract/entity-types |
| 默认关系类型 | GET | /api/v1/graph/extract/edge-types |

---

【问题诊断】
- 抽取失败 → 检查 LLM / Prompt
- JSON 解析失败 → 检查文件格式
```

#### G-12.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【数据抽取】模块。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: /data/import(抽取 Tab)
- 权限前缀: graphiti:data:extract

【前端文件清单】
- 导入页(抽取 Tab): ontograph-frontend/src/views/data/import.vue
- API 封装: ontograph-frontend/src/api/data.ts

【后端文件清单】
- Controller: .../controller/admin/DataExtractController.java
- Service: .../service/DataExtractService.java + impl
- Service: .../service/EntityExtractorService.java + impl
- Service: .../service/EdgeExtractorService.java + impl

【API 端点】
(见 G-12.2 测试提示词)

【功能需求】
1. 文本抽取(实体+关系)
2. JSON 文件抽取
3. 实体/关系分别抽取
4. JSON 结构预览
5. 自定义指令支持

【LLM 集成】
- 使用 entity-extraction 提示词
- 自定义指令:PromptTemplateService.render

【测试验证】
实现完成后,使用 G-12.2 测试提示词中的测试场景验证。

【交付物清单】
- [ ] 前端抽取 UI
- [ ] 后端 Controller(已存在)
- [ ] 后端 Service(已存在)
- [ ] 通过 G-12.2 所有测试场景
```

---

### G-13 数据导出

**页面路径**: `/data/export`
**源码文件**: `ontograph-frontend/src/views/data/export.vue`
**后端控制器**: `GraphitiController.java`(`/api/v1/graph/{graphId}/export`)

#### G-13.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| G-13-01 | 选择图谱 | |
| G-13-02 | 导出 JSON | 完整数据 |
| G-13-03 | 导出 CSV | 节点/边分别导出 |
| G-13-04 | 导出范围选择 | 全部/按类 |

#### G-13.2 测试提示词

```
/browser 或 /open-gstack-browser
打开数据导出页面,执行测试。

【前置操作】
1. 使用 admin/admin123 登录
2. 进入「数据管理」→「数据导出」

---

【测试场景 1:选择图谱】
1. 选择目标图谱
2. 验证图谱下拉

预期结果:
✅ 图谱选择

---

【测试场景 2:导出 JSON】
1. 选择导出格式 JSON
2. 点击「导出」
3. 触发:
   - GET /api/v1/graph/{graphId}/export
4. 验证下载文件包含 nodes、edges、metadata

预期结果:
✅ JSON 导出成功

---

【测试场景 3:导出 CSV】
1. 选择 CSV 格式
2. 节点 CSV:uuid, name, type, properties
3. 边 CSV:uuid, source, target, type, fact
4. 验证下载 2 个 CSV 文件

预期结果:
✅ CSV 导出

---

【测试场景 4:导出范围】
1. 选择「按类导出」
2. 选中部分类
3. 验证只导出选中类节点

预期结果:
✅ 范围过滤生效

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 导出图谱 | GET | /api/v1/graph/{graphId}/export |

---

【问题诊断】
- 文件下载失败 → 检查响应头 Content-Disposition
- 大文件超时 → 增加超时配置
```

#### G-13.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【数据导出】模块。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: /data/export
- 权限前缀: graphiti:data:export

【前端文件清单】
- 导出页: ontograph-frontend/src/views/data/export.vue
- API 封装: ontograph-frontend/src/api/data.ts

【后端文件清单】
- Controller: .../controller/admin/GraphitiController.java
- Service: .../service/GraphitiService.java(exportGraph)

【API 端点】
| 操作 | 方法 | 路径 |
|------|------|------|
| 导出图谱 | GET | /api/v1/graph/{graphId}/export |

【功能需求】
1. 支持 JSON/CSV 格式
2. 支持范围选择(全部/按类)
3. 大文件流式下载

【UI 规范】
- 格式选择:Antd Radio
- 类多选:Antd Select multiple

【测试验证】
实现完成后,使用 G-13.2 测试提示词中的测试场景验证。

【交付物清单】
- [ ] 前端导出页 .vue
- [ ] 后端 exportGraph 方法
- [ ] 通过 G-13.2 所有测试场景
```

---

### G-14 搜索

**页面路径**: `/search`
**源码文件**: `ontograph-frontend/src/views/search/index.vue`
**后端控制器**: `SearchController.java`
**API 文件**: `ontograph-frontend/src/api/search.ts`

#### G-14.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| G-14-01 | 全局搜索 | 跨图谱 |
| G-14-02 | 图谱内搜索 | 指定图谱 |
| G-14-03 | 记忆搜索 | 用户记忆 |
| G-14-04 | 混合搜索 | 向量+关键词 |
| G-14-05 | 语义搜索 | Embedding |
| G-14-06 | BFS 搜索 | 广度优先 |
| G-14-07 | 实体边检索 | |
| G-14-08 | 剧集检索 | |
| G-14-09 | 搜索结果高亮 | |

#### G-14.2 测试提示词

```
/browser 或 /open-gstack-browser
打开搜索页面,执行测试。

【前置操作】
1. 使用 admin/admin123 登录
2. 进入「搜索」

---

【测试场景 1:全局搜索】
1. 输入关键词
2. 触发:
   - POST /api/v1/graph/search/global
3. 验证返回跨图谱结果

预期结果:
✅ 全局搜索结果

---

【测试场景 2:图谱内搜索】
1. 选择图谱
2. 输入关键词
3. 触发:
   - POST /api/v1/graph/search/graph/{graphId}

预期结果:
✅ 图谱内结果

---

【测试场景 3:记忆搜索】
1. 触发:
   - POST /api/v1/graph/search/memory
2. 验证返回用户记忆(过去查询/行为)

预期结果:
✅ 记忆返回

---

【测试场景 4:混合搜索】
1. 触发:
   - POST /api/v1/graph/search/hybrid/{graphId}
2. 验证结合向量和关键词

预期结果:
✅ 混合搜索

---

【测试场景 5:语义搜索】
1. 触发:
   - POST /api/v1/graph/search/semantic/{graphId}
2. 验证基于 Embedding 的相似度

预期结果:
✅ 语义搜索

---

【测试场景 6:BFS 搜索】
1. 触发:
   - POST /api/v1/graph/search/bfs/{graphId}
2. 验证广度优先遍历

预期结果:
✅ BFS 搜索

---

【测试场景 7:实体边检索】
1. 触发:
   - GET /api/v1/graph/search/retrieve/entity-edge/{uuid}

预期结果:
✅ 实体边检索

---

【测试场景 8:剧集检索】
1. 触发:
   - GET /api/v1/graph/search/retrieve/episodes/{graphId}

预期结果:
✅ 剧集检索

---

【测试场景 9:搜索结果高亮】
1. 验证搜索关键词在结果中以高亮显示

预期结果:
✅ 高亮渲染

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 全局搜索 | POST | /api/v1/graph/search/global |
| 图谱搜索 | POST | /api/v1/graph/search/graph/{graphId} |
| 记忆搜索 | POST | /api/v1/graph/search/memory |
| 实体边 | GET | /api/v1/graph/search/retrieve/entity-edge/{uuid} |
| 剧集 | GET | /api/v1/graph/search/retrieve/episodes/{graphId} |
| 混合 | POST | /api/v1/graph/search/hybrid/{graphId} |
| 语义 | POST | /api/v1/graph/search/semantic/{graphId} |
| BFS | POST | /api/v1/graph/search/bfs/{graphId} |

---

【问题诊断】
- 搜索无结果 → 检查 Embedding 缓存
- 搜索慢 → 检查 Neo4j 索引
```

#### G-14.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【搜索】模块。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: /search
- 权限前缀: graphiti:search:

【前端文件清单】
- 搜索页: ontograph-frontend/src/views/search/index.vue
- API 封装: ontograph-frontend/src/api/search.ts

【后端文件清单】
- Controller: .../controller/admin/SearchController.java
- Service: .../service/SearchService.java + impl
- Service: .../service/EmbeddingCacheService.java

【API 端点】
(见 G-14.2 测试提示词)

【功能需求】
1. 多种搜索方式(全局/图谱/混合/语义/BFS)
2. 嵌入缓存
3. 结果高亮

【LLM 集成】
- Embedding:OpenAiEmbedderServiceImpl
- Rerank:CrossEncoderRerankerServiceImpl

【Neo4j 集成】
- 向量索引:Neo4j Vector Index
- 关键词:全文索引

【测试验证】
实现完成后,使用 G-14.2 测试提示词中的测试场景验证。

【交付物清单】
- [ ] 前端搜索页 .vue
- [ ] 后端 Controller(已存在)
- [ ] 后端 Service(已存在)
- [ ] 通过 G-14.2 所有测试场景
```

---

### G-15 搜索管线

**页面路径**: 可能集成在搜索页或 Pipeline 配置页
**后端控制器**: `SearchPipelineController.java`
**特性**: 多阶段管线(Rerank / Parallel)

#### G-15.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| G-15-01 | 管线搜索 | 多阶段 |
| G-15-02 | 并行搜索 | 多路召回 |
| G-15-03 | 重排序 | Rerank |
| G-15-04 | 管线配置 | Reranker / Filters |
| G-15-05 | 性能基准 | |
| G-15-06 | 缓存命中 | |

#### G-15.2 测试提示词

```
/browser 或 /open-gstack-browser
打开搜索管线测试。

【前置操作】
1. 使用 admin/admin123 登录
2. 进入搜索功能,选择「高级管线」模式

---

【测试场景 1:管线搜索】
1. 填写搜索词 + 图谱 + 管线配置
2. 触发:
   - POST /api/v1/graph/search/pipeline/search
3. 验证返回 results 与 timing 信息

预期结果:
✅ 管线执行

---

【测试场景 2:并行搜索】
1. 触发:
   - POST /api/v1/graph/search/pipeline/parallel
2. 验证多路并行召回

预期结果:
✅ 并行执行

---

【测试场景 3:重排序】
1. 触发:
   - POST /api/v1/graph/search/pipeline/rerank
2. 验证 Rerank 后顺序变化

预期结果:
✅ Rerank 生效

---

【测试场景 4:管线配置】
1. 配置 Reranker 类型(cross-encoder / MMR / RRF)
2. 配置 Filters(类/时间范围)
3. 验证配置生效

预期结果:
✅ 配置生效

---

【测试场景 5:性能基准】
1. 重复执行 10 次管线搜索
2. 验证响应时间(平均 < 2s)

预期结果:
✅ 性能稳定

---

【测试场景 6:缓存命中】
1. 第二次相同搜索
2. 验证响应时间显著降低

预期结果:
✅ 缓存命中

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 管线搜索 | POST | /api/v1/graph/search/pipeline/search |
| 并行搜索 | POST | /api/v1/graph/search/pipeline/parallel |
| 重排序 | POST | /api/v1/graph/search/pipeline/rerank |

---

【问题诊断】
- Rerank 慢 → 检查 CrossEncoder 模型
- 并行失败 → 检查线程池
```

#### G-15.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【搜索管线】模块。

【模块信息】
- 服务: ontograph-java(:9090)
- 路径:集成在搜索功能
- 权限前缀: graphiti:search:pipeline

【前端文件清单】
- 搜索页: ontograph-frontend/src/views/search/index.vue
- API 封装: ontograph-frontend/src/api/search.ts

【后端文件清单】
- Controller: .../controller/admin/SearchPipelineController.java
- Service: .../service/SearchPipelineService.java + impl
- Service: .../service/CrossEncoderRerankerService.java + impl
- Service: .../service/MmrRerankerService.java + impl
- Service: .../service/RrfRerankerService.java + impl

【API 端点】
(见 G-15.2 测试提示词)

【功能需求】
1. 多阶段管线
2. 多种 Reranker(Cross-Encoder / MMR / RRF)
3. 并行召回
4. 结果缓存

【测试验证】
实现完成后,使用 G-15.2 测试提示词中的测试场景验证。

【交付物清单】
- [ ] 前端管线 UI(可集成在搜索页)
- [ ] 后端 Controller(已存在)
- [ ] 后端 Service(已存在)
- [ ] 通过 G-15.2 所有测试场景
```

---

### G-16 Prompt 模板

**页面路径**: `/prompt`
**源码文件**: `ontograph-frontend/src/views/prompt/index.vue`
**后端控制器**: `PromptController.java`
**API 文件**: `ontograph-frontend/src/api/prompt.ts`

#### G-16.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| G-16-01 | 模板列表 | 按类型筛选 |
| G-16-02 | 创建模板 | 名称/类型/内容/变量 |
| G-16-03 | 编辑模板 | |
| G-16-04 | 删除模板 | |
| G-16-05 | 按 code 查询 | |
| G-16-06 | 按 type 查询 | |
| G-16-07 | 启用/禁用切换 | toggle |
| G-16-08 | 版本管理 | 创建版本 |
| G-16-09 | 版本回滚 | rollback |
| G-16-10 | 模板渲染 | 变量替换 |

#### G-16.2 测试提示词

```
/browser 或 /open-gstack-browser
打开 Prompt 模板管理。

【前置操作】
1. 使用 admin/admin123 登录
2. 进入「Prompt 管理」

---

【测试场景 1:模板列表】
1. 验证 GET /api/v1/prompt/templates
2. 验证列:模板ID、code、名称、类型、版本、状态、创建时间

预期结果:
✅ 模板列表加载

---

【测试场景 2:创建模板】
1. 填写:
   - code: entity-extract-v1
   - name: 实体抽取模板
   - type: EXTRACTION
   - content: 从文本中提取实体:{{text}}
   - variables: [{name: text, type: string}]
2. 触发:
   - POST /api/v1/prompt/templates
3. 验证创建成功

预期结果:
✅ 模板创建

---

【测试场景 3:编辑模板】
1. 触发:
   - PUT /api/v1/prompt/templates/{id}

预期结果:
✅ 编辑成功

---

【测试场景 4:删除模板】
1. 触发:
   - DELETE /api/v1/prompt/templates/{id}

预期结果:
✅ 删除成功

---

【测试场景 5:按 code 查询】
1. 触发:
   - GET /api/v1/prompt/templates/code/{code}

预期结果:
✅ 模板返回

---

【测试场景 6:按 type 查询】
1. 触发:
   - GET /api/v1/prompt/templates/type/{type}

预期结果:
✅ 同类型模板列表

---

【测试场景 7:启用/禁用切换】
1. 触发:
   - PUT /api/v1/prompt/templates/{id}/toggle

预期结果:
✅ 状态切换

---

【测试场景 8:版本管理】
1. 修改模板后,点击「保存为新版本」
2. 触发:
   - POST /api/v1/prompt/templates/{id}/versions
3. 验证版本号递增

预期结果:
✅ 新版本创建

---

【测试场景 9:版本回滚】
1. 选择历史版本
2. 触发:
   - POST /api/v1/prompt/templates/{id}/rollback?version=...

预期结果:
✅ 回滚成功

---

【测试场景 10:模板渲染】
1. 填写变量值
2. 触发:
   - POST /api/v1/prompt/templates/{id}/render
3. 验证返回渲染后的 prompt

预期结果:
✅ 渲染正确

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 列表 | GET | /api/v1/prompt/templates |
| 详情 | GET | /api/v1/prompt/templates/{id} |
| 按 code | GET | /api/v1/prompt/templates/code/{code} |
| 按 type | GET | /api/v1/prompt/templates/type/{type} |
| 创建 | POST | /api/v1/prompt/templates |
| 更新 | PUT | /api/v1/prompt/templates/{id} |
| 删除 | DELETE | /api/v1/prompt/templates/{id} |
| 切换 | PUT | /api/v1/prompt/templates/{id}/toggle |
| 新版本 | POST | /api/v1/prompt/templates/{id}/versions |
| 版本列表 | GET | /api/v1/prompt/templates/{id}/versions |
| 回滚 | POST | /api/v1/prompt/templates/{id}/rollback |
| 渲染 | POST | /api/v1/prompt/templates/{id}/render |
| 类型列表 | GET | /api/v1/prompt/types |

---

【问题诊断】
- 渲染失败 → 检查变量定义
- 版本回滚失败 → 检查历史快照
```

#### G-16.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【Prompt 模板管理】模块。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: /prompt
- 权限前缀: graphiti:prompt:

【前端文件清单】
- Prompt 页: ontograph-frontend/src/views/prompt/index.vue
- API 封装: ontograph-frontend/src/api/prompt.ts

【后端文件清单】
- Controller: .../controller/admin/PromptController.java
- Service: .../service/PromptTemplateService.java + impl
- DO/Mapper:
  - PromptTemplateDO/Mapper
  - PromptVersionDO/Mapper
  - PromptVariableDO/Mapper

【API 端点】
(见 G-16.2 测试提示词)

【功能需求】
1. 模板 CRUD
2. 版本管理
3. 变量定义
4. 模板渲染
5. 启用/禁用

【MySQL 集成】
- 表:prompt_template, prompt_version, prompt_variable
- 使用 MyBatis-Plus

【测试验证】
实现完成后,使用 G-16.2 测试提示词中的测试场景验证。

【交付物清单】
- [ ] 前端 Prompt 页 .vue
- [ ] 后端 Controller(已存在)
- [ ] 后端 Service(已存在)
- [ ] MySQL 表(已存在)
- [ ] 通过 G-16.2 所有测试场景
```

---

### G-17 Prompt 测试

**页面路径**: `/prompt` (测试 Tab)
**后端控制器**: `PromptTestController.java`

#### G-17.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| G-17-01 | 执行 Prompt | 调用 LLM |
| G-17-02 | 实体抽取 | 使用 entity-extract 模板 |
| G-17-03 | 关系抽取 | |
| G-17-04 | 生成样本数据 | 模拟数据生成 |
| G-17-05 | 测试结果对比 | 多次执行 |

#### G-17.2 测试提示词

```
/browser 或 /open-gstack-browser
打开 Prompt 测试功能。

---

【测试场景 1:执行 Prompt】
1. 选择模板或输入 prompt
2. 填写变量
3. 触发:
   - POST /api/v1/prompt/test/execute
4. 验证 LLM 返回

预期结果:
✅ Prompt 执行

---

【测试场景 2:实体抽取】
1. 选择 entity-extract 模板
2. 输入文本
3. 触发:
   - POST /api/v1/prompt/test/extract
4. 验证返回 entities

预期结果:
✅ 抽取结果

---

【测试场景 3:关系抽取】
1. 同上,但抽取关系

预期结果:
✅ 关系结果

---

【测试场景 4:生成样本数据】
1. 填写 ontology 信息
2. 触发:
   - POST /api/v1/prompt/test/generate-sample
3. 验证返回 mock 数据

预期结果:
✅ 样本生成

---

【测试场景 5:测试结果对比】
1. 同一 prompt 执行多次
2. 验证结果一致性(或 LLM 温度参数)

预期结果:
✅ 结果返回

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 执行 | POST | /api/v1/prompt/test/execute |
| 抽取 | POST | /api/v1/prompt/test/extract |
| 生成样本 | POST | /api/v1/prompt/test/generate-sample |

---

【问题诊断】
- LLM 超时 → 检查 LM Studio
- 返回为空 → 检查 prompt 模板
```

#### G-17.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【Prompt 测试】模块。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: /prompt(测试 Tab)
- 权限前缀: graphiti:prompt:test

【前端文件清单】
- Prompt 页(测试 Tab): ontograph-frontend/src/views/prompt/index.vue

【后端文件清单】
- Controller: .../controller/admin/PromptTestController.java
- Service: .../service/LlmClientService.java + impl

【API 端点】
(见 G-17.2 测试提示词)

【功能需求】
1. Prompt 执行(直接输入)
2. 实体/关系抽取测试
3. 样本数据生成

【LLM 集成】
- 调用 LlmClientService
- 使用不同 Prompt 模板

【测试验证】
实现完成后,使用 G-17.2 测试提示词中的测试场景验证。

【交付物清单】
- [ ] 前端测试 Tab
- [ ] 后端 Controller(已存在)
- [ ] 通过 G-17.2 所有测试场景
```

---

### G-18 自定义指令

**页面路径**: `/custom-instructions`
**源码文件**: `ontograph-frontend/src/views/custom-instructions/index.vue`
**后端控制器**: `CustomInstructionController.java`
**API 文件**: `ontograph-frontend/src/api/customInstruction.ts`

#### G-18.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| G-18-01 | 指令列表 | 全部/按 graphId |
| G-18-02 | 创建指令 | instruction + graphId |
| G-18-03 | 删除指令 | |
| G-18-04 | 指令在抽取中生效 | |

#### G-18.2 测试提示词

```
/browser 或 /open-gstack-browser
打开自定义指令管理。

【前置操作】
1. 使用 admin/admin123 登录
2. 进入「自定义指令」

---

【测试场景 1:指令列表】
1. 验证 GET /api/v1/custom-instructions
2. 验证可选参数 graphId 过滤

预期结果:
✅ 列表加载

---

【测试场景 2:创建指令】
1. 填写:
   - instruction: 重点关注人物和组织
   - graphId: (可选)
2. 触发:
   - POST /api/v1/custom-instructions

预期结果:
✅ 指令创建

---

【测试场景 3:删除指令】
1. 触发:
   - DELETE /api/v1/custom-instructions/{id}

预期结果:
✅ 删除成功

---

【测试场景 4:指令在抽取中生效】
1. 创建指令后,执行数据抽取
2. 验证抽取结果按指令优化

预期结果:
✅ 指令生效

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 列表 | GET | /api/v1/custom-instructions |
| 创建 | POST | /api/v1/custom-instructions |
| 删除 | DELETE | /api/v1/custom-instructions/{id}

---

【问题诊断】
- 指令不生效 → 检查 graphId 关联
```

#### G-18.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【自定义指令】模块。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: /custom-instructions
- 权限前缀: graphiti:custom-instruction:

【前端文件清单】
- 自定义指令页: ontograph-frontend/src/views/custom-instructions/index.vue
- API 封装: ontograph-frontend/src/api/customInstruction.ts

【后端文件清单】
- Controller: .../controller/admin/CustomInstructionController.java
- Service: .../service/CustomInstructionService.java + impl
- DO/Mapper:CustomInstructionDO/Mapper

【API 端点】
(见 G-18.2 测试提示词)

【功能需求】
1. 指令 CRUD
2. 按 graphId 关联
3. 抽取时注入指令

【MySQL 集成】
- 表:custom_instruction

【测试验证】
实现完成后,使用 G-18.2 测试提示词中的测试场景验证。

【交付物清单】
- [ ] 前端页面 .vue
- [ ] 后端 Controller(已存在)
- [ ] MySQL 表(已存在)
- [ ] 通过 G-18.2 所有测试场景
```

---

### G-19 时序数据

**页面路径**: `/graph/temporal`
**源码文件**: `ontograph-frontend/src/views/graph/temporal.vue`
**后端控制器**: `TemporalController.java`
**API 文件**: `ontograph-frontend/src/api/temporal.ts`

#### G-19.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| G-19-01 | 当前事实 | 当前时刻 facts |
| G-19-02 | 指定时刻事实 | facts/at/{time} |
| G-19-03 | 指定时刻关系 | relationships/at/{time} |
| G-19-04 | 实体历史 | history/{entityName} |
| G-19-05 | 失效事实 | invalidate |

#### G-19.2 测试提示词

```
/browser 或 /open-gstack-browser
打开时序数据页面。

【前置操作】
1. 使用 admin/admin123 登录
2. 进入「图谱管理」→「时序历史」

---

【测试场景 1:当前事实】
1. 选择图谱
2. 触发:
   - GET /api/v1/graph/{graphId}/temporal/facts/current
3. 验证返回 facts 列表

预期结果:
✅ 当前事实返回

---

【测试场景 2:指定时刻事实】
1. 填写 referenceTime(毫秒时间戳)
2. 触发:
   - GET /api/v1/graph/{graphId}/temporal/facts/at/{referenceTime}
3. 验证返回该时间点的事实

预期结果:
✅ 历史事实返回

---

【测试场景 3:指定时刻关系】
1. 触发:
   - GET /api/v1/graph/{graphId}/temporal/relationships/at/{referenceTime}
4. 验证返回该时间点的关系

预期结果:
✅ 历史关系返回

---

【测试场景 4:实体历史】
1. 填写 entityName
2. 触发:
   - GET /api/v1/graph/{graphId}/temporal/history/{entityName}
3. 验证实体的所有时序变化

预期结果:
✅ 实体历史返回

---

【测试场景 5:失效事实】
1. 选择事实
2. 触发:
   - POST /api/v1/graph/{graphId}/temporal/facts/invalidate
3. 验证事实标记为 invalid

预期结果:
✅ 事实失效

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 当前事实 | GET | /api/v1/graph/{graphId}/temporal/facts/current |
| 指定时刻事实 | GET | /api/v1/graph/{graphId}/temporal/facts/at/{referenceTime} |
| 指定时刻关系 | GET | /api/v1/graph/{graphId}/temporal/relationships/at/{referenceTime} |
| 实体历史 | GET | /api/v1/graph/{graphId}/temporal/history/{entityName} |
| 失效事实 | POST | /api/v1/graph/{graphId}/temporal/facts/invalidate |

---

【问题诊断】
- 时序数据缺失 → 检查 valid_at / invalid_at 字段
- 失效失败 → 检查事务
```

#### G-19.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【时序数据】模块。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: /graph/temporal
- 权限前缀: graphiti:temporal:

【前端文件清单】
- 时序页: ontograph-frontend/src/views/graph/temporal.vue
- API 封装: ontograph-frontend/src/api/temporal.ts

【后端文件清单】
- Controller: .../controller/admin/TemporalController.java
- Service: .../service/TemporalService.java + impl

【API 端点】
(见 G-19.2 测试提示词)

【功能需求】
1. 时序事实查询
2. 历史快照
3. 事实失效

【Neo4j 集成】
- 节点属性:valid_at, invalid_at, expired_at
- 时序索引

【测试验证】
实现完成后,使用 G-19.2 测试提示词中的测试场景验证。

【交付物清单】
- [ ] 前端时序页 .vue
- [ ] 后端 Controller(已存在)
- [ ] 后端 Service(已存在)
- [ ] 通过 G-19.2 所有测试场景
```

---

### G-20 法律知识图谱

**页面路径**: `/legal-kg`
**源码文件**: `ontograph-frontend/src/views/legal-kg/index.vue`
**后端控制器**: `LegalExtractController.java`, `LegalImportController.java`
**API 文件**: `ontograph-frontend/src/api/legal-extract.ts`, `legal-import.ts`, `legal-kg-data.ts`

#### G-20.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| G-20-01 | 法律抽取预览 | |
| G-20-02 | 法律抽取保存 | |
| G-20-03 | 法律本体字段配置 | |
| G-20-04 | 抽取模板列表 | |
| G-20-05 | 法律导入 | 节点/边/法条/案件 |
| G-20-06 | 法律导出 | |
| G-20-07 | 案件导入 | |
| G-20-08 | 法条导入 | |
| G-20-09 | 节点导入 | |
| G-20-10 | 边导入 | |
| G-20-11 | 抽取数据查看 | |
| G-20-12 | 导入结果统计 | |

#### G-20.2 测试提示词

```
/browser 或 /open-gstack-browser
打开法律知识图谱管理。

【前置操作】
1. 使用 admin/admin123 登录
2. 进入「法律知识图谱」

---

【测试场景 1:法律抽取预览】
1. 上传裁判文书或输入法律文本
2. 触发:
   - POST /api/v1/graph/legal/extract/preview
3. 验证返回:
   - 案件(case)
   - 当事人(party)
   - 法官(judge)
   - 律师(lawyer)
   - 法院(court)
   - 证据(evidence)
   - 判决(judgment)
   - 法条(provision)

预期结果:
✅ 抽取预览返回

---

【测试场景 2:法律抽取保存】
1. 确认预览结果
2. 触发:
   - POST /api/v1/graph/legal/extract/save
3. 验证数据写入 Neo4j

预期结果:
✅ 抽取保存

---

【测试场景 3:法律本体字段配置】
1. 配置 ontology fields(案件、当事人等的本体映射)
2. 触发:
   - GET /api/v1/graph/legal/extract/ontology-fields
3. 验证配置返回

预期结果:
✅ 本体配置

---

【测试场景 4:抽取模板列表】
1. 触发:
   - GET /api/v1/graph/legal/extract/templates
2. 验证返回模板列表

预期结果:
✅ 模板列表

---

【测试场景 5:法律导入(批量)】
1. 上传 JSON 文件(多个案件)
2. 触发:
   - POST /api/v1/graph/legal/import
3. 验证导入结果

预期结果:
✅ 批量导入

---

【测试场景 6:法律导出】
1. 选择导出格式
2. 触发:
   - GET /api/v1/graph/legal/export
3. 验证下载文件

预期结果:
✅ 导出成功

---

【测试场景 7-10:分别导入节点/边/法条/案件】
1. POST /api/v1/graph/legal/nodes
2. POST /api/v1/graph/legal/edges
3. POST /api/v1/graph/legal/provisions
4. POST /api/v1/graph/legal/cases

预期结果:
✅ 分别导入成功

---

【测试场景 11:抽取数据查看】
1. 在「抽取历史」中查看
2. 验证显示:文件名、抽取时间、抽取数量、状态

预期结果:
✅ 历史展示

---

【测试场景 12:导入结果统计】
1. 查看导入结果
2. 验证:成功数、失败数、错误详情

预期结果:
✅ 统计展示

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 抽取预览 | POST | /api/v1/graph/legal/extract/preview |
| 抽取保存 | POST | /api/v1/graph/legal/extract/save |
| 抽取(通用) | POST | /api/v1/graph/legal/extract |
| 本体字段 | GET | /api/v1/graph/legal/extract/ontology-fields |
| 模板列表 | GET | /api/v1/graph/legal/extract/templates |
| 批量导入 | POST | /api/v1/graph/legal/import |
| 节点导入 | POST | /api/v1/graph/legal/nodes |
| 边导入 | POST | /api/v1/graph/legal/edges |
| 法条导入 | POST | /api/v1/graph/legal/provisions |
| 案件导入 | POST | /api/v1/graph/legal/cases |
| 法律导出 | GET | /api/v1/graph/legal/export |

---

【问题诊断】
- 抽取失败 → 检查 LLM / Prompt
- 导入失败 → 检查 Neo4j / 本体匹配
- 数据缺失 → 检查字段映射
```

#### G-20.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【法律知识图谱】模块。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: /legal-kg
- 权限前缀: graphiti:legal:

【前端文件清单】
- 法律 KG 页: ontograph-frontend/src/views/legal-kg/index.vue
- API 封装:
  - ontograph-frontend/src/api/legal-extract.ts
  - ontograph-frontend/src/api/legal-import.ts
  - ontograph-frontend/src/api/legal-kg-data.ts

【后端文件清单】
- Controller: .../controller/admin/LegalExtractController.java
- Controller: .../controller/admin/LegalImportController.java
- Service: .../service/LegalExtractService.java + impl
- Service: .../service/LegalImportService.java + impl
- VO: .../vo/legal/*

【API 端点】
(见 G-20.2 测试提示词)

【功能需求】
1. 裁判文书智能抽取(LLM)
2. 抽取预览 + 保存
3. 多种实体类型(案件/当事人/法官/律师/法院/证据/判决/法条)
4. 法律本体映射
5. 批量导入 + 导出

【LLM 集成】
- 法律专用 Prompt 模板
- 结构化输出(Schema 约束)

【Neo4j 集成】
- 节点:Case, Party, Judge, Lawyer, Court, Evidence, Judgment, Provision
- 关系:PARTICIPATES_IN, REPRESENTED_BY, ADJUDICATES, etc.

【测试验证】
实现完成后,使用 G-20.2 测试提示词中的测试场景验证。

【交付物清单】
- [ ] 前端法律 KG 页 .vue
- [ ] 前端 API 封装 .ts
- [ ] 后端 Controller(已存在)
- [ ] 后端 Service(已存在)
- [ ] 法律本体定义
- [ ] LLM Prompt 模板
- [ ] 通过 G-20.2 所有测试场景
```

---

### SYS-00 登录注销与首页

**页面路径**: `http://localhost:5173/login`、首页 `/dashboard`  
**前端布局文件**: `ontograph-frontend/src/components/Layout/BasicLayout.vue`, `Header.vue`, `Sidebar.vue`  
**API 文件**: `ontograph-frontend/src/api/auth.ts` (`authApi`)  
**后端控制器**: `ontograph-backend/src/main/java/com/ontograph/module/auth/controller/AuthController.java`

#### SYS-00.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| SYS-00-01 | 登录页渲染 | 验证登录页 UI 完整加载 |
| SYS-00-02 | 正确账号登录 | admin/admin123 → 首页 |
| SYS-00-03 | 错误密码登录 | 错误密码 → 错误提示 |
| SYS-00-04 | 空账号登录 | 空用户名/密码 → 表单校验 |
| SYS-00-05 | 注销功能 | 点击注销 → 返回登录页 |
| SYS-00-06 | 首页信息展示 | 头部/侧边栏/内容区正确显示 |
| SYS-00-07 | 首页导航菜单 | 四大分组菜单正确渲染 |
| SYS-00-08 | 未登录访问受限页 | 重定向到登录页 |

#### SYS-00.2 测试提示词

```
/browser 或 /open-gstack-browser
打开 OntoGraph Console,执行完整的登录、注销与首页信息展示测试。

【前置操作】
1. 确认前端(:5173)和后端(:9090)均已启动
2. 确认浏览器无已登录 Cookie(建议用无痕/隐私模式)

---

【测试场景 1:登录页渲染】
1. 打开 http://localhost:5173/login
2. 验证页面正常渲染,无白屏、无 JS 错误
3. 验证包含以下元素:
   - Logo 区域(OntoGraph Console 标题)
   - 用户名输入框(username)
   - 密码输入框(password)
   - 登录按钮(Login / 登录)
   - 语言切换组件(如存在)
4. 验证表单布局美观、输入框对齐

预期结果:
✅ 页面 8 列完整
✅ Logo 标题显示
✅ 用户名/密码输入框存在
✅ 登录按钮可点击

---

【测试场景 2:正确账号登录】
1. 在用户名输入框输入:admin
2. 在密码输入框输入:admin123
3. 点击「登录」按钮
4. 验证:
   - API POST /auth/login 成功返回 token
   - 页面跳转到首页 /dashboard
   - Header 右上角显示用户昵称(admin)
   - 左侧 Sidebar 正确加载四大分组菜单
   - 控制台无 Error 级别报错

预期结果:
✅ 登录 API 返回成功
✅ 跳转到 /dashboard
✅ Header 显示用户名
✅ Sidebar 菜单完整渲染

---

【测试场景 3:错误密码登录】
1. 在用户名输入框输入:admin
2. 在密码输入框输入:wrongpassword
3. 点击「登录」按钮
4. 验证:
   - API POST /auth/login 返回错误(code≠0 或 HTTP 401)
   - 页面不跳转,停留在登录页
   - 显示错误提示信息(「用户名或密码错误」)

预期结果:
✅ 错误提示显示
✅ 页面不跳转
✅ 可重新输入

---

【测试场景 4:空账号登录(前端校验)】
1. 不填写任何内容,直接点击「登录」
2. 验证浏览器前端表单校验生效
3. 验证必填提示(如:请输入用户名)

预期结果:
✅ 前端表单校验生效
✅ 提示「请输入用户名」
✅ 不触发后端请求

---

【测试场景 5:注销功能】
1. 在首页右上角找到用户下拉菜单(点击头像或用户名)
2. 点击「注销」或「Logout」
3. 验证:
   - API POST /auth/logout 成功
   - 页面跳转到登录页 /login
   - localStorage/SessionStorage 中的 Token 被清除
   - 再次访问 /dashboard 被重定向到 /login

预期结果:
✅ 注销成功跳转
✅ Token 已清除
✅ 受限页面重定向正常

---

【测试场景 6:首页信息展示(Header + Sidebar)】
1. 登录成功后停留在首页 /dashboard
2. 验证 Header 组件:
   - Logo 区域(OntoGraph Console 文字/图标)可点击
   - 点击 Logo 返回首页
   - 右上角:语言切换器
   - 右上角:通知铃铛(带 Badge)
   - 右上角:用户名/头像下拉菜单(个人中心/注销)
3. 验证 Sidebar 组件:
   - 四大分组菜单:
     a. 「图谱管理」(含图谱列表/Graph IDE/时序历史/社区发现)
     b. 「数据管理」(含类管理/属性管理/约束管理/实体管理/边管理/导入/导出/法律知识图谱)
     c. 「工具」(含混合搜索/自定义指令/Prompt 管理)
     d. 「系统管理」(含用户管理/角色管理/菜单管理/参数配置/操作日志/系统监控)
   - 每个分组可折叠/展开
   - 当前激活菜单项高亮显示

预期结果:
✅ Header Logo 可点击
✅ 语言切换器存在
✅ 通知铃铛存在
✅ 用户下拉菜单正常
✅ Sidebar 四大分组完整
✅ 菜单可折叠/展开
✅ 当前页菜单高亮

---

【测试场景 7:首页内容展示(Dashboard)】
1. 停留在 /dashboard 页面
2. 验证 Dashboard 页面内容加载:
   - 页面标题/欢迎语
   - 统计数据卡片(如图谱数量、实体数量等)
   - 内容区域正常渲染
   - 无 Loading 卡死
   - 无 JS 错误

预期结果:
✅ Dashboard 页面完整渲染
✅ 无 JS 错误

---

【测试场景 8:未登录访问受限页(重定向)】
1. 清除浏览器 Cookie 和 Token
2. 直接访问 http://localhost:5173/dashboard
3. 验证页面自动重定向到 /login
4. 登录后验证正确跳转回 /dashboard

预期结果:
✅ 未登录时重定向到 /login
✅ 登录后正确跳转

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 用户登录 | POST | /auth/login |
| 用户注销 | POST | /auth/logout |
| 获取用户信息 | GET | /auth/info |
| 获取菜单树 | GET | /auth/menus |

---

【问题诊断】
- 登录页白屏 → 检查 Vue 路由配置 login 组件是否正确注册
- 登录后不跳转 → 检查 authStore.login() 是否正确处理 LoginResult.token
- Header 不显示用户名 → 检查 authApi.getInfo() 返回的 nickname/username 字段
- Sidebar 菜单不加载 → 检查 authApi.getMenus() 返回的 MenuItem[] 树结构
- 注销后仍能访问 → 检查路由守卫 beforeEach 是否正确拦截
- Token 不存储 → 检查 auth.ts login() 返回后是否写入 localStorage/sessionStorage
- 登录 API 404 → 检查后端 AuthController 是否映射 /auth/login
```

#### SYS-00.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【登录、注销与首页】模块(若已存在则增强)。

【模块信息】
- 服务: ontograph-java(:9090)
- 前端入口: http://localhost:5173
- 路由: /login, /dashboard
- 权限前缀: 无(登录/注销属公共接口)

【前端文件清单】
- 布局容器: ontograph-frontend/src/components/Layout/BasicLayout.vue
- 头部组件: ontograph-frontend/src/components/Layout/Header.vue
- 侧边栏组件: ontograph-frontend/src/components/Layout/Sidebar.vue
- 登录页: ontograph-frontend/src/views/login/index.vue
- 首页: ontograph-frontend/src/views/dashboard/index.vue
- 认证 API: ontograph-frontend/src/api/auth.ts (authApi 对象)
- Token 工具: ontograph-frontend/src/utils/auth.ts
- 用户状态: ontograph-frontend/src/store/modules/user.ts
- 路由守卫: ontograph-frontend/src/router/index.ts (beforeEach)

【后端文件清单】
- Controller: ontograph-backend/src/main/java/com/ontograph/module/auth/controller/AuthController.java
- Service: .../service/AuthService.java + impl
- DTO: .../dto/LoginRequest.java, LoginResponse.java
- UserDO: .../dal/dataobject/UserDO.java
- Mapper: .../dal/mysql/UserMapper.java
- Security Config: .../security/ 和 .../config/SecurityConfig.java

【API 端点】
| 操作 | 方法 | 路径 |
|------|------|------|
| 用户登录 | POST | /auth/login |
| 用户注销 | POST | /auth/logout |
| 获取用户信息 | GET | /auth/info |
| 获取菜单树 | GET | /auth/menus |

【功能需求 — 前端】
1. 登录页:用户名+密码表单,提交后调用 authApi.login()
2. 登录成功后:
   - 将 Token 写入 localStorage
   - 调用 authApi.getInfo() 获取用户信息并存入 userStore
   - 调用 authApi.getMenus() 获取菜单树并存入 permissionStore
   - 跳转到 /dashboard
3. Header 组件:
   - Logo 点击返回首页
   - 语言切换器组件(LanguageSwitcher)
   - 通知铃铛(调用 notificationApi.getUnreadCount())
   - 用户下拉菜单(个人中心/注销)
4. Sidebar 组件:
   - 四大分组:图谱管理/数据管理/工具/系统管理
   - 每个分组可折叠/展开
   - 当前路由菜单高亮(active)
   - 支持动态菜单(从 permissionStore 读取)和静态菜单兜底
5. 路由守卫:
   - 未登录访问 /dashboard 等受保护路由 → 重定向 /login
   - 已登录访问 /login → 跳转 /dashboard
6. 注销:
   - 调用 authApi.logout()
   - 清除 Token 和用户信息
   - 跳转 /login

【功能需求 — 后端】
1. POST /auth/login:
   - 接收 {username, password}
   - 验证用户名+密码
   - 返回 JWT Token + 用户信息 + 菜单树
2. POST /auth/logout:
   - 使当前 Token 失效(或记录注销日志)
3. GET /auth/info:
   - 从 Token 解析用户 ID
   - 返回用户详细信息
4. GET /auth/menus:
   - 从 Token 解析用户角色
   - 返回该角色对应的菜单树结构

【LoginResponse 返回格式示例】
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 1,
    "username": "admin",
    "nickname": "管理员",
    "email": "admin@example.com",
    "avatar": null
  },
  "menus": [
    {
      "id": 1,
      "name": "图谱管理",
      "path": "",
      "icon": "ShareAltOutlined",
      "children": [
        { "id": 11, "name": "图谱列表", "path": "/graph/list", "icon": "UnorderedListOutlined" },
        { "id": 12, "name": "Graph IDE", "path": "/graph/ide", "icon": "CodeOutlined" }
      ]
    }
  ]
}
```

【UI 规范】
- UI 库: ant-design-vue
- 登录页:居中卡片布局,含 Logo、用户名、密码、登录按钮
- Header:高度 56px,深色背景(#0a0e1a),含 Logo、语言切换、通知铃铛、用户下拉
- Sidebar:宽度 240px,深色背景,折叠箭头菜单,当前页高亮(#eceff6)
- 通知 Badge:antd Badge 组件
- 用户头像:antd Avatar,取用户名首字母

【参考实现】
- 布局参考:ontograph-frontend/src/components/Layout/BasicLayout.vue
- Header 参考:ontograph-frontend/src/components/Layout/Header.vue
- Sidebar 参考:ontograph-frontend/src/components/Layout/Sidebar.vue
- Auth API 参考:ontograph-frontend/src/api/auth.ts
- Store 参考:ontograph-frontend/src/store/modules/user.ts, permission.ts
- 路由守卫参考:ontograph-frontend/src/router/index.ts 的 beforeEach

【测试验证】
实现完成后,使用 SYS-00.2 测试提示词中的测试场景验证,重点验证:
1. 登录页 8 列完整(Logo/用户名/密码/按钮)
2. admin/admin123 登录成功跳转 /dashboard
3. 错误密码显示错误提示
4. 空账号触发前端校验
5. 注销后 Token 清除并重定向 /login
6. Header 显示用户名/通知铃铛/用户菜单
7. Sidebar 四大分组完整(图谱管理/数据管理/工具/系统管理)
8. Dashboard 页面正常渲染
9. 未登录访问 /dashboard 重定向 /login

【交付物清单】
- [ ] 前端登录页 .vue(login/index.vue)
- [ ] 前端首页 .vue(dashboard/index.vue)
- [ ] 前端布局组件 BasicLayout.vue / Header.vue / Sidebar.vue
- [ ] 前端认证 API auth.ts(authApi)
- [ ] 前端 Token 工具 utils/auth.ts
- [ ] 前端用户状态 store/modules/user.ts
- [ ] 前端路由守卫 router/index.ts
- [ ] 后端 AuthController(含 /auth/login, /logout, /info, /menus)
- [ ] 后端 AuthService + 实现
- [ ] 后端 LoginRequest / LoginResponse DTO
- [ ] 后端 SecurityConfig 配置(公开 /auth/login)
- [ ] JWT Token 生成与验证逻辑
- [ ] 通过 SYS-00.2 所有测试场景
```

---

### SYS-01 参数配置

**页面路径**: 左侧菜单「系统管理」→「参数配置」
**源码文件**: `ontograph-frontend/src/views/system/config/index.vue`
**后端控制器**: `ontograph-backend/src/main/java/com/ontograph/module/system/controller/SystemConfigController.java`
**API 文件**: `ontograph-frontend/src/api/system.ts` (`systemApi`)
**权限标识**: `system:config:create`, `system:config:update`, `system:config:delete`, `system:config:query`

#### SYS-01.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| SYS-01-01 | 参数列表加载 | 验证分页/筛选 |
| SYS-01-02 | 新增参数 | 表单校验 + 唯一性 |
| SYS-01-03 | 编辑参数 | 数据回填 + 修改 |
| SYS-01-04 | 删除参数 | 二次确认 |
| SYS-01-05 | 按 Key 查询 | 详情弹窗 |
| SYS-01-06 | 状态切换 | 启用/禁用 |

#### SYS-01.2 测试提示词

```
/browser 或 /open-gstack-browser
打开参数配置页面,执行完整的参数配置 CRUD 测试。

【前置操作】
1. 使用 admin/admin123 登录系统
2. 在左侧菜单点击「系统管理」→「参数配置」
3. 等待页面加载完成

---

【测试场景 1:参数列表加载与基础显示】
1. 验证参数列表正常加载
2. 检查列:配置键(configKey)、配置值(configValue)、配置名称(configName)、分组(groupName)、排序、状态、创建时间、操作
3. 验证分页功能正常
4. 验证顶部「新增」按钮可点击
5. 验证搜索条件:configKey / configName / groupName / status

预期结果:
✅ 表格 8 列完整
✅ 分页正常
✅ 顶部「新增」按钮存在

---

【测试场景 2:搜索功能】
1. 在「配置键」输入框输入关键词,点击「搜索」
2. 验证列表过滤出匹配 configKey 的结果
3. 在「配置名称」输入框输入关键词
4. 验证列表过滤出匹配 configName 的结果
5. 选择「分组」下拉过滤
6. 选择「状态」过滤
7. 点击「重置」,验证条件清空,列表恢复

预期结果:
✅ configKey 搜索过滤正确
✅ configName 搜索过滤正确
✅ 分组/状态过滤生效
✅ 重置清空条件

---

【测试场景 3:新增参数】
1. 点击顶部「新增」按钮
2. 验证弹出 FormModal,标题为「新增参数」
3. 验证表单字段:
   - configKey(必填,唯一)
   - configValue(必填)
   - configName(必填)
   - configDescription(可选)
   - configType(单选:1-文本/2-数字/3-布尔/4-JSON)
   - groupName(必填)
   - sortNum(数字)
   - status(单选:0-禁用/1-启用)
4. 填写:
   - configKey:test.config.key
   - configValue:test_value_001
   - configName:测试参数
   - configDescription:自动化测试创建
   - configType:1
   - groupName:DEFAULT
   - sortNum:100
   - status:1
5. 提交并验证:
   - API POST /api/v1/admin/system/config/create
   - 显示「新增成功」
   - 弹窗关闭,列表刷新

预期结果:
✅ 新增弹窗表单字段完整
✅ 提交成功,列表刷新

---

【测试场景 4:编辑参数】
1. 点击刚创建参数的「编辑」按钮
2. 验证弹窗弹出,数据正确回填
3. 修改 configValue 为 test_value_001_modified
4. 提交并验证:
   - API PUT /api/v1/admin/system/config/{id}
   - 列表中数据更新

预期结果:
✅ 编辑弹窗正确回填
✅ 修改提交后数据更新

---

【测试场景 5:configKey 唯一性校验】
1. 新增参数,configKey 设为已存在的 key
2. 提交验证:后端返回重复错误
3. 弹窗不关闭,显示错误提示

预期结果:
✅ 后端唯一性校验生效

---

【测试场景 6:删除参数】
1. 点击参数的「删除」按钮
2. 验证确认弹窗
3. 点击确认,验证删除成功
4. 验证列表刷新,参数消失

预期结果:
✅ 删除确认正常
✅ 删除成功后列表刷新

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 列表分页 | GET | /api/v1/admin/system/config/list |
| 全部配置 | GET | /api/v1/admin/system/config/all |
| 配置详情 | GET | /api/v1/admin/system/config/{id} |
| 按 Key 查询 | GET | /api/v1/admin/system/config/key/{key} |
| 创建配置 | POST | /api/v1/admin/system/config/create |
| 更新配置 | PUT | /api/v1/admin/system/config/{id} |
| 删除配置 | DELETE | /api/v1/admin/system/config/{id} |

---

【问题诊断】
- 列表不加载 → 检查 systemApi.getConfigs() 是否成功
- 编辑时数据不回填 → 检查 FormModal watch 逻辑
- 删除失败 → 检查参数是否被系统引用(后端保护)
- configKey 重复 → 后端唯一性约束冲突
```

#### SYS-01.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【参数配置】模块(若已存在则增强)。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: 左侧菜单「系统管理」→「参数配置」
- 路由: /system/config
- 权限前缀: system:config:
- 涉及权限码:
  - system:config:create(新增配置)
  - system:config:update(编辑配置)
  - system:config:delete(删除配置)
  - system:config:query(查询配置)

【前端文件清单】
- 主页面: ontograph-frontend/src/views/system/config/index.vue
- API 封装: ontograph-frontend/src/api/system.ts (systemApi 对象)

【后端文件清单】
- Controller: ontograph-backend/src/main/java/com/ontograph/module/system/controller/SystemConfigController.java
- Service: .../service/SystemConfigService.java + impl
- DO: .../dal/dataobject/SystemConfigDO.java
- Mapper: .../dal/mysql/SystemConfigMapper.java

【API 端点】
| 操作 | 方法 | 路径 |
|------|------|------|
| 列表分页 | GET | /api/v1/admin/system/config/list |
| 全部配置 | GET | /api/v1/admin/system/config/all |
| 配置详情 | GET | /api/v1/admin/system/config/{id} |
| 按 Key 查询 | GET | /api/v1/admin/system/config/key/{key} |
| 创建配置 | POST | /api/v1/admin/system/config/create |
| 更新配置 | PUT | /api/v1/admin/system/config/{id} |
| 删除配置 | DELETE | /api/v1/admin/system/config/{id} |

【功能需求】
1. 支持参数配置的创建、查询、更新、删除
2. configKey 唯一性校验
3. 支持按配置键(configKey)快速查询
4. 支持分组管理(groupName)
5. 支持状态切换(启用/禁用)
6. 支持按配置类型(text/number/boolean/json)

【UI 规范】
- UI 库: ant-design-vue
- 列表字段:configKey、configValue、configName、groupName、sortNum、status、createTime、操作
- 弹窗组件:AntdModal
- 操作按钮:编辑、删除
- 状态:使用 Switch 组件
- 新增按钮:页面顶部

【MySQL 集成】
- 表:system_config(已存在)
- DO 字段:id, config_key, config_value, config_name, config_description, config_type, group_name, sort_num, status, deleted, create_time, update_time
- 使用 MyBatis-Plus 逻辑删除(deleted 字段)

【参考实现】
- 参考 ontograph-frontend/src/views/system/user/index.vue 的列表+弹窗模式
- 复用 ontograph-frontend/src/api/system.ts 的 systemApi
- 后端 CRUD 参考 UserController 的标准模式

【测试验证】
实现完成后,使用 SYS-01.2 测试提示词中的测试场景验证,重点验证:
1. 列表 8 列完整渲染
2. 多条件搜索过滤(4 个搜索条件)
3. 新增/编辑/删除 CRUD 流程
4. configKey 唯一性校验生效
5. 按 Key 查询 API 返回正确
6. 状态切换(启用/禁用)生效

【交付物清单】
- [ ] 前端主页面 .vue(config/index.vue)
- [ ] 前端 API 封装 .ts(已存在,需增强)
- [ ] 后端 Controller(已存在)
- [ ] 后端 Service 接口 + 实现(已存在)
- [ ] 后端 DO / Mapper(已存在)
- [ ] 路由注册(/system/config)
- [ ] 菜单注册(左侧菜单「系统管理」→「参数配置」)
- [ ] 权限码注册(system:config:*)
- [ ] 通过 SYS-01.2 所有测试场景
```

---

### SYS-02 用户管理

**页面路径**: 左侧菜单「系统管理」→「用户管理」
**源码文件**: `ontograph-frontend/src/views/system/user/index.vue`
**后端控制器**: `ontograph-backend/src/main/java/com/ontograph/module/system/controller/UserController.java`
**API 文件**: `ontograph-frontend/src/api/user.ts` (`userApi`)
**权限标识**: `system:user:create`, `system:user:update`, `system:user:delete`, `system:user:query`

#### SYS-02.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| SYS-02-01 | 用户列表加载 | 分页/搜索/筛选 |
| SYS-02-02 | 新增用户 | 用户名唯一性 + 密码强度 |
| SYS-02-03 | 编辑用户 | 数据回填 |
| SYS-02-04 | 删除用户 | 二次确认 |
| SYS-02-05 | 批量删除 | 多选删除 |
| SYS-02-06 | 状态切换 | 启用/禁用 |
| SYS-02-07 | 分配角色 | 角色多选 |
| SYS-02-08 | 重置密码 | 后端未实现提示 |

#### SYS-02.2 测试提示词

```
/browser 或 /open-gstack-browser
打开用户管理页面,执行完整的用户管理 CRUD 测试。

【前置操作】
1. 使用 admin/admin123 登录系统
2. 在左侧菜单点击「系统管理」→「用户管理」
3. 等待页面加载完成

---

【测试场景 1:用户列表加载与基础显示】
1. 验证用户列表正常加载
2. 检查列:用户名(username)、昵称(nickname)、邮箱(email)、手机(phone)、状态、创建时间、操作
3. 验证分页功能正常
4. 验证顶部「新增」按钮可点击
5. 验证搜索条件:username / nickname / status

预期结果:
✅ 表格 7 列完整
✅ 分页正常
✅ 顶部「新增」按钮存在

---

【测试场景 2:搜索与筛选】
1. 在「用户名」输入框输入关键词,点击「搜索」
2. 验证列表过滤出匹配 username 的结果(模糊匹配)
3. 在「昵称」输入框输入关键词
4. 验证列表过滤出匹配 nickname 的结果
5. 选择「状态」过滤(启用/禁用)
6. 点击「重置」,验证条件清空

预期结果:
✅ username 模糊搜索生效
✅ nickname 模糊搜索生效
✅ 状态过滤生效

---

【测试场景 3:新增用户】
1. 点击「新增」按钮
2. 验证弹出 UserFormModal,标题为「新增用户」
3. 验证表单字段:
   - username(必填,唯一)
   - nickname(必填)
   - password(必填,强度提示)
   - email(可选,邮箱格式)
   - phone(可选,手机号格式)
   - status(单选:0-禁用/1-启用)
4. 填写:
   - username:test_user_001
   - nickname:测试用户
   - password:Test@123456
   - email:test@example.com
   - phone:13800138000
   - status:1
5. 提交并验证:
   - API POST /api/v1/admin/system/user/create
   - 显示「新增成功」
   - 弹窗关闭,列表刷新

预期结果:
✅ 新增弹窗表单字段完整
✅ 密码强度校验生效
✅ 邮箱/手机号格式校验生效
✅ username 唯一性校验生效

---

【测试场景 4:编辑用户】
1. 点击刚创建用户的「编辑」按钮
2. 验证弹窗弹出,数据正确回填
3. 修改 nickname 为「测试用户_modified」
4. 提交并验证:
   - API PUT /api/v1/admin/system/user/update
   - 列表中数据更新

预期结果:
✅ 编辑弹窗正确回填
✅ 修改后列表更新

---

【测试场景 5:删除用户】
1. 点击用户的「删除」按钮
2. 验证确认弹窗
3. 点击确认,验证删除成功
4. 验证列表刷新,用户消失

预期结果:
✅ 删除确认正常
✅ 删除成功后列表刷新

---

【测试场景 6:批量删除用户】
1. 多选 2-3 个用户
2. 点击「批量删除」按钮
3. 验证确认弹窗
4. 确认后,验证所选用户全部删除

预期结果:
✅ 批量删除生效
✅ 列表自动刷新

---

【测试场景 7:状态切换】
1. 点击某个用户的「启用/禁用」开关
2. 验证状态切换
3. 触发 API PUT /api/v1/admin/system/user/update
4. 验证列表显示新状态

预期结果:
✅ 状态切换生效

---

【测试场景 8:分配角色(若前端支持)】
1. 点击用户的「分配角色」按钮
2. 验证弹出角色选择弹窗
3. 验证角色列表(来自 /admin/system/role/list)
4. 选择 1-N 个角色,提交
5. 验证后端保存

预期结果:
✅ 角色选择弹窗正常
✅ 角色可多选

---

【测试场景 9:重置密码(后端未实现)】
1. 点击「重置密码」按钮
2. 验证弹出错误提示「后端尚未实现此功能」

预期结果:
✅ 错误提示正常显示

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 用户列表 | GET | /api/v1/admin/system/user/list |
| 用户详情 | GET | /api/v1/admin/system/user/get/{userId} |
| 创建用户 | POST | /api/v1/admin/system/user/create |
| 更新用户 | PUT | /api/v1/admin/system/user/update |
| 删除用户 | DELETE | /api/v1/admin/system/user/delete/{userId} |
| 更新状态 | PUT | /api/v1/admin/system/user/update(传 status) |

---

【问题诊断】
- 列表不加载 → 检查 userApi.getUsers() 是否成功
- 编辑时数据不回填 → 检查 UserFormModal watch 逻辑
- 删除失败 → 检查用户是否有关联数据(后端保护)
- 密码强度不足 → 前端校验提示
- 重置密码报错 → 后端尚未实现,显示友好提示
```

#### SYS-02.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【用户管理】模块(若已存在则增强)。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: 左侧菜单「系统管理」→「用户管理」
- 路由: /system/user
- 权限前缀: system:user:
- 涉及权限码:
  - system:user:create(新增用户)
  - system:user:update(编辑用户)
  - system:user:delete(删除用户)
  - system:user:query(查询用户)

【前端文件清单】
- 主页面: ontograph-frontend/src/views/system/user/index.vue
- API 封装: ontograph-frontend/src/api/user.ts (userApi 对象)

【后端文件清单】
- Controller: ontograph-backend/src/main/java/com/ontograph/module/system/controller/UserController.java
- Service: .../service/UserService.java + impl
- DO: .../dal/dataobject/UserDO.java
- Mapper: .../dal/mysql/UserMapper.java
- 关联表:user_role(用户-角色关联)

【API 端点】
| 操作 | 方法 | 路径 |
|------|------|------|
| 用户列表 | GET | /api/v1/admin/system/user/list |
| 用户详情 | GET | /api/v1/admin/system/user/get/{userId} |
| 创建用户 | POST | /api/v1/admin/system/user/create |
| 更新用户 | PUT | /api/v1/admin/system/user/update |
| 删除用户 | DELETE | /api/v1/admin/system/user/delete/{userId} |

【功能需求】
1. 用户 CRUD(创建/查询/更新/删除)
2. username 唯一性校验
3. 密码强度校验(前端 + 后端 BCrypt 加密)
4. 邮箱/手机号格式校验
5. 用户状态切换(启用/禁用)
6. 批量删除(前端多选 + 循环调用单删 API)
7. 用户-角色分配(通过 user_role 关联表)

【UI 规范】
- UI 库: ant-design-vue
- 列表字段:username、nickname、email、phone、status、createTime、操作
- 弹窗组件:AntdModal + AntdForm
- 操作按钮:编辑、删除、分配角色
- 状态:Switch 组件
- 搜索:AntdInputSearch
- 分页:AntdPagination

【MySQL 集成】
- 表:system_user(主表)、user_role(关联表)
- DO 字段:id, username, password(Bcrypt), nickname, email, mobile, status, deleted, create_time, update_time
- 逻辑删除:deleted 字段
- 密码加密:Spring Security BCryptPasswordEncoder

【Spring Security 集成】
- UserDetailsServiceImpl 实现 UserDetailsService
- 密码加密:BCryptPasswordEncoder
- 登录认证:AuthenticationManager

【参考实现】
- 参考 ontograph-frontend/src/views/system/user/index.vue(已存在)
- 复用 ontograph-frontend/src/api/user.ts 的 userApi
- 复用 ontograph-frontend/src/api/role.ts 的 roleApi(分配角色下拉)
- 后端 CRUD 参考 UserController(已存在)

【测试验证】
实现完成后,使用 SYS-02.2 测试提示词中的测试场景验证,重点验证:
1. 列表 7 列完整渲染
2. 多条件搜索过滤(3 个搜索条件)
3. 新增/编辑/删除/批量删除 CRUD 流程
4. username 唯一性校验生效
5. 密码强度校验生效
6. 邮箱/手机号格式校验生效
7. 状态切换生效
8. 用户-角色分配(若支持)

【交付物清单】
- [ ] 前端主页面 .vue(user/index.vue)
- [ ] 前端 API 封装 .ts(已存在)
- [ ] 后端 Controller(已存在)
- [ ] 后端 Service 接口 + 实现(已存在)
- [ ] 后端 DO / Mapper(已存在)
- [ ] user_role 关联表(已存在)
- [ ] Spring Security 集成(已存在)
- [ ] 路由注册(/system/user)
- [ ] 菜单注册(左侧菜单「系统管理」→「用户管理」)
- [ ] 权限码注册(system:user:*)
- [ ] 通过 SYS-02.2 所有测试场景
```

---

### SYS-03 角色管理

**页面路径**: 左侧菜单「系统管理」→「角色管理」
**源码文件**: `ontograph-frontend/src/views/system/role/index.vue`
**后端控制器**: `ontograph-backend/src/main/java/com/ontograph/module/system/controller/RoleController.java`
**API 文件**: `ontograph-frontend/src/api/role.ts` (`roleApi`)
**权限标识**: `system:role:create`, `system:role:update`, `system:role:delete`, `system:role:query`

#### SYS-03.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| SYS-03-01 | 角色列表加载 | 搜索/筛选 |
| SYS-03-02 | 新增角色 | code 唯一性 |
| SYS-03-03 | 编辑角色 | 数据回填 |
| SYS-03-04 | 删除角色 | 二次确认 |
| SYS-03-05 | 状态切换 | 启用/禁用 |
| SYS-03-06 | 分配菜单权限 | 菜单树多选 |
| SYS-03-07 | 角色下拉数据 | 供用户分配使用 |

#### SYS-03.2 测试提示词

```
/browser 或 /open-gstack-browser
打开角色管理页面,执行完整的角色管理 CRUD 测试。

【前置操作】
1. 使用 admin/admin123 登录系统
2. 在左侧菜单点击「系统管理」→「角色管理」
3. 等待页面加载完成

---

【测试场景 1:角色列表加载与基础显示】
1. 验证角色列表正常加载
2. 检查列:角色名称(name)、角色编码(code)、描述(description)、状态、创建时间、操作
3. 验证分页功能正常(注意:后端返回全量,前端做分页过滤)
4. 验证顶部「新增」按钮可点击
5. 验证搜索条件:name / code / status

预期结果:
✅ 表格 6 列完整
✅ 分页正常(前端过滤)
✅ 顶部「新增」按钮存在

---

【测试场景 2:搜索与筛选】
1. 在「角色名称」输入框输入关键词,点击「搜索」
2. 验证列表过滤(前端 includes 匹配)
3. 在「角色编码」输入框输入关键词
4. 验证列表过滤
5. 选择「状态」过滤
6. 点击「重置」,验证条件清空

预期结果:
✅ 名称搜索(前端 includes)生效
✅ 编码搜索生效
✅ 状态过滤生效

---

【测试场景 3:新增角色】
1. 点击「新增」按钮
2. 验证弹出 RoleFormModal,标题为「新增角色」
3. 验证表单字段:
   - name(必填)
   - code(必填,唯一,大写英文)
   - description(可选)
   - status(单选:0-禁用/1-启用)
4. 填写:
   - name:测试角色
   - code:TEST_ROLE
   - description:自动化测试创建
   - status:1
5. 提交并验证:
   - API POST /api/v1/admin/system/role/create
   - 显示「新增成功」
   - 弹窗关闭,列表刷新

预期结果:
✅ 新增弹窗表单字段完整
✅ code 唯一性校验生效

---

【测试场景 4:编辑角色】
1. 点击刚创建角色的「编辑」按钮
2. 验证弹窗弹出,数据正确回填
3. 修改 description 为「自动化测试创建_modified」
4. 提交并验证:
   - API PUT /api/v1/admin/system/role/update
   - 列表中数据更新

预期结果:
✅ 编辑弹窗正确回填
✅ 修改后列表更新

---

【测试场景 5:删除角色】
1. 点击角色的「删除」按钮
2. 验证确认弹窗
3. 点击确认,验证删除成功
4. 验证列表刷新,角色消失

预期结果:
✅ 删除确认正常
✅ 删除成功后列表刷新
✅ 有关联用户时拒绝删除(后端保护)

---

【测试场景 6:状态切换】
1. 点击某个角色的「启用/禁用」开关
2. 验证状态切换
3. 触发 API PUT /api/v1/admin/system/role/update
4. 验证列表显示新状态

预期结果:
✅ 状态切换生效

---

【测试场景 7:分配菜单权限(若前端支持)】
1. 点击角色的「分配菜单」按钮
2. 验证弹出菜单树弹窗
3. 验证菜单树(来自 /admin/system/menu/list)
4. 多选菜单,提交
5. 验证 role_menu 关联表更新

预期结果:
✅ 菜单树渲染正常
✅ 菜单可多选
✅ 权限分配保存

---

【测试场景 8:角色下拉数据(供用户管理使用)】
1. 在「用户管理 → 新增用户 → 分配角色」中
2. 验证角色下拉显示
3. 验证调用 /api/v1/admin/system/role/list

预期结果:
✅ 角色下拉正常

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 角色列表(全量) | GET | /api/v1/admin/system/role/list |
| 角色详情 | GET | /api/v1/admin/system/role/get/{roleId} |
| 创建角色 | POST | /api/v1/admin/system/role/create |
| 更新角色 | PUT | /api/v1/admin/system/role/update |
| 删除角色 | DELETE | /api/v1/admin/system/role/delete/{roleId} |
| 更新状态 | PUT | /api/v1/admin/system/role/update(传 status) |

---

【问题诊断】
- 列表不加载 → 检查 roleApi.getRoles() 是否成功(后端返回全量)
- 编辑时数据不回填 → 检查 RoleFormModal watch 逻辑
- 删除失败 → 检查角色是否被用户引用(后端保护)
- code 重复 → 后端唯一性约束冲突
- 菜单权限不生效 → 检查 role_menu 关联表写入
```

#### SYS-03.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【角色管理】模块(若已存在则增强)。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: 左侧菜单「系统管理」→「角色管理」
- 路由: /system/role
- 权限前缀: system:role:
- 涉及权限码:
  - system:role:create(新增角色)
  - system:role:update(编辑角色)
  - system:role:delete(删除角色)
  - system:role:query(查询角色)

【前端文件清单】
- 主页面: ontograph-frontend/src/views/system/role/index.vue
- API 封装: ontograph-frontend/src/api/role.ts (roleApi 对象)

【后端文件清单】
- Controller: ontograph-backend/src/main/java/com/ontograph/module/system/controller/RoleController.java
- Service: .../service/RoleService.java + impl
- DO: .../dal/dataobject/RoleDO.java
- Mapper: .../dal/mysql/RoleMapper.java
- 关联表:role_menu(角色-菜单关联)

【API 端点】
| 操作 | 方法 | 路径 |
|------|------|------|
| 角色列表 | GET | /api/v1/admin/system/role/list |
| 角色详情 | GET | /api/v1/admin/system/role/get/{roleId} |
| 创建角色 | POST | /api/v1/admin/system/role/create |
| 更新角色 | PUT | /api/v1/admin/system/role/update |
| 删除角色 | DELETE | /api/v1/admin/system/role/delete/{roleId} |

【功能需求】
1. 角色 CRUD
2. code 唯一性校验
3. 角色状态切换(启用/禁用)
4. 角色-菜单权限分配(通过 role_menu 关联表)
5. 角色下拉数据(供用户分配使用)

【UI 规范】
- UI 库: ant-design-vue
- 列表字段:name、code、description、status、createTime、操作
- 弹窗组件:AntdModal + AntdForm
- 操作按钮:编辑、删除、分配菜单
- 状态:Switch 组件
- 菜单分配:AntdTree(可多选)

【MySQL 集成】
- 表:system_role(主表)、role_menu(关联表)
- DO 字段:id, name, code, description, status, deleted, create_time, update_time
- 逻辑删除:deleted 字段

【参考实现】
- 参考 ontograph-frontend/src/views/system/role/index.vue(已存在)
- 复用 ontograph-frontend/src/api/role.ts 的 roleApi
- 复用 ontograph-frontend/src/api/menu.ts 的 menuApi(分配菜单树)
- 后端 CRUD 参考 RoleController(已存在)

【测试验证】
实现完成后,使用 SYS-03.2 测试提示词中的测试场景验证,重点验证:
1. 列表 6 列完整渲染
2. 多条件搜索过滤(3 个搜索条件)
3. 新增/编辑/删除 CRUD 流程
4. code 唯一性校验生效
5. 状态切换生效
6. 菜单权限分配生效

【交付物清单】
- [ ] 前端主页面 .vue(role/index.vue)
- [ ] 前端 API 封装 .ts(已存在)
- [ ] 后端 Controller(已存在)
- [ ] 后端 Service 接口 + 实现(已存在)
- [ ] 后端 DO / Mapper(已存在)
- [ ] role_menu 关联表(已存在)
- [ ] 路由注册(/system/role)
- [ ] 菜单注册(左侧菜单「系统管理」→「角色管理」)
- [ ] 权限码注册(system:role:*)
- [ ] 通过 SYS-03.2 所有测试场景
```

---

### SYS-04 菜单管理

**页面路径**: 左侧菜单「系统管理」→「菜单管理」
**源码文件**: `ontograph-frontend/src/views/system/menu/index.vue`
**后端控制器**: `ontograph-backend/src/main/java/com/ontograph/module/system/controller/MenuController.java`
**API 文件**: `ontograph-frontend/src/api/menu.ts` (`menuApi`)
**权限标识**: `system:menu:create`, `system:menu:update`, `system:menu:delete`, `system:menu:query`

#### SYS-04.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| SYS-04-01 | 菜单树加载 | 树形结构展示 |
| SYS-04-02 | 新增菜单(目录) | type=1 |
| SYS-04-03 | 新增菜单(菜单) | type=2 |
| SYS-04-04 | 新增菜单(按钮) | type=3 |
| SYS-04-05 | 编辑菜单 | 数据回填 |
| SYS-04-06 | 删除菜单 | 二次确认 |
| SYS-04-07 | 状态切换 | 启用/禁用 |
| SYS-04-08 | 父菜单选择 | 树形下拉 |
| SYS-04-09 | 菜单展开/折叠 | 树交互 |

#### SYS-04.2 测试提示词

```
/browser 或 /open-gstack-browser
打开菜单管理页面,执行完整的菜单管理 CRUD 测试。

【前置操作】
1. 使用 admin/admin123 登录系统
2. 在左侧菜单点击「系统管理」→「菜单管理」
3. 等待页面加载完成

---

【测试场景 1:菜单树加载与基础显示】
1. 验证菜单树正常加载
2. 验证树形结构(目录 → 菜单 → 按钮)
3. 检查列:菜单名称(name)、图标(icon)、路径(url)、权限标识(permission)、排序、状态、操作
4. 验证树形展开/折叠交互
5. 验证顶部「新增」按钮可点击

预期结果:
✅ 菜单树形结构正确
✅ 树交互正常
✅ 顶部「新增」按钮存在

---

【测试场景 2:新增目录菜单】
1. 点击「新增」按钮
2. 验证弹出 MenuFormModal,标题为「新增菜单」
3. 验证表单字段:
   - parentId(树形下拉,可为空顶级)
   - name(必填)
   - type(单选:1-目录/2-菜单/3-按钮)
   - icon(目录/菜单可选)
   - path/url(目录/菜单必填)
   - component(菜单必填,目录/按钮可选)
   - permission(按钮必填,目录/菜单可选)
   - sort(数字)
   - status(单选:0-禁用/1-启用)
4. 选择 type=目录,填写:
   - name:测试目录
   - path:/test
   - sort:100
   - status:1
5. 提交并验证:
   - API POST /api/v1/admin/system/menu/create
   - 显示「新增成功」

预期结果:
✅ 新增弹窗根据 type 动态显示字段
✅ 目录创建成功

---

【测试场景 3:新增菜单项】
1. 点击「新增」,type=菜单
2. 填写:
   - parentId:刚才创建的「测试目录」
   - name:测试菜单
   - path:/test/page
   - component:test/Page
   - icon:SettingOutlined
   - sort:1
   - status:1
3. 提交:
   - POST /api/v1/admin/system/menu/create
4. 验证菜单树出现新菜单项

预期结果:
✅ 菜单项创建成功
✅ 父子关系正确

---

【测试场景 4:新增按钮权限】
1. 点击「新增」,type=按钮
2. 填写:
   - parentId:刚才创建的「测试菜单」
   - name:测试按钮
   - permission:test:button:add
   - sort:1
   - status:1
3. 提交:
   - POST /api/v1/admin/system/menu/create
4. 验证菜单树出现按钮节点

预期结果:
✅ 按钮节点创建成功
✅ 父子关系正确
✅ permission 字段生效

---

【测试场景 5:编辑菜单】
1. 点击某个菜单的「编辑」按钮
2. 验证弹窗弹出,数据正确回填
3. 修改 sort 为 99
4. 提交:
   - PUT /api/v1/admin/system/menu/update
5. 验证排序生效

预期结果:
✅ 编辑弹窗正确回填
✅ 修改后列表更新

---

【测试场景 6:删除菜单】
1. 点击「测试按钮」的「删除」
2. 验证确认弹窗
3. 确认后:
   - DELETE /api/v1/admin/system/menu/delete/{menuId}
4. 验证菜单树移除

预期结果:
✅ 叶子节点删除成功

---

【测试场景 7:删除有子菜单的父菜单】
1. 尝试删除「测试目录」
2. 验证后端保护(若有子菜单则拒绝)
3. 验证错误提示

预期结果:
✅ 后端保护生效
✅ 友好提示

---

【测试场景 8:状态切换】
1. 点击菜单的「启用/禁用」开关
2. 触发 API PUT /api/v1/admin/system/menu/update
3. 验证状态切换

预期结果:
✅ 状态切换生效
✅ 左侧菜单同步隐藏

---

【测试场景 9:菜单展开/折叠】
1. 点击父菜单的展开/折叠按钮
2. 验证子菜单展开/折叠

预期结果:
✅ 树交互流畅

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 菜单树 | GET | /api/v1/admin/system/menu/list |
| 菜单详情 | GET | /api/v1/admin/system/menu/get/{menuId} |
| 创建菜单 | POST | /api/v1/admin/system/menu/create |
| 更新菜单 | PUT | /api/v1/admin/system/menu/update |
| 删除菜单 | DELETE | /api/v1/admin/system/menu/delete/{menuId} |
| 更新状态 | PUT | /api/v1/admin/system/menu/update(传 status) |

---

【问题诊断】
- 菜单树空白 → 检查 menuApi.getMenus() 返回结构
- 编辑时数据不回填 → 检查 MenuFormModal watch 逻辑
- 删除失败 → 检查后端保护(子菜单/角色引用)
- 权限标识不生效 → 检查 permission 字段与路由 v-has-permi 指令
- 状态切换后左侧菜单未更新 → 刷新页面或重新登录
```

#### SYS-04.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【菜单管理】模块(若已存在则增强)。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: 左侧菜单「系统管理」→「菜单管理」
- 路由: /system/menu
- 权限前缀: system:menu:
- 涉及权限码:
  - system:menu:create(新增菜单)
  - system:menu:update(编辑菜单)
  - system:menu:delete(删除菜单)
  - system:menu:query(查询菜单)

【前端文件清单】
- 主页面: ontograph-frontend/src/views/system/menu/index.vue
- API 封装: ontograph-frontend/src/api/menu.ts (menuApi 对象)

【后端文件清单】
- Controller: ontograph-backend/src/main/java/com/ontograph/module/system/controller/MenuController.java
- Service: .../service/MenuService.java + impl
- DO: .../dal/dataobject/MenuDO.java
- Mapper: .../dal/mysql/MenuMapper.java
- 关联表:role_menu(角色-菜单关联)

【API 端点】
| 操作 | 方法 | 路径 |
|------|------|------|
| 菜单树 | GET | /api/v1/admin/system/menu/list |
| 菜单详情 | GET | /api/v1/admin/system/menu/get/{menuId} |
| 创建菜单 | POST | /api/v1/admin/system/menu/create |
| 更新菜单 | PUT | /api/v1/admin/system/menu/update |
| 删除菜单 | DELETE | /api/v1/admin/system/menu/delete/{menuId} |

【功能需求】
1. 菜单 CRUD
2. 树形结构展示(目录/菜单/按钮 3 种类型)
3. 父菜单选择(树形下拉)
4. 删除保护(有子菜单时拒绝)
5. 菜单状态切换(启用/禁用)
6. 路由与权限标识(permission 字段)绑定

【UI 规范】
- UI 库: ant-design-vue
- 列表:AntdTree(可展开/折叠)
- 弹窗组件:AntdModal + AntdForm
- 父菜单选择:AntdTreeSelect
- 图标选择:AntdIconPicker
- 操作按钮:新增子菜单、编辑、删除

【MySQL 集成】
- 表:system_menu(主表)、role_menu(关联表)
- DO 字段:id, parent_id, name, type(1-目录/2-菜单/3-按钮), icon, url, component, permission, sort, status, deleted, create_time, update_time
- 逻辑删除:deleted 字段
- 树形构建:parent_id 自关联

【参考实现】
- 参考 ontograph-frontend/src/views/system/menu/index.vue(已存在)
- 复用 ontograph-frontend/src/api/menu.ts 的 menuApi
- 后端 MenuController 已提供 buildMenuTree(全部扁平 → 树形)
- 前端路由配置参考 src/router/index.ts

【测试验证】
实现完成后,使用 SYS-04.2 测试提示词中的测试场景验证,重点验证:
1. 菜单树形结构正确
2. 三种类型菜单创建(目录/菜单/按钮)
3. 父子关系正确
4. 编辑/删除/状态切换 CRUD 流程
5. 删除保护(子菜单)生效
6. 权限标识(permission)与前端 v-has-permi 指令对应

【交付物清单】
- [ ] 前端主页面 .vue(menu/index.vue)
- [ ] 前端 API 封装 .ts(已存在)
- [ ] 后端 Controller(已存在,含 buildMenuTree)
- [ ] 后端 Service 接口 + 实现(已存在)
- [ ] 后端 DO / Mapper(已存在)
- [ ] role_menu 关联表(已存在)
- [ ] 路由注册(/system/menu)
- [ ] 菜单注册(左侧菜单「系统管理」→「菜单管理」)
- [ ] 权限码注册(system:menu:*)
- [ ] 通过 SYS-04.2 所有测试场景
```

---

### SYS-05 操作日志

**页面路径**: 左侧菜单「系统管理」→「操作日志」
**源码文件**: `ontograph-frontend/src/views/system/log/index.vue`
**后端控制器**: `ontograph-backend/src/main/java/com/ontograph/module/system/controller/OperationLogController.java`
**API 文件**: `ontograph-frontend/src/api/log.ts` (`logApi`)
**权限标识**: `system:log:query`, `system:log:delete`, `system:log:export`

#### SYS-05.1 测试场景

| 场景编号 | 场景名称 | 说明 |
|---------|---------|------|
| SYS-05-01 | 日志列表加载 | 分页/多条件筛选 |
| SYS-05-02 | 用户名搜索 | 精确匹配 |
| SYS-05-03 | 操作名称搜索 | 模糊匹配 |
| SYS-05-04 | 状态筛选 | 成功/失败 |
| SYS-05-05 | 时间范围筛选 | startTime / endTime |
| SYS-05-06 | 日志详情 | 查看完整参数 |
| SYS-05-07 | 删除单条 | 二次确认 |
| SYS-05-08 | 清空日志 | 二次确认 |
| SYS-05-09 | 导出日志 | 文件下载 |
| SYS-05-10 | 错误日志红色高亮 | 视觉提示 |

#### SYS-05.2 测试提示词

```
/browser 或 /open-gstack-browser
打开操作日志页面,执行完整的操作日志查询与导出测试。

【前置操作】
1. 使用 admin/admin123 登录系统
2. 在左侧菜单点击「系统管理」→「操作日志」
3. 等待页面加载完成

---

【测试场景 1:日志列表加载与基础显示】
1. 验证日志列表正常加载
2. 检查列:用户、操作模块(operation)、请求方法(method)、IP、状态、耗时(duration)、错误信息、创建时间、操作
3. 验证分页功能正常
4. 验证顶部「导出」「清空」按钮

预期结果:
✅ 表格 9 列完整
✅ 分页正常
✅ 失败日志红色高亮

---

【测试场景 2:用户名搜索】
1. 在「用户名」输入框输入 admin
2. 点击「搜索」
3. 验证列表过滤出 username=admin 的日志

预期结果:
✅ 用户名搜索过滤正确

---

【测试场景 3:操作名称搜索】
1. 在「操作」输入框输入「登录」
2. 验证列表过滤出 operation 包含「登录」的日志

预期结果:
✅ 操作名称模糊搜索正确

---

【测试场景 4:状态筛选】
1. 选择「状态:成功」
2. 验证列表过滤
3. 选择「状态:失败」
4. 验证列表过滤出错误日志

预期结果:
✅ 状态筛选生效

---

【测试场景 5:时间范围筛选】
1. 选择 startTime / endTime(最近 7 天)
2. 验证列表过滤

预期结果:
✅ 时间范围筛选生效

---

【测试场景 6:日志详情】
1. 点击某条日志的「详情」按钮
2. 验证弹出详情弹窗
3. 验证显示:完整参数(params,可能为 JSON)、IP、location、errorMsg 等

预期结果:
✅ 详情弹窗字段完整
✅ params JSON 格式化展示

---

【测试场景 7:删除单条日志】
1. 点击某条日志的「删除」按钮
2. 验证确认弹窗
3. 确认后:
   - DELETE /api/v1/admin/system/log/{id}
4. 验证列表移除

预期结果:
✅ 删除成功

---

【测试场景 8:清空日志】
1. 点击「清空」按钮
2. 验证二次确认
3. 确认后:
   - DELETE /api/v1/admin/system/log/clear
4. 验证列表清空

预期结果:
✅ 清空成功

---

【测试场景 9:导出日志】
1. 填写筛选条件
2. 点击「导出」按钮
3. 验证下载文件
4. 验证文件包含数据

预期结果:
✅ 导出文件下载
✅ 文件包含数据

---

【测试场景 10:错误日志红色高亮】
1. 触发一次失败操作(如参数错误)
2. 验证日志列表中该条红色高亮(status=0)

预期结果:
✅ 视觉提示生效

---

【API 端点对照】

| 操作 | API 方法 | 路径 |
|------|---------|------|
| 日志列表 | GET | /api/v1/admin/system/log/list |
| 日志详情 | GET | /api/v1/admin/system/log/{id} |
| 删除单条 | DELETE | /api/v1/admin/system/log/{id} |
| 清空日志 | DELETE | /api/v1/admin/system/log/clear |
| 导出日志 | GET | /api/v1/admin/system/log/export |

---

【问题诊断】
- 列表不加载 → 检查 logApi.getLogs() 是否成功
- 导出文件失败 → 检查 responseType: 'blob'
- 清空失败 → 检查后端实现
- 错误日志不高亮 → 检查 status 字段与样式
```

#### SYS-05.3 开发提示词

```
请基于以下信息,在 ontograph-java 仓库中实现【操作日志】模块(若已存在则增强)。

【模块信息】
- 服务: ontograph-java(:9090)
- 页面路径: 左侧菜单「系统管理」→「操作日志」
- 路由: /system/log
- 权限前缀: system:log:
- 涉及权限码:
  - system:log:query(查询日志)
  - system:log:delete(删除日志)
  - system:log:export(导出日志)
  - system:log:clear(清空日志)

【前端文件清单】
- 主页面: ontograph-frontend/src/views/system/log/index.vue
- API 封装: ontograph-frontend/src/api/log.ts (logApi 对象)

【后端文件清单】
- Controller: ontograph-backend/src/main/java/com/ontograph/module/system/controller/OperationLogController.java
- Service: .../service/OperationLogService.java + impl
- DO: .../dal/dataobject/OperationLogDO.java
- Mapper: .../dal/mysql/OperationLogMapper.java
- AOP 切面:OperationLogAspect(自动记录操作日志)

【API 端点】
| 操作 | 方法 | 路径 |
|------|------|------|
| 日志列表 | GET | /api/v1/admin/system/log/list |
| 日志详情 | GET | /api/v1/admin/system/log/{id} |
| 删除单条 | DELETE | /api/v1/admin/system/log/{id} |
| 清空日志 | DELETE | /api/v1/admin/system/log/clear |
| 导出日志 | GET | /api/v1/admin/system/log/export |

【功能需求】
1. 操作日志分页查询(支持多条件筛选)
2. 详情查看(完整 params 展示)
3. 单条删除 / 批量清空
4. 日志导出(Excel)
5. 错误日志视觉高亮
6. 异步记录(通过 AOP 切面自动写入)

【UI 规范】
- UI 库: ant-design-vue
- 列表字段:username、operation、method、ip、status、duration、errorMsg、createTime、操作
- 状态:Tag 组件(成功=绿色,失败=红色)
- 时间筛选:AntdDatePicker.RangePicker
- 详情弹窗:AntdDrawer 或 AntdModal

【AOP 切面】
- 使用 Spring AOP 拦截 Controller 方法
- 注解:@OperationLog("操作名称")
- 自动写入:username、operation、method、params、status、duration、errorMsg

【MySQL 集成】
- 表:operation_log
- DO 字段:id, user_id, username, operation, method, params, ip, location, status, error_msg, duration, create_time
- 索引:username、operation、status、create_time

【参考实现】
- 参考 ontograph-frontend/src/views/system/log/index.vue(已存在)
- 复用 ontograph-frontend/src/api/log.ts 的 logApi
- 后端 OperationLogController 已存在
- AOP 切面参考 ontograph-backend 的 aspect 包

【测试验证】
实现完成后,使用 SYS-05.2 测试提示词中的测试场景验证,重点验证:
1. 列表 9 列完整渲染
2. 多条件搜索过滤(5 个筛选条件)
3. 详情弹窗字段完整
4. 删除/清空/导出 CRUD 流程
5. 错误日志红色高亮

【交付物清单】
- [ ] 前端主页面 .vue(log/index.vue)
- [ ] 前端 API 封装 .ts(已存在)
- [ ] 后端 Controller(已存在)
- [ ] 后端 Service 接口 + 实现(已存在)
- [ ] 后端 DO / Mapper(已存在)
- [ ] AOP 切面(自动记录)
- [ ] 路由注册(/system/log)
- [ ] 菜单注册(左侧菜单「系统管理」→「操作日志」)
- [ ] 权限码注册(system:log:*)
- [ ] 通过 SYS-05.2 所有测试场景
```

---

## 6.测试结果报告模板

```markdown
# Graphiti + System 服务测试报告

**测试日期**: 2026-06-17
**测试人员**: AI Agent (gstack /qa)
**测试环境**: http://localhost:5173
**Graphiti 服务**: http://localhost:9090
**System 服务**: http://localhost:9090

## 测试摘要

| 指标 | 值 |
|------|------|
| 总用例数 | 197 |
| 通过 | X |
| 失败 | Y |
| 跳过 | Z |
| 通过率 | X/197% |
| 执行时长 | M 分钟 |

## 结果汇总

### Graphiti 服务

| 模块 | 用例数 | 通过 | 失败 | 跳过 | 通过率 |
|------|--------|------|------|------|--------|
| G-01 图谱管理 | 12 | 12 | 0 | 0 | 100% |
| G-02 Graph IDE 可视化 | 10 | 10 | 0 | 0 | 100% |
| G-03 Graph IDE Schema | 8 | 8 | 0 | 0 | 100% |
| G-04 Graph IDE 节点/边 | 8 | 8 | 0 | 0 | 100% |
| G-05 级联编辑 | 6 | 6 | 0 | 0 | 100% |
| G-06 本体管理 | 10 | 10 | 0 | 0 | 100% |
| G-07 版本回滚 | 6 | 6 | 0 | 0 | 100% |
| G-08 推理机 | 5 | 5 | 0 | 0 | 100% |
| G-09 域规则 | 8 | 8 | 0 | 0 | 100% |
| G-10 元数据管理 | 12 | 12 | 0 | 0 | 100% |
| G-11 数据导入 | 9 | 9 | 0 | 0 | 100% |
| G-12 数据抽取 | 8 | 8 | 0 | 0 | 100% |
| G-13 数据导出 | 4 | 4 | 0 | 0 | 100% |
| G-14 搜索 | 9 | 9 | 0 | 0 | 100% |
| G-15 搜索管线 | 6 | 6 | 0 | 0 | 100% |
| G-16 Prompt 模板 | 10 | 10 | 0 | 0 | 100% |
| G-17 Prompt 测试 | 5 | 5 | 0 | 0 | 100% |
| G-18 自定义指令 | 4 | 4 | 0 | 0 | 100% |
| G-19 时序数据 | 5 | 5 | 0 | 0 | 100% |
| G-20 法律知识图谱 | 12 | 12 | 0 | 0 | 100% |
| **小计** | **155** | **X** | **Y** | **Z** | **X/155%** |

### System 服务

| 模块 | 用例数 | 通过 | 失败 | 跳过 | 通过率 |
|------|--------|------|------|------|--------|
| SYS-00 登录注销与首页 | 8 | 8 | 0 | 0 | 100% |
| SYS-01 参数配置 | 6 | 6 | 0 | 0 | 100% |
| SYS-02 用户管理 | 9 | 9 | 0 | 0 | 100% |
| SYS-03 角色管理 | 8 | 8 | 0 | 0 | 100% |
| SYS-04 菜单管理 | 9 | 9 | 0 | 0 | 100% |
| SYS-05 操作日志 | 10 | 10 | 0 | 0 | 100% |
| **小计** | **50** | **X** | **Y** | **Z** | **X/50%** |

| **总计** | **205** | **X** | **Y** | **Z** | **X/205%** |

## 失败用例详情

### G-02 Graph IDE 可视化 — 测试场景 6:邻居展开

- **现象**: 点击「展开邻居」后画布未更新
- **控制台错误**: `TypeError: Cannot read property 'edges' of undefined`
- **截图**: [.gstack/qa-reports/screenshots/issue-g02-006.png](.gstack/qa-reports/screenshots/issue-g02-006.png)
- **定位**: `ide.vue` 的 expandNeighbors 方法未正确处理返回数据
- **修复建议**: 检查 expandNeighbors API 响应处理
- **修复文件**: `src/views/graph/ide.vue`
- **状态**: 已修复

## Neo4j 集成检查

| 检查项 | 状态 | 备注 |
|--------|------|------|
| Neo4j 启动 | ✅ | bolt://localhost:7687 |
| 节点创建 | ✅ | 实体/剧集/社区 |
| 关系创建 | ✅ | RELATES_TO/MENTIONS |
| 索引存在 | ✅ | uuid / graphId |
| 事务回滚 | ✅ | 异常时数据回滚 |

## LLM 集成检查

| 检查项 | 状态 | 备注 |
|--------|------|------|
| LM Studio 启动 | ✅ | http://localhost:1234 |
| Chat 模型 | ✅ | google/gemma-4-e4b |
| Embedding 模型 | ✅ | text-embedding-qwen3-embedding-0.6b |
| Rerank 模型 | ✅ | text-embedding-bge-reranker-v2-m3 |
| 平均响应时间 | ✅ | < 5s |

## 建议

1. **高优先级**:修复 Graph IDE 画布性能问题,1000+ 节点渲染卡顿
2. **中优先级**:优化 LLM 抽取的稳定性,减少偶发失败
3. **低优先级**:增加更多可视化布局选项
4. **测试补充**:增加并发导入、批量操作的压力测试
```

---

## 7.模块开发对照与补全清单

本章节对照 Graphiti 服务的 20 个测试模块 + System 服务的 5 个测试模块,梳理实现状态与补全建议。

### Graphiti 模块补全对照表

| 模块 | 前端路径 | 后端控制器 | 前端状态 | 后端状态 | 补全建议 |
|------|---------|-----------|---------|---------|---------|
| G-01 图谱管理 | views/graph/list.vue | GraphitiController | ✅ 完整 | ✅ | 完善删除预览/克隆 |
| G-02 IDE 可视化 | views/graph/ide.vue | GraphIDEController | ✅ 完整 | ✅ | 优化画布性能 |
| G-03 IDE Schema | views/graph/ide.vue(Schema Tab) | GraphIDEController | ✅ 完整 | ✅ | 增强属性编辑器 |
| G-04 IDE 节点/边 | views/graph/ide.vue | GraphIDEController | ✅ 完整 | ✅ | 增加批量操作 |
| G-05 级联编辑 | views/graph/ide.vue(Cascade) | GraphIDEController | ✅ 完整 | ✅ | 增强预览分布图 |
| G-06 本体 CRUD | views/data/classes.vue | OntologyController | ✅ 完整 | ✅ | 完善版本对比 |
| G-07 版本回滚 | views/data/classes.vue(Version) | OntologyController | ✅ 完整 | ✅ | 增加 diff 可视化 |
| G-08 推理机 | views/data/classes.vue(Reasoner) | OntologyController | ✅ 完整 | ✅ | 增加推理结果展示 |
| G-09 域规则 | views/data/classes.vue(Rules) | OntologyController | ✅ 完整 | ✅ | 增强 SpEL 编辑器 |
| G-10 元数据 | views/data/community-episode.vue | OntMetadataController | ✅ 完整 | ✅ | 增加导入/导出 |
| G-11 数据导入 | views/data/import.vue | DataImportController | ✅ 完整 | ✅ | 优化大批量导入 |
| G-12 数据抽取 | views/data/import.vue(Extract) | DataExtractController | ✅ 完整 | ✅ | 增强可视化 |
| G-13 数据导出 | views/data/export.vue | GraphitiController | ✅ 完整 | ✅ | 增加更多格式 |
| G-14 搜索 | views/search/index.vue | SearchController | ✅ 完整 | ✅ | 增强筛选 UI |
| G-15 搜索管线 | views/search/index.vue | SearchPipelineController | ✅ 完整 | ✅ | 增加管线配置 UI |
| G-16 Prompt 模板 | views/prompt/index.vue | PromptController | ✅ 完整 | ✅ | 增加可视化编辑器 |
| G-17 Prompt 测试 | views/prompt/index.vue(Test) | PromptTestController | ✅ 完整 | ✅ | 增强结果对比 |
| G-18 自定义指令 | views/custom-instructions/index.vue | CustomInstructionController | ✅ 完整 | ✅ | 增加按 graphId 过滤 |
| G-19 时序数据 | views/graph/temporal.vue | TemporalController | ✅ 完整 | ✅ | 增强时间线 UI |
| G-20 法律 KG | views/legal-kg/index.vue | LegalExtractController + LegalImportController | ✅ 完整 | ✅ | 完善文书解析 |

### System 模块补全对照表

| 模块 | 前端路径 | 后端控制器 | 前端状态 | 后端状态 | 补全建议 |
|------|---------|-----------|---------|---------|---------|
| SYS-00 登录注销与首页 | components/Layout/*.vue, views/login, views/dashboard | AuthController | ✅ 完整 | ✅ | 增强:记住密码、多租户切换 |
| SYS-01 参数配置 | views/system/config/index.vue | SystemConfigController | ✅ 完整 | ✅ | 增加按 groupName 分组管理 |
| SYS-02 用户管理 | views/system/user/index.vue | UserController | ✅ 完整 | ✅ | 实现后端密码重置 API |
| SYS-03 角色管理 | views/system/role/index.vue | RoleController | ✅ 完整 | ✅ | 增强菜单权限分配 UI |
| SYS-04 菜单管理 | views/system/menu/index.vue | MenuController | ✅ 完整 | ✅ | 增强图标选择器 |
| SYS-05 操作日志 | views/system/log/index.vue | OperationLogController | ✅ 完整 | ✅ | 增强详情弹窗(参数 JSON 美化) |

---

## 8.文档版本

| 版本 | 日期 | 修改内容 | 作者 |
|------|------|---------|------|
| 1.0.0 | 2026-06-17 | 从当前工程代码自动生成,覆盖 Graphiti 服务 20 个核心模块 | AI Agent (gsd-map-codebase) |
| 1.2.0 | 2026-06-17 | 追加 SYS-00 登录注销与首页模块,含 8 个测试场景+完整开发提示词;更新汇总至 26 个模块、205 个场景、~13h | AI Agent |
| 1.1.0 | 2026-06-17 | 追加 System 服务 5 个核心模块(SYS-01~SYS-05),共 25 个模块、197 个测试场景 | AI Agent (gsd-map-codebase) |

| 项目 | 值 |
|------|------|
| 适用服务 | Graphiti(知识图谱,:9090)+ System(系统管理,:9090) |
| 前端入口 | http://localhost:5173 |
| 默认账号 | admin / admin123 |
| 默认租户 ID | 1 |
| 测试 Skill | `/qa`, `/qa-only`, `/open-gstack-browser`, MCP Playwright |
