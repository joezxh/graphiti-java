# 社区系统通用化改造 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Community 和 Episode 模块从法律领域专用改造为通用领域适配，支持法律、金融、企业管理、医疗、社会治理五大领域。

**Architecture:** 采用三层架构：
1. **数据层**：PostgreSQL/MySQL 的 `ont_community_type` 和 `ont_episode_type` 表提供元数据配置，`OntCommunityTypeMapper`/`OntEpisodeTypeMapper` 以 `@Select` 注解查询
2. **推理层**：`DomainInferenceService` 封装 LLM 两阶段推断（领域推断 → 子类型推断）
3. **业务层**：`CommunityServiceImpl` 接收 `CommunityCreateContext`（含 domainType/region/scenarioType），直接用 Neo4j Java Driver 写图；`EpisodeServiceImpl` 复用 `GraphNeo4jService` 写 Episode
4. **前端层**：`community-episode.vue` 通过 `metadata.ts` 实时拉取下拉选项，色彩从 `metadata.color` 读取

**Tech Stack:** Java Spring Boot, Neo4j Java Driver, MyBatis (注解), PostgreSQL, MySQL, Vue 3 + TypeScript

---

## 实施顺序总览

```
Phase 1: 数据库 DDL（PostgreSQL + MySQL）
Phase 2: Java DO/Mapper 层字段重命名
Phase 3: DomainInferenceService（新增）
Phase 4: CommunityServiceImpl 改造
Phase 5: EpisodeServiceImpl + GraphNeo4jService 改造
Phase 6: ont_community_type 初始数据（五领域）
Phase 7: ont_episode_type 初始数据（通用 + 社会治理 + 法律）
Phase 8: 前端层改造
Phase 9: Neo4j 迁移脚本
Phase 10: 文档更新
```

---

## Phase 1: 数据库 DDL

### Task 1.1: PostgreSQL DDL — ont_episode_type 字段重命名

**文件：** `sql/postgresql/schema-v3.sql`

在现有 `ont_episode_type` 表定义中，将三个字段名重命名（等效于 ALTER TABLE RENAME COLUMN 效果）。注意：此 SQL 文件是建表语句，改造后重新执行即可，无需 ALTER。

**修改 `sql/postgresql/schema-v3.sql` 第 51-68 行**，将 `legal_process VARCHAR(32)` 整块替换为新版本：

- [ ] **Step 1: 读取当前 schema-v3.sql 中 ont_episode_type 定义（行 42-73）**

```sql
-- 旧（行 42-73，供参考）：
CREATE TABLE ont_episode_type (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    type_code VARCHAR(32) NOT NULL,
    type_name VARCHAR(128) NOT NULL,
    type_name_en VARCHAR(64),
    legal_process VARCHAR(32),    -- litigation|mediation|arbitration|execution
    stage_label VARCHAR(32),      -- 立案|庭审|调解|判决|执行
    court_level VARCHAR(32),       -- 一审|二审|再审|死刑复核
    is_trial_stage BOOLEAN DEFAULT FALSE,
    ...
);
```

- [ ] **Step 2: 替换为通用化字段**

```sql
CREATE TABLE ont_episode_type (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    type_code VARCHAR(32) NOT NULL,
    type_name VARCHAR(128) NOT NULL,
    type_name_en VARCHAR(64),
    process_type VARCHAR(32),       -- business_process|workflow|lifecycle
    stage_label VARCHAR(32),       -- 通用阶段标签
    stage_level VARCHAR(32),        -- 阶段级别（通用，可配置，法律领域填一审/二审等）
    is_review_stage BOOLEAN DEFAULT FALSE,  -- 是否审查/评议阶段
    ...
    legal_process VARCHAR(32),    -- 保留（向后兼容），新增映射字段
    court_level VARCHAR(32),       -- 保留（向后兼容）
    is_trial_stage BOOLEAN DEFAULT FALSE,   -- 保留（向后兼容）
    ...
);
```

**注意：** `legal_process`、`court_level`、`is_trial_stage` 三个旧字段在 DDL 中**保留注释标注为"向后兼容字段，Phase 3 迁移完成后删除"**，这样 PostgreSQL 建表语句中同时包含新旧字段名，实现 DDL 向前向后兼容。实际 ALTER TABLE 脚本在 Task 1.3 单独提供。

- [ ] **Step 3: 更新 ont_episode_type 表注释**

在 schema-v3.sql 末尾追加（或在表定义 COMMENT 处更新）：

```sql
COMMENT ON TABLE ont_episode_type IS
  '定义知识图谱中社区的分类体系，支持多领域通用分类（领域、区域、场景三个正交维度）';

COMMENT ON COLUMN ont_episode_type.process_type IS
  '业务流程类型：business_process(业务流程)|workflow(工作流)|lifecycle(生命周期)';

COMMENT ON COLUMN ont_episode_type.stage_level IS
  '阶段级别（通用，可配置。法律领域：一审/二审/再审/死刑复核；其他领域：可自定义）';

COMMENT ON COLUMN ont_episode_type.is_review_stage IS
  '是否审查/评议阶段（法律庭审=true；其他流程视情况）';

-- 以下字段为向后兼容字段，Phase 3 迁移完成后删除
COMMENT ON COLUMN ont_episode_type.legal_process IS
  '[向后兼容] 旧字段，已迁移到 process_type，计划 Phase 3 删除';
COMMENT ON COLUMN ont_episode_type.court_level IS
  '[向后兼容] 旧字段，已迁移到 stage_level，计划 Phase 3 删除';
COMMENT ON COLUMN ont_episode_type.is_trial_stage IS
  '[向后兼容] 旧字段，已迁移到 is_review_stage，计划 Phase 3 删除';
```

### Task 1.2: MySQL DDL — ont_episode_type 字段重命名

**文件：** `sql/mysql/schema.sql`（或对应 MySQL schema 文件）

- [ ] **Step 1: 定位 MySQL schema 中 `ont_episode_type` 表定义**

```sql
-- 旧 MySQL DDL 中找到：
`legal_process` VARCHAR(32) COMMENT '...',
`court_level` VARCHAR(32) COMMENT '...',
`is_trial_stage` TINYINT(1) DEFAULT 0 COMMENT '...',
```

- [ ] **Step 2: 替换为新字段，保留旧字段注释标注**

```sql
`process_type` VARCHAR(32) DEFAULT NULL COMMENT '业务流程类型：business_process|workflow|lifecycle',
`stage_level` VARCHAR(32) DEFAULT NULL COMMENT '阶段级别（通用，可配置）',
`is_review_stage` TINYINT(1) DEFAULT 0 COMMENT '是否审查/评议阶段',
-- 向后兼容字段，计划 Phase 3 删除
`legal_process` VARCHAR(32) DEFAULT NULL COMMENT '[向后兼容] 已迁移到 process_type',
`court_level` VARCHAR(32) DEFAULT NULL COMMENT '[向后兼容] 已迁移到 stage_level',
`is_trial_stage` TINYINT(1) DEFAULT 0 COMMENT '[向后兼容] 已迁移到 is_review_stage',
```

### Task 1.3: 独立 ALTER TABLE 迁移脚本（PostgreSQL）

**文件：** `sql/migrations/v004_episode_type_column_rename.sql`

- [ ] **Step 1: 创建 ALTER TABLE 脚本**

```sql
-- ============================================================
-- v004_episode_type_column_rename.sql
-- 将 ont_episode_type 表的 legal_process/court_level/is_trial_stage
-- 重命名为 process_type/stage_level/is_review_stage
-- 执行前请确认无活跃长事务
-- ============================================================

BEGIN;

-- 1. 添加新字段（如果不存在）
ALTER TABLE ont_episode_type ADD COLUMN IF NOT EXISTS process_type VARCHAR(32);
ALTER TABLE ont_episode_type ADD COLUMN IF NOT EXISTS stage_level VARCHAR(32);
ALTER TABLE ont_episode_type ADD COLUMN IF NOT EXISTS is_review_stage BOOLEAN DEFAULT FALSE;

-- 2. 从旧字段迁移数据到新字段
UPDATE ont_episode_type SET process_type = legal_process WHERE process_type IS NULL AND legal_process IS NOT NULL;
UPDATE ont_episode_type SET stage_level = court_level WHERE stage_level IS NULL AND court_level IS NOT NULL;
UPDATE ont_episode_type SET is_review_stage = is_trial_stage WHERE is_review_stage = FALSE AND is_trial_stage = TRUE;

-- 3. 验证迁移结果
SELECT 'process_type count' as metric, count(process_type) as value FROM ont_episode_type
UNION ALL
SELECT 'stage_level count', count(stage_level) FROM ont_episode_type
UNION ALL
SELECT 'is_review_stage count', count(is_review_stage) FROM ont_episode_type WHERE is_review_stage = TRUE;

-- 4. 删除旧字段（确认迁移无误后执行）
-- ALTER TABLE ont_episode_type DROP COLUMN IF EXISTS legal_process;
-- ALTER TABLE ont_episode_type DROP COLUMN IF EXISTS court_level;
-- ALTER TABLE ont_episode_type DROP COLUMN IF EXISTS is_trial_stage;

COMMIT;
```

