# 多领域知识图谱 — 本体论设计

## 概述

Graphiti-Java 是一个通用知识图谱平台，支持多领域（Multi-Domain）的知识图谱构建。现已从法律领域专用改造为**通用领域适配**，可服务于：

- 法律知识图谱
- 金融风险图谱
- 企业管理图谱
- 医疗知识图谱
- 社会综合治理图谱

**核心改造原则**：
- 移除所有领域硬编码逻辑
- 领域分类由 LLM 推断 + 用户可覆盖
- 前后端下拉选项由元数据表驱动

---

## 核心概念

### 1. 社区（Community）

指知识图谱中具有共同属性或主题的实体聚集单元。

**属性字段（V3.1.0 通用化）**：

| 字段 | 说明 | 示例 |
|------|------|------|
| `domain_type` | 领域类型 | DOMAIN_LEGAL / DOMAIN_FINANCE |
| `sub_domain_type` | 子领域 | DOMAIN_SOCIAL_DISPUTE_MARRIAGE |
| `region` | 区域 | REGION_CN / REGION_US |
| `scenario_type` | 场景类型 | SCENARIO_JUDICIAL / SCENARIO_COMPLIANCE |
| `community_type` | 社区类型 | top_level / sub_level |
| `inference_confidence` | LLM 推断置信度 | 0.85 |
| `user_overridden` | 用户是否覆盖了推断结果 | false |

**向后兼容字段（V3.0.0，已废弃，迁移后自动映射）**：

| 旧字段 | 替换为 |
|--------|--------|
| `legal_domain` | `domain_type` |
| `jurisdiction` | `region` |
| `practice_type` | `scenario_type` |

### 2. 剧集（Episode）

围绕某一事件或主题的聚集单元，包含相关实体和关系。

**属性字段（V3.1.0 通用化）**：

| 字段 | 说明 | 示例 |
|------|------|------|
| `episode_type` | Episode 类型代码 | EP_TRIAL / EP_MEDIATION |
| `process_type` | 流程类型 | business_process / workflow / lifecycle |
| `stage_level` | 阶段级别 | 一审 / 二审 / 初诊 |
| `stage_label` | 阶段标签 | 立案 / 庭审 / 调解 |
| `is_review_stage` | 是否为审查/评议阶段 | true / false |

**向后兼容字段（V3.0.0，已废弃）**：

| 旧字段 | 替换为 |
|--------|--------|
| `legal_process` | `process_type` |
| `court_level` | `stage_level` |
| `is_trial_stage` | `is_review_stage` |

### 3. 实体（Entity）

知识图谱中的核心节点对象。

### 4. 关系（Relationship）

连接实体、社区、剧集之间的语义关联。

---

## 五大领域分类体系

### 顶层领域

| type_code | type_name | 说明 |
|-----------|-----------|------|
| DOMAIN_ROOT | 知识领域 | 顶层虚拟节点 |
| DOMAIN_LEGAL | 法律 | 处理法律纠纷、诉讼、合规、判决 |
| DOMAIN_FINANCE | 金融 | 处理银行、证券、保险、风险、信贷 |
| DOMAIN_ENTERPRISE | 企业管理 | 处理人力资源、财务、合规、治理 |
| DOMAIN_MEDICAL | 医疗 | 处理诊疗、药品、公共卫生 |
| DOMAIN_SOCIAL_GOV | 社会治理 | 处理社会综合治理事件 |

### 法律子领域

| type_code | type_name |
|-----------|-----------|
| DOMAIN_CIVIL | 民商事 |
| DOMAIN_CRIMINAL | 刑事法律 |
| DOMAIN_ADMIN | 行政法律 |
| DOMAIN_IP | 知识产权 |
| DOMAIN_LABOR | 劳动法律 |

### 金融子领域

| type_code | type_name |
|-----------|-----------|
| DOMAIN_BANKING | 银行与信贷 |
| DOMAIN_SECURITIES | 证券与投资 |
| DOMAIN_INSURANCE | 保险业务 |
| DOMAIN_RISK | 风险管控 |

