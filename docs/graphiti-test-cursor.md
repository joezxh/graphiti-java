# OntoGraph 浏览器自动化测试 Skill

> **适用版本**: OntoGraph 1.0.0
> **测试环境**: 前端 `http://localhost:5173`, 后端 `http://localhost:9090`
> **测试工具**: gstack `browse` (Puppeteer/Chromium)
> **调用方式**: `$B goto <url>` / `$B snapshot` / `$B click @eN` / `$B fill <sel> <val>` / `$B screenshot <path>` 等
> **会话**: 登录态在 `$B` 会话内持久（cookies 跨命令保持），不同模块共享同一会话
> **API 文档**: `http://localhost:9090/swagger-ui.html`

---

## 目录

- [通用测试框架](#通用测试框架)
- [0. 登录](#0-登录)
- [1. 仪表盘](#1-仪表盘)
- [2. 图谱管理](#2-图谱管理)
  - [2.1 图谱列表](#21-图谱列表)
  - [2.2 图谱 IDE](#22-图谱-ide)
  - [2.3 图谱创建](#23-图谱创建)
  - [2.4 时序历史](#24-时序历史)
- [3. 数据管理](#3-数据管理)
  - [3.1 类定义管理](#31-类定义管理)
  - [3.2 属性管理](#32-属性管理)
  - [3.3 约束管理](#33-约束管理)
  - [3.4 实体管理](#34-实体管理)
  - [3.5 边管理](#35-边管理)
  - [3.6 社区管理](#36-社区管理)
  - [3.7 Episode 管理](#37-episode-管理)
  - [3.8 社区-Episode 关联](#38-社区-episode-关联)
  - [3.9 数据导入](#39-数据导入)
  - [3.10 数据导出](#310-数据导出)
  - [3.11 法律知识图谱](#311-法律知识图谱)
- [4. 工具](#4-工具)
  - [4.1 混合检索](#41-混合检索)
  - [4.2 自定义指令](#42-自定义指令)
  - [4.3 提示词管理](#43-提示词管理)
- [5. 个人中心](#5-个人中心)
- [6. 通知中心](#6-通知中心)
- [7. 系统管理](#7-系统管理)
  - [7.1 用户管理](#71-用户管理)
  - [7.2 角色管理](#72-角色管理)
  - [7.3 菜单管理](#73-菜单管理)
  - [7.4 系统配置](#74-系统配置)
  - [7.5 操作日志](#75-操作日志)
  - [7.6 系统监控](#76-系统监控)
- [8. 跨模块端到端流程](#8-跨模块端到端流程)
- [9. 功能缺失清单](#9-功能缺失清单待开发)
- [10. 测试执行优先级与回归策略](#10-测试执行优先级与回归策略)

---

## 通用测试框架

### 环境前置检查

```bash
# 验证后端 API 可用
$B goto http://localhost:9090/actuator/health
$B text
# 期望看到 {"status":"UP"}

# 验证前端可访问
$B goto http://localhost:5173
$B is visible "body"
```

### 登录流程（所有模块共用）

```bash
# 1. 访问登录页
$B goto http://localhost:5173/login
$B snapshot -i

# 2. 定位用户名/密码输入框并填写
$B fill "[placeholder*='用户名' i], [placeholder*='username' i], input[type='text']" "admin"
$B fill "[placeholder*='密码' i], [placeholder*='password' i], input[type='password']" "admin123"

# 3. 点击登录按钮
$B click "button[type='submit'], button:has-text('登录')"

# 4. 等待跳转到仪表盘并验证
$B wait --networkidle
$B url
$B is visible "[class*='dashboard'], [class*='Dashboard'], [class*='layout']"
$B screenshot /tmp/login-success.png
```

### 退出登录流程

```bash
$B click "[class*='avatar'], [class*='user-info'], [class*='dropdown']"
$B wait --networkidle
$B snapshot -i
$B click "button:has-text('退出'), button:has-text('Logout'), [class*='logout']"
$B wait --networkidle
$B url  # 期望: /login
```

### 问题报告格式

```
## [模块名] 测试报告

**时间**: YYYY-MM-DD HH:mm
**URL**: http://localhost:5173/[路由]
**结果**: ✅ 通过 | ❌ 失败 | ⚠️ 部分通过

### 截图证据
- /tmp/[模块]-success.png (成功状态)
- /tmp/[模块]-issue.png (失败状态，如有)

### 通过项
- [功能描述] — 预期结果与实际一致

### 失败项
- [功能描述] — 问题现象 — 定位文件 → 修复建议

### 回归测试
- [相关模块列表]
```

### 通用定位参考

| 问题类型 | 后端定位 | 前端定位 |
|----------|----------|----------|
| API 5xx 错误 | Controller 层异常未捕获 | — |
| 数据不展示 | Service 层查询逻辑 | API 调用参数错误 |
| UI 空白 | — | Vue 组件 `v-if` 条件不满足 |
| 图表不渲染 | — | ECharts 实例未正确初始化 |
| 表单验证失效 | — | 前端验证规则未绑定 |
| 按钮无响应 | — | `@click` 事件未绑定或 loading 状态卡死 |

---

## 0. 登录

### 测试提示词

```
## 登录 (Login) 测试

### 步骤 1：成功登录
$B goto http://localhost:5173/login
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/login-page.png
# 验证页面包含：用户名输入框、密码输入框、登录按钮
$B is visible "input[type='text'], input[placeholder*='用户名']"
$B is visible "input[type='password'], input[placeholder*='密码']"
$B is visible "button[type='submit']"

### 步骤 2：空表单提交
$B click "button[type='submit']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/login-empty-submit.png
# 期望：前端阻止提交，显示"请输入用户名"等必填提示
$B is visible "[class*='error'], [class*='message'], [class*='tip']"

### 步骤 3：密码错误登录
$B fill "input[type='text'], input[placeholder*='用户名']" "admin"
$B fill "input[type='password'], input[placeholder*='密码']" "wrongpassword"
$B screenshot /tmp/login-wrong-pwd.png
$B click "button[type='submit']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/login-wrong-result.png
# 期望：返回 401 或显示错误提示"用户名或密码错误"
$B text

### 步骤 4：用户名不存在
$B fill "input[type='text']" "nonexistent_user_$(date +%s)"
$B fill "input[type='password']" "anypassword"
$B click "button[type='submit']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/login-nonexistent-result.png
# 期望：返回 401 或显示错误提示

### 步骤 5：XSS/注入测试
$B goto http://localhost:5173/login
$B wait --networkidle
$B fill "input[type='text']" "<script>alert(1)</script>"
$B fill "input[type='password']" "password"
$B click "button[type='submit']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/login-xss-test.png
# 期望：后端正确拒绝或转义，不执行脚本

### 步骤 6：记住登录态（JWT Token）
$B goto http://localhost:5173/login
$B wait --networkidle
$B fill "input[type='text']" "admin"
$B fill "input[type='password']" "admin123"
$B snapshot -i
# 检查是否有"记住我"或"记住登录状态"复选框
$B click "button[type='submit']"
$B wait --networkidle
$B url  # 期望跳转到 /dashboard
$B cookies  # 验证 JWT Token 被正确存储到 cookie
$B screenshot /tmp/login-jwt-cookie.png

### 步骤 7：Token 失效回跳
$B goto http://localhost:5173/dashboard
$B wait --networkidle
$B cookies  # 清除 JWT Token
$B storage set token "invalid_token"
$B reload
$B wait --networkidle
$B url  # 期望自动跳转到 /login
$B screenshot /tmp/login-token-invalid.png

### 步骤 8：并发登录测试
$B goto http://localhost:5173/login
$B wait --networkidle
$B fill "input[type='text']" "admin"
$B fill "input[type='password']" "admin123"
$B click "button[type='submit']"
$B wait --networkidle
# 在另一个浏览器会话中用同一账号登录（browse 使用同一会话故跳过此步骤）
# 验证：第二个登录后，第一个会话的 Token 是否被强制失效（单设备登录场景）

### 预期结果
- 登录页加载正常，包含完整的用户名/密码表单
- 空表单提交被前端验证阻止
- 错误密码显示友好的错误提示（不泄露具体是用户名错还是密码错）
- 登录成功后跳转 /dashboard，JWT Token 存储在 cookie
- Token 失效时自动回跳到登录页
- XSS 输入被后端过滤或转义

### 问题发现
$B console --errors
$B network
$B screenshot /tmp/login-issue.png
# backend: AuthController, JwtAuthenticationFilter
# frontend: views/login/index.vue
```

---

## 1. 仪表盘

### 测试提示词

```
## 仪表盘 (Dashboard) 测试

### 步骤 1：登录
$B goto http://localhost:5173/login
$B snapshot -i
$B fill "[placeholder*='用户名' i], input[type='text']" "admin"
$B fill "[placeholder*='密码' i], input[type='password']" "admin123"
$B click "button[type='submit']"
$B wait --networkidle
$B url  # 期望: /dashboard
$B screenshot /tmp/dashboard-login.png

### 步骤 2：页面结构验证
$B goto http://localhost:5173/dashboard
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/dashboard-loaded.png
# 验证页面包含：统计卡片区域、图表区域、侧边栏、导航菜单
$B is visible "[class*='card'], [class*='stat'], [class*='chart'], .ant-card"
$B is visible "canvas, svg, [class*='echarts']"
$B is visible "[class*='sidebar'], [class*='layout']"

### 步骤 3：统计数据合理性检查
$B text
# 检查输出中是否包含数字（非 NaN、非空、非负）
# 期望看到：实体数量、关系数量、图谱数量、社区数量 等统计值
$B screenshot /tmp/dashboard-stats.png

### 步骤 4：图谱统计卡片交互
$B snapshot -i
$B click "[class*='card']:first-child, [class*='statistic']"
$B wait --networkidle
$B url  # 期望跳转到对应详情页（图谱 IDE 或图谱列表）
$B screenshot /tmp/dashboard-card-click.png
$B back
$B wait --networkidle

### 步骤 5：最新图谱列表验证
$B goto http://localhost:5173/dashboard
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/dashboard-recent-graphs.png
$B text
# 期望看到最近操作的图谱列表，包含图谱名称和操作时间
$B is visible "[class*='graph'], [class*='list-item']"

### 步骤 6：最近活动记录
$B snapshot -i
$B is visible "[class*='activity'], [class*='recent'], [class*='timeline']"
$B screenshot /tmp/dashboard-activity.png
$B text  # 期望看到最近的数据导入、实体创建等操作记录

### 步骤 7：系统健康状态指示
$B text
$B screenshot /tmp/dashboard-health.png
# 期望看到各组件健康状态（Neo4j / PostgreSQL / Redis）
# 健康：绿色标识；异常：红色标识

### 步骤 8：快速操作入口
$B snapshot -i
$B is visible "[class*='quick'], [class*='action']"
$B screenshot /tmp/dashboard-quick-actions.png
# 期望看到：新建图谱、导入数据、快速搜索 等快捷入口

### 步骤 9：刷新一致性
$B reload
$B wait --networkidle
$B text  # 对比刷新前后的统计数据是否一致
$B screenshot /tmp/dashboard-reload.png

### 步骤 10：响应式验证
$B viewport 375x812
$B wait --networkidle
$B snapshot
$B screenshot /tmp/dashboard-mobile.png
$B viewport 1280x720
$B wait --networkidle

### 预期结果
- 登录后 URL 跳转到 /dashboard
- 统计卡片不少于 4 个，数据为有效数字（非 NaN、非负）
- ECharts 图表正确渲染（无空白、无报错）
- 点击统计卡片可跳转详情页
- 最新图谱列表展示正确
- 系统健康状态指示准确
- 移动端布局正常，不发生错乱
- 刷新后数据保持一致

### 问题发现
$B console --errors
$B network  # 查看失败的 API 请求
$B screenshot /tmp/dashboard-issue.png
# backend: GraphitiController (GET /api/v1/graph/stats)
# frontend: views/dashboard/index.vue, src/api/graph.ts
```

---

## 2. 图谱管理

### 2.1 图谱列表

### 测试提示词

```
## 图谱列表 (Graph List) 测试

### 步骤 1：进入图谱列表
$B goto http://localhost:5173/graph/list
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/graphlist-loaded.png

### 步骤 2：列表展示验证
$B text
# 期望看到图谱卡片，包含名称、描述、实体数量、关系数量、创建时间
$B is visible "[class*='card'], [class*='graph-card']"
$B screenshot /tmp/graphlist-cards.png

### 步骤 3：分页验证（如图谱数量较多）
$B snapshot -i
$B is visible "[class*='pagination'], .ant-pagination"
$B screenshot /tmp/graphlist-pagination.png
# 切换到第 2 页，验证数据正确刷新
$B click "[class*='pagination'] [class*='next'], .ant-pagination-next"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/graphlist-page2.png

### 步骤 4：卡片操作按钮验证
$B snapshot -i
$B screenshot /tmp/graphlist-card-actions.png
# 每个卡片应包含：进入 IDE、克隆、删除 等操作按钮
$B is visible "button:has-text('进入'), button:has-text('克隆'), button:has-text('删除')"

### 步骤 5：进入图谱 IDE
$B click "[class*='card']:first-child button:has-text('进入'), [class*='card']:first-child [class*='icon']:first-child"
$B wait --networkidle
$B url  # 期望: /graph/ide/:id 或 /graph/ide?id=:id
$B screenshot /tmp/graphlist-enter-ide.png
$B back
$B wait --networkidle

### 步骤 6：克隆图谱
$B snapshot -i
$B click "[class*='card']:first-child button:has-text('克隆')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/graphlist-clone-modal.png
$B fill "[placeholder*='名称' i], [placeholder*='name' i], input[type='text']" "Cloned-Graph-$(date +%s)"
$B click "button:has-text('确认'), button:has-text('克隆'), button[type='submit']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/graphlist-clone-result.png
$B text  # 验证新克隆图谱出现在列表顶部或列表中

### 步骤 7：删除图谱预览
$B snapshot -i
$B click "[class*='card']:last-child button:has-text('删除')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/graphlist-delete-preview.png
# 期望看到删除预览：将被删除的节点数量、关系数量
$B text
# 点击取消
$B click "button:has-text('取消'), button:has-text('取消删除')"
$B wait --networkidle

### 步骤 8：确认删除图谱
$B snapshot -i
$B click "[class*='card']:last-child button:has-text('删除')"
$B wait --networkidle
$B click "button:has-text('确认删除'), button:has-text('确定删除'), .ant-modal button:has-text('确定')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/graphlist-delete-confirmed.png
$B text  # 验证图谱从列表消失

### 步骤 9：搜索过滤
$B goto http://localhost:5173/graph/list
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='搜索' i], [placeholder*='search' i], input[type='search']" "test"
$B wait 1s
$B text
$B screenshot /tmp/graphlist-search-result.png
# 验证列表实时过滤为匹配结果
$B fill "[placeholder*='搜索' i]" ""
$B wait 1s
$B text  # 验证清空搜索后恢复完整列表

### 预期结果
- 图谱列表正确展示所有图谱卡片
- 每个卡片显示：名称、描述、实体数、关系数、创建时间
- 分页控件功能正常
- 卡片操作按钮（进入 IDE、克隆、删除）均可用
- 克隆图谱后新图谱出现在列表
- 删除图谱需二次确认，确认后图谱消失且关联数据被级联删除
- 搜索过滤实时响应

### 问题发现
$B console --errors
$B network
$B screenshot /tmp/graphlist-issue.png
# backend: GraphitiController (GET /api/v1/graph/list, POST /api/v1/graph/:id/clone, DELETE /api/v1/graph/:id)
# frontend: views/graph/list.vue
```

---

### 2.2 图谱 IDE

### 测试提示词

```
## 图谱 IDE (Graph IDE) 测试

### 步骤 1：进入图谱 IDE
$B goto http://localhost:5173/graph/ide
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/graphide-entry.png

### 步骤 2：图谱选择器
$B snapshot -i
$B click "[class*='select'], [class*='dropdown'], [class*='graph-selector'], select"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/graphide-selector.png
$B click "[class*='option']:first-child, [role='option']:first-child"
$B wait --networkidle
$B url
$B screenshot /tmp/graphide-graph-selected.png

### 步骤 3：图谱画布加载
$B wait --networkidle
$B snapshot -i
$B is visible "canvas, svg, [class*='echarts']"
$B screenshot /tmp/graphide-canvas.png
# 期望看到节点（圆点）和边（连线），节点以不同颜色/形状区分类型

### 步骤 4：工具栏按钮验证
$B snapshot -i
$B screenshot /tmp/graphide-toolbar.png
# 期望看到：添加节点、添加边、缩放控制、布局切换、搜索过滤、全屏 等按钮
$B is visible "[class*='toolbar'], [class*='tool']"

### 步骤 5：节点点击交互
$B wait --networkidle
$B snapshot -i
# 点击画布空白处
$B click "canvas:first-child, svg:first-child" --offset 100,100
$B wait 1s
$B snapshot -i
# 点击一个节点区域（尝试多个坐标）
$B click "canvas" --offset 300,200
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/graphide-node-click.png
# 期望看到节点详情面板弹出（包含节点名称、类型、属性）
$B is visible "[class*='detail'], [class*='panel'], [class*='modal'], [class*='drawer']"

### 步骤 6：节点详情面板操作
$B snapshot -i
$B screenshot /tmp/graphide-node-detail.png
# 点击"编辑"按钮
$B click "button:has-text('编辑'), button:has-text('Edit')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/graphide-node-edit-form.png
$B fill "[placeholder*='名称' i], [placeholder*='name' i]" "Modified-Node-Name"
$B click "button:has-text('保存'), button[type='submit']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/graphide-node-edit-result.png

### 步骤 7：关闭详情面板
$B click "button:has-text('关闭'), [class*='close'], [aria-label*='close']"
$B wait --networkidle
$B snapshot -i

### 步骤 8：添加节点
$B snapshot -i
$B click "button:has-text('添加节点'), button:has-text('新建节点'), [title*='添加节点']"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/graphide-add-node-form.png
$B fill "[placeholder*='名称' i], [placeholder*='name' i]" "E2E-Test-Node-$(date +%s)"
$B screenshot /tmp/graphide-add-node-filled.png
$B click "button[type='submit'], button:has-text('确认'), button:has-text('添加')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/graphide-add-node-result.png
# 验证新节点出现在画布中

### 步骤 9：添加边（关系）
$B snapshot -i
$B click "button:has-text('添加边'), button:has-text('新建关系'), [title*='添加边']"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/graphide-add-edge-form.png
# 选择源节点和目标节点
$B click "[class*='select']:first-child, [class*='source']"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B click "[class*='select']:last-child, [class*='target']"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B fill "[placeholder*='描述' i], [placeholder*='fact' i]" "E2E 自动测试创建的关系"
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/graphide-add-edge-result.png
# 验证新边出现在画布中

### 步骤 10：画布缩放与拖拽
$B wait --networkidle
$B screenshot /tmp/graphide-before-zoom.png
$B click "[title*='放大'], [aria-label*='zoom in']"
$B wait 1s
$B screenshot /tmp/graphide-after-zoom.png
$B click "[title*='缩小'], [aria-label*='zoom out']"
$B wait 1s
# 拖拽画布
$B click "canvas"
$B wait --networkidle
$B screenshot /tmp/graphide-drag.png

### 步骤 11：布局切换
$B snapshot -i
$B click "button:has-text('布局'), [title*='布局'], [class*='layout-btn']"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/graphide-layout-select.png
$B click "[class*='option']:has-text('环形'), [class*='item']:has-text('force')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/graphide-layout-changed.png

### 步骤 12：搜索过滤高亮
$B snapshot -i
$B fill "[placeholder*='搜索' i], [placeholder*='search' i]" "Node"
$B wait 2s
$B snapshot
$B screenshot /tmp/graphide-search-highlight.png
# 期望匹配节点被高亮，其他节点淡化
$B fill "[placeholder*='搜索' i]" ""
$B wait 1s

### 步骤 13：删除节点
$B snapshot -i
$B click "canvas" --offset 300,200
$B wait --networkidle
$B snapshot -i
$B click "button:has-text('删除'), button:has-text('Delete')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认'), button:has-text('确定')"
$B wait --networkidle
$B screenshot /tmp/graphide-node-deleted.png
# 验证节点从画布消失

### 预期结果
- 图谱 IDE 加载后正确渲染节点和边
- 节点点击弹出详情面板，包含属性和操作按钮
- 添加节点/边成功，数据实时反映到画布
- 缩放、拖拽、布局切换流畅
- 搜索过滤高亮匹配节点
- 删除节点需确认，删除后边一并清理

### 问题发现
$B console --errors
$B network
$B screenshot /tmp/graphide-issue.png
# backend: GraphIDEController (GET /api/v1/graph/:id/visualization, POST /api/v1/graph/:id/nodes, POST /api/v1/graph/:id/edges)
# frontend: views/graph/ide.vue, src/components/GraphCanvas.vue
```

---

### 2.3 图谱创建

### 测试提示词

```
## 图谱创建 (Graph Create) 测试

### 步骤 1：进入图谱创建页
$B goto http://localhost:5173/graph/create
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/graphcreate-loaded.png
# 验证表单包含：图谱名称（必填）、描述（可选）

### 步骤 2：空表单提交
$B click "button[type='submit'], button:has-text('创建')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/graphcreate-empty-submit.png
# 期望：显示必填字段验证错误
$B is visible "[class*='error'], [class*='message'], .ant-form-item-explain-error"

### 步骤 3：成功创建图谱
$B snapshot -i
$B fill "[placeholder*='名称' i], [placeholder*='name' i]" "E2E-Created-Graph-$(date +%s)"
$B fill "[placeholder*='描述' i], [placeholder*='desc' i], textarea" "自动化测试创建的图谱"
$B screenshot /tmp/graphcreate-form-filled.png
$B click "button[type='submit'], button:has-text('创建')"
$B wait --networkidle
$B url  # 期望: /graph/ide/:id 或 /graph/list
$B screenshot /tmp/graphcreate-success.png
$B text

### 步骤 4：名称重复校验
$B goto http://localhost:5173/graph/create
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='名称' i]" "admin"  # 使用已存在的图谱名称
$B fill "textarea" "重复名称测试"
$B click "button:has-text('创建')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/graphcreate-duplicate-name.png
# 期望：显示名称重复错误提示

### 步骤 5：创建后跳转到 IDE
$B goto http://localhost:5173/graph/create
$B wait --networkidle
$B fill "[placeholder*='名称' i]" "E2E-IDE-Entry-$(date +%s)"
$B click "button:has-text('创建并进入')"
$B wait --networkidle
$B url  # 期望直接进入图谱 IDE
$B is visible "canvas, svg, [class*='echarts']"
$B screenshot /tmp/graphcreate-ide-redirect.png

### 预期结果
- 创建表单验证必填字段
- 成功创建后跳转到图谱 IDE 或图谱列表
- 名称重复被后端校验拦截
- "创建并进入"选项直接跳转到 IDE

### 问题发现
$B console --errors
$B network
$B screenshot /tmp/graphcreate-issue.png
# backend: GraphitiController (POST /api/v1/graph/create)
# frontend: views/graph/create.vue
```

---

### 2.4 时序历史

### 测试提示词

```
## 时序历史 (Temporal History) 测试

### 步骤 1：进入时序历史页面
$B goto http://localhost:5173/graph/temporal
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/temporal-loaded.png

### 步骤 2：选择图谱
$B snapshot -i
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child, [role='option']:first-child"
$B wait --networkidle
$B screenshot /tmp/temporal-graph-selected.png

### 步骤 3：当前状态视图（有效事实）
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/temporal-current-view.png
$B text
# 期望看到当前有效的事实列表（valid_at <= now 且 invalid_at IS NULL）
$B is visible "[class*='fact'], [class*='entity'], [class*='timeline']"

### 步骤 4：时间轴视图
$B snapshot -i
$B click "button:has-text('时间轴'), [class*='timeline-btn']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/temporal-timeline-view.png
# 期望看到 Episode 节点按 valid_at 时间戳排列的时间轴

### 步骤 5：历史快照查询（过去时间点）
$B snapshot -i
$B click "[class*='date-picker'], [class*='picker'], input[placeholder*='时间']"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/temporal-datepicker.png
$B click "[class*='cell']:first-child, [class*='date']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/temporal-snapshot-result.png
$B text  # 期望看到该时间点的快照，过期事实应被隐藏

### 步骤 6：Saga 链验证
$B snapshot -i
$B click "[class*='fact']:first-child, [class*='episode']:first-child"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/temporal-episode-detail.png
# 期望看到 NEXT_EPISODE 关系链的展开视图
$B is visible "[class*='next'], [class*='chain'], [class*='relation-link']"

### 步骤 7：事实失效操作
$B snapshot -i
$B click "button:has-text('失效'), button:has-text('使失效')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/temporal-invalidate-form.png
$B fill "[placeholder*='时间' i], [class*='date']" "2024-01-01"
$B click "button:has-text('确认失效')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/temporal-invalidated-result.png
# 刷新页面，验证该事实从当前视图中消失

### 步骤 8：实体历史版本查询
$B snapshot -i
$B fill "[placeholder*='实体' i], [placeholder*='entity' i]" "Apple"
$B wait 2s
$B snapshot
$B screenshot /tmp/temporal-entity-history.png
$B text  # 期望看到该实体在不同时间点的所有版本

### 预期结果
- 当前状态视图展示所有有效事实
- 时间轴视图正确展示 Episode 的起止时间
- 历史快照查询返回指定时间点的正确状态
- Saga 链完整展示 NEXT_EPISODE 关系
- 事实失效操作正确设置 invalid_at 时间戳

### 问题发现
$B console --errors
$B screenshot /tmp/temporal-issue.png
# backend: TemporalController (GET /api/v1/graph/:graphId/temporal/facts/current, /facts/at/:ref)
# frontend: views/graph/temporal.vue
```

---

## 3. 数据管理

### 3.1 类定义管理

### 测试提示词

```
## 类定义管理 (Data Classes) 测试

### 步骤 1：进入类管理页面
$B goto http://localhost:5173/data/classes
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/classes-loaded.png

### 步骤 2：选择图谱
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle

### 步骤 3：类列表验证
$B text
$B screenshot /tmp/classes-list.png
# 期望看到本体类列表（Entity, Person, Organization 等），每行显示：类名、描述、父类、属性数量
$B is visible "[class*='table'], [class*='list']"

### 步骤 4：类层级树视图
$B snapshot -i
$B click "button:has-text('树'), [class*='tree-view'], [title*='Tree']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/classes-tree-view.png
# 期望看到树形结构，Entity 为根节点，子类正确挂载在父类下

### 步骤 5：Tab 切换（列表/树）
$B snapshot -i
$B click "button:has-text('列表'), [class*='list-view']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/classes-list-view.png
# 验证 Tab 切换后内容正确切换

### 步骤 6：创建类
$B snapshot -i
$B click "button:has-text('新建类'), button:has-text('新建'), button:has-text('Create')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/classes-create-form.png
$B fill "[placeholder*='类名' i], [placeholder*='name' i]" "E2E-Test-Class-$(date +%s)"
$B fill "[placeholder*='描述' i], [placeholder*='desc' i]" "自动化测试创建的本体类"
$B screenshot /tmp/classes-create-filled.png
$B click "button[type='submit'], button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/classes-create-result.png
$B text  # 验证新类出现在列表/树中

### 步骤 7：设置继承关系
$B snapshot -i
$B click "button:has-text('新建类')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='类名' i]" "E2E-Sub-Class-$(date +%s)"
$B click "[class*='parent'], [class*='select']"  # 选择父类
$B wait --networkidle
$B click "[class*='option']:has-text('Entity'), [class*='option']:first-child"
$B wait --networkidle
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/classes-inherit-result.png
# 验证新子类正确挂载在 Entity 下

### 步骤 8：编辑类
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑'), [class*='row']:first-child [class*='edit']"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/classes-edit-form.png
$B fill "[placeholder*='描述' i]" "修改后的类描述 - E2E"
$B click "button:has-text('保存')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/classes-edit-result.png

### 步骤 9：循环继承检测
$B snapshot -i
$B click "button:has-text('新建类')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='类名' i]" "Cycle-Class-A"
# 设置父类为 Cycle-Class-B（如果存在），或尝试创建循环
$B click "[class*='parent']"
$B wait --networkidle
$B click "[class*='option']:last-child"
$B wait --networkidle
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/classes-cycle-detect.png
# 期望看到"循环继承"错误提示

### 步骤 10：删除类
$B snapshot -i
$B click "[class*='row']:last-child button:has-text('删除'), [class*='row']:last-child [class*='delete']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/classes-delete-check.png
$B click "button:has-text('确认'), .ant-modal button:has-text('确定')"
$B wait --networkidle
$B screenshot /tmp/classes-delete-result.png

### 预期结果
- 类列表展示所有本体类及元数据
- 树形视图正确展示继承层级结构
- 创建/编辑类成功
- 循环继承被后端 6 层验证引擎 Layer 1 拦截
- 删除类不受实例引用保护（删除类不删除实例数据）

### 问题发现
$B console --errors
$B screenshot /tmp/classes-issue.png
# backend: OntologyController (GET/POST/PUT/DELETE /api/v1/ontology/:graphId/classes)
# frontend: views/data/classes.vue, src/components/Ontology/OntologyClassExplorer.vue
```

---

### 3.2 属性管理

### 测试提示词

```
## 属性管理 (Data Properties) 测试

### 步骤 1：进入属性管理页面
$B goto http://localhost:5173/data/properties
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/properties-loaded.png

### 步骤 2：选择图谱
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle

### 步骤 3：属性列表验证
$B text
$B screenshot /tmp/properties-list.png
# 期望看到属性列表，每行显示：属性名、所属类（Domain）、数据类型（Range）、是否必需
$B is visible "[class*='table'], [class*='list']"

### 步骤 4：按类过滤属性
$B snapshot -i
$B click "[class*='filter'], [class*='select']"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/properties-filtered.png
$B text  # 验证只显示该类的属性

### 步骤 5：创建属性
$B snapshot -i
$B click "button:has-text('新建属性'), button:has-text('新建')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/properties-create-form.png
$B fill "[placeholder*='属性名' i], [placeholder*='name' i]" "testProperty"
# 选择所属类（Domain）
$B click "[class*='domain'], [class*='select']:first-child"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
# 选择数据类型（Range）
$B click "[class*='type'], [class*='select']:last-child, [class*='dataType']"
$B wait --networkidle
$B click "[class*='option']:has-text('string')"
$B wait --networkidle
$B screenshot /tmp/properties-create-filled.png
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/properties-create-result.png
$B text  # 验证新属性出现在列表

### 步骤 6：创建不同数据类型的属性
$B snapshot -i
$B click "button:has-text('新建属性')"
$B wait --networkidle
$B fill "[placeholder*='属性名' i]" "integerProp"
$B click "[class*='type']"
$B wait --networkidle
$B click "[class*='option']:has-text('integer')"
$B wait --networkidle
$B click "button:has-text('确认')"
$B wait --networkidle

$B click "button:has-text('新建属性')"
$B wait --networkidle
$B fill "[placeholder*='属性名' i]" "dateProp"
$B click "[class*='type']"
$B wait --networkidle
$B click "[class*='option']:has-text('date')"
$B click "button:has-text('确认')"
$B wait --networkidle

$B click "button:has-text('新建属性')"
$B wait --networkidle
$B fill "[placeholder*='属性名' i]" "booleanProp"
$B click "[class*='type']"
$B wait --networkidle
$B click "[class*='option']:has-text('boolean')"
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/properties-multi-type.png

### 步骤 7：编辑属性（设置必需和默认值）
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/properties-edit-form.png
$B fill "[placeholder*='默认值' i], [placeholder*='default' i]" "default_value"
# 设置为必需
$B click "[class*='switch'], [class*='toggle'], [type='checkbox']"
$B wait --networkidle
$B click "button:has-text('保存')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/properties-edit-result.png

### 步骤 8：查看类属性详情
$B snapshot -i
$B click "[class*='row']:first-child [class*='name'], [class*='row']:first-child [class*='domain']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/properties-detail.png
# 期望看到该属性的完整详情，包含 Domain、Range、约束列表

### 步骤 9：删除属性
$B snapshot -i
$B click "[class*='row']:last-child button:has-text('删除')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/properties-delete-result.png

### 预期结果
- 属性列表正确展示所有属性及其 Domain/Range
- 按类过滤功能正常
- 创建/编辑属性成功
- 不同数据类型的属性创建均正常
- 属性详情展示完整信息

### 问题发现
$B console --errors
$B screenshot /tmp/properties-issue.png
# backend: OntologyController (GET/POST/PUT/DELETE /api/v1/ontology/:graphId/properties)
# frontend: views/data/properties.vue
```

---

### 3.3 约束管理

### 测试提示词

```
## 约束管理 (Data Constraints) 测试

### 步骤 1：进入约束管理页面
$B goto http://localhost:5173/data/constraints
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/constraints-loaded.png

### 步骤 2：选择图谱
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle

### 步骤 3：约束列表
$B text
$B screenshot /tmp/constraints-list.png
# 期望看到约束列表，每行显示：属性名、约束类型（pattern/min/max）、约束值
$B is visible "[class*='table'], [class*='list']"

### 步骤 4：创建约束
$B snapshot -i
$B click "button:has-text('新建约束'), button:has-text('新建')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/constraints-create-form.png
# 选择属性
$B click "[class*='property'], [class*='select']"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
# 选择约束类型
$B click "[class*='type'], [class*='constraint-type']"
$B wait --networkidle
$B click "[class*='option']:has-text('pattern')"
$B wait --networkidle
$B fill "[placeholder*='值' i], [placeholder*='value' i]" "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"
$B screenshot /tmp/constraints-create-filled.png
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/constraints-create-result.png
$B text

### 步骤 5：批量验证
$B snapshot -i
$B click "button:has-text('批量验证'), button:has-text('验证全部')"
$B wait --networkidle
$B wait 5s
$B snapshot
$B screenshot /tmp/constraints-batch-validate.png
$B text  # 期望看到验证报告：通过数量、失败数量、失败详情

### 步骤 6：一致性检查
$B snapshot -i
$B click "button:has-text('一致性检查'), button:has-text('Consistency')"
$B wait --networkidle
$B wait 5s
$B snapshot
$B screenshot /tmp/constraints-consistency.png
$B text  # 期望看到一致性检查报告

### 步骤 7：版本历史
$B snapshot -i
$B click "button:has-text('版本历史'), button:has-text('历史')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/constraints-history.png
# 期望看到约束变更的时间线和差异对比

### 步骤 8：版本回滚
$B snapshot -i
$B click "button:has-text('回滚'), button:has-text('Rollback')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/constraints-rollback.png
$B click "button:has-text('确认回滚')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/constraints-rollback-result.png

### 预期结果
- 约束列表展示所有约束及类型
- 批量验证生成详细报告（通过/失败数量和详情）
- 一致性检查返回检查结果
- 版本历史展示变更记录
- 版本回滚功能正常

### 问题发现
$B console --errors
$B screenshot /tmp/constraints-issue.png
# backend: OntologyController (POST /api/v1/ontology/:graphId/validate/batch, GET /api/v1/ontology/:graphId/history)
# frontend: views/data/constraints.vue, src/components/Ontology/ConstraintEditor.vue
```

---

### 3.4 实体管理

### 测试提示词

```
## 实体管理 (Data Entities) 测试

### 步骤 1：进入实体管理页面
$B goto http://localhost:5173/data/entities
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/entities-loaded.png

### 步骤 2：选择图谱
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle

### 步骤 3：实体列表
$B text
$B screenshot /tmp/entities-list.png
# 期望看到实体列表，每行显示：名称、类型、创建时间、来源
$B is visible "[class*='table'], [class*='data-table'], .ant-table"

### 步骤 4：分页验证
$B snapshot -i
$B is visible ".ant-pagination"
$B click ".ant-pagination-next"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/entities-page2.png

### 步骤 5：搜索过滤
$B fill "[placeholder*='搜索' i], [placeholder*='search' i]" "Apple"
$B wait 1s
$B text
$B screenshot /tmp/entities-search-result.png
$B fill "[placeholder*='搜索' i]" ""

### 步骤 6：按类型过滤
$B snapshot -i
$B click "[class*='filter'], [class*='type-select']"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/entities-type-filter.png
$B text

### 步骤 7：创建实体
$B snapshot -i
$B click "button:has-text('新建实体'), button:has-text('新建'), button:has-text('Create')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/entities-create-form.png
$B fill "[placeholder*='名称' i], [placeholder*='name' i]" "E2E-Test-Entity-$(date +%s)"
$B screenshot /tmp/entities-create-filled.png
$B click "button[type='submit'], button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/entities-create-result.png
$B text  # 验证新实体出现在列表

### 步骤 8：查看实体详情
$B snapshot -i
$B click "[class*='row']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/entities-detail.png
$B text  # 期望看到：实体名称、类型、属性列表、关联关系列表、关联 Episode 列表

### 步骤 9：编辑实体
$B snapshot -i
$B click "button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/entities-edit-form.png
$B fill "[placeholder*='名称' i]" "Modified-Entity-Name-$(date +%s)"
$B click "button:has-text('保存')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/entities-edit-result.png

### 步骤 10：本体验证（违规测试）
$B snapshot -i
$B click "button:has-text('新建实体')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='名称' i]" "Validation-Test"
# 故意填写不符合数据类型的内容（如果表单支持该字段）
$B screenshot /tmp/entities-validation-test.png
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/entities-validation-result.png
# 期望看到验证错误提示（由 OntologyValidationService Layer 3 拦截）

### 步骤 11：批量选择
$B goto http://localhost:5173/data/entities
$B wait --networkidle
$B snapshot -i
$B click "[class*='checkbox']:nth-child(2), .ant-table-tbody [class*='checkbox']:first-child"
$B wait --networkidle
$B click "[class*='checkbox']:nth-child(3), .ant-table-tbody [class*='checkbox']:nth-child(2)"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/entities-multi-selected.png

### 步骤 12：批量删除
$B snapshot -i
$B click "button:has-text('批量删除'), button:has-text('删除选中')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/entities-batch-delete-confirm.png
$B click "button:has-text('确认'), .ant-modal button:has-text('确定')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/entities-batch-delete-result.png

### 步骤 13：去重功能
$B snapshot -i
$B click "button:has-text('去重'), button:has-text('Dedupe')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/entities-dedupe.png
$B text  # 期望看到相似实体对及相似度分数

### 步骤 14：从图谱 IDE 跳转查看
$B goto http://localhost:5173/graph/ide
$B wait --networkidle
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
$B click "canvas" --offset 300,200  # 点击一个节点
$B wait --networkidle
$B snapshot -i
$B click "button:has-text('在列表中查看'), button:has-text('查看详情')"
$B wait --networkidle
$B url  # 期望跳转到 /data/entities 并自动搜索该节点名称
$B screenshot /tmp/entities-from-ide.png

### 预期结果
- 实体列表正确展示，支持分页
- 搜索和类型过滤实时响应
- 创建/编辑实体成功，数据写入 Neo4j
- 本体验证在 Layer 3 正确拦截违规数据
- 批量选择和删除功能正常
- 去重功能识别相似实体
- 从图谱 IDE 可跳转到实体列表并自动搜索

### 问题发现
$B console --errors
$B network
$B screenshot /tmp/entities-issue.png
# backend: NodeController (GET/POST/PUT/DELETE /api/v1/nodes)
# frontend: views/data/entities.vue, src/components/Ontology/InstanceDataTable.vue, InstanceForm.vue
```

---

### 3.5 边管理

### 测试提示词

```
## 边管理 (Data Edges) 测试

### 步骤 1：进入边管理页面
$B goto http://localhost:5173/data/edges
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/edges-loaded.png

### 步骤 2：选择图谱
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle

### 步骤 3：边列表
$B text
$B screenshot /tmp/edges-list.png
# 期望看到边列表，每行显示：源实体 → 目标实体、关系类型、事实描述、valid_at
$B is visible "[class*='table'], .ant-table"

### 步骤 4：分页
$B snapshot -i
$B is visible ".ant-pagination"
$B click ".ant-pagination-next"
$B wait --networkidle
$B snapshot

### 步骤 5：搜索过滤
$B fill "[placeholder*='搜索' i], [placeholder*='fact' i]" "公司"
$B wait 1s
$B text
$B screenshot /tmp/edges-search-result.png
$B fill "[placeholder*='搜索' i]" ""

### 步骤 6：按关系类型过滤
$B snapshot -i
$B click "[class*='filter'], [class*='type-select']"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/edges-type-filter.png

### 步骤 7：查看边详情
$B snapshot -i
$B click "[class*='row']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/edges-detail.png
$B text  # 期望看到：源实体、目标实体、关系类型、事实描述、时间戳

### 步骤 8：创建边
$B snapshot -i
$B click "button:has-text('新建边'), button:has-text('新建')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/edges-create-form.png
# 选择源实体
$B click "[class*='source'], [class*='select']:first-child"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
# 选择目标实体
$B click "[class*='target'], [class*='select']:last-child"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
# 选择关系类型
$B click "[class*='edge-type'], [class*='type-select']"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
$B fill "[placeholder*='描述' i], [placeholder*='fact' i]" "E2E-Test-Relation"
$B screenshot /tmp/edges-create-filled.png
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/edges-create-result.png
$B text

### 步骤 9：编辑边
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='描述' i]" "Modified-Fact-Description"
$B click "button:has-text('保存')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/edges-edit-result.png

### 步骤 10：时序边设置
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/edges-temporal-edit.png
$B fill "[placeholder*='valid' i], [class*='date-picker']" "2024-01-01"
$B fill "[placeholder*='invalid' i], [class*='date-picker']" "2025-12-31"
$B click "button:has-text('保存')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/edges-temporal-result.png

### 步骤 11：删除边
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('删除')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认'), .ant-modal button:has-text('确定')"
$B wait --networkidle
$B screenshot /tmp/edges-delete-result.png
# 验证边被删除，源/目标实体节点保留

### 步骤 12：从图谱 IDE 跳转
$B goto http://localhost:5173/graph/ide
$B wait --networkidle
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
$B click "canvas" --offset 300,200
$B wait --networkidle
$B snapshot -i
$B click "button:has-text('查看关系'), button:has-text('关联边')"
$B wait --networkidle
$B url  # 期望跳转到 /data/edges
$B screenshot /tmp/edges-from-ide.png

### 预期结果
- 边列表展示所有关系及元数据
- 搜索和类型过滤正常
- 创建/编辑边成功，时序字段可设置
- 删除边不影响节点
- 从图谱 IDE 可跳转到边管理页面

### 问题发现
$B console --errors
$B screenshot /tmp/edges-issue.png
# backend: EdgeController (POST/GET/PUT/DELETE /api/v1/graph/edge)
# frontend: views/data/edges.vue
```

---

### 3.6 社区管理

### 测试提示词

```
## 社区管理 (Data Communities) 测试

### 步骤 1：进入社区管理页面
$B goto http://localhost:5173/data/communities
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/communities-loaded.png

### 步骤 2：选择图谱
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle

### 步骤 3：社区列表验证
$B text
$B screenshot /tmp/communities-list.png
# 期望看到社区卡片/列表，每项显示：社区名称、类型、实体数量、LLM 摘要

### 步骤 4：触发社区构建
$B snapshot -i
$B click "button:has-text('构建社区'), button:has-text('检测'), button:has-text('Build')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/communities-building.png
# 等待构建完成（后台任务）
$B wait 5s
$B snapshot
$B screenshot /tmp/communities-built.png
$B text

### 步骤 5：社区详情
$B snapshot -i
$B click "[class*='card']:first-child, [class*='community']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/communities-detail.png
$B text  # 期望看到：社区内实体列表、关系图谱、LLM 摘要

### 步骤 6：编辑社区
$B snapshot -i
$B click "button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/communities-edit-form.png
$B fill "[placeholder*='名称' i]" "Modified-Community-Name"
$B click "[class*='type'], [class*='select']"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
$B click "button:has-text('保存')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/communities-edit-result.png

### 步骤 7：社区类型管理
$B goto http://localhost:5173/data/communities
$B wait --networkidle
$B snapshot -i
$B click "button:has-text('类型管理'), button:has-text('类型')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/communities-type-management.png
# 创建新类型
$B click "button:has-text('新建类型')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='类型名' i]" "E2E-Test-Type"
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/communities-type-created.png

### 步骤 8：LLM 摘要生成
$B goto http://localhost:5173/data/communities
$B wait --networkidle
$B snapshot -i
$B click "[class*='card']:first-child"
$B wait --networkidle
$B snapshot -i
$B click "button:has-text('生成摘要'), button:has-text('AI 摘要')"
$B wait --networkidle
$B wait 8s
$B snapshot
$B screenshot /tmp/communities-summary-generated.png
$B text  # 期望看到 LLM 生成的自然语言描述

### 步骤 9：删除社区
$B goto http://localhost:5173/data/communities
$B wait --networkidle
$B snapshot -i
$B click "[class*='card']:last-child button:has-text('删除')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认'), .ant-modal button:has-text('确定')"
$B wait --networkidle
$B screenshot /tmp/communities-delete-result.png
# 验证社区节点被删除，社区内实体保留

### 预期结果
- 社区列表展示所有社区
- 社区构建有进度提示（构建中/完成）
- 社区详情展示实体列表和关联图谱
- 编辑社区类型/名称成功
- LLM 摘要生成后显示描述
- 删除社区保留社区内实体

### 问题发现
$B console --errors
$B screenshot /tmp/communities-issue.png
# backend: GraphitiController (POST /api/v1/graph/:graphId/communities/build, GET /api/v1/graph/:graphId/communities/list)
# frontend: views/data/communities.vue
```

---

### 3.7 Episode 管理

### 测试提示词

```
## Episode 管理 (Data Episodes) 测试

### 步骤 1：进入 Episode 管理页面
$B goto http://localhost:5173/data/episodes
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/episodes-loaded.png

### 步骤 2：选择图谱
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle

### 步骤 3：Episode 列表
$B text
$B screenshot /tmp/episodes-list.png
# 期望看到 Episode 列表，每行显示：名称、内容摘要、类型、来源、创建时间
$B is visible "[class*='table'], .ant-table"

### 步骤 4：分页
$B snapshot -i
$B is visible ".ant-pagination"
$B click ".ant-pagination-next"
$B wait --networkidle
$B snapshot

### 步骤 5：搜索过滤
$B fill "[placeholder*='搜索' i]" "test"
$B wait 1s
$B text
$B screenshot /tmp/episodes-search-result.png
$B fill "[placeholder*='搜索' i]" ""

### 步骤 6：Episode 详情
$B snapshot -i
$B click "[class*='row']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/episodes-detail.png
$B text  # 期望看到完整 Episode 内容、关联实体列表、关联关系列表

### 步骤 7：创建 Episode
$B snapshot -i
$B click "button:has-text('新建 Episode'), button:has-text('新建')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/episodes-create-form.png
$B fill "[placeholder*='名称' i]" "E2E-Test-Episode-$(date +%s)"
$B fill "textarea, [placeholder*='内容' i]" "这是自动化测试创建的 Episode 内容，包含事实描述。"
$B screenshot /tmp/episodes-create-filled.png
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/episodes-create-result.png
$B text

### 步骤 8：Episode 类型管理
$B snapshot -i
$B click "button:has-text('类型管理'), button:has-text('类型')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/episodes-type-management.png
$B click "button:has-text('新建类型')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='类型名' i]" "E2E-Test-Episode-Type"
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/episodes-type-created.png

### 步骤 9：Saga 链编排
$B goto http://localhost:5173/data/episodes
$B wait --networkidle
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('链接下一集'), button:has-text('Saga')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/episodes-saga-link-form.png
$B click "[class*='select']"
$B wait --networkidle
$B click "[class*='option']:nth-child(2)"
$B wait --networkidle
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/episodes-saga-linked.png
# 验证 NEXT_EPISODE 关系建立

### 步骤 10：时间线视图
$B snapshot -i
$B click "button:has-text('时间线'), [class*='timeline-view']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/episodes-timeline.png
# 期望看到 Episode 按时间顺序排列，Saga 链以箭头连接

### 步骤 11：删除 Episode
$B snapshot -i
$B click "[class*='row']:last-child button:has-text('删除')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/episodes-delete-result.png
# 验证 Episode 被删除，NEXT_EPISODE 关系一并清理

### 预期结果
- Episode 列表正确展示
- 创建/编辑 Episode 成功
- Episode 类型管理正常
- Saga 链编排正确建立 NEXT_EPISODE 关系
- 删除 Episode 级联清理 NEXT_EPISODE 关系

### 问题发现
$B console --errors
$B screenshot /tmp/episodes-issue.png
# backend: EpisodeController (GET/POST/DELETE /api/v1/graph/episode)
# frontend: views/data/episodes.vue, src/components/Ontology/EpisodeTypeEditModal.vue
```

---

### 3.8 社区-Episode 关联

### 测试提示词

```
## 社区-Episode 关联 (Community Episode) 测试

### 步骤 1：进入社区-Episode 关联页面
$B goto http://localhost:5173/data/community-episode
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/community-episode-loaded.png

### 步骤 2：选择图谱
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle

### 步骤 3：关联列表验证
$B text
$B screenshot /tmp/community-episode-list.png
# 期望看到社区与 Episode 的关联列表，每行显示：社区名称、Episode 名称、关联类型
$B is visible "[class*='table'], [class*='list']"

### 步骤 4：按社区过滤
$B snapshot -i
$B click "[class*='filter'], [class*='select']"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/community-episode-filtered.png

### 步骤 5：关联详情
$B snapshot -i
$B click "[class*='row']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/community-episode-detail.png
$B text  # 期望看到社区详情和关联的 Episode 详情

### 步骤 6：解除关联
$B snapshot -i
$B click "button:has-text('解除关联'), button:has-text('移除')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/community-episode-unlinked.png
# 验证关联关系被移除，社区和 Episode 实体本身保留

### 预期结果
- 关联列表展示所有社区-Episode 关系
- 按社区过滤正常
- 关联详情展示完整信息
- 解除关联不影响社区和 Episode 实体本身

### 问题发现
$B console --errors
$B screenshot /tmp/community-episode-issue.png
# backend: GraphitiController (社区关联 Episode 的查询端点)
# frontend: views/data/community-episode.vue
```

---

### 3.9 数据导入

### 测试提示词

```
## 数据导入 (Data Import) 测试

### 步骤 1：进入数据导入页面
$B goto http://localhost:5173/data/import
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/import-loaded.png

### 步骤 2：选择图谱
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle

### 步骤 3：Tab 验证（文本导入 / 文件导入）
$B snapshot -i
$B screenshot /tmp/import-tabs.png
$B is visible "button:has-text('文本导入'), button:has-text('文件导入')"
$B click "button:has-text('文件导入'), [class*='tab']:has-text('文件')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/import-file-tab.png
$B click "button:has-text('文本导入'), [class*='tab']:has-text('文本')"
$B wait --networkidle

### 步骤 4：文本导入
$B snapshot -i
$B fill "[placeholder*='名称' i]" "E2E-Import-$(date +%s)"
$B fill "textarea" "苹果公司由史蒂夫·乔布斯和史蒂夫·沃兹尼亚克于1976年在加利福尼亚州库比蒂诺创立。乔布斯后来于1985年离开苹果，并在1997年回归带领公司走向辉煌。苹果公司于1980年上市。"
$B screenshot /tmp/import-text-filled.png
$B click "button:has-text('导入'), button:has-text('提交')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/import-submitting.png
# 等待导入完成
$B wait 10s
$B snapshot
$B screenshot /tmp/import-result.png
$B text  # 期望看到导入结果：成功数量、失败数量

### 步骤 5：导入任务状态追踪
$B snapshot -i
$B click "button:has-text('查看任务'), button:has-text('任务列表')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/import-tasks.png
$B text  # 期望看到任务状态：pending → processing → completed/failed

### 步骤 6：验证导入结果（跨模块数据验证）
$B goto http://localhost:5173/data/entities
$B wait --networkidle
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
$B fill "[placeholder*='搜索' i]" "苹果"
$B wait 1s
$B text
$B screenshot /tmp/import-verified-entities.png
# 期望看到"苹果公司"实体

$B goto http://localhost:5173/data/edges
$B wait --networkidle
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
$B fill "[placeholder*='搜索' i]" "乔布斯"
$B wait 1s
$B text
$B screenshot /tmp/import-verified-edges.png
# 期望看到"乔布斯"相关的边

### 步骤 7：文件导入预览（JSON）
$B goto http://localhost:5173/data/import
$B wait --networkidle
$B click "button:has-text('文件导入'), [class*='tab']:has-text('文件')"
$B wait --networkidle
$B snapshot -i
$B click "[class*='upload'], input[type='file']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/import-file-upload-area.png
# 注意：需要提供实际文件路径
# $B upload "input[type='file']" "/path/to/test.json"
# 如果没有实际文件，跳过此步骤

### 步骤 8：本体约束验证（导入阶段）
$B goto http://localhost:5173/data/import
$B wait --networkidle
$B click "button:has-text('文本导入')"
$B wait --networkidle
$B fill "[placeholder*='名称' i]" "Validation-Import-Test"
$B fill "textarea" "invalid data that violates ontology constraints"
$B click "button:has-text('导入')"
$B wait --networkidle
$B wait 5s
$B snapshot
$B screenshot /tmp/import-validation-rejected.png
# 期望看到部分导入被拒绝的提示（合法数据成功，违规数据被 OntologyValidationService Layer 5 拦截）

### 步骤 9：取消导入任务
$B goto http://localhost:5173/data/import
$B wait --networkidle
$B snapshot -i
$B click "button:has-text('任务列表')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/import-tasks-cancel.png
$B click "button:has-text('取消'), [class*='cancel']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/import-task-cancelled.png

### 预期结果
- 文本导入成功，实体和关系被正确抽取
- 导入任务有状态跟踪（pending → processing → completed/failed）
- 导入后在实体/边管理页面可验证数据
- 本体约束在导入阶段生效，违规数据被拦截
- 取消导入任务功能正常

### 问题发现
$B console --errors
$B network
$B screenshot /tmp/import-issue.png
# backend: DataImportController (POST /api/v1/graph/data/add), DataExtractController (POST /api/v1/graph/extract/text)
# frontend: views/data/import.vue
```

---

### 3.10 数据导出

### 测试提示词

```
## 数据导出 (Data Export) 测试

### 步骤 1：进入数据导出页面
$B goto http://localhost:5173/data/export
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/export-loaded.png

### 步骤 2：选择图谱
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle

### 步骤 3：导出格式选择
$B snapshot -i
$B screenshot /tmp/export-formats.png
# 期望看到格式选项：JSON、CSV、Cypher
$B is visible "[class*='radio'], [class*='option']"

### 步骤 4：JSON 导出
$B snapshot -i
$B click "[class*='radio']:has-text('JSON'), [class*='option']:has-text('JSON')"
$B wait --networkidle
$B screenshot /tmp/export-json-selected.png
$B click "button:has-text('导出'), button:has-text('下载')"
$B wait --networkidle
$B wait 5s
$B snapshot
$B screenshot /tmp/export-json-result.png
# 期望生成可下载的 JSON 文件

### 步骤 5：CSV 导出
$B snapshot -i
$B click "[class*='radio']:has-text('CSV'), [class*='option']:has-text('CSV')"
$B wait --networkidle
$B click "button:has-text('导出')"
$B wait --networkidle
$B wait 5s
$B snapshot
$B screenshot /tmp/export-csv-result.png

### 步骤 6：Cypher 脚本导出
$B snapshot -i
$B click "[class*='radio']:has-text('Cypher'), [class*='option']:has-text('Cypher')"
$B wait --networkidle
$B click "button:has-text('导出')"
$B wait --networkidle
$B wait 5s
$B snapshot
$B screenshot /tmp/export-cypher-result.png

### 步骤 7：导出范围控制
$B snapshot -i
$B screenshot /tmp/export-scope-options.png
# 期望看到范围选项：全部、仅节点、仅边、仅 Entity、仅 Episode
$B click "[class*='radio']:has-text('仅节点'), [class*='option']:has-text('Entity')"
$B wait --networkidle
$B click "button:has-text('导出')"
$B wait --networkidle
$B wait 5s
$B snapshot
$B screenshot /tmp/export-entity-only-result.png

### 步骤 8：时间范围导出
$B snapshot -i
$B click "[class*='date-picker'], [class*='range']"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/export-date-range.png
$B click "[class*='cell']:first-child"  # 开始日期
$B wait --networkidle
$B click "[class*='cell']:nth-child(10)"  # 结束日期
$B wait --networkidle
$B click "button:has-text('导出')"
$B wait --networkidle
$B wait 5s
$B screenshot /tmp/export-date-range-result.png

### 步骤 9：导出历史
$B snapshot -i
$B click "button:has-text('导出历史'), button:has-text('历史')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/export-history.png
$B text  # 期望看到历史导出记录，包含导出时间、格式、大小

### 预期结果
- JSON 导出生成有效的可下载文件
- CSV 导出生成有效的可下载文件
- Cypher 脚本导出生成可被 Neo4j Browser 执行的脚本
- 导出范围控制正常（仅节点/仅边/全部）
- 时间范围过滤正确
- 导出历史记录历史导出

### 问题发现
$B console --errors
$B screenshot /tmp/export-issue.png
# backend: GraphitiController (GET /api/v1/graph/:graphId/export)
# frontend: views/data/export.vue
```

---

### 3.11 法律知识图谱

### 测试提示词

```
## 法律知识图谱 (Legal Knowledge Graph) 测试

### 步骤 1：进入法律知识图谱页面
$B goto http://localhost:5173/legal-kg
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/legal-kg-loaded.png

### 步骤 2：选择图谱
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle

### 步骤 3：页面布局验证
$B snapshot -i
$B screenshot /tmp/legal-kg-layout.png
# 期望看到：图谱可视化区域（ECharts）、左侧实体浏览器 Tab（法条/法院/当事人/案例）、右侧详情面板

### 步骤 4：法律条例导入
$B snapshot -i
$B click "button:has-text('导入条例'), button:has-text('导入'), [class*='upload']"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/legal-kg-import-form.png
$B fill "[placeholder*='名称' i]" "民商事调解条例-E2E-$(date +%s)"
$B fill "textarea" "《商事调解条例》第一条 为了规范商事调解活动，保障当事人合法权益，促进社会和谐稳定，制定本条例。第二条  本条例适用于中华人民共和国境内的商事调解活动。"
$B screenshot /tmp/legal-kg-provision-filled.png
$B click "button:has-text('导入')"
$B wait --networkidle
$B wait 10s
$B snapshot
$B screenshot /tmp/legal-kg-import-result.png
$B text  # 期望看到导入结果

### 步骤 5：法条列表验证
$B snapshot -i
$B click "button:has-text('法条'), [class*='tab']:has-text('法条')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/legal-kg-provisions-list.png
$B text  # 期望看到 LegalProvision 实体列表（条例名称、类型、章节数）

### 步骤 6：法院列表
$B snapshot -i
$B click "button:has-text('法院'), [class*='tab']:has-text('法院')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/legal-kg-courts-list.png
$B text

### 步骤 7：当事人列表
$B snapshot -i
$B click "button:has-text('当事人')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/legal-kg-parties-list.png
$B text

### 步骤 8：案例列表
$B snapshot -i
$B click "button:has-text('案例'), [class*='tab']:has-text('案例')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/legal-kg-cases-list.png
$B text

### 步骤 9：案例详情与关联图谱
$B snapshot -i
$B click "[class*='row']:first-child, [class*='card']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/legal-kg-case-detail.png
$B is visible "canvas, svg, [class*='echarts']"
$B text  # 期望看到案例详情及关联图谱（关联的法院、当事人、法条）

### 步骤 10：法律领域配置
$B snapshot -i
$B click "button:has-text('领域配置'), button:has-text('领域')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/legal-kg-domain-config.png
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:has-text('民')"
$B click "button:has-text('保存')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/legal-kg-domain-result.png

### 步骤 11：级联编辑（批量创建案例）
$B goto http://localhost:5173/legal-kg
$B wait --networkidle
$B snapshot -i
$B click "button:has-text('级联创建'), button:has-text('批量创建')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/legal-kg-cascade-form.png
$B fill "[placeholder*='案例名称' i]" "E2E-Test-Case-$(date +%s)"
$B fill "[placeholder*='当事人' i]" "张三"
$B fill "[placeholder*='法官' i]" "李法官"
$B screenshot /tmp/legal-kg-cascade-filled.png
$B click "button:has-text('提交'), button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/legal-kg-cascade-result.png
# 验证案例、当事人、法官实体均被创建，且关系（PRESIDED_BY、INVOLVES_PARTY）正确建立

### 步骤 12：法条关联图谱
$B snapshot -i
$B click "button:has-text('法条')"
$B wait --networkidle
$B click "[class*='row']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/legal-kg-provision-graph.png
$B is visible "canvas, svg"
$B text  # 期望看到该法条关联的所有案例

### 预期结果
- 法律知识图谱页面正确加载可视化区域和 Tab 面板
- 法律条例导入生成 LegalProvision 实体
- 法条/法院/当事人/案例 Tab 分类展示不同类型法律实体
- 案例详情展示关联图谱
- 领域配置可保存
- 级联编辑正确创建关联实体和关系
- 法条关联图谱展示关联案例

### 问题发现
$B console --errors
$B network
$B screenshot /tmp/legal-kg-issue.png
# backend: LegalImportController (POST /api/v1/graph/legal/import), LegalExtractController
# frontend: views/legal-kg/index.vue
```

---

## 4. 工具

### 4.1 混合检索

### 测试提示词

```
## 混合检索 (Hybrid Search) 测试

### 步骤 1：进入检索页面
$B goto http://localhost:5173/search
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/search-loaded.png

### 步骤 2：选择图谱
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle

### 步骤 3：基础检索
$B fill "[placeholder*='搜索' i], [placeholder*='search' i], input[type='search']" "苹果"
$B wait 1s
$B snapshot
$B screenshot /tmp/search-result.png
$B text  # 期望看到匹配结果

### 步骤 4：搜索模式切换
$B snapshot -i
$B screenshot /tmp/search-mode-options.png
$B is visible "[class*='mode'], [class*='type']"

# 仅 BM25 全文检索
$B click "[class*='mode'], [class*='select']"
$B wait --networkidle
$B click "[class*='option']:has-text('BM25'), [class*='item']:has-text('全文')"
$B wait --networkidle
$B fill "[placeholder*='搜索' i]" "苹果"
$B wait 2s
$B snapshot
$B screenshot /tmp/search-bm25-result.png
$B text

# 仅向量语义搜索
$B click "[class*='mode']"
$B wait --networkidle
$B click "[class*='option']:has-text('向量'), [class*='item']:has-text('语义')"
$B wait --networkidle
$B fill "[placeholder*='搜索' i]" "苹果"
$B wait 3s
$B snapshot
$B screenshot /tmp/search-vector-result.png

# 仅 BFS 图遍历
$B click "[class*='mode']"
$B wait --networkidle
$B click "[class*='option']:has-text('BFS')"
$B wait --networkidle
$B fill "[placeholder*='搜索' i]" "苹果"
$B wait 3s
$B snapshot
$B screenshot /tmp/search-bfs-result.png

# 混合搜索（RRF 融合）
$B click "[class*='mode']"
$B wait --networkidle
$B click "[class*='option']:has-text('混合'), [class*='item']:has-text('混合')"
$B wait --networkidle
$B fill "[placeholder*='搜索' i]" "苹果"
$B wait 3s
$B snapshot
$B screenshot /tmp/search-hybrid-result.png

### 步骤 5：搜索结果交互
$B text
$B is visible "[class*='result'], [class*='item'], [class*='card']"
$B screenshot /tmp/search-result-detail.png
$B click "[class*='result']:first-child, [class*='item']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/search-result-click-detail.png
$B text  # 期望看到实体详情（属性、关联关系）

### 步骤 6：从搜索结果跳转图谱 IDE
$B snapshot -i
$B click "button:has-text('在图谱中查看'), button:has-text('查看图谱')"
$B wait --networkidle
$B url  # 期望跳转到 /graph/ide
$B screenshot /tmp/search-to-ide.png
$B back
$B wait --networkidle

### 步骤 7：无结果场景
$B fill "[placeholder*='搜索' i]" "xyznonexistent99999"
$B wait 2s
$B snapshot
$B screenshot /tmp/search-no-result.png
# 期望看到"未找到结果"友好提示

### 步骤 8：搜索历史
$B snapshot -i
$B click "button:has-text('搜索历史'), [class*='history']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/search-history-panel.png
$B text  # 期望看到历史搜索列表
$B click "[class*='history-item']:first-child"
$B wait --networkidle
$B fill "[placeholder*='搜索' i]"  # 验证搜索词被填充
$B wait 1s

### 步骤 9：清空搜索历史
$B snapshot -i
$B click "button:has-text('清空历史'), button:has-text('Clear')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/search-history-cleared.png
$B text  # 验证历史列表已清空

### 步骤 10：高级搜索选项
$B snapshot -i
$B click "button:has-text('高级'), button:has-text('高级搜索'), [class*='advanced']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/search-advanced-options.png
# 期望看到高级选项：按类型过滤、按时间范围过滤、按关系过滤
$B is visible "[class*='filter'], [class*='type-filter'], [class*='date-range']"

### 预期结果
- 基础检索返回相关结果
- 4 种搜索模式（BM25/向量/BFS/混合）均可切换并返回结果
- 混合搜索结果综合了多路搜索
- 搜索结果可点击查看详情
- 无结果时显示友好提示
- 搜索历史记录和展示正常
- 高级搜索选项可用（类型过滤、时间范围、关系过滤）

### 问题发现
$B console --errors
$B network
$B screenshot /tmp/search-issue.png
# backend: SearchController (POST /api/v1/graph/search/hybrid/:graphId), SearchPipelineController (POST /api/v1/graph/search/pipeline/search)
# frontend: views/search/index.vue
```

---

### 4.2 自定义指令

### 测试提示词

```
## 自定义指令 (Custom Instructions) 测试

### 步骤 1：进入自定义指令页面
$B goto http://localhost:5173/custom-instructions
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/instructions-loaded.png

### 步骤 2：指令列表
$B text
$B screenshot /tmp/instructions-list.png
# 期望看到指令列表，每行显示：名称、内容摘要、状态（启用/禁用）、优先级
$B is visible "[class*='table'], [class*='list']"

### 步骤 3：创建指令
$B snapshot -i
$B click "button:has-text('新建指令'), button:has-text('新建')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/instructions-create-form.png
$B fill "[placeholder*='名称' i], [placeholder*='name' i]" "E2E-Test-Instruction-$(date +%s)"
$B fill "[placeholder*='内容' i], [placeholder*='content' i], textarea" "在抽取实体时，优先识别公司、组织等商业实体，以及金额、时间等关键信息。"
$B screenshot /tmp/instructions-create-filled.png
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/instructions-create-result.png
$B text

### 步骤 4：启用/禁用
$B snapshot -i
$B click "[class*='row']:first-child [class*='switch'], [class*='toggle']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/instructions-toggle.png
$B text  # 验证状态已切换

### 步骤 5：编辑指令
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/instructions-edit-form.png
$B fill "textarea" "修改后的自定义指令内容 - E2E"
$B click "button:has-text('保存')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/instructions-edit-result.png

### 步骤 6：优先级调整
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('上移'), [class*='up']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/instructions-reorder.png
$B text  # 验证优先级顺序已调整

### 步骤 7：删除指令
$B snapshot -i
$B click "[class*='row']:last-child button:has-text('删除')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/instructions-delete-result.png

### 步骤 8：验证指令在数据导入中生效
$B goto http://localhost:5173/data/import
$B wait --networkidle
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
$B fill "textarea" "腾讯是一家中国互联网巨头。"
$B click "button:has-text('导入')"
$B wait --networkidle
$B wait 10s
$B snapshot
$B screenshot /tmp/instructions-import-effect.png
$B text  # 期望看到抽取结果反映了自定义指令的优先级（公司实体优先被识别）

### 预期结果
- 指令列表正确展示
- 创建/编辑/删除指令成功
- 启用/禁用状态切换正常
- 优先级调整功能正常
- 自定义指令在数据导入中生效（LLM 调用时包含指令内容）

### 问题发现
$B console --errors
$B screenshot /tmp/instructions-issue.png
# backend: CustomInstructionController (GET/POST/DELETE /api/v1/custom-instructions)
# frontend: views/custom-instructions/index.vue
```

---

### 4.3 提示词管理

### 测试提示词

```
## 提示词管理 (Prompt Management) 测试

### 步骤 1：进入提示词管理页面
$B goto http://localhost:5173/prompt
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/prompt-loaded.png

### 步骤 2：模板列表
$B text
$B screenshot /tmp/prompt-list.png
# 期望看到模板列表，每行显示：编码、名称、类型、版本、启用状态
$B is visible "[class*='card'], [class*='table']"

### 步骤 3：模板类型过滤
$B snapshot -i
$B click "[class*='filter'], [class*='type-select']"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/prompt-type-filter.png

### 步骤 4：创建模板
$B snapshot -i
$B click "button:has-text('新建模板'), button:has-text('新建')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/prompt-create-form.png
$B fill "[placeholder*='编码' i]" "e2e_prompt_$(date +%s)"
$B fill "[placeholder*='名称' i]" "E2E-Test-Template"
$B click "[class*='type'], [class*='select']"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
$B fill "[placeholder*='System'], [class*='system-prompt']" "你是一个实体抽取助手。请从文本中抽取实体。"
$B fill "[placeholder*='User'], [class*='user-prompt']" "请从以下文本中抽取 {entity_type} 实体：{text}"
$B screenshot /tmp/prompt-create-filled.png
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/prompt-create-result.png
$B text

### 步骤 5：模板测试
$B snapshot -i
$B click "button:has-text('测试'), button:has-text('Test')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/prompt-test-form.png
$B fill "[placeholder*='entity' i]" "Organization"
$B fill "[placeholder*='text' i]" "苹果公司是一家美国科技公司。"
$B click "button:has-text('执行测试'), button:has-text('测试')"
$B wait --networkidle
$B wait 10s
$B snapshot
$B screenshot /tmp/prompt-test-result.png
$B text  # 期望看到 LLM 返回结果（JSON 格式）

### 步骤 6：启用/禁用模板
$B goto http://localhost:5173/prompt
$B wait --networkidle
$B snapshot -i
$B click "[class*='row']:first-child [class*='switch']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/prompt-toggle.png

### 步骤 7：版本管理
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('版本'), button:has-text('历史')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/prompt-version-history.png
$B text  # 期望看到版本列表和变更差异

### 步骤 8：回滚版本
$B snapshot -i
$B click "button:has-text('回滚'), button:has-text('Rollback')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/prompt-rollback-confirm.png
$B click "button:has-text('确认回滚')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/prompt-rollback-result.png

### 步骤 9：删除模板
$B snapshot -i
$B click "[class*='row']:last-child button:has-text('删除')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/prompt-delete-result.png

### 预期结果
- 模板列表正确展示
- 创建模板成功，模板内容正确保存
- 模板测试返回 LLM 结果
- 启用/禁用状态切换正常
- 版本历史展示变更记录
- 版本回滚功能正常
- 删除模板功能正常

### 问题发现
$B console --errors
$B screenshot /tmp/prompt-issue.png
# backend: PromptController (GET/POST/PUT/DELETE /api/v1/prompt/templates), PromptTestController (POST /api/v1/prompt/test/execute)
# frontend: views/prompt/index.vue
```

---

## 5. 个人中心

### 测试提示词

```
## 个人中心 (Profile) 测试

### 步骤 1：进入个人中心
$B goto http://localhost:5173/profile
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/profile-loaded.png

### 步骤 2：个人信息展示
$B text
$B screenshot /tmp/profile-info.png
# 期望看到：用户名、昵称、邮箱、手机号、头像、角色、创建时间
$B is visible "[class*='avatar'], [class*='user-info']"

### 步骤 3：编辑基本信息
$B snapshot -i
$B click "button:has-text('编辑资料'), button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/profile-edit-form.png
$B fill "[placeholder*='昵称' i], [placeholder*='nickname' i]" "E2E-Modified-Nickname"
$B fill "[placeholder*='邮箱' i], [placeholder*='email' i]" "e2e_modified@test.com"
$B fill "[placeholder*='手机号' i], [placeholder*='phone' i]" "13800138000"
$B screenshot /tmp/profile-edit-filled.png
$B click "button:has-text('保存')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/profile-edit-result.png
$B text  # 验证修改生效

### 步骤 4：修改密码（如果功能存在）
$B snapshot -i
$B click "button:has-text('修改密码'), button:has-text('密码')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/profile-password-form.png
$B fill "[placeholder*='原密码' i], [placeholder*='old' i]" "admin123"
$B fill "[placeholder*='新密码' i], [placeholder*='new' i]" "NewPassword123!"
$B fill "[placeholder*='确认密码' i], [placeholder*='confirm' i]" "NewPassword123!"
$B screenshot /tmp/profile-password-filled.png
$B click "button:has-text('确认修改'), button:has-text('保存')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/profile-password-result.png
# 用新密码重新登录验证
$B goto http://localhost:5173/login
$B wait --networkidle
$B fill "input[type='text']" "admin"
$B fill "input[type='password']" "NewPassword123!"
$B click "button[type='submit']"
$B wait --networkidle
$B url  # 期望成功登录
$B screenshot /tmp/profile-password-login.png
# 恢复原密码
$B goto http://localhost:5173/profile
$B wait --networkidle
$B click "button:has-text('修改密码')"
$B wait --networkidle
$B fill "[placeholder*='原密码' i]" "NewPassword123!"
$B fill "[placeholder*='新密码' i]" "admin123"
$B fill "[placeholder*='确认密码' i]" "admin123"
$B click "button:has-text('确认修改')"
$B wait --networkidle

### 步骤 5：头像上传（如果功能存在）
$B snapshot -i
$B click "[class*='avatar'], [class*='avatar-upload']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/profile-avatar-upload.png
# $B upload "[class*='avatar'] input[type='file']" "/path/to/avatar.png"
# 如果没有实际图片，跳过

### 步骤 6：通知设置
$B snapshot -i
$B click "button:has-text('通知设置'), button:has-text('通知偏好')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/profile-notification-settings.png
# 期望看到：邮件通知、站内通知、通知频率 等开关
$B click "[class*='switch']:first-child"  # 切换开关
$B wait --networkidle
$B click "button:has-text('保存')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/profile-notification-saved.png

### 步骤 7：登录历史（如果功能存在）
$B snapshot -i
$B click "button:has-text('登录历史'), button:has-text('登录记录')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/profile-login-history.png
$B text  # 期望看到登录历史列表：时间、IP、设备

### 步骤 8：退出登录
$B snapshot -i
$B click "button:has-text('退出登录'), button:has-text('Logout')"
$B wait --networkidle
$B url  # 期望跳转到 /login
$B screenshot /tmp/profile-logout.png

### 预期结果
- 个人中心展示当前用户的所有信息
- 编辑基本信息后数据正确更新
- 修改密码功能（如存在）正常工作
- 头像上传功能（如存在）正常工作
- 通知设置功能（如存在）正常工作
- 登录历史功能（如存在）展示历史记录
- 退出登录后正确跳转到登录页

### 问题发现
$B console --errors
$B screenshot /tmp/profile-issue.png
# backend: AuthController (GET /api/v1/auth/info, PUT /api/v1/auth/update)
# frontend: views/profile/index.vue

# ⚠️ 功能缺失标记（见附录 9.2）
# - 修改密码功能：需验证是否已在后端实现
# - 头像上传功能：需验证是否已在后端实现
# - 登录历史功能：需验证是否已在后端实现
```

---

## 6. 通知中心

### 测试提示词

```
## 通知中心 (Notification) 测试

### 步骤 1：进入通知中心
$B goto http://localhost:5173/notification
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/notification-loaded.png

### 步骤 2：通知列表验证
$B text
$B screenshot /tmp/notification-list.png
# 期望看到通知列表，每条显示：标题、内容摘要、时间、已读/未读状态
$B is visible "[class*='list'], [class*='notification']"

### 步骤 3：未读数量 Badge
$B snapshot -i
$B screenshot /tmp/notification-unread-badge.png
$B text  # 期望看到未读数量 Badge（如"5"）

### 步骤 4：按类型过滤
$B snapshot -i
$B click "[class*='filter'], [class*='type-select']"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/notification-type-filter.png

### 步骤 5：按已读状态过滤
$B snapshot -i
$B click "button:has-text('未读'), [class*='filter']:has-text('未读')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/notification-unread-filter.png
$B text  # 期望只显示未读通知

### 步骤 6：标记单条已读
$B snapshot -i
$B click "[class*='item']:first-child [class*='checkbox'], [class*='item']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/notification-mark-read.png
$B text  # 验证该通知变为已读状态（样式变化）

### 步骤 7：标记全部已读
$B snapshot -i
$B click "button:has-text('全部已读'), button:has-text('标为已读')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/notification-mark-all-read.png
$B text  # 验证所有通知已变为已读状态，Badge 数量归零

### 步骤 8：通知详情
$B snapshot -i
$B click "[class*='item']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/notification-detail.png
$B text  # 期望看到完整通知内容、发送时间、类型

### 步骤 9：删除单条通知
$B snapshot -i
$B click "button:has-text('删除'), [class*='delete']"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/notification-deleted.png
$B text  # 验证通知从列表消失

### 步骤 10：清空所有通知
$B snapshot -i
$B click "button:has-text('清空全部'), button:has-text('清空')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/notification-clear-confirm.png
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/notification-cleared.png
$B text  # 验证所有通知已清空

### 步骤 11：侧边栏通知入口（未读 Badge）
$B goto http://localhost:5173/dashboard
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/dashboard-notification-icon.png
# 检查侧边栏是否有通知图标，图标上是否显示未读数量 Badge
$B is visible "[class*='notification-icon'], [class*='bell'], [class*='badge']"

### 预期结果
- 通知列表正确展示所有通知
- 未读数量 Badge 准确显示
- 按类型和已读状态过滤正常
- 标记单条/全部已读功能正常
- 通知详情展示完整内容
- 删除单条和清空全部通知功能正常
- 侧边栏通知入口显示未读 Badge

### 问题发现
$B console --errors
$B screenshot /tmp/notification-issue.png
# backend: NotificationController (GET/PUT/DELETE /api/v1/notifications)
# frontend: views/notification/index.vue
```

---

## 7. 系统管理

### 7.1 用户管理

### 测试提示词

```
## 用户管理 (System User) 测试

### 步骤 1：进入用户管理页面
$B goto http://localhost:5173/system/user
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/users-loaded.png

### 步骤 2：用户列表
$B text
$B screenshot /tmp/users-list.png
# 期望看到用户列表，每行显示：用户名、昵称、邮箱、角色、状态、创建时间
$B is visible "[class*='table'], .ant-table"

### 步骤 3：分页
$B snapshot -i
$B is visible ".ant-pagination"
$B click ".ant-pagination-next"
$B wait --networkidle
$B snapshot

### 步骤 4：搜索过滤
$B fill "[placeholder*='搜索' i], [placeholder*='search' i]" "admin"
$B wait 1s
$B text
$B screenshot /tmp/users-search-result.png
$B fill "[placeholder*='搜索' i]" ""

### 步骤 5：新建用户
$B snapshot -i
$B click "button:has-text('新建用户'), button:has-text('新建')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/users-create-form.png
$B fill "[placeholder*='用户名' i]" "e2e_user_$(date +%s)"
$B fill "[placeholder*='密码' i]" "Test123456"
$B fill "[placeholder*='昵称' i]" "E2E 测试用户"
$B fill "[placeholder*='邮箱' i]" "e2e@test.com"
$B fill "[placeholder*='手机号' i]" "13800138000"
$B screenshot /tmp/users-create-filled.png
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/users-create-result.png
$B text

### 步骤 6：唯一性验证
$B snapshot -i
$B click "button:has-text('新建用户')"
$B wait --networkidle
$B fill "[placeholder*='用户名' i]" "admin"
$B fill "[placeholder*='密码' i]" "Test123456"
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/users-duplicate-check.png
# 期望看到"用户名已存在"错误提示

### 步骤 7：编辑用户
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/users-edit-form.png
$B fill "[placeholder*='昵称' i]" "E2E-Modified-Nickname"
$B click "button:has-text('保存')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/users-edit-result.png

### 步骤 8：分配角色
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/users-role-edit.png
$B click "[class*='role-select'], [class*='select']"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B click "button:has-text('保存')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/users-role-result.png

### 步骤 9：禁用用户
$B snapshot -i
$B click "[class*='row']:first-child [class*='switch'], [class*='toggle']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/users-disable-result.png
# 尝试用被禁用账号登录
$B goto http://localhost:5173/login
$B wait --networkidle
$B fill "input[type='text']" "admin"
$B fill "input[type='password']" "admin123"
$B click "button[type='submit']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/users-disabled-login.png
# 期望：登录被拒绝，返回 401

### 步骤 10：删除用户
$B goto http://localhost:5173/system/user
$B wait --networkidle
$B snapshot -i
$B click "[class*='row']:last-child button:has-text('删除')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/users-delete-result.png

### 预期结果
- 用户列表正确展示，支持分页和搜索
- 新建用户成功
- 用户名唯一性在前后端双重校验
- 编辑/分配角色功能正常
- 禁用用户立即生效（登录被拒绝）
- 删除用户执行逻辑删除（deleted=true），不物理删除数据

### 问题发现
$B console --errors
$B screenshot /tmp/users-issue.png
# backend: UserController (GET/POST/PUT/DELETE /api/v1/admin/system/user)
# frontend: views/system/user/index.vue
```

---

### 7.2 角色管理

### 测试提示词

```
## 角色管理 (System Role) 测试

### 步骤 1：进入角色管理页面
$B goto http://localhost:5173/system/role
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/roles-loaded.png

### 步骤 2：角色列表
$B text
$B screenshot /tmp/roles-list.png
# 期望看到角色列表，每行显示：角色名称、编码、备注、用户数量
$B is visible "[class*='table'], .ant-table"

### 步骤 3：创建角色
$B snapshot -i
$B click "button:has-text('新建角色'), button:has-text('新建')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/roles-create-form.png
$B fill "[placeholder*='名称' i]" "E2E-Role"
$B fill "[placeholder*='编码' i]" "ROLE_E2E_TEST"
$B fill "[placeholder*='备注' i]" "自动化测试创建的角色"
$B screenshot /tmp/roles-create-filled.png
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/roles-create-result.png

### 步骤 4：分配权限
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑'), [class*='row']:first-child button:has-text('权限')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/roles-permission-form.png
# 期望看到权限树（树形结构）
$B is visible "[class*='tree'], [class*='permission-tree']"
# 选择部分权限
$B click "[class*='tree-node'] [class*='checkbox']:first-child, [class*='tree-node'] [class*='checkbox']:nth-child(2)"
$B wait --networkidle
$B snapshot
$B click "button:has-text('保存')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/roles-permission-result.png

### 步骤 5：删除保护验证
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('删除')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/roles-delete-check.png
# 期望看到保护提示："该角色下有 X 个用户，无法删除"
$B click "button:has-text('取消')"
$B wait --networkidle

### 步骤 6：删除空角色
$B snapshot -i
$B click "[class*='row']:last-child button:has-text('删除')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/roles-delete-result.png

### 预期结果
- 角色列表正确展示
- 创建角色成功
- 权限树正确展示，权限选择和保存正常
- 有用户关联的角色被保护不可删除
- 无用户关联的角色可正常删除

### 问题发现
$B console --errors
$B screenshot /tmp/roles-issue.png
# backend: RoleController (GET/POST/PUT/DELETE /api/v1/admin/system/role)
# frontend: views/system/role/index.vue
```

---

### 7.3 菜单管理

### 测试提示词

```
## 菜单管理 (System Menu) 测试

### 步骤 1：进入菜单管理页面
$B goto http://localhost:5173/system/menu
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/menus-loaded.png

### 步骤 2：菜单树
$B text
$B screenshot /tmp/menus-tree.png
# 期望看到树形菜单结构
$B is visible "[class*='tree'], [class*='menu-tree'], .ant-tree"

### 步骤 3：创建菜单
$B snapshot -i
$B click "button:has-text('新建菜单'), button:has-text('新建')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/menus-create-form.png
$B fill "[placeholder*='名称' i]" "E2E-Test-Menu"
$B fill "[placeholder*='路由' i]" "/e2e-test"
$B fill "[placeholder*='组件路径' i]" "e2eTest/index.vue"
$B screenshot /tmp/menus-create-filled.png
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/menus-create-result.png

### 步骤 4：创建子菜单
$B snapshot -i
$B click "[class*='tree-node']:first-child button:has-text('添加子菜单'), button:has-text('新增子级')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='名称' i]" "E2E-Sub-Menu"
$B fill "[placeholder*='路由' i]" "/e2e-test/sub"
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/menus-submenu-created.png

### 步骤 5：编辑菜单
$B snapshot -i
$B click "[class*='tree-node']:first-child button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/menus-edit-form.png
$B fill "[placeholder*='名称' i]" "Modified-Menu-Name"
$B click "button:has-text('保存')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/menus-edit-result.png

### 步骤 6：拖拽排序
$B snapshot -i
$B click "button:has-text('排序'), button:has-text('拖拽排序')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/menus-sort-mode.png
# 期望看到拖拽手柄，拖动菜单项到新位置
$B click "button:has-text('保存排序')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/menus-sort-result.png

### 步骤 7：删除菜单
$B snapshot -i
$B click "[class*='tree-node']:last-child button:has-text('删除')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/menus-delete-result.png
# 验证子菜单一并被删除或提升

### 预期结果
- 菜单树正确展示层级结构
- 创建菜单/子菜单功能正常
- 编辑菜单功能正常
- 拖拽排序功能正常
- 删除菜单后从所有角色的权限树中移除

### 问题发现
$B console --errors
$B screenshot /tmp/menus-issue.png
# backend: MenuController (GET/POST/PUT/DELETE /api/v1/admin/system/menu)
# frontend: views/system/menu/index.vue
```

---

### 7.4 系统配置

### 测试提示词

```
## 系统配置 (System Config) 测试

### 步骤 1：进入系统配置页面
$B goto http://localhost:5173/system/config
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/config-loaded.png

### 步骤 2：配置分组展示
$B text
$B screenshot /tmp/config-list.png
# 期望看到按分组展示的配置列表：通用配置、AI 配置、数据库配置
$B is visible "[class*='table'], [class*='list'], [class*='group']"

### 步骤 3：Tab 切换配置分组
$B snapshot -i
$B screenshot /tmp/config-tabs.png
$B click "[class*='tab']:has-text('AI'), [class*='tab']:has-text('AI配置')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/config-ai-tab.png
$B text  # 期望看到 LLM 提供商、API Key、模型选择等配置项

$B click "[class*='tab']:has-text('通用'), [class*='tab']:has-text('通用配置')"
$B wait --networkidle

### 步骤 4：编辑配置
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/config-edit-form.png
$B fill "[placeholder*='值' i], [class*='input']" "modified_value"
$B click "button:has-text('保存')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/config-edit-result.png

### 步骤 5：配置值格式验证
$B snapshot -i
$B click "[class*='row']:nth-child(2) button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B fill "[class*='input']" "invalid-format-!@#$"
$B click "button:has-text('保存')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/config-format-error.png
# 期望看到格式验证错误

### 步骤 6：重置配置
$B snapshot -i
$B click "button:has-text('重置'), button:has-text('恢复默认')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/config-reset-result.png

### 预期结果
- 配置按分组展示（通用/AI/数据库）
- AI 配置 Tab 展示 LLM 相关配置项
- 编辑配置后保存成功
- 配置值格式验证正常
- 重置配置恢复默认值

### 问题发现
$B console --errors
$B screenshot /tmp/config-issue.png
# backend: SystemConfigController (GET/PUT/POST/DELETE /api/v1/admin/system/config)
# frontend: views/system/config/index.vue
```

---

### 7.5 操作日志

### 测试提示词

```
## 操作日志 (System Log) 测试

### 步骤 1：进入操作日志页面
$B goto http://localhost:5173/system/log
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/logs-loaded.png

### 步骤 2：日志列表
$B text
$B screenshot /tmp/logs-list.png
# 期望看到操作日志列表，每行显示：操作用户、操作类型、操作对象、操作时间、IP 地址、耗时
$B is visible "[class*='table'], .ant-table"

### 步骤 3：分页
$B snapshot -i
$B is visible ".ant-pagination"
$B click ".ant-pagination-next"
$B wait --networkidle
$B snapshot

### 步骤 4：按用户过滤
$B snapshot -i
$B click "[class*='filter'], [class*='select']"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/logs-user-filter.png

### 步骤 5：按操作类型过滤
$B snapshot -i
$B click "[class*='filter'], [class*='select']"
$B wait --networkidle
$B click "[class*='option']:has-text('CREATE')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/logs-type-filter.png

### 步骤 6：时间范围过滤
$B snapshot -i
$B click "[class*='date-picker'], [class*='range']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/logs-date-range.png
$B click "[class*='cell']:first-child"
$B wait --networkidle
$B click "[class*='cell']:last-child"
$B wait --networkidle
$B click "button:has-text('确定')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/logs-date-filtered.png

### 步骤 7：日志详情
$B snapshot -i
$B click "[class*='row']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/logs-detail.png
$B text  # 期望看到：请求 URL、请求方法、请求参数、返回结果、耗时、状态码

### 步骤 8：日志导出
$B snapshot -i
$B click "button:has-text('导出'), button:has-text('下载')"
$B wait --networkidle
$B wait 3s
$B snapshot
$B screenshot /tmp/logs-export-result.png
# 期望生成 CSV/Excel 文件

### 步骤 9：清空日志
$B snapshot -i
$B click "button:has-text('清空日志'), button:has-text('清空')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/logs-cleared.png
$B text  # 验证日志列表为空

### 预期结果
- 日志列表正确展示所有操作记录
- 按用户/操作类型/时间范围过滤正常
- 日志详情展示完整的请求和响应信息
- 日志导出功能正常
- 清空日志功能正常

### 问题发现
$B console --errors
$B screenshot /tmp/logs-issue.png
# backend: OperationLogController (GET/DELETE /api/v1/admin/system/log)
# frontend: views/system/log/index.vue
```

---

### 7.6 系统监控

### 测试提示词

```
## 系统监控 (System Monitor) 测试

### 步骤 1：进入系统监控页面
$B goto http://localhost:5173/monitor
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/monitor-loaded.png

### 步骤 2：健康状态概览
$B text
$B screenshot /tmp/monitor-health-overview.png
# 期望看到各组件健康状态：后端服务、Neo4j、PostgreSQL、Redis
# 健康状态：绿色/UP；异常：红色/DOWN

### 步骤 3：ECharts 资源图表
$B wait --networkidle
$B is visible "canvas, svg, [class*='echarts']"
$B screenshot /tmp/monitor-charts.png
# 期望看到：内存使用率、CPU 使用率、JVM 堆内存 等折线图

### 步骤 4：数据库连接池
$B wait --networkidle
$B snapshot
$B screenshot /tmp/monitor-db-pool.png
# 期望看到：活跃连接数、空闲连接数、最大连接数、连接池命中率

### 步骤 5：Neo4j 状态
$B wait --networkidle
$B snapshot
$B screenshot /tmp/monitor-neo4j.png
# 期望看到：当前会话数、事务状态、页面缓存使用率

### 步骤 6：Redis 状态
$B wait --networkidle
$B snapshot
$B screenshot /tmp/monitor-redis.png
# 期望看到：内存使用、键数量、命中率、从节点状态

### 步骤 7：API 请求量排行
$B wait --networkidle
$B snapshot
$B screenshot /tmp/monitor-api-ranking.png
# 期望看到各 API 接口的请求次数排行

### 步骤 8：刷新功能
$B click "button:has-text('刷新'), button:has-text('Refresh')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/monitor-refreshed.png
$B text  # 验证数据已更新

### 步骤 9：预警阈值验证
$B text
$B screenshot /tmp/monitor-alerts.png
# 检查是否有超过阈值的警告（如 CPU > 80%、内存 > 85%、连接池耗尽）
# 期望有警告时显示红色/橙色提示

### 步骤 10：Tab 切换（如有多个监控 Tab）
$B snapshot -i
$B is visible "[class*='tab']"
$B screenshot /tmp/monitor-tabs.png
$B click "[class*='tab']:nth-child(2)"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/monitor-tab2.png

### 预期结果
- 健康状态指示准确（UP/DOWN）
- 各组件状态（数据库/Neo4j/Redis）可见且数据正确
- ECharts 图表正确渲染
- 刷新功能实时更新数据
- 异常情况下显示预警提示
- 多个监控 Tab 切换正常

### 问题发现
$B console --errors
$B screenshot /tmp/monitor-issue.png
# backend: MonitorService, Spring Actuator
# frontend: views/monitor/index.vue
```

---

## 8. 跨模块端到端流程

```
## 端到端流程测试

这些测试验证多个模块间的数据流转，确保数据在模块间正确传递。

### 流程 1：仪表盘 → 图谱 IDE → 数据导入 → 仪表盘数据更新

### 步骤 1.1：登录并进入仪表盘
$B goto http://localhost:5173/login
$B wait --networkidle
$B fill "input[type='text']" "admin"
$B fill "input[type='password']" "admin123"
$B click "button[type='submit']"
$B wait --networkidle
$B url  # /dashboard

### 步骤 1.2：记录仪表盘初始统计
$B goto http://localhost:5173/dashboard
$B wait --networkidle
$B text  # 记录当前实体数量和图谱数量

### 步骤 1.3：进入图谱 IDE
$B goto http://localhost:5173/graph/ide
$B wait --networkidle
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
$B screenshot /tmp/e2e-ide-loaded.png

### 步骤 1.4：添加节点
$B click "button:has-text('添加节点')"
$B wait --networkidle
$B fill "[placeholder*='名称' i]" "E2E-Integration-Node"
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/e2e-node-added.png

### 步骤 1.5：验证节点出现在实体管理列表
$B goto http://localhost:5173/data/entities
$B wait --networkidle
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
$B fill "[placeholder*='搜索' i]" "E2E-Integration-Node"
$B wait 1s
$B text
$B screenshot /tmp/e2e-entity-verified.png
$B fill "[placeholder*='搜索' i]" ""

### 步骤 1.6：验证仪表盘统计数据更新
$B goto http://localhost:5173/dashboard
$B wait --networkidle
$B text  # 对比步骤 1.2，实体数量应 +1
$B screenshot /tmp/e2e-dashboard-updated.png

### 流程 2：数据导入 → 实体管理 → 图谱 IDE → 搜索

### 步骤 2.1：数据导入
$B goto http://localhost:5173/data/import
$B wait --networkidle
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
$B fill "textarea" "特斯拉是一家美国电动汽车和清洁能源公司，由埃隆·马斯克于2003年创立。"
$B click "button:has-text('导入')"
$B wait --networkidle
$B wait 10s
$B snapshot
$B screenshot /tmp/e2e-import-completed.png

### 步骤 2.2：验证实体出现在实体管理列表
$B goto http://localhost:5173/data/entities
$B wait --networkidle
$B fill "[placeholder*='搜索' i]" "特斯拉"
$B wait 1s
$B text
$B screenshot /tmp/e2e-import-entity-verified.png

### 步骤 2.3：验证实体出现在图谱 IDE
$B goto http://localhost:5173/graph/ide
$B wait --networkidle
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
$B fill "[placeholder*='搜索' i]" "特斯拉"
$B wait 2s
$B snapshot
$B screenshot /tmp/e2e-import-ide-verified.png

### 步骤 2.4：验证实体出现在混合检索
$B goto http://localhost:5173/search
$B wait --networkidle
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle
$B fill "[placeholder*='搜索' i]" "特斯拉"
$B wait 2s
$B text
$B screenshot /tmp/e2e-import-search-verified.png

### 流程 3：用户管理 → 角色权限 → 菜单访问控制

### 步骤 3.1：创建一个数据分析师角色
$B goto http://localhost:5173/system/role
$B wait --networkidle
$B click "button:has-text('新建角色')"
$B wait --networkidle
$B fill "[placeholder*='名称' i]" "E2E-Data-Analyst"
$B fill "[placeholder*='编码' i]" "ROLE_DATA_ANALYST"
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot

### 步骤 3.2：只分配查看权限（不含编辑）
$B click "[class*='row']:last-child button:has-text('编辑')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('保存')"
$B wait --networkidle

### 步骤 3.3：创建一个数据分析师用户
$B goto http://localhost:5173/system/user
$B wait --networkidle
$B click "button:has-text('新建用户')"
$B wait --networkidle
$B fill "[placeholder*='用户名' i]" "data_analyst"
$B fill "[placeholder*='密码' i]" "Analyst123!"
$B fill "[placeholder*='昵称' i]" "数据分析师"
$B click "button:has-text('确认')"
$B wait --networkidle

### 步骤 3.4：分配数据分析师角色
$B click "[class*='row']:last-child button:has-text('编辑')"
$B wait --networkidle
$B click "[class*='role-select']"
$B wait --networkidle
$B click "[class*='option']:has-text('Data-Analyst')"
$B click "button:has-text('保存')"
$B wait --networkidle

### 步骤 3.5：以数据分析师身份登录（跳过 — 需多浏览器会话）
# 注意：由于 browse 使用同一会话，此步骤需要手动验证
# 预期：以 data_analyst 账号登录后，编辑按钮应被隐藏，只能查看数据

### 预期结果
- 端到端数据流验证成功
- 节点在 IDE 添加后实时出现在实体列表
- 仪表盘统计随数据变化同步更新
- 数据导入后实体在 IDE 和搜索中均可发现
- 权限控制正确限制数据分析师的编辑权限
```

---

## 9. 功能缺失清单（待开发）

以下功能在企业级知识图谱平台中属于标准配置，但当前前端实现中**缺失或未完善**。测试时将这些作为"预期失败"项记录，推动后续开发。

### 9.1 高优先级缺失功能

| # | 功能 | 模块 | 影响 | 建议实现 |
|---|------|------|------|----------|
| M01 | **密码修改** | 个人中心 `/profile` | 用户无法自助修改密码 | 在 `views/profile/index.vue` 添加密码修改表单，调用 `AuthController` 或新增 `/api/v1/auth/password` 端点 |
| M02 | **头像上传** | 个人中心 `/profile` | 用户无法上传头像 | 添加文件上传组件，调用后端 `/api/v1/auth/avatar` 接口（需后端支持） |
| M03 | **登录历史** | 个人中心 `/profile` | 用户无法查看自己账号的登录记录 | 新增 `LoginHistoryController`，前端在 `/profile` 添加"登录历史"Tab |
| M04 | **通知设置** | 个人中心 `/profile` | 用户无法配置通知偏好（邮件/站内） | 在 `views/profile/index.vue` 添加通知偏好设置表单，调用 `NotificationController` 的 `/settings` 端点 |
| M05 | **高级搜索选项** | 混合检索 `/search` | 无法按类型/时间/关系过滤搜索结果 | 在 `views/search/index.vue` 添加展开式高级搜索面板，调用 `SearchController` 的高级参数 |
| M06 | **API Key 管理界面** | 系统配置 `/system/config` | 管理员无法通过 UI 管理 LLM API Key | 在 `views/system/config/index.vue` 添加 API Key 管理 Tab，调用 `SystemConfigController` 的 `/key/{key}` 接口 |

### 9.2 中优先级缺失功能

| # | 功能 | 模块 | 影响 | 建议实现 |
|---|------|------|------|----------|
| M07 | **批量导入预览与字段映射** | 数据导入 `/data/import` | 文件导入时无法预览数据结构和映射字段 | 在文件导入 Tab 添加预览表格和字段映射步骤，调用 `DataExtractController` 的 `/preview` 端点 |
| M08 | **导出任务队列** | 数据导出 `/data/export` | 大规模导出无异步任务追踪 | 添加导出任务 Tab，调用 `ImportTaskController` 类似的 `ExportTaskController`，展示导出进度 |
| M09 | **数据回收站** | 实体/边管理 `/data/entities` | 误删数据无法恢复 | 在实体管理添加"回收站"入口，实体/边删除改为软删除（`deleted=true`），回收站提供恢复功能 |
| M10 | **Schema.org 导入** | 类定义管理 `/data/classes` | 无法从 Schema.org 快速导入标准本体 | 在类管理页面添加"从 Schema.org 导入"按钮，调用 `OntologyController` 的 `/import/schema-org` 端点 |
| M11 | **推理机预热与状态** | 类定义管理 `/data/classes` | 无法管理本体推理机状态 | 在类管理页面添加推理机状态指示和"预热"按钮，调用 `/reasoners/warmup` 和 `/reasoners/status` 端点 |
| M12 | **多语言切换 UI** | 全局 | 前端不支持语言切换 | 在侧边栏或用户菜单添加语言切换器，调用 Vue i18n 的 `locale` 切换 |

### 9.3 低优先级缺失功能

| # | 功能 | 模块 | 影响 | 建议实现 |
|---|------|------|------|----------|
| M13 | **批量编辑实体** | 实体管理 `/data/entities` | 只能逐个编辑实体 | 在实体管理列表添加批量选择后的"批量编辑"功能 |
| M14 | **关系类型管理** | 边管理 `/data/edges` | 无法管理关系类型的元数据（是否必需、描述等） | 在边管理添加"关系类型管理"Tab，调用 `OntMetadataController` 的 `/relationship-meta` 端点 |
| M15 | **数据质量报告页** | 数据管理 | 无独立的数据质量可视化报告 | 新增 `/data/quality` 页面，展示完整性、一致性、唯一性等质量指标图表 |
| M16 | **操作日志实时推送** | 操作日志 `/system/log` | 日志仅手动刷新，无 WebSocket 实时推送 | 集成 WebSocket，实时推送新日志到前端页面 |
| M17 | **图谱版本对比** | 图谱管理 `/graph/list` | 无法对比两个图谱版本的差异 | 在图谱列表添加"版本对比"按钮，调用时序 API 并用 `VersionDiffViewer` 组件展示差异 |
| M18 | **定时任务管理 UI** | 系统管理 | 无定时任务管理界面 | 新增 `/system/schedule` 页面，管理导入/导出/社区检测等定时任务 |

### 缺失功能测试清单模板

```
## [M01] 密码修改 — 功能缺失测试

### 前置条件
测试账号：admin / admin123（管理员）

### 测试步骤
$B goto http://localhost:5173/profile
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/missing-password-change.png

### 验证
$B is visible "button:has-text('修改密码'), button:has-text('密码')"
# 期望：❌ 失败 — 该功能尚未实现

### 现状截图
# [附截图]

### 建议实现
1. 后端：在 AuthController 添加 PUT /api/v1/auth/password
2. 前端：在 views/profile/index.vue 添加密码修改表单
3. 验证：修改后用新密码登录成功，原密码登录失败

### 相关 API
- 后端待实现：PUT /api/v1/auth/password {oldPassword, newPassword}
- 前端待实现：views/profile/index.vue 密码修改表单组件

### 优先级
[P1] 高优先级 — 影响用户账号安全（无法修改密码）
```

---

## 10. 测试执行优先级与回归策略

### 测试执行优先级

| 优先级 | 模块 | 理由 |
|--------|------|------|
| P0 | 登录 → 仪表盘 | 核心入口，其他依赖登录态 |
| P0 | 图谱列表 → 图谱 IDE | 核心业务功能 |
| P0 | 数据导入 → 实体/边管理 | 核心数据流 |
| P0 | 端到端流程 1-2 | 验证跨模块数据一致性 |
| P1 | 混合检索 | 核心搜索能力 |
| P1 | 类/属性/约束管理 | 本体系统核心 |
| P1 | 用户管理 → 角色管理 → 菜单管理 | 系统管理基础 |
| P1 | 个人中心、通知中心 | 用户体验核心 |
| P2 | Episode 管理 → 社区管理 | 高级图谱功能 |
| P2 | 法律知识图谱 | 专项扩展功能 |
| P2 | 自定义指令 → 提示词管理 | AI 配置功能 |
| P2 | 缺失功能清单（M01-M06） | 推动功能完善 |
| P3 | 时序历史 | 高级时序功能 |
| P3 | 数据导出 | 数据流转功能 |
| P3 | 系统配置 → 操作日志 → 系统监控 | 系统保障功能 |
| P3 | 缺失功能清单（M07-M18） | 推动功能完善 |

### 回归测试策略

#### 冒烟回归（每次代码变更后）

```
执行：登录 → 仪表盘 → 数据导入 → 图谱 IDE → 混合检索
验证：核心数据流不中断
时间：约 15 分钟
```

#### 完整回归（每周或重大版本发布前）

```
执行：按优先级顺序执行所有 30 个模块测试
验证：
  1. Neo4j 和 PostgreSQL 两端数据一致
  2. 不同角色用户的菜单和数据访问控制正确
  3. JWT Token 认证在所有页面正常
时间：约 3-4 小时
```

#### 性能回归（每月）

```
- 1000+ 节点图谱的 IDE 加载 < 10s
- 1000+ 节点混合检索 < 5s
- 100+ Episode 的时序历史加载 < 5s
- 仪表盘统计数据加载 < 3s
```

#### 缺失功能验收（持续推进）

```
每次 sprint 完成后，对照附录 9 的缺失功能清单，
验证新实现的功能是否通过 E2E 测试。
```

---

**文档版本**: v1.1.0
**最后更新**: 2026-06-16
**适用系统版本**: OntoGraph 1.0.0-SNAPSHOT
**共包含模块**: 30 个（23 个功能模块 + 3 个新增页面 + 3 个跨模块流程 + 1 个缺失功能清单）