**注意：** 旧字段 DROP 注释掉，Phase 3（Neo4j 迁移完成后）再取消注释执行。

---

## Phase 2: Java DO / Mapper 层字段重命名

### Task 2.1: 更新 OntEpisodeTypeDO 字段

**文件：** `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/metadata/OntEpisodeTypeDO.java`

- [ ] **Step 1: 读取当前 OntEpisodeTypeDO 全部内容**

- [ ] **Step 2: 添加新字段，保留旧字段（注释标注向后兼容）**

在现有字段下方添加新字段：

```java
// ========== 通用化字段（Phase 4 新增）==========

/**
 * 业务流程类型：business_process|workflow|lifecycle
 */
private String processType;

/**
 * 阶段级别（通用，可配置。法律领域：一审/二审/再审；其他领域：可自定义）
 */
private String stageLevel;

/**
 * 是否审查/评议阶段
 */
private Boolean isReviewStage;

// ========== 向后兼容旧字段（Phase 3 迁移完成后删除）==========

/**
 * [向后兼容] 旧字段，已迁移到 processType
 */
private String legalProcess;

/**
 * [向后兼容] 旧字段，已迁移到 stageLevel
 */
private String courtLevel;

/**
 * [向后兼容] 旧字段，已迁移到 isReviewStage
 */
private Boolean isTrialStage;
```

- [ ] **Step 3: 在 getLegalProcess() 方法下方添加新 getter/setter（保持原有 getter 不变）**

```java
public String getProcessType() {
    return processType;
}

public void setProcessType(String processType) {
    this.processType = processType;
}

public String getStageLevel() {
    return stageLevel;
}

public void setStageLevel(String stageLevel) {
    this.stageLevel = stageLevel;
}

public Boolean getIsReviewStage() {
    return isReviewStage;
}

public void setIsReviewStage(Boolean isReviewStage) {
    this.isReviewStage = isReviewStage;
}
```

### Task 2.2: 更新 OntCommunityTypeDO 的 category 值说明

**文件：** `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/metadata/OntCommunityTypeDO.java`

- [ ] **Step 1: 读取 OntCommunityTypeDO，检查是否有 category 相关注释**

- [ ] **Step 2: 在 category 字段的注释中更新值说明**

```java
/**
 * 分类类别：
 *   domain(领域)|region(区域)|scenario(场景)
 * 旧值 jurisdiction(管辖区)、practice(应用场景) 已废弃，迁移到 region/scenario
 */
private String category;
```

### Task 2.3: 更新 OntEpisodeTypeMapper 查询方法

**文件：** `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/mysql/metadata/OntEpisodeTypeMapper.java`

- [ ] **Step 1: 读取当前 OntEpisodeTypeMapper.java**

- [ ] **Step 2: 修改 `selectByLegalProcess` 方法签名和 SQL**

旧：
```java
List<OntEpisodeTypeDO> selectByLegalProcess(@Param("definitionId") Long definitionId,
                                           @Param("legalProcess") String legalProcess);
```

新：
```java
List<OntEpisodeTypeDO> selectByProcessType(@Param("definitionId") Long definitionId,
                                          @Param("processType") String processType);
```

对应的 `@Select` SQL 改为：
```java
@Select("SELECT id, definition_id, type_code, type_name, type_name_en, " +
        "process_type, stage_label, stage_level, is_review_stage, " +
        "description, sort_order, metadata, status, created_at, updated_at " +
        "FROM ont_episode_type " +
        "WHERE definition_id = #{definitionId} AND process_type = #{processType} " +
        "AND status = 'ACTIVE' ORDER BY sort_order")
```

- [ ] **Step 3: 在 `selectActiveByDefinitionId` 的 @Select 中同步更新列名**

将列名列表从 `legal_process, stage_label, court_level, is_trial_stage` 改为：
```sql
id, definition_id, type_code, type_name, type_name_en,
process_type, stage_label, stage_level, is_review_stage,
legal_process, court_level, is_trial_stage,  -- 向后兼容旧字段
description, sort_order, metadata, status, created_at, updated_at
```

- [ ] **Step 4: 在 `selectByTypeCode` 的 @Select 中同步更新列名**（同上）

### Task 2.4: 更新 OntCommunityTypeMapper（category 值映射）

**文件：** `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/mysql/metadata/OntCommunityTypeMapper.java`

- [ ] **Step 1: 读取当前 OntCommunityTypeMapper.java**

- [ ] **Step 2: 检查 `selectByCategory` 方法的 @Select 注解中的 category 值**

旧 SQL 可能过滤 `category = 'domain'` 等，需要改为支持新 category 值（`domain|region|scenario`）。

由于 category 值是字符串过滤，不需要改 SQL 语法，只需要确认 Java 侧传入的 category 值与 DB 中的值一致即可。无需修改 mapper。

### Task 2.5: 更新 OntMetadataServiceImpl — Episode 类型 CRUD

**文件：** `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/metadata/OntMetadataServiceImpl.java`

- [ ] **Step 1: 读取 OntMetadataServiceImpl 中 `listByProcess` 方法（约 line 240）**

找到 `selectByLegalProcess` 的调用处，改为调用 `selectByProcessType`：

旧：
```java
List<OntEpisodeTypeDO> listByLegalProcess(Long definitionId, String legalProcess);
```

新：
```java
List<OntEpisodeTypeDO> listByProcess(Long definitionId, String processType);
```

在实现中：
```java
@Override
public List<OntEpisodeTypeDO> listByProcess(Long definitionId, String processType) {
    return ontEpisodeTypeMapper.selectByProcessType(definitionId, processType);
}
```

- [ ] **Step 2: 在 `OntMetadataService` 接口中也更新方法签名**

**文件：** `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/metadata/OntMetadataService.java`

旧：
```java
List<OntEpisodeTypeDO> listByLegalProcess(Long definitionId, String legalProcess);
```

新：
```java
List<OntEpisodeTypeDO> listByProcess(Long definitionId, String processType);
```

- [ ] **Step 3: 更新 `createEpisodeType` 方法中 legal_process 字段的写入**

在 `OntMetadataServiceImpl.createEpisodeType` 中（约 line 180），将：
```java
episodeType.setLegalProcess(reqVO.getLegalProcess());
```
替换为：
```java
episodeType.setProcessType(reqVO.getProcessType());
episodeType.setStageLevel(reqVO.getStageLevel());
episodeType.setIsReviewStage(reqVO.getIsReviewStage());
// 向后兼容旧字段
episodeType.setLegalProcess(reqVO.getLegalProcess());
episodeType.setCourtLevel(reqVO.getCourtLevel());
episodeType.setIsTrialStage(reqVO.getIsTrialStage());
```

### Task 2.6: 更新 EpisodeTypeReqVO / RespVO

**文件：** `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/metadata/OntEpisodeTypeReqVO.java`

- [ ] **Step 1: 读取 OntEpisodeTypeReqVO.java**

- [ ] **Step 2: 添加新字段，保留旧字段（注释标注向后兼容）**

```java
// ========== 通用化字段 ==========
private String processType;
private String stageLevel;
private Boolean isReviewStage;

// ========== 向后兼容旧字段（Phase 3 迁移完成后删除）==========
private String legalProcess;
private String courtLevel;
private Boolean isTrialStage;
```

**文件：** `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/metadata/OntEpisodeTypeRespVO.java`

- [ ] **Step 3: 同样添加新字段并保留旧字段**

---

## Phase 3: DomainInferenceService（新增）

### Task 3.1: 创建 DomainInferenceResult DTO

**文件：** `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dto/DomainInferenceResult.java`（新建）

- [ ] **Step 1: 创建 DTO 类**

```java
package com.graphiti.module.graphiti.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainInferenceResult {
    /** 顶层领域代码，如 DOMAIN_LEGAL / DOMAIN_SOCIAL_GOV */
    private String domainType;
    /** 二级子领域代码，如 DOMAIN_SOCIAL_DISPUTE_MARRIAGE */
    private String subDomainType;
    /** 区域代码，如 REGION_CN */
    private String region;
    /** 场景类型代码，如 SCENARIO_LAW_REGULATE */
    private String scenarioType;
    /** 推断置信度 0.0~1.0 */
    private Double confidence;
    /** 推断理由 */
    private String reasoning;
    /** 用户是否手动覆盖了推断结果 */
    private boolean userOverridden;
}
```

