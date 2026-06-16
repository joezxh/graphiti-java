# OntoGraph 浏览器自动化测试 Skill

> **适用版本**: OntoGraph 1.0.0  
> **测试环境**: 前端 `http://localhost:5173`, 后端 `http://localhost:9090`  
> **测试工具**: gstack `browse` (Puppeteer/Chromium)  
> **调用方式**: `$B goto <url>` / `$B snapshot` / `$B click @eN` / `$B fill <sel> <val>` / `$B screenshot <path>` 等  
> **会话**: 登录态在 `$B` 会话内持久（cookies 跨命令保持），不同模块共享同一会话

---

## 目录

- [通用测试框架](#通用测试框架)
- [1. 仪表盘](#1-仪表盘)
- [2. 图谱管理](#2-图谱管理)
  - [2.1 图谱列表](#21-图谱列表)
  - [2.2 图谱 IDE](#22-图谱-ide)
  - [2.3 时序历史](#23-时序历史)
  - [2.4 社区检测](#24-社区检测)
- [3. 数据管理](#3-数据管理)
  - [3.1 类定义管理](#31-类定义管理)
  - [3.2 属性管理](#32-属性管理)
  - [3.3 约束管理](#33-约束管理)
  - [3.4 实体管理](#34-实体管理)
  - [3.5 边管理](#35-边管理)
  - [3.6 社区管理](#36-社区管理)
  - [3.7 Episode 管理](#37-episode-管理)
  - [3.8 数据导入](#38-数据导入)
  - [3.9 数据导出](#39-数据导出)
  - [3.10 法律知识图谱](#310-法律知识图谱)
- [4. 工具](#4-工具)
  - [4.1 混合检索](#41-混合检索)
  - [4.2 自定义指令](#42-自定义指令)
  - [4.3 提示词管理](#43-提示词管理)
- [5. 系统管理](#5-系统管理)
  - [5.1 用户管理](#51-用户管理)
  - [5.2 角色管理](#52-角色管理)
  - [5.3 菜单管理](#53-菜单管理)
  - [5.4 系统配置](#54-系统配置)
  - [5.5 操作日志](#55-操作日志)
  - [5.6 系统监控](#56-系统监控)

---

## 通用测试框架

### 环境前置检查

在执行任何模块测试前，先验证服务可用性：

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
$B click "button[type='submit'], button:has-text('登录'), button:has-text('登录')"

# 4. 等待跳转到仪表盘并验证
$B wait --networkidle
$B url
$B is visible "[class*='dashboard'], [class*='Dashboard'], [class*='layout']"
$B screenshot /tmp/login-success.png
```

### 问题报告格式

每个模块测试完成后，按以下格式输出报告：

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

---

## 1. 仪表盘

### 测试提示词

```
## 仪表盘 (Dashboard) 测试

执行以下步骤，使用 browse 工具完成端到端验证：

### 步骤 1：登录
$B goto http://localhost:5173/login
$B snapshot -i
$B fill "[placeholder*='用户名' i], [placeholder*='username' i], input[type='text']" "admin"
$B fill "[placeholder*='密码' i], [placeholder*='password' i], input[type='password']" "admin123"
$B click "button[type='submit'], button:has-text('登录')"
$B wait --networkidle
$B url  # 期望: /dashboard 或包含 dashboard 的 URL
$B screenshot /tmp/dashboard-login.png

### 步骤 2：页面结构验证
$B goto http://localhost:5173/dashboard
$B wait --networkidle
$B snapshot -i
$B is visible "[class*='card'], [class*='stat'], [class*='chart'], .ant-card"  # 统计卡片区域
$B is visible "canvas, svg, [class*='echarts']"  # 图表区域
$B screenshot /tmp/dashboard-loaded.png

### 步骤 3：统计数据合理性检查
$B text
# 检查输出中是否包含数字（非 NaN、非空、非负）
# 期望看到：实体数量、关系数量、图谱数量、社区数量 等统计值

### 步骤 4：图表交互
$B click "[class*='card']:first-child, [class*='stat']:first-child"
$B wait --networkidle
$B url  # 期望跳转到对应详情页或图谱 IDE
$B screenshot /tmp/dashboard-card-click.png
$B back

### 步骤 5：响应式验证
$B viewport 375x812
$B wait --networkidle
$B snapshot
$B screenshot /tmp/dashboard-mobile.png
$B viewport 1280x720
$B wait --networkidle

### 步骤 6：页面刷新一致性
$B reload
$B wait --networkidle
$B snapshot
$B screenshot /tmp/dashboard-reload.png
$B text  # 对比刷新前后的统计数据是否一致

### 预期结果
- 登录后 URL 跳转到 /dashboard
- 统计卡片不少于 4 个，数据为有效数字
- ECharts 图表正确渲染（无空白、无报错）
- 点击卡片可跳转详情页
- 移动端布局正常，不发生错乱
- 刷新后数据保持一致

### 问题发现
发现问题时：
1. 执行 $B console --errors 捕获 JS 错误
2. 执行 $B network 查看是否有失败的 API 请求
3. 截图保存到 /tmp/dashboard-issue.png
4. 记录：问题现象 → 失败元素 selector → 推断定位文件

### 定位参考
- 页面空白/慢 → frontend/src/views/dashboard/index.vue, $B console 错误
- 数据 NaN → frontend/src/api/graph.ts 数据字段名不匹配后端
- 图表不渲染 → frontend/src/components/StatsCard.vue ECharts 配置
- 刷新数据不一致 → backend SearchResultCacheService 错误缓存统计接口
```

---

## 2. 图谱管理

### 2.1 图谱列表

### 测试提示词

```
## 图谱列表 (Graph List) 测试

### 步骤 1：登录（如尚未登录）
$B goto http://localhost:5173/login
$B snapshot -i
$B fill "[placeholder*='用户名' i], input[type='text']" "admin"
$B fill "[placeholder*='密码' i], input[type='password']" "admin123"
$B click "button[type='submit']"
$B wait --networkidle

### 步骤 2：进入图谱列表
$B goto http://localhost:5173/graph/list
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/graphlist-loaded.png

### 步骤 3：列表展示验证
$B text
# 期望看到图谱卡片，包含名称、描述、实体数量、关系数量
$B is visible "[class*='card'], [class*='graph']"
$B click "[class*='card']:first-child"
$B wait --networkidle
$B url  # 期望跳转到图谱 IDE
$B screenshot /tmp/graphlist-card-click.png
$B back
$B wait --networkidle

### 步骤 4：搜索过滤
$B goto http://localhost:5173/graph/list
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='搜索' i], [placeholder*='search' i], input[type='search']" "test"
$B wait 1s
$B text
$B screenshot /tmp/graphlist-search.png
# 清空搜索
$B fill "[placeholder*='搜索' i], [placeholder*='search' i], input[type='search']" ""
$B wait 1s

### 步骤 5：创建图谱
$B goto http://localhost:5173/graph/create
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='名称' i], [placeholder*='name' i], input[type='text']" "E2E-Test-Graph-$(date +%s)"
$B fill "[placeholder*='描述' i], [placeholder*='desc' i], textarea" "自动化测试创建的图谱"
$B screenshot /tmp/graphlist-create-form.png
$B click "button[type='submit'], button:has-text('创建')"
$B wait --networkidle
$B url  # 期望: /graph/list 或 /graph/ide/:id
$B screenshot /tmp/graphlist-create-result.png

### 步骤 6：克隆图谱
$B goto http://localhost:5173/graph/list
$B wait --networkidle
$B snapshot -i
$B click "[class*='card']:first-child [class*='clone'], button:has-text('克隆')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='名称' i], input[type='text']" "Cloned-Graph-$(date +%s)"
$B click "button[type='submit'], button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/graphlist-clone-result.png

### 步骤 7：删除图谱
$B goto http://localhost:5173/graph/list
$B wait --networkidle
$B snapshot -i
$B click "[class*='card']:last-child [class*='delete'], button:has-text('删除')"
$B wait --networkidle
$B snapshot
# 期望看到确认对话框
$B click "button:has-text('确认'), button:has-text('确定'), .ant-modal button:has-text('确定')"
$B wait --networkidle
$B screenshot /tmp/graphlist-delete-result.png

### 预期结果
- 图谱列表正确展示所有图谱卡片
- 搜索过滤实时响应
- 创建图谱成功，新图谱出现在列表
- 克隆图谱成功，数据独立
- 删除图谱需二次确认，删除后列表刷新

### 问题发现
$B console --errors
$B network
$B screenshot /tmp/graphlist-issue.png
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
$B screenshot /tmp/graphide-loaded.png

### 步骤 2：选择图谱并加载
$B click "[class*='select'], [class*='dropdown'], select, [class*='graph-selector']"
$B wait --networkidle
$B snapshot -i
$B click "[class*='option']:first-child, [class*='item']:first-child, [role='option']:first-child"
$B wait --networkidle
$B screenshot /tmp/graphide-graph-selected.png

### 步骤 3：图谱画布验证
$B wait --networkidle
$B is visible "canvas, svg, [class*='echarts']"
$B screenshot /tmp/graphide-canvas.png
# 期望看到节点（圆点）和边（连线）

### 步骤 4：节点交互
$B snapshot -i
$B click "canvas:first-child, svg:first-child"  # 点击画布空白处
$B wait 1s
$B snapshot -i
# 点击一个节点（坐标可能需要多次尝试）
$B click "canvas" --offset 200,200
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/graphide-node-click.png
# 期望看到节点详情面板弹出
$B is visible "[class*='detail'], [class*='panel'], [class*='modal']"

### 步骤 5：画布操作
$B click "button:has-text('放大'), [title*='放大'], [aria-label*='zoom in']"  # 缩放
$B wait 1s
$B screenshot /tmp/graphide-zoom.png
$B click "button:has-text('布局'), [title*='layout']"  # 布局切换
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/graphide-layout-change.png

### 步骤 6：搜索过滤
$B snapshot -i
$B fill "[placeholder*='搜索' i], [placeholder*='search' i], input[type='search']" "Entity"
$B wait 2s
$B snapshot
$B screenshot /tmp/graphide-search.png
# 期望匹配节点高亮或过滤显示
$B fill "[placeholder*='搜索' i], input" ""

### 步骤 7：添加节点
$B snapshot -i
$B click "button:has-text('添加'), button:has-text('新建'), [title*='添加']"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='名称' i], [placeholder*='name' i]" "E2E-Test-Node"
$B screenshot /tmp/graphide-add-node-form.png
$B click "button[type='submit'], button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/graphide-add-node-result.png

### 预期结果
- 图谱画布正确渲染节点和边
- 点击节点弹出详情面板
- 缩放和布局切换正常
- 搜索过滤实时响应
- 添加节点成功，新节点出现在画布

### 问题发现
$B console --errors
$B click "canvas" --offset 300,300
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/graphide-issue.png
# 检查 backend: frontend/src/views/graph/ide.vue, GraphCanvas.vue
# backend: GraphVisualizationService, GraphNeo4jService
```

---

### 2.3 时序历史

### 测试提示词

```
## 时序历史 (Temporal History) 测试

### 步骤 1：进入时序历史页面
$B goto http://localhost:5173/graph/temporal
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/temporal-loaded.png

### 步骤 2：选择图谱
$B click "[class*='select'], [class*='dropdown'], select"
$B wait --networkidle
$B snapshot -i
$B click "[class*='option']:first-child, [role='option']:first-child"
$B wait --networkidle
$B screenshot /tmp/temporal-graph-selected.png

### 步骤 3：时间轴视图
$B wait --networkidle
$B snapshot -i
$B is visible "[class*='timeline'], [class*='time'], [class*='episode']"
$B screenshot /tmp/temporal-timeline.png
# 期望看到 Episode 节点按时间排列

### 步骤 4：历史快照查询
$B snapshot -i
$B click "button:has-text('时间'), [placeholder*='时间' i], [class*='date-picker']"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/temporal-datepicker.png
# 选择一个过去日期
$B click "[class*='cell']:first-child, [class*='date']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/temporal-snapshot.png

### 步骤 5：Saga 链验证
$B click "[class*='episode']:first-child, [class*='node']:first-child"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/temporal-episode-detail.png
$B is visible "[class*='next'], [class*='chain'], [class*='relation']"
# 期望看到 NEXT_EPISODE 关系链

### 步骤 6：刷新验证
$B reload
$B wait --networkidle
$B snapshot
$B screenshot /tmp/temporal-reload.png
$B text  # 对比刷新前后时间轴数据

### 预期结果
- 时间轴正确展示所有 Episode 的起止时间
- 历史快照切换后，图谱状态反映过去时间点
- Saga 链完整展示

### 问题发现
$B console --errors
$B screenshot /tmp/temporal-issue.png
# backend: TemporalService, SagaService
```

---

### 2.4 社区检测

### 测试提示词

```
## 社区检测 (Community Detection) 测试

### 步骤 1：进入社区检测页面
$B goto http://localhost:5173/data/communities
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/communities-loaded.png

### 步骤 2：选择图谱
$B click "[class*='select'], [class*='dropdown'], select"
$B wait --networkidle
$B snapshot -i
$B click "[class*='option']:first-child, [role='option']:first-child"
$B wait --networkidle

### 步骤 3：触发社区构建
$B snapshot -i
$B click "button:has-text('构建'), button:has-text('检测'), button:has-text('Build')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/communities-building.png
# 等待构建完成
$B wait 5s
$B snapshot
$B screenshot /tmp/communities-built.png

### 步骤 4：社区列表验证
$B text
$B is visible "[class*='community'], [class*='card']"
$B screenshot /tmp/communities-list.png

### 步骤 5：社区详情
$B snapshot -i
$B click "[class*='card']:first-child, [class*='community']:first-child"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/communities-detail.png
# 期望看到社区内实体列表

### 步骤 6：社区类型管理
$B goto http://localhost:5173/data/communities
$B wait --networkidle
$B snapshot -i
$B click "[class*='type'], [class*='manage']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/communities-types.png

### 步骤 7：LLM 摘要（如已配置）
$B snapshot -i
$B click "button:has-text('摘要'), button:has-text('生成'), button:has-text('Summary')"
$B wait --networkidle
$B wait 5s
$B snapshot
$B screenshot /tmp/communities-summary.png

### 预期结果
- 社区构建有进度提示
- 构建完成后社区列表更新
- 点击社区显示详情和实体列表
- LLM 摘要生成后显示描述文本

### 问题发现
$B console --errors
$B screenshot /tmp/communities-issue.png
# backend: CommunityService, LlmClientService
```

---

## 3. 数据管理

### 3.1 类定义管理

### 测试提示词

```
## 类定义管理 (Class Management) 测试

### 步骤 1：进入类管理页面
$B goto http://localhost:5173/data/classes
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/classes-loaded.png

### 步骤 2：选择图谱
$B click "[class*='select'], [class*='dropdown'], select"
$B wait --networkidle
$B click "[class*='option']:first-child, [role='option']:first-child"
$B wait --networkidle

### 步骤 3：类列表验证
$B text
# 期望看到本体类列表（Entity, Person, Organization 等）
$B is visible "[class*='table'], [class*='list'], [class*='tree']"
$B screenshot /tmp/classes-list.png

### 步骤 4：层级树视图
$B snapshot -i
$B click "button:has-text('树'), button:has-text('Tree'), [class*='tree']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/classes-tree.png
# 期望看到继承层级结构

### 步骤 5：创建类
$B snapshot -i
$B click "button:has-text('新建'), button:has-text('添加'), button:has-text('Create')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='类名' i], [placeholder*='name' i]" "E2E-Test-Class-$(date +%s)"
$B fill "[placeholder*='描述' i], [placeholder*='desc' i]" "自动化测试创建的类"
$B screenshot /tmp/classes-create-form.png
$B click "button[type='submit'], button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/classes-create-result.png
$B text  # 验证新类出现在列表

### 步骤 6：编辑类
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑'), [class*='row']:first-child [class*='edit']"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='描述' i]" "修改后的描述"
$B click "button:has-text('保存'), button[type='submit']"
$B wait --networkidle
$B screenshot /tmp/classes-edit-result.png

### 步骤 7：循环继承检测
$B snapshot -i
$B click "button:has-text('新建')"
$B wait --networkidle
$B snapshot -i
# 尝试设置循环继承（子类继承已有子类）
$B fill "[placeholder*='类名' i]" "Cycle-Test-Class"
$B screenshot /tmp/classes-cycle-test.png
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/classes-cycle-result.png
# 期望看到错误提示

### 步骤 8：删除类
$B snapshot -i
$B click "[class*='row']:last-child button:has-text('删除')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认'), button:has-text('确定')"
$B wait --networkidle
$B screenshot /tmp/classes-delete-result.png

### 预期结果
- 类列表展示所有本体类
- 树形视图正确展示继承关系
- 创建类成功，新类即时出现在列表
- 循环继承被 6 层验证引擎拦截

### 问题发现
$B console --errors
$B screenshot /tmp/classes-issue.png
# backend: OntologyClassService, OntologyValidationService (Layer 1)
```

---

### 3.2 属性管理

### 测试提示词

```
## 属性管理 (Property Management) 测试

### 步骤 1：进入属性管理页面
$B goto http://localhost:5173/data/properties
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/properties-loaded.png

### 步骤 2：选择图谱
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child, [role='option']:first-child"
$B wait --networkidle

### 步骤 3：属性列表验证
$B text
$B is visible "[class*='table'], [class*='list']"
$B screenshot /tmp/properties-list.png

### 步骤 4：创建属性
$B snapshot -i
$B click "button:has-text('新建'), button:has-text('添加')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='属性名' i], [placeholder*='name' i]" "testProperty"
$B screenshot /tmp/properties-create-form.png
$B click "button[type='submit'], button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/properties-create-result.png
$B text

### 步骤 5：数据类型验证
$B snapshot -i
$B click "button:has-text('新建')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='属性名' i]" "stringProp"
$B click "[class*='type'], [class*='select']"  # 选择数据类型
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/properties-datatype.png
$B click "button:has-text('取消')"

### 步骤 6：Domain/Range 约束设置
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B click "button:has-text('约束'), button:has-text('Constraint')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/properties-constraint.png

### 步骤 7：删除属性
$B snapshot -i
$B click "[class*='row']:last-child button:has-text('删除')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/properties-delete-result.png

### 预期结果
- 属性列表正确展示
- 创建属性成功，新属性出现在列表
- Domain/Range 约束可设置和编辑
- 数据类型在下拉选项中可见

### 问题发现
$B console --errors
$B screenshot /tmp/properties-issue.png
# backend: OntologyPropertyService, OntologyValidationService (Layer 3)
```

---

### 3.3 约束管理

### 测试提示词

```
## 约束管理 (Constraint Management) 测试

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
$B is visible "[class*='table'], [class*='list']"
$B screenshot /tmp/constraints-list.png

### 步骤 4：创建约束
$B snapshot -i
$B click "button:has-text('新建'), button:has-text('添加')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='属性' i], [class*='select']" "Person.email"  # 选择已有属性
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/constraints-create-form.png
$B click "button[type='submit'], button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/constraints-create-result.png

### 步骤 5：批量验证
$B snapshot -i
$B click "button:has-text('批量验证'), button:has-text('Validate')"
$B wait --networkidle
$B wait 3s
$B snapshot
$B screenshot /tmp/constraints-validate-result.png
# 期望看到验证报告（通过/失败数量）

### 步骤 6：一致性检查
$B snapshot -i
$B click "button:has-text('一致性'), button:has-text('Consistency')"
$B wait --networkidle
$B wait 3s
$B snapshot
$B screenshot /tmp/constraints-consistency.png

### 步骤 7：版本历史
$B snapshot -i
$B click "button:has-text('历史'), button:has-text('Version')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/constraints-history.png

### 预期结果
- 约束列表展示所有约束
- 批量验证生成报告
- 一致性检查返回结果
- 版本历史展示变更记录

### 问题发现
$B console --errors
$B screenshot /tmp/constraints-issue.png
# backend: OntologyValidationService
```

---

### 3.4 实体管理

### 测试提示词

```
## 实体管理 (Entity Management) 测试

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
$B is visible "[class*='table'], [class*='list'], [class*='data-table']"
$B screenshot /tmp/entities-list.png

### 步骤 4：搜索过滤
$B fill "[placeholder*='搜索' i], [placeholder*='search' i]" "Entity"
$B wait 1s
$B text
$B screenshot /tmp/entities-search.png
$B fill "[placeholder*='搜索' i]" ""

### 步骤 5：创建实体
$B snapshot -i
$B click "button:has-text('新建'), button:has-text('添加'), button:has-text('Create')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/entities-create-form.png
$B fill "[placeholder*='名称' i], [placeholder*='name' i]" "E2E-Test-Entity-$(date +%s)"
$B screenshot /tmp/entities-create-filled.png
$B click "button[type='submit'], button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/entities-create-result.png
$B text  # 验证新实体在列表中

### 步骤 6：编辑实体
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑'), [class*='row']:first-child [class*='edit']"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/entities-edit-form.png
$B click "button:has-text('保存')"
$B wait --networkidle
$B screenshot /tmp/entities-edit-result.png

### 步骤 7：本体验证（违规测试）
$B snapshot -i
$B click "button:has-text('新建')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='名称' i]" "Validation-Test"
$B fill "[placeholder*='age' i]" "not-a-number"  # 故意填入错误类型
$B screenshot /tmp/entities-validation-test.png
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/entities-validation-result.png
# 期望看到验证错误提示

### 步骤 8：批量选择与删除
$B snapshot -i
$B click "[class*='checkbox']:first-child"  # 选择第一个实体
$B wait --networkidle
$B click "button:has-text('删除'), button:has-text('批量删除')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/entities-delete-result.png

### 步骤 9：去重
$B snapshot -i
$B click "button:has-text('去重'), button:has-text('Dedupe')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/entities-dedupe.png

### 预期结果
- 实体列表正确展示
- 搜索过滤实时响应
- 创建/编辑实体成功
- 验证违规被正确拦截并显示错误
- 批量删除需要确认
- 去重功能识别相似实体

### 问题发现
$B console --errors
$B network
$B screenshot /tmp/entities-issue.png
# backend: NodeService, EntityDedupService, OntologyValidationService
```

---

### 3.5 边管理

### 测试提示词

```
## 边管理 (Edge Management) 测试

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
$B is visible "[class*='table'], [class*='list']"
$B screenshot /tmp/edges-list.png

### 步骤 4：按类型过滤
$B snapshot -i
$B click "[class*='filter'], [class*='type']"
$B wait --networkidle
$B snapshot -i
$B click "[class*='option']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/edges-filtered.png

### 步骤 5：创建边
$B snapshot -i
$B click "button:has-text('新建'), button:has-text('添加')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/edges-create-form.png
$B fill "[placeholder*='描述' i], [placeholder*='fact' i]" "E2E-Test-Edge-Relation"
$B screenshot /tmp/edges-create-filled.png
$B click "button[type='submit'], button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/edges-create-result.png
$B text

### 步骤 6：编辑边
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='描述' i]" "Modified description"
$B click "button:has-text('保存')"
$B wait --networkidle
$B screenshot /tmp/edges-edit-result.png

### 步骤 7：时序边设置
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='valid' i], [class*='date']" "2024-01-01"
$B fill "[placeholder*='invalid' i], [class*='date']" "2025-01-01"
$B click "button:has-text('保存')"
$B wait --networkidle
$B screenshot /tmp/edges-temporal-result.png

### 步骤 8：删除边
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('删除')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/edges-delete-result.png

### 预期结果
- 边列表正确展示源→目标、关系类型、事实描述
- 过滤功能正常
- 创建/编辑/删除边成功
- 时序字段可设置

### 问题发现
$B console --errors
$B screenshot /tmp/edges-issue.png
# backend: EdgeService
```

---

### 3.6 社区管理

### 测试提示词

```
## 社区管理 (Community Management) 测试

### 步骤 1：进入社区管理页面
$B goto http://localhost:5173/data/communities
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/community-mgmt-loaded.png

### 步骤 2：选择图谱
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:first-child"
$B wait --networkidle

### 步骤 3：社区列表
$B text
$B is visible "[class*='card'], [class*='community']"
$B screenshot /tmp/community-mgmt-list.png

### 步骤 4：社区详情
$B snapshot -i
$B click "[class*='card']:first-child, [class*='community']:first-child"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/community-mgmt-detail.png
# 期望看到社区内实体列表

### 步骤 5：修改社区类型
$B snapshot -i
$B click "button:has-text('编辑'), button:has-text('修改')"
$B wait --networkidle
$B snapshot -i
$B click "[class*='type'], [class*='select']"  # 选择新的社区类型
$B wait --networkidle
$B snapshot
$B click "button:has-text('保存')"
$B wait --networkidle
$B screenshot /tmp/community-mgmt-type-result.png

### 步骤 6：法律领域配置
$B goto http://localhost:5173/data/communities
$B wait --networkidle
$B snapshot -i
$B click "[class*='domain'], button:has-text('领域')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/community-mgmt-domain.png

### 步骤 7：社区与 Episode 关联
$B goto http://localhost:5173/data/community-episode
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/community-episode-loaded.png

### 步骤 8：删除社区
$B goto http://localhost:5173/data/communities
$B wait --networkidle
$B snapshot -i
$B click "[class*='card']:last-child button:has-text('删除')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/community-mgmt-delete-result.png

### 预期结果
- 社区列表展示所有社区
- 点击社区显示详情和实体列表
- 修改社区类型成功
- 删除社区保留社区内实体节点

### 问题发现
$B console --errors
$B screenshot /tmp/community-mgmt-issue.png
# backend: CommunityService
```

---

### 3.7 Episode 管理

### 测试提示词

```
## Episode 管理 (Episode Management) 测试

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
$B is visible "[class*='table'], [class*='list']"
$B screenshot /tmp/episodes-list.png

### 步骤 4：创建 Episode
$B snapshot -i
$B click "button:has-text('新建'), button:has-text('添加')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='名称' i], [placeholder*='name' i]" "E2E-Test-Episode"
$B fill "[placeholder*='内容' i], [placeholder*='content' i], textarea" "这是自动化测试创建的 Episode 内容"
$B screenshot /tmp/episodes-create-form.png
$B click "button[type='submit'], button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/episodes-create-result.png
$B text

### 步骤 5：Episode 类型管理
$B snapshot -i
$B click "button:has-text('类型'), [class*='type-tab']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/episodes-types.png
$B click "button:has-text('新建类型')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='类型名' i]" "meeting_record"
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/episodes-type-created.png

### 步骤 6：Episode 详情
$B snapshot -i
$B click "[class*='row']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/episodes-detail.png

### 步骤 7：Saga 链编排
$B snapshot -i
$B click "button:has-text('链接'), button:has-text('链接下一集')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/episodes-saga-link.png
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/episodes-saga-result.png

### 步骤 8：删除 Episode
$B snapshot -i
$B click "button:has-text('删除')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/episodes-delete-result.png

### 预期结果
- Episode 列表正确展示
- 创建 Episode 成功
- Episode 类型可添加和管理
- Saga 链可编排（NEXT_EPISODE 关系）

### 问题发现
$B console --errors
$B screenshot /tmp/episodes-issue.png
# backend: EpisodeService, SagaService
```

---

### 3.8 数据导入

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

### 步骤 3：文本导入
$B snapshot -i
$B fill "[placeholder*='名称' i]" "E2E-Test-Import-$(date +%s)"
$B fill "textarea" "苹果公司由史蒂夫·乔布斯和史蒂夫·沃兹尼亚克于1976年在加利福尼亚州库比蒂诺创立。乔布斯后来于1985年离开苹果，并在1997年回归带领公司走向辉煌。"
$B screenshot /tmp/import-text-filled.png
$B click "button:has-text('导入'), button:has-text('提交')"
$B wait --networkidle
$B wait 5s
$B snapshot
$B screenshot /tmp/import-result.png
$B text

### 步骤 4：任务列表
$B snapshot -i
$B click "button:has-text('任务'), [class*='task']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/import-tasks.png
# 期望看到导入任务状态：pending → processing → completed

### 步骤 5：验证导入结果
$B goto http://localhost:5173/data/entities
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/import-verified-entities.png
$B text  # 期望看到 "苹果公司"、"乔布斯"、"沃兹尼亚克"、"库比蒂诺" 等实体

$B goto http://localhost:5173/data/edges
$B wait --networkidle
$B snapshot
$B screenshot /tmp/import-verified-edges.png
$B text  # 期望看到关系数据

### 步骤 6：文件导入
$B goto http://localhost:5173/data/import
$B wait --networkidle
$B snapshot -i
$B click "button:has-text('文件'), [class*='upload']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/import-file-upload.png
# 注意：browse 的 upload 命令需要文件路径
# 如果需要测试文件上传，跳过此步骤或提供实际文件路径

### 步骤 7：本体约束验证
$B goto http://localhost:5173/data/import
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='名称' i]" "Validation-Test"
$B fill "textarea" "测试内容 with invalid data that violates ontology"
$B click "button:has-text('导入')"
$B wait --networkidle
$B wait 5s
$B snapshot
$B screenshot /tmp/import-validation.png
# 期望看到部分导入被拒绝的提示

### 预期结果
- 文本导入成功，实体和关系被正确抽取
- 导入任务有状态跟踪
- 导入后在实体/边管理页面可验证数据

### 问题发现
$B console --errors
$B network
$B screenshot /tmp/import-issue.png
# backend: DataImportService, EntityExtractorService, LlmClientService
```

---

### 3.9 数据导出

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

### 步骤 3：JSON 导出
$B snapshot -i
$B click "[class*='format'], [class*='type']"  # 选择格式
$B wait --networkidle
$B snapshot -i
$B click "[class*='option']:has-text('JSON')"  # 选择 JSON
$B wait --networkidle
$B snapshot
$B screenshot /tmp/export-json-selected.png
$B click "button:has-text('导出'), button:has-text('下载')"
$B wait --networkidle
$B wait 3s
$B snapshot
$B screenshot /tmp/export-json-result.png

### 步骤 4：CSV 导出
$B snapshot -i
$B click "[class*='format']"
$B wait --networkidle
$B click "[class*='option']:has-text('CSV')"
$B wait --networkidle
$B click "button:has-text('导出')"
$B wait --networkidle
$B wait 3s
$B snapshot
$B screenshot /tmp/export-csv-result.png

### 步骤 5：导出范围控制
$B snapshot -i
$B click "button:has-text('范围'), [class*='scope']"
$B wait --networkidle
$B snapshot -i
$B click "[class*='option']:has-text('Entity')"  # 选择仅导出实体
$B wait --networkidle
$B click "button:has-text('导出')"
$B wait --networkidle
$B wait 3s
$B screenshot /tmp/export-scope-result.png

### 步骤 6：导出历史
$B snapshot -i
$B click "button:has-text('历史'), [class*='history']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/export-history.png

### 预期结果
- JSON 导出生成可下载文件
- CSV 导出生成可下载文件
- 导出范围控制正常（Entity / Edge / All）
- 导出历史展示历史记录

### 问题发现
$B console --errors
$B screenshot /tmp/export-issue.png
# backend: GraphitiService (exportGraph)
```

---

### 3.10 法律知识图谱

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

### 步骤 3：法律条例导入
$B snapshot -i
$B click "button:has-text('导入条例'), button:has-text('导入'), [class*='upload']"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/legal-kg-import-form.png
$B fill "[placeholder*='名称' i]" "民商事条例-E2E"
$B fill "textarea" "《商事调解条例》第一条 为了规范商事调解活动，保障当事人合法权益，促进社会和谐稳定，制定本条例。"
$B screenshot /tmp/legal-kg-provision-filled.png
$B click "button:has-text('导入')"
$B wait --networkidle
$B wait 8s
$B snapshot
$B screenshot /tmp/legal-kg-import-result.png
$B text

### 步骤 4：法律实体管理
$B snapshot -i
$B click "button:has-text('法条'), [class*='tab']:has-text('法条')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/legal-kg-provisions.png

$B click "button:has-text('法院'), [class*='tab']:has-text('法院')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/legal-kg-courts.png

$B click "button:has-text('当事人'), [class*='tab']:has-text('当事人')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/legal-kg-parties.png

$B click "button:has-text('案例'), [class*='tab']:has-text('案例')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/legal-kg-cases.png

### 步骤 5：案例详情与关联图谱
$B snapshot -i
$B click "[class*='row']:first-child, [class*='card']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/legal-kg-case-detail.png
$B is visible "canvas, svg, [class*='graph']"  # 期望看到关联图谱

### 步骤 6：法律领域配置
$B snapshot -i
$B click "button:has-text('领域'), button:has-text('配置')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/legal-kg-domain-config.png
$B click "[class*='select'], select"
$B wait --networkidle
$B click "[class*='option']:has-text('民事')"
$B click "button:has-text('保存')"
$B wait --networkidle
$B screenshot /tmp/legal-kg-domain-result.png

### 步骤 7：级联编辑
$B goto http://localhost:5173/legal-kg
$B wait --networkidle
$B snapshot -i
$B click "button:has-text('级联'), button:has-text('批量创建')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/legal-kg-cascade-form.png
$B fill "[placeholder*='案例' i]" "E2E-Test-Case"
$B fill "[placeholder*='当事人' i]" "张三"
$B screenshot /tmp/legal-kg-cascade-filled.png
$B click "button:has-text('提交'), button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/legal-kg-cascade-result.png

### 步骤 8：法条关联图谱
$B snapshot -i
$B click "button:has-text('法条')"
$B wait --networkidle
$B click "[class*='row']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/legal-kg-provision-graph.png

### 预期结果
- 法律条例导入成功，生成 LegalProvision 等实体
- 各 Tab 分类展示不同类型法律实体
- 案例详情展示关联图谱
- 领域配置可保存
- 级联编辑可批量创建关联实体

### 问题发现
$B console --errors
$B screenshot /tmp/legal-kg-issue.png
# backend: LegalImportService, LegalExtractService, CascadeEditService
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
$B click "[class*='mode'], [class*='type']"  # 点击模式选择
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/search-mode-select.png

$B click "[class*='option']:has-text('BM25')"  # 仅全文
$B wait --networkidle
$B fill "[placeholder*='搜索' i]" "苹果"
$B wait 2s
$B snapshot
$B screenshot /tmp/search-bm25.png

$B click "[class*='mode']"  # 切换到混合
$B wait --networkidle
$B click "[class*='option']:has-text('混合')"
$B wait --networkidle
$B fill "[placeholder*='搜索' i]" "苹果"
$B wait 3s
$B snapshot
$B screenshot /tmp/search-hybrid.png

### 步骤 5：搜索结果验证
$B text
$B is visible "[class*='result'], [class*='item'], [class*='card']"
$B screenshot /tmp/search-result-detail.png

### 步骤 6：无结果场景
$B fill "[placeholder*='搜索' i]" "xyznonexistent12345"
$B wait 2s
$B snapshot
$B screenshot /tmp/search-no-result.png
# 期望看到"未找到结果"提示

### 步骤 7：搜索历史
$B snapshot -i
$B click "button:has-text('历史'), [class*='history']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/search-history.png
# 期望看到搜索历史列表

### 步骤 8：缓存验证（相同查询）
$B fill "[placeholder*='搜索' i]" "苹果"
$B wait 1s  # 第一次（可能未缓存）
$B fill "[placeholder*='搜索' i]" "苹果"
$B wait 1s  # 第二次（应命中缓存，更快）
$B snapshot
$B screenshot /tmp/search-cache-test.png

### 预期结果
- 基础检索返回相关结果
- 不同搜索模式（BM25/混合）返回不同排序结果
- 无结果时显示友好提示
- 搜索历史记录历史查询

### 问题发现
$B console --errors
$B network
$B screenshot /tmp/search-issue.png
# backend: SearchService, SearchPipelineService, SearchResultCacheService
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
$B is visible "[class*='list'], [class*='table']"
$B screenshot /tmp/instructions-list.png

### 步骤 3：创建指令
$B snapshot -i
$B click "button:has-text('新建'), button:has-text('添加')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='名称' i]" "E2E-Test-Instruction"
$B fill "textarea, [class*='editor']" "在抽取实体时，优先识别公司、组织等商业实体"
$B screenshot /tmp/instructions-create-form.png
$B click "button[type='submit'], button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/instructions-create-result.png
$B text

### 步骤 4：启用/禁用
$B snapshot -i
$B click "[class*='toggle'], [class*='switch']"  # 切换启用状态
$B wait --networkidle
$B snapshot
$B screenshot /tmp/instructions-toggle.png

### 步骤 5：编辑指令
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B fill "textarea" "修改后的指令内容"
$B click "button:has-text('保存')"
$B wait --networkidle
$B screenshot /tmp/instructions-edit-result.png

### 步骤 6：优先级调整
$B snapshot -i
$B click "button:has-text('上移'), button:has-text('下移'), button:has-text('排序')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/instructions-reorder.png

### 步骤 7：删除指令
$B snapshot -i
$B click "[class*='row']:last-child button:has-text('删除')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/instructions-delete-result.png

### 预期结果
- 指令列表正确展示
- 创建/编辑/删除指令成功
- 启用/禁用状态切换正常
- 优先级可调整

### 问题发现
$B console --errors
$B screenshot /tmp/instructions-issue.png
# backend: CustomInstructionService
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
$B is visible "[class*='card'], [class*='list'], [class*='table']"
$B screenshot /tmp/prompt-list.png

### 步骤 3：创建模板
$B snapshot -i
$B click "button:has-text('新建'), button:has-text('添加')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='编码' i]" "e2e_prompt_$(date +%s)"
$B fill "[placeholder*='名称' i]" "E2E-Test-Template"
$B fill "[placeholder*='System'], [class*='system']" "你是一个实体抽取助手。"
$B fill "[placeholder*='User'], [class*='user']" "请从以下文本中抽取 {entity_type} 实体：{text}"
$B screenshot /tmp/prompt-create-form.png
$B click "button[type='submit'], button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/prompt-create-result.png
$B text

### 步骤 4：模板测试
$B snapshot -i
$B click "button:has-text('测试')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='entity' i]" "Organization"
$B fill "[placeholder*='text' i]" "苹果公司是一家美国科技公司。"
$B screenshot /tmp/prompt-test-form.png
$B click "button:has-text('执行'), button:has-text('测试')"
$B wait --networkidle
$B wait 5s
$B snapshot
$B screenshot /tmp/prompt-test-result.png
$B text  # 期望看到 LLM 返回结果

### 步骤 5：版本管理
$B goto http://localhost:5173/prompt
$B wait --networkidle
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='System']" "修改后的 System Prompt"
$B click "button:has-text('保存')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('历史'), button:has-text('版本')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/prompt-version-history.png

### 步骤 6：删除模板
$B snapshot -i
$B click "[class*='row']:last-child button:has-text('删除')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/prompt-delete-result.png

### 预期结果
- 模板列表展示所有提示词模板
- 创建模板成功
- 模板测试返回 LLM 结果
- 版本历史展示变更记录

### 问题发现
$B console --errors
$B screenshot /tmp/prompt-issue.png
# backend: PromptTemplateService, PromptTestController, LlmClientService
```

---

## 5. 系统管理

### 5.1 用户管理

### 测试提示词

```
## 用户管理 (User Management) 测试

### 步骤 1：进入用户管理页面
$B goto http://localhost:5173/system/user
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/users-loaded.png

### 步骤 2：用户列表
$B text
$B is visible "[class*='table'], [class*='list']"
$B screenshot /tmp/users-list.png

### 步骤 3：新建用户
$B snapshot -i
$B click "button:has-text('新建'), button:has-text('添加')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='用户名' i]" "e2e_test_user_$(date +%s)"
$B fill "[placeholder*='密码' i]" "Test123456"
$B fill "[placeholder*='昵称' i]" "E2E 测试用户"
$B fill "[placeholder*='邮箱' i]" "e2e@test.com"
$B screenshot /tmp/users-create-form.png
$B click "button[type='submit'], button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/users-create-result.png
$B text

### 步骤 4：唯一性验证
$B snapshot -i
$B click "button:has-text('新建')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='用户名' i]" "admin"  # 使用已存在的用户名
$B fill "[placeholder*='密码' i]" "Test123456"
$B click "button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/users-duplicate-check.png
# 期望看到"用户名已存在"错误提示

### 步骤 5：编辑用户
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='昵称' i]" "E2E-修改后的昵称"
$B click "button:has-text('保存')"
$B wait --networkidle
$B screenshot /tmp/users-edit-result.png

### 步骤 6：分配角色
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B click "[class*='role'], [class*='select']"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/users-role-select.png
$B click "[class*='option']:first-child"
$B wait --networkidle
$B click "button:has-text('保存')"
$B wait --networkidle
$B screenshot /tmp/users-role-result.png

### 步骤 7：禁用用户
$B snapshot -i
$B click "[class*='row']:first-child [class*='toggle'], [class*='switch']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/users-disable-result.png

### 步骤 8：删除用户
$B snapshot -i
$B click "[class*='row']:last-child button:has-text('删除')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/users-delete-result.png

### 预期结果
- 用户列表正确展示
- 新建用户成功
- 用户名唯一性被校验
- 编辑/禁用/删除用户成功

### 问题发现
$B console --errors
$B screenshot /tmp/users-issue.png
# backend: UserService, UserMapper
```

---

### 5.2 角色管理

### 测试提示词

```
## 角色管理 (Role Management) 测试

### 步骤 1：进入角色管理页面
$B goto http://localhost:5173/system/role
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/roles-loaded.png

### 步骤 2：角色列表
$B text
$B is visible "[class*='table'], [class*='list']"
$B screenshot /tmp/roles-list.png

### 步骤 3：创建角色
$B snapshot -i
$B click "button:has-text('新建'), button:has-text('添加')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='名称' i]" "E2E-Role"
$B fill "[placeholder*='编码' i]" "ROLE_E2E_TEST"
$B fill "[placeholder*='备注' i]" "自动化测试创建的角色"
$B screenshot /tmp/roles-create-form.png
$B click "button[type='submit'], button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/roles-create-result.png

### 步骤 4：分配权限
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/roles-permission-form.png
$B click "[class*='tree'] [class*='checkbox']:first-child"  # 选择第一个权限
$B wait --networkidle
$B snapshot
$B click "button:has-text('保存')"
$B wait --networkidle
$B screenshot /tmp/roles-permission-result.png

### 步骤 5：删除保护验证
$B snapshot -i
$B click "[class*='row']:last-child button:has-text('删除')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/roles-delete-check.png
# 期望看到"该角色下有用户"的保护提示或直接被阻止

### 步骤 6：删除空角色
$B snapshot -i
$B click "[class*='row']:last-child button:has-text('删除')"
$B wait --networkidle
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/roles-delete-result.png

### 预期结果
- 角色列表正确展示
- 创建角色成功
- 权限树可选择
- 有用户的角色被保护不可删除

### 问题发现
$B console --errors
$B screenshot /tmp/roles-issue.png
# backend: RoleService, MenuService
```

---

### 5.3 菜单管理

### 测试提示词

```
## 菜单管理 (Menu Management) 测试

### 步骤 1：进入菜单管理页面
$B goto http://localhost:5173/system/menu
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/menus-loaded.png

### 步骤 2：菜单树
$B text
$B is visible "[class*='tree'], [class*='menu-tree']"
$B screenshot /tmp/menus-tree.png

### 步骤 3：创建菜单
$B snapshot -i
$B click "button:has-text('新建'), button:has-text('添加')"
$B wait --networkidle
$B snapshot -i
$B fill "[placeholder*='名称' i]" "E2E-Test-Menu"
$B fill "[placeholder*='路由' i]" "/e2e-test"
$B fill "[placeholder*='路径' i]" "e2eTest/index.vue"
$B screenshot /tmp/menus-create-form.png
$B click "button[type='submit'], button:has-text('确认')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/menus-create-result.png

### 步骤 4：菜单排序
$B snapshot -i
$B click "button:has-text('排序'), button:has-text('拖拽')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/menus-sort.png

### 步骤 5：菜单图标
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/menus-icon-edit.png
$B click "button:has-text('保存')"
$B wait --networkidle
$B screenshot /tmp/menus-icon-result.png

### 步骤 6：删除菜单
$B snapshot -i
$B click "[class*='row']:last-child button:has-text('删除')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/menus-delete-result.png

### 预期结果
- 菜单树正确展示层级结构
- 创建菜单成功
- 菜单可排序和编辑
- 删除菜单后从权限树中移除

### 问题发现
$B console --errors
$B screenshot /tmp/menus-issue.png
# backend: MenuService
```

---

### 5.4 系统配置

### 测试提示词

```
## 系统配置 (System Config) 测试

### 步骤 1：进入系统配置页面
$B goto http://localhost:5173/system/config
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/config-loaded.png

### 步骤 2：配置列表
$B text
$B is visible "[class*='table'], [class*='list'], [class*='form']"
$B screenshot /tmp/config-list.png

### 步骤 3：编辑配置
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/config-edit-form.png
$B click "button:has-text('保存')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/config-edit-result.png

### 步骤 4：AI 配置（查看）
$B snapshot -i
$B click "button:has-text('AI'), [class*='tab']:has-text('AI')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/config-ai.png
# 期望看到 LLM 提供商配置（不修改以避免破坏环境）

### 步骤 5：配置验证
$B snapshot -i
$B click "[class*='row']:first-child button:has-text('编辑')"
$B wait --networkidle
$B snapshot -i
$B fill "[class*='input']" "invalid-value-not-a-number"
$B click "button:has-text('保存')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/config-validation.png
# 期望看到格式验证错误

### 步骤 6：重置配置
$B snapshot -i
$B click "button:has-text('重置'), button:has-text('恢复默认')"
$B wait --networkidle
$B snapshot
$B click "button:has-text('确认')"
$B wait --networkidle
$B screenshot /tmp/config-reset-result.png

### 预期结果
- 配置列表按分组展示
- 编辑配置后保存成功
- 格式验证在保存前拦截无效值
- 重置配置恢复默认值

### 问题发现
$B console --errors
$B screenshot /tmp/config-issue.png
# backend: SystemConfigService
```

---

### 5.5 操作日志

### 测试提示词

```
## 操作日志 (Operation Log) 测试

### 步骤 1：进入操作日志页面
$B goto http://localhost:5173/system/log
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/logs-loaded.png

### 步骤 2：日志列表
$B text
$B is visible "[class*='table'], [class*='list']"
$B screenshot /tmp/logs-list.png
# 期望看到操作记录（操作用户、操作类型、操作对象、时间、IP）

### 步骤 3：按用户过滤
$B snapshot -i
$B click "[class*='filter'], [class*='select']"
$B wait --networkidle
$B snapshot -i
$B click "[class*='option']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/logs-filtered.png

### 步骤 4：时间范围过滤
$B snapshot -i
$B click "[class*='date'], [class*='range']"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/logs-date-range.png
$B click "[class*='cell']:first-child"
$B wait --networkidle
$B click "[class*='cell']:last-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/logs-date-filtered.png

### 步骤 5：日志详情
$B snapshot -i
$B click "[class*='row']:first-child"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/logs-detail.png
# 期望看到请求参数、返回结果、耗时

### 步骤 6：日志导出
$B snapshot -i
$B click "button:has-text('导出'), button:has-text('下载')"
$B wait --networkidle
$B wait 3s
$B snapshot
$B screenshot /tmp/logs-export-result.png

### 预期结果
- 日志列表正确展示所有操作记录
- 按用户/时间过滤正常
- 日志详情展示完整信息
- 导出生成文件

### 问题发现
$B console --errors
$B screenshot /tmp/logs-issue.png
# backend: OperationLogService
```

---

### 5.6 系统监控

### 测试提示词

```
## 系统监控 (System Monitor) 测试

### 步骤 1：进入系统监控页面
$B goto http://localhost:5173/monitor
$B wait --networkidle
$B snapshot -i
$B screenshot /tmp/monitor-loaded.png

### 步骤 2：健康状态
$B text
$B is visible "[class*='health'], [class*='status'], [class*='card']"
$B screenshot /tmp/monitor-health.png
# 期望看到健康指示（绿色/红色）

### 步骤 3：数据库连接池
$B wait --networkidle
$B snapshot
$B screenshot /tmp/monitor-db-pool.png
# 期望看到活跃/空闲连接数

### 步骤 4：Neo4j 状态
$B wait --networkidle
$B snapshot
$B screenshot /tmp/monitor-neo4j.png
# 期望看到 Neo4j 连接状态

### 步骤 5：Redis 状态
$B wait --networkidle
$B snapshot
$B screenshot /tmp/monitor-redis.png
# 期望看到 Redis 内存使用、命中率

### 步骤 6：系统资源图表
$B wait --networkidle
$B is visible "canvas, svg, [class*='echarts']"
$B screenshot /tmp/monitor-charts.png
# 期望看到内存、CPU 使用率折线图

### 步骤 7：刷新
$B click "button:has-text('刷新')"
$B wait --networkidle
$B snapshot
$B screenshot /tmp/monitor-refresh.png

### 步骤 8：预警阈值
$B wait --networkidle
$B text
# 检查是否有超过阈值的警告（如 CPU > 80%）
$B screenshot /tmp/monitor-alerts.png

### 预期结果
- 健康状态指示正确（UP/ DOWN）
- 各组件（数据库/Neo4j/Redis）状态可见
- 图表正确渲染
- 刷新功能更新数据
- 异常情况显示警告

### 问题发现
$B console --errors
$B screenshot /tmp/monitor-issue.png
# backend: MonitorService, Spring Actuator
```

---

## 测试执行优先级

| 优先级 | 模块 | 理由 |
|--------|------|------|
| P0 | 登录 → 仪表盘 | 核心入口，其他依赖登录态 |
| P0 | 图谱列表 → 图谱 IDE | 核心业务功能 |
| P0 | 数据导入 → 实体/边管理 | 核心数据流 |
| P1 | 混合检索 | 核心搜索能力 |
| P1 | 类/属性/约束管理 | 本体系统核心 |
| P1 | 用户管理 → 角色管理 → 菜单管理 | 系统管理基础 |
| P2 | Episode 管理 → 社区检测 | 高级图谱功能 |
| P2 | 法律知识图谱 | 专项扩展功能 |
| P2 | 自定义指令 → 提示词管理 | AI 配置功能 |
| P3 | 时序历史 | 高级时序功能 |
| P3 | 数据导出 | 数据流转功能 |
| P3 | 系统配置 → 操作日志 → 系统监控 | 系统保障功能 |

---

## 回归测试策略

### 冒烟回归（每次代码变更后）

执行：登录 → 仪表盘 → 数据导入 → 图谱 IDE → 混合检索
验证核心数据流不中断。

### 完整回归（每周或重大版本发布前）

按优先级顺序执行所有 24 个模块测试。重点：
- Neo4j 和 PostgreSQL 两端数据一致性
- 不同角色用户的菜单和数据访问控制

### 性能回归（每月）

- 1000+ 节点图谱的 IDE 加载 < 10s
- 1000+ 节点混合检索 < 5s
- 100+ Episode 时序历史加载 < 5s

---

**文档版本**: v1.0.0  
**最后更新**: 2026-06-16  
**适用系统版本**: OntoGraph 1.0.0-SNAPSHOT
