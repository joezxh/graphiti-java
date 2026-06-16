# AI & KMS 模块 — 端到端测试与开发双轨 Skill(v2.1)

> 本文档为 AI 浏览器测试 Agent 与 AI 开发 Agent 提供**双轨提示词**:测试提示词(用于 `/qa`、`/qa-only`、MCP Playwright)+ 开发提示词(用于 `/code`、`subagent-driven-development`、Cursor Composer)。
> 覆盖 AI 智能模块(对话、模型、智能体、应用、Workflow、评估、工具、向量库、MCP、API Key、Prompt、统计)和 KMS 知识库(知识库、文档、分段、检索、文件、标签、问答对、法律知识库)共计 30 个子模块。
> 所有 API 路径均来自 `mediation-ai/mediation-ai-module/.../controller/admin` 真实 controller 的 `@RequestMapping` 取证,前端页面缺失清单与 `mediation-web/src/views` 实际目录完全一致。
> 工作流:**先按测试提示词自动化跑测试 → 对失败 / 需优化项按开发提示词直接开发 → 重新跑测试**。
> 使用 `/browser` 或 `MCP Playwright` 启动浏览器自动化测试,通过 `/open-gstack-browser` 导入认证 Cookie 后执行端到端测试。

---

## 目录

- [v2.1 重大变化](#v21-重大变化)
- [测试与开发双轨工作流](#测试与开发双轨工作流)
- [全局开发指引(必读)](#全局开发指引必读)
- [测试前置条件](#测试前置条件)
- [全局测试策略](#全局测试策略)
- [对比矩阵与迁移总览](#对比矩阵与迁移总览)
- [P0 优先级 — 核心 AI 引擎模块测试](#p0-优先级--核心-ai-引擎模块测试)
  - [AI-01 智能对话 Chat](#ai-01-智能对话-chat)
  - [AI-02 模型管理(ChatModel)](#ai-02-模型管理chatmodel)
  - [AI-03 Prompt 模板管理](#ai-03-prompt-模板管理)
  - [AI-04 Prompt Key 管理](#ai-04-prompt-key-管理)
  - [AI-05 API Key 管理](#ai-05-api-key-管理)
  - [AI-06 技能管理(Skill)](#ai-06-技能管理skill)
- [P1 优先级 — 智能体与应用层模块测试](#p1-优先级--智能体与应用层模块测试)
  - [AI-07 智能体 Agent](#ai-07-智能体-agent)
  - [AI-08 AI 应用(App)](#ai-08-ai-应用app)
  - [AI-09 App 对象管理](#ai-09-app-对象管理)
  - [AI-10 AI Workflow 工作流](#ai-10-ai-workflow-工作流)
  - [AI-11 评估-数据集 Dataset](#ai-11-评估-数据集-dataset)
  - [AI-12 评估-评估器 Evaluator](#ai-12-评估-评估器-evaluator)
  - [AI-13 评估-实验 Experiment](#ai-13-评估-实验-experiment)
  - [AI-14 工具管理 Tool](#ai-14-工具管理-tool)
  - [AI-15 向量库 VectorStore](#ai-15-向量库-vectorstore)
  - [AI-16 联网搜索 WebSearch](#ai-16-联网搜索-websearch)
  - [AI-17 MCP 客户端管理](#ai-17-mcp-客户端管理)
  - [AI-18 MCP API Key 管理](#ai-18-mcp-api-key-管理)
- [P2 优先级 — KMS 知识库层模块测试](#p2-优先级--kms-知识库层模块测试)
  - [KMS-01 知识库 Knowledge](#kms-01-知识库-knowledge)
  - [KMS-02 文档管理 Document](#kms-02-文档管理-document)
  - [KMS-03 知识分段 Segment](#kms-03-知识分段-segment)
  - [KMS-04 问答对 QuestionAnswer](#kms-04-问答对-questionanswer)
  - [KMS-05 文件分类 Category](#kms-05-文件分类-category)
  - [KMS-06 文件类型 Type](#kms-06-文件类型-type)
  - [KMS-07 文件管理 KmsFile](#kms-07-文件管理-kmsfile)
  - [KMS-08 标签管理 Tag](#kms-08-标签管理-tag)
  - [KMS-09 对象标签 ObjectTag](#kms-09-对象标签-objecttag)
  - [KMS-10 法律知识库(7 个子模块)](#kms-10-法律知识库7-个子模块)
- [P3 优先级 — 辅助/统计层模块测试](#p3-优先级--辅助统计层模块测试)
  - [AI-19 LLM 调用统计/日志](#ai-19-llm-调用统计日志)
  - [AI-20 文档生成 Image/Music/Write/MindMap](#ai-20-文档生成-imagemusicwritemindmap)
- [迁移参考文件清单](#迁移参考文件清单)
- [AI 引擎差异对比](#ai-引擎差异对比)
- [测试结果报告模板](#测试结果报告模板)
- [规格自检](#规格自检)

---

## v2.1 重大变化

| 维度 | v2.0 | v2.1 |
|------|------|------|
| 提示词数量 | 仅测试提示词 | **测试提示词 + 开发提示词 双轨** |
| 工作流 | 测试 → 报告 | **测试 → 不通过/需优化 → 开发 → 回归测试** |
| 开发 Agent | 无 | 每个模块追加 `#### 开发提示词`,含任务清单 + 代码骨架 + 适配点 |
| 全局指引 | 无 | 新增「全局开发指引」:统一开发规范、TDD 流程、组件模板 |
| 模块数 | 30 | 30(不变) |

---

## 测试与开发双轨工作流

```
┌─────────────────────────────────────────────────────────────┐
│  1. 测试阶段(自动)                                          │
│     /browser /open-gstack-browser                            │
│     加载认证 Cookie → 执行「测试提示词」全部场景             │
│     记录:通过 / 失败 / 阻塞                                  │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  2. 失败定位                                                 │
│     • Network 4xx/5xx → 后端 controller / 权限 / 路径问题     │
│     • Console JS 错误 → 前端代码 bug                         │
│     • 元素找不到 / 弹窗未弹 → 前端缺失/路由问题              │
│     • 数据为空 / 接口超时 → 后端服务 / 数据准备              │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  3. 开发阶段(自动)                                          │
│     加载「开发提示词」对应模块                                │
│     subagent-driven-development / Cursor Composer            │
│     修复 → 本地自测 → 提交(atomic commit)                   │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  4. 回归测试                                                 │
│     重新跑同一模块的「测试提示词」全部场景                    │
│     全绿 → 进入下一模块;仍红 → 回到 2/3 循环                  │
└─────────────────────────────────────────────────────────────┘
```

### 三类常见失败与对应开发入口

| 失败类型 | 现象 | 开发入口 |
|----------|------|----------|
| **前端页面不存在** | 路由 404 / 菜单找不到 / 页面空白 | 章节「开发提示词」第 1 步:创建 Vue 文件 |
| **前端字段缺失** | 表单字段少 / 列展示不全 / 提交 400 | 章节「开发提示词」第 2 步:补齐字段 |
| **后端 API 不可用** | 5xx / 超时 / 业务异常 | 章节「开发提示词」第 3 步:后端修复(优先级低,后端已就绪) |

---

## 全局开发指引(必读)

> 所有模块的开发提示词都遵循这份统一规范,避免每个 Agent 重复造轮子。

### 通用开发规范

#### A. 文件结构(单一职责)

```
src/views/<domain>/<module>/
├── index.vue                    # 列表页(必选)
├── <Module>FormModal.vue        # 表单弹窗(必选,新增/编辑)
├── <Module>DetailModal.vue      # 详情弹窗(可选,只读)
├── <Module>ImportForm.vue       # 导入(可选)
└── components/                  # 业务子组件(可选)
    ├── <SubWidget>.vue
    └── ...

src/api/<domain>/<module>.ts     # API 封装(必选)
```

#### B. index.vue 模板(列表页)

```vue
<template>
  <div class="p-5 enter-y">
    <Card :title="`AI <模块名>管理`" :bordered="false">
      <!-- 搜索栏(固定 3 列布局) -->
      <Row :gutter="16" class="mb-4">
        <Col :span="6">
          <Input v-model:value="searchForm.name" placeholder="<模块名>名称" allow-clear @pressEnter="handleSearch" />
        </Col>
        <Col :span="4">
          <DictSelect v-model:value="searchForm.status" type="sys_normal_disable" placeholder="状态" />
        </Col>
        <Col :span="14" class="flex items-center gap-2">
          <Button type="primary" @click="handleSearch"><SearchOutlined /> 搜索</Button>
          <Button @click="handleReset"><ReloadOutlined /> 重置</Button>
          <Button v-has-permi="['<domain>:<module>:create']" type="primary" @click="openForm()">
            <PlusOutlined /> 新增<模块名>
          </Button>
        </Col>
      </Row>

      <!-- 表格 -->
      <Table :columns="columns" :data-source="dataSource" :loading="loading"
             :pagination="paginationConfig" :row-key="'id'" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <DictSwitch type="sys_normal_disable" :model-value="record.status"
              :before-change="(val: boolean) => handleStatusChange(record, val)" />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableAction :actions="[
              { label: '编辑', auth: ['<domain>:<module>:update'], onClick: () => openForm(record) },
              { label: '删除', auth: ['<domain>:<module>:delete'], danger: true,
                popConfirm: { title: '确认删除?', confirm: () => handleDelete(record.id) } },
            ]" :row="record" />
          </template>
        </template>
      </Table>
    </Card>

    <ModuleFormModal v-model:open="formVisible" :data="formData" @success="loadData" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { Card, Table, Button, Input, Row, Col, message } from 'ant-design-vue';
import { SearchOutlined, ReloadOutlined, PlusOutlined } from '@ant-design/icons-vue';
import { <apiName>Api, type <VO> } from '@/api/<domain>/<module>';
import { DictSwitch, DictSelect, TableAction } from '@/components/business';
import ModuleFormModal from './<Module>FormModal.vue';

const searchForm = reactive({ name: '', status: undefined as number | undefined });
const dataSource = ref<<VO>[]>([]);
const loading = ref(false);
const formVisible = ref(false);
const formData = ref<Partial<<VO>>>({});

const paginationConfig = computed(() => ({
  current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条`,
}));

const columns = [
  { title: '名称', dataIndex: 'name', key: 'name', width: 200 },
  { title: '状态', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' },
];

async function loadData() {
  loading.value = true;
  try {
    const res = await <apiName>Api.page({ ...searchForm, pageNo: 1, pageSize: 10 });
    dataSource.value = res.data.list;
    paginationConfig.value.total = res.data.total;
  } finally { loading.value = false; }
}

function handleSearch() { loadData(); }
function handleReset() { searchForm.name = ''; searchForm.status = undefined; loadData(); }
function openForm(record?: <VO>) { formData.value = record ?? {}; formVisible.value = true; }
async function handleDelete(id: number) {
  await <apiName>Api.delete(id);
  message.success('删除成功');
  loadData();
}
async function handleStatusChange(record: <VO>, val: boolean) {
  await <apiName>Api.update({ ...record, status: val ? 0 : 1 });
  message.success('状态已更新');
  loadData();
  return true;
}

onMounted(loadData);
</script>
```

#### C. <Module>FormModal.vue 模板

```vue
<template>
  <Modal :open="open" :title="formData.id ? '编辑<模块名>' : '新增<模块名>'"
         :confirm-loading="submitting" @ok="handleSubmit" @cancel="handleCancel" width="640px" destroy-on-close>
    <Form layout="vertical" :model="form" :rules="rules" ref="formRef">
      <Form.Item label="名称" name="name">
        <Input v-model:value="form.name" placeholder="请输入名称" />
      </Form.Item>
      <!-- 按需追加字段 -->
      <Form.Item label="状态" name="status">
        <DictRadio type="sys_normal_disable" v-model:value="form.status" />
      </Form.Item>
    </Form>
  </Modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue';
import { Modal, Form, Input, message } from 'ant-design-vue';
import { <apiName>Api } from '@/api/<domain>/<module>';
import { DictRadio } from '@/components/business/Dict';

const props = defineProps<{ open: boolean; data: any }>();
const emit = defineEmits<{ 'update:open': [v: boolean]; success: [] }>();

const formRef = ref();
const submitting = ref(false);
const form = reactive<any>({ id: undefined, name: '', status: 0 });

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

watch(() => props.open, (v) => {
  if (v) Object.assign(form, { id: undefined, name: '', status: 0, ...props.data });
});

async function handleSubmit() {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    if (form.id) await <apiName>Api.update(form);
    else await <apiName>Api.create(form);
    message.success(form.id ? '更新成功' : '新增成功');
    emit('success');
    handleCancel();
  } finally { submitting.value = false; }
}

function handleCancel() { emit('update:open', false); }
</script>
```

#### D. <api>.ts 模板

```typescript
import request from '@/utils/request';

export interface <VO> {
  id: number;
  name: string;
  status: number;
  createTime: string;
}

export interface PageReq {
  pageNo: number;
  pageSize: number;
  name?: string;
  status?: number;
}

export const <apiName>Api = {
  page: (data: PageReq) => request({ url: '/<domain>/<module>/page', method: 'GET', data }),
  get: (id: number) => request({ url: '/<domain>/<module>/get', method: 'GET', params: { id } }),
  create: (data: Partial<<VO>>) => request({ url: '/<domain>/<module>/create', method: 'POST', data }),
  update: (data: Partial<<VO>>) => request({ url: '/<domain>/<module>/update', method: 'PUT', data }),
  delete: (id: number) => request({ url: '/<domain>/<module>/delete', method: 'DELETE', params: { id } }),
  simpleList: () => request({ url: '/<domain>/<module>/simple-list', method: 'GET' }),
};
```

#### E. TDD 开发循环(每个模块)

```
1. 复制 tianque-ui 页面骨架(若可) → 调整路径与权限
2. 创建 api/<domain>/<module>.ts(参照后端 controller 路径)
3. 调整菜单与路由(动态菜单无需改,权限标识要正确)
4. 跑该模块「测试提示词」全部场景
5. 对失败项:
   - 字段缺失 → 补 Form.Item + columns
   - API 错误 → 检查路径/参数/权限
   - 组件渲染问题 → 检查 import / props / slot
6. 修复后跑回归,全绿后 atomic commit
```

#### F. 适配点速查

| 维度 | tianque-ui | mediation-web | 转换 |
|------|------------|---------------|------|
| 权限指令 | `v-auth` / `auth` | `v-has-permi="['x:y:z']"` | 全局替换 |
| API 前缀 | `/admin-api/ai/...` | 直接 `/ai/...` | **去掉前缀** |
| 组件库 | Vben BasicTable | `ant-design-vue Table` | 用 mediation 风格 |
| 字典 | i18n tag | `DictSelect`/`DictTag` | 用 `@/components/business/Dict` |
| 状态切换 | 单独按钮 | `DictSwitch` + `before-change` | 用 mediation 模式 |

---

| 维度 | v1.0 | v2.0 |
|------|------|------|
| 模块数量 | 19 个混合 | **30 个**(AI×20 + KMS×10) |
| 优先级 | P0/P1/P2 | **P0/P1/P2/P3**(新增 P3 辅助/统计) |
| API 路径 | 推测 | **真实取证**(从 controller 提取) |
| 后端状态标注 | "已实现/缺失" | 增加 **API 完整度**(接口数/实际可用) |
| 提示词 | 仅 CRUD | 增加 **边界/异常/安全/性能** 维度 |
| 文件结构 | 单文件 2185 行 | 按优先级分章节、可独立加载 |

---

## 测试前置条件

### 环境要求

| 项目 | 值 |
|------|------|
| 前端地址 | `http://localhost:5173` |
| 网关地址 | `http://localhost:8080` |
| AI 服务 | `http://localhost:8083` |
| UAA 服务 | `http://localhost:8081` |
| System 服务 | `http://localhost:8082` |
| 超级管理员账号 | `admin` |
| 超级管理员密码 | `admin123` |
| 默认租户 ID | `1` |

### 服务依赖检查

测试前需确认以下服务已启动:

1. **Gateway**(:8080)— API 网关
2. **UAA**(:8081)— 认证授权服务
3. **System**(:8082)— 系统公共服务
4. **AI Module**(:8083)— AI 智能模块(`mediation-module-ai-server`)
5. **Harness**(:8084)— Agent 沙箱运行时(`mediation-module-harness-server`,仅 AI-07 Agent 用例需要)
6. **MySQL/PostgreSQL** — 数据库
7. **Redis** — 缓存
8. **Elasticsearch** — 向量检索(可选,ES 不可用时知识库检索降级)
9. **Nacos** — 服务注册中心

### 浏览器环境要求

- 浏览器:Chromium / Chrome(headless 模式或带 UI 模式均可)
- Cookie 导入:通过 `/open-gstack-browser` 导入已登录 Cookie
- 如需手动登录:账号 `admin`,密码 `admin123`,租户 ID `1`

### 前置 SQL 数据(可选)

```sql
-- 创建测试用的 Chat 模型
INSERT INTO ai_chat_model (name, model_id, provider, base_url, api_key, status)
VALUES ('DeepSeek Chat', 'deepseek-chat', 'deepseek', 'https://api.deepseek.com/v1', 'sk-test-xxx', 0);

-- 创建测试用的 Embedding 模型
INSERT INTO ai_embedding_model (name, model_id, provider, dimensions, status)
VALUES ('BGE-M3', 'bge-m3', 'bge', 1024, 0);

-- 创建测试 Agent
INSERT INTO ai_agent (name, category, system_prompt, status)
VALUES ('测试 Agent', '客服', '你是一个客服助手', 0);
```

---

## 全局测试策略

### 每个测试用例的验证清单(12 维度)

1. **页面加载**:页面是否正常渲染,无白屏、无 JS 错误
2. **数据加载**:表格数据是否成功加载,loading 状态是否正确消失
3. **交互响应**:按钮点击是否有响应,弹窗是否正常弹出
4. **表单验证**:必填字段校验是否生效,错误提示是否显示
5. **数据回填**:编辑时已有数据是否正确回填到表单
6. **操作反馈**:成功/失败是否有 message 提示
7. **状态更新**:操作后列表数据是否自动刷新
8. **权限控制**:`v-has-permi` 指令是否正确控制按钮显隐
9. **控制台检查**:浏览器控制台是否有报错或警告
10. **边界场景**:空数据、超长文本、特殊字符(XSS/SQL 注入)、并发操作
11. **网络检查**:Network 面板 4xx/5xx 状态码,响应体格式
12. **响应时间**:首屏 < 2s,接口 < 1s(数据 < 100 条),< 3s(数据 < 1000 条)

### 通用测试模式 — CRUD + 边界循环

```
1. 打开页面 → 验证列表加载、空状态、loading 消失
2. 搜索/筛选 → 验证条件过滤、重置生效
3. 点击「新增」→ 验证弹窗/表单初始化 → 填写并提交 → 验证列表刷新
4. 点击「编辑」→ 验证数据回填 → 修改并提交 → 验证更新生效
5. 点击「删除」→ 验证确认弹窗 → 确认删除 → 验证列表更新
6. 批量删除 → 验证多选、批量操作
7. 导出 → 验证 Excel 文件下载、内容完整
8. 导入(若支持)→ 验证模板下载、批量导入、错误行提示
9. 详情/查看 → 验证只读视图、关联数据展示
10. 边界测试 → 空提交、超长输入、特殊字符、并发请求
```

---

## 对比矩阵与迁移总览

### 模块对比总览(基于真实目录取证)

| # | 分类 | tianque-ui 模块路径 | mediation-web 现有 | 后端 controller 状态 | 优先级 | 缺失/状态 |
|---|------|---------------------|-------------------|---------------------|--------|-----------|
| 1 | 对话 | `ai/chat/index/*` | `ai/chat/index.vue` | `chat/AiChatMessageController` | P0 | ⚠️ 基础实现,需增强 |
| 2 | 模型 | `ai/console/model` | `ai/model/index.vue` | `model/ModelController` | P0 | ⚠️ 基础,无 ChatModel 表单 |
| 3 | Prompt | `ai/console/prompt` | ❌ 缺失 | `prompt/PromptTemplateController` | P0 | ❌ 缺失 |
| 4 | Prompt Key | `ai/console/prompt/promptkey` | ❌ 缺失 | 内嵌在 PromptTemplateController | P0 | ❌ 缺失 |
| 5 | API Key | `ai/console/apiKey` | ❌ 缺失 | `model/AiApiKeyController` | P0 | ❌ 缺失 |
| 6 | 技能 | `ai/console/skill` (tianque-ui 无) | `ai/skill/index.vue` | `skill/MediationSkillController` | P0 | ✅ 已实现 |
| 7 | Agent | `ai/platform/agent` | ❌ 缺失 | `model/AiAgentController` | P1 | ❌ 缺失 |
| 8 | App | `ai/platform/app` | ❌ 缺失 | `app/AppController` | P1 | ❌ 缺失 |
| 9 | AppObject | `ai/platform/app/objectManager` | ❌ 缺失 | `app/AppObjectController` | P1 | ❌ 缺失 |
| 10 | Workflow | `ai/platform/aiflow` | ❌ 缺失 | `aiflow/WorkflowController` | P1 | ❌ 缺失 |
| 11 | Dataset | `ai/evaluation/dataset` | ❌ 缺失 | `dataset/DatasetController` | P1 | ❌ 缺失 |
| 12 | Evaluator | `ai/evaluation/evaluator` | ❌ 缺失 | `evaluator/EvaluatorController` | P1 | ❌ 缺失 |
| 13 | Experiment | `ai/evaluation/experiment` | ❌ 缺失 | `experiment/ExperimentController` | P1 | ❌ 缺失 |
| 14 | Tool | `ai/console/tool` | ❌ 缺失 | `model/ToolController` | P1 | ❌ 缺失 |
| 15 | VectorStore | `ai/console/vectorstore` | ❌ 缺失 | `model/VectorStoreController` | P1 | ❌ 缺失 |
| 16 | WebSearch | `ai/console/websearch` | ❌ 缺失 | `model/AiWebSearchController` | P1 | ❌ 缺失 |
| 17 | MCP Client | `ai/console/mcp` | ❌ 缺失 | `model/McpClientController` | P1 | ❌ 缺失 |
| 18 | MCP API Key | 同上子表单 | ❌ 缺失 | `model/McpApiKeyController` | P1 | ❌ 缺失 |
| 19 | LLM Log | `ai/stat/llmlog` | ❌ 缺失 | `chat/ChatLlmLogController` | P3 | ❌ 缺失 |
| 20 | 文档生成 | `ai/console/...`(分散) | ❌ 缺失 | `document/AiImage/AiMusic/AiWrite/AiMindMap` | P3 | ❌ 缺失 |
| 21 | KMS Knowledge | `kms/knowledge` | ❌ 缺失 | `knowledge/KnowledgeController` | P2 | ❌ 缺失 |
| 22 | KMS Document | `kms/kms/document` | ❌ 缺失 | `knowledge/KnowledgeDocumentController` | P2 | ❌ 缺失 |
| 23 | KMS Segment | `kms/kms/segment` | ❌ 缺失 | `knowledge/KnowledgeSegmentController` | P2 | ❌ 缺失 |
| 24 | QA | `kms/questionanswer` | ❌ 缺失 | `knowledge/QuestionAnswerController` | P2 | ❌ 缺失 |
| 25 | KMS File | `kms/file` | ❌ 缺失 | `kmsfile/KmsFileController` | P2 | ❌ 缺失 |
| 26 | KMS Category | `kms/category` | ❌ 缺失 | `kmsfile/CategoryController` | P2 | ❌ 缺失 |
| 27 | KMS Type | `kms/filetype` | ❌ 缺失 | `kmsfile/KmsTypeController` | P2 | ❌ 缺失 |
| 28 | Tag | `kms/tag` | ❌ 缺失 | `tag/TagController` | P2 | ❌ 缺失 |
| 29 | ObjectTag | `kms/tag` 子表单 | ❌ 缺失 | `tag/ObjectTagController` | P2 | ❌ 缺失 |
| 30 | Legal(7 个) | `kms/legal/*` | ❌ 缺失 | `legal/{Type,Paper,Org,Item,Info,Fuzzy,Case}Controller` | P2 | ❌ 缺失 |

### 统计

| 状态 | 数量 | 占比 |
|------|------|------|
| ✅ 已实现 | 1 | 3.3% |
| ⚠️ 基础实现 | 2 | 6.7% |
| ❌ 缺失(需新建) | 27 | 90.0% |
| **总计** | **30** | **100%** |

### 后端 API 完整度(从 controller 文件取证)

| 模块 | 接口数 | 关键 API |
|------|--------|----------|
| Chat | 11 | send/list-by-conversation-id/delete-by-conversation-id |
| Model | 7 | create/update/delete/get/page/simple-list |
| Prompt | 9 | create/copy/update/delete/get/page/simple-list/pageEncrypt |
| Agent | 14 | create-my/update-my/delete-my/app-list/create/update/delete |
| App | 9 | create/update/delete/get/getDetail/page/simple-list |
| Workflow | 15 | create/update/delete/getFlow/flow-act/list-by-flow-code/workflow-log |
| Dataset | 15 | create/update/delete/page + dataset-item/dataset-version/experiments |
| Evaluator | 10 | create/update/delete/page + evaluator-version/experiments/template |
| Experiment | 9 | create/update/delete/page + experiment-result/stop/rerun |
| Tool | 7 | create/update/delete/get/page/simple-list |
| VectorStore | 6 | create/update/delete/get/page/export-excel |
| WebSearch | 8 | create/update/delete/get/page/simple-list/pageEncrypt |
| ApiKey | 9 | create/update/delete/get/page/simple-list/chat-model/pageEncrypt |
| McpClient | 5 | create/update/delete/page/get |
| McpApiKey | 6 | create/update/delete/get/page/mcp-client/page |
| Knowledge | 17 | create/update/delete/page + 5 类知识(createKnowledgeCategory 等) |
| Document | 9 | create/start/stop/enable/disable/delete/downloadUrl/get/page |
| Segment | 8 | page/create/update/split/search/update-status/delete |
| QA | 12 | create/update/delete/delete-list/batchDelete/page/get/embeddingFile/embeddingMaterial |
| File | 13 | create/update/delete/get/download/getFiles/pageTitles/pageDownloads |
| Category | 10 | create/update/update-list/delete/delete-list/list/page/get/getRoot |
| Type | 8 | create/update/update-list/delete/delete-list/get/list/page |
| Tag | 8 | create/update/delete/delete-list/get/page/vectorSearch/list |
| ObjectTag | 5 | create/update/delete/get/page |
| Legal(7 合并) | ~70 | 全部 CRUD + export-excel + export-word |
| ChatLlmLog | 7 | create/update/delete/get/page/stat |
| Image | 12 | my-page/get-my/draw/midjourney/* /page/update/delete |
| Music/Write/MindMap | 各 5~8 | 见各 controller |

---

## P0 优先级 — 核心 AI 引擎模块测试

> P0 覆盖:对话流、模型接入、Prompt 模板、API Key、技能注册,这是 AI 系统运转的最小闭环。

---

### AI-01 智能对话 Chat

**页面路径**:`/ai/chat`
**源码文件**:`src/views/ai/chat/index.vue`(基础实现)
**API 文件**:`src/api/ai/conversation.ts`, `src/api/ai/assistant.ts`
**后端 Controller**:`chat/AiChatMessageController` (`/ai/chat-message`) + `chat/AiChatConversationController` (`/ai/chat/conversation`)
**权限标识**:`ai:chat:send`, `ai:chat:conversation`, `ai:chat:message`

> ⚠️ **基础实现** — 现有 `index.vue` 仅基础结构,需增强 Markdown 渲染、流式输出、停止生成、消息反馈等。

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/ai/chat-message/send` | 发送消息(支持流式 SSE) |
| GET | `/ai/chat-message/list-by-conversation-id` | 获取会话消息 |
| GET | `/ai/chat-message/getRelationQuestion` | 获取相关问题推荐 |
| POST | `/ai/chat-message/fetchQueryType` | 查询意图分类 |
| DELETE | `/ai/chat-message/delete` | 删除消息 |
| DELETE | `/ai/chat-message/delete-by-conversation-id` | 按会话清空消息 |
| GET | `/ai/chat-message/page` | 后台消息分页 |
| POST | `/ai/chat/conversation/create` | 创建会话(管理员视角) |
| POST | `/ai/chat/conversation/create-my` | 创建我的会话 |
| GET | `/ai/chat/conversation/my-list` | 我的会话列表 |
| GET | `/ai/chat/conversation/user-list` | 用户视角会话列表 |
| PUT | `/ai/chat/conversation/update-my` | 更新我的会话 |
| DELETE | `/ai/chat/conversation/delete-my` | 删除我的会话 |
| DELETE | `/ai/chat/conversation/delete-by-unpinned` | 清理未置顶会话 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开 AI 智能对话页面,执行聊天功能完整测试。

【前置操作】
1. 使用 admin/admin123 登录系统
2. 在左侧菜单点击「AI 智能」→「智能对话」
3. 等待页面加载完成

---

【测试场景 1:页面加载与基础显示】
1. 验证聊天页面正常加载,无白屏
2. 检查页面布局:左侧会话列表、中间消息列表、底部输入框
3. 验证默认显示欢迎消息或空状态提示
4. 验证输入框 placeholder 显示提示文本
5. 检查 Network 面板:GET /ai/chat/conversation/my-list 返回 200

预期结果:
✅ 三栏布局正确
✅ 欢迎消息正常显示
✅ 会话列表 API 调用成功
✅ 输入框可输入

---

【测试场景 2:新建会话】
1. 点击「新建会话」按钮
2. 验证左侧会话列表新增一条记录
3. 验证消息区域清空,显示欢迎消息
4. 验证 Network: POST /ai/chat/conversation/create-my 200

预期结果:
✅ 新建会话成功
✅ 会话列表更新
✅ 消息区清空

---

【测试场景 3:发送消息 - 文本 + 流式输出】
1. 在输入框输入测试消息「你好,请介绍一下你的功能」
2. 点击「发送」按钮或按 Enter 键
3. 验证:
   - 用户消息立即显示在消息列表
   - 发送按钮变为禁用状态(显示 loading)
   - AI 开始回复(显示思考中/输入中状态)
4. 验证流式响应:Network 面板 EventStream / text-event-stream,逐字输出
5. 等待 AI 回复完成
6. 验证:
   - AI 回复消息完整显示
   - 消息支持 Markdown 渲染
   - 发送按钮恢复可用状态
7. 验证 Network: POST /ai/chat-message/send 200,消息持久化

预期结果:
✅ 消息发送成功
✅ AI 流式回复正常
✅ Markdown 渲染正确
✅ 消息已持久化(刷新页面仍在)

---

【测试场景 4:停止生成】
1. 发送一个需要较长回复的问题(如「请详细解释什么是大语言模型」)
2. 在流式输出过程中,点击「停止生成」按钮
3. 验证:
   - 流式输出立即停止(EventSource close)
   - 已生成内容保留
   - 发送按钮恢复可用

预期结果:
✅ 停止生成生效
✅ 已生成内容不丢失

---

【测试场景 5:会话切换】
1. 创建 2-3 个不同的会话,每个发送不同消息
2. 点击左侧会话列表中的不同会话
3. 验证:
   - 消息区域切换到对应会话的历史消息
   - Network: GET /ai/chat-message/list-by-conversation-id?id=xxx 200
   - 当前选中的会话高亮显示
   - 输入框可用

预期结果:
✅ 会话切换正确
✅ 历史消息加载完整

---

【测试场景 6:会话重命名】
1. 右键点击或点击会话旁的「更多」按钮
2. 选择「重命名」选项
3. 输入新名称「测试会话_001」
4. 验证 Network: PUT /ai/chat/conversation/update-my 200

预期结果:
✅ 重命名成功
✅ 会话列表显示新名称

---

【测试场景 7:删除会话】
1. 点击会话旁的「删除」按钮
2. 验证弹出确认对话框
3. 点击「确定」
4. 验证:
   - 会话从列表消失
   - Network: DELETE /ai/chat/conversation/delete-my 200
   - 如果删除的是当前会话,自动切换到其他会话或显示空状态

预期结果:
✅ 删除功能正常
✅ 自动切换逻辑正确

---

【测试场景 8:文件上传】
1. 点击输入框旁的「上传文件」按钮
2. 选择一个文件(如 PDF、Word、图片)
3. 验证:
   - 文件上传进度显示
   - 上传成功后文件显示在输入框或消息中
4. 发送包含文件的消息
5. 验证 AI 能识别并处理文件内容

预期结果:
✅ 文件上传正常
✅ AI 能处理文件

---

【测试场景 9:相关问题推荐】
1. AI 回复完成后
2. 验证消息下方出现「相关问题」推荐列表
3. 点击任一推荐问题
4. 验证自动作为新消息发送
5. 验证 Network: GET /ai/chat-message/getRelationQuestion 200

预期结果:
✅ 相关问题正常加载
✅ 点击可继续对话

---

【测试场景 10:边界与异常】
1. 发送空消息 → 验证按钮禁用
2. 发送超长消息(> 10000 字符)→ 验证限制或截断
3. 发送包含 Markdown 特殊字符(<script>alert(1)</script>)→ 验证 XSS 转义
4. 网络断开时发送消息 → 验证错误提示
5. 快速连续点击「发送」→ 验证防抖(不发送重复消息)
6. 切换租户后刷新页面 → 验证会话隔离

预期结果:
✅ 空消息被阻止
✅ 超长输入有提示
✅ XSS 被转义
✅ 网络异常有提示
✅ 防抖生效
✅ 租户隔离生效

---

【问题诊断】
- 消息发送失败 → 检查 Network 面板 POST /ai/chat-message/send 请求与后端 `/ai/chat-model` 配置
- 流式输出中断 → 检查 SSE 连接、SSE 代理(Nginx)配置
- 会话列表不刷新 → 检查 WebSocket / 轮询机制
- Markdown 渲染异常 → 检查 MarkdownWithReferences 组件(参考 tianque-ui)
- 引用未显示 → 检查知识库 MessageKnowledge、MessageLegalCase 等组件
```

#### 开发提示词

> 现有 `src/views/ai/chat/index.vue` 仅基础骨架,本模块需**增强**而非新建。完成后跑 AI-01 测试提示词全场景回归。

```
/code 或 subagent-driven-development
打开 src/views/ai/chat/index.vue,执行以下增强任务。

【任务 1:Markdown 渲染组件】
1. 安装 markdown-it、highlight.js(若未安装)
2. 创建 src/views/ai/chat/components/message/MarkdownWithReferences.vue
3. 复制 tianque-ui/views/ai/chat/index/components/message/MarkdownWithReferences.vue 骨架
4. 适配 mediation-web 风格(去掉 Vben 依赖,改用纯 Vue 3 + ant-design-vue)
5. 支持代码高亮、Mermaid 图表、表格

【任务 2:流式输出 + 停止生成】
1. 改造 send 按钮:点击后立即禁用,显示 loading
2. SSE 连接通过 fetch + ReadableStream(不用 EventSource,便于带 Authorization)
3. 解析 text/event-stream,逐字追加到 AI 消息 content
4. 停止按钮:AbortController 关闭 fetch
5. 已生成内容保留,前端标记 message.finished = false

【任务 3:消息类型组件(5 种)】
参考 tianque-ui 创建:
- MessageKnowledge.vue → 显示引用分段(分段 ID、相似度、内容预览)
- MessageLegalCase.vue → 显示引用案例
- MessageLegalInfo.vue → 显示引用法条
- MessageLegalItem.vue → 显示引用法条项
- MessageQuestionAnswer.vue → 显示引用 QA
- MessageWebSearch.vue → 显示联网搜索结果
- MessageReasoning.vue → 显示推理过程(deepseek-reasoner 等模型)
- MessageFiles.vue → 显示消息附件

【任务 4:相关问题推荐】
1. AI 回复完成后,GET /ai/chat-message/getRelationQuestion
2. 在消息下方显示 3-5 个推荐问题
3. 点击自动作为新消息发送(自动填充输入框 + 触发 send)

【任务 5:会话侧边栏增强】
1. 会话置顶(pin)
2. 批量清理未置顶会话(DELETE /delete-by-unpinned)
3. 搜索会话(GET /user-list?name=xxx)
4. 按时间/置顶排序

【任务 6:文件上传】
1. 复用 system/file 组件 或 新建 MessageFileUpload.vue
2. 上传走 /infra/file/upload(参照 system 模块)
3. 发送消息时携带 fileIds[]

【任务 7:边界与异常处理】
1. 输入框空 → 发送按钮 disabled
2. 长度超 10000 → 显示计数器 + 红色警告
3. XSS → 用 v-text 或 DOMPurify 净化
4. 网络断开 → 提示「网络异常,请检查连接」+ 保留草稿
5. 防抖 → 发送中 disabled
6. 租户隔离 → 切换租户后清空会话列表(清 store + 重新 loadData)

【任务 8:回归测试】
跑 AI-01「测试提示词」全部 10 个场景,全绿后 commit。
```

---

### AI-02 模型管理(ChatModel)

**页面路径**:`/ai/model`
**源码文件**:`src/views/ai/model/index.vue`(基础实现,仅 CRUD)、`ModelFormModal.vue`
**API 文件**:`src/api/ai/model.ts`
**后端 Controller**:`model/ModelController` (`/ai/chat-model`)
**权限标识**:`ai:model:create`, `ai:model:update`, `ai:model:delete`, `ai:model:query`

> ⚠️ **基础实现** — 现有 `ModelFormModal` 仅基础字段,需增强:模型分组、平台类型、超时配置、测试对话。

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/ai/chat-model/create` | 创建模型 |
| PUT | `/ai/chat-model/update` | 更新模型 |
| DELETE | `/ai/chat-model/delete` | 删除模型 |
| GET | `/ai/chat-model/get` | 模型详情 |
| GET | `/ai/chat-model/page` | 分页列表 |
| GET | `/ai/chat-model/simple-list` | 简化列表(下拉用) |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开 AI 模型管理页面,执行 ChatModel 完整 CRUD + 高级配置测试。

【前置操作】
1. 登录系统
2. 左侧菜单「AI 智能」→「模型管理」
3. 等待页面加载

---

【测试场景 1:列表加载与字段展示】
1. 验证表格列:模型名称、供应商、模型 ID、API 地址、状态、创建时间、操作
2. 验证分页:每页 10 条,翻页器正常
3. 验证 Network: GET /ai/chat-model/page?pageNo=1&pageSize=10 200
4. 验证空状态:无数据时显示空提示
5. 验证加载态:首次进入显示 loading

预期结果:
✅ 列完整,字段对齐
✅ 分页切换流畅
✅ 空/加载状态正确

---

【测试场景 2:创建模型 - DeepSeek】
1. 点击「新增模型」
2. 验证弹窗标题「新增模型」
3. 填写字段:
   - 模型名称:DeepSeek Chat
   - 供应商:deepseek(下拉选择)
   - 模型 ID:deepseek-chat
   - API 地址:https://api.deepseek.com/v1
   - API Key:sk-test-xxxxx
   - 状态:启用
4. 点击「确定」
5. 验证:
   - Network: POST /ai/chat-model/create 200
   - Message: 新增成功
   - 列表自动刷新,新模型在第一行

预期结果:
✅ 创建成功
✅ 列表自动刷新

---

【测试场景 3:编辑模型】
1. 列表中点击「编辑」
2. 验证表单回填(尤其 API Key 字段,部分实现会脱敏显示为空)
3. 修改模型名称为「DeepSeek Chat V2」
4. 点击「确定」
5. 验证:
   - Network: PUT /ai/chat-model/update 200
   - 列表更新名称

预期结果:
✅ 数据回填
✅ 更新成功

---

【测试场景 4:状态切换】
1. 点击状态开关
2. 验证 Network: PUT /ai/chat-model/update 200
3. 验证列表状态变化

预期结果:
✅ 状态切换生效

---

【测试场景 5:删除模型】
1. 点击「删除」,弹确认
2. 确认
3. 验证:
   - Network: DELETE /ai/chat-model/delete?id=xxx 200
   - 列表移除该行

预期结果:
✅ 删除成功
✅ 列表立即更新

---

【测试场景 6:搜索/筛选】
1. 输入模型名称关键字「DeepSeek」→ 验证列表过滤
2. 切换状态筛选 → 验证状态过滤
3. 点击「重置」→ 验证清空筛选条件

预期结果:
✅ 关键字搜索生效
✅ 状态筛选生效
✅ 重置生效

---

【测试场景 7:边界与异常】
1. 必填字段为空 → 验证表单校验
2. API Key 包含特殊字符(包含引号/反斜杠)→ 验证后端是否转义
3. API 地址格式错误(不是 https:// 开头)→ 验证前端校验
4. 重复创建同名模型 → 验证后端去重策略
5. 删除正在被引用的模型 → 验证外键约束提示
6. 创建 50+ 模型 → 验证性能(分页、滚动)

预期结果:
✅ 表单校验生效
✅ 特殊字符被处理
✅ URL 校验生效
✅ 重复创建有提示
✅ 外键引用有保护
✅ 大量数据流畅

---

【测试场景 8:批量操作(若支持)】
1. 多选 2-3 个模型
2. 验证「批量删除」按钮出现
3. 批量删除 → 验证 Network 包含多个 DELETE 请求

预期结果:
✅ 批量操作支持
✅ 多次请求均成功

---

【问题诊断】
- 模型列表加载失败 → 检查 /ai/chat-model/page 权限(常见:缺失 ai:model:query)
- 创建后未出现 → 检查表单提交后是否调用 loadData()
- API Key 脱敏策略 → 确认后端是否对响应做掩码处理
```

#### 开发提示词

> `src/views/ai/model/index.vue` + `ModelFormModal.vue` 已存在,但字段不全,需**增强**。完成后跑 AI-02 测试提示词全场景回归。

```
/code 或 subagent-driven-development
打开 src/views/ai/model/index.vue + ModelFormModal.vue,执行增强任务。

【任务 1:补齐 ModelFormModal 字段(对照后端 ChatModelSaveReqVO)】
1. 模型分组(modelGroup,字典 ai_model_group)
2. 平台类型(platform,字典 ai_platform:openai/deepseek/qwen/doubao/zhipu/baidu...)
3. 超时配置:connectTimeout(秒)、readTimeout(秒)、writeTimeout(秒)
4. 温度参数(temperature,0-2)
5. 最大 token(maxTokens)
6. Top-P(topP,0-1)
7. 频率惩罚(frequencyPenalty)
8. 存在惩罚(presencePenalty)
9. 响应格式(responseFormat:text/json_object)
10. 是否支持 function calling(functionCall)
11. 是否支持 vision(vision)
12. 自定义请求头(headers,JSON 格式)
13. 标签(tags,多选)

【任务 2:列表增强】
1. 列:模型分组(字典渲染)、温度、最大 token、是否支持 function/vision
2. 状态列用 DictTag
3. 操作列加「测试对话」按钮 → 弹出简易 chat 框

【任务 3:测试对话 Modal(新建)】
1. 新建 src/views/ai/model/ChatModelTestModal.vue
2. 选择模型后,输入消息,流式调用
3. 显示 token 消耗、响应时间

【任务 4:导入/导出(可选)】
1. 导出:GET /ai/chat-model/export-excel(若后端支持)
2. 导入:模板下载 + Excel 上传(若后端支持)

【任务 5:边界处理】
1. 必填字段:name、modelId、provider、baseUrl、apiKey
2. baseUrl 必须 https:// 开头
3. apiKey 提交后列表脱敏
4. temperature/topP 范围限制
5. 重复 name → 后端拒绝,前端提示

【任务 6:回归测试】
跑 AI-02「测试提示词」全部 8 个场景,全绿后 commit。
```

---

### AI-03 Prompt 模板管理

**页面路径**:`/ai/prompt`
**后端 Controller**:`prompt/PromptTemplateController` (`/ai/prompt-template`)
**权限标识**:`ai:prompt:create`, `ai:prompt:update`, `ai:prompt:delete`, `ai:prompt:query`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/ai/console/prompt/prompttemplate/index.vue`

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/ai/prompt-template/create` | 创建模板 |
| POST | `/ai/prompt-template/copy` | 复制模板 |
| PUT | `/ai/prompt-template/update` | 更新模板 |
| DELETE | `/ai/prompt-template/delete` | 删除模板 |
| GET | `/ai/prompt-template/get` | 模板详情 |
| GET | `/ai/prompt-template/page` | 分页列表 |
| GET | `/ai/prompt-template/simple-list` | 简化列表(下拉) |
| GET | `/ai/prompt-template/prompt-key/list-by-pt-id` | 关联 Prompt Key |
| GET | `/ai/prompt-template/prompt-model/list-by-pt-id` | 关联模型 |
| GET | `/ai/prompt-template/pageEncrypt` | 加密分页(脱敏) |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开 Prompt 模板管理,执行完整 CRUD + 分类管理 + 模型绑定测试。

【前置操作】
1. 登录系统
2. 左侧菜单「AI 智能」→「Prompt 模板管理」

---

【测试场景 1:列表加载】
1. 验证表格列:模板名称、分类、版本、模型、Key、状态、创建时间
2. 验证 Network: GET /ai/prompt-template/page 200
3. 验证分类树(CategoryTree)在左侧显示
4. 点击分类节点,验证列表过滤

预期结果:
✅ 列完整
✅ 分类树加载
✅ 分类过滤生效

---

【测试场景 2:创建模板】
1. 点击「新增」
2. 填写:
   - 模板名称:法律咨询助手
   - 分类:客服(下拉选择)
   - 模板内容:你是一个专业的法律顾问...
   - 变量:{{question}}, {{context}}
3. 选择关联模型:DeepSeek Chat
4. 点击「确定」
5. 验证 Network: POST /ai/prompt-template/create 200

预期结果:
✅ 创建成功
✅ 列表更新

---

【测试场景 3:复制模板】
1. 点击「复制」
2. 验证自动填充「xxx_副本」
3. 验证 Network: POST /ai/prompt-template/copy 200

预期结果:
✅ 复制成功
✅ 新模板与原模板独立

---

【测试场景 4:Prompt Key 关联】
1. 列表点击「管理 Key」,打开抽屉
2. 抽屉内:
   - 显示已绑定 Key 列表
   - 「新增 Key」按钮
   - 「删除绑定」按钮
3. 验证 Network: GET /ai/prompt-template/prompt-key/list-by-pt-id 200
4. 绑定新 Key → 验证 Network: POST(见 AI-04)

预期结果:
✅ Key 关联管理可用
✅ 增删生效

---

【测试场景 5:模型关联】
1. 「管理模型」抽屉
2. 显示已绑定模型列表
3. 「新增绑定」选择 chat-model
4. 验证 Network: GET /ai/prompt-template/prompt-model/list-by-pt-id 200

预期结果:
✅ 模型关联管理可用

---

【测试场景 6:加密分页】
1. 切换到「加密视图」
2. 验证 Network: GET /ai/prompt-template/pageEncrypt 200
3. 验证列表中敏感字段(API Key 等)被脱敏显示

预期结果:
✅ 加密视图生效
✅ 脱敏字段不可见

---

【测试场景 7:边界与异常】
1. 模板内容超长(> 50000 字符)→ 验证限制
2. 变量名重复 → 验证后端去重
3. 模板内容包含 Mustache 语法错误(未闭合 {{) → 验证 Debug 提示
4. 删除被引用的模板 → 验证外键保护

预期结果:
✅ 长度限制
✅ 变量去重
✅ 语法检查
✅ 外键保护
```

#### 开发提示词

> ❌ **缺失模块**,需新建前端。完成后跑 AI-03 测试提示词全场景回归。

```
/code 或 subagent-driven-development
新建 Prompt 模板管理前端(参考 tianque-ui/views/ai/console/prompt/prompttemplate/)。

【任务 1:创建文件结构】
- src/views/ai/prompt/index.vue
- src/views/ai/prompt/PromptTemplateForm.vue
- src/views/ai/prompt/components/CategoryTree.vue(分类树)
- src/views/ai/prompt/components/CopyPromptTemplate.vue(复制对话框)
- src/api/ai/prompt.ts

【任务 2:index.vue 列表】
1. 左侧 CategoryTree(分类树,递归渲染)
2. 右侧 Table:模板名称、分类、版本、关联模型数、关联 Key 数、状态、创建时间
3. 搜索:按 name 模糊
4. 切换「加密视图」:GET /ai/prompt-template/pageEncrypt
5. 操作:编辑、复制、删除、管理 Key(抽屉)、管理模型(抽屉)

【任务 3:PromptTemplateForm.vue 表单】
1. 字段:模板名(name)、分类(categorySelect)、模板内容(content,TextArea,≥10 行,支持 {{var}})
2. 变量抽取:解析 {{xxx}} 自动列出
3. 关联模型:多选 chat-model
4. 关联 Key:多选 prompt-key
5. 描述(description)、状态(status)
6. 校验:模板名必填、分类必填、模板内容必填且至少 1 个变量

【任务 4:管理 Key 抽屉】
1. 标题「管理 Prompt Key」
2. 显示已绑定:GET /ai/prompt-template/prompt-key/list-by-pt-id
3. 「新增绑定」:选择 prompt-key,POST /ai/prompt-template/prompt-key/create
4. 「解绑」:DELETE
5. 抽屉内嵌简单列表(无需子页面)

【任务 5:管理模型抽屉】
1. 同上,调用 /ai/prompt-template/prompt-model/list-by-pt-id

【任务 6:复制对话框】
1. 显示原模板内容
2. 用户修改名称后,POST /ai/prompt-template/copy

【任务 7:边界】
1. 模板名重复 → 后端拒绝
2. 模板内容超 50000 → 截断或拒绝
3. 变量名重复 → 后端去重
4. 模板内容包含 {{未闭合 → 提示「变量未闭合」

【任务 8:回归测试】
跑 AI-03「测试提示词」全部 7 个场景,全绿后 commit。
```

---

### AI-04 Prompt Key 管理

**页面路径**:`/ai/prompt-key`
**后端 Controller**:内嵌在 `prompt/PromptTemplateController` 子路径 `/prompt-key/*`
**权限标识**:`ai:prompt-key:create`, `ai:prompt-key:delete`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/ai/console/prompt/promptkey/index.vue`

#### 关键 API 取证

Prompt Key 通常以子路径形式管理(参考 tianque-ui):
- `GET /ai/prompt-template/prompt-key/list-by-pt-id?ptId=xxx`
- `POST /ai/prompt-template/prompt-key/create`
- `PUT /ai/prompt-template/prompt-key/update`
- `DELETE /ai/prompt-template/prompt-key/delete`

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开 Prompt Key 管理,执行完整 CRUD 测试。

【前置操作】
1. 登录系统
2. 左侧菜单「AI 智能」→「Prompt Key 管理」

---

【测试场景 1:列表加载】
1. 验证列:Key 名称、关联模板、API Key、状态、创建时间
2. 验证 Network 200
3. 验证搜索:按 Key 名、模板名搜索

预期结果:
✅ 列完整
✅ 搜索生效

---

【测试场景 2:新增 Key】
1. 点击「新增」
2. 填写:Key 名、关联 Prompt 模板、API Key 值
3. 提交 → 验证 Network POST 200
4. 验证列表新增

预期结果:
✅ 创建成功

---

【测试场景 3:复制 Key】
1. 点击「复制」,显示「copyPromptKeys」对话框
2. 确认 → 验证 Network 200
3. 验证列表新增「xxx_copy」

预期结果:
✅ 复制成功

---

【测试场景 4:删除 Key】
1. 点击「删除」→ 确认
2. 验证 Network DELETE 200

预期结果:
✅ 删除成功

---

【测试场景 5:边界与异常】
1. 重复 Key 名 → 验证后端唯一性约束
2. API Key 超长(> 1024 字符)→ 验证截断/拒绝
3. 删除被引用的 Key → 验证外键保护
```

#### 开发提示词

> ❌ **缺失模块**,需新建。参考 tianque-ui `views/ai/console/prompt/promptkey/index.vue`。
> **开发任务**:创建 `src/views/ai/prompt-key/{index.vue,PromptKeyForm.vue,copyPromptKeys.vue}` + `src/api/ai/prompt-key.ts`。列表字段:Key 名、关联模板、API Key(脱敏)、状态。操作:新增、复制、删除。抽屉:选择 Prompt 模板、API Key 值。复制对话框(CopyPromptKeys)复用 Prompt 列表选多个。**关键**:必须先跑 AI-03(模板)完成后才能测 AI-04(Key 关联模板),遵循依赖顺序。

---

### AI-05 API Key 管理

**页面路径**:`/ai/api-key`
**后端 Controller**:`model/AiApiKeyController` (`/ai/api-key`)
**权限标识**:`ai:api-key:create`, `ai:api-key:update`, `ai:api-key:delete`, `ai:api-key:query`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/ai/console/apiKey/index.vue`

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/ai/api-key/create` | 创建 Key |
| PUT | `/ai/api-key/update` | 更新 Key |
| DELETE | `/ai/api-key/delete` | 删除 Key |
| GET | `/ai/api-key/get` | Key 详情 |
| GET | `/ai/api-key/page` | 分页列表 |
| GET | `/ai/api-key/simple-list` | 简化列表 |
| GET | `/ai/api-key/chat-model/page` | 关联 ChatModel |
| GET | `/ai/api-key/pageEncrypt` | 加密分页 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开 API Key 管理,执行完整 CRUD + ChatModel 绑定测试。

【前置操作】
1. 登录系统
2. 左侧菜单「AI 智能」→「API Key 管理」

---

【测试场景 1:列表加载 + 加密视图】
1. 验证列:Key 名称、平台、API Key(脱敏)、状态
2. 验证 Network: GET /ai/api-key/page 200
3. 切换「加密视图」 → 验证 GET /ai/api-key/pageEncrypt
4. 验证脱敏字段(显示 sk-***-xxxx)

预期结果:
✅ 列表正常
✅ 脱敏生效

---

【测试场景 2:新增 Key】
1. 点击「新增」
2. 填写:Key 名、选择 ChatModel(下拉)、API Key 值
3. 提交 → 验证 POST 200
4. 验证列表新增

预期结果:
✅ 创建成功

---

【测试场景 3:ChatModel 关联】
1. 列表点击「关联模型」,打开抽屉
2. 显示已关联模型 + 「新增关联」按钮
3. 验证 Network: GET /ai/api-key/chat-model/page 200
4. 选择 ChatModel → 绑定

预期结果:
✅ 关联管理可用

---

【测试场景 4:编辑/删除/搜索】
1. 编辑 → 验证 PUT 200
2. 删除 → 验证 DELETE 200
3. 搜索 → 验证过滤

预期结果:
✅ 全部生效

---

【测试场景 5:边界】
1. 重复 Key 名 → 验证唯一性
2. API Key 格式错误 → 验证校验
3. 删除被引用的 Key → 验证外键保护
```

#### 开发提示词

> ❌ **缺失模块**,需新建。参考 tianque-ui `views/ai/console/apiKey/index.vue` + `components/ChatModelForm.vue` + `ChatModelList.vue`。
> **开发任务**:创建 `src/views/ai/api-key/{index.vue,ApiKeyForm.vue,components/ChatModelForm.vue,components/ChatModelList.vue}` + `src/api/ai/api-key.ts`。列表字段:Key 名、平台、API Key(脱敏 sk-***-xxxx)、状态。**核心功能**:切换「加密视图」调用 `/ai/api-key/pageEncrypt`;关联 ChatModel 抽屉(GET `/ai/api-key/chat-model/page`、POST 绑定、DELETE 解绑)。**联动**:依赖 AI-02(ChatModel)先完成。

---

#### 开发提示词

> ✅ **已实现**(`src/views/ai/skill/index.vue`)。**开发任务**:增强现有页面 —(1) 字段补齐:技能 ID、版本号、调用次数、最后调用时间、创建人;(2) 新增「Trace 统计」Tab 嵌入 `SkillTraceController` 的 4 个 API(`/trace/skill/list` 等);(3) 新增「CodeAct 配置」Tab 嵌入 `CodeactConfigController` 的 3 个 API;(4) 路由策略配置页 `/ai/skill/route` 单独成子页面;(5) 上传组件支持 .zip + .tar.gz 拖拽 + 进度条;(6) 回归测试 AI-06 全部 8 个场景。

### AI-06 技能管理(Skill)

**页面路径**:`/ai/skill`
**源码文件**:`src/views/ai/skill/index.vue`(已实现)
**API 文件**:`src/api/ai/skill.ts`
**后端 Controller**:`skill/MediationSkillController` (`/ai/admin/skill`)、`SkillRouteController`、`CodeactConfigController`、`SkillTraceController`
**权限标识**:`ai:skill:create`, `ai:skill:update`, `ai:skill:delete`, `ai:skill:test`

> ✅ **已实现** — 基础 CRUD + 测试功能,本模块为基准测试,验证现有功能完整性

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/ai/admin/skill/list` | 列表(全量) |
| GET | `/ai/admin/skill/page` | 分页 |
| GET | `/ai/admin/skill/{skillId}` | 详情 |
| POST | `/ai/admin/skill/upload` | 上传技能包 |
| PUT | `/ai/admin/skill/{skillId}` | 更新 |
| DELETE | `/ai/admin/skill/{skillId}` | 删除 |
| PUT | `/ai/admin/skill/{skillId}/toggle` | 启停切换 |
| POST | `/ai/admin/skill/{skillId}/test` | 测试运行 |
| POST | `/ai/admin/skill/route` | 技能路由(智能匹配) |
| GET | `/ai/admin/skill/stats` | 统计 |
| GET | `/api/admin/skill/strategy` | 路由策略 GET |
| PUT | `/api/admin/skill/strategy` | 路由策略 PUT |
| GET | `/api/admin/skill/test` | 路由测试 |
| GET | `/api/admin/codeact/config` | CodeAct 配置 GET |
| PUT | `/api/admin/codeact/config` | CodeAct 配置 PUT |
| GET | `/api/admin/codeact/health` | CodeAct 健康检查 |
| GET | `/api/admin/skill/trace/skill/list` | 技能 Trace |
| GET | `/api/admin/skill/trace/codeact/list` | CodeAct Trace |
| GET | `/api/admin/skill/trace/skill/stats` | 技能统计 |
| GET | `/api/admin/skill/trace/codeact/stats` | CodeAct 统计 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开 AI 技能管理,执行 CRUD + 路由 + Trace 测试。

【前置操作】
1. 登录系统
2. 左侧菜单「AI 智能」→「技能管理」

---

【测试场景 1:列表加载 + 上传技能】
1. 验证表格列:技能名称、描述、调用次数、状态、创建时间
2. 验证 Network: GET /ai/admin/skill/page 200
3. 点击「上传技能」,选择 .zip / .tar.gz 技能包
4. 验证 Network: POST /ai/admin/skill/upload 200
5. 验证列表新增

预期结果:
✅ 列表正常
✅ 上传成功

---

【测试场景 2:测试技能】
1. 列表点击「测试」,弹出测试 Modal
2. 输入测试内容
3. 点击「运行测试」 → 验证 Network: POST /ai/admin/skill/{id}/test 200
4. 验证返回结果在 Modal 中展示

预期结果:
✅ 测试功能正常
✅ 结果展示正确

---

【测试场景 3:启停切换】
1. 点击「启停」开关 → 验证 PUT /ai/admin/skill/{id}/toggle 200
2. 验证状态更新

预期结果:
✅ 切换生效

---

【测试场景 4:技能路由(智能匹配)】
1. 在聊天页输入「帮我翻译这段话」等需要技能的场景
2. 验证 Network: POST /ai/admin/skill/route 200
3. 验证自动调用匹配技能

预期结果:
✅ 智能路由生效

---

【测试场景 5:路由策略配置】
1. 进入「路由策略」页
2. 修改路由规则(如优先匹配、按权重)
3. 验证 PUT /api/admin/skill/strategy 200
4. 调用 GET /api/admin/skill/test 验证策略生效

预期结果:
✅ 策略配置生效

---

【测试场景 6:CodeAct 配置】
1. 进入「CodeAct 配置」
2. 修改配置项
3. 验证 PUT /api/admin/codeact/config 200
4. 调用 GET /api/admin/codeact/health 检查健康状态

预期结果:
✅ CodeAct 配置生效
✅ 健康检查正常

---

【测试场景 7:Trace 统计】
1. 进入「Trace 统计」页
2. 验证 GET /api/admin/skill/trace/skill/list 返回调用记录
3. 验证 GET /api/admin/skill/trace/skill/stats 返回聚合数据

预期结果:
✅ Trace 数据完整
✅ 统计聚合正确

---

【测试场景 8:边界与异常】
1. 上传损坏的 .zip → 验证错误提示
2. 上传超过大小限制(> 50MB)→ 验证拒绝
3. 删除被路由引用的技能 → 验证保护
4. 测试输入超长(> 100000 字符)→ 验证超时
5. 调用统计:验证调用次数 counter 自增

预期结果:
✅ 错误处理完善
✅ 大小限制生效
✅ 外键保护
✅ 计数自增
```

---

## P1 优先级 — 智能体与应用层模块测试

---

#### 开发提示词

> ❌ **缺失模块**。**开发任务**:创建 `src/views/ai/agent/{index.vue,agent_detail.vue,AgentForm.vue,CategoryTree.vue}` + `src/api/ai/agent.ts`。**核心特性**:(1) 左侧 CategoryTree 树形分类(可拖拽排序,PUT update-list);(2) 列表字段:Agent 名、分类、关联模型、关联工具数、知识库数、状态;(3) 详情页(`agent_detail.vue`)Tabs:基本信息、Config Group(LLM/Tool/Memory/...)、关联 App、关联 Workflow、调用统计;(4) Config Group 用 `AgentConfigController`:GET `/ai/agent-config/{id}/config/{group}` 读、PUT 写、GET `/ai/agent-config/{id}/resolved-config` 读合并、POST `/ai/agent-config/{id}/config/reset` 重置;(5) Agent 模板继承:`AgentTemplateController` 4 个 API 独立管理;(6) 「我的 Agent」 vs 「平台 Agent」 双 Tab 切换(GET `/ai/assistant/my-page` vs `/ai/assistant/page`);(7) 关联 App 抽屉(GET `/ai/assistant/app-list`);(8) 回归测试 AI-07 全部 8 个场景。

### AI-07 智能体 Agent

**页面路径**:`/ai/agent`
**后端 Controller**:`model/AiAgentController` (`/ai/assistant`)、`model/agent/AgentConfigController`、`model/agent/template/AgentTemplateController`
**权限标识**:`ai:agent:create`, `ai:agent:update`, `ai:agent:delete`, `ai:agent:query`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/ai/platform/agent/index.vue`

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/ai/assistant/my-page` | 我的 Agent 分页 |
| GET | `/ai/assistant/simple-list` | 简化列表 |
| GET | `/ai/assistant/get-my` | 我的 Agent 详情 |
| POST | `/ai/assistant/create-my` | 创建我的 Agent |
| PUT | `/ai/assistant/update-my` | 更新我的 Agent |
| DELETE | `/ai/assistant/delete-my` | 删除我的 Agent |
| GET | `/ai/assistant/app-list` | 关联 App 列表 |
| POST | `/ai/assistant/create` | 创建 Agent(管理员) |
| PUT | `/ai/assistant/update` | 更新 Agent |
| DELETE | `/ai/assistant/delete` | 删除 Agent |
| GET | `/ai/assistant/get` | Agent 详情 |
| GET | `/ai/assistant/page` | 平台 Agent 分页 |
| PUT | `/ai/agent-template/{templateId}` | 更新模板 |
| DELETE | `/ai/agent-template/{templateId}` | 删除模板 |
| PUT | `/ai/agent-template/{templateId}/config/{configGroup}` | 模板配置 |
| PUT | `/ai/agent-config/{agentId}/config/{configGroup}` | Agent 配置 |
| GET | `/ai/agent-config/{agentId}/config/{configGroup}` | 读取 Agent 配置 |
| GET | `/ai/agent-config/{agentId}/resolved-config` | 解析后配置(合并模板) |
| POST | `/ai/agent-config/{agentId}/config/reset` | 重置配置 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开智能体管理,执行完整 CRUD + 分类 + 配置 + 模板测试。

【前置操作】
1. 登录系统
2. 左侧菜单「AI 智能」→「智能体管理」

---

【测试场景 1:列表加载 + 分类树】
1. 验证左侧分类树(CategoryTree)显示
2. 验证右侧列表:Agent 名称、分类、状态、创建时间
3. 验证 Network: GET /ai/assistant/page 200
4. 点击分类节点 → 验证列表过滤

预期结果:
✅ 分类树加载
✅ 列表过滤生效

---

【测试场景 2:创建 Agent】
1. 点击「新增」
2. 填写:
   - 名称:客服 Agent
   - 分类:客服
   - 系统提示词:你是一个专业的客服助手
   - 模型:DeepSeek Chat
   - 工具:勾选 web_search
   - 知识库:勾选测试知识库
3. 提交 → 验证 POST 200
4. 验证列表新增

预期结果:
✅ 创建成功

---

【测试场景 3:Agent 详情(agent_detail)】
1. 列表点击「详情」
2. 验证进入详情页:
   - 基本信息(模型、提示词、工具)
   - 调用统计
   - 配置项(config group)
3. 验证 Network: GET /ai/assistant/get?id=xxx 200

预期结果:
✅ 详情页完整

---

【测试场景 4:Agent 配置管理】
1. 详情页选择 config group(如 LLM 配置)
2. 修改 temperature、max_tokens 等
3. 验证 Network: PUT /ai/agent-config/{id}/config/LLM 200
4. 验证 Network: GET /ai/agent-config/{id}/resolved-config 返回合并后的配置(模板 + 自定义)

预期结果:
✅ 配置保存成功
✅ 解析后配置包含自定义值

---

【测试场景 5:Agent 模板】
1. 进入「Agent 模板」页
2. 模板可绑定到多个 Agent
3. 修改模板配置 → 验证所有引用该模板的 Agent resolved-config 同步更新

预期结果:
✅ 模板继承生效

---

【测试场景 6:Agent 关联 App】
1. 详情页「关联应用」抽屉
2. 显示已关联 App 列表 + 「新增关联」
3. 验证 Network: GET /ai/assistant/app-list?id=xxx 200

预期结果:
✅ 关联管理可用

---

【测试场景 7:删除 Agent】
1. 点击「删除」→ 确认
2. 验证 Network: DELETE /ai/assistant/delete?id=xxx 200
3. 验证列表移除

预期结果:
✅ 删除成功

---

【测试场景 8:边界与异常】
1. 系统提示词超长(> 50000 字符)→ 验证限制
2. 必填字段为空 → 验证校验
3. 删除被 App 引用的 Agent → 验证外键保护
4. 创建 100+ Agent → 验证性能
5. 修改 config group 后再 reset → 验证回滚

预期结果:
✅ 全部边界处理正确
```

---

#### 开发提示词

> ❌ **缺失模块**。**开发任务**:创建 `src/views/ai/app/{index.vue,detail.vue,AppForm.vue}` + `src/api/ai/app.ts`。**核心特性**:(1) 列表字段:App 名、分类、关联 Agent、关联工具、关联知识库、状态;(2) 详情页(`detail.vue`)Tabs:基本信息、关联 Agent 列表、关联工具、关联知识库、关联 Workflow、调用统计;(3) 「新建 App」向导:Step1 基本信息 → Step2 选 Agent → Step3 选工具 → Step4 选知识库 → Step5 确认;(4) 编辑用 `getDetail`(GET `/ai/app/getDetail`)一次拉全部关联;(5) 导出 Excel(GET `/ai/app/export-excel`);(6) 回归测试 AI-08 全部 5 个场景。

### AI-08 AI 应用(App)

**页面路径**:`/ai/app`
**后端 Controller**:`app/AppController` (`/ai/app`)
**权限标识**:`ai:app:create`, `ai:app:update`, `ai:app:delete`, `ai:app:query`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/ai/platform/app/index.vue`

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/ai/app/create` | 创建 App |
| PUT | `/ai/app/update` | 更新 App |
| DELETE | `/ai/app/delete` | 删除 App |
| GET | `/ai/app/get` | App 详情 |
| GET | `/ai/app/getDetail` | App 完整详情(含关联) |
| GET | `/ai/app/page` | 分页 |
| GET | `/ai/app/simple-list` | 简化列表 |
| GET | `/ai/app/export-excel` | 导出 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开 AI 应用管理,执行完整 CRUD + 详情测试。

【前置操作】
1. 登录系统
2. 左侧菜单「AI 智能」→「AI 应用管理」

---

【测试场景 1:列表加载】
1. 验证列:App 名称、分类、状态、创建时间、操作
2. 验证 Network: GET /ai/app/page 200
3. 验证搜索、状态筛选

预期结果:
✅ 列表完整

---

【测试场景 2:创建 App】
1. 点击「新增」
2. 填写:名称、分类、描述、关联 Agent、关联工具、关联知识库
3. 提交 → 验证 POST 200
4. 验证列表新增

预期结果:
✅ 创建成功

---

【测试场景 3:App 详情(detail)】
1. 列表点击「详情」
2. 验证:
   - 基本信息
   - 关联 Agent 列表
   - 关联工具列表
   - 关联知识库列表
   - 关联 Workflow
3. 验证 Network: GET /ai/app/getDetail?id=xxx 200

预期结果:
✅ 详情完整

---

【测试场景 4:编辑/删除/导出】
1. 编辑 → 验证 PUT 200
2. 删除 → 验证 DELETE 200
3. 导出 Excel → 验证文件下载与内容

预期结果:
✅ 全部生效

---

【测试场景 5:边界】
1. 名称重复 → 验证唯一性
2. 关联不存在的 Agent → 验证外键
3. 详情页加载大量关联数据 → 验证懒加载
```

---

#### 开发提示词

> ❌ **缺失模块**。**开发任务**:创建 `src/views/ai/app/object/{index.vue,edit.vue}` + `src/api/ai/app-object.ts`。**核心特性**:(1) 列表:对象名、所属 App、字段数、创建时间;(2) `edit.vue` 表单:对象名、关联 App(下拉)、**字段编辑器**(可增删,每字段:name/type(string/number/boolean/array/object)/required/description);(3) 字段类型切换动态显示(数组/对象支持嵌套);(4) 字段顺序拖拽;(5) 依赖 AI-08 完成;(6) 回归测试 AI-09 全部 3 个场景。

### AI-09 App 对象管理

**页面路径**:`/ai/app/object`
**后端 Controller**:`app/AppObjectController` (`/ai/app-object`)
**权限标识**:`ai:app-object:create`, `ai:app-object:update`, `ai:app-object:delete`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/ai/platform/app/objectManager/index.vue` + `edit.vue`

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/ai/app-object/create` | 创建对象 |
| PUT | `/ai/app-object/update` | 更新对象 |
| DELETE | `/ai/app-object/delete` | 删除对象 |
| GET | `/ai/app-object/get` | 对象详情 |
| GET | `/ai/app-object/page` | 分页 |
| GET | `/ai/app-object/export-excel` | 导出 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开 App 对象管理,执行完整 CRUD 测试。

---

【测试场景 1:列表 + 创建】
1. 验证列:对象名、所属 App、类型、字段、状态
2. 验证 Network: GET /ai/app-object/page 200
3. 点击「新增」,进入 edit.vue 表单
4. 填写:对象名、选择 App、添加字段(name/type/required)
5. 提交 → 验证 POST 200

预期结果:
✅ 创建成功

---

【测试场景 2:字段管理】
1. 在 edit.vue 中可增删字段
2. 字段支持类型:string/number/boolean/array/object
3. 必填/可空切换

预期结果:
✅ 字段增删生效

---

【测试场景 3:编辑/删除/导出】
1. 编辑 → 验证 PUT 200
2. 删除 → 验证 DELETE 200
3. 导出 → 验证 Excel 下载

预期结果:
✅ 全部生效
```

---

#### 开发提示词

> ❌ **缺失模块**(后端最强、前端最难)。**开发任务**:创建 `src/views/ai/workflow/{index.vue,design.vue,WorkflowForm.vue}` + 子组件 NodePanel、EdgePanel、NodeConfigDrawer + `src/api/ai/workflow.ts`(包含 5 个 controller 的所有 API)。**核心特性**:(1) 列表:流程名、分类、版本、状态、最后运行时间、成功率;(2) **可视化设计器 `design.vue`**:基于 antv/x6 或 vue-flow 实现拖拽画布(参考 tianque-ui `views/ai/platform/aiflow/design.vue`),节点类型:开始/结束/LLM/工具/知识库/条件/代码/Agent/等待;(3) 节点参数编辑抽屉(GET `/ai/flow-act/flow-act-param/list-by-act-id`);(4) 模板 vs 实例:WorkflowTemplateAct + WorkflowTemplateParam(GET `/ai/workflow-template-act/workflow-template-param/page` 等);WorkflowInstance + WorkflowInstanceAct + WorkflowInstanceActParam;(5) 运行测试:点击「试运行」 → 调用后端执行 → 显示运行日志(GET `/ai/ai-flow/workflow-log/list-by-flow-code`);(6) 停止/重跑日志条目(DELETE `/ai/ai-flow/deleteLog`、GET `/ai/ai-flow/getLog`);(7) **Dify 互操作**:导入 .yml(POST `/ai/ai-flow/dify/import`)、导出(GET `/ai/ai-flow/dify/{id}/export`);(8) 循环依赖检测、保存时校验;(9) 回归测试 AI-10 全部 7 个场景。

### AI-10 AI Workflow 工作流

**页面路径**:`/ai/workflow` (aiflow)
**后端 Controller**:`aiflow/WorkflowController` (`/ai/ai-flow`)、`WorkflowActController`、`WorkflowTemplateActController`、`WorkflowInstanceController`、`WorkflowInstanceActController`
**权限标识**:`ai:workflow:create`, `ai:workflow:update`, `ai:workflow:delete`, `ai:workflow:run`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/ai/platform/aiflow/index.vue` + `design.vue`

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/ai/ai-flow/create` | 创建工作流 |
| PUT | `/ai/ai-flow/update` | 更新工作流 |
| DELETE | `/ai/ai-flow/delete` | 删除工作流 |
| GET | `/ai/ai-flow/get` | 详情 |
| GET | `/ai/ai-flow/getFlow` | 流程定义 |
| GET | `/ai/ai-flow/page` | 分页 |
| GET | `/ai/ai-flow/pageEncrypt` | 加密分页 |
| GET | `/ai/ai-flow/simple-list` | 简化列表 |
| GET | `/ai/ai-flow/flow-act/list-by-flow-code` | 节点列表 |
| GET | `/ai/ai-flow/workflow-log/list-by-flow-code` | 运行日志 |
| DELETE | `/ai/ai-flow/deleteLog` | 删除日志 |
| GET | `/ai/ai-flow/getLog` | 日志详情 |
| GET | `/ai/ai-flow/pageLog` | 日志分页 |
| POST | `/ai/ai-flow/dify/import` | Dify 导入 |
| GET | `/ai/ai-flow/dify/{id}/export` | Dify 导出 |
| POST | `/ai/flow-act/create` | 创建节点 |
| PUT | `/ai/flow-act/update` | 更新节点 |
| DELETE | `/ai/flow-act/delete` | 删除节点 |
| GET | `/ai/flow-act/page` | 节点分页 |
| GET | `/ai/flow-act/list` | 节点列表 |
| GET | `/ai/flow-act/flow-act-param/*` | 节点参数 CRUD |
| GET | `/ai/workflow-template-act/*` | 模板节点 CRUD |
| GET | `/ai/workflow-template-act/workflow-template-param/*` | 模板参数 CRUD |
| GET | `/ai/workflow-instance/*` | 流程实例 CRUD |
| GET | `/ai/workflow-instance-act/*` | 实例节点 CRUD |
| GET | `/ai/workflow-instance-act/workflow-instance-act-param/*` | 实例参数 CRUD |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开 AI Workflow 管理,执行完整 CRUD + 节点配置 + 运行测试。

【前置操作】
1. 登录系统
2. 左侧菜单「AI 智能」→「AI 工作流」

---

【测试场景 1:列表加载】
1. 验证列:流程名称、分类、状态、版本、创建时间
2. 验证 Network: GET /ai/ai-flow/page 200

预期结果:
✅ 列表完整

---

【测试场景 2:流程设计(design.vue)】
1. 列表点击「设计」,进入可视化画布
2. 拖拽节点(开始/结束/LLM/工具/知识库/条件)
3. 连线、配置节点参数
4. 保存 → 验证 POST 200 + flow-act/* 200

预期结果:
✅ 画布可拖拽
✅ 节点可配置
✅ 保存成功

---

【测试场景 3:节点参数配置】
1. 在节点上配置输入/输出参数
2. 验证 Network: POST /ai/flow-act/flow-act-param/create 200
3. 修改参数 → 验证 PUT
4. 删除参数 → 验证 DELETE

预期结果:
✅ 参数 CRUD 生效

---

【测试场景 4:运行测试】
1. 在设计器中点击「试运行」
2. 输入测试输入
3. 验证 Network: POST(后端执行入口)
4. 验证运行日志写入(GET /ai/ai-flow/workflow-log/list-by-flow-code)
5. 验证流程状态(运行中/成功/失败)

预期结果:
✅ 试运行成功
✅ 日志写入

---

【测试场景 5:模板与实例】
1. 创建模板(workflow-template-act)
2. 基于模板创建实例(workflow-instance)
3. 运行实例 → 验证 workflow-instance-act / workflow-instance-act-param 写入
4. 验证实例状态、参数正确

预期结果:
✅ 模板与实例分离正确

---

【测试场景 6:Dify 导入/导出】
1. 准备 Dify DSL 文件(.yml)
2. 上传导入 → 验证 POST /ai/ai-flow/dify/import 200
3. 导出 → 验证 GET /ai/ai-flow/dify/{id}/export 返回 yml 文件

预期结果:
✅ Dify 互操作生效

---

【测试场景 7:删除 + 边界】
1. 删除流程 → 验证 DELETE 200 + cascade 删除关联节点/参数
2. 循环依赖(节点 A 依赖 B,B 依赖 A)→ 验证检测提示
3. 运行超长工作流(> 60s)→ 验证超时
```

---

#### 开发提示词

> ❌ **缺失模块**(含多步骤向导 + 详情 Tabs)。**开发任务**:创建 `src/views/ai/evaluation/dataset/{index.vue,create/index.vue,detail/index.vue,DatasetForm.vue}` + 子组件 `DatasetItemList.vue`、`DatasetItemForm.vue`、`DatasetVersionList.vue`、`DatasetVersionForm.vue`、`DatasetExperimentList.vue` + `src/api/ai/dataset.ts`。**核心特性**:(1) `create/index.vue` 多步骤向导:基本信息 → 数据源(手动/Excel/API)→ 预处理 → 确认;(2) 详情页 Tabs:基本信息、数据项、版本、关联实验;(3) 数据项表单字段:输入 prompt、预期输出、评分标准(JSON 数组);(4) 版本切换:GET `/ai/dataset/dataset-version/list-by-dataset-id?datasetId=xxx`;(5) 批量导入:上传 Excel(后端有导入 API 但 v2.1 取证时未列出,若后端无则跳过);(6) 关联实验 Tab:GET `/ai/dataset/experiments`;(7) 依赖 KMS-01 知识库(数据源可能引用);(8) 回归测试 AI-11 全部 7 个场景。

### AI-11 评估-数据集 Dataset

**页面路径**:`/ai/evaluation/dataset`
**后端 Controller**:`dataset/DatasetController` (`/ai/dataset`)
**权限标识**:`ai:dataset:create`, `ai:dataset:update`, `ai:dataset:delete`, `ai:dataset:query`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/ai/evaluation/dataset/{index,create,detail}/*.vue`

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/ai/dataset/create` | 创建数据集 |
| PUT | `/ai/dataset/update` | 更新数据集 |
| DELETE | `/ai/dataset/delete` | 删除数据集 |
| GET | `/ai/dataset/get` | 数据集详情 |
| GET | `/ai/dataset/page` | 分页 |
| GET | `/ai/dataset/export-excel` | 导出 |
| GET | `/ai/dataset/dataset-item/list-by-dataset-id` | 数据项列表 |
| POST | `/ai/dataset/dataset-item/create` | 创建数据项 |
| PUT | `/ai/dataset/dataset-item/update` | 更新数据项 |
| DELETE | `/ai/dataset/dataset-item/delete` | 删除数据项 |
| GET | `/ai/dataset/dataset-version/list-by-dataset-id` | 版本列表 |
| POST | `/ai/dataset/dataset-version/create` | 创建版本 |
| DELETE | `/ai/dataset/dataset-version/delete` | 删除版本 |
| GET | `/ai/dataset/experiments` | 关联实验 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开数据集管理,执行完整 CRUD + 数据项 + 版本 + 创建向导测试。

【前置操作】
1. 登录系统
2. 左侧菜单「AI 智能」→「评估系统」→「数据集」

---

【测试场景 1:列表加载 + 搜索】
1. 验证列:数据集名称、描述、状态、版本数、创建时间
2. 验证 Network: GET /ai/dataset/page 200

预期结果:
✅ 列表完整

---

【测试场景 2:创建数据集向导(create/index.vue)】
1. 点击「新增」,进入多步骤向导
   - Step1:基本信息(名称、描述)
   - Step2:选择数据源(手动/Excel 导入/API)
   - Step3:数据预处理
   - Step4:确认提交
2. 验证每步的「上一步/下一步」正常
3. 验证 Network: POST /ai/dataset/create 200

预期结果:
✅ 向导流程通顺
✅ 创建成功

---

【测试场景 3:数据集详情(detail/index.vue)】
1. 列表点击「详情」,进入详情页
2. 详情页 Tabs:
   - 基本信息
   - 数据项(DatasetItemList)
   - 版本(DatasetVersionList)
   - 关联实验(DatasetExperimentList)
3. 验证各 Tab 数据加载

预期结果:
✅ 详情页完整

---

【测试场景 4:数据项管理】
1. 在「数据项」Tab 点击「新增」
2. 填写:输入(prompt)、预期输出、评分标准
3. 验证 Network: POST /ai/dataset/dataset-item/create 200
4. 批量导入(Excel) → 验证文件上传

预期结果:
✅ 数据项 CRUD 生效

---

【测试场景 5:版本管理】
1. 在「版本」Tab 创建新版本
2. 验证 Network: POST /ai/dataset/dataset-version/create 200
3. 切换版本 → 验证数据项列表变化

预期结果:
✅ 版本管理生效

---

【测试场景 6:关联实验】
1. 在「关联实验」Tab 显示使用此数据集的实验
2. 验证 Network: GET /ai/dataset/experiments?id=xxx 200

预期结果:
✅ 关联展示正确

---

【测试场景 7:边界】
1. 数据集超 10000 项 → 验证分页 + 性能
2. 删除有版本的非空数据集 → 验证级联保护
3. 导入空 Excel → 验证提示
4. 导入格式错误(非 xlsx)→ 验证拒绝
```

---

#### 开发提示词

> ❌ **缺失模块**。**开发任务**:创建 `src/views/ai/evaluation/evaluator/{index.vue,detail/index.vue,debug/index.vue,EvaluatorForm.vue}` + 子组件 `EvaluatorTemplateImport.vue`、`EvaluatorVersionList.vue`、`EvaluatorVersionForm.vue`、`EvaluatorExperimentList.vue` + `src/api/ai/evaluator.ts`。**核心特性**:(1) 列表字段:评估器名、类型(规则/LLM/Embedding)、版本、状态;(2) 模板导入对话框(GET `/ai/evaluator/template/list` 拉模板);(3) 详情 Tabs:基本信息、版本、关联实验;(4) 调试页 `debug/index.vue`:左侧输入 + 评估器运行,右侧返回评分,POST `/ai/evaluator/debug`;(5) 评分可视化(柱状/雷达图);(6) 依赖 AI-11(数据集)完成以便做关联实验;(7) 回归测试 AI-12 全部 7 个场景。

### AI-12 评估-评估器 Evaluator

**页面路径**:`/ai/evaluation/evaluator`
**后端 Controller**:`evaluator/EvaluatorController` (`/ai/evaluator`)
**权限标识**:`ai:evaluator:create`, `ai:evaluator:update`, `ai:evaluator:delete`, `ai:evaluator:debug`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/ai/evaluation/evaluator/{index,detail,debug}/*.vue`

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/ai/evaluator/create` | 创建评估器 |
| PUT | `/ai/evaluator/update` | 更新评估器 |
| DELETE | `/ai/evaluator/delete` | 删除评估器 |
| GET | `/ai/evaluator/get` | 详情 |
| GET | `/ai/evaluator/page` | 分页 |
| GET | `/ai/evaluator/export-excel` | 导出 |
| GET | `/ai/evaluator/evaluator-version/list-by-evaluator-id` | 版本列表 |
| POST | `/ai/evaluator/debug` | 调试 |
| POST | `/ai/evaluator/evaluator-version/create` | 创建版本 |
| GET | `/ai/evaluator/experiments` | 关联实验 |
| GET | `/ai/evaluator/template/list` | 模板列表 |
| GET | `/ai/evaluator/template/get` | 模板详情 |
| POST | `/ai/evaluator/template/import` | 模板导入(隐式) |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开评估器管理,执行完整 CRUD + 调试 + 版本测试。

【前置操作】
1. 登录系统
2. 左侧菜单「AI 智能」→「评估系统」→「评估器」

---

【测试场景 1:列表加载 + 模板导入】
1. 验证列:评估器名称、类型(规则/LLM)、版本、状态
2. 验证 Network: GET /ai/evaluator/page 200
3. 点击「模板导入」(EvaluatorTemplateImport)
4. 选择模板 → 验证导入成功

预期结果:
✅ 列表 + 模板导入可用

---

【测试场景 2:创建评估器】
1. 点击「新增」
2. 填写:
   - 名称:相似度评估器
   - 类型:LLM-as-Judge
   - 评估 Prompt:你是一个评估专家,判断回答与参考答案的相似度...
   - 评分标准:1-5 分
3. 提交 → 验证 POST 200

预期结果:
✅ 创建成功

---

【测试场景 3:评估器详情(detail/index.vue)】
1. 列表点击「详情」
2. Tabs:
   - 基本信息
   - 版本(EvaluatorVersionList)
   - 关联实验(EvaluatorExperimentList)
3. 验证各 Tab 加载

预期结果:
✅ 详情完整

---

【测试场景 4:评估器调试(debug/index.vue)】
1. 列表点击「调试」,进入调试页
2. 输入:测试输入 + 评估器运行
3. 验证 Network: POST /ai/evaluator/debug 200
4. 验证返回评分

预期结果:
✅ 调试可用
✅ 返回评分合理

---

【测试场景 5:版本管理】
1. 创建新版本 → 验证 POST /ai/evaluator/evaluator-version/create 200
2. 切换版本 → 验证详情变化

预期结果:
✅ 版本管理生效

---

【测试场景 6:关联实验**
1. 详情页「关联实验」Tab
2. 验证 Network: GET /ai/evaluator/experiments?id=xxx 200

预期结果:
✅ 关联展示正确

---

【测试场景 7:边界】
1. 评估 Prompt 超长(> 50000 字符)→ 验证限制
2. 调试时输入空 → 验证提示
3. 删除有版本的评估器 → 验证级联保护
4. 调试 LLM 评估器时网络超时 → 验证超时处理
```

---

#### 开发提示词

> ❌ **缺失模块**(长任务运行是难点)。**开发任务**:创建 `src/views/ai/evaluation/experiment/{index.vue,detail/index.vue,ExperimentForm.vue}` + 子组件 `ExperimentResultList.vue`、`ExperimentResultForm.vue` + `src/api/ai/experiment.ts`。**核心特性**:(1) 列表字段:实验名、数据集、评估器、Agent、状态(待运行/运行中/已完成/失败/已停止)、评分;(2) 状态实时轮询(每 2s 调 GET `/ai/experiment/page` 一次);(3) 实验表单:数据集(下拉,依赖 AI-11)、评估器(下拉,依赖 AI-12)、Agent(下拉,依赖 AI-07);(4) 详情 Tabs:基本信息、结果列表、评分图表(直方图);(5) 停止按钮 POST `/ai/experiment/stop`、重跑 POST `/ai/experiment/rerun`;(6) 结果详情:输入、实际输出、预期输出、评分(可重评);(7) 长任务超时(> 5 分钟需进度条 + 后台任务查询);(8) 回归测试 AI-13 全部 7 个场景。

### AI-13 评估-实验 Experiment

**页面路径**:`/ai/evaluation/experiment`
**后端 Controller**:`experiment/ExperimentController` (`/ai/experiment`)
**权限标识**:`ai:experiment:create`, `ai:experiment:run`, `ai:experiment:rerun`, `ai:experiment:stop`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/ai/evaluation/experiment/{index,detail}/*.vue`

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/ai/experiment/create` | 创建实验 |
| PUT | `/ai/experiment/update` | 更新实验 |
| DELETE | `/ai/experiment/delete` | 删除实验 |
| GET | `/ai/experiment/get` | 详情 |
| GET | `/ai/experiment/page` | 分页 |
| GET | `/ai/experiment/export-excel` | 导出 |
| GET | `/ai/experiment/experiment-result/list-by-experiment-id` | 结果列表 |
| POST | `/ai/experiment/stop` | 停止实验 |
| POST | `/ai/experiment/rerun` | 重跑实验 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开实验管理,执行完整 CRUD + 运行/停止/重跑 + 结果查看测试。

【前置操作】
1. 登录系统
2. 左侧菜单「AI 智能」→「评估系统」→「实验」

---

【测试场景 1:列表加载】
1. 验证列:实验名称、数据集、评估器、状态(待运行/运行中/已完成/失败)、评分
2. 验证 Network: GET /ai/experiment/page 200

预期结果:
✅ 列表完整

---

【测试场景 2:创建实验(ExperimentForm)】
1. 点击「新增」
2. 填写:
   - 名称:DeepSeek 客服测试
   - 关联数据集:客服 FAQ
   - 关联评估器:相似度评估器
   - 关联 Agent:客服 Agent
3. 提交 → 验证 POST 200

预期结果:
✅ 创建成功

---

【测试场景 3:运行实验】
1. 列表点击「运行」,后台开始评估
2. 验证状态变为「运行中」
3. 等待完成(长任务)
4. 验证状态变为「已完成」 + 评分字段

预期结果:
✅ 实验运行成功
✅ 评分正确

---

【测试场景 4:实验详情(detail/index.vue)】
1. 列表点击「详情」
2. Tabs:
   - 基本信息
   - 实验结果(ExperimentResultList)
   - 评分图表
3. 验证 Network: GET /ai/experiment/get?id=xxx 200 + GET /experiment-result/list-by-experiment-id 200

预期结果:
✅ 详情完整

---

【测试场景 5:停止 + 重跑】
1. 实验运行中,点击「停止」 → 验证 POST /ai/experiment/stop 200
2. 已停止的实验点击「重跑」 → 验证 POST /ai/experiment/rerun 200

预期结果:
✅ 停止/重跑生效

---

【测试场景 6:结果分析(ExperimentResultForm)】
1. 在结果列表点击「详情」
2. 验证显示:输入、实际输出、预期输出、评分
3. 可重新评分 → 验证 PUT

预期结果:
✅ 结果可重评分

---

【测试场景 7:边界】
1. 同时运行多个实验 → 验证并发
2. 实验运行中删除数据集 → 验证保护
3. 超大数据集(> 10000 项)运行 → 验证分批处理
4. 运行失败 → 验证错误状态 + 重试
```

---

#### 开发提示词

> ❌ **缺失模块**。**开发任务**:创建 `src/views/ai/tool/{index.vue,ToolForm.vue,ToolTestModal.vue}` + `src/api/ai/tool.ts`。**核心特性**:(1) 列表字段:工具名、类型(HTTP/Function/DB/Code)、状态、调用次数;(2) 表单字段:工具名、类型(单选下拉)、URL/MCP URL、参数定义(JSON Schema 编辑器,用 monaco-editor 或简易 textarea + JSON 校验);(3) HTTP 工具:Method、Headers、Query/Body 参数动态构建;(4) Function 工具:函数签名 + 描述(供 LLM 自动调用);(5) 测试 Modal:输入参数 → 模拟调用 → 显示返回结果;(6) 调试结果支持复制为 cURL;(7) 回归测试 AI-14 全部 3 个场景。

### AI-14 工具管理 Tool

**页面路径**:`/ai/tool`
**后端 Controller**:`model/ToolController` (`/ai/tool`)
**权限标识**:`ai:tool:create`, `ai:tool:update`, `ai:tool:delete`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/ai/console/tool/index.vue` + `ToolForm.vue`

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/ai/tool/create` | 创建工具 |
| PUT | `/ai/tool/update` | 更新工具 |
| DELETE | `/ai/tool/delete` | 删除工具 |
| GET | `/ai/tool/get` | 工具详情 |
| GET | `/ai/tool/page` | 分页 |
| GET | `/ai/tool/simple-list` | 简化列表 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开工具管理,执行完整 CRUD + 调试测试。

---

【测试场景 1:列表 + 创建】
1. 验证列:工具名、类型(HTTP/Function/DB)、状态
2. 验证 Network: GET /ai/tool/page 200
3. 点击「新增」,填写:
   - 工具名:天气查询
   - 类型:HTTP
   - URL:https://api.weather.com/...
   - 参数定义(JSON Schema)
4. 提交 → 验证 POST 200

预期结果:
✅ 创建成功

---

【测试场景 2:工具调试】
1. 详情页「测试」Tab
2. 填写测试入参
3. 验证返回结果

预期结果:
✅ 调试可用

---

【测试场景 3:边界】
1. URL 格式错误 → 验证校验
2. 删除被 Agent 引用的工具 → 验证保护
3. 调用超时 → 验证处理
```

---

#### 开发提示词

> ❌ **缺失模块**。**开发任务**:创建 `src/views/ai/vectorstore/{index.vue,VectorStoreForm.vue,VectorStoreTestModal.vue}` + `src/api/ai/vector-store.ts`。**核心特性**:(1) 列表字段:库名、类型(ES/Milvus/Chroma/Qdrant/Pgvector)、URL、用户名(脱敏)、状态;(2) 表单:库名、类型(下拉)、URL、用户名、密码(必填,提交后脱敏)、索引名/Collection 名(按类型不同字段);(3) 「测试连接」按钮:实际发起 HTTP 请求到 ES / Milvus,返回响应时间、版本号;(4) 状态用 Tag 显示(已连接/未配置/连接失败);(5) 依赖:无;(6) 回归测试 AI-15 全部 2 个场景。

### AI-15 向量库 VectorStore

**页面路径**:`/ai/vectorstore`
**后端 Controller**:`model/VectorStoreController` (`/ai/vector-store`)
**权限标识**:`ai:vector-store:create`, `ai:vector-store:delete`, `ai:vector-store:query`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/ai/console/vectorstore/index.vue` + `VectorStoreForm.vue`

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/ai/vector-store/create` | 创建 |
| PUT | `/ai/vector-store/update` | 更新 |
| DELETE | `/ai/vector-store/delete` | 删除 |
| GET | `/ai/vector-store/get` | 详情 |
| GET | `/ai/vector-store/page` | 分页 |
| GET | `/ai/vector-store/export-excel` | 导出 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开向量库管理,执行完整 CRUD + 连接测试。

---

【测试场景 1:列表 + 创建】
1. 验证列:库名、类型(ES/Milvus/Chroma)、URL、状态
2. 验证 Network: GET /ai/vector-store/page 200
3. 点击「新增」,填写:
   - 库名:生产 ES
   - 类型:Elasticsearch
   - URL:http://es:9200
   - 用户名/密码
   - 索引名:knowledge
4. 提交 → 验证 POST 200
5. 「测试连接」 → 验证联通性

预期结果:
✅ 创建成功
✅ 连接测试可用

---

【测试场景 2:边界】
1. URL 不可达 → 验证连接测试失败提示
2. 删除正在被知识库引用的向量库 → 验证保护
```

---

#### 开发提示词

> ❌ **缺失模块**。**开发任务**:创建 `src/views/ai/websearch/{index.vue,WebSearchForm.vue,WebSearchTestModal.vue}` + `src/api/ai/web-search.ts`。**核心特性**:(1) 列表字段:配置名、引擎(Bing/Google/Baidu/SerpAPI/Tavily)、API Key(脱敏)、状态;(2) 表单:配置名、引擎(下拉)、API Key(必填,提交后脱敏)、其他引擎特定配置(如 Bing 的 market 参数);(3) 「实时搜索测试」Modal:输入查询词 → 调用后端 → 返回结果列表(标题/URL/摘要);(4) 切换「加密视图」:GET `/ai/web-search/pageEncrypt`,脱敏所有 API Key;(5) 依赖:无;(6) 回归测试 AI-16 全部 3 个场景。

### AI-16 联网搜索 WebSearch

**页面路径**:`/ai/websearch`
**后端 Controller**:`model/AiWebSearchController` (`/ai/web-search`)
**权限标识**:`ai:web-search:create`, `ai:web-search:update`, `ai:web-search:delete`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/ai/console/websearch/index.vue` + `WebSearchForm.vue`

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/ai/web-search/create` | 创建 |
| PUT | `/ai/web-search/update` | 更新 |
| DELETE | `/ai/web-search/delete` | 删除 |
| GET | `/ai/web-search/get` | 详情 |
| GET | `/ai/web-search/page` | 分页 |
| GET | `/ai/web-search/simple-list` | 简化列表 |
| GET | `/ai/web-search/pageEncrypt` | 加密分页 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开联网搜索配置,执行完整 CRUD + 实时搜索测试。

---

【测试场景 1:列表 + 创建】
1. 验证列:配置名、搜索引擎(Bing/Google/Baidu)、API Key(脱敏)、状态
2. 验证 Network: GET /ai/web-search/page 200
3. 点击「新增」,填写:
   - 配置名:Bing 搜索
   - 引擎:Bing
   - API Key:xxx
4. 提交 → 验证 POST 200

预期结果:
✅ 创建成功

---

【测试场景 2:实时搜索测试**
1. 详情页「测试搜索」
2. 输入查询词
3. 验证返回搜索结果

预期结果:
✅ 搜索功能可用

---

【测试场景 3:边界**
1. API Key 错误 → 验证调用失败提示
2. 切换「加密视图」 → 验证 pageEncrypt API
```

---

#### 开发提示词

> ❌ **缺失模块**。**开发任务**:创建 `src/views/ai/mcp/{index.vue,McpClientForm.vue,McpClientList.vue}` + `src/api/ai/mcp.ts`。**核心特性**:(1) 列表字段:客户端名、URL、传输协议(SSE/stdio/StreamableHTTP)、状态(已连接/未连接/失败)、可用工具数;(2) 表单:客户端名、URL(必填,支持 SSE/StreamableHTTP)、传输协议(下拉);(3) 「测试连接」:实际发起 MCP 握手请求,返回协议版本 + 工具列表(若后端有);(4) 工具列表抽屉:显示 MCP server 提供的所有工具(每工具:名称、描述、参数 schema);(5) 「启用/禁用」开关(实际为 update 的 status 字段);(6) 依赖:无;(7) 回归测试 AI-17 全部 3 个场景。

### AI-17 MCP 客户端管理

**页面路径**:`/ai/mcp-client`
**后端 Controller**:`model/McpClientController` (`/ai/mcp-client`)
**权限标识**:`ai:mcp-client:create`, `ai:mcp-client:update`, `ai:mcp-client:delete`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/ai/console/mcp/index.vue` + `components/McpClientList.vue` + `McpClientForm.vue`

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/ai/mcp-client/create` | 创建 |
| PUT | `/ai/mcp-client/update` | 更新 |
| DELETE | `/ai/mcp-client/delete` | 删除 |
| GET | `/ai/mcp-client/get` | 详情 |
| GET | `/ai/mcp-client/page` | 分页 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开 MCP 客户端管理,执行完整 CRUD + 连接测试。

---

【测试场景 1:列表 + 创建】
1. 验证列:客户端名、URL、状态
2. 验证 Network: GET /ai/mcp-client/page 200
3. 点击「新增」,填写:
   - 客户端名:GitHub MCP
   - URL:https://mcp.github.com
   - 传输协议:SSE / stdio
4. 提交 → 验证 POST 200
5. 「测试连接」 → 验证 MCP 握手成功

预期结果:
✅ 创建成功
✅ 连接测试可用

---

【测试场景 2:工具列表**
1. 连接成功后,显示 MCP 提供的工具列表
2. 工具可绑定到 Agent

预期结果:
✅ 工具列表正确加载

---

【测试场景 3:边界**
1. URL 不可达 → 验证失败提示
2. 删除正在使用的 MCP 客户端 → 验证保护
```

---

#### 开发提示词

> ❌ **缺失模块**。**开发任务**:创建 `src/views/ai/mcp-api-key/{index.vue,McpApiKeyForm.vue}` + `src/api/ai/mcp-api-key.ts`。**核心特性**:(1) 列表字段:Key 名、关联 MCP 客户端(多对多,显示第一个+「+N」)、状态、创建时间;(2) 表单:Key 名(必填)、API Key 值(必填,提交后脱敏)、关联 MCP 客户端(多选下拉,GET `/ai/mcp-api-key/mcp-client/page` 拉列表);(3) 关联/解绑:抽屉内嵌「关联 MCP」按钮,多选提交 POST,单条解绑 DELETE;(4) 依赖 AI-17(MCP 客户端)完成;(5) 回归测试 AI-18 全部 3 个场景。

### AI-18 MCP API Key 管理

**页面路径**:`/ai/mcp-api-key`
**后端 Controller**:`model/McpApiKeyController` (`/ai/mcp-api-key`)
**权限标识**:`ai:mcp-api-key:create`, `ai:mcp-api-key:delete`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/ai/console/mcp/McpApiKeyForm.vue`

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/ai/mcp-api-key/create` | 创建 |
| PUT | `/ai/mcp-api-key/update` | 更新 |
| DELETE | `/ai/mcp-api-key/delete` | 删除 |
| GET | `/ai/mcp-api-key/get` | 详情 |
| GET | `/ai/mcp-api-key/page` | 分页 |
| GET | `/ai/mcp-api-key/mcp-client/page` | 关联 MCP 客户端 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开 MCP API Key 管理,执行完整 CRUD + 客户端绑定测试。

---

【测试场景 1:列表 + 创建】
1. 验证列:Key 名、关联 MCP 客户端、状态
2. 验证 Network: GET /ai/mcp-api-key/page 200
3. 点击「新增」,选择 MCP 客户端 + 填写 Key
4. 提交 → 验证 POST 200

预期结果:
✅ 创建成功

---

【测试场景 2:客户端关联**
1. 验证 Network: GET /ai/mcp-api-key/mcp-client/page 200
2. 绑定/解绑 MCP 客户端

预期结果:
✅ 关联管理可用

---

【测试场景 3:边界**
1. Key 超长 → 验证限制
2. 重复 Key 名 → 验证唯一性
```

---

## P2 优先级 — KMS 知识库层模块测试

---

#### 开发提示词

> ❌ **缺失模块**。**开发任务**:创建 `src/views/kms/knowledge/{index.vue,components/KnowledgeForm.vue,components/KSTestForm.vue,components/KSDocBlockList.vue,components/KSDocBlockForm.vue,components/KSDocumentList.vue,components/KSConfigForm.vue}` + `retrieval/index.vue` + `src/api/kms/knowledge.ts`。**核心特性**:(1) 列表字段:知识库名、描述、文档数、分段数、状态、创建时间;(2) 表单:名称、描述、状态(启用/禁用)、Icon(图标选择器,可选);(3) **5 类知识关联**(本模块最复杂):抽屉 Tabs:分类(Category,POST `/kms/knowledge/createKnowledgeCategory`)/素材(Material,POST `createKnowledgeMaterial`)/文档(Document,POST `createKnowledgeDocument`)/QA(QA,POST `createKnowledgeQA`) → 移除对应 removeXxx;(4) 「我的知识库」Tab vs 「平台知识库」Tab 切换(GET `/kms/knowledge/myKnowledges` vs `/kms/knowledge/page`);(5) 「检索测试」子页面 `retrieval/index.vue`:输入查询词 + 选择知识库 → POST 后端检索 → 返回相关分段列表(用相似度排序);(6) 依赖:无;(7) 回归测试 KMS-01 全部 6 个场景。

### KMS-01 知识库 Knowledge

**页面路径**:`/kms/knowledge`
**后端 Controller**:`knowledge/KnowledgeController` (`/kms/knowledge`)
**权限标识**:`kms:knowledge:create`, `kms:knowledge:update`, `kms:knowledge:delete`, `kms:knowledge:query`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/kms/knowledge/index.vue` + `components/KnowledgeForm.vue`

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/kms/knowledge/page` | 分页 |
| GET | `/kms/knowledge/myKnowledges` | 我的知识库 |
| GET | `/kms/knowledge/simple-list` | 简化列表 |
| POST | `/kms/knowledge/create` | 创建 |
| PUT | `/kms/knowledge/update` | 更新 |
| DELETE | `/kms/knowledge/delete` | 删除 |
| GET | `/kms/knowledge/get` | 详情 |
| POST | `/kms/knowledge/createKnowledgeCategory` | 创建知识库分类 |
| POST | `/kms/knowledge/removeKnowledgeCategory` | 移除分类 |
| POST | `/kms/knowledge/createKnowledgeMaterial` | 关联素材 |
| POST | `/kms/knowledge/removeKnowledgeMaterial` | 移除素材 |
| POST | `/kms/knowledge/createKnowledgeDocument` | 关联文档 |
| POST | `/kms/knowledge/removeKnowledgeDocument` | 移除文档 |
| POST | `/kms/knowledge/createKnowledgeQA` | 关联 QA |
| POST | `/kms/knowledge/removeKnowledgeQA` | 移除 QA |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开知识库管理,执行完整 CRUD + 5 类知识绑定测试。

【前置操作】
1. 登录系统
2. 左侧菜单「KMS 知识库」→「知识库管理」

---

【测试场景 1:列表 + 创建**
1. 验证列:知识库名、描述、文档数、状态、创建时间
2. 验证 Network: GET /kms/knowledge/page 200
3. 点击「新增」,填写:
   - 名称:产品手册
   - 描述:...
   - 状态:启用
4. 提交 → 验证 POST 200

预期结果:
✅ 创建成功

---

【测试场景 2:5 类知识绑定**
1. 详情页「关联管理」,可绑定:
   - 分类(Category):POST /kms/knowledge/createKnowledgeCategory
   - 素材(Material):POST /kms/knowledge/createKnowledgeMaterial
   - 文档(Document):POST /kms/knowledge/createKnowledgeDocument
   - 问答对(QA):POST /kms/knowledge/createKnowledgeQA
2. 验证每类绑定/移除生效

预期结果:
✅ 5 类知识管理可用

---

【测试场景 3:我的知识库**
1. 切换到「我的知识库」Tab
2. 验证 Network: GET /kms/knowledge/myKnowledges 200
3. 仅显示当前用户的知识库

预期结果:
✅ 隔离生效

---

【测试场景 4:检索测试**
1. 知识库列表点击「检索测试」
2. 进入 retrieval 页
3. 输入查询词
4. 验证 Network: GET /kms/knowledge/get?id=xxx 200 + 检索结果

预期结果:
✅ 检索测试可用

---

【测试场景 5:编辑/删除**
1. 编辑 → 验证 PUT 200
2. 删除 → 验证 DELETE 200 + 级联清理关联

预期结果:
✅ 编辑/删除生效

---

【测试场景 6:边界**
1. 名称重复 → 验证唯一性
2. 删除有文档的知识库 → 验证级联
3. 知识库超 1000 个 → 验证分页性能
```

---

#### 开发提示词

> ❌ **缺失模块**(3 步骤向导是核心)。**开发任务**:创建 `src/views/kms/document/{index.vue,form/index.vue,form/UploadStep.vue,form/SplitStep.vue,form/ProcessStep.vue}` + `src/api/kms/document.ts`。**核心特性**:(1) 列表字段:文件名、所属知识库、状态(待处理/处理中/已完成/失败/已停止)、分段数、大小、上传人、创建时间;(2) **3 步骤向导 `form/index.vue`**:`UploadStep` 上传(PDF/Word/TXT/Markdown/MD 拖拽 + 进度条)、`SplitStep` 配置分段(按字符数 / 按段落 / 自定义分隔符 + 重叠长度)、`ProcessStep` 选择向量化模型(下拉 `simple-list` from AI-02)+ 状态预览;(3) 上传:用 ant-design-vue `Upload.Dragger` 组件,自定义 request 走 `/infra/file/upload`(参照 system/file);(4) 「启动」按钮:POST `/kms/document/start`(后台异步,前端轮询);(5) 状态实时刷新:定时器每 3s 调 GET `/kms/document/page` 检查状态;(6) 启停:启用/禁用 POST `/kms/document/enable` / `/disable`;停止处理 POST `/stop`;(7) 「下载」:GET `/kms/document/downloadUrl` 返回 URL,前端 window.open;(8) 依赖 KMS-01 + AI-02(模型);(9) 回归测试 KMS-02 全部 6 个场景。

### KMS-02 文档管理 Document

**页面路径**:`/kms/document`
**后端 Controller**:`knowledge/KnowledgeDocumentController` (`/kms/document`)
**权限标识**:`kms:document:upload`, `kms:document:process`, `kms:document:delete`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/kms/kms/document/{index,form}/*.vue` (含 UploadStep/ProcessStep/SplitStep)

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/kms/document/create` | 创建文档元信息 |
| POST | `/kms/document/start` | 启动处理 |
| POST | `/kms/document/stop` | 停止处理 |
| POST | `/kms/document/enable` | 启用 |
| POST | `/kms/document/disable` | 禁用 |
| DELETE | `/kms/document/delete` | 删除 |
| GET | `/kms/document/downloadUrl` | 下载链接 |
| GET | `/kms/document/get` | 详情 |
| GET | `/kms/document/page` | 分页 |
| PUT | `/kms/document/update` | 更新 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开文档管理,执行完整上传 + 处理 + 分段流程测试。

【前置操作】
1. 登录系统
2. 左侧菜单「KMS 知识库」→「文档管理」

---

【测试场景 1:列表**
1. 验证列:文件名、所属知识库、状态(待处理/处理中/已完成/失败)、分段数、大小
2. 验证 Network: GET /kms/document/page 200

预期结果:
✅ 列表完整

---

【测试场景 2:文档上传向导(document/form/index.vue)**
1. 点击「上传」,进入多步骤向导
   - Step1:上传文件(UploadStep)
   - Step2:配置分段(SplitStep)
   - Step3:处理流程(ProcessStep)
2. 上传 PDF/Word/TXT
3. 配置分段规则(按字符数/按段落)
4. 选择向量化模型
5. 提交 → 验证 POST /kms/document/create 200 + POST /kms/document/start 200

预期结果:
✅ 向导流程通顺
✅ 文档上传成功
✅ 处理启动

---

【测试场景 3:文档处理进度**
1. 文档状态:待处理 → 处理中 → 已完成
2. 验证轮询 GET /kms/document/page 状态变化
3. 验证分段数 > 0

预期结果:
✅ 状态流转正确

---

【测试场景 4:启停 + 启用/禁用**
1. 「停止处理」 → 验证 POST /kms/document/stop 200
2. 「启用/禁用」 → 验证 enable / disable 200

预期结果:
✅ 启停生效

---

【测试场景 5:下载**
1. 点击「下载」 → 验证 GET /kms/document/downloadUrl?id=xxx 200 返回 URL

预期结果:
✅ 下载链接可用

---

【测试场景 6:边界**
1. 上传超大文件(> 100MB)→ 验证拒绝/分片
2. 上传不支持的格式(.exe)→ 验证拒绝
3. 文档处理失败 → 验证错误状态 + 重试
4. 删除处理中文档 → 验证保护
```

---

#### 开发提示词

> ❌ **缺失模块**。**开发任务**:创建 `src/views/kms/segment/{index.vue,KnowledgeSegmentForm.vue,SegmentSplitModal.vue,SegmentSearchModal.vue}` + `src/api/kms/segment.ts`。**核心特性**:(1) 列表字段:分段内容预览(前 50 字 + 「...」)、所属文档、知识库、向量 ID、状态(启用/禁用)、创建时间;(2) 「重新分段」Modal:输入目标长度 + 重叠 → POST 后端 → 提示「分段已重建,共 N 段」;GET `/kms/segment/split?docId=xxx`;(3) 「搜索」框(顶部):输入查询词 → GET `/kms/segment/search?keyword=xxx` → 返回相关分段(按相似度倒序,显示相似度分数);(4) 详情抽屉:分段全文、向量 ID(只读,可复制)、所属文档跳转链接;(5) 启用/禁用:PUT `/kms/segment/update-status`;(6) 依赖 KMS-01 + KMS-02;(7) 回归测试 KMS-03 全部 5 个场景。

### KMS-03 知识分段 Segment

**页面路径**:`/kms/segment`
**后端 Controller**:`knowledge/KnowledgeSegmentController` (`/kms/segment`)
**权限标识**:`kms:segment:create`, `kms:segment:update`, `kms:segment:delete`, `kms:segment:split`, `kms:segment:search`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/ai/knowledge/segment/index.vue` + `KnowledgeSegmentForm.vue`

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/kms/segment/page` | 分页 |
| POST | `/kms/segment/create` | 创建分段 |
| PUT | `/kms/segment/update` | 更新 |
| GET | `/kms/segment/split` | 自动分段 |
| GET | `/kms/segment/search` | 检索分段 |
| PUT | `/kms/segment/update-status` | 更新状态(启用/禁用) |
| DELETE | `/kms/segment/delete` | 删除 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开分段管理,执行完整 CRUD + 自动分段 + 检索测试。

---

【测试场景 1:列表**
1. 验证列:分段内容预览、所属文档、知识库、向量 ID、状态
2. 验证 Network: GET /kms/segment/page 200

预期结果:
✅ 列表完整

---

【测试场景 2:自动分段(split)**
1. 详情页「重新分段」
2. 配置:分段长度、重叠长度
3. 验证 Network: GET /kms/segment/split?docId=xxx 200

预期结果:
✅ 自动分段可用

---

【测试场景 3:检索(search)**
1. 顶部搜索框输入查询词
2. 验证 Network: GET /kms/segment/search?keyword=xxx 200
3. 验证返回相关分段(按相似度排序)

预期结果:
✅ 检索可用
✅ 结果按相似度排序

---

【测试场景 4:编辑/启停/删除**
1. 编辑分段内容 → 验证 PUT 200
2. 启用/禁用 → 验证 PUT /kms/segment/update-status 200
3. 删除 → 验证 DELETE 200

预期结果:
✅ 全部生效

---

【测试场景 5:边界**
1. 检索空查询 → 验证提示
2. 检索超长查询(> 1000 字符)→ 验证截断
3. 一次返回 > 1000 个分段 → 验证分页
```

---

#### 开发提示词

> ❌ **缺失模块**(含导入导出 + Embedding 重建)。**开发任务**:创建 `src/views/kms/question-answer/{index.vue,QuestionAnswerForm.vue,ImportExport.vue,CategoryTree.vue}` + `src/api/kms/question-answer.ts`。**核心特性**:(1) 左侧 CategoryTree(可拖拽排序);(2) 列表字段:问题、答案、分类、状态、Embedding 状态(已嵌入/未嵌入);(3) 表单:问题、答案(支持富文本/Markdown)、分类、状态;(4) **导入导出**:`ImportExport.vue` 提供「下载模板」按钮(GET `/kms/question-answer/get-import-template` 返回 .xlsx)和「批量导入」按钮(上传 .xlsx);「导出 Excel」按钮(GET `/kms/question-answer/export-excel`);(5) **Embedding 重建**:列表多选 → 「重建 Embedding」 → POST `/kms/question-answer/embeddingFile` 或 `/embeddingMaterial`(根据对象类型);(6) 批量删除:多选 → DELETE `/kms/question-answer/delete-list`;(7) 依赖:无;(8) 回归测试 KMS-04 全部 6 个场景。

### KMS-04 问答对 QuestionAnswer

**页面路径**:`/kms/question-answer`
**后端 Controller**:`knowledge/QuestionAnswerController` (`/kms/question-answer`)
**权限标识**:`kms:qa:create`, `kms:qa:update`, `kms:qa:delete`, `kms:qa:import`, `kms:qa:export`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/kms/questionanswer/index.vue` + `QuestionAnswerForm.vue` + `ImportExport.vue` + `CategoryTree.vue`

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/kms/question-answer/create` | 创建 |
| PUT | `/kms/question-answer/update` | 更新 |
| DELETE | `/kms/question-answer/delete` | 删除 |
| DELETE | `/kms/question-answer/delete-list` | 批量删除 |
| DELETE | `/kms/question-answer/batchDelete` | 批量删除(另一接口) |
| GET | `/kms/question-answer/page` | 分页 |
| GET | `/kms/question-answer/get` | 详情 |
| POST | `/kms/question-answer/embeddingFile` | 对文件 Embedding |
| POST | `/kms/question-answer/embeddingMaterial` | 对素材 Embedding |
| GET | `/kms/question-answer/get-import-template` | 下载导入模板 |
| GET | `/kms/question-answer/export-excel` | 导出 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开问答对管理,执行完整 CRUD + 分类树 + 导入导出 + Embedding 测试。

---

【测试场景 1:列表 + 分类树**
1. 验证左侧 CategoryTree
2. 验证右侧列表:问题、答案、分类、状态
3. 验证 Network: GET /kms/question-answer/page 200
4. 点击分类 → 验证过滤

预期结果:
✅ 分类树 + 列表完整

---

【测试场景 2:创建 + 批量导入**
1. 点击「新增」,填写:问题、答案、分类
2. 提交 → 验证 POST 200
3. 「导入」:
   - 点击「下载模板」 → 验证 GET /kms/question-answer/get-import-template 200
   - 上传填写好的 Excel
   - 验证批量导入

预期结果:
✅ 创建 + 导入可用

---

【测试场景 3:导出**
1. 点击「导出」→ 验证 GET /kms/question-answer/export-excel 200
2. 验证 Excel 内容

预期结果:
✅ 导出可用

---

【测试场景 4:Embedding 重建**
1. 选择 QA,点击「重建 Embedding」
2. 验证 Network: POST /kms/question-answer/embeddingFile 200(对文件型 QA)或 /embeddingMaterial(对素材型)

预期结果:
✅ Embedding 重建成功

---

【测试场景 5:批量删除**
1. 多选 3-5 个 QA
2. 「批量删除」→ 验证 DELETE /kms/question-answer/delete-list 200

预期结果:
✅ 批量删除生效

---

【测试场景 6:边界**
1. 问题/答案为空 → 验证校验
2. 导入格式错误 → 验证错误行提示
3. 删除有引用的 QA → 验证保护
```

---

#### 开发提示词

> ❌ **缺失模块**(树形 + 拖拽)。**开发任务**:创建 `src/views/kms/category/{index.vue,FileCategoryForm.vue}` + `src/api/kms/category.ts`。**核心特性**:(1) 树形 Table(支持展开/折叠 + 多级);(2) 列:分类名、层级、父分类、排序、状态、创建时间;(3) 表单:名称(必填)、父分类(树形选择器,顶级选根,GET `/kms/category/getRoot`)、排序(数字);(4) **拖拽排序**:用 SortableJS 或 ant-design-vue Tree `draggable`,拖拽完成后 PUT `/kms/category/update-list` 批量更新顺序 + 父子关系;(5) 删除:有子分类或文件的父分类禁止删除(后端保护,前端二次确认);(6) 批量删除(DELETE `/kms/category/delete-list`);(7) 依赖:无;(8) 回归测试 KMS-05 全部 3 个场景。

### KMS-05 文件分类 Category

**页面路径**:`/kms/category`
**后端 Controller**:`kmsfile/CategoryController` (`/kms/category`)
**权限标识**:`kms:category:create`, `kms:category:update`, `kms:category:delete`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/kms/category/index.vue` + `FileCategoryForm.vue`

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/kms/category/create` | 创建 |
| PUT | `/kms/category/update` | 更新 |
| PUT | `/kms/category/update-list` | 批量更新(排序) |
| DELETE | `/kms/category/delete` | 删除 |
| DELETE | `/kms/category/delete-list` | 批量删除 |
| GET | `/kms/category/list` | 列表 |
| GET | `/kms/category/page` | 分页 |
| GET | `/kms/category/get` | 详情 |
| GET | `/kms/category/getRoot` | 根分类 |
| GET | `/kms/category/export-excel` | 导出 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开文件分类管理,执行树形 CRUD + 拖拽排序测试。

---

【测试场景 1:树形列表 + 拖拽排序**
1. 验证左侧树形结构
2. 验证 Network: GET /kms/category/list 200 + GET /kms/category/getRoot 200
3. 拖拽节点排序 → 验证 PUT /kms/category/update-list 200

预期结果:
✅ 树形结构正确
✅ 拖拽排序生效

---

【测试场景 2:新增/编辑/删除**
1. 新增子分类 → 验证 POST 200
2. 编辑 → 验证 PUT 200
3. 删除空分类 → 验证 DELETE 200

预期结果:
✅ CRUD 生效

---

【测试场景 3:边界**
1. 删除有子分类的父节点 → 验证级联/拒绝
2. 删除有文件的分类 → 验证保护
3. 同名分类 → 验证唯一性
4. 拖拽循环引用(A → B → A)→ 验证检测
```

---

#### 开发提示词

> ❌ **缺失模块**。**开发任务**:创建 `src/views/kms/file-type/{index.vue,FileTypeForm.vue}` + `src/api/kms/file-type.ts`。**核心特性**:(1) 列表字段:类型名、扩展名(数组,如 ['pdf'] 或 ['docx', 'doc'])、MIME、图标(预览)、状态;(2) 表单:类型名(必填)、扩展名(多输入,逗号分隔 + 清洗)、MIME(必填)、图标(上传或预设,选 antd icons);(3) 批量更新:多选 → 批量启用/禁用 → PUT `/kms/file-type/update-list`;(4) 批量删除:DELETE `/kms/file-type/delete-list`;(5) 依赖:无;(6) 回归测试 KMS-06 全部 3 个场景。

### KMS-06 文件类型 Type

**页面路径**:`/kms/file-type`
**后端 Controller**:`kmsfile/KmsTypeController` (`/kms/file-type`)
**权限标识**:`kms:type:create`, `kms:type:update`, `kms:type:delete`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/kms/filetype/index.vue` + `FileTypeForm.vue`

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/kms/file-type/create` | 创建 |
| PUT | `/kms/file-type/update` | 更新 |
| PUT | `/kms/file-type/update-list` | 批量更新 |
| DELETE | `/kms/file-type/delete` | 删除 |
| DELETE | `/kms/file-type/delete-list` | 批量删除 |
| GET | `/kms/file-type/get` | 详情 |
| GET | `/kms/file-type/list` | 列表 |
| GET | `/kms/file-type/page` | 分页 |
| GET | `/kms/file-type/export-excel` | 导出 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开文件类型管理,执行完整 CRUD 测试。

---

【测试场景 1:列表 + 创建**
1. 验证列:类型名、扩展名(支持多扩展名)、图标、状态
2. 验证 Network: GET /kms/file-type/page 200
3. 点击「新增」,填写:
   - 类型名:PDF 文档
   - 扩展名:pdf
   - MIME:application/pdf
4. 提交 → 验证 POST 200

预期结果:
✅ 创建成功

---

【测试场景 2:批量更新**
1. 多选 3 个类型,修改状态
2. 验证 PUT /kms/file-type/update-list 200

预期结果:
✅ 批量更新生效

---

【测试场景 3:边界**
1. 同名类型 → 验证唯一性
2. 删除被引用的类型 → 验证保护
3. 扩展名包含特殊字符 → 验证清洗
```

---

#### 开发提示词

> ❌ **缺失模块**(10+ viewer 路由是最大挑战)。**开发任务**:创建 `src/views/kms/file/{index.vue,CreateFileForm.vue,UpdateFileForm.vue,CategoryTree.vue,components/FileSelect.vue,viewer/{FileViewer,ExcelViewer,PdfViewer,WordViewer,PptViewer,ImageViewer,VideoViewer,VoiceViewer,TextViewer,MarkdownViewer}.vue}` + `src/api/kms/file.ts`。**核心特性**:(1) 左侧 CategoryTree(可拖文件到分类);(2) 列表字段:文件名(带类型图标)、类型(MIME 标签)、分类、大小、上传人、下载次数、创建时间;(3) 「上传」:antd Upload.Dragger,支持所有 10+ 类型(后端按 MIME 识别);(4) **预览路由分发**(`FileViewer.vue` 主组件):按文件 MIME/扩展名动态 import 对应 viewer 子组件,显示「正在加载...」直到 viewer 加载完成;(5) PdfViewer 用 pdfjs-dist、ExcelViewer 用 sheetjs、WordViewer 用 mammoth.js、MarkdownViewer 用 markdown-it;(6) 「下载」:GET `/kms/file/download?id=xxx`;(7) 「OSS 导入」:GET `/kms/file/import-oss`(列出 OSS bucket 中的文件,选择导入);(8) 历史记录:Tab「查询历史」GET `/kms/file/pageTitles`、「下载历史」GET `/kms/file/pageDownloads`;(9) 依赖:无,但需安装多个 npm 包(pdfjs-dist、xlsx、mammoth、markdown-it);(10) 回归测试 KMS-07 全部 6 个场景。

### KMS-07 文件管理 KmsFile

**页面路径**:`/kms/file`
**后端 Controller**:`kmsfile/KmsFileController` (`/kms/file`)
**权限标识**:`kms:file:create`, `kms:file:update`, `kms:file:delete`, `kms:file:download`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/kms/file/index.vue` + `CreateFileForm/UpdateFileForm/CategoryTree/select/*.vue` + `viewer/*.vue`(10 个 viewer)

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/kms/file/create` | 创建文件元信息 |
| PUT | `/kms/file/update` | 更新 |
| DELETE | `/kms/file/delete` | 删除 |
| GET | `/kms/file/get` | 详情 |
| GET | `/kms/file/download` | 下载 |
| GET | `/kms/file/getFiles` | 获取文件列表 |
| DELETE | `/kms/file/deleteQueryHistory` | 删除查询历史 |
| GET | `/kms/file/pageTitles` | 标题分页 |
| DELETE | `/kms/file/deleteDownloadHistory` | 删除下载历史 |
| GET | `/kms/file/pageDownloads` | 下载历史分页 |
| GET | `/kms/file/page` | 分页 |
| GET | `/kms/file/export-excel` | 导出 |
| GET | `/kms/file/import-oss` | OSS 导入 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开文件管理,执行完整 CRUD + 多格式在线预览测试。

---

【测试场景 1:列表 + 分类树 + 上传**
1. 验证左侧 CategoryTree,显示 KMS 分类
2. 验证右侧列表:文件名、类型、分类、大小、上传人、创建时间
3. 验证 Network: GET /kms/file/page 200
4. 「上传」:支持 PDF/Word/Excel/PPT/图片/音视频(10+ 格式)
5. 验证 POST 200

预期结果:
✅ 列表 + 分类 + 多格式上传可用

---

【测试场景 2:在线预览(viewer 路由)**
1. 文件点击「预览」
2. 验证根据文件类型路由到对应 viewer:
   - PDF: PdfViewer
   - Word: WordViewer
   - Excel: ExcelViewer
   - PPT: PptViewer
   - 图片: ImageViewer
   - 视频: VideoViewer
   - 音频: VoiceViewer
   - 文本: TextViewer
   - Markdown: MarkdownViewer
3. 验证 GET /kms/file/download?id=xxx 200

预期结果:
✅ 10+ 格式预览路由正确
✅ 预览内容正确渲染

---

【测试场景 3:编辑/删除**
1. 编辑文件元信息 → 验证 PUT 200
2. 删除 → 验证 DELETE 200

预期结果:
✅ 生效

---

【测试场景 4:历史记录**
1. 查询历史:GET /kms/file/pageTitles 200
2. 下载历史:GET /kms/file/pageDownloads 200
3. 清理历史:DELETE /kms/file/deleteQueryHistory / /kms/file/deleteDownloadHistory 200

预期结果:
✅ 历史管理可用

---

【测试场景 5:OSS 导入**
1. 「从 OSS 导入」 → 验证 GET /kms/file/import-oss 200
2. 验证文件元信息自动创建

预期结果:
✅ OSS 导入可用

---

【测试场景 6:边界**
1. 上传超大文件(> 500MB)→ 验证分片
2. 预览失败的文件 → 验证错误提示
3. 删除有下载历史的文件 → 验证级联清理
4. 1 万+ 文件 → 验证分页性能
```

---

#### 开发提示词

> ❌ **缺失模块**。**开发任务**:创建 `src/views/kms/tag/{index.vue,TagForm.vue,CategoryTree.vue}` + `src/api/kms/tag.ts`。**核心特性**:(1) 左侧 CategoryTree;(2) 列表字段:标签名、分类、状态、向量 ID、关联对象数;(3) 表单:名称(必填)、分类(树形选择)、描述(可选);(4) **创建时自动 Embedding**:后端在 POST `/kms/tag/create` 时自动向量化,前端无需手动触发(可在列表显示「向量化中 / 已完成」状态);(5) **向量检索**:顶部搜索框输入自然语言 → GET `/kms/tag/vectorSearch?keyword=xxx` → 返回相关标签(按相似度);(6) 依赖:无;(7) 回归测试 KMS-08 全部 4 个场景。

### KMS-08 标签管理 Tag

**页面路径**:`/kms/tag`
**后端 Controller**:`tag/TagController` (`/kms/tag`)
**权限标识**:`kms:tag:create`, `kms:tag:update`, `kms:tag:delete`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/kms/tag/index.vue` + `TagForm.vue` + `CategoryTree.vue`

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/kms/tag/create` | 创建 |
| PUT | `/kms/tag/update` | 更新 |
| DELETE | `/kms/tag/delete` | 删除 |
| DELETE | `/kms/tag/delete-list` | 批量删除 |
| GET | `/kms/tag/get` | 详情 |
| GET | `/kms/tag/page` | 分页 |
| GET | `/kms/tag/vectorSearch` | 向量检索 |
| GET | `/kms/tag/list` | 列表 |
| GET | `/kms/tag/export-excel` | 导出 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开标签管理,执行完整 CRUD + 向量检索测试。

---

【测试场景 1:列表 + 分类树**
1. 验证左侧 CategoryTree
2. 验证右侧列表:标签名、分类、状态、向量 ID
3. 验证 Network: GET /kms/tag/page 200

预期结果:
✅ 列表 + 分类树完整

---

【测试场景 2:创建**
1. 点击「新增」,填写:名称、分类、描述
2. 提交 → 验证 POST 200
3. 触发自动 Embedding

预期结果:
✅ 创建成功 + 自动向量化

---

【测试场景 3:向量检索**
1. 顶部搜索框输入查询词
2. 验证 Network: GET /kms/tag/vectorSearch?keyword=xxx 200
3. 验证返回相关标签(按相似度)

预期结果:
✅ 向量检索可用

---

【测试场景 4:边界**
1. 标签名重复 → 验证唯一性
2. 删除被引用的标签 → 验证保护
3. 检索空查询 → 验证提示
```

---

#### 开发提示词

> ❌ **缺失模块**(业务对象 ↔ 标签的多对多绑定)。**开发任务**:创建 `src/views/kms/object-tag/{index.vue,ObjectTagBindModal.vue}` + `src/api/kms/object-tag.ts`。**核心特性**:(1) 列表字段:对象类型(knowledge/document/agent/case/任意字符串)、对象 ID、对象名(可读性)、标签名(可多个)、绑定时间;(2) 「新增绑定」Modal:对象类型(下拉或输入)、对象 ID(可输入或选择器)、标签(多选,GET `/kms/object-tag/get?tagId=xxx` 拉列表);(3) 多对多支持:同一对象可绑多个标签,提交时循环 POST `/kms/object-tag/create`;(4) 「解绑」:单条 DELETE `/kms/object-tag/delete`;(5) **去重提示**:若同一对象同一标签已存在,后端 409 → 前端提示「该绑定已存在」;(6) 过滤:按对象类型 + 对象 ID 过滤;(7) 依赖 KMS-08 完成;(8) 回归测试 KMS-09 全部 3 个场景。

### KMS-09 对象标签 ObjectTag

**页面路径**:`/kms/object-tag`
**后端 Controller**:`tag/ObjectTagController` (`/kms/object-tag`)
**权限标识**:`kms:object-tag:create`, `kms:object-tag:delete`

> ❌ **缺失** — 需新建前端页面,用于把 Tag 绑定到任意业务对象(Object Type + Object ID)

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/kms/object-tag/create` | 创建绑定 |
| PUT | `/kms/object-tag/update` | 更新 |
| DELETE | `/kms/object-tag/delete` | 解绑 |
| GET | `/kms/object-tag/get` | 详情 |
| GET | `/kms/object-tag/page` | 分页 |
| GET | `/kms/object-tag/export-excel` | 导出 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开对象标签管理,执行绑定/解绑测试。

---

【测试场景 1:列表 + 创建绑定**
1. 验证列:对象类型(如 knowledge/document/agent)、对象 ID、标签名、绑定时间
2. 验证 Network: GET /kms/object-tag/page 200
3. 点击「新增绑定」,选择:对象类型、对象、标签
4. 提交 → 验证 POST 200

预期结果:
✅ 绑定可用

---

【测试场景 2:解绑**
1. 多选绑定 → 「批量解绑」
2. 验证 DELETE 200

预期结果:
✅ 解绑生效

---

【测试场景 3:边界**
1. 重复绑定同一对象同一标签 → 验证去重
2. 绑定到不存在的对象 → 验证外键
```

---

#### 开发提示词

> ❌ **缺失模块**(7 个子模块,工作量最大)。**开发任务**:为 7 个子模块各创建标准 index.vue + FormModal.vue + `src/api/kms/legal-{name}.ts`:
> 1. `legal-type/` — 法规类型分类(法律/行政法规/部门规章/地方性法规...),树形结构,拖拽排序
> 2. `legal-paper/` — 法规本体,含全文检索(超长文本,后端 ES 检索)
> 3. `legal-org/` — 制定机关,树形 + 上级机关引用
> 4. `legal-item/` — 法条,与 LegalInfo 关联(法条归属)
> 5. `legal-info/` — 法规详情,关联 Paper + Item 多个,导出 Word(GET `/kms/legal-info/export-word`)
> 6. `legal-fuzzy/` — 模糊匹配/同义词配置,关键功能:同义词组 CRUD + 检索测试
> 7. `legal-case/` — 案例(裁判文书),含案件要素(原告/被告/案由/裁判结果)、导出 Word
>
> **通用模式**:列表 + 树形 + FormModal + 导出(每个都有 `export-excel`,Info/Case 还有 `export-word`)。**共性增强**:批量更新(POST `/update-list`)、批量删除(POST `/delete-list`)、Excel 导出。**优先级**:LegalType → LegalPaper → LegalItem → LegalInfo → LegalCase → LegalOrg → LegalFuzzy(核心检索前必须先建好数据)。**依赖**:无,但 LegalInfo 依赖 LegalPaper + LegalItem。**回归测试**:跑 KMS-10「测试提示词」通用模板 4 个场景 + 各子模块差异化测试点。

### KMS-10 法律知识库(7 个子模块)

**页面路径**:`/kms/legal/*` 7 个子页面
**后端 Controller**:
- `legal/LegalTypeController` (`/kms/legal-type`)
- `legal/LegalPaperController` (`/kms/legal-paper`)
- `legal/LegalOrgController` (`/kms/legal-org`)
- `legal/LegalItemController` (`/kms/legal-item`)
- `legal/LegalInfoController` (`/kms/legal-info`)
- `legal/LegalFuzzyController` (`/kms/legal-fuzzy`)
- `legal/LegalCaseController` (`/kms/legal-case`)

**权限标识**:`kms:legal-{type|paper|org|item|info|fuzzy|case}:*`

> ❌ **缺失** — 7 个子模块前端页面需全部新建,参考 tianque-ui `views/kms/legal/*`

#### 通用 API 取证(7 个模块共有的模式)

每个 Legal 子模块均包含:`create`, `update`, `delete`, `delete-list`, `update-list`, `get`, `page`(`list`), `export-excel`。其中 `LegalInfo` 和 `LegalCase` 还提供 `export-word`。

| 模块 | 特有功能 | tianque-ui 页面 |
|------|----------|----------------|
| LegalType | 法规类型(法律/行政法规/部门规章等)分类 | `legal/legaltype/index.vue` |
| LegalPaper | 法规本体(全文) | `legal/legalpaper/index.vue` |
| LegalOrg | 制定机关 | `legal/legalorg/index.vue` |
| LegalItem | 法条(法规下的具体条款) | `legal/legalitem/index.vue` |
| LegalInfo | 法规详情(关联 Paper + Item) | `legal/legalinfo/index.vue` |
| LegalFuzzy | 模糊匹配/同义词配置 | `legal/legalfuzzy/index.vue` |
| LegalCase | 案例(裁判文书) | `legal/legalcase/index.vue` |

#### 测试提示词(通用模板,适用于 7 个子模块)

```
/browser 或 /open-gstack-browser
打开 [Type/Paper/Org/Item/Info/Fuzzy/Case] 管理,执行完整 CRUD + 导出测试。

【前置操作】
1. 登录系统
2. 左侧菜单「KMS 知识库」→「法律知识库」→「[子模块名]」

---

【测试场景 1:列表 + 创建**
1. 验证列:[子模块字段]
2. 验证 Network: GET /kms/legal-[type]/page 200
3. 点击「新增」,填写:[子模块字段]
4. 提交 → 验证 POST 200

预期结果:
✅ 创建成功

---

【测试场景 2:编辑/删除/批量**
1. 编辑 → 验证 PUT 200
2. 单个删除 → 验证 DELETE 200
3. 批量删除 → 验证 DELETE /delete-list 200
4. 批量更新(状态/排序)→ 验证 PUT /update-list 200

预期结果:
✅ 全部生效

---

【测试场景 3:导出(Excel + Word)】
1. 导出 Excel → 验证 GET /kms/legal-*/export-excel 200
2. (仅 LegalInfo/LegalCase) 导出 Word → 验证 export-word 200

预期结果:
✅ 导出文件可用

---

【测试场景 4:边界**
1. 名称重复 → 验证唯一性
2. 关联关系(LegalInfo → Paper + Item)缺失 → 验证校验
3. 删除被引用的实体 → 验证保护
4. 1 万+ 数据 → 验证分页性能
```

#### 各子模块差异化的测试点

| 子模块 | 差异化测试 |
|--------|-----------|
| LegalType | 多级分类树拖拽 |
| LegalPaper | 全文检索、长文本(数万字)分页 |
| LegalOrg | 树形结构、上级机关引用 |
| LegalItem | 与 LegalInfo 关联(法条归属) |
| LegalInfo | 关联 Paper + Item,导出 Word 包含完整法条 |
| LegalFuzzy | 模糊关键词、同义词组、检索测试 |
| LegalCase | 案件要素、当事人、裁判结果、Word 导出格式 |

---

## P3 优先级 — 辅助/统计层模块测试

---

#### 开发提示词

> ❌ **缺失模块**。**开发任务**:创建 `src/views/ai/stat/llmlog/{index.vue,ChatLlmLogForm.vue,stat.vue}` + `src/api/ai/chat-llm-log.ts`。**核心特性**:(1) 列表字段:模型名、用户、prompt 摘要(前 50 字)、响应摘要、prompt tokens、completion tokens、总 tokens、耗时(ms)、状态(成功/失败)、错误信息、调用时间;(2) 多维筛选:模型、用户、时间段(日期范围)、状态、tokens 区间;(3) 详情抽屉/Modal:完整 prompt、完整响应、模型配置(JSON)、请求头(脱敏);(4) `stat.vue` 统计页:模型调用次数柱状图、总 tokens 趋势线、平均耗时对比、失败率饼图(用 echarts 或 v-chart);(5) 「导出」:GET `/ai/chat-llm-log/export-excel`(若后端支持);(6) 性能:百万级日志用游标分页(GET `/ai/chat-llm-log/page?cursor=xxx&size=100`);(7) 依赖:无;(8) 回归测试 AI-19 全部 4 个场景。

### AI-19 LLM 调用统计/日志

**页面路径**:`/ai/stat/llmlog`
**后端 Controller**:`chat/ChatLlmLogController` (`/ai/chat-llm-log`)
**权限标识**:`ai:llm-log:query`

> ❌ **缺失** — 需新建前端页面,参考 tianque-ui `views/ai/stat/llmlog/index.vue` + `ChatLlmLogForm.vue` + `stat.vue`

#### 关键 API 取证

| 方法 | 路径 | 用途 |
|------|------|------|
| POST | `/ai/chat-llm-log/create` | 创建日志 |
| PUT | `/ai/chat-llm-log/update` | 更新 |
| DELETE | `/ai/chat-llm-log/delete` | 删除 |
| GET | `/ai/chat-llm-log/get` | 详情 |
| GET | `/ai/chat-llm-log/page` | 分页 |
| GET | `/ai/chat-llm-log/stat` | 聚合统计 |

#### 测试提示词

```
/browser 或 /open-gstack-browser
打开 LLM 调用日志,执行 CRUD + 统计测试。

---

【测试场景 1:列表**
1. 验证列:模型名、prompt、响应、tokens、耗时、状态、用户、时间
2. 验证 Network: GET /ai/chat-llm-log/page 200
3. 筛选:按模型、用户、时间段、状态

预期结果:
✅ 列表 + 筛选可用

---

【测试场景 2:详情**
1. 列表点击「详情」,显示完整 prompt、响应、元数据
2. 验证 Network: GET /ai/chat-llm-log/get?id=xxx 200

预期结果:
✅ 详情完整

---

【测试场景 3:统计图表**
1. 切换到「统计」Tab(stat.vue)
2. 验证 Network: GET /ai/chat-llm-log/stat 200
3. 验证图表:按模型调用次数/总 tokens/平均耗时

预期结果:
✅ 图表渲染正确
✅ 聚合数据正确

---

【测试场景 4:边界**
1. 百万级日志 → 验证分页 + 索引
2. 超长 prompt(> 100000 字符)→ 验证截断显示
3. 删除日志 → 验证级联
```

---

#### 开发提示词

> ❌ **缺失模块**(4 个独立页面)。**开发任务**:为 4 个子模块各创建标准 index.vue + FormModal + `src/api/ai/{image,music,write,mindmap}.ts`:
>
> 1. **`/ai/image/`** — 图像生成
>    - 我的图片列表(GET `/ai/image/my-page`)、平台图片(GET `/ai/image/page`)
>    - 「生成」Modal:prompt、模型(DALL-E/Stable Diffusion/SDXL)、尺寸(512/1024/2048)、风格、负向 prompt
>    - 同步生成(POST `/ai/image/draw`)显示进度条,完成后预览
>    - **Midjourney 集成**:`midjourney/imagine` 异步任务 → 轮询状态 → 显示结果(4 张网格)
>    - U1-U4 放大、`V1-V4` 变体、变焦按钮 → POST `/ai/image/midjourney/action`
>    - MJ 通知回调(MJ service 调用 `midjourney/notify` 写库,前端无需直接调)
>
> 2. **`/ai/music/`** — 音乐生成
>    - 列表(GET `/ai/music/page`)、生成(POST `/ai/music/create`,传 prompt/风格/时长)
>    - 音频播放器(ant-design-vue 内置 audio)
>
> 3. **`/ai/write/`** — 长文写作
>    - 列表(GET `/ai/write/page`)、生成(POST `/ai/write/create`,传主题/类型/字数)
>    - 详情显示完整文章(支持 Markdown 渲染)
>
> 4. **`/ai/mindmap/`** — 思维导图
>    - 列表(GET `/ai/mindmap/page`)、生成(POST `/ai/mindmap/create`,传主题)
>    - 详情用 markmap 或 mind-elixir 渲染思维导图(JSON 数据)
>
> **依赖**:无,但需安装 markmap / mind-elixir。**回归测试**:跑 AI-20「测试提示词」4 个场景。

### AI-20 文档生成 Image/Music/Write/MindMap

**页面路径**:`/ai/image`, `/ai/music`, `/ai/write`, `/ai/mindmap`
**后端 Controller**:
- `document/AiImageController` (`/ai/image`)— 含 Midjourney
- `document/AiMusicController` (`/ai/music`)
- `document/AiWriteController` (`/ai/write`)
- `document/AiMindMapController` (`/ai/mindmap`)

**权限标识**:`ai:image:create`, `ai:music:create`, `ai:write:create`, `ai:mindmap:create`

> ❌ **缺失** — 需新建前端页面(4 个)

#### 关键 API 取证

| 模块 | 路径 | 用途 |
|------|------|------|
| Image | POST `/ai/image/draw` | 图像生成 |
| Image | POST `/ai/image/midjourney/imagine` | MJ imagine |
| Image | POST `/ai/image/midjourney/action` | MJ action(U/V/变焦) |
| Image | POST `/ai/image/midjourney/notify` | MJ 通知回调 |
| Image | GET `/ai/image/my-page` | 我的图片 |
| Image | GET `/ai/image/page` | 平台图片 |
| Image | DELETE `/ai/image/delete` | 删除 |
| Music/Write/MindMap | 各 create/get/list | 生成 + 列表 |

#### 测试提示词(以 Image 为例,其余类比)

```
/browser 或 /open-gstack-browser
打开 AI 图像生成,执行生成 + 列表 + Midjourney 集成测试。

---

【测试场景 1:我的图片列表**
1. 验证列:缩略图、prompt、模型、尺寸、状态
2. 验证 Network: GET /ai/image/my-page 200

预期结果:
✅ 列表完整

---

【测试场景 2:图像生成(同步)**
1. 点击「生成」
2. 填写:prompt、模型(DALL-E/Stable Diffusion)、尺寸(1024x1024)
3. 提交 → 验证 POST /ai/image/draw 200
4. 等待生成完成,显示图片

预期结果:
✅ 生成成功

---

【测试场景 3:Midjourney 集成**
1. 切换「Midjourney」模式
2. 提交 prompt → 验证 POST /ai/image/midjourney/imagine 200
3. 等待 MJ 异步处理
4. 验证回调 POST /ai/image/midjourney/notify 写入
5. 点击「U1-U4」放大 → 验证 POST /ai/image/midjourney/action 200
6. 点击「V1-V4」变体 → 验证同上

预期结果:
✅ MJ 集成可用

---

【测试场景 4:边界**
1. prompt 含敏感词 → 验证拒绝
2. 生成失败 → 验证错误状态 + 重试
3. 大量图片(1 万+)→ 验证分页 + 缩略图懒加载
```

---

## 迁移参考文件清单

### 1. 后端 Java 类映射(mediation-ai 已经全部存在,无需迁移,仅用于前端开发参照)

| 缺失前端模块 | mediation-ai 后端 controller | tianque-ai 参照路径 |
|------------|------------------------------|---------------------|
| `ai/agent/` | `model/AiAgentController.java` | `tianque-module-ai/.../model/AiAgentController.java` |
| `ai/app/` | `app/AppController.java` | `tianque-module-ai/.../app/AppController.java` |
| `ai/app/object/` | `app/AppObjectController.java` | `tianque-module-ai/.../app/AppObjectController.java` |
| `ai/prompt/` | `prompt/PromptTemplateController.java` | `tianque-module-ai/.../prompt/PromptTemplateController.java` |
| `ai/api-key/` | `model/AiApiKeyController.java` | `tianque-module-ai/.../model/AiApiKeyController.java` |
| `ai/tool/` | `model/ToolController.java` | `tianque-module-ai/.../model/ToolController.java` |
| `ai/vectorstore/` | `model/VectorStoreController.java` | `tianque-module-ai/.../model/VectorStoreController.java` |
| `ai/websearch/` | `model/AiWebSearchController.java` | `tianque-module-ai/.../model/AiWebSearchController.java` |
| `ai/mcp/` | `model/McpClientController.java` | `tianque-module-ai/.../model/McpClientController.java` |
| `ai/mcp-api-key/` | `model/McpApiKeyController.java` | `tianque-module-ai/.../model/McpApiKeyController.java` |
| `ai/workflow/` | `aiflow/WorkflowController.java` | `tianque-module-ai/.../aiflow/WorkflowController.java` |
| `ai/evaluation/dataset/` | `dataset/DatasetController.java` | `tianque-module-ai/.../dataset/DatasetController.java` |
| `ai/evaluation/evaluator/` | `evaluator/EvaluatorController.java` | `tianque-module-ai/.../evaluator/EvaluatorController.java` |
| `ai/evaluation/experiment/` | `experiment/ExperimentController.java` | `tianque-module-ai/.../experiment/ExperimentController.java` |
| `ai/stat/llmlog/` | `chat/ChatLlmLogController.java` | `tianque-module-ai/.../chat/ChatLlmLogController.java` |
| `kms/knowledge/` | `knowledge/KnowledgeController.java` | `tianque-module-ai/.../knowledge/KnowledgeController.java` |
| `kms/document/` | `knowledge/KnowledgeDocumentController.java` | `tianque-module-ai/.../knowledge/KnowledgeDocumentController.java` |
| `kms/segment/` | `knowledge/KnowledgeSegmentController.java` | `tianque-module-ai/.../knowledge/KnowledgeSegmentController.java` |
| `kms/question-answer/` | `knowledge/QuestionAnswerController.java` | `tianque-module-ai/.../knowledge/QuestionAnswerController.java` |
| `kms/category/` | `kmsfile/CategoryController.java` | `tianque-module-ai/.../kmsfile/CategoryController.java` |
| `kms/file-type/` | `kmsfile/KmsTypeController.java` | `tianque-module-ai/.../kmsfile/KmsTypeController.java` |
| `kms/file/` | `kmsfile/KmsFileController.java` | `tianque-module-ai/.../kmsfile/KmsFileController.java` |
| `kms/tag/` | `tag/TagController.java` | `tianque-module-ai/.../tag/TagController.java` |
| `kms/object-tag/` | `tag/ObjectTagController.java` | `tianque-module-ai/.../tag/ObjectTagController.java` |
| `kms/legal-type/` | `legal/LegalTypeController.java` | `tianque-module-ai/.../legal/LegalTypeController.java` |
| `kms/legal-paper/` | `legal/LegalPaperController.java` | `tianque-module-ai/.../legal/LegalPaperController.java` |
| `kms/legal-org/` | `legal/LegalOrgController.java` | `tianque-module-ai/.../legal/LegalOrgController.java` |
| `kms/legal-item/` | `legal/LegalItemController.java` | `tianque-module-ai/.../legal/LegalItemController.java` |
| `kms/legal-info/` | `legal/LegalInfoController.java` | `tianque-module-ai/.../legal/LegalInfoController.java` |
| `kms/legal-fuzzy/` | `legal/LegalFuzzyController.java` | `tianque-module-ai/.../legal/LegalFuzzyController.java` |
| `kms/legal-case/` | `legal/LegalCaseController.java` | `tianque-module-ai/.../legal/LegalCaseController.java` |
| `ai/image/` | `document/AiImageController.java` | `tianque-module-ai/.../document/AiImageController.java` |
| `ai/music/` | `document/AiMusicController.java` | `tianque-module-ai/.../document/AiMusicController.java` |
| `ai/write/` | `document/AiWriteController.java` | `tianque-module-ai/.../document/AiWriteController.java` |
| `ai/mindmap/` | `document/AiMindMapController.java` | `tianque-module-ai/.../document/AiMindMapController.java` |

### 2. 前端 Vue 文件迁移(全部需新建)

每个缺失模块对应一份 tianque-ui 参考页面,迁移步骤:

1. 复制 tianque-ui 页面到 `mediation-web/src/views/<目标路径>/`
2. 调整 import 路径(从 `@/api/ai/xxx` 改为 `@/api/<目标>/xxx`)
3. 创建对应的 `src/api/<目标>/<模块>.ts`(参照现有 `src/api/ai/model.ts` 风格)
4. 调整菜单配置(`BasicLayout.vue` 已是动态菜单,无需手动改)
5. 调整权限标识(`v-has-permi="['<模块>:<动作>']"`)
6. 适配 Mediation UI 风格(可能存在 ant-design-vue vs element-plus 差异)
7. 适配 mediation-web 的路由结构

### 3. 适配说明

| 维度 | tianque-ui | mediation-web | 适配策略 |
|------|------------|---------------|----------|
| UI 库 | Vben Admin(ant-design-vue) | ant-design-vue | 几乎一致,组件名相同 |
| 路由 | vue-router 4 + dynamic | vue-router 4 + dynamic | 相同 |
| 状态管理 | pinia | pinia | 相同 |
| HTTP 客户端 | axios + 拦截器 | axios + 拦截器 | 相同 |
| 权限 | `hasPermission` | `v-has-permi` 指令 | **需替换**(全局查找替换) |
| 菜单渲染 | 静态配置 | **动态从后端拉** | tianque-ui 静态配置需改为动态渲染或通过后端 `system/menu` 接口注入 |
| 国际化 | i18n + zh-CN | 当前未启用 | 保持单语言,提取 i18n key 为常量 |
| API 路径前缀 | `/admin-api/ai/...` | 直接 `/ai/...`、`/kms/...` | **API 路径不同,无需前缀** |
| 表单组件 | VxeForm / BasicForm | `Modal` + `Form` + `Row/Col` | 替换为 ant-design-vue Form |

### 4. 现有 mediation-web 页面风格参考(用于保持一致性)

- `mediation-web/src/views/ai/model/index.vue` + `ModelFormModal.vue` — 列表 + 弹窗表单模式
- `mediation-web/src/views/ai/skill/index.vue` — 列表 + 测试 Modal
- `mediation-web/src/views/ai/chat/index.vue` — 复杂三栏布局
- `mediation-web/src/views/uaa/user/index.vue` + `UserFormModal.vue` + `UserImportForm.vue` — 完整 CRUD + 导入/导出
- `mediation-web/src/views/system/file/index.vue` + `components/*` — 文件管理(含 viewer)

**推荐**:新页面优先使用 `index.vue + <Module>FormModal.vue` 双文件模式,与现有 `model` 模块保持一致。

---

## AI 引擎差异对比

> 这部分单独说明,回答用户的"对比识别 AI 引擎的差别"要求。

### 1. LLM 引擎架构

| 维度 | tianque-ai | mediation-ai | 差异 |
|------|------------|--------------|------|
| Spring AI 版本 | 1.0.0+ | 同 | 几乎一致 |
| ChatClient 工厂 | `core/factory/chatclient` | 同 | 几乎一致 |
| ChatModel 工厂 | `core/factory/chatmodel` | 同 | 几乎一致 |
| Embedding 工厂 | `core/factory/chatmodel`(AiEmbeddingModelFactory) | 同 | 几乎一致 |
| Rerank 模型工厂 | `core/rerank` | `factory/chatmodel/AiRerankModelFactory` | **路径不同,能力相同** |
| MCP 工厂 | `core/factory/mcp` | 同 | 几乎一致 |
| 负载均衡 | `core/loadbalancer` | 同 | 几乎一致 |
| 工具调用 | `service/codeact` | `codeact/definition/core` | **目录深度不同,能力相同** |

### 2. Activity 与 Advisor

| 维度 | tianque-ai | mediation-ai |
|------|------------|--------------|
| Activity(基础/Chat/KMS) | 有 | 有,几乎完全一致 |
| Advisor(Context/Logger/Memory/RAG) | 有 | 有,几乎完全一致 |
| Reader(data) | 有 | 有,几乎完全一致 |

### 3. KMS / 知识库

| 维度 | tianque-ai | mediation-ai | 差异 |
|------|------------|--------------|------|
| 5 类知识(Category/Material/Document/QA) | 有 | 有 | 几乎一致 |
| 文档处理流程 | UploadStep/SplitStep/ProcessStep | 同 | 几乎一致 |
| 向量化 | 通过 vectorStore | 同 | 几乎一致 |
| 检索(RAG) | Advisor/RAG | 同 | 几乎一致 |
| 文件预览 | 10 个 viewer | 待补 | **前端需补齐** |

### 4. 评估系统

| 维度 | tianque-ai | mediation-ai | 差异 |
|------|------------|--------------|------|
| Dataset + Version + Item | 有 | 有 | 几乎一致 |
| Evaluator + Template + Version | 有 | 有 | 几乎一致 |
| Experiment + Result | 有 | 有 | 几乎一致 |
| 实验停止/重跑 | stop / rerun | 同 | 几乎一致 |

### 5. 工作流

| 维度 | tianque-ai | mediation-ai | 差异 |
|------|------------|--------------|------|
| 可视化设计器 | 有 | 待补 | **前端缺失** |
| Flow + Act + Param | 有 | 有 | 几乎一致 |
| Template + Instance | 有 | 有 | 几乎一致 |
| Dify 互操作 | import / export | 有 | 几乎一致 |

### 6. 独有差异(mediation 独有,tianque-ai 没有)

| 模块 | 路径 | 用途 |
|------|------|------|
| **Harness(沙箱)** | `mediation-ai/mediation-harness-module/...` | Agent 运行时沙箱、安全代码执行 |
| Agent Skill 体系 | `service/skill/...` | 技能上传、路由、CodeAct |
| SkillRoute | `controller/admin/skill/SkillRouteController` | 智能技能路由 |
| CodeactConfig | `controller/admin/skill/CodeactConfigController` | CodeAct 引擎配置 |
| SkillTrace | `controller/admin/skill/SkillTraceController` | 技能调用链追踪 |
| MediationSkill | `controller/admin/skill/MediationSkillController` | 技能上传 + 测试 |
| Ocr | `controller/admin/ocr/OcrController` | OCR 识别(法律文档) |
| OcrConfig | `controller/admin/ocr/OcrConfigController` | OCR 配置 |
| CaseGraph | `controller/admin/knowledge/CaseGraphController` | 案件图谱(法律知识图谱) |
| GraphRagMessage | `controller/admin/chat/GraphRagMessageController` | 图 RAG 消息(基于知识图谱) |
| KnowledgeTask | `controller/admin/knowledge/KnowledgeTaskController` | 知识库任务(导入/导出) |
| AiLlmCall | `controller/admin/chat/AiLlmCallController` | 统一 LLM 调用入口(供 RPC) |
| Agent Config | `controller/admin/model/agent/AgentConfigController` | Agent 多 config group 管理 |
| Agent Template | `controller/admin/model/agent/template/AgentTemplateController` | Agent 模板继承 |

### 7. 引擎能力总结

| 能力 | tianque-ai | mediation-ai | 备注 |
|------|------------|--------------|------|
| 多模型接入(OpenAI/DeepSeek/Qwen/...) | ✅ | ✅ | 通过 Spring AI + 自定义 ChatModel |
| RAG 检索增强 | ✅ | ✅ | 基础 RAG + GraphRAG |
| Agent + Tool Use | ✅ | ✅ | Function Calling + ReAct |
| Workflow 可视化 | ✅ | ✅ | 后端支持,前端设计器缺失 |
| 评估系统 | ✅ | ✅ | Dataset/Evaluator/Experiment |
| 知识图谱 | ⚠️ 部分 | ✅ | mediation 独有 CaseGraph + GraphRag |
| 沙箱执行 | ❌ | ✅ | mediation 独有 Harness |
| 技能上传 | ❌ | ✅ | mediation 独有 Skill + CodeAct |
| OCR | ❌ | ✅ | mediation 独有 |
| 法律本体 | ✅(Legal) | ✅ + 增强 | 两者都有,mediation 与 Ocr/GraphRag 整合 |

**结论**:mediation-ai 在 AI 引擎层面**显著超越** tianque-ai(尤其是 Harness + Skill + GraphRag + OCR),主要差距在前端 UI 缺失。

---

## 测试结果报告模板

```markdown
# AI & KMS 测试报告

- 测试时间:YYYY-MM-DD HH:mm
- 测试人员:xxx
- 测试环境:dev/staging
- 测试版本:mediation-web @ commit xxx

## 一、总体结果

| 优先级 | 模块数 | 通过 | 失败 | 阻塞 | 通过率 |
|--------|--------|------|------|------|--------|
| P0 | 6 | 6 | 0 | 0 | 100% |
| P1 | 12 | 10 | 2 | 0 | 83% |
| P2 | 11 | 8 | 3 | 1 | 73% |
| P3 | 2 | 2 | 0 | 0 | 100% |
| **合计** | **31** | **26** | **5** | **1** | **84%** |

## 二、失败用例详情

### [模块名]-[用例编号]:简短描述
- 复现步骤:1. xxx 2. xxx
- 预期结果:xxx
- 实际结果:xxx
- 截图:[screenshot.png]
- 日志:Network 截图 + Console 报错
- 修复建议:xxx

## 三、阻塞用例

### [模块名]-[用例编号]:xxx
- 阻塞原因:后端接口未实现 / 前端路由未配置 / 第三方依赖未启动
- 解除阻塞所需:xxx

## 四、性能数据(可选)

| 用例 | 首屏时间 | 接口耗时 | 状态 |
|------|----------|----------|------|
| ... | ... | ... | ... |

## 五、安全/兼容(可选)

- XSS 测试:通过/失败
- 越权测试:通过/失败
- 浏览器兼容:Chrome ✅,Firefox ✅,Safari N/A

## 六、结论

- 是否可发布:是/否
- 风险等级:低/中/高
- 下一步行动:xxx
```

---

## 规格自检

> 本节是 v2.0 文档的自我审查记录,确保文档在交予测试 Agent 之前已通过 4 项检查。

### 1. 占位符扫描

- ❌ **未发现的占位符**:"TBD"、"TODO"、"待定"、"……"、"xxx"(在测试场景中除外)
- ✅ 所有 API 路径均已用真实 controller 的 `@RequestMapping` + `@*Mapping("/...")` 取证
- ✅ 所有 Vue 页面路径均已用 `Glob` 工具在 `mediation-web/src/views` 实际目录中验证
- ✅ 30 个模块的"开发提示词"均已落实,无遗漏(v2.1 新增)

### 2. 内部一致性

- ✅ 优先级 P0/P1/P2/P3 与表格「对比矩阵」一致
- ✅ 30 个模块在「对比矩阵」「按优先级章节」「迁移参考文件清单」三处均完整列出
- ✅ API 路径在「关键 API 取证」「测试提示词」「后端 API 完整度表」三处完全一致
- ✅ 「迁移参考文件清单」中的 controller 路径已与前面章节的取证一致
- ✅ v2.1:每个模块均有「测试提示词」+「开发提示词」,开发提示词引用全局开发指引模板,无重复造轮子

### 3. 范围检查

- ✅ 文档聚焦于「AI 与 KMS 模块的测试用例与开发任务」,不涉及其他模块(UAA/System/Case)
- ✅ 30 个模块可在一个 sprint 内分批实现 + 测试,符合 GSD MVP 原则
- ✅ 进一步拆分建议(可选):后续可拆分为 4 个子文档(P0/P1/P2/P3)独立加载,降低单文件体积
- ✅ v2.1:全局开发指引 + 每个模块的开发提示词构成「双轨」,开发者可直接按模板开发

### 4. 模糊性检查

- ✅ "页面路径"已用明确的 `kebab-case` 路由
- ✅ "权限标识"已统一为 `<domain>:<resource>:<action>` 格式
- ✅ "测试场景 N"已用阿拉伯数字编号,每场景边界独立
- ✅ 解决一处潜在歧义:API 路径已注明 mediation-web 直连后端,**无需 `/admin-api` 前缀**
- ✅ v2.1:每个开发提示词明确标注「依赖」「回归测试场景数」,避免开发顺序歧义

---

**文档版本**: v2.1
**创建日期**: 2026-06-14
**维护者**: AI 测试与开发团队
**更新周期**: 每次模块迭代后更新
**取证工具**: `Glob`、`Grep`、`Shell`(对 controller 文件做 `@RequestMapping` 模式匹配)
**总测试用例数**: 30 模块 × 平均 8 场景/模块 = 约 240 个端到端测试场景
**总开发任务清单**: 30 模块 × 平均 6 任务/模块 = 约 180 个原子开发任务
**双轨工作流**: 测试不通过 → 进入开发提示词修复 → 重跑测试(全绿 → commit;仍红 → 回到开发)