### Task 3.2: 创建 SubTypeInferenceResult DTO

**文件：** `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dto/SubTypeInferenceResult.java`（新建）

- [ ] **Step 1: 创建 DTO 类**

```java
package com.graphiti.module.graphiti.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubTypeInferenceResult {
    private String domainType;
    private String region;
    private String scenarioType;
    private Double confidence;
    private String reasoning;
}
```

### Task 3.3: 创建 DomainInferenceService 接口

**文件：** `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/DomainInferenceService.java`（新建）

- [ ] **Step 1: 创建接口**

```java
package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.dto.DomainInferenceResult;
import com.graphiti.module.graphiti.dto.SubTypeInferenceResult;
import com.graphiti.module.graphiti.dal.dataobject.metadata.OntCommunityTypeDO;

import java.util.List;

public interface DomainInferenceService {
    /**
     * 两阶段领域推断。
     * 第一阶段：推断顶层领域（DOMAIN_LEGAL/FINANCE/ENTERPRISE/MEDICAL/SOCIAL_GOV）
     * 第二阶段：在该领域内推断 subDomainType、region、scenarioType
     *
     * @param communityName 社区名称
     * @param memberContents 成员实体的内容摘要列表（用于上下文分析）
     * @param availableTypes 可用的 ont_community_type 配置
     * @return 推断结果（含置信度）
     */
    DomainInferenceResult infer(String communityName,
                                List<String> memberContents,
                                List<OntCommunityTypeDO> availableTypes);

    /**
     * 第一阶段：仅推断顶层领域
     */
    String inferTopLevelDomain(String communityName, List<String> memberContents);

    /**
     * 第二阶段：在已知领域内推断子类型
     *
     * @param topLevelDomain 顶层领域代码
     * @param communityName 社区名称
     * @param memberContents 成员内容摘要
     * @param availableTypes 可用的 ont_community_type 配置
     * @return 子类型推断结果
     */
    SubTypeInferenceResult inferSubTypes(String topLevelDomain,
                                         String communityName,
                                         List<String> memberContents,
                                         List<OntCommunityTypeDO> availableTypes);
}
```

### Task 3.4: 创建 DomainInferenceServiceImpl

**文件：** `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/DomainInferenceServiceImpl.java`（新建）

- [ ] **Step 1: 创建实现类框架**

依赖注入（`LlmClientService` 是现有接口，完整类名见 `service/LlmClientService.java`）：
```java
@Service
public class DomainInferenceServiceImpl implements DomainInferenceService {

    private final LlmClientService llmClientService;

    private static final List<String> TOP_LEVEL_DOMAINS = List.of(
        "DOMAIN_LEGAL", "DOMAIN_FINANCE", "DOMAIN_ENTERPRISE", "DOMAIN_MEDICAL", "DOMAIN_SOCIAL_GOV"
    );
```

- [ ] **Step 2: 实现 `inferTopLevelDomain` 方法**

```java
@Override
public String inferTopLevelDomain(String communityName, List<String> memberContents) {
    if (communityName == null && (memberContents == null || memberContents.isEmpty())) {
        return "DOMAIN_ROOT";
    }

    String prompt = buildTopLevelDomainPrompt(communityName, memberContents);
    String llmResponse = llmClient.chat(prompt);

    for (String domain : TOP_LEVEL_DOMAINS) {
        if (llmResponse.contains(domain)) {
            return domain;
        }
    }
    return "DOMAIN_ROOT";  // 兜底
}
```

- [ ] **Step 3: 实现 `inferSubTypes` 方法**

```java
@Override
public SubTypeInferenceResult inferSubTypes(String topLevelDomain,
                                             String communityName,
                                             List<String> memberContents,
                                             List<OntCommunityTypeDO> availableTypes) {
    if (topLevelDomain == null || topLevelDomain.equals("DOMAIN_ROOT")) {
        return SubTypeInferenceResult.builder()
            .region("REGION_ROOT")
            .scenarioType("SCENARIO_ROOT")
            .confidence(0.3)
            .reasoning("顶层领域为 ROOT，返回通用默认值")
            .build();
    }

    String prompt = buildSubTypePrompt(topLevelDomain, communityName, memberContents, availableTypes);
    String llmResponse = llmClient.chat(prompt);

    return parseSubTypeResult(llmResponse, topLevelDomain);
}
```

- [ ] **Step 4: 实现 `infer` 方法（两阶段组合）**

```java
@Override
public DomainInferenceResult infer(String communityName,
                                    List<String> memberContents,
                                    List<OntCommunityTypeDO> availableTypes) {
    String topLevelDomain = inferTopLevelDomain(communityName, memberContents);
    SubTypeInferenceResult subTypes = inferSubTypes(topLevelDomain, communityName, memberContents, availableTypes);

    return DomainInferenceResult.builder()
        .domainType(topLevelDomain)
        .subDomainType(subTypes.getDomainType())
        .region(subTypes.getRegion() != null ? subTypes.getRegion() : "REGION_CN")
        .scenarioType(subTypes.getScenarioType())
        .confidence(subTypes.getConfidence())
        .reasoning(subTypes.getReasoning())
        .userOverridden(false)
        .build();
}
```

- [ ] **Step 5: 实现 `buildTopLevelDomainPrompt` 私有方法**

```java
private String buildTopLevelDomainPrompt(String communityName, List<String> memberContents) {
    StringBuilder sb = new StringBuilder();
    sb.append("你是一个领域分类专家。请根据以下社区信息，判断该社区属于哪个领域。\n\n");
    sb.append("已知顶层领域：\n");
    sb.append("- DOMAIN_LEGAL（法律）：处理法律纠纷、诉讼、合规、判决等\n");
    sb.append("- DOMAIN_FINANCE（金融）：处理银行、证券、保险、风险、信贷等\n");
    sb.append("- DOMAIN_ENTERPRISE（企业管理）：处理人力资源、财务、合规、治理等\n");
    sb.append("- DOMAIN_MEDICAL（医疗）：处理诊疗、药品、公共卫生等\n");
    sb.append("- DOMAIN_SOCIAL_GOV（社会治理）：处理社会综合治理事件，包括婚恋家庭纠纷、劳动人事争议纠纷、侵权责任纠纷、邻里关系纠纷、房屋物业纠纷、山林土地水利纠纷、消费服务纠纷、经济金融活动纠纷、行政纠纷与信访维权、咨询与公证服务等\n\n");
    sb.append("社区名称：").append(communityName != null ? communityName : "无").append("\n");
    sb.append("社区成员内容摘要：\n");
    if (memberContents != null && !memberContents.isEmpty()) {
        for (int i = 0; i < Math.min(memberContents.size(), 5); i++) {
            sb.append("- ").append(memberContents.get(i)).append("\n");
        }
    } else {
        sb.append("无\n");
    }
    sb.append("\n请从上述五个领域中选择最匹配的一个，返回领域代码（如 DOMAIN_LEGAL）。只返回一个代码，不需要解释。");
    return sb.toString();
}
```

- [ ] **Step 6: 实现 `buildSubTypePrompt` 私有方法**

```java
private String buildSubTypePrompt(String topLevelDomain,
                                   String communityName,
                                   List<String> memberContents,
                                   List<OntCommunityTypeDO> availableTypes) {
    StringBuilder sb = new StringBuilder();
    sb.append("已知该社区属于领域 ").append(topLevelDomain).append("。请推断其具体子类型。\n\n");

    // 将 availableTypes 按 parent 分类，过滤出该 topLevelDomain 的子类型
    List<OntCommunityTypeDO> subTypes = availableTypes.stream()
        .filter(t -> topLevelDomain.equals(t.getParentTypeCode()))
        .collect(Collectors.toList());

    sb.append("该领域下的子类型（type_code | type_name | category）：\n");
    for (OntCommunityTypeDO t : subTypes) {
        sb.append("- ").append(t.getTypeCode()).append(" | ").append(t.getTypeName())
          .append(" | ").append(t.getCategory()).append("\n");
    }

    sb.append("\n同时请从以下区域中选择最匹配的（默认 REGION_CN）：\n");
    sb.append("- REGION_ROOT（通用）\n- REGION_CN（中国）\n- REGION_US（美国）\n- REGION_EU（欧洲）\n");
    sb.append("\n请从以下场景中选择最匹配的（默认 SCENARIO_ROOT）：\n");
    sb.append("- SCENARIO_ROOT（通用场景）\n- SCENARIO_LAW_REGULATE（依法调解）\n- SCENARIO_FEEDBACK（反馈处置）\n- SCENARIO_GOVERNANCE（综合治理）\n- SCENARIO_PREVENTION（预防预警）\n");
    sb.append("- SCENARIO_JUDICIAL（司法实践）\n- SCENARIO_COMPLIANCE（合规管理）\n- SCENARIO_RISK（风险管控）\n\n");

    sb.append("社区名称：").append(communityName != null ? communityName : "无").append("\n");
    if (memberContents != null && !memberContents.isEmpty()) {
        sb.append("成员内容摘要：");
        for (int i = 0; i < Math.min(memberContents.size(), 3); i++) {
            sb.append("\n- ").append(memberContents.get(i));
        }
    }

    sb.append("\n\n请输出 JSON，格式：{\"domainType\":\"子领域代码\",\"region\":\"区域代码\",\"scenarioType\":\"场景代码\",\"confidence\":0.0~1.0,\"reasoning\":\"推断理由\"}");
    return sb.toString();
}
```

