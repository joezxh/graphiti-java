# 法律知识图谱本体与范例数据更新设计

- **文档版本**: v1.1
- **创建日期**: 2026-05-18
- **最后更新**: 2026-05-18
- **状态**: 已批准

## 1. 背景与目标

当前法律知识图谱存在以下问题：
1. 所有本体类的 `parent_class_id` 均为 NULL，继承体系未建立
2. `ont_class_inheritance` 表为空，无法支持多继承查询
3. `init-data.sql` 缺少 Neo4j 中已有的完整节点数据（法院、法条、当事人等）
4. Neo4j 节点与 PostgreSQL/MySQL 本体定义之间缺少 `definition_id` 关联
5. 真实案例数据分散在 `D:\work\docs\legal\人民法院案例库` 的多个子目录中
6. 前端页面（`legal-kg/index.vue`）与后端本体定义（`init-data.sql`）之间的属性命名不一致

**目标**：
- 建立完整的本体继承体系（严格继承，支持多继承表查询）
- 从真实案例库提取约 35-55 个案例（每个目录 3-5 个），扩充图谱数据
- 补全 PostgreSQL/MySQL init-data.sql 中缺失的节点数据
- Neo4j 节点增加 `definition_id` 关联到本体定义表
- 保证 ont_class、ont_property、ont_constraint 之间的引用完整性
- 前端法律知识图谱页面与后端本体定义保持属性命名一致

## 2. 设计决策

| 决策项 | 选择 | 说明 |
|--------|------|------|
| 继承策略 | parent_class_id + ont_class_inheritance 双模式 | 同时支持单继承字段和多继承表查询 |
| 继承深度 | 严格继承（1-2层） | CivilCase 等都直接继承 Case，LegalPerson 继承 Party |
| 案例提取量 | 每个目录 3-5 个 | 覆盖 11+ 个分类目录，总计 35-55 个案例 |
| 数据文件 | PG + MySQL + Neo4j 全量更新 | sql/postgresql/init-data.sql、sql/mysql/init-data.sql、V7__seed_legal_neo4j_data.sql |
| Neo4j 关联 | definition_id 关联 | 所有节点携带 definition_id，通过 Cypher 可 JOIN 到 PG 本体定义 |

## 3. 本体继承体系设计

### 3.1 继承层次

```
Thing（顶层隐含基类）
├── Case（案件基类）
│   ├── CivilCase（民事案件）
│   ├── CriminalCase（刑事案件）
│   ├── AdministrativeCase（行政案件）
│   ├── CommercialCase（商事案件）
│   └── ExecutionCase（执行案件）
├── Party（当事人基类）
│   └── LegalPerson（法人，Party 的具体化）
├── Court（法院）
├── Judge（法官）
├── LegalDocument（法律文件基类）
│   └── LegalProvision（法律条文）
├── JudgmentDocument（裁判文书）
├── CaseReasoning（裁判要旨）
├── Evidence（证据）
├── CaseFact（案件事实）
├── CommercialMediationOrganization（商事调解组织）
├── Mediator（调解员）
└── MediationAgreement（调解协议）
```

### 3.2 ont_class.parent_class_id 设置

| 类 local_name | parent_class_id 指向 | 说明 |
|---------------|---------------------|------|
| Case | NULL | 案件体系顶层 |
| CivilCase | Case.id | 民事案件 |
| CriminalCase | Case.id | 刑事案件 |
| AdministrativeCase | Case.id | 行政案件 |
| CommercialCase | Case.id | 商事案件 |
| ExecutionCase | Case.id | 执行案件 |
| Party | NULL | 当事人体系顶层 |
| LegalPerson | Party.id | 法人 |
| Court | NULL | 无继承 |
| Judge | NULL | 无继承 |
| LegalDocument | NULL | 无继承 |
| LegalProvision | NULL | 无继承 |
| JudgmentDocument | NULL | 无继承 |
| CaseReasoning | NULL | 无继承 |
| Evidence | NULL | 无继承 |
| CaseFact | NULL | 无继承 |
| CommercialMediationOrganization | NULL | 无继承 |
| Mediator | NULL | 无继承 |
| MediationAgreement | NULL | 无继承 |

