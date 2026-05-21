# 社区系统通用化改造 — 设计文档

## 1. 背景与目标

将 Community 和 Episode 模块从法律领域专用改造为通用领域适配，使其能够服务于：
- 法律知识图谱（现有）
- 金融风险图谱
- 企业管理图谱
- 医疗知识图谱
- 社会综合治理图谱（新）

**核心原则**：
- 移除所有法律领域硬编码逻辑
- 领域分类由 LLM 推断 + 用户可覆盖
- 纯新字段，迁移脚本一次性转换
- 前后端下拉选项由元数据表驱动

---

## 2. 设计决策

| 决策项 | 选择 | 说明 |
|--------|------|------|
| 向后兼容性 | C | 纯新字段，旧数据通过迁移脚本一次性转换 |
| 社区分类来源 | B | LLM 推断 + 用户可覆盖 |
| Episode 字段处理 | B | 通用化重命名（legal_process → process_type 等） |
| 前端下拉来源 | A | 从 `ont_community_type` 表实时拉取 |
| 示例数据范围 | B | 五个领域（法律、金融、企业管理、医疗、社会治理） |
| LLM 推断策略 | B | 领域感知推断（先领域 → 再子类型） |
| LLM Service 位置 | 新增 DomainInferenceService | 独立 Service，不复用现有 GraphNeo4jService |
| 迁移脚本 | 独立文件 | `sql/migrations/` 下独立迁移脚本 |

---

## 3. 字段映射

### 3.1 Community 字段

| 旧字段（Neo4j 属性） | 新字段 | 说明 |
|---------------------|--------|------|
| `legal_domain` | `domain_type` | 领域类型 |
| `jurisdiction` | `region` | 区域/管辖区 |
| `practice_type` | `scenario_type` | 场景类型 |

### 3.2 Episode 字段

| 旧字段（Neo4j 属性） | 新字段 | 说明 |
|---------------------|--------|------|
| `legal_process` | `process_type` | 业务流程类型 |
| `court_level` | `stage_level` | 阶段级别 |
| `is_trial_stage` | `is_review_stage` | 是否审查阶段 |

### 3.3 数据库表字段重命名（PostgreSQL/MySQL DDL）

**ont_episode_type 表**：
- `legal_process` → `process_type`
- `court_level` → `stage_level`
- `is_trial_stage` → `is_review_stage`

**ont_community_type 表注释更新**：
- 表注释改为"定义知识图谱中社区的分类体系，支持多领域通用分类"
- `category` 字段值：`jurisdiction` → `region`，`practice` → `scenario`

---

## 4. 数据库初始数据

### 4.1 ont_community_type — 五领域示例

**顶层领域（domain）**：

| type_code | type_name | category | parent_type_code | sort_order | domain |
|-----------|-----------|----------|-----------------|------------|--------|
| DOMAIN_ROOT | 知识领域 | domain | NULL | 0 | 顶层 |
| DOMAIN_LEGAL | 法律 | domain | DOMAIN_ROOT | 1 | 法律 |
| DOMAIN_FINANCE | 金融 | domain | DOMAIN_ROOT | 10 | 金融 |
| DOMAIN_ENTERPRISE | 企业管理 | domain | DOMAIN_ROOT | 20 | 企业管理 |
| DOMAIN_MEDICAL | 医疗 | domain | DOMAIN_ROOT | 30 | 医疗 |
| DOMAIN_SOCIAL_GOV | 社会治理 | domain | DOMAIN_ROOT | 40 | 社会综合治理 |

**法律子领域**：

| type_code | type_name | parent_type_code | sort_order |
|-----------|-----------|-----------------|------------|
| DOMAIN_CIVIL | 民商事 | DOMAIN_LEGAL | 1 |
| DOMAIN_CRIMINAL | 刑事法律 | DOMAIN_LEGAL | 2 |
| DOMAIN_ADMIN | 行政法律 | DOMAIN_LEGAL | 3 |
| DOMAIN_IP | 知识产权 | DOMAIN_LEGAL | 4 |
| DOMAIN_LABOR | 劳动法律 | DOMAIN_LEGAL | 5 |

**金融子领域**：

| type_code | type_name | parent_type_code | sort_order |
|-----------|-----------|-----------------|------------|
| DOMAIN_BANKING | 银行与信贷 | DOMAIN_FINANCE | 1 |
| DOMAIN_SECURITIES | 证券与投资 | DOMAIN_FINANCE | 2 |
| DOMAIN_INSURANCE | 保险业务 | DOMAIN_FINANCE | 3 |
| DOMAIN_RISK | 风险管控 | DOMAIN_FINANCE | 4 |

**企业管理子领域**：

| type_code | type_name | parent_type_code | sort_order |
|-----------|-----------|-----------------|------------|
| DOMAIN_HR | 人力资源 | DOMAIN_ENTERPRISE | 1 |
| DOMAIN_FINANCE_MGMT | 财务管理 | DOMAIN_ENTERPRISE | 2 |
| DOMAIN_COMPLIANCE | 企业合规 | DOMAIN_ENTERPRISE | 3 |
| DOMAIN_GOVERNANCE | 公司治理 | DOMAIN_ENTERPRISE | 4 |

**医疗子领域**：

| type_code | type_name | parent_type_code | sort_order |
|-----------|-----------|-----------------|------------|
| DOMAIN_CLINICAL | 临床诊疗 | DOMAIN_MEDICAL | 1 |
| DOMAIN_DRUG | 药品与器械 | DOMAIN_MEDICAL | 2 |
| DOMAIN_PUBLIC_HEALTH | 公共卫生 | DOMAIN_MEDICAL | 3 |