- [ ] **Step 7: 实现 `parseSubTypeResult` 私有方法（JSON 解析）**

```java
private SubTypeInferenceResult parseSubTypeResult(String llmResponse, String topLevelDomain) {
    try {
        // 尝试从 LLM 响应中提取 JSON
        int jsonStart = llmResponse.indexOf('{');
        int jsonEnd = llmResponse.lastIndexOf('}');
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            String jsonStr = llmResponse.substring(jsonStart, jsonEnd + 1);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(jsonStr);
            return SubTypeInferenceResult.builder()
                .domainType(node.has("domainType") ? node.get("domainType").asText() : null)
                .region(node.has("region") ? node.get("region").asText() : "REGION_CN")
                .scenarioType(node.has("scenarioType") ? node.get("scenarioType").asText() : "SCENARIO_ROOT")
                .confidence(node.has("confidence") ? node.get("confidence").asDouble() : 0.5)
                .reasoning(node.has("reasoning") ? node.get("reasoning").asText() : "")
                .build();
        }
    } catch (Exception e) {
        // 解析失败，忽略
    }
    // 解析失败时返回默认值
    return SubTypeInferenceResult.builder()
        .region("REGION_CN")
        .scenarioType("SCENARIO_ROOT")
        .confidence(0.3)
        .reasoning("LLM 解析失败，使用默认值")
        .build();
}
```

**注意：** `LLMClient` 是现有类，需确认其 `chat(String prompt)` 方法的签名。如果现有代码中用的是其他 LLM 调用方式（如 OpenAI Java SDK），请相应调整。

---

## Phase 4: CommunityServiceImpl 改造

### Task 4.1: 创建 CommunityCreateContext 类

**文件：** `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dto/CommunityCreateContext.java`（新建）

- [ ] **Step 1: 创建 DTO 类**

```java
package com.graphiti.module.graphiti.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityCreateContext {
    private String communityName;
    private String communityType;
    /** 领域类型（LLM 推断 + 用户覆盖），如 DOMAIN_SOCIAL_GOV */
    private String domainType;
    /** 二级子领域，如 DOMAIN_SOCIAL_DISPUTE_MARRIAGE */
    private String subDomainType;
    /** 区域，如 REGION_CN */
    private String region;
    /** 场景类型，如 SCENARIO_LAW_REGULATE */
    private String scenarioType;
    /** LLM 推断置信度 */
    private Double inferenceConfidence;
    /** 用户是否手动覆盖了推断结果 */
    private boolean userOverridden;
    private String summary;
    private String description;
    private Integer memberCount;
    private List<String> memberUuids;
    private String parentCommunityUuid;
}
```

### Task 4.2: 更新 CommunityService 接口

**文件：** `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/CommunityService.java`

- [ ] **Step 1: 读取 CommunityService.java**

- [ ] **Step 2: 添加新方法签名**

在接口中添加（保留旧方法用于向后兼容）：

```java
/**
 * 使用领域推断上下文创建社区
 * @param graphId 图谱ID
 * @param context 社区创建上下文（含 LLM 推断的 domain/region/scenarioType）
 * @return 创建结果
 */
Map<String, Object> createCommunity(String graphId, CommunityCreateContext context);
```

### Task 4.3: 更新 CommunityServiceImpl — 移除硬编码，引入 DomainInferenceService

**文件：** `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/CommunityServiceImpl.java`

- [ ] **Step 1: 读取 CommunityServiceImpl.java 全文**

- [ ] **Step 2: 在类顶部注入 DomainInferenceService 和 OntCommunityTypeMapper**

```java
@Autowired
private DomainInferenceService domainInferenceService;

@Autowired
private OntCommunityTypeMapper ontCommunityTypeMapper;
```

- [ ] **Step 3: 移除 `resolveCommunityType` 方法（约 line 220-231）**

删除整个方法。

- [ ] **Step 4: 移除 `resolveLegalDomain` 方法（约 line 236-245）**

删除整个方法。

- [ ] **Step 5: 在 `buildSingleCommunity` 方法中，将硬编码的 jurisdiction/practice_type 改为参数化**

找到 `buildSingleCommunity` 方法（约 line 154-162），将 Cypher 中的：
```cypher
legal_domain: $legal_domain,
jurisdiction: $jurisdiction,
practice_type: $practice_type
```
替换为：
```cypher
domain_type: $domain_type,
region: $region,
scenario_type: $scenario_type
```

同时在方法参数中新增：`String domainType, String region, String scenarioType`。

- [ ] **Step 6: 重构 `createCommunity` 方法**

在 `createCommunity` 方法（约 line 452-504）中，找到硬编码的 `legalDomain`/`jurisdiction`/`practiceType` 赋值处，替换为：

```java
// 如果 context 中没有传入 domainType，则使用 LLM 推断
CommunityCreateContext ctx = (CommunityCreateContext) body.get("inferenceContext");
String domainType = ctx != null ? ctx.getDomainType() : null;
String subDomainType = ctx != null ? ctx.getSubDomainType() : null;
String region = ctx != null ? ctx.getRegion() : "REGION_CN";
String scenarioType = ctx != null ? ctx.getScenarioType() : "SCENARIO_ROOT";
String summary = (String) body.getOrDefault("summary", "");
```

然后将 `buildSingleCommunity` 调用改为传入新参数。

- [ ] **Step 7: 实现 `createCommunity(String graphId, CommunityCreateContext context)` 方法**

在 CommunityServiceImpl 中新增方法（`createCommunity` 重载）：

```java
@Override
public Map<String, Object> createCommunity(String graphId, CommunityCreateContext context) {
    // 1. 如果 context 中无推断结果，自动调用 DomainInferenceService 推断
    if (context.getDomainType() == null || context.getDomainType().isEmpty()) {
        List<OntCommunityTypeDO> availableTypes = ontCommunityTypeMapper.selectActiveByDefinitionId(
            getDefinitionIdByGraphId(graphId));
        DomainInferenceResult result = domainInferenceService.infer(
            context.getCommunityName(),
            getMemberContents(context.getMemberUuids()),
            availableTypes
        );
        context.setDomainType(result.getDomainType());
        context.setSubDomainType(result.getSubDomainType());
        context.setRegion(result.getRegion());
        context.setScenarioType(result.getScenarioType());
        context.setInferenceConfidence(result.getConfidence());
        context.setUserOverridden(result.isUserOverridden());
    }

    // 2. 构建 Cypher 参数
    Map<String, Object> params = new HashMap<>();
    params.put("graph_id", graphId);
    params.put("uuid", UUID.randomUUID().toString());
    params.put("name", context.getCommunityName());
    params.put("summary", context.getSummary() != null ? context.getSummary() : "");
    params.put("community_type", context.getCommunityType() != null ? context.getCommunityType() : "top_level");
    params.put("member_count", context.getMemberCount() != null ? context.getMemberCount() : 0);
    params.put("domain_type", context.getDomainType());
    params.put("sub_domain_type", context.getSubDomainType());
    params.put("region", context.getRegion());
    params.put("scenario_type", context.getScenarioType());
    params.put("inference_confidence", context.getInferenceConfidence());
    params.put("created_at", Instant.now().toString());
    params.put("updated_at", Instant.now().toString());

    // 3. 执行 Cypher 创建节点
    String cypher = """
        CREATE (c:Community {graph_id: $graph_id, uuid: $uuid, name: $name,
          summary: $summary, member_count: $member_count,
          community_type: $community_type,
          domain_type: $domain_type,
          sub_domain_type: $sub_domain_type,
          region: $region,
          scenario_type: $scenario_type,
          inference_confidence: $inference_confidence,
          created_at: $created_at, updated_at: $updated_at})
        WITH c
        UNWIND $member_uuids as memberUuid
        MATCH (m:Entity {graph_id: $graph_id, uuid: memberUuid})
        CREATE (m)-[:HAS_COMMUNITY]->(c)
        RETURN c
        """;

    Map<String, Object> queryParams = new HashMap<>(params);
    queryParams.put("member_uuids", context.getMemberUuids() != null ? context.getMemberUuids() : List.of());

    Map<String, Object> result = writeTransaction(session -> {
        Result r = session.run(cypher, new ValueMapperParameters(queryParams));
        return r.single().asMap();
    });

    // 4. 构建返回结果
    Map<String, Object> response = new HashMap<>();
    response.put("uuid", context.getMemberUuids());
    response.put("inferenceConfidence", context.getInferenceConfidence());
    response.put("userOverridden", context.isUserOverridden());
    return response;
}
```

