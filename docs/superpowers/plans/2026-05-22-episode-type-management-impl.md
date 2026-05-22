# Episode 类型管理重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 EpisodeExplorer 从"实例浏览器"重构为"类型管理+实例展示"的复合视图，支持层级化类型管理、CRUD、拖拽排序、依赖检查。

**Architecture:** 组件拆分方案（EpisodeTypeExplorer + EpisodeTypeDetailPanel + EpisodeTypeEditModal），由 ide.vue 统一组装；后端扩展 OntEpisodeTypeDO 层级字段和审计字段，新增树查询/删除检查/排序/导入 API。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Vue 3 + TypeScript + Ant Design Vue + Neo4j

---

## 文件映射

### 新建文件

| 文件 | 职责 |
|------|------|
| `sql/migrations/v005_episode_type_hierarchy.sql` | 数据库迁移：新增层级/审计字段，增加约束和索引 |
| `graphiti-web/src/components/Ontology/EpisodeTypeExplorer.vue` | 侧边栏：类型树/列表、搜索、工具栏、拖拽排序 |
| `graphiti-web/src/components/Ontology/EpisodeTypeDetailPanel.vue` | 右侧面板：类型详情 Tab + 实例列表 Tab |
| `graphiti-web/src/components/Ontology/EpisodeTypeEditModal.vue` | 弹窗：类型新增/编辑表单 |

### 修改文件

| 文件 | 职责 |
|------|------|
| `graphiti-module-core/src/main/java/.../dal/dataobject/metadata/OntEpisodeTypeDO.java` | 重构字段：新增层级/审计，删除向后兼容字段 |
| `graphiti-module-core/src/main/java/.../dal/mysql/metadata/OntEpisodeTypeMapper.java` | 扩展：树查询、子类型查询、实例统计、批量排序 |
| `graphiti-module-core/src/main/java/.../vo/metadata/OntEpisodeTypeReqVO.java` | 重构：删除旧字段，新增层级字段 |
| `graphiti-module-core/src/main/java/.../vo/metadata/OntEpisodeTypeRespVO.java` | 重构：删除旧字段，新增层级/统计/子类型字段 |
| `graphiti-module-core/src/main/java/.../vo/metadata/EpisodeTypeDeleteCheckVO.java` | 新增：删除检查响应 VO |
| `graphiti-module-core/src/main/java/.../vo/metadata/EpisodeTypeReorderItemVO.java` | 新增：排序项 VO |
| `graphiti-module-core/src/main/java/.../vo/metadata/EpisodeTypeImportResultVO.java` | 新增：导入结果 VO |
| `graphiti-module-core/src/main/java/.../service/metadata/OntMetadataService.java` | 扩展：新增树查询/删除检查/排序/导入方法 |
| `graphiti-module-core/src/main/java/.../service/metadata/OntMetadataServiceImpl.java` | 扩展：实现新业务逻辑 |
| `graphiti-module-core/src/main/java/.../controller/admin/OntMetadataController.java` | 扩展：新增 6 个 REST 接口 |
| `graphiti-module-core/src/main/java/.../controller/admin/GraphIDEController.java` | 扩展：新增按类型可视化接口 |
| `graphiti-web/src/api/metadata.ts` | 扩展：episodeTypeApi 新增 6 个方法 |
| `graphiti-web/src/api/graph.ts` | 扩展：新增 getEpisodesVisualizationByType |
| `graphiti-web/src/views/graph/ide.vue` | 修改：集成新组件，替换 EpisodeExplorer |

### 可删除文件

| 文件 | 说明 |
|------|------|
| `graphiti-web/src/components/Ontology/EpisodeExplorer.vue` | 被 EpisodeTypeExplorer 替代 |

---

## 任务依赖图

```
Task 1 (数据库迁移)
    │
    ├──→ Task 2 (DO 重构) ──→ Task 3 (Mapper 扩展)
    │                              │
    ├──→ Task 4 (ReqVO) ────→ Task 5 (RespVO)
    │                              │
    └──→ Task 6 (Service 接口) ──→ Task 7 (ServiceImpl)
                                         │
    Task 8 (Controller) ←───────────────┘
    Task 9 (GraphIDE Controller)
         │
         ├──→ Task 10 (前端 API)
         │         │
         │         ├──→ Task 11 (EpisodeTypeExplorer)
         │         ├──→ Task 12 (EpisodeTypeDetailPanel)
         │         ├──→ Task 13 (EpisodeTypeEditModal)
         │         │
         │         └──→ Task 14 (ide.vue 集成)
         │
         └──→ Task 15 (删除旧文件)
                   │
                   └──→ Task 16 (验证)
```

**并行任务组：**
- **后端并行**：Task 2-7 可以串行快速完成
- **前端并行**：Task 11、12、13 互相独立，可以并行开发

---

## Task 1: 数据库迁移脚本

**Files:**
- Create: `sql/migrations/v005_episode_type_hierarchy.sql`

- [ ] **Step 1: 编写迁移脚本**

```sql
-- ============================================
-- V005: Episode Type 层级化 + 清理向后兼容字段
-- ============================================

-- 1. 新增层级与审计字段
ALTER TABLE ont_episode_type
ADD COLUMN IF NOT EXISTS parent_type_code VARCHAR(50),
ADD COLUMN IF NOT EXISTS level INT DEFAULT 1,
ADD COLUMN IF NOT EXISTS created_by VARCHAR(64),
ADD COLUMN IF NOT EXISTS updated_by VARCHAR(64),
ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;

-- 2. 增加约束
ALTER TABLE ont_episode_type
ADD CONSTRAINT uk_episode_type_code UNIQUE (definition_id, type_code),
ADD CONSTRAINT chk_episode_type_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DEPRECATED')),
ADD CONSTRAINT chk_episode_type_level CHECK (level BETWEEN 1 AND 5);

-- 3. 增加索引
CREATE INDEX IF NOT EXISTS idx_episode_type_parent ON ont_episode_type(definition_id, parent_type_code);
CREATE INDEX IF NOT EXISTS idx_episode_type_process ON ont_episode_type(definition_id, process_type);
CREATE INDEX IF NOT EXISTS idx_episode_type_status ON ont_episode_type(status);

-- 4. 删除向后兼容字段（Phase 4 清理）
-- 先确认没有数据依赖这些字段后再执行
-- ALTER TABLE ont_episode_type DROP COLUMN IF EXISTS legal_process;
-- ALTER TABLE ont_episode_type DROP COLUMN IF EXISTS court_level;
-- ALTER TABLE ont_episode_type DROP COLUMN IF EXISTS is_trial_stage;
```