**社会治理子领域**（社会综合治理中心，采用标准纠纷分类体系，二级分类共 200+ 项）：

*一级分类（category=domain，二级分类=domain_type，二级分类 parent_type_code = 一级分类 type_code）*：

| 一级分类 code | 一级分类名 | 二级分类 code 前缀 | 二级分类数量 |
|---|---|---|---|
| DOMAIN_SOCIAL_DISPUTE_MARRIAGE | 婚恋家庭纠纷 | DOMAIN_SOCIAL_DISPUTE_MARRIAGE_ | 约 12 项 |
| DOMAIN_SOCIAL_DISPUTE_LABOR | 劳动人事争议纠纷 | DOMAIN_SOCIAL_DISPUTE_LABOR_ | 约 9 项 |
| DOMAIN_SOCIAL_DISPUTE_TORT | 侵权责任纠纷 | DOMAIN_SOCIAL_DISPUTE_TORT_ | 约 25 项 |
| DOMAIN_SOCIAL_DISPUTE_NEIGHBOR | 邻里关系纠纷 | DOMAIN_SOCIAL_DISPUTE_NEIGHBOR_ | 约 5 项 |
| DOMAIN_SOCIAL_DISPUTE_PROPERTY | 房屋物业纠纷 | DOMAIN_SOCIAL_DISPUTE_PROPERTY_ | 约 7 项 |
| DOMAIN_SOCIAL_DISPUTE_LAND | 山林土地水利纠纷 | DOMAIN_SOCIAL_DISPUTE_LAND_ | 约 6 项 |
| DOMAIN_SOCIAL_DISPUTE_CONSUMER | 消费服务纠纷 | DOMAIN_SOCIAL_DISPUTE_CONSUMER_ | 约 19 项 |
| DOMAIN_SOCIAL_DISPUTE_ECONOMIC | 经济金融活动纠纷 | DOMAIN_SOCIAL_DISPUTE_ECONOMIC_ | 约 23 项 |
| DOMAIN_SOCIAL_ADMIN_PETITION | 行政纠纷与信访维权 | DOMAIN_SOCIAL_ADMIN_ | 约 50+ 项 |
| DOMAIN_SOCIAL_CONSULT_SERVICE | 咨询与公证服务 | DOMAIN_SOCIAL_CONSULT_ | 约 12 项 |

**婚恋家庭纠纷（DOMAIN_SOCIAL_DISPUTE_MARRIAGE）— 二级分类**：

| type_code | type_name | parent_type_code |
|-----------|-----------|-----------------|
| DOMAIN_SOCIAL_DISPUTE_MARRIAGE_01 | 夫妻关系矛盾纠纷 | DOMAIN_SOCIAL_DISPUTE_MARRIAGE |
| DOMAIN_SOCIAL_DISPUTE_MARRIAGE_02 | 离异夫妻矛盾纠纷 | DOMAIN_SOCIAL_DISPUTE_MARRIAGE |
| DOMAIN_SOCIAL_DISPUTE_MARRIAGE_03 | 未婚恋爱纠纷 | DOMAIN_SOCIAL_DISPUTE_MARRIAGE |
| DOMAIN_SOCIAL_DISPUTE_MARRIAGE_04 | 同居关系纠纷 | DOMAIN_SOCIAL_DISPUTE_MARRIAGE |
| DOMAIN_SOCIAL_DISPUTE_MARRIAGE_05 | 分家、继承与赡养纠纷 | DOMAIN_SOCIAL_DISPUTE_MARRIAGE |
| DOMAIN_SOCIAL_DISPUTE_MARRIAGE_06 | 父母子女矛盾纠纷 | DOMAIN_SOCIAL_DISPUTE_MARRIAGE |
| DOMAIN_SOCIAL_DISPUTE_MARRIAGE_07 | 兄弟姐妹矛盾纠纷 | DOMAIN_SOCIAL_DISPUTE_MARRIAGE |
| DOMAIN_SOCIAL_DISPUTE_MARRIAGE_08 | 家庭其它成员矛盾纠纷 | DOMAIN_SOCIAL_DISPUTE_MARRIAGE |
| DOMAIN_SOCIAL_DISPUTE_MARRIAGE_09 | 婚姻自主权纠纷 | DOMAIN_SOCIAL_DISPUTE_MARRIAGE |
| DOMAIN_SOCIAL_DISPUTE_MARRIAGE_10 | 宣告失踪、死亡纠纷 | DOMAIN_SOCIAL_DISPUTE_MARRIAGE |
| DOMAIN_SOCIAL_DISPUTE_MARRIAGE_11 | 认定无民事行为能力纠纷 | DOMAIN_SOCIAL_DISPUTE_MARRIAGE |

**劳动人事争议纠纷（DOMAIN_SOCIAL_DISPUTE_LABOR）— 二级分类**：

| type_code | type_name | parent_type_code |
|-----------|-----------|-----------------|
| DOMAIN_SOCIAL_DISPUTE_LABOR_01 | 劳动报酬追索纠纷 | DOMAIN_SOCIAL_DISPUTE_LABOR |
| DOMAIN_SOCIAL_DISPUTE_LABOR_02 | 经济补偿与赔偿纠纷 | DOMAIN_SOCIAL_DISPUTE_LABOR |
| DOMAIN_SOCIAL_DISPUTE_LABOR_03 | 福利待遇纠纷 | DOMAIN_SOCIAL_DISPUTE_LABOR |
| DOMAIN_SOCIAL_DISPUTE_LABOR_04 | 招聘录用纠纷 | DOMAIN_SOCIAL_DISPUTE_LABOR |
| DOMAIN_SOCIAL_DISPUTE_LABOR_05 | 人事任免纠纷 | DOMAIN_SOCIAL_DISPUTE_LABOR |
| DOMAIN_SOCIAL_DISPUTE_LABOR_06 | 劳动合同纠纷 | DOMAIN_SOCIAL_DISPUTE_LABOR |
| DOMAIN_SOCIAL_DISPUTE_LABOR_07 | 临时用工纠纷 | DOMAIN_SOCIAL_DISPUTE_LABOR |
| DOMAIN_SOCIAL_DISPUTE_LABOR_08 | 竞业限制纠纷 | DOMAIN_SOCIAL_DISPUTE_LABOR |