- [ ] **Step 8: 补充 `getMemberContents` 和 `getDefinitionIdByGraphId` 辅助方法**

```java
private List<String> getMemberContents(List<String> memberUuids) {
    if (memberUuids == null || memberUuids.isEmpty()) {
        return List.of();
    }
    String cypher = "MATCH (m:Entity) WHERE m.uuid IN $uuids RETURN m.name, m.summary LIMIT 5";
    List<String> contents = new ArrayList<>();
    try (Session session = neo4jDriver.session()) {
        Result result = session.run(cypher, Values.parameters("uuids", memberUuids));
        while (result.hasNext()) {
            Record record = result.next();
            String name = record.get("name").isNull() ? "" : record.get("name").asString();
            String summary = record.get("summary").isNull() ? "" : record.get("summary").asString();
            contents.add(name + (summary.isEmpty() ? "" : "：" + summary));
        }
    }
    return contents;
}

private Long getDefinitionIdByGraphId(String graphId) {
    List<OntDefinitionDO> definitions = definitionMapper.selectList(
        new LambdaQueryWrapper<OntDefinitionDO>()
            .eq(OntDefinitionDO::getGraphId, graphId)
            .orderByDesc(OntDefinitionDO::getId)
            .last("LIMIT 1")
    );
    return definitions.isEmpty() ? null : definitions.get(0).getId();
}
```

需要注入的字段（在类顶部已有 `neo4jDriver`，新增 `definitionMapper`）：
```java
private final Driver neo4jDriver;
private final LlmClientService llmClientService;
private final OntDefinitionMapper definitionMapper;  // 新增
```

**注意：** `getMemberContents` 中 `ValueMapperParameters` 来自 `org.neo4j.driver.ValueMapperParameters`，`readTransaction` 的写法需与现有 `CommunityServiceImpl` 中 `writeTransaction` 的用法保持一致（参考现有 Cypher 执行方式）。

---

## Phase 5: EpisodeServiceImpl + GraphNeo4jService 改造

### Task 5.1: 更新 GraphNeo4jService — Episode 节点属性重命名

**文件：** `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/GraphNeo4jService.java`

- [ ] **Step 1: 读取 GraphNeo4jService 中的 `createEpisode` 方法（约 line 602-658）**

- [ ] **Step 2: 修改 Cypher 中的属性名**

在 `createEpisode` 方法的 Cypher 语句中，找到：
```cypher
episode_type: $episode_type,
legal_process: $legal_process,
stage_label: $stage_label,
court_level: $court_level,
is_trial_stage: $is_trial_stage,
```
替换为：
```cypher
episode_type: $episode_type,
process_type: $process_type,
stage_label: $stage_label,
stage_level: $stage_level,
is_review_stage: $is_review_stage,
```

- [ ] **Step 3: 修改参数注入部分**

找到方法中构建 `props` Map 的地方，将：
```java
props.put("legal_process", v3Props.get("legal_process"));
props.put("court_level", v3Props.get("court_level"));
props.put("is_trial_stage", v3Props.get("is_trial_stage"));
```
替换为：
```java
props.put("process_type", v3Props.getOrDefault("process_type", v3Props.get("legal_process")));
props.put("stage_level", v3Props.getOrDefault("stage_level", v3Props.get("court_level")));
props.put("is_review_stage", v3Props.getOrDefault("is_review_stage", v3Props.get("is_trial_stage")));
```

这样实现向后兼容：优先读新字段，旧字段存在时也兼容读取。

### Task 5.2: 更新 EpisodeServiceImpl 中的字段引用

**文件：** `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/EpisodeServiceImpl.java`

- [ ] **Step 1: 读取 EpisodeServiceImpl 全文**

- [ ] **Step 2: 在 `createEpisode` 方法中，将 v3Props 中的旧字段引用替换为新字段名**

在 `createEpisode` 方法（约 line 88-107）中，找到：
```java
v3Props.put("legal_process", episodeData.getOrDefault("legal_process", "business_process"));
v3Props.put("court_level", episodeData.get("court_level"));
v3Props.put("is_trial_stage", episodeData.getOrDefault("is_trial_stage", false));
```
替换为：
```java
// 优先使用新字段，兼容旧字段作为 fallback
v3Props.put("process_type", episodeData.getOrDefault("process_type", episodeData.getOrDefault("legal_process", "business_process")));
v3Props.put("stage_level", episodeData.getOrDefault("stage_level", episodeData.get("court_level")));
v3Props.put("is_review_stage", episodeData.getOrDefault("is_review_stage", episodeData.getOrDefault("is_trial_stage", false)));
```

- [ ] **Step 3: 在 `convertToEpisodeInfo` 方法中，新增新字段的映射**

找到 `convertToEpisodeInfo` 方法（约 line 155-172），在返回的 `EpisodeInfoRespVO` 构建处添加：
```java
resp.setProcessType((String) row.get("process_type"));
resp.setStageLevel((String) row.get("stage_level"));
resp.setIsReviewStage(row.get("is_review_stage") != null ? (Boolean) row.get("is_review_stage") : null);
// 向后兼容旧字段
resp.setLegalProcess((String) row.get("legal_process"));
resp.setCourtLevel((String) row.get("court_level"));
resp.setIsTrialStage(row.get("is_trial_stage") != null ? (Boolean) row.get("is_trial_stage") : null);
```

### Task 5.3: 更新 EpisodeInfoRespVO

**文件：** `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/EpisodeInfoRespVO.java`（或对应 RespVO 文件）

- [ ] **Step 1: 读取 RespVO 文件**

- [ ] **Step 2: 添加新字段（保留旧字段）**

```java
// 通用化字段
private String processType;
private String stageLevel;
private Boolean isReviewStage;

// 向后兼容旧字段
private String legalProcess;
private String courtLevel;
private Boolean isTrialStage;
```

添加对应 getter/setter。

---

## Phase 6: ont_community_type 初始数据（五领域）

### Task 6.1: 写入法律、金融、企业管理、医疗顶层数据

**文件：** `sql/postgresql/init-data-v3.sql`

- [ ] **Step 1: 读取当前 init-data-v3.sql 文件，了解现有 INSERT 语句格式**

- [ ] **Step 2: 追加新的五领域 INSERT 语句**

在文件末尾追加（保持与现有格式一致，使用 JSONB 语法）：