### 3.3 ont_class_inheritance 表填充

| 子类 | 父类 | distance |
|------|------|----------|
| CivilCase | Case | 1 |
| CriminalCase | Case | 1 |
| AdministrativeCase | Case | 1 |
| CommercialCase | Case | 1 |
| ExecutionCase | Case | 1 |
| LegalPerson | Party | 1 |

## 4. 本体属性完善

### 4.1 现有属性（保留，补充 domain_class_id）

| 类 | 属性 | 数据类型 | 必填 |
|----|------|---------|------|
| Case | caseNumber | string | 是 |
| Case | caseName | string | 是 |
| Case | caseType | string | 是 |
| Case | caseStatus | string | 否 |
| Case | filingDate | date | 否 |
| Case | closedDate | date | 否 |
| Case | amountInDispute | decimal | 否 |
| Case | caseSummary | text | 否 |
| CommercialCase | disputeType | string | 否 |
| CommercialCase | mediationAttempted | boolean | 否 |
| Party | partyName | string | 是 |
| Party | partyType | string | 是 |
| Party | partyRole | string | 是 |
| Party | idNumber | string | 否 |
| Party | address | string | 否 |
| Court | courtName | string | 是 |
| Court | courtLevel | string | 否 |
| Court | location | string | 否 |
| Judge | judgeName | string | 是 |
| Judge | judgeTitle | string | 否 |
| LegalProvision | provisionId | string | 是 |
| LegalProvision | articleNumber | string | 是 |
| LegalProvision | provisionContent | text | 是 |
| LegalProvision | lawName | string | 是 |
| LegalProvision | lawType | string | 否 |
| LegalProvision | effectiveDate | date | 否 |
| JudgmentDocument | documentNumber | string | 是 |
| JudgmentDocument | documentType | string | 否 |
| JudgmentDocument | issueDate | date | 否 |
| JudgmentDocument | judgmentResult | text | 否 |
| JudgmentDocument | legalBasis | text | 否 |
| MediationAgreement | agreementNumber | string | 是 |
| MediationAgreement | performanceMethod | string | 否 |
| MediationAgreement | performanceDeadline | date | 否 |
| MediationAgreement | judiciallyConfirmed | boolean | 否 |

### 4.2 新增属性