**侵权责任纠纷（DOMAIN_SOCIAL_DISPUTE_TORT）— 部分二级分类**（共约 25 项）：

| type_code | type_name | parent_type_code |
|-----------|-----------|-----------------|
| DOMAIN_SOCIAL_DISPUTE_TORT_01 | 医疗医美损害责任纠纷 | DOMAIN_SOCIAL_DISPUTE_TORT |
| DOMAIN_SOCIAL_DISPUTE_TORT_02 | 人身安全与健康权纠纷 | DOMAIN_SOCIAL_DISPUTE_TORT |
| DOMAIN_SOCIAL_DISPUTE_TORT_03 | 姓名权、肖像权、声音权纠纷 | DOMAIN_SOCIAL_DISPUTE_TORT |
| DOMAIN_SOCIAL_DISPUTE_TORT_04 | 名誉权、荣誉权纠纷 | DOMAIN_SOCIAL_DISPUTE_TORT |
| DOMAIN_SOCIAL_DISPUTE_TORT_05 | 隐私和个人信息保护纠纷 | DOMAIN_SOCIAL_DISPUTE_TORT |
| DOMAIN_SOCIAL_DISPUTE_TORT_06 | 财物返还及损害赔偿纠纷 | DOMAIN_SOCIAL_DISPUTE_TORT |
| DOMAIN_SOCIAL_DISPUTE_TORT_07 | 网络侵权纠纷 | DOMAIN_SOCIAL_DISPUTE_TORT |
| DOMAIN_SOCIAL_DISPUTE_TORT_08 | 群众性活动纠纷 | DOMAIN_SOCIAL_DISPUTE_TORT |
| DOMAIN_SOCIAL_DISPUTE_TORT_09 | 学校及教育机构纠纷 | DOMAIN_SOCIAL_DISPUTE_TORT |
| DOMAIN_SOCIAL_DISPUTE_TORT_10 | 交通事故责任纠纷 | DOMAIN_SOCIAL_DISPUTE_TORT |
| DOMAIN_SOCIAL_DISPUTE_TORT_11 | 医疗事故责任纠纷 | DOMAIN_SOCIAL_DISPUTE_TORT |
| DOMAIN_SOCIAL_DISPUTE_TORT_12 | 性骚扰损害责任纠纷 | DOMAIN_SOCIAL_DISPUTE_TORT |
| DOMAIN_SOCIAL_DISPUTE_TORT_13 | 环境与生态环境责任纠纷及公益诉讼 | DOMAIN_SOCIAL_DISPUTE_TORT |
| DOMAIN_SOCIAL_DISPUTE_TORT_14 | 食品药品安全责任纠纷 | DOMAIN_SOCIAL_DISPUTE_TORT |
| DOMAIN_SOCIAL_DISPUTE_TORT_15 | 饲养动物损害责任纠纷 | DOMAIN_SOCIAL_DISPUTE_TORT |
| DOMAIN_SOCIAL_DISPUTE_TORT_16 | 国家赔偿纠纷 | DOMAIN_SOCIAL_DISPUTE_TORT |
| DOMAIN_SOCIAL_DISPUTE_TORT_17 | 口角琐事纠纷 | DOMAIN_SOCIAL_DISPUTE_TORT |
| DOMAIN_SOCIAL_DISPUTE_TORT_18 | 知识产权纠纷 | DOMAIN_SOCIAL_DISPUTE_TORT |

**邻里关系纠纷（DOMAIN_SOCIAL_DISPUTE_NEIGHBOR）**：

| type_code | type_name | parent_type_code |
|-----------|-----------|-----------------|
| DOMAIN_SOCIAL_DISPUTE_NEIGHBOR_01 | 相邻用水、排水、通行、通风、采光纠纷 | DOMAIN_SOCIAL_DISPUTE_NEIGHBOR |
| DOMAIN_SOCIAL_DISPUTE_NEIGHBOR_02 | 相邻土地利用与建筑物纠纷 | DOMAIN_SOCIAL_DISPUTE_NEIGHBOR |
| DOMAIN_SOCIAL_DISPUTE_NEIGHBOR_03 | 相邻污染损害防免纠纷 | DOMAIN_SOCIAL_DISPUTE_NEIGHBOR |
| DOMAIN_SOCIAL_DISPUTE_NEIGHBOR_04 | 高空抛物责任纠纷 | DOMAIN_SOCIAL_DISPUTE_NEIGHBOR |
| DOMAIN_SOCIAL_DISPUTE_NEIGHBOR_05 | 邻里口角琐事纠纷 | DOMAIN_SOCIAL_DISPUTE_NEIGHBOR |

**房屋物业纠纷（DOMAIN_SOCIAL_DISPUTE_PROPERTY）**：

