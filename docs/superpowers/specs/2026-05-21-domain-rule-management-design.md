# 域规则管理界面设计文档

**创建日期**: 2026-05-21  
**状态**: 待审批  
**作者**: AI Assistant (Brainstorming)

---

## 一、需求概述

为本体管理控制台 (OntologyWorkbench) 增加域规则管理功能,提供完整的 CRUD 操作和 SpEL 表达式测试能力。

### 核心需求
1. 在 OntologyWorkbench 中新建"域规则列表"标签页
2. 域规则列表展示与管理
3. 新建/编辑域规则表单
4. 删除域规则 (带确认)
5. 启用/禁用域规则开关
6. SpEL 表达式测试功能
7. 适用类支持"全部类"选项
8. SpEL 编辑器集成 LLM 辅助 (语法帮助、模板库、AI 生成)
9. 列表显示最近测试结果

### 技术约束
- UI 框架: Ant Design Vue
- 集成方式: OntologyWorkbench 标签页系统
- 后端 API: 已实现的 `ontologyApi` 域规则管理端点

---

## 二、架构设计

### 2.1 组件结构

```
ontograph-web/src/components/Ontology/
├── DomainRuleListPanel.vue      (新增) - 域规则列表主面板 (~350 行)
├── DomainRuleEditModal.vue      (新增) - 新建/编辑模态框 (~280 行)
└── DomainRuleTestModal.vue      (新增) - SpEL 表达式测试模态框 (~180 行)
```

### 2.2 组件职责

| 组件 | 职责 |
|------|------|
| DomainRuleListPanel | 数据加载、列表展示、搜索过滤、操作入口 |
| DomainRuleEditModal | 表单验证、CRUD 操作、SpEL 编辑器 |
| DomainRuleTestModal | SpEL 测试交互、结果展示 |

### 2.3 数据流

```
OntologyWorkbench
    │
    ├─ 用户点击"域规则列表" → store.openTab({ type: 'domain-rule-list' })
    │
    ├─ DomainRuleListPanel 挂载
    │     ├─ onLoad → ontologyApi.listDomainRules(graphId)
    │     ├─ 加载本体类列表 → ontologyApi.listClasses(graphId)
    │     └─ 渲染表格
    │
    ├─ 用户点击"新建" → 打开 DomainRuleEditModal
    │     └─ 提交 → ontologyApi.createDomainRule() → 刷新列表
    │
    ├─ 用户点击"编辑" → 打开 DomainRuleEditModal (填充数据)
    │     └─ 提交 → ontologyApi.updateDomainRule() → 刷新列表
    │
    ├─ 用户切换开关 → ontologyApi.toggleDomainRule() → 更新本地状态
    │
    ├─ 用户点击"测试" → 打开 DomainRuleTestModal
    │     └─ 执行 → ontologyApi.testDomainRule() → 展示结果
    │
    └─ 用户点击"删除" → 确认后 → ontologyApi.deleteDomainRule() → 刷新列表
```

---

## 三、详细设计

### 3.1 DomainRuleListPanel

#### 3.1.1 布局结构

```
┌──────────────────────────────────────────────────────────────┐
│ 🔧 域规则管理                              [+ 新建规则]      │
├──────────────────────────────────────────────────────────────┤
│ 搜索框: [输入规则名称/代码搜索...]                     🔍    │
├──────────────────────────────────────────────────────────────┤
│ 规则名称  │ 规则代码 │ SpEL表达式  │ 适用类 │ 级别 │ 测试 │ 操作 │
│──────────┼──────────┼───────────┼────────┼──────┼──────┼──────┤
│ 金额校验  │ AMT_CHK  │ #amount>0 │ 交易   │ ERROR│ ✅   │✏️🧪🗑️│
│ 日期有效  │ DATE_VAL │ #date>=...│ 合同   │ WARN │ ❌   │✏️🧪🗑️│
├──────────────────────────────────────────────────────────────┤
│ 共 5 条规则                                                   │
└──────────────────────────────────────────────────────────────┘
```

#### 3.1.2 表格列定义

```typescript
const columns = [
  { title: '规则名称', dataIndex: 'ruleName', width: '15%' },
  { title: '规则代码', dataIndex: 'ruleCode', width: '12%' },
  { title: 'SpEL 表达式', dataIndex: 'spelExpression', width: '25%', ellipsis: true },
  { title: '适用类', key: 'applicableClassIds', width: '15%' },
  { title: '严重级别', dataIndex: 'severity', width: '10%' },
  { title: '最近测试', key: 'lastTest', width: '15%' },
  { title: '操作', key: 'action', width: '18%' }
]
```

#### 3.1.3 关键功能