| 类 | 属性 | 数据类型 | 必填 | 说明 |
|----|------|---------|------|------|
| Court | jurisdiction | string | 否 | 管辖范围 |
| Court | parentCourt | string | 否 | 上级法院名称 |
| Judge | specialty | string | 否 | 专业领域 |
| LegalProvision | keywords | string | 否 | 关键词 |
| LegalProvision | source | string | 否 | 发布来源 |
| JudgmentDocument | mainContent | text | 否 | 正文摘要 |
| JudgmentDocument | courtName | string | 否 | 作出法院 |
| Party | unifiedSocialCreditCode | string | 否 | 统一社会信用代码（法人） |
| Party | contact | string | 否 | 联系方式 |
| Party | isEnterprise | boolean | 否 | 是否企业 |
| Evidence | evidenceNumber | string | 是 | 证据编号 |
| Evidence | evidenceType | string | 是 | 证据类型 |
| Evidence | content | text | 是 | 证据内容 |
| Evidence | submittedBy | string | 否 | 提交方 |
| Evidence | purpose | string | 否 | 证明目的 |
| Evidence | admissibility | string | 否 | 采纳情况 |
| CaseFact | factDescription | text | 是 | 事实描述 |
| CaseFact | factCategory | string | 否 | 事实类别 |
| CaseFact | factImportance | string | 否 | 重要程度 |
| CaseReasoning | guidanceLevel | string | 否 | 指导级别 |
| CaseReasoning | applicableScenario | string | 否 | 适用场景 |
| CaseReasoning | keywords | string | 否 | 关键词 |
| CommercialMediationOrganization | orgType | string | 否 | 组织类型 |
| CommercialMediationOrganization | licenseNumber | string | 否 | 证照编号 |
| CommercialMediationOrganization | establishedDate | date | 否 | 成立日期 |
| CommercialMediationOrganization | assetAmount | decimal | 否 | 资产金额 |
| CommercialMediationOrganization | mediatorCount | integer | 否 | 调解员数量 |
| Mediator | qualification | string | 否 | 资质 |
| Mediator | licenseNumber | string | 否 | 证照编号 |
| Mediator | organizationName | string | 否 | 所属组织 |
| Mediator | specialty | string | 否 | 专长 |
| Mediator | yearsExperience | integer | 否 | 从业年限 |
| MediationAgreement | mainFacts | text | 否 | 主要事实 |
| MediationAgreement | disputeItems | string | 否 | 争议事项 |
| MediationAgreement | agreementContent | text | 否 | 协议内容 |
| MediationAgreement | signDate | date | 否 | 签订日期 |
| MediationAgreement | judiciallyConfirmDate | date | 否 | 司法确认日期 |
| MediationAgreement | judiciallyConfirmCourt | string | 否 | 司法确认法院 |
| MediationAgreement | judiciallyConfirmNumber | string | 否 | 司法确认编号 |

## 5. 关系（Relationships）设计

### 5.1 Neo4j 关系类型

| 源节点 | 关系 | 目标节点 | 属性 |
|--------|------|---------|------|
| Case | :CASE_PARTY | Party | role, representationType, caseLevel |
| Case | :CASE_COURT | Court | courtRole, jurisdictionBasis |
| Case | :CASE_JUDGE | Judge | role, caseLevel |
| Case | :CASE_LEGAL_PROVISION | LegalProvision | usageType, reasoning, importance |
| Case | :CASE_JUDGMENT | JudgmentDocument | documentRole |
| Case | :CASE_EVIDENCE | Evidence | evidenceRole, admissibility |
| Case | :HAS_CASE_FACT | CaseFact | factRole, factNarrative |
| Case | :HAS_CASE_REASONING | CaseReasoning | reasoningRole, reasoningSummary |
| Case | :CASE_MEDIATION_ORG | CommercialMediationOrganization | mediationStage, mediationResult |
| Case | :CASE_MEDIATION_AGREEMENT | MediationAgreement | agreementRole |
| Court | :COURT_HIERARCHY | Court | relationType |
| CommercialMediationOrganization | :ORG_MEDIATOR | Mediator | employmentType, hireDate |
| LegalProvision | :LEGAL_PROVISION_RELATED | LegalProvision | relationType, description |
| MediationAgreement | :AGREEMENT_JUDICIALLY_CONFIRMED | Court | confirmDate, confirmResult |

### 5.2 PostgreSQL ont_class_inheritance 关系

| class_id | parent_class_id | distance | definition_id |
|----------|-----------------|----------|---------------|
| CivilCase | Case | 1 | 1 |
| CriminalCase | Case | 1 | 1 |
| AdministrativeCase | Case | 1 | 1 |
| CommercialCase | Case | 1 | 1 |
| ExecutionCase | Case | 1 | 1 |
| LegalPerson | Party | 1 | 1 |

## 6. 案例数据提取策略

### 6.1 数据来源

从 `D:\work\docs\legal\人民法院案例库` 的以下子目录提取：
- 1-13-未分类
- 13-33-分类
- 33-47-分类
- 47-77-分类
- 77-87-分类
- 87-116-分类
- 116-155-分类
- 155-306-分类
- 306-356-分类
- 356-407-分类
- 407-456-分类
- 456-506-分类