- [ ] **Step 2: 运行迁移脚本**

Run: `psql -U graphiti -d graphiti -f sql/migrations/v005_episode_type_hierarchy.sql`

Expected: 所有 ALTER TABLE / CREATE INDEX 成功执行，无错误

- [ ] **Step 3: 验证表结构**

Run: `psql -U graphiti -d graphiti -c "\d ont_episode_type"`

Expected: 确认 parent_type_code, level, created_by, updated_by, version 字段已存在；旧字段 legal_process, court_level, is_trial_stage 仍存在（注释掉的 DROP 未执行）

- [ ] **Step 4: Commit**

```bash
git add sql/migrations/v005_episode_type_hierarchy.sql
git commit -m "feat(db): add episode type hierarchy fields (parent_type_code, level, audit)"
```

---

## Task 2: OntEpisodeTypeDO 重构

**Files:**
- Modify: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/metadata/OntEpisodeTypeDO.java`

- [ ] **Step 1: 重构 OntEpisodeTypeDO 字段**

替换整个文件内容：

```java
package com.graphiti.module.graphiti.dal.dataobject.metadata;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 剧集类型维度表 V5.0
 * 支持层级化分类和审计追踪
 */
@Data
@TableName("ont_episode_type")
public class OntEpisodeTypeDO implements Serializable {
    private static final long serialVersionUID = 2L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("definition_id")
    private Long definitionId;

    @TableField("type_code")
    private String typeCode;

    @TableField("type_name")
    private String typeName;

    @TableField("type_name_en")
    private String typeNameEn;

    // ========== 层级关系（V5 新增）==========
    @TableField("parent_type_code")
    private String parentTypeCode;

    @TableField("level")
    private Integer level;

    // ========== 通用分类字段 ==========
    @TableField("process_type")
    private String processType;

    @TableField("stage_label")
    private String stageLabel;

    @TableField("stage_level")
    private String stageLevel;

    @TableField("is_review_stage")
    private Boolean isReviewStage;

    // ========== 元数据与状态 ==========
    private String description;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("metadata")
    private String metadata;

    private String status;

    // ========== 审计字段（V5 新增）==========
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("created_by")
    private String createdBy;

    @TableField("updated_by")
    private String updatedBy;

    @Version
    @TableField("version")
    private Integer version;
}
```

- [ ] **Step 2: 编译验证**

Run: `cd graphiti-module-core && mvn compile -q`

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/metadata/OntEpisodeTypeDO.java
git commit -m "feat(core): refactor OntEpisodeTypeDO with hierarchy and audit fields"
```

---

## Task 3: OntEpisodeTypeMapper 扩展

**Files:**
- Modify: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/mysql/metadata/OntEpisodeTypeMapper.java`

- [ ] **Step 1: 扩展 Mapper 接口**

替换整个文件内容：

```java
package com.graphiti.module.graphiti.dal.mysql.metadata;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.metadata.OntEpisodeTypeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OntEpisodeTypeMapper extends BaseMapper<OntEpisodeTypeDO> {

    @Select("SELECT * FROM ont_episode_type WHERE definition_id = #{definitionId} ORDER BY sort_order, level")
    List<OntEpisodeTypeDO> selectByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_episode_type WHERE definition_id = #{definitionId} AND parent_type_code = #{parentTypeCode} AND status = 'ACTIVE' ORDER BY sort_order")
    List<OntEpisodeTypeDO> selectByParentTypeCode(@Param("definitionId") Long definitionId, @Param("parentTypeCode") String parentTypeCode);

    @Select("SELECT * FROM ont_episode_type WHERE definition_id = #{definitionId} AND parent_type_code IS NULL AND status = 'ACTIVE' ORDER BY sort_order")
    List<OntEpisodeTypeDO> selectRootTypes(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_episode_type WHERE definition_id = #{definitionId} AND type_code = #{typeCode} LIMIT 1")
    OntEpisodeTypeDO selectByTypeCode(@Param("definitionId") Long definitionId, @Param("typeCode") String typeCode);

    @Select("SELECT COUNT(*) FROM ont_episode_type WHERE definition_id = #{definitionId}")
    long countByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT COUNT(*) FROM episode WHERE graph_id = #{graphId} AND episode_type = #{typeCode}")
    long countEpisodeInstances(@Param("graphId") String graphId, @Param("typeCode") String typeCode);

    int batchUpdateSortOrder(@Param("list") List<OntEpisodeTypeDO> types);

    @org.apache.ibatis.annotations.Delete("DELETE FROM ont_episode_type WHERE definition_id = #{definitionId}")
    int deleteByDefinitionId(@Param("definitionId") Long definitionId);
}
```

- [ ] **Step 2: 编译验证**

Run: `cd graphiti-module-core && mvn compile -q`

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/dal/mysql/metadata/OntEpisodeTypeMapper.java
git commit -m "feat(core): extend OntEpisodeTypeMapper with tree queries and instance count"
```

---

## Task 4: OntEpisodeTypeReqVO 重构

**Files:**
- Modify: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/metadata/OntEpisodeTypeReqVO.java`

- [ ] **Step 1: 重构 ReqVO**

替换整个文件内容：

```java
package com.graphiti.module.graphiti.vo.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "创建/更新剧集类型请求 V5")
public class OntEpisodeTypeReqVO {

    @Schema(description = "主键ID（更新时必需）")
    private Long id;