- **搜索过滤**: 前端过滤 `ruleName` 和 `ruleCode`
- **启用/禁用**: `a-switch` 直接调用 API
- **最近测试**: 显示 badge (成功/失败) + 测试时间
- **操作列**: 编辑 / 测试 / 删除 (带 `a-popconfirm`)

---

### 3.2 DomainRuleEditModal

#### 3.2.1 表单字段

| 字段 | 类型 | 验证 | 说明 |
|------|------|------|------|
| ruleName | string | 必填, max 100 | 规则名称 |
| ruleCode | string | 必填, 唯一, regex `^[A-Z_]+$` | 规则代码 |
| spelExpression | string | 必填, 包含 `#` | SpEL 表达式 |
| applicableClassIds | number[] | 可选 | 适用类 ID 列表 (空=全部类) |
| severity | string | 必填, enum | ERROR/WARNING/INFO |
| errorMessage | string | 可选 | 验证失败消息 |
| description | string | 可选 | 规则描述 |

#### 3.2.2 SpEL 编辑器增强

**工具栏按钮**:
- 📖 语法帮助 - 弹出 SpEL 语法速查表
- 📝 模板库 - 提供常用规则模板
- ✨ AI 生成 - 调用 LLM 生成表达式

**语法帮助内容**:
```
常用语法:
├─ 变量引用: #propertyName
├─ 比较运算: >, <, >=, <=, ==, !=
├─ 逻辑运算: &&, ||, !
├─ 字符串: #name.contains('test'), #name.matches('regex')
├─ 数值: #amount > 0, #age between 18 and 65
├─ 集合: #tags.size() > 0, #tags.contains('active')
└─ 空值检查: #field != null, #field?.length > 0
```

**模板库示例**:
```
├─ 金额校验: #amount > 0 && #amount <= #{maxAmount}
├─ 日期范围: #date >= T(java.time.LocalDate).now()
├─ 必填字段: #field != null && !#field.trim().isEmpty()
├─ 格式验证: #email matches '^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$'
├─ 年龄限制: #age >= 18 && #age <= 120
└─ 枚举校验: #status in {'ACTIVE', 'PENDING', 'COMPLETED'}
```

**LLM 生成接口** (需后端新增):
```
POST /ontology/{graphId}/domain-rules/generate-spel
Request:
{
  "description": "交易金额必须大于0且不超过100万",
  "propertyHints": ["amount"]
}
Response:
{
  "spelExpression": "#amount > 0 && #amount <= 1000000",
  "explanation": "检查金额在有效范围内",
  "alternatives": [
    "#amount in 1..1000000",
    "#amount > 0 and #amount <= 1000000"
  ]
}
```

#### 3.2.3 适用类选择

```vue
<a-select 
  v-model:value="form.applicableClassIds" 
  mode="multiple"
  placeholder="选择适用的本体类 (留空表示全部类)"
  allow-clear
>
  <a-select-option :value="null" style="font-weight: bold; color: #5e6ad2">
    🌐 全部类 (应用于所有节点)
  </a-select-option>
  <a-select-divider />
  <a-select-option v-for="cls in classList" :key="cls.id" :value="cls.id">
    {{ cls.localName }} ({{ cls.classUri }})
  </a-select-option>
</a-select>
```

**逻辑**:
- `applicableClassIds` 为空 → 后端存储为空,验证时应用于所有类
- `applicableClassIds` 有值 → 仅应用于指定的类

---

### 3.3 DomainRuleTestModal

#### 3.3.1 布局

```
┌─────────────────────────────────────────────┐
│ 🧪 测试 SpEL 表达式                          │
├─────────────────────────────────────────────┤
│ 当前表达式:                                  │
│ #amount > 0 && #amount <= 1000000           │
├─────────────────────────────────────────────┤
│ 测试数据 (JSON 格式):                        │
│ ┌───────────────────────────────────────┐   │
│ │ {                                     │   │
│ │   "amount": 500                       │   │
│ │ }                                     │   │
│ └───────────────────────────────────────┘   │
│                                              │
│ [▶ 执行测试]                                 │
├─────────────────────────────────────────────┤
│ 测试结果:                                    │
│ ✅ 验证通过                                  │
│ 或                                          │
│ ❌ 验证失败: 表达式返回 false                │
└─────────────────────────────────────────────┘
```

#### 3.3.2 功能

- 显示当前规则的 SpEL 表达式 (只读)
- JSON 编辑器输入测试数据
- 调用 `ontologyApi.testDomainRule()`
- 展示测试结果 (`passed`/`error`)
- 测试成功后更新列表的 `lastTestResult`

---

## 四、集成方案

### 4.1 OntologyStore 扩展

**文件**: `store/modules/ontology.ts`

```typescript
export type OntologyTabType = 
  | 'class-editor'
  | 'property-editor'
  | 'constraint-list'
  | 'domain-rule-list'  // ← 新增
  // ... 其他类型
```