```sql
-- ============================================================
-- 五领域通用分类体系初始数据
-- Phase 4: 社区系统通用化改造
-- ============================================================

-- 1. 顶层领域（domain）
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_ROOT', '知识领域', 'domain', NULL, 0, '{"color": "#607D8B"}', 'ACTIVE'),
  (1, 'DOMAIN_LEGAL', '法律', 'domain', 'DOMAIN_ROOT', 1, '{"color": "#1565C0"}', 'ACTIVE'),
  (1, 'DOMAIN_FINANCE', '金融', 'domain', 'DOMAIN_ROOT', 10, '{"color": "#2E7D32"}', 'ACTIVE'),
  (1, 'DOMAIN_ENTERPRISE', '企业管理', 'domain', 'DOMAIN_ROOT', 20, '{"color": "#E65100"}', 'ACTIVE'),
  (1, 'DOMAIN_MEDICAL', '医疗', 'domain', 'DOMAIN_ROOT', 30, '{"color": "#C62828"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_GOV', '社会治理', 'domain', 'DOMAIN_ROOT', 40, '{"color": "#6A1B9A"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 2. 法律子领域
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_CIVIL', '民商事', 'domain', 'DOMAIN_LEGAL', 1, '{"color": "#1976D2"}', 'ACTIVE'),
  (1, 'DOMAIN_CRIMINAL', '刑事法律', 'domain', 'DOMAIN_LEGAL', 2, '{"color": "#D32F2F"}', 'ACTIVE'),
  (1, 'DOMAIN_ADMIN', '行政法律', 'domain', 'DOMAIN_LEGAL', 3, '{"color": "#F57C00"}', 'ACTIVE'),
  (1, 'DOMAIN_IP', '知识产权', 'domain', 'DOMAIN_LEGAL', 4, '{"color": "#7B1FA2"}', 'ACTIVE'),
  (1, 'DOMAIN_LABOR', '劳动法律', 'domain', 'DOMAIN_LEGAL', 5, '{"color": "#388E3C"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 3. 金融子领域
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_BANKING', '银行与信贷', 'domain', 'DOMAIN_FINANCE', 1, '{"color": "#1B5E20"}', 'ACTIVE'),
  (1, 'DOMAIN_SECURITIES', '证券与投资', 'domain', 'DOMAIN_FINANCE', 2, '{"color": "#004D40"}', 'ACTIVE'),
  (1, 'DOMAIN_INSURANCE', '保险业务', 'domain', 'DOMAIN_FINANCE', 3, '{"color": "#006064"}', 'ACTIVE'),
  (1, 'DOMAIN_RISK', '风险管控', 'domain', 'DOMAIN_FINANCE', 4, '{"color": "#263238"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 4. 企业管理子领域
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_HR', '人力资源', 'domain', 'DOMAIN_ENTERPRISE', 1, '{"color": "#E65100"}', 'ACTIVE'),
  (1, 'DOMAIN_FINANCE_MGMT', '财务管理', 'domain', 'DOMAIN_ENTERPRISE', 2, '{"color": "#BF360C"}', 'ACTIVE'),
  (1, 'DOMAIN_COMPLIANCE', '企业合规', 'domain', 'DOMAIN_ENTERPRISE', 3, '{"color": "#E64A19"}', 'ACTIVE'),
  (1, 'DOMAIN_GOVERNANCE', '公司治理', 'domain', 'DOMAIN_ENTERPRISE', 4, '{"color": "#D84315"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 5. 医疗子领域
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_CLINICAL', '临床诊疗', 'domain', 'DOMAIN_MEDICAL', 1, '{"color": "#B71C1C"}', 'ACTIVE'),
  (1, 'DOMAIN_DRUG', '药品与器械', 'domain', 'DOMAIN_MEDICAL', 2, '{"color": "#880E4F"}', 'ACTIVE'),
  (1, 'DOMAIN_PUBLIC_HEALTH', '公共卫生', 'domain', 'DOMAIN_MEDICAL', 3, '{"color": "#4A148C"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;
```

### Task 6.2: 写入社会治理子领域（10 个一级 + 二级分类）

- [ ] **Step 1: 追加社会治理一级分类**

```sql
-- 6. 社会治理一级分类（10 个）
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', '婚恋家庭纠纷', 'domain', 'DOMAIN_SOCIAL_GOV', 1, '{"color": "#AD1457"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LABOR', '劳动人事争议纠纷', 'domain', 'DOMAIN_SOCIAL_GOV', 2, '{"color": "#6A1B9A"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT', '侵权责任纠纷', 'domain', 'DOMAIN_SOCIAL_GOV', 3, '{"color": "#4527A0"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR', '邻里关系纠纷', 'domain', 'DOMAIN_SOCIAL_GOV', 4, '{"color": "#283593"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_PROPERTY', '房屋物业纠纷', 'domain', 'DOMAIN_SOCIAL_GOV', 5, '{"color": "#1565C0"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LAND', '山林土地水利纠纷', 'domain', 'DOMAIN_SOCIAL_GOV', 6, '{"color": "#00838F"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', '消费服务纠纷', 'domain', 'DOMAIN_SOCIAL_GOV', 7, '{"color": "#00695C"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', '经济金融活动纠纷', 'domain', 'DOMAIN_SOCIAL_GOV', 8, '{"color": "#2E7D32"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION', '行政纠纷与信访维权', 'domain', 'DOMAIN_SOCIAL_GOV', 9, '{"color": "#558B2F"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_CONSULT_SERVICE', '咨询与公证服务', 'domain', 'DOMAIN_SOCIAL_GOV', 10, '{"color": "#F9A825"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;
```

- [ ] **Step 2: 追加婚恋家庭纠纷二级分类**

```sql
-- 婚恋家庭纠纷二级分类（11 项）
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_01', '夫妻关系矛盾纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 1, '{"color": "#F48FB1"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_02', '离异夫妻矛盾纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 2, '{"color": "#F06292"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_03', '未婚恋爱纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 3, '{"color": "#EC407A"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_04', '同居关系纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 4, '{"color": "#E91E63"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_05', '分家、继承与赡养纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 5, '{"color": "#D81B60"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_06', '父母子女矛盾纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 6, '{"color": "#C2185B"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_07', '兄弟姐妹矛盾纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 7, '{"color": "#AD1457"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_08', '家庭其它成员矛盾纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 8, '{"color": "#880E4F"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_09', '婚姻自主权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 9, '{"color": "#7B1FA2"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_10', '宣告失踪、死亡纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 10, '{"color": "#6A1B9A"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_11', '认定无民事行为能力纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 11, '{"color": "#4A148C"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;
```

- [ ] **Step 3: 追加劳动人事争议纠纷（8 项）、侵权责任纠纷（18 项）、邻里关系纠纷（5 项）、房屋物业纠纷（7 项）、山林土地水利纠纷（6 项）**

格式同上，INSERT INTO 语句。注意每个一级分类下批量 INSERT，使用 `ON CONFLICT (definition_id, type_code) DO NOTHING`。

- [ ] **Step 4: 追加消费服务纠纷（12 项）、经济金融活动纠纷（12 项）、行政纠纷与信访维权（26 项）、咨询与公证服务（12 项）**

格式同上。

### Task 6.3: 追加 region 和 scenario 初始数据

```sql
-- 7. 区域分类（region）
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'REGION_ROOT', '全球/通用', 'region', NULL, 0, '{"color": "#9E9E9E"}', 'ACTIVE'),
  (1, 'REGION_CN', '中国', 'region', NULL, 1, '{"color": "#D32F2F"}', 'ACTIVE'),
  (1, 'REGION_US', '美国', 'region', NULL, 2, '{"color": "#1565C0"}', 'ACTIVE'),
  (1, 'REGION_EU', '欧洲', 'region', NULL, 3, '{"color": "#1976D2"}', 'ACTIVE'),
  (1, 'REGION_INTERNATIONAL', '国际', 'region', NULL, 4, '{"color": "#7B1FA2"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 8. 场景分类（scenario）
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'SCENARIO_ROOT', '通用场景', 'scenario', NULL, 0, '{"color": "#607D8B"}', 'ACTIVE'),
  (1, 'SCENARIO_JUDICIAL', '司法实践', 'scenario', NULL, 1, '{"color": "#1565C0"}', 'ACTIVE'),
  (1, 'SCENARIO_COMPLIANCE', '合规管理', 'scenario', NULL, 2, '{"color": "#2E7D32"}', 'ACTIVE'),
  (1, 'SCENARIO_RISK', '风险管控', 'scenario', NULL, 3, '{"color": "#E65100"}', 'ACTIVE'),
  (1, 'SCENARIO_LIFECYCLE', '生命周期', 'scenario', NULL, 4, '{"color": "#C62828"}', 'ACTIVE'),
  (1, 'SCENARIO_LAW_REGULATE', '依法调解', 'scenario', NULL, 5, '{"color": "#6A1B9A"}', 'ACTIVE'),
  (1, 'SCENARIO_FEEDBACK', '反馈处置', 'scenario', NULL, 6, '{"color": "#00838F"}', 'ACTIVE'),
  (1, 'SCENARIO_GOVERNANCE', '综合治理', 'scenario', NULL, 7, '{"color": "#558B2F"}', 'ACTIVE'),
  (1, 'SCENARIO_PREVENTION', '预防预警', 'scenario', NULL, 8, '{"color": "#F9A825"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;
```

---

## Phase 7: ont_episode_type 初始数据（通用 + 社会治理 + 法律）

### Task 7.1: 追加 Episode 通用化初始数据

**文件：** `sql/postgresql/init-data-v3.sql`（追加）

- [ ] **Step 1: 追加 process_type 通用 Episode 数据**