| type_code | type_name | parent_type_code |
|-----------|-----------|-----------------|
| DOMAIN_SOCIAL_DISPUTE_PROPERTY_01 | 物业管理纠纷 | DOMAIN_SOCIAL_DISPUTE_PROPERTY |
| DOMAIN_SOCIAL_DISPUTE_PROPERTY_02 | 业主与业委会纠纷 | DOMAIN_SOCIAL_DISPUTE_PROPERTY |
| DOMAIN_SOCIAL_DISPUTE_PROPERTY_03 | 不动产登记纠纷 | DOMAIN_SOCIAL_DISPUTE_PROPERTY |
| DOMAIN_SOCIAL_DISPUTE_PROPERTY_04 | 车位车库使用权纠纷 | DOMAIN_SOCIAL_DISPUTE_PROPERTY |
| DOMAIN_SOCIAL_DISPUTE_PROPERTY_05 | 居住权纠纷 | DOMAIN_SOCIAL_DISPUTE_PROPERTY |
| DOMAIN_SOCIAL_DISPUTE_PROPERTY_06 | 房屋买卖与租赁纠纷 | DOMAIN_SOCIAL_DISPUTE_PROPERTY |
| DOMAIN_SOCIAL_DISPUTE_PROPERTY_07 | 建筑质量损害纠纷 | DOMAIN_SOCIAL_DISPUTE_PROPERTY |

**山林土地水利纠纷（DOMAIN_SOCIAL_DISPUTE_LAND）**：

| type_code | type_name | parent_type_code |
|-----------|-----------|-----------------|
| DOMAIN_SOCIAL_DISPUTE_LAND_01 | 土地承包经营权纠纷 | DOMAIN_SOCIAL_DISPUTE_LAND |
| DOMAIN_SOCIAL_DISPUTE_LAND_02 | 宅基地使用权纠纷 | DOMAIN_SOCIAL_DISPUTE_LAND |
| DOMAIN_SOCIAL_DISPUTE_LAND_03 | 取水、养殖、捕捞权纠纷 | DOMAIN_SOCIAL_DISPUTE_LAND |
| DOMAIN_SOCIAL_DISPUTE_LAND_04 | 建设用地使用权纠纷 | DOMAIN_SOCIAL_DISPUTE_LAND |
| DOMAIN_SOCIAL_DISPUTE_LAND_05 | 探矿权、采矿权纠纷 | DOMAIN_SOCIAL_DISPUTE_LAND |
| DOMAIN_SOCIAL_DISPUTE_LAND_06 | 侵害集体经济组织权益纠纷 | DOMAIN_SOCIAL_DISPUTE_LAND |

**消费服务纠纷（DOMAIN_SOCIAL_DISPUTE_CONSUMER）**：

| type_code | type_name | parent_type_code |
|-----------|-----------|-----------------|
| DOMAIN_SOCIAL_DISPUTE_CONSUMER_01 | 商品买卖与质量纠纷 | DOMAIN_SOCIAL_DISPUTE_CONSUMER |
| DOMAIN_SOCIAL_DISPUTE_CONSUMER_02 | 交通出行服务纠纷 | DOMAIN_SOCIAL_DISPUTE_CONSUMER |
| DOMAIN_SOCIAL_DISPUTE_CONSUMER_03 | 住宿餐饮服务纠纷 | DOMAIN_SOCIAL_DISPUTE_CONSUMER |
| DOMAIN_SOCIAL_DISPUTE_CONSUMER_04 | 邮政快递与跑腿服务纠纷 | DOMAIN_SOCIAL_DISPUTE_CONSUMER |
| DOMAIN_SOCIAL_DISPUTE_CONSUMER_05 | 通信与网络服务纠纷 | DOMAIN_SOCIAL_DISPUTE_CONSUMER |
| DOMAIN_SOCIAL_DISPUTE_CONSUMER_06 | 公用事业服务纠纷 | DOMAIN_SOCIAL_DISPUTE_CONSUMER |
| DOMAIN_SOCIAL_DISPUTE_CONSUMER_07 | 旅游服务纠纷 | DOMAIN_SOCIAL_DISPUTE_CONSUMER |
| DOMAIN_SOCIAL_DISPUTE_CONSUMER_08 | 家政服务纠纷 | DOMAIN_SOCIAL_DISPUTE_CONSUMER |
| DOMAIN_SOCIAL_DISPUTE_CONSUMER_09 | 养老服务纠纷 | DOMAIN_SOCIAL_DISPUTE_CONSUMER |
| DOMAIN_SOCIAL_DISPUTE_CONSUMER_10 | 美容保健服务纠纷 | DOMAIN_SOCIAL_DISPUTE_CONSUMER |
| DOMAIN_SOCIAL_DISPUTE_CONSUMER_11 | 培训服务纠纷 | DOMAIN_SOCIAL_DISPUTE_CONSUMER |
| DOMAIN_SOCIAL_DISPUTE_CONSUMER_12 | 房地产服务纠纷 | DOMAIN_SOCIAL_DISPUTE_CONSUMER |

**经济金融活动纠纷（DOMAIN_SOCIAL_DISPUTE_ECONOMIC）**：