### 企业管理子领域

| type_code | type_name |
|-----------|-----------|
| DOMAIN_HR | 人力资源 |
| DOMAIN_FINANCE_MGMT | 财务管理 |
| DOMAIN_COMPLIANCE | 企业合规 |
| DOMAIN_GOVERNANCE | 公司治理 |

### 医疗子领域

| type_code | type_name |
|-----------|-----------|
| DOMAIN_CLINICAL | 临床诊疗 |
| DOMAIN_DRUG | 药品与器械 |
| DOMAIN_PUBLIC_HEALTH | 公共卫生 |

### 社会治理子领域

| type_code | type_name |
|-----------|-----------|
| DOMAIN_SOCIAL_DISPUTE_MARRIAGE | 婚恋家庭纠纷 |
| DOMAIN_SOCIAL_DISPUTE_LABOR | 劳动人事争议纠纷 |
| DOMAIN_SOCIAL_DISPUTE_TORT | 侵权责任纠纷 |
| DOMAIN_SOCIAL_DISPUTE_NEIGHBOR | 邻里关系纠纷 |
| DOMAIN_SOCIAL_DISPUTE_PROPERTY | 房屋物业纠纷 |
| DOMAIN_SOCIAL_DISPUTE_LAND | 山林土地水利纠纷 |
| DOMAIN_SOCIAL_DISPUTE_CONSUMER | 消费服务纠纷 |
| DOMAIN_SOCIAL_DISPUTE_ECONOMIC | 经济金融活动纠纷 |
| DOMAIN_SOCIAL_ADMIN_PETITION | 行政纠纷与信访维权 |
| DOMAIN_SOCIAL_CONSULT_SERVICE | 咨询与公证服务 |

---

## 迁移说明

### Neo4j 迁移脚本

执行 `sql/migrations/v004_community_generic_rename.cypher` 将旧字段名迁移为新字段名：

```cypher
// Community
MATCH (c:Community)
SET c.domain_type = c.legal_domain
SET c.region = COALESCE(c.jurisdiction, 'REGION_CN')
SET c.scenario_type = COALESCE(c.practice_type, 'SCENARIO_ROOT')
REMOVE c.legal_domain, c.jurisdiction, c.practice_type

// Episode
MATCH (e:Episode)
SET e.process_type = e.legal_process
SET e.stage_level = e.court_level
SET e.is_review_stage = e.is_trial_stage
REMOVE e.legal_process, e.court_level, e.is_trial_stage
```

### MySQL/PostgreSQL 表字段重命名

执行 `sql/migrations/v004_episode_type_column_rename.sql`（PostgreSQL）或 `sql/migrations/v004_episode_type_column_rename_mysql.sql`（MySQL）。

### 回滚

如需回滚，执行 `sql/migrations/v004_rollback.cypher`。

---

## 技术栈

- **后端**：Java + Spring Boot + MyBatis-Plus
- **图数据库**：Neo4j
- **LLM 服务**：可配置的 LLM Client
- **前端**：Vue 3 + TypeScript + Ant Design Vue
- **本体论**：OWL 风格本体定义，存储于 MySQL

## 相关文档

- 设计文档：`docs/superpowers/specs/2026-05-20-community-generic-design.md`
- 实现计划：`docs/superpowers/plans/2026-05-20-community-generic-implementation.md`
- 法律图谱 V3 设计：`docs/superpowers/specs/2026-05-20-legal-graph-v3-design.md`

## 支持的领域

本系统支持五大领域：
- 法律（DOMAIN_LEGAL）
- 金融（DOMAIN_FINANCE）
- 企业管理（DOMAIN_ENTERPRISE）
- 医疗（DOMAIN_MEDICAL）
- 社会治理（DOMAIN_SOCIAL_GOV）

各领域详情见 ont_community_type 表。