每个目录抽取 3-5 个代表性案例，覆盖不同案件类型（民事、刑事、行政、商事）。

### 6.2 JSON → 节点映射

```json
// 输入 JSON 结构（人民法院案例库格式）
{
  "title": "案件名称",
  "content": "全文内容",
  "api_data": {
    "cpws_al_ajzh": "案号",
    "cpws_al_case_sort_name": "案件类型",
    "cpws_al_slfy_name": "审理法院",
    "cpws_al_slcx_name": "审理程序",
    "cpws_al_infos": "基本信息字符串",
    "cpws_al_cpyz": "裁判要旨"
  }
}
```

### 6.3 节点数据提取规则

| 目标节点 | 提取逻辑 |
|---------|---------|
| Case | title → caseName；cpws_al_ajzh → caseNumber；cpws_al_case_sort_name → caseType；cpws_al_infos 中的日期 → filingDate/closedDate |
| Party | 从 content 文本中正则提取当事人姓名，role 根据位置判断（原告/被告/第三人） |
| Court | cpws_al_slfy_name → courtName；根据法院名称推断 courtLevel |
| Judge | 从裁判文书署名部分提取，匹配"审判长/审判员/人民陪审员" |
| LegalProvision | 从 content 中法律条文引用部分提取（如"《民法典》第XX条"） |
| CaseReasoning | cpws_al_cpyz → reasoning；判断指导级别（参考/典型） |

## 7. SQL 脚本更新计划

### 7.1 sql/postgresql/init-data.sql

在现有 `DO $$` 块中新增：

1. **继承关系填充**（在 ont_class 插入后、ont_property 插入前）：
   - UPDATE ont_class SET parent_class_id WHERE local_name IN (...)
   - INSERT INTO ont_class_inheritance (class_id, parent_class_id, definition_id, distance)

2. **新增属性插入**：在现有属性插入后继续 INSERT 新属性

3. **约束填充**：补充 ont_constraint 数据

4. **本体版本历史更新**：INSERT ont_version_history

5. **继承关系验证查询**：SELECT 验证 parent_class_id 和 ont_class_inheritance 数据正确

### 7.2 sql/mysql/init-data.sql

与 PostgreSQL 版本保持同步：
- 使用 MySQL 的 `SET @var = (SELECT ...)` 模式获取 class_id
- 继承关系通过 INSERT ... ON DUPLICATE KEY UPDATE 模式
- ont_class_inheritance 表通过独立 INSERT 填充

### 7.3 sql/postgresql/V7__seed_legal_neo4j_data.sql

更新内容：
1. 所有节点增加 `definition_id: 1` 属性
2. 增加约 33-52 个新案例节点（每个目录 3 个 + 每个目录 5 个各选 2 个 = 约 55 个总案例）
3. 每个新案例关联对应的 Party、Court、Judge、LegalProvision、Evidence 节点
4. 新增案例-法律条文引用关系（CASE_LEGAL_PROVISION）
5. 新增案例-裁判要旨关系（HAS_CASE_REASONING）

## 8. 数据一致性保障

### 8.1 幂等性
- 所有 INSERT 语句使用 `ON CONFLICT ON CONSTRAINT uk_xxx DO UPDATE` 或 `ON CONFLICT DO NOTHING`
- 脚本可重复执行而不产生重复数据

### 8.2 外键完整性
- 插入顺序：ont_definition → ont_class → ont_property → ont_constraint → ont_class_inheritance
- Neo4j 中先 MERGE 节点再创建关系
- CASE_LEGAL_PROVISION 关系在 LegalProvision 节点存在后才创建

### 8.3 graph_id 和 definition_id 隔离
- 所有节点/关系携带 `graph_id: 'legal-knowledge-graph'`
- Neo4j 节点额外携带 `definition_id: 1`，指向 PG/MySQL ont_definition 表
- 不同图谱数据通过 graph_id 隔离