| type_code | type_name | parent_type_code |
|-----------|-----------|-----------------|
| DOMAIN_SOCIAL_DISPUTE_ECONOMIC_01 | 借贷担保纠纷 | DOMAIN_SOCIAL_DISPUTE_ECONOMIC |
| DOMAIN_SOCIAL_DISPUTE_ECONOMIC_02 | 储蓄存款纠纷 | DOMAIN_SOCIAL_DISPUTE_ECONOMIC |
| DOMAIN_SOCIAL_DISPUTE_ECONOMIC_03 | 投资、信托理财纠纷 | DOMAIN_SOCIAL_DISPUTE_ECONOMIC |
| DOMAIN_SOCIAL_DISPUTE_ECONOMIC_04 | 证券、基金、期货纠纷 | DOMAIN_SOCIAL_DISPUTE_ECONOMIC |
| DOMAIN_SOCIAL_DISPUTE_ECONOMIC_05 | 保险理赔纠纷 | DOMAIN_SOCIAL_DISPUTE_ECONOMIC |
| DOMAIN_SOCIAL_DISPUTE_ECONOMIC_06 | 票据与信用证纠纷 | DOMAIN_SOCIAL_DISPUTE_ECONOMIC |
| DOMAIN_SOCIAL_DISPUTE_ECONOMIC_07 | 政府类债务纠纷 | DOMAIN_SOCIAL_DISPUTE_ECONOMIC |
| DOMAIN_SOCIAL_DISPUTE_ECONOMIC_08 | 非法融资纠纷 | DOMAIN_SOCIAL_DISPUTE_ECONOMIC |
| DOMAIN_SOCIAL_DISPUTE_ECONOMIC_09 | 公司企业生产经营纠纷 | DOMAIN_SOCIAL_DISPUTE_ECONOMIC |
| DOMAIN_SOCIAL_DISPUTE_ECONOMIC_10 | 拖欠企业账款纠纷 | DOMAIN_SOCIAL_DISPUTE_ECONOMIC |
| DOMAIN_SOCIAL_DISPUTE_ECONOMIC_11 | 房地产纠纷 | DOMAIN_SOCIAL_DISPUTE_ECONOMIC |
| DOMAIN_SOCIAL_DISPUTE_ECONOMIC_12 | 涉众经济金融纠纷 | DOMAIN_SOCIAL_DISPUTE_ECONOMIC |

**行政纠纷与信访维权（DOMAIN_SOCIAL_ADMIN_PETITION）— 按行政管理领域分二级**：

| type_code | type_name | parent_type_code |
|-----------|-----------|-----------------|
| DOMAIN_SOCIAL_ADMIN_PETITION_01 | 公安治安管理纠纷 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_02 | 道路交通管理纠纷 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_03 | 劳动和社会保障行政管理纠纷 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_04 | 民政行政管理纠纷 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_05 | 工商行政管理纠纷 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_06 | 规划、拆迁、房屋登记等城乡建设行政管理纠纷 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_07 | 教育行政管理纠纷 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_08 | 卫生行政管理纠纷 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_09 | 食品药品安全行政管理纠纷 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_10 | 税务行政管理纠纷 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_11 | 环境保护行政管理纠纷 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_12 | 金融行政管理纠纷 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_13 | 海关行政管理纠纷 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_14 | 乡政府管理 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_15 | 村（社区、居）务管理纠纷 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_16 | 行政复议纠纷 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_17 | 纪检监察举报申诉 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_18 | 综合行政执法举报投诉 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_19 | 市场监督执法举报投诉 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_20 | 涉诉涉法举报申诉 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_21 | 其他投诉举报 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_22 | 检举控告类事项 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_23 | 建议意见类事项 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_24 | 申诉求决类事项 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_25 | 涉法涉诉信访 | DOMAIN_SOCIAL_ADMIN_PETITION |
| DOMAIN_SOCIAL_ADMIN_PETITION_26 | 涉军维权事项 | DOMAIN_SOCIAL_ADMIN_PETITION |

**咨询与公证服务（DOMAIN_SOCIAL_CONSULT_SERVICE）**：

| type_code | type_name | parent_type_code |
|-----------|-----------|-----------------|
| DOMAIN_SOCIAL_CONSULT_LEGAL | 法律咨询 | DOMAIN_SOCIAL_CONSULT_SERVICE |
| DOMAIN_SOCIAL_CONSULT_PSYCH | 心理咨询 | DOMAIN_SOCIAL_CONSULT_SERVICE |
| DOMAIN_SOCIAL_CONSULT_PETITION | 信访咨询 | DOMAIN_SOCIAL_CONSULT_SERVICE |
| DOMAIN_SOCIAL_CONSULT_POLICE | 涉警咨询 | DOMAIN_SOCIAL_CONSULT_SERVICE |
| DOMAIN_SOCIAL_CONSULT_NOTARY | 公证咨询 | DOMAIN_SOCIAL_CONSULT_SERVICE |
| DOMAIN_SOCIAL_CONSULT_LEGAL_AID | 法律援助咨询 | DOMAIN_SOCIAL_CONSULT_SERVICE |
| DOMAIN_SOCIAL_SERVICE_NOTARY | 公证服务 | DOMAIN_SOCIAL_CONSULT_SERVICE |
| DOMAIN_SOCIAL_SERVICE_APPRAISAL | 司法鉴定 | DOMAIN_SOCIAL_CONSULT_SERVICE |
| DOMAIN_SOCIAL_SERVICE_LEGAL_AID | 法律援助 | DOMAIN_SOCIAL_CONSULT_SERVICE |
| DOMAIN_SOCIAL_SERVICE_WORK_INJURY | 工伤认定 | DOMAIN_SOCIAL_CONSULT_SERVICE |
| DOMAIN_SOCIAL_CONSULT_OTHER | 其他咨询与服务 | DOMAIN_SOCIAL_CONSULT_SERVICE |
| DOMAIN_SOCIAL_SUGGESTION | 意见建议 | DOMAIN_SOCIAL_CONSULT_SERVICE |

**社会治理场景（scenario）**：

| type_code | type_name | category |
|-----------|-----------|----------|
| SCENARIO_LAW_REGULATE | 依法调解 | scenario |
| SCENARIO_FEEDBACK | 反馈处置 | scenario |
| SCENARIO_GOVERNANCE | 综合治理 | scenario |
| SCENARIO_PREVENTION | 预防预警 | scenario |

**区域分类（region）**：