```sql
-- ============================================================
-- Episode 类型初始数据（通用 + 社会治理 + 法律）
-- Phase 4: 社区系统通用化改造
-- ============================================================

-- 1. 通用流程类型（lifecycle/workflow，跨领域适用）
INSERT INTO ont_episode_type (definition_id, type_code, type_name, process_type, stage_label, stage_level, is_review_stage, sort_order, metadata, status)
VALUES
  (1, 'EP_INITIATION', '发起/启动', 'lifecycle', '启动', NULL, FALSE, 1, '{"color": "#4CAF50"}', 'ACTIVE'),
  (1, 'EP_EVALUATION', '评估/审查', 'lifecycle', '审查', NULL, TRUE, 2, '{"color": "#FF9800"}', 'ACTIVE'),
  (1, 'EP_EXECUTION', '执行/实施', 'lifecycle', '执行', NULL, FALSE, 3, '{"color": "#2196F3"}', 'ACTIVE'),
  (1, 'EP_RESOLUTION', '解决/终结', 'lifecycle', '终结', NULL, FALSE, 4, '{"color": "#9C27B0"}', 'ACTIVE'),
  (1, 'EP_WORKFLOW_START', '流程启动', 'workflow', '启动', NULL, FALSE, 10, '{"color": "#00BCD4"}', 'ACTIVE'),
  (1, 'EP_WORKFLOW_NODE', '流程节点', 'workflow', '流转', NULL, FALSE, 11, '{"color": "#3F51B5"}', 'ACTIVE'),
  (1, 'EP_WORKFLOW_END', '流程结束', 'workflow', '结束', NULL, FALSE, 12, '{"color": "#795548"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;
```

- [ ] **Step 2: 追加社会治理领域 Episode 类型**

```sql
-- 2. 社会治理领域 Episode 类型
INSERT INTO ont_episode_type (definition_id, type_code, type_name, process_type, stage_label, stage_level, is_review_stage, sort_order, metadata, status)
VALUES
  (1, 'EP_REPORT_RECEIVE', '事件接收', 'lifecycle', '接收', NULL, FALSE, 20, '{"color": "#E91E63"}', 'ACTIVE'),
  (1, 'EP_TRIAGE_ASSESS', '事件分流评估', 'workflow', '评估', NULL, TRUE, 21, '{"color": "#FF5722"}', 'ACTIVE'),
  (1, 'EP_MEDIATION', '调解处理', 'workflow', '调解', NULL, FALSE, 22, '{"color": "#9C27B0"}', 'ACTIVE'),
  (1, 'EP_COORDINATION', '协调处置', 'workflow', '协调', NULL, FALSE, 23, '{"color": "#673AB7"}', 'ACTIVE'),
  (1, 'EP_FEEDBACK', '结果反馈', 'lifecycle', '反馈', NULL, FALSE, 24, '{"color": "#2196F3"}', 'ACTIVE'),
  (1, 'EP_FOLLOW_UP', '跟踪回访', 'lifecycle', '回访', NULL, FALSE, 25, '{"color": "#00BCD4"}', 'ACTIVE'),
  (1, 'EP_CLOSE', '事件办结', 'lifecycle', '办结', NULL, FALSE, 26, '{"color": "#4CAF50"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;
```

- [ ] **Step 3: 追加法律领域 Episode 类型（带 court_level）**

```sql
-- 3. 法律领域 Episode 类型（保留 court_level，体现领域特色）
INSERT INTO ont_episode_type (definition_id, type_code, type_name, process_type, stage_label, stage_level, is_review_stage, sort_order, metadata, status)
VALUES
  (1, 'EP_FILING', '立案', 'business_process', '立案', NULL, FALSE, 30, '{"color": "#1976D2"}', 'ACTIVE'),
  (1, 'EP_TRIAL_1ST', '一审庭审', 'business_process', '庭审', '一审', TRUE, 31, '{"color": "#F57C00"}', 'ACTIVE'),
  (1, 'EP_JUDGMENT_1ST', '一审判决', 'business_process', '判决', '一审', TRUE, 32, '{"color": "#D32F2F"}', 'ACTIVE'),
  (1, 'EP_APPEAL', '上诉', 'business_process', '上诉', '二审', TRUE, 33, '{"color": "#7B1FA2"}', 'ACTIVE'),
  (1, 'EP_TRIAL_2ND', '二审庭审', 'business_process', '庭审', '二审', TRUE, 34, '{"color": "#F57C00"}', 'ACTIVE'),
  (1, 'EP_JUDGMENT_2ND', '二审判决', 'business_process', '判决', '二审', TRUE, 35, '{"color": "#D32F2F"}', 'ACTIVE'),
  (1, 'EP_EXECUTION_LEGAL', '执行', 'business_process', '执行', NULL, FALSE, 36, '{"color": "#388E3C"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;
```

---

## Phase 8: 前端层改造

### Task 8.1: 更新 TypeScript 类型定义

**文件：** `graphiti-web/src/types/legal-graph-v3.ts`

- [ ] **Step 1: 读取 legal-graph-v3.ts**

- [ ] **Step 2: 在 CommunityV3 接口中添加新字段**

```typescript
// CommunityV3 — 通用化改造
interface CommunityV3 {
  // ... 现有字段

  // 新增通用化字段
  domainType?: string      // 替换 legalDomain，领域类型
  subDomainType?: string   // 二级子领域（如社会治理的婚恋家庭纠纷）
  region?: string          // 替换 jurisdiction，区域
  scenarioType?: string    // 替换 practiceType，场景类型
  inferenceConfidence?: number  // LLM 推断置信度
  userOverridden?: boolean     // 用户是否覆盖了 LLM 推断
}
```

### Task 8.2: 更新 metadata.ts API

**文件：** `graphiti-web/src/api/metadata.ts`

- [ ] **Step 1: 读取 metadata.ts**

- [ ] **Step 2: 在 OntEpisodeTypeVO 中添加新字段**

```typescript
export interface OntEpisodeTypeVO {
  // ... 现有字段
  processType?: string    // 新增
  stageLevel?: string     // 新增（替换 courtLevel）
  isReviewStage?: boolean // 新增（替换 isTrialStage）
  // 向后兼容旧字段
  legalProcess?: string
  courtLevel?: string
  isTrialStage?: boolean
}
```

- [ ] **Step 3: 确认 `communityTypeApi.list()` 和 `communityTypeApi.getTree()` 的返回类型**

`list()` 返回 `OntCommunityTypeVO[]`，已包含 `category`/`parentTypeCode`/`metadata` 字段，前端可直接按 `category === 'domain'/'region'/'scenario'` 过滤。

### Task 8.3: 更新 community-episode.vue 下拉选项数据来源

**文件：** `graphiti-web/src/views/data/community-episode.vue`

- [ ] **Step 1: 读取 community-episode.vue，定位下拉选项的定义位置**

- [ ] **Step 2: 替换硬编码下拉选项为从 API 拉取**

找到硬编码的 `domainOptions`、`regionOptions`、`scenarioOptions` 定义（约在 `<script setup>` 部分），替换为：

```typescript
import { communityTypeApi } from '@/api/metadata'

// 从 ont_community_type 表实时拉取
const communityTypes = ref<OntCommunityTypeVO[]>([])

const loadCommunityTypes = async () => {
  try {
    const res = await communityTypeApi.list(graphId)
    communityTypes.value = res.data || []
  } catch (error) {
    console.error('加载社区类型失败:', error)
  }
}

// 一级领域选项（category === 'domain'，parent_type_code === null 或 'DOMAIN_ROOT'）
const domainOptions = computed(() =>
  communityTypes.value.filter(t =>
    t.category === 'domain' &&
    (t.parentTypeCode === 'DOMAIN_ROOT' || t.parentTypeCode === null)
  )
)

// 子领域选项（根据选中的顶层领域动态过滤）
const subDomainOptions = computed(() => {
  if (!form.domainType) return []
  return communityTypes.value.filter(t =>
    t.category === 'domain' && t.parentTypeCode === form.domainType
  )
})

// 区域选项（category === 'region'）
const regionOptions = computed(() =>
  communityTypes.value.filter(t => t.category === 'region')
)

// 场景选项（category === 'scenario'）
const scenarioOptions = computed(() =>
  communityTypes.value.filter(t => t.category === 'scenario')
)
```

- [ ] **Step 3: 在组件 mounted 或 setup 中调用 loadCommunityTypes**

```typescript
onMounted(async () => {
  await loadCommunityTypes()
  // ... 其他初始化逻辑
})
```

- [ ] **Step 4: 替换模板中的硬编码下拉选项**

将模板中硬编码的 `<el-option>` 替换为：

```html
<el-select v-model="form.domainType" placeholder="请选择领域">
  <el-option
    v-for="item in domainOptions"
    :key="item.typeCode"
    :label="item.typeName"
    :value="item.typeCode"
  />
</el-select>

<el-select v-model="form.subDomainType" placeholder="请选择子领域" :disabled="!form.domainType">
  <el-option
    v-for="item in subDomainOptions"
    :key="item.typeCode"
    :label="item.typeName"
    :value="item.typeCode"
  />
</el-select>

<el-select v-model="form.region" placeholder="请选择区域">
  <el-option
    v-for="item in regionOptions"
    :key="item.typeCode"
    :label="item.typeName"
    :value="item.typeCode"
  />
</el-select>

<el-select v-model="form.scenarioType" placeholder="请选择场景">
  <el-option
    v-for="item in scenarioOptions"
    :key="item.typeCode"
    :label="item.typeName"
    :value="item.typeCode"
  />
</el-select>
```

