# 法律案件处置工作台 MVP 设计

**状态**：已确认，待计划分解  
**目标用户**：FDE（前线部署工程师）、案件处理专员、审查人、审批人  
**目标**：在一个已配置的法律图谱中，完成案件材料接入、AI 辅助结构化、人工核验、分派、审查与结案，并留下可审计的证据链。

## 1. 范围与成功标准

首个版本以“本体原生工作台”为实现方式：案件领域数据、待办任务和审批决定都具有明确的类型、属性和关系，而不是在图谱外另建一套案件系统。

### 纳入范围

- 以案件为中心的工作台：列表、详情、关系图、时间线、待办与操作面板。
- 案件材料导入，调用既有法律抽取能力，并允许人工修改抽取结果。
- 本体验证、数据质量问题展示、修复后重新验证。
- 分派、请求补证、提交审查、通过、驳回和结案六类可审计操作。
- 任务、审查意见、审批决定的状态机和操作历史。
- 可安装的法律案件模板包：本体、校验规则、提示词、角色权限、流程定义和演示数据。

### 不纳入范围

- 自动法律意见、裁判建议或无需人工确认的案件结论。
- 通用拖拽式低代码编排器、跨租户 SaaS、多系统连接器市场。
- 完整 BPMN 运行时；MVP 只实现上述固定操作的可配置状态机。

### 可验收结果

FDE 可以为一个新图谱安装模板包；案件专员上传材料后，在同一工作台内完成抽取校正、验证、分派、补证、审查、审批与结案。任意结论均可追溯到操作者、时间、使用的证据和审批决定。

## 2. 路线选择

| 路线 | 取舍 |
| --- | --- |
| 本体原生工作台（采用） | 复用既有本体、图谱、抽取和验证能力；同时让工作流对象可检索、可治理、可模板化。 |
| 独立案件 CRUD | 初期快，但产生图谱外平行模型，无法形成 AIP 的对象—操作闭环。 |
| 先引入 BPMN 引擎 | 通用性强，但流程定义、部署与运维复杂度会拖慢首个交付。 |

## 3. 领域模型

既有对象继续承担事实与语义关系：`Case`、`Party`、`Court`、`Evidence`、`LegalProvision`、`JudgmentDocument`。新增执行对象与关系如下。

| 对象 | 关键属性 | 关键关系 |
| --- | --- | --- |
| `CaseTask` | type、status、assignee、dueAt、priority | `FOR_CASE` → Case；`ASSIGNED_TO` → User；`CREATED_FROM` → CaseAction |
| `Review` | status、opinion、reviewer、submittedAt | `REVIEWS` → Case；`CITES` → Evidence/LegalProvision |
| `ApprovalDecision` | decision、comment、approver、decidedAt | `DECIDES` → Review/Case；`CITES` → Evidence |
| `CaseAction` | actionType、actor、occurredAt、inputSnapshot、resultSnapshot | `ACTS_ON` → Case；`PRODUCES` → Task/Review/Decision |
| `CaseWorkflowState` | state、version、enteredAt | `CURRENT_STATE_OF` → Case |

案件状态机为：`DRAFT → INTAKE_REVIEW → ASSIGNED → EVIDENCE_PENDING → REVIEWING → APPROVAL_PENDING → CLOSED`。驳回回到 `EVIDENCE_PENDING`；所有迁移均由一个 `CaseAction` 记录驱动，不允许前端直接改状态。

## 4. 系统边界与数据流

在现有 Spring Boot、Neo4j、PostgreSQL/Redis、Vue 前端基础上增加三个独立模块：

1. **Case Workbench**：案件视图与操作面板，聚合现有 Graph IDE、时间线和本体动态表单。
2. **Action Runtime**：依据角色、案件状态和本体约束执行固定操作；原子创建/更新执行对象与审计事件。
3. **Solution Package**：导入导出模板包，安装时校验版本、创建本体与规则、注入示例数据。

处理流程：材料上传 → 既有法律抽取 → 草稿图数据 → 人工校正 → 本体验证 → 分派任务 → 补证/审查 → 审批决定 → 结案。每一阶段同时写入图谱事实、状态记录与不可变操作审计；失败时不推进状态，向用户返回字段级问题和可重试操作。

## 5. 权限、审计与安全

角色最小集：`FDE_ADMIN`、`CASE_HANDLER`、`REVIEWER`、`APPROVER`、`AUDITOR`。授权分为图谱范围、对象类型和操作三层；审批人不能审批自己提交的审查。

审计记录必须包含：操作者、角色快照、案件、前后状态、输入摘要、关联证据、结果、时间和失败原因。LLM 输出仅作为待确认草稿，保留模型/提示词版本与人工修订差异。

## 6. 质量与验证策略

- 单元测试覆盖状态迁移、权限矩阵、审查人隔离、幂等与异常回滚。
- 集成测试覆盖 Neo4j 图关系、本体验证、操作审计和模板安装。
- 端到端测试覆盖“材料到结案”主路径与补证/驳回路径。
- 发布门槛：主路径无手工数据库操作；所有状态改变均存在关联的 `CaseAction`；模板可在空图谱重复安装并安全失败或幂等完成。

## 7. 实施分解原则

先完成可运行、可审计的单案件闭环，再扩展模板可配置性和跨案件分析。旧计划中的节点/边更新、后端一致性检查和测试基线问题需在基础 Sprint 先收口，因为它们会直接影响案件工作台的数据正确性。