| type_code | type_name | category |
|-----------|-----------|----------|
| REGION_ROOT | 全球/通用 | region |
| REGION_CN | 中国 | region |
| REGION_US | 美国 | region |
| REGION_EU | 欧洲 | region |
| REGION_INTERNATIONAL | 国际 | region |

**场景分类（scenario）**：

| type_code | type_name | category |
|-----------|-----------|----------|
| SCENARIO_ROOT | 通用场景 | scenario |
| SCENARIO_JUDICIAL | 司法实践 | scenario |
| SCENARIO_COMPLIANCE | 合规管理 | scenario |
| SCENARIO_RISK | 风险管控 | scenario |
| SCENARIO_LIFECYCLE | 生命周期 | scenario |

### 4.2 ont_episode_type — 通用化初始数据

**process_type（通用）**：

| type_code | type_name | process_type | stage_label | is_review_stage |
|-----------|-----------|-------------|-------------|----------------|
| EP_INITIATION | 发起/启动 | lifecycle | 启动 | false |
| EP_EVALUATION | 评估/审查 | lifecycle | 审查 | true |
| EP_EXECUTION | 执行/实施 | lifecycle | 执行 | false |
| EP_RESOLUTION | 解决/终结 | lifecycle | 终结 | false |
| EP_WORKFLOW_START | 流程启动 | workflow | 启动 | false |
| EP_WORKFLOW_NODE | 流程节点 | workflow | 流转 | false |
| EP_WORKFLOW_END | 流程结束 | workflow | 结束 | false |

**社会治理领域保留 Episode 类型**（标注为社会治理可选）：

| type_code | type_name | process_type | stage_label | is_review_stage |
|-----------|-----------|-------------|-------------|----------------|
| EP_REPORT_RECEIVE | 事件接收 | lifecycle | 接收 | false |
| EP_TRIAGE_ASSESS | 事件分流评估 | workflow | 评估 | true |
| EP_MEDIATION | 调解处理 | workflow | 调解 | false |
| EP_COORDINATION | 协调处置 | workflow | 协调 | false |
| EP_FEEDBACK | 结果反馈 | lifecycle | 反馈 | false |
| EP_FOLLOW_UP | 跟踪回访 | lifecycle | 回访 | false |
| EP_CLOSE | 事件办结 | lifecycle | 办结 | false |

**法律领域保留 Episode 类型**（标注为法律可选）：

| type_code | type_name | process_type | stage_label | stage_level | is_review_stage |
|-----------|-----------|-------------|-------------|-------------|----------------|
| EP_FILING | 立案 | business_process | 立案 | NULL | false |
| EP_TRIAL | 庭审 | business_process | 庭审 | 一审 | true |
| EP_JUDGMENT | 判决 | business_process | 判决 | 一审 | true |
| EP_APPEAL | 上诉 | business_process | 上诉 | 二审 | true |
| EP_EXECUTION | 执行 | business_process | 执行 | NULL | false |

---

## 5. LLM 领域感知推断设计

### 5.1 推断流程（两阶段）

```
请求创建社区
        │
        ▼
┌───────────────────────┐
│ 第一阶段：领域推断      │
│ 输入：社区名称 + 成员内容 │
│ 输出：顶层领域代码       │
│   DOMAIN_LEGAL/FINANCE │
│   /ENTERPRISE/MEDICAL  │
└───────────┬───────────┘
            │
            ▼
┌───────────────────────┐
│ 第二阶段：子类型推断     │
│ 输入：领域 + 社区信息    │
│ 输出：domain_type（子）  │
│       region（区域）   │
│       scenario_type    │
│       + 置信度        │
└───────────────────────┘
        │
        ▼
    返回推断结果
        │
        ├── 置信度 >= 0.7：预填充，用户可修改
        └── 置信度 < 0.7：强制用户确认
```

### 5.2 DomainInferenceService

**位置**：`graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/DomainInferenceService.java`

**接口设计**：

```java
public interface DomainInferenceService {
    /**
     * 两阶段领域推断
     * @param communityName 社区名称
     * @param memberContents 成员实体的内容摘要（用于上下文分析）
     * @param availableTypes 可用的 ont_community_type 配置
     * @return 推断结果，包含置信度和用户覆盖字段
     */
    DomainInferenceResult infer(String communityName,
                                List<String> memberContents,
                                List<OntCommunityTypeDO> availableTypes);

    /**
     * 仅推断顶层领域（第一阶段）
     */
    String inferTopLevelDomain(String communityName,
                                List<String> memberContents,
                                List<String> availableDomains);

    /**
     * 在已知领域内推断子类型（第二阶段）
     */
    SubTypeInferenceResult inferSubTypes(String topLevelDomain,
                                          String communityName,
                                          List<String> memberContents,
                                          List<OntCommunityTypeDO> availableTypes);
}
```

**DomainInferenceResult**：

```java
public class DomainInferenceResult {
    private String domainType;        // 顶层领域
    private String subDomainType;     // 子领域（可选）
    private String region;            // 区域
    private String scenarioType;      // 场景类型
    private Double confidence;        // 置信度 0.0~1.0
    private String reasoning;         // 推断理由
}
```

### 5.3 LLM Prompt 设计

**领域推断 Prompt（第一阶段）**：