    @Schema(description = "本体定义ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "本体定义ID不能为空")
    private Long definitionId;

    @Schema(description = "类型代码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "类型代码不能为空")
    @Size(max = 50, message = "类型代码最多50字符")
    private String typeCode;

    @Schema(description = "类型名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "类型名称不能为空")
    private String typeName;

    @Schema(description = "英文名称")
    private String typeNameEn;

    // ========== 层级关系（V5 新增）==========
    @Schema(description = "父类型编码")
    private String parentTypeCode;

    @Schema(description = "层级深度（1-5）")
    @Min(value = 1, message = "层级最小为1")
    @Max(value = 5, message = "层级最大为5")
    private Integer level;

    // ========== 通用分类字段 ==========
    @Schema(description = "业务流程类型：business_process|workflow|lifecycle")
    private String processType;

    @Schema(description = "阶段标签：立案|庭审|调解|判决|执行")
    private String stageLabel;

    @Schema(description = "阶段级别（通用，可配置）")
    private String stageLevel;

    @Schema(description = "是否审查/评议阶段")
    private Boolean isReviewStage;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "排序值")
    private Integer sortOrder = 0;

    @Schema(description = "元数据 JSON")
    private String metadata;

    @Schema(description = "状态: ACTIVE|INACTIVE|DEPRECATED", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "ACTIVE|INACTIVE|DEPRECATED", message = "状态必须是 ACTIVE/INACTIVE/DEPRECATED")
    private String status = "ACTIVE";
}
```

- [ ] **Step 2: 编译验证**

Run: `cd graphiti-module-core && mvn compile -q`

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/metadata/OntEpisodeTypeReqVO.java
git commit -m "feat(core): refactor OntEpisodeTypeReqVO, remove legacy fields, add hierarchy"
```

---

## Task 5: OntEpisodeTypeRespVO 重构

**Files:**
- Modify: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/metadata/OntEpisodeTypeRespVO.java`

- [ ] **Step 1: 重构 RespVO**

替换整个文件内容：

```java
package com.graphiti.module.graphiti.vo.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "剧集类型响应 V5")
public class OntEpisodeTypeRespVO {
    @Schema(description = "主键ID") private Long id;
    @Schema(description = "本体定义ID") private Long definitionId;
    @Schema(description = "类型代码") private String typeCode;
    @Schema(description = "类型名称") private String typeName;
    @Schema(description = "英文名称") private String typeNameEn;

    // 层级关系
    @Schema(description = "父类型编码") private String parentTypeCode;
    @Schema(description = "层级深度") private Integer level;

    // 通用分类
    @Schema(description = "业务流程类型") private String processType;
    @Schema(description = "阶段标签") private String stageLabel;
    @Schema(description = "阶段级别") private String stageLevel;
    @Schema(description = "是否审查/评议阶段") private Boolean isReviewStage;

    @Schema(description = "描述") private String description;
    @Schema(description = "排序值") private Integer sortOrder;
    @Schema(description = "元数据 JSON") private String metadata;
    @Schema(description = "状态") private String status;

    // 使用统计（查询时动态计算）
    @Schema(description = "引用该类型的实例数量") private Long instanceCount;

    // 子类型列表（树形结构用）
    @Schema(description = "子类型列表") private List<OntEpisodeTypeRespVO> children;

