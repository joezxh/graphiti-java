# 本体论（Ontology）— 通用化改造 v4.0.0

## 概述

从 v4.0.0 起，Graphiti 支持多领域知识图谱（法律、金融、企业管理、医疗、社会治理）。

社区系统从法律领域专用改造为通用领域适配，所有领域分类通过 `ont_community_type` 元数据表配置，支持动态扩展。

## 社区分类体系

### 维度一：领域（domain）

从 `ont_community_type` 表中 `category = 'domain'` 的记录读取。

五大顶层领域：

| type_code | type_name |
|-----------|-----------|
| DOMAIN_LEGAL | 法律 |
| DOMAIN_FINANCE | 金融 |
| DOMAIN_ENTERPRISE | 企业管理 |
| DOMAIN_MEDICAL | 医疗 |
| DOMAIN_SOCIAL_GOV | 社会治理 |

每个顶层领域下有若干子领域。社会治理领域采用标准纠纷分类体系（10 个一级分类，200+ 二级分类）。

### 维度二：区域（region）

从 `ont_community_type` 表中 `category = 'region'` 的记录读取。

| type_code | type_name |
|-----------|-----------|
| REGION_ROOT | 全球/通用 |
| REGION_CN | 中国 |
| REGION_US | 美国 |
| REGION_EU | 欧洲 |

### 维度三：场景（scenario）

从 `ont_community_type` 表中 `category = 'scenario'` 的记录读取。

| type_code | type_name |
|-----------|-----------|
| SCENARIO_ROOT | 通用场景 |
| SCENARIO_JUDICIAL | 司法实践 |
| SCENARIO_LAW_REGULATE | 依法调解 |
| SCENARIO_FEEDBACK | 反馈处置 |
| SCENARIO_GOVERNANCE | 综合治理 |
| SCENARIO_PREVENTION | 预防预警 |

## Episode 流程类型

从 `ont_episode_type` 表读取，支持 `process_type`：

| process_type | 说明 |
|-------------|------|
| lifecycle | 生命周期（发起→审查→执行→终结） |
| workflow | 工作流（启动→流转→结束） |
| business_process | 业务流程（法律专用：立案→庭审→判决→执行） |

社会治理领域流程：接收 → 评估 → 调解 → 协调 → 反馈 → 回访 → 办结

法律领域流程：立案 → 庭审 → 判决 → 上诉 → 执行

## LLM 领域推断

系统支持 LLM 自动推断社区所属领域，采用两阶段推断：
1. 第一阶段：推断顶层领域（法律/金融/企业管理/医疗/社会治理）
2. 第二阶段：在该领域内推断具体子类型、区域、场景

推断置信度 >= 0.7 时预填充，用户可修改；置信度 < 0.7 时强制用户确认。

## 迁移说明

v3.x → v4.0.0 字段映射：

| 旧字段 | 新字段 |
|--------|--------|
| legal_domain | domain_type |
| jurisdiction | region |
| practice_type | scenario_type |
| legal_process | process_type |
| court_level | stage_level |
| is_trial_stage | is_review_stage |

迁移脚本位于 `sql/migrations/v004_community_generic_rename.cypher`。