```
你是一个领域分类专家。请根据以下社区信息，判断该社区属于哪个领域。

已知领域：
- DOMAIN_LEGAL（法律）：处理法律纠纷、诉讼、合规、判决等
- DOMAIN_FINANCE（金融）：处理银行、证券、保险、风险、信贷等
- DOMAIN_ENTERPRISE（企业管理）：处理人力资源、财务、合规、治理等
- DOMAIN_MEDICAL（医疗）：处理诊疗、药品、公共卫生等
- DOMAIN_SOCIAL_GOV（社会治理）：处理社会综合治理事件，分类包括：
  - 婚恋家庭纠纷（婚姻、恋爱、继承、赡养等家庭内部矛盾）
  - 劳动人事争议纠纷（劳动合同、工资报酬、劳动关系等）
  - 侵权责任纠纷（交通事故、医疗损害、名誉侵权、网络侵权等）
  - 邻里关系纠纷（相邻关系、高空抛物等）
  - 房屋物业纠纷（物业、业主自治、房产等）
  - 山林土地水利纠纷（土地承包、宅基地、矿产等）
  - 消费服务纠纷（商品质量、服务违约等）
  - 经济金融活动纠纷（借贷、投资、保险、非法融资等）
  - 行政纠纷与信访维权（公安、工商、税务、环保等行政管理纠纷；信访举报）
  - 咨询与公证服务（法律咨询、公证、司法鉴定、法律援助等）

社区名称：{communityName}
社区成员内容摘要：{memberContents}

请输出 JSON：
{
  "domain": "DOMAIN_XXX",
  "reasoning": "推断理由（1-2句话）",
  "confidence": 0.0~1.0
}
```

**子类型推断 Prompt（第二阶段）**：

```
你是一个领域子类型分类专家。已知该社区属于领域 {topLevelDomain}，请推断其具体子类型。

可用配置（从 ont_community_type 表加载）：
{availableTypesJson}

请从该领域的子类型中，选择最匹配的：
- domain_type（子领域）
- region（区域，默认为 REGION_CN 或 REGION_ROOT）
- scenario_type（场景类型）

输出 JSON：
{
  "domainType": "子领域代码",
  "region": "区域代码",
  "scenarioType": "场景代码",
  "confidence": 0.0~1.0,
  "reasoning": "推断理由"
}
```

---

## 6. CommunityServiceImpl 改造

### 6.1 移除的代码

- `resolveCommunityType(String communityName)` 方法 — 删除
- `resolveLegalDomain(String communityType)` 方法 — 删除
- Cypher 中的硬编码字段：
  - `jurisdiction: "JURISDICTION_CN"` — 改为参数传入
  - `practice_type: "PRACTICE_JUDICIAL"` — 改为参数传入
  - `legal_domain` → `domain_type`

### 6.2 新增字段

```java
// CommunityCreateContext.java
public class CommunityCreateContext {
    String communityName;
    String communityType;
    String domainType;          // 新增（LLM 推断 + 用户覆盖）
    String subDomainType;       // 新增（LLM 推断 + 用户覆盖）
    String region;              // 新增（LLM 推断 + 用户覆盖）
    String scenarioType;        // 新增（LLM 推断 + 用户覆盖）
    Double inferenceConfidence; // 推断置信度
    boolean userOverridden;     // 用户是否手动覆盖
    String summary;
    Integer memberCount;
    List<String> memberUuids;
    String parentCommunityUuid;
}
```

### 6.3 createCommunity 签名变更

```java
// 旧签名（硬编码推断）：
public CommunityCreateResult createCommunity(String graphId, String name, ...)

// 新签名（参数传入）：
public CommunityCreateResult createCommunity(String graphId,
                                             CommunityCreateContext context)
```

---

## 7. EpisodeServiceImpl 改造

### 7.1 字段重命名映射

| 旧字段 | 新字段 |
|--------|--------|
| `legal_process` | `process_type` |
| `court_level` | `stage_level` |
| `is_trial_stage` | `is_review_stage` |

### 7.2 注释标注

所有法律可选字段在注释中标注：

```java
// legal_process 字段（法律领域可选字段）
String processType = (String) episodeData.getOrDefault("process_type", "business_process");
// court_level 字段（法律领域可选字段，其他领域可传 null）
String stageLevel = (String) episodeData.get("stage_level");
// is_trial_stage 字段（法律领域可选字段）
Boolean isReviewStage = (Boolean) episodeData.getOrDefault("is_review_stage", false);
```

---

## 8. 前端改造

### 8.1 TypeScript 类型更新

**文件**：`graphiti-web/src/types/legal-graph-v3.ts`

```typescript
// CommunityV3
interface CommunityV3 {
  domainType?: string    // 替换 legalDomain
  region?: string        // 替换 jurisdiction
  scenarioType?: string  // 替换 practiceType
  subDomainType?: string // 子领域
}

// EpisodeV3
interface EpisodeV3 {
  processType?: string     // 替换 legalProcess
  stageLevel?: string     // 替换 courtLevel
  isReviewStage?: boolean  // 替换 isTrialStage
}
```

### 8.2 下拉选项数据来源

**文件**：`graphiti-web/src/views/data/community-episode.vue`

```typescript
// 从 ont_community_type 表实时拉取
const communityTypes = ref<CommunityTypeItem[]>([])

const loadCommunityTypes = async () => {
  const res = await getCommunityTypes(graphId)
  communityTypes.value = res.data
}

const domainOptions = computed(() =>
  communityTypes.value.filter(t => t.category === 'domain')
)
const regionOptions = computed(() =>
  communityTypes.value.filter(t => t.category === 'region')
)
const scenarioOptions = computed(() =>
  communityTypes.value.filter(t => t.category === 'scenario')
)
```

### 8.3 色彩映射改为元数据驱动

```typescript
// 不再维护硬编码色彩常量表
// 色彩从 ont_community_type.metadata.color 字段读取
const getCommunityColor = (typeCode: string) => {
  const type = communityTypes.value.find(t => t.type_code === typeCode)
  return type?.metadata?.color || '#37474F'
}
```