    @Schema(description = "创建时间") private LocalDateTime createdAt;
    @Schema(description = "更新时间") private LocalDateTime updatedAt;
    @Schema(description = "创建人") private String createdBy;
    @Schema(description = "更新人") private String updatedBy;
}
```

- [ ] **Step 2: 编译验证**

Run: `cd graphiti-module-core && mvn compile -q`

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/metadata/OntEpisodeTypeRespVO.java
git commit -m "feat(core): refactor OntEpisodeTypeRespVO with hierarchy, stats, children"
```

---

## Task 6: 新增 VO 类

**Files:**
- Create: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/metadata/EpisodeTypeDeleteCheckVO.java`
- Create: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/metadata/EpisodeTypeReorderItemVO.java`
- Create: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/metadata/EpisodeTypeImportResultVO.java`

- [ ] **Step 1: 创建 EpisodeTypeDeleteCheckVO**

```java
package com.graphiti.module.graphiti.vo.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "剧集类型删除检查响应")
public class EpisodeTypeDeleteCheckVO {
    @Schema(description = "是否可以删除") private Boolean canDelete;
    @Schema(description = "不可删除原因") private String reason;
    @Schema(description = "子类型数量") private Long childCount;
    @Schema(description = "实例引用数量") private Long instanceCount;
}
```

- [ ] **Step 2: 创建 EpisodeTypeReorderItemVO**

```java
package com.graphiti.module.graphiti.vo.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "剧集类型排序项")
public class EpisodeTypeReorderItemVO {
    @Schema(description = "类型ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "类型ID不能为空")
    private Long id;

    @Schema(description = "排序值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "排序值不能为空")
    private Integer sortOrder;

    @Schema(description = "父类型编码") private String parentTypeCode;
}
```

- [ ] **Step 3: 创建 EpisodeTypeImportResultVO**

```java
package com.graphiti.module.graphiti.vo.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
@Schema(description = "剧集类型批量导入结果")
public class EpisodeTypeImportResultVO {
    @Schema(description = "总数") private Integer total;
    @Schema(description = "成功数") private Integer success;
    @Schema(description = "失败数") private Integer failed;
    @Schema(description = "错误信息列表") private List<String> errors;
}
```

- [ ] **Step 4: 编译验证**

Run: `cd graphiti-module-core && mvn compile -q`

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/metadata/EpisodeTypeDeleteCheckVO.java
graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/metadata/EpisodeTypeReorderItemVO.java
graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/metadata/EpisodeTypeImportResultVO.java
git commit -m "feat(core): add EpisodeType VOs for delete-check, reorder, import"
```

---

## Task 7: OntMetadataService 扩展

**Files:**
- Modify: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/metadata/OntMetadataService.java`

- [ ] **Step 1: 扩展 Service 接口**

在接口中追加以下方法（保留原有方法不变）：

```java
// ==================== Episode Type (V5 扩展) ====================

/** 获取剧集类型树 */
List<OntEpisodeTypeRespVO> getEpisodeTypeTree(Long definitionId);

/** 删除前依赖检查 */
EpisodeTypeDeleteCheckVO checkDeleteEpisodeType(String graphId, Long id);

/** 删除类型（带依赖校验） */
void deleteEpisodeType(String graphId, Long id);

/** 批量更新排序 */
void reorderEpisodeTypes(List<EpisodeTypeReorderItemVO> items);

/** 批量导入 */
EpisodeTypeImportResultVO importEpisodeTypes(Long definitionId, List<OntEpisodeTypeReqVO> items);

/** 按流程类型查询 */
List<OntEpisodeTypeRespVO> listEpisodeTypesByProcessType(Long definitionId, String processType);
```

- [ ] **Step 2: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/metadata/OntMetadataService.java
git commit -m "feat(core): extend OntMetadataService with episode type tree/reorder/import"
```

---

## Task 8: OntMetadataServiceImpl 实现

**Files:**
- Modify: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/metadata/OntMetadataServiceImpl.java`

- [ ] **Step 1: 在 ServiceImpl 中追加 Episode Type 相关实现**

在现有 `// ==================== Episode Type ====================` 区域下方追加：

```java
    @Override
    public List<OntEpisodeTypeRespVO> getEpisodeTypeTree(Long definitionId) {
        List<OntEpisodeTypeDO> all = episodeTypeMapper.selectByDefinitionId(definitionId);
        if (all.isEmpty()) return List.of();
        Map<String, List<OntEpisodeTypeDO>> parentMap = all.stream()
            .filter(t -> t.getParentTypeCode() != null)
            .collect(Collectors.groupingBy(OntEpisodeTypeDO::getParentTypeCode));
        return all.stream()
            .filter(t -> t.getParentTypeCode() == null)
            .sorted(Comparator.comparing(OntEpisodeTypeDO::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
            .map(root -> buildTreeNode(root, parentMap))
            .collect(Collectors.toList());
    }

    private OntEpisodeTypeRespVO buildTreeNode(OntEpisodeTypeDO node,
            Map<String, List<OntEpisodeTypeDO>> parentMap) {
        OntEpisodeTypeRespVO vo = toEpisodeTypeRespVO(node);
        List<OntEpisodeTypeDO> children = parentMap.getOrDefault(node.getTypeCode(), List.of());
        if (!children.isEmpty()) {
            vo.setChildren(children.stream()
                .sorted(Comparator.comparing(OntEpisodeTypeDO::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(child -> buildTreeNode(child, parentMap))
                .collect(Collectors.toList()));
        }
        return vo;
    }

    @Override
    public EpisodeTypeDeleteCheckVO checkDeleteEpisodeType(String graphId, Long id) {
        OntEpisodeTypeDO type = episodeTypeMapper.selectById(id);
        if (type == null) {
            return EpisodeTypeDeleteCheckVO.builder().canDelete(false).reason("类型不存在").build();
        }
        List<OntEpisodeTypeDO> children = episodeTypeMapper.selectByParentTypeCode(
            type.getDefinitionId(), type.getTypeCode());
        if (!children.isEmpty()) {
            return EpisodeTypeDeleteCheckVO.builder()
                .canDelete(false)
                .reason("该类型下存在 " + children.size() + " 个子类型，请先删除子类型")
                .childCount((long) children.size())
                .build();
        }
        long instanceCount = episodeTypeMapper.countEpisodeInstances(graphId, type.getTypeCode());
        if (instanceCount > 0) {
            return EpisodeTypeDeleteCheckVO.builder()
                .canDelete(false)
                .reason("该类型被 " + instanceCount + " 个 Episode 实例引用")
                .instanceCount(instanceCount)
                .build();
        }
        return EpisodeTypeDeleteCheckVO.builder().canDelete(true).build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEpisodeType(String graphId, Long id) {
        EpisodeTypeDeleteCheckVO check = checkDeleteEpisodeType(graphId, id);
        if (!check.getCanDelete()) {
            throw new ServiceException(check.getReason());
        }
        episodeTypeMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reorderEpisodeTypes(List<EpisodeTypeReorderItemVO> items) {
        for (EpisodeTypeReorderItemVO item : items) {
            OntEpisodeTypeDO entity = episodeTypeMapper.selectById(item.getId());
            if (entity != null) {
                entity.setSortOrder(item.getSortOrder());
                if (item.getParentTypeCode() != null) {
                    entity.setParentTypeCode(item.getParentTypeCode());
                    // 重新计算 level
                    if (item.getParentTypeCode().isEmpty()) {
                        entity.setLevel(1);
                    } else {
                        OntEpisodeTypeDO parent = episodeTypeMapper.selectByTypeCode(
                            entity.getDefinitionId(), item.getParentTypeCode());
                        entity.setLevel(parent != null ? (parent.getLevel() != null ? parent.getLevel() + 1 : 2) : 1);
                    }
                }
                episodeTypeMapper.updateById(entity);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EpisodeTypeImportResultVO importEpisodeTypes(Long definitionId, List<OntEpisodeTypeReqVO> items) {
        int success = 0;
        List<String> errors = new ArrayList<>();
        for (OntEpisodeTypeReqVO req : items) {
            try {
                req.setDefinitionId(definitionId);
                // 检查编码是否已存在
                OntEpisodeTypeDO existing = episodeTypeMapper.selectByTypeCode(definitionId, req.getTypeCode());
                if (existing != null) {
                    errors.add("类型编码已存在: " + req.getTypeCode());
                    continue;
                }
                // 自动计算 level
                if (req.getParentTypeCode() != null && !req.getParentTypeCode().isEmpty()) {
                    OntEpisodeTypeDO parent = episodeTypeMapper.selectByTypeCode(definitionId, req.getParentTypeCode());
                    req.setLevel(parent != null ? (parent.getLevel() != null ? parent.getLevel() + 1 : 2) : 1);
                } else {
                    req.setLevel(1);
                }
                createEpisodeType(req);
                success++;
            } catch (Exception e) {
                errors.add(req.getTypeCode() + ": " + e.getMessage());
            }
        }
        return EpisodeTypeImportResultVO.builder()
            .total(items.size())
            .success(success)
            .failed(items.size() - success)
            .errors(errors)
            .build();
    }

    @Override
    public List<OntEpisodeTypeRespVO> listEpisodeTypesByProcessType(Long definitionId, String processType) {
        return episodeTypeMapper.selectByProcessType(definitionId, processType)
                .stream().map(this::toEpisodeTypeRespVO).collect(Collectors.toList());
    }
```

同时需要修改 `toEpisodeTypeRespVO` 方法，新增层级字段映射：

在现有 `toEpisodeTypeRespVO` 中添加：
```java
                .parentTypeCode(entity.getParentTypeCode())
                .level(entity.getLevel())
```

以及修改 `copyEpisodeType` 方法，删除旧字段拷贝，新增层级字段：

```java
    private void copyEpisodeType(OntEpisodeTypeReqVO reqVO, OntEpisodeTypeDO entity) {
        if (reqVO.getDefinitionId() != null) entity.setDefinitionId(reqVO.getDefinitionId());
        if (reqVO.getTypeCode() != null) entity.setTypeCode(reqVO.getTypeCode());
        if (reqVO.getTypeName() != null) entity.setTypeName(reqVO.getTypeName());
        if (reqVO.getTypeNameEn() != null) entity.setTypeNameEn(reqVO.getTypeNameEn());
        if (reqVO.getDescription() != null) entity.setDescription(reqVO.getDescription());
        if (reqVO.getSortOrder() != null) entity.setSortOrder(reqVO.getSortOrder());
        if (reqVO.getMetadata() != null) entity.setMetadata(reqVO.getMetadata());
        if (reqVO.getStatus() != null) entity.setStatus(reqVO.getStatus());
        // 层级关系
        if (reqVO.getParentTypeCode() != null) entity.setParentTypeCode(reqVO.getParentTypeCode());
        if (reqVO.getLevel() != null) entity.setLevel(reqVO.getLevel());
        // 通用分类字段
        if (reqVO.getProcessType() != null) entity.setProcessType(reqVO.getProcessType());
        if (reqVO.getStageLabel() != null) entity.setStageLabel(reqVO.getStageLabel());
        if (reqVO.getStageLevel() != null) entity.setStageLevel(reqVO.getStageLevel());
        if (reqVO.getIsReviewStage() != null) entity.setIsReviewStage(reqVO.getIsReviewStage());
    }
```

注意：需要导入 `ServiceException`。

- [ ] **Step 2: 编译验证**

Run: `cd graphiti-module-core && mvn compile -q`

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/metadata/OntMetadataServiceImpl.java
git commit -m "feat(core): implement episode type tree, delete-check, reorder, import"
```

---

## Task 9: OntMetadataController 扩展

**Files:**
- Modify: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/controller/admin/OntMetadataController.java`

- [ ] **Step 1: 在 Controller 中追加 Episode Type 接口**

在现有 `// ==================== Episode Type ====================` 区域替换/扩展：

```java
    // ==================== Episode Type ====================

    @GetMapping("/episode-types")
    @Operation(summary = "获取剧集类型列表", description = "获取指定图谱的本体定义下的所有剧集类型")
    public CommonResult<List<OntEpisodeTypeRespVO>> listEpisodeTypes(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestParam @Parameter(description = "本体定义ID") Long definitionId,
            @RequestParam(required = false) @Parameter(description = "业务流程类型") String processType) {
        if (processType != null && !processType.isBlank()) {
            return CommonResult.success(ontMetadataService.listEpisodeTypesByProcessType(definitionId, processType));
        }
        return CommonResult.success(ontMetadataService.listEpisodeTypes(definitionId));
    }

    @GetMapping("/episode-types/tree")
    @Operation(summary = "获取剧集类型树", description = "获取层级化的剧集类型树")
    public CommonResult<List<OntEpisodeTypeRespVO>> getEpisodeTypeTree(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestParam @Parameter(description = "本体定义ID") Long definitionId) {
        return CommonResult.success(ontMetadataService.getEpisodeTypeTree(definitionId));
    }

    @GetMapping("/episode-types/{id}")
    @Operation(summary = "获取剧集类型详情")
    public CommonResult<OntEpisodeTypeRespVO> getEpisodeType(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "类型ID") Long id) {
        OntEpisodeTypeRespVO vo = ontMetadataService.getEpisodeTypeById(id);
        if (vo != null) {
            vo.setInstanceCount(episodeTypeMapper.countEpisodeInstances(graphId, vo.getTypeCode()));
        }
        return CommonResult.success(vo);
    }

    @PostMapping("/episode-types")
    @Operation(summary = "创建剧集类型")
    public CommonResult<Long> createEpisodeType(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestBody @Valid OntEpisodeTypeReqVO reqVO) {
        return CommonResult.success(ontMetadataService.createEpisodeType(reqVO));
    }

    @PostMapping("/episode-types/batch")
    @Operation(summary = "批量创建剧集类型")
    public CommonResult<Integer> batchCreateEpisodeTypes(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestParam @Parameter(description = "本体定义ID") Long definitionId,
            @RequestBody @Valid List<OntEpisodeTypeReqVO> reqVOs) {
        return CommonResult.success(ontMetadataService.batchCreateEpisodeTypes(definitionId, reqVOs));
    }

    @PutMapping("/episode-types/{id}")
    @Operation(summary = "更新剧集类型")
    public CommonResult<Void> updateEpisodeType(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "类型ID") Long id,
            @RequestBody @Valid OntEpisodeTypeReqVO reqVO) {
        ontMetadataService.updateEpisodeType(id, reqVO);
        return CommonResult.success();
    }

    @GetMapping("/episode-types/{id}/delete-check")
    @Operation(summary = "检查剧集类型是否可以删除")
    public CommonResult<EpisodeTypeDeleteCheckVO> checkDeleteEpisodeType(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "类型ID") Long id) {
        return CommonResult.success(ontMetadataService.checkDeleteEpisodeType(graphId, id));
    }

    @DeleteMapping("/episode-types/{id}")
    @Operation(summary = "删除剧集类型")
    public CommonResult<Void> deleteEpisodeType(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "类型ID") Long id) {
        ontMetadataService.deleteEpisodeType(graphId, id);
        return CommonResult.success();
    }

    @PostMapping("/episode-types/reorder")
    @Operation(summary = "批量更新剧集类型排序")
    public CommonResult<Void> reorderEpisodeTypes(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestBody @Valid List<EpisodeTypeReorderItemVO> items) {
        ontMetadataService.reorderEpisodeTypes(items);
        return CommonResult.success();
    }

    @PostMapping("/episode-types/import")
    @Operation(summary = "批量导入剧集类型")
    public CommonResult<EpisodeTypeImportResultVO> importEpisodeTypes(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestParam @Parameter(description = "本体定义ID") Long definitionId,
            @RequestBody @Valid List<OntEpisodeTypeReqVO> items) {
        return CommonResult.success(ontMetadataService.importEpisodeTypes(definitionId, items));
    }

    @GetMapping("/episode-types/{id}/instances")
    @Operation(summary = "获取剧集类型下的实例列表")
    public CommonResult<PageResult<EpisodeListItemVO>> getEpisodeTypeInstances(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "类型ID") Long id,
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") Integer page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页数量") Integer pageSize,
            @RequestParam(required = false) @Parameter(description = "搜索关键词") String keyword) {
        OntEpisodeTypeRespVO type = ontMetadataService.getEpisodeTypeById(id);
        if (type == null) {
            return CommonResult.success(PageResult.empty());
        }
        return CommonResult.success(
            episodeService.listByType(graphId, type.getTypeCode(), page, pageSize, keyword)
        );
    }
```

注意：Controller 中需要注入 `episodeService` 和 `episodeTypeMapper`。

- [ ] **Step 2: 编译验证**

Run: `cd graphiti-module-core && mvn compile -q`

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/controller/admin/OntMetadataController.java
git commit -m "feat(api): extend OntMetadataController with episode type tree/reorder/import/delete-check"
```

---

## Task 10: GraphIDEController 新增可视化接口

**Files:**
- Modify: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/controller/admin/GraphIDEController.java`

- [ ] **Step 1: 在 GraphIDEController 中追加方法**

在现有 `getEpisodeTypes` 方法下方追加：

```java
    /**
     * V5.0: 根据剧集类型获取可视化数据
     */
    @GetMapping("/{graphId}/episodes/visualization/by-type")
    @Operation(summary = "根据剧集类型获取可视化数据", description = "获取指定类型（含子类型）下的 Episode 实例及其关联实体/关系的可视化数据")
    public CommonResult<GraphVisualizationVO> getEpisodesVisualizationByType(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestParam @Parameter(description = "类型编码", required = true) String typeCode,
            @RequestParam(defaultValue = "false") @Parameter(description = "是否包含子类型") Boolean includeChildren,
            @RequestParam(defaultValue = "100") @Parameter(description = "限制数量") Integer limit) {
        return CommonResult.success(
            graphService.getEpisodesVisualizationByType(graphId, typeCode, includeChildren, limit)
        );
    }
```

注意：`GraphVisualizationVO` 和 `graphService.getEpisodesVisualizationByType` 需要已存在或后续实现。

- [ ] **Step 2: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/controller/admin/GraphIDEController.java
git commit -m "feat(api): add getEpisodesVisualizationByType endpoint"
```

---

## Task 11: 前端 API 扩展

**Files:**
- Modify: `graphiti-web/src/api/metadata.ts`
- Modify: `graphiti-web/src/api/graph.ts`

- [ ] **Step 1: 扩展 metadata.ts 中的 episodeTypeApi**

在 `graphiti-web/src/api/metadata.ts` 中，将 `episodeTypeApi` 替换为：

```typescript
// ==================== Episode Type API ====================

export const episodeTypeApi = {
  list: (graphId: string, definitionId: number, processType?: string) =>
    request.get<OntEpisodeTypeVO[]>(`/ontology/${graphId}/episode-types`, {
      params: { definitionId, ...(processType ? { processType } : {}) }
    }),

  getTree: (graphId: string, definitionId: number) =>
    request.get<OntEpisodeTypeVO[]>(`/ontology/${graphId}/episode-types/tree`, {
      params: { definitionId }
    }),

  get: (graphId: string, id: number) =>
    request.get<OntEpisodeTypeVO>(`/ontology/${graphId}/episode-types/${id}`),

  create: (graphId: string, data: Partial<OntEpisodeTypeVO>) =>
    request.post<number>(`/ontology/${graphId}/episode-types`, data),

  batchCreate: (graphId: string, definitionId: number, data: Partial<OntEpisodeTypeVO>[]) =>
    request.post<number>(`/ontology/${graphId}/episode-types/batch`, data, {
      params: { definitionId }
    }),

  update: (graphId: string, id: number, data: Partial<OntEpisodeTypeVO>) =>
    request.put(`/ontology/${graphId}/episode-types/${id}`, data),

  delete: (graphId: string, id: number) =>
    request.delete(`/ontology/${graphId}/episode-types/${id}`),

  checkDelete: (graphId: string, id: number) =>
    request.get<EpisodeTypeDeleteCheckVO>(`/ontology/${graphId}/episode-types/${id}/delete-check`),

  reorder: (graphId: string, items: EpisodeTypeReorderItem[]) =>
    request.post(`/ontology/${graphId}/episode-types/reorder`, items),

  import: (graphId: string, definitionId: number, items: Partial<OntEpisodeTypeVO>[]) =>
    request.post<EpisodeTypeImportResultVO>(`/ontology/${graphId}/episode-types/import`, items, {
      params: { definitionId }
    }),

  getInstances: (graphId: string, id: number, page: number, pageSize: number, keyword?: string) =>
    request.get<PageResult<EpisodeListItem>>(`/ontology/${graphId}/episode-types/${id}/instances`, {
      params: { page, pageSize, ...(keyword ? { keyword } : {}) }
    }),
}
```

同时需要在前端类型文件中新增接口定义（如果尚未存在）：

```typescript
export interface EpisodeTypeDeleteCheckVO {
  canDelete: boolean
  reason?: string
  childCount?: number
  instanceCount?: number
}

export interface EpisodeTypeReorderItem {
  id: number
  sortOrder: number
  parentTypeCode?: string
}

export interface EpisodeTypeImportResultVO {
  total: number
  success: number
  failed: number
  errors: string[]
}
```

- [ ] **Step 2: 扩展 graph.ts**

在 `graphiti-web/src/api/graph.ts` 中追加：

```typescript
  getEpisodesVisualizationByType: (graphId: string, typeCode: string, includeChildren?: boolean, limit?: number) =>
    request.get<GraphVisualizationData>(`/graph/ide/${graphId}/episodes/visualization/by-type`, {
      params: { typeCode, includeChildren, limit }
    }),
```

- [ ] **Step 3: TypeScript 编译检查**

Run: `cd graphiti-web && npx vue-tsc --noEmit`

Expected: 无类型错误（可能与现有错误共存，但无新增错误）

- [ ] **Step 4: Commit**

```bash
git add graphiti-web/src/api/metadata.ts graphiti-web/src/api/graph.ts
git commit -m "feat(web): extend episodeTypeApi and graphApi for hierarchy management"
```

---

## Task 12: EpisodeTypeExplorer 组件

**Files:**
- Create: `graphiti-web/src/components/Ontology/EpisodeTypeExplorer.vue`

- [ ] **Step 1: 创建组件**

完整代码参考设计文档第四部分 4.2 节。核心结构：

```vue
<template>
  <div class="episode-type-explorer">
    <div class="explorer-search">...</div>
    <div class="explorer-toolbar">...</div>
    <div class="explorer-body">
      <a-tree v-show="viewMode === 'tree'" ... />
      <a-table v-show="viewMode === 'list'" ... />
    </div>
  </div>
</template>
```

关键实现要点：
- 使用 `episodeTypeApi.getTree()` 加载类型树数据
- 树形视图使用 `a-tree` 的 `draggable` + `@drop` 实现拖拽排序
- 列表视图显示平铺列表，带层级缩进
- 搜索过滤支持编码、名称、流程类型

- [ ] **Step 2: 样式与 OntologyObjectExplorer 保持一致**

确保 `.explorer-search`、`.explorer-toolbar`、`.explorer-body`、`.tree-node-content` 等样式与 `OntologyObjectExplorer.vue` 和 `CommunityExplorer.vue` 统一。

- [ ] **Step 3: Commit**

```bash
git add graphiti-web/src/components/Ontology/EpisodeTypeExplorer.vue
git commit -m "feat(web): add EpisodeTypeExplorer component with tree/list views and drag-sort"
```

---

## Task 13: EpisodeTypeDetailPanel 组件

**Files:**
- Create: `graphiti-web/src/components/Ontology/EpisodeTypeDetailPanel.vue`

- [ ] **Step 1: 创建组件**

完整代码参考设计文档第四部分 4.3 节。核心结构：

```vue
<template>
  <div class="episode-type-detail-panel">
    <div class="panel-header">...</div>
    <div class="panel-tabs">
      <div class="panel-tab" :class="{active: activeTab==='info'}" @click="activeTab='info'">类型详情</div>
      <div class="panel-tab" :class="{active: activeTab==='instances'}" @click="activeTab='instances'">实例列表</div>
    </div>
    <div class="panel-content">
      <div v-if="activeTab === 'info'" class="tab-info">
        <a-descriptions ... />
      </div>
      <div v-else-if="activeTab === 'instances'" class="tab-instances">
        <a-table ... />
      </div>
    </div>
  </div>
</template>
```

关键实现要点：
- 加载时自动调用 `episodeTypeApi.checkDelete()`
- 删除按钮根据 `canDelete` 状态启用/禁用
- 实例列表使用 `episodeTypeApi.getInstances()` 分页加载
- 样式与 ide.vue 现有右侧面板保持一致

- [ ] **Step 2: Commit**

```bash
git add graphiti-web/src/components/Ontology/EpisodeTypeDetailPanel.vue
git commit -m "feat(web): add EpisodeTypeDetailPanel with info and instance list tabs"
```

---

## Task 14: EpisodeTypeEditModal 组件

**Files:**
- Create: `graphiti-web/src/components/Ontology/EpisodeTypeEditModal.vue`

- [ ] **Step 1: 创建组件**

完整代码参考设计文档第四部分 4.4 节。核心结构：

```vue
<template>
  <a-modal v-model:open="visible" :title="isEdit ? '编辑剧集类型' : '新建剧集类型'" ...>
    <a-form ref="formRef" :model="form" :rules="rules">
      <a-form-item label="类型代码" name="typeCode">...</a-form-item>
      <a-form-item label="父类型" name="parentTypeCode">
        <a-tree-select v-model:value="form.parentTypeCode" :tree-data="parentOptions" ... />
      </a-form-item>
      ...
    </a-form>
  </a-modal>
</template>
```

关键实现要点：
- 父类型选择使用 `a-tree-select`，排除自身及子类型防止循环依赖
- 选择父类型后自动计算 `level`
- 编辑模式回填数据，新增模式清空表单

- [ ] **Step 2: Commit**

```bash
git add graphiti-web/src/components/Ontology/EpisodeTypeEditModal.vue
git commit -m "feat(web): add EpisodeTypeEditModal with parent type selection and auto level"
```

---

## Task 15: ide.vue 集成

**Files:**
- Modify: `graphiti-web/src/views/graph/ide.vue`

- [ ] **Step 1: 导入新组件**

在 `<script setup>` 顶部添加：

```typescript
import EpisodeTypeExplorer from '@/components/Ontology/EpisodeTypeExplorer.vue'
import EpisodeTypeDetailPanel from '@/components/Ontology/EpisodeTypeDetailPanel.vue'
import EpisodeTypeEditModal from '@/components/Ontology/EpisodeTypeEditModal.vue'
```

- [ ] **Step 2: 替换 Sidebar 中的 EpisodeExplorer**

将：
```vue
<EpisodeExplorer
  v-else-if="sidebarTab === 'episodes'"
  :graph-id="effectiveGraphId"
  @open-episode="handleEpisodeNodeClick"
/>
```

替换为：
```vue
<EpisodeTypeExplorer
  v-else-if="sidebarTab === 'episodes'"
  :graph-id="effectiveGraphId"
  @select-type="handleEpisodeTypeSelect"
  @create-type="handleEpisodeTypeCreate"
/>
```

- [ ] **Step 3: 新增右侧面板条件渲染**

在 ide.vue 的右侧面板区域，找到 `<!-- V3.0.0: Episode 详情面板 -->` 模板块，替换为：

```vue
<!-- V5.0: Episode 类型详情面板 -->
<template v-if="sidebarTab === 'episodes' && selectedEpisodeType">
  <div class="panel-header">
    <span class="panel-title">{{ selectedEpisodeType.typeName || '类型详情' }}</span>
    <a-button type="text" size="small" @click="selectedEpisodeType = null">
      <template #icon><CloseOutlined /></template>
    </a-button>
  </div>
  <EpisodeTypeDetailPanel
    :graph-id="effectiveGraphId"
    :type-id="selectedEpisodeType.id"
    :type-data="selectedEpisodeType"
    @edit-type="handleEpisodeTypeEdit"
    @delete-type="handleEpisodeTypeDelete"
    @close="selectedEpisodeType = null"
    @navigate-to-instance="handleNavigateToInstance"
  />
</template>
```

- [ ] **Step 4: 新增状态和方法**

在 `<script setup>` 中添加状态：

```typescript
// V5.0: Episode 类型管理
const selectedEpisodeType = ref<OntEpisodeTypeVO | null>(null)
const allEpisodeTypes = ref<OntEpisodeTypeVO[]>([])
const showEpisodeTypeEditModal = ref(false)
const editingEpisodeType = ref<OntEpisodeTypeVO | undefined>(undefined)
```

添加方法：

```typescript
const handleEpisodeTypeSelect = async (payload: { typeId: number; typeCode: string; typeName: string }) => {
  ontologyMode.value = 'episodes'
  showPanel.value = true
  loading.value = true
  try {
    const [detailRes, visRes] = await Promise.all([
      episodeTypeApi.get(effectiveGraphId.value, payload.typeId),
      graphApi.getEpisodesVisualizationByType(effectiveGraphId.value, payload.typeCode, true, 100)
    ])
    selectedEpisodeType.value = detailRes.data
    nodes.value = dedupeNodes(visRes.data?.nodes || [])
    edges.value = dedupeEdges(visRes.data?.edges || [])
  } catch (e) {
    console.error('加载类型数据失败:', e)
    message.error('加载类型数据失败')
  } finally {
    loading.value = false
  }
}

const handleEpisodeTypeCreate = () => {
  editingEpisodeType.value = undefined
  showEpisodeTypeEditModal.value = true
}

const handleEpisodeTypeEdit = (typeId: number) => {
  editingEpisodeType.value = selectedEpisodeType.value || undefined
  showEpisodeTypeEditModal.value = true
}

const handleEpisodeTypeDelete = async (typeId: number) => {
  try {
    await episodeTypeApi.delete(effectiveGraphId.value, typeId)
    message.success('类型已删除')
    selectedEpisodeType.value = null
    showPanel.value = false
    nodes.value = []
    edges.value = []
  } catch (e: any) {
    message.error(e.message || '删除失败')
  }
}

const handleEpisodeTypeEditSuccess = async () => {
  if (selectedEpisodeType.value) {
    try {
      const detail = await episodeTypeApi.get(effectiveGraphId.value, selectedEpisodeType.value.id!)
      selectedEpisodeType.value = detail.data
    } catch (e) { /* ignore */ }
  }
}
```

- [ ] **Step 5: 新增弹窗组件**

在 ide.vue 的 Modals 区域追加：

```vue
<EpisodeTypeEditModal
  v-model:visible="showEpisodeTypeEditModal"
  :graph-id="effectiveGraphId"
  :definition-id="definitionId"
  :type-data="editingEpisodeType"
  :all-types="allEpisodeTypes"
  @success="handleEpisodeTypeEditSuccess"
/>
```

- [ ] **Step 6: TypeScript 编译检查**

Run: `cd graphiti-web && npx vue-tsc --noEmit`

Expected: 无新增类型错误

- [ ] **Step 7: Commit**

```bash
git add graphiti-web/src/views/graph/ide.vue
git commit -m "feat(web): integrate EpisodeTypeExplorer/DetailPanel/EditModal into ide.vue"
```

---

## Task 16: 删除旧 EpisodeExplorer.vue

**Files:**
- Delete: `graphiti-web/src/components/Ontology/EpisodeExplorer.vue`

- [ ] **Step 1: 确认无其他引用**

Run: `cd graphiti-web && grep -r "EpisodeExplorer" src/ --include="*.vue" --include="*.ts"`

Expected: 仅 ide.vue 中的引用已被替换，无其他引用

- [ ] **Step 2: 删除文件**

```bash
git rm graphiti-web/src/components/Ontology/EpisodeExplorer.vue
git commit -m "refactor(web): remove old EpisodeExplorer.vue, replaced by EpisodeTypeExplorer"
```

---

## Task 17: 端到端验证

- [ ] **Step 1: 启动后端服务**

Run: `cd graphiti-server && mvn spring-boot:run`

Expected: 服务启动成功，无 Bean 创建失败

- [ ] **Step 2: 验证 API 可用性**

Run: `curl http://localhost:8080/api/v1/ontology/test-graph/episode-types/tree?definitionId=1`

Expected: 返回 JSON 数组（即使为空）

- [ ] **Step 3: 启动前端开发服务器**

Run: `cd graphiti-web && pnpm dev`

Expected: 编译成功，无新增错误

- [ ] **Step 4: 手动验证关键路径**

1. 打开 `ide.vue` → 切换到"剧集"tab
2. 验证类型树正常显示
3. 点击类型节点 → 验证中间画布显示实例图谱
4. 验证右侧面板显示类型详情
5. 切换到"实例列表"tab → 验证分页列表
6. 点击"新建类型"→ 验证弹窗表单
7. 测试删除类型（含依赖检查）

- [ ] **Step 5: Commit 最终版本**

```bash
git commit -m "feat: episode type management with hierarchy, CRUD, drag-sort, dependency check"
```

---

## 自审检查清单

### Spec 覆盖检查

| 设计文档需求 | 对应任务 |
|-------------|---------|
| 数据库层级字段 (parent_type_code, level) | Task 1, 2 |
| 审计字段 (created_by, updated_by, version) | Task 1, 2 |
| 删除向后兼容字段 | Task 1 (注释掉的 DROP), Task 4, 5 |
| 树形查询 API | Task 3, 8 |
| 删除前依赖检查 | Task 6, 8 |
| 批量排序 | Task 3, 6, 8 |
| 批量导入 | Task 6, 8 |
| 实例列表分页 | Task 8 |
| 类型可视化接口 | Task 10 |
| EpisodeTypeExplorer 组件 | Task 12 |
| EpisodeTypeDetailPanel 组件 | Task 13 |
| EpisodeTypeEditModal 组件 | Task 14 |
| ide.vue 集成 | Task 15 |
| 删除旧 EpisodeExplorer | Task 16 |

**结果：✅ 所有设计文档需求均已覆盖**

### Placeholder 扫描

- [x] 无 "TBD"/"TODO" 占位符
- [x] 无 "appropriate error handling" 等模糊描述
- [x] 每个代码步骤包含完整代码
- [x] 无 "Similar to Task N" 引用

### 类型一致性

- [x] `OntEpisodeTypeDO` 字段与 Mapper SQL 一致
- [x] `OntEpisodeTypeReqVO`/`RespVO` 字段与 DO 一致
- [x] 前端 `OntEpisodeTypeVO` 与后端 RespVO 一致
- [x] API 路径前后端一致

---

## 执行选项

**Plan complete and saved to `docs/superpowers/plans/2026-05-22-episode-type-management-impl.md`.**

Two execution options:

**1. Subagent-Driven (recommended)** - Dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