### Task 8.4: 更新 IDE 图谱页面 — 社区色彩映射

**文件：** `graphiti-web/src/views/graph/ide.vue`

- [ ] **Step 1: 读取 ide.vue，搜索 `getCommunityColor` 或硬编码色彩常量**

- [ ] **Step 2: 将硬编码色彩映射替换为从 metadata.color 读取**

找到类似以下代码：
```typescript
const getCommunityColor = (domain: string) => {
  const map: Record<string, string> = {
    DOMAIN_CIVIL: '#1565C0',
    DOMAIN_CRIMINAL: '#D32F2F',
    // ... 硬编码映射
  }
  return map[domain] || '#37474F'
}
```

替换为：
```typescript
const communityTypesMap = computed(() => {
  const map = new Map<string, OntCommunityTypeVO>()
  for (const t of communityTypes.value) {
    map.set(t.typeCode, t)
  }
  return map
})

const getCommunityColor = (typeCode: string) => {
  const type = communityTypesMap.value.get(typeCode)
  if (type?.metadata && typeof type.metadata === 'object' && 'color' in type.metadata) {
    return (type.metadata as { color: string }).color
  }
  return '#37474F'  // 默认灰色
}

const getCommunityName = (typeCode: string) => {
  const type = communityTypesMap.value.get(typeCode)
  return type?.typeName || typeCode
}
```

同时确保 `communityTypes` 数据在 ide.vue 中被正确初始化（如果 ide.vue 还未加载 communityTypes，需要调用 `communityTypeApi.list(graphId)`）。

---

## Phase 9: Neo4j 迁移脚本

### Task 9.1: 创建 Neo4j 属性重命名迁移脚本

**文件：** `sql/migrations/v004_community_generic_rename.cypher`（新建）

- [ ] **Step 1: 创建 Cypher 迁移脚本**

```cypher
// ============================================================
// v004_community_generic_rename.cypher
// 将 Community 节点的 legal_domain/jurisdiction/practice_type
// 重命名为 domain_type/region/scenario_type
// 将 Episode 节点的 legal_process/court_level/is_trial_stage
// 重命名为 process_type/stage_level/is_review_stage
//
// 执行方式（Neo4j Browser 或 cypher-shell）：
// :source sql/migrations/v004_community_generic_rename.cypher
// ============================================================

// 1. Community 节点属性重命名
MATCH (c:Community)
WHERE c.legal_domain IS NOT NULL
SET c.domain_type = c.legal_domain
      + COALESCE(c.sub_domain_type, '')
REMOVE c.legal_domain
WITH c
SET c.region = COALESCE(c.jurisdiction, 'REGION_CN')
REMOVE c.jurisdiction
WITH c
SET c.scenario_type = COALESCE(c.practice_type, 'SCENARIO_ROOT')
REMOVE c.practice_type;

// 2. Episode 节点属性重命名
MATCH (e:Episode)
WHERE e.legal_process IS NOT NULL
SET e.process_type = e.legal_process
REMOVE e.legal_process
WITH e
SET e.stage_level = COALESCE(e.court_level, NULL)
REMOVE e.court_level
WITH e
SET e.is_review_stage = COALESCE(e.is_trial_stage, FALSE)
REMOVE e.is_trial_stage;

// 3. 验证迁移结果
// Community 验证：应全部有 domain_type
MATCH (c:Community)
RETURN 'Community 总数' as metric, count(c) as total,
       count(c.domain_type) as with_domain_type,
       count(c.region) as with_region,
       count(c.scenario_type) as with_scenario_type;

// Episode 验证：应全部有 process_type
MATCH (e:Episode)
RETURN 'Episode 总数' as metric, count(e) as total,
       count(e.process_type) as with_process_type,
       count(e.stage_level) as with_stage_level,
       count(e.is_review_stage) as with_is_review_stage;

// 4. 确认旧属性已全部删除（查询应返回 0 行）
MATCH (c:Community) WHERE c.legal_domain IS NOT NULL RETURN count(c) as legacy_legal_domain_count;
MATCH (e:Episode) WHERE e.legal_process IS NOT NULL RETURN count(e) as legacy_legal_process_count;
```

### Task 9.2: 创建回滚脚本

**文件：** `sql/migrations/v004_rollback.cypher`（新建）

```cypher
// 回滚脚本（如果迁移出错，执行此脚本）
// 将 domain_type/region/scenario_type 回滚为 legal_domain/jurisdiction/practice_type

// Community 回滚
MATCH (c:Community)
WHERE c.domain_type IS NOT NULL
SET c.legal_domain = c.domain_type
REMOVE c.domain_type
WITH c
SET c.jurisdiction = c.region
REMOVE c.region
WITH c
SET c.practice_type = c.scenario_type
REMOVE c.scenario_type;

// Episode 回滚
MATCH (e:Episode)
WHERE e.process_type IS NOT NULL
SET e.legal_process = e.process_type
REMOVE e.process_type
WITH e
SET e.court_level = e.stage_level
REMOVE e.stage_level
WITH e
SET e.is_trial_stage = e.is_review_stage
REMOVE e.is_review_stage;
```

---

## Phase 10: 文档更新

### Task 10.1: 更新 legal_graph.md

**文件：** `docs/legal_graph.md`

- [ ] **Step 1: 读取 legal_graph.md**

- [ ] **Step 2: 将文档中的"法律知识图谱"描述扩展为"多领域知识图谱"**

更新介绍段落，说明现在支持五大领域。

- [ ] **Step 3: 更新 Community 节点属性说明**

将 legal_domain/jurisdiction/practice_type 的说明替换为 domain_type/region/scenario_type 的新说明。

### Task 10.2: 创建/更新 ontology.md

**文件：** `docs/ontology.md`（或新建 `docs/ontology-v4.md`）

- [ ] **Step 1: 创建 ontology 文档，描述通用化改造后的元数据体系**

```markdown
# 本体论（Ontology）— 通用化改造 v4.0.0

## 概述

从 v4.0.0 起，Graphiti 支持多领域知识图谱（法律、金融、企业管理、医疗、社会治理）。

## 社区分类体系

### 维度一：领域（domain）

从 `ont_community_type` 表中 `category = 'domain'` 的记录读取。

五大顶层领域：DOMAIN_LEGAL、DOMAIN_FINANCE、DOMAIN_ENTERPRISE、DOMAIN_MEDICAL、DOMAIN_SOCIAL_GOV。

每个顶层领域下有若干子领域。社会治理领域采用标准纠纷分类体系（10 个一级分类，200+ 二级分类）。

### 维度二：区域（region）

从 `ont_community_type` 表中 `category = 'region'` 的记录读取。

### 维度三：场景（scenario）

从 `ont_community_type` 表中 `category = 'scenario'` 的记录读取。

## Episode 流程类型

从 `ont_episode_type` 表读取，支持 `process_type` = lifecycle/workflow/business_process。

社会治理领域：接收 → 评估 → 调解 → 协调 → 反馈 → 回访 → 办结。
法律领域：立案 → 庭审 → 判决 → 上诉 → 执行。
```

---

## 自检清单

完成实施后，逐项确认：

- [ ] PostgreSQL DDL 执行成功（`ont_episode_type` 三个字段重命名）
- [ ] MySQL DDL 执行成功
- [ ] `OntEpisodeTypeDO` / `OntCommunityTypeDO` 字段添加完成
- [ ] `OntEpisodeTypeMapper` 的 `@Select` 列名更新（包含新旧字段）
- [ ] `DomainInferenceService` / `DomainInferenceServiceImpl` 创建完成
- [ ] `CommunityCreateContext` 创建完成
- [ ] `CommunityServiceImpl` 中 `resolveCommunityType`/`resolveLegalDomain` 删除
- [ ] `CommunityServiceImpl` 的 Cypher 写入使用 `domain_type/region/scenario_type`
- [ ] `GraphNeo4jService` Episode 属性名更新
- [ ] `EpisodeServiceImpl` 字段引用更新
- [ ] 五领域初始数据 INSERT 执行成功
- [ ] `community-episode.vue` 下拉从 API 拉取
- [ ] `ide.vue` 色彩从 `metadata.color` 读取
- [ ] Neo4j 迁移脚本在测试环境执行成功
- [ ] `legal_graph.md` 文档更新
- [ ] 后端单元测试通过（DomainInferenceService）
- [ ] 前端构建成功（无 TypeScript 错误）