---

## 9. 数据迁移策略

### 9.1 迁移步骤

```
Phase 1: 部署新代码（新字段 + 双写）
  ├── Java: 新字段写入 domain_type/region/scenario_type
  │         旧字段 legal_domain/jurisdiction/practice_type 同步写入（确保迁移前已有数据在读取时兼容）
  ├── PostgreSQL DDL: 重命名表字段（ALTER TABLE ... RENAME COLUMN）
  ├── Neo4j: 无 DDL，通过迁移脚本处理
  └── 验证: 新增数据正确写入新字段，旧数据属性仍保留

Phase 2: 执行迁移脚本
  └── sql/migrations/v004_community_generic_rename.cypher
      ├── 将 Neo4j 中所有 Community 节点属性 legal_domain → domain_type 等
      ├── 将 Episode 节点属性 legal_process → process_type 等
      └── 验证: 旧数据正确映射到新属性

Phase 3: 清理旧字段（可选，后续迭代）
  ├── 移除双写逻辑中的旧字段写入
  └── 移除 Cypher 查询中的旧字段别名
```

### 9.2 Neo4j 迁移脚本

**文件**：`sql/migrations/v004_community_generic_rename.cypher`

```cypher
// Community 节点属性重命名
MATCH (c:Community)
WHERE c.legal_domain IS NOT NULL
SET c.domain_type = c.legal_domain
REMOVE c.legal_domain
SET c.region = COALESCE(c.jurisdiction, 'REGION_ROOT')
REMOVE c.jurisdiction
SET c.scenario_type = COALESCE(c.practice_type, 'SCENARIO_ROOT')
REMOVE c.practice_type;

// Episode 节点属性重命名
MATCH (e:Episode)
WHERE e.legal_process IS NOT NULL
SET e.process_type = e.legal_process
REMOVE e.legal_process
SET e.stage_level = COALESCE(e.court_level, 'NULL')
REMOVE e.court_level
SET e.is_review_stage = COALESCE(e.is_trial_stage, false)
REMOVE e.is_trial_stage;

// 验证
MATCH (c:Community) RETURN count(c) as total, count(c.domain_type) as with_domain_type;
MATCH (e:Episode) RETURN count(e) as total, count(e.process_type) as with_process_type;
```

---

## 10. 数据库 DDL 变更

### 10.1 ont_episode_type 字段重命名（PostgreSQL）

```sql
ALTER TABLE ont_episode_type
  RENAME COLUMN legal_process TO process_type;

ALTER TABLE ont_episode_type
  RENAME COLUMN court_level TO stage_level;

ALTER TABLE ont_episode_type
  RENAME COLUMN is_trial_stage TO is_review_stage;

COMMENT ON COLUMN ont_episode_type.process_type IS
  '业务流程类型：business_process(业务流程)|workflow(工作流)|lifecycle(生命周期)';

COMMENT ON COLUMN ont_episode_type.stage_level IS
  '阶段级别（通用，可配置）';

COMMENT ON COLUMN ont_episode_type.is_review_stage IS
  '是否审查/评议阶段';
```

### 10.2 ont_community_type 注释和 category 值更新

```sql
ALTER TABLE ont_community_type
  ALTER COLUMN category TYPE VARCHAR(32);

COMMENT ON TABLE ont_community_type IS
  '定义知识图谱中社区的分类体系，支持多领域通用分类（领域、区域、场景三个正交维度）';
```

---

## 11. 测试验证

| 测试类型 | 验证内容 |
|----------|----------|
| 单元测试 | DomainInferenceService 两阶段推断逻辑 |
| 集成测试 | 五领域社区创建/查询/更新，数据正确写入 Neo4j 新字段 |
| 迁移测试 | 迁移脚本执行后，old_community 数据正确映射到新字段 |
| 前端测试 | 下拉选项从 API 拉取，色彩从 metadata.color 读取 |
| 兼容性测试 | 旧 API 请求（字段缺失）有合理错误提示 |
| LLM 推断测试 | 各领域社区推断准确率 >= 80%（置信度加权） |

---

## 12. 风险评估

| 风险项 | 影响 | 缓解措施 |
|--------|------|----------|
| 迁移脚本执行失败 | 旧数据不可读 | 先在测试环境执行验证，提供回滚脚本 |
| LLM 推断不稳定 | 领域分类错误 | 置信度机制 + 用户覆盖兜底；提供手动选择入口 |
| 前端拉取元数据失败 | 下拉选项为空 | 保留内存缓存兜底（上次拉取的选项），提供重试机制 |
| 数据库字段重命名影响现有查询 | 查询报错 | DDL 执行前确认无活跃长事务，备好回滚 |

---

## 13. 实施顺序

1. **数据库 DDL**（PostgreSQL + MySQL）— 字段重命名 + 表注释更新
2. **Java 后端** — VO/DO/Mapper 字段更新 → DomainInferenceService → Service 层改造 → 迁移脚本
3. **前端层** — TypeScript 类型 → API 调用 → community-episode.vue 下拉选项
4. **Neo4j 迁移脚本** — 独立迁移文件交付
5. **文档更新** — legal_graph.md、ontology.md

---

## 14. 预期成果

1. **多领域适配**：支持法律、金融、企业管理、医疗、社会治理五大领域的社区分类
2. **配置驱动**：社区类型、领域、区域、场景通过 `ont_community_type` 表配置
3. **无硬编码**：移除所有法律领域特定的推断逻辑
4. **LLM 智能推断**：两阶段领域感知推断 + 用户覆盖机制
5. **扩展性强**：新增领域只需在元数据表中添加配置，无需修改代码