### 4.2 OntologyWorkbench 集成

**文件**: `components/Ontology/OntologyWorkbench.vue`

**1. 新建菜单添加**:
```vue
<a-menu-item key="domain-rule-list">
  <span>⚙️</span> 域规则列表
</a-menu-item>
```

**2. 标签页渲染**:
```vue
<DomainRuleListPanel
  v-else-if="store.activeTab.type === 'domain-rule-list'"
  :graph-id="graphId"
/>
```

**3. 菜单映射**:
```typescript
const menuMap = {
  // ... 现有映射
  'domain-rule-list': { type: 'domain-rule-list', title: '域规则列表' }
}
```

**4. 异步加载组件**:
```typescript
const DomainRuleListPanel = defineAsyncComponent(
  () => import('./DomainRuleListPanel.vue')
)
```

### 4.3 API 集成

使用 `ontology.ts` 已实现的 API:

```typescript
import { ontologyApi } from '@/api/ontology'

// 列表
const rules = await ontologyApi.listDomainRules(graphId)

// 创建
await ontologyApi.createDomainRule(graphId, formData)

// 更新
await ontologyApi.updateDomainRule(graphId, ruleId, formData)

// 删除
await ontologyApi.deleteDomainRule(graphId, ruleId)

// 切换状态
await ontologyApi.toggleDomainRule(graphId, ruleId, enabled)

// 测试
const result = await ontologyApi.testDomainRule(graphId, spelExpression, testData)
```

---

## 五、错误处理

| 场景 | 处理方式 |
|------|----------|
| API 请求失败 | `message.error()` 展示后端错误信息 |
| 表单验证失败 | `a-form` 内置验证,高亮错误字段 |
| 删除确认 | `a-popconfirm` 提示"删除后无法恢复" |
| SpEL 语法错误 | 测试模态框展示详细错误信息 |
| LLM 生成失败 | 降级为手动输入,提示"AI 生成失败,请手动编写" |
| 网络超时 | 设置 30s 超时,展示重试按钮 |

---

## 六、响应式布局

### 6.1 表格响应式

- **桌面 (>1200px)**: 完整表格,所有列显示
- **平板 (768px-1200px)**: 隐藏"规则代码"列,SpEL 表达式缩短
- **移动端 (<768px)**: 切换为卡片布局 (可选)

### 6.2 模态框响应式

```vue
<a-modal 
  :width="windowWidth < 768 ? '95%' : '800px'"
  :body-style="{ maxHeight: '70vh', overflowY: 'auto' }"
>
```

---

## 七、测试策略

### 7.1 单元测试

- 表单验证逻辑测试
- SpEL 表达式基础语法检查
- 搜索过滤函数测试

### 7.2 集成测试

- CRUD 操作完整流程
- 启用/禁用开关状态同步
- 测试 API 调用与结果展示

### 7.3 用户验收测试

- 创建域规则并应用到类
- 编辑现有规则
- 测试 SpEL 表达式
- 删除规则确认流程

---

## 八、性能优化

### 8.1 数据加载

- 组件挂载时并行加载规则列表和类列表
- 使用 `Promise.all()` 减少加载时间

### 8.2 搜索优化

- 前端搜索使用 `computed` 缓存
- 防抖处理 (300ms) 避免频繁过滤

### 8.3 组件懒加载

- 使用 `defineAsyncComponent` 延迟加载
- 模态框按需渲染 (v-if 而非 v-show)

---

## 九、后续扩展

### 9.1 批量操作

- 批量启用/禁用
- 批量删除

### 9.2 规则导入导出

- JSON 格式导出
- 从文件导入规则

### 9.3 规则模板市场

- 预设行业规则模板
- 用户自定义模板分享

---

## 十、文件清单

| 文件 | 行数 (预估) | 说明 |
|------|------------|------|
| `DomainRuleListPanel.vue` | ~350 | 列表主面板 |
| `DomainRuleEditModal.vue` | ~280 | 编辑模态框 |
| `DomainRuleTestModal.vue` | ~180 | 测试模态框 |
| `ontology.ts` (修改) | +10 | 类型定义已存在 |
| `OntologyWorkbench.vue` (修改) | +15 | 集成新组件 |
| `store/modules/ontology.ts` (修改) | +2 | 添加 tab 类型 |
| **总计** | **~857** | |

---

## 十一、风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| LLM API 未实现 | AI 生成功能不可用 | 提供手动输入降级方案 |
| SpEL 表达式复杂度高 | 用户编写困难 | 加强模板库和语法帮助 |
| 规则数量过多 | 列表加载慢 | 实现分页或虚拟滚动 |
| 测试数据格式错误 | 测试失败 | JSON 编辑器添加语法高亮和验证 |

---

**审批状态**: 待用户确认  
**下一步**: 调用 `writing-plans` 创建实现计划