## 9. 实现步骤

1. 更新 `sql/postgresql/init-data.sql`：在 DO 块中填充继承关系、补充新属性、完善约束
2. 更新 `sql/mysql/init-data.sql`：同步 PostgreSQL 的所有变更
3. 提取真实案例：从每个目录读取 3-5 个案例 JSON，提取节点数据
4. 更新 `sql/postgresql/V7__seed_legal_neo4j_data.sql`：增加 definition_id、扩充案例数据
5. 执行验证：运行脚本确认数据完整性
6. 提交代码并创建 PR

## 10. 预期成果

- 20 个本体类全部设置正确的 parent_class_id
- ont_class_inheritance 表包含 6 条继承记录
- ~55 个真实案例节点（覆盖民、刑、行政、商事类型）
- ~100+ 个当事人节点
- ~20+ 个法院节点
- ~30+ 个法律条文节点
- ~20+ 个裁判文书节点
- ~15+ 个证据节点
- ~15+ 个案件事实节点
- ~10+ 个裁判要旨节点
- ~10+ 个商事调解相关节点
- 完整的 CASE_PARTY、CASE_COURT、CASE_JUDGE、CASE_LEGAL_PROVISION 等关系

## 11. 前端法律知识图谱页面对齐

### 11.1 当前问题

前端 `legal-kg/index.vue` 页面中使用的本体定义（`LEGAL_ENTITIES` 和 `LEGAL_EDGES`）与后端 SQL 中的本体定义存在不一致：

#### 实体类型命名差异

| 后端 SQL 类名 | 前端 `LEGAL_ENTITIES` 类名 | 差异说明 |
|-------------|---------------------------|---------|
| CommercialMediationOrganization | LegalOrganization | 命名不一致 |
| CaseReasoning | 无 | 前端缺失此类 |
| CaseFact | 无 | 前端缺失此类 |
| CivilCase / CriminalCase 等 | 无 | 前端缺失 Case 子类 |

#### 关系类型差异

| 后端 Cypher 关系类型 | 前端 `LEGAL_EDGES` 关系类型 | 差异说明 |
|--------------------|----------------------------|---------|
| HAS_CASE_REASONING | 无 | 前端缺失 |
| HAS_CASE_FACT | 无 | 前端缺失 |
| AGREEMENT_JUDICIALLY_CONFIRMED | 无 | 前端缺失 |
| COURT_HIERARCHY | 无 | 前端缺失 |
| CASE_PARTY (属性: role) | PARTY (属性: role) | 关系名不一致 |
| 无 | CASE_PARTY (org → mediator) | 前端错误定义为 ORG_MEDIATOR |

#### 属性命名差异

| 后端节点属性 | 前端节点属性 | 差异说明 |
|------------|------------|---------|
| courtLevel | level | 不一致 |
| partyName | name | 不一致 |
| judgeName | name | 不一致 |
| provisionContent | content | 不一致 |
| documentNumber | 无 | 前端缺失 |

### 11.2 对齐方案

#### 11.2.1 统一本体定义（`legal-kg-data.ts`）

前端 `LEGAL_ENTITIES` 与后端 SQL 保持完全一致：

1. **统一类名**：`LegalOrganization` → `CommercialMediationOrganization`（与 SQL 一致）
2. **补全子类**：增加 `CivilCase`、`CriminalCase`、`AdministrativeCase`、`CommercialCase`、`ExecutionCase`
3. **补全类**：增加 `CaseReasoning`、`CaseFact`
4. **属性对齐**：统一属性名与 SQL 的 `ont_property.local_name` 一致

#### 11.2.2 统一关系定义（`legal-kg-data.ts`）

1. **修正关系名**：`CASE_PARTY` 替换前端错误的 `PARTY`
2. **补全关系**：增加 `HAS_CASE_REASONING`、`HAS_CASE_FACT`、`AGREEMENT_JUDICIALLY_CONFIRMED`、`COURT_HIERARCHY`
3. **补全 Mediation 关系**：增加从 `CommercialMediationOrganization` 到 `Mediator` 的 `ORG_MEDIATOR` 关系（当前前端错误地从 `LegalOrganization` 出发）

#### 11.2.3 统一节点数据（`legal-kg-data.ts` 中的 `LEGAL_NODES`）

1. **修正节点类型**：`LegalOrganization` → `CommercialMediationOrganization`
2. **补全节点**：增加 `CaseReasoning`、`CaseFact` 节点
3. **修正属性名**：统一使用后端属性名

#### 11.2.4 统一 `autoSuggestMappings` 字段匹配（`legal-kg/index.vue`）

在 `autoSuggestMappings` 函数中，将前端字段匹配规则与后端 SQL `ont_property.local_name` 对齐：

```typescript
// Case 字段（保持不变，与 SQL 一致）
if (path.includes('ajmc') || value.includes('案') && value.includes('名')) {
  suggestions['Case.caseName'] = jsonPath
}
if (path.includes('ah') || path.includes('ajh') || path.includes('case_number')) {
  suggestions['Case.caseNumber'] = jsonPath  // caseNumber（与 SQL 一致）
}
// ... 其他字段类似
```

#### 11.2.5 统一 `loadGraphStats` 节点类型统计（`legal-kg/index.vue`）

将节点类型过滤逻辑与后端本体定义对齐：

```typescript
// 当前代码（错误）
stats.caseCount = nodes.filter(n => n.type === 'Case').length

// 修正为（含子类）
stats.caseCount = nodes.filter(n =>
  n.type === 'Case' ||
  n.type === 'CivilCase' ||
  n.type === 'CriminalCase' ||
  n.type === 'AdministrativeCase' ||
  n.type === 'CommercialCase' ||
  n.type === 'ExecutionCase'
).length
```

#### 11.2.6 统一 `getEdgeColor` 关系颜色映射（`legal-kg/index.vue`）

补全缺失的关系类型颜色：

```typescript
// 补全以下关系类型颜色
'COURT_HIERARCHY': 'purple'
'HAS_CASE_REASONING': 'gold'
'HAS_CASE_FACT': 'cyan'
'AGREEMENT_JUDICIALLY_CONFIRMED': 'pink'
```

### 11.3 对齐文件清单

| 文件 | 修改内容 |
|------|---------|
| `graphiti-web/src/api/legal-kg-data.ts` | 修正 `LEGAL_ENTITIES` 类名、补全子类、补全 `LEGAL_EDGES` 关系、修正节点数据属性名 |
| `graphiti-web/src/views/legal-kg/index.vue` | 修正 `autoSuggestMappings` 字段匹配、修正 `loadGraphStats` 统计逻辑、补全 `getEdgeColor` 颜色映射 |

### 11.4 验证方法

1. 前端导入本体后，通过 `handleSetOntology` 检查本体定义是否正确写入后端
2. 前端导入节点后，`loadGraphStats` 统计数量应与 Neo4j 实际节点数量一致
3. 前端 LLM 提取后，`fieldMappings` 自动建议的字段名应与后端 `ont_property.local_name` 一致
4. 前端关系列表中应显示所有 12 种关系类型（含补全的 4 种）

### 11.5 实现步骤

1. 更新 `LEGAL_ENTITIES`：修正类名、补全 Case 子类、补全缺失类
2. 更新 `LEGAL_EDGES`：修正关系名、补全缺失关系
3. 更新 `LEGAL_NODES`：修正节点类型、补全节点
4. 更新 `index.vue` 的 `autoSuggestMappings`：对齐字段匹配规则
5. 更新 `index.vue` 的 `loadGraphStats`：修正 Case 子类统计
6. 更新 `index.vue` 的 `getEdgeColor`：补全缺失颜色
7. 验证前后端本体定义一致性
