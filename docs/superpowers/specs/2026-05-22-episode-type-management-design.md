# Episode 类型管理重构设计文档

> 日期：2026-05-22
> 版本：v1.0
> 范围：前端 EpisodeExplorer 重构 + 后端 Episode Type 层级化改造

---

## 一、概述

### 1.1 背景

当前 `EpisodeExplorer.vue` 是一个"剧集实例浏览器"，在 `ide.vue` 侧边栏的"剧集"tab 中展示 Episode 实例列表/树。随着 V4.0.0 通用化改造的推进，Episode 类型（`ont_episode_type`）需要支持层级化管理，以适配多领域的业务流程分类需求。

### 1.2 目标

将 EpisodeExplorer 从"实例浏览器"重构为"类型管理+实例展示"的复合视图：

- **左侧**：剧集类型层级树/列表（支持 CRUD、拖拽排序、批量导入）
- **中间**：GraphCanvas 展示选中类型下的 Episode 实例及其关联实体/关系
- **右侧**：类型详情面板（类型信息 + 实例列表标签页）

### 1.3 非目标

- 不改造 GraphCanvas 组件本身
- 不改造 Neo4j 图谱数据结构
- 不新增 Episode 实例的后端业务逻辑（复用现有 API）

---

## 二、关键决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 数据库层级字段 | `parent_type_code` + `level` | 社区类型已采用相同模式，保持一致 |
| 向后兼容字段 | 彻底删除（Phase 4 完成） | 用户明确不考虑旧数据升级 |
| 组件架构 | 方案 2：组件拆分 | 职责清晰，可维护性高 |
| 交互模式 | 模式 A：类型导航 + 实例展示 | 保留图谱可视化的核心价值 |
| instanceCount | 动态统计，不持久化 | 避免数据不一致 |
| 父类型关联键 | `type_code` 而非 `id` | 避免 ID 变更导致层级断裂 |

---

## 三、数据库模型设计

### 3.1 DDL 迁移脚本

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
-- ALTER TABLE ont_episode_type DROP COLUMN IF EXISTS legal_process;
-- ALTER TABLE ont_episode_type DROP COLUMN IF EXISTS court_level;
-- ALTER TABLE ont_episode_type DROP COLUMN IF EXISTS is_trial_stage;
```

### 3.2 OntEpisodeTypeDO

```java
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

### 3.3 Mapper 扩展

```java
@Mapper
public interface OntEpisodeTypeMapper extends BaseMapper<OntEpisodeTypeDO> {

    @Select("SELECT * FROM ont_episode_type WHERE definition_id = #{definitionId} ORDER BY sort_order, level")
    List<OntEpisodeTypeDO> selectByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_episode_type WHERE definition_id = #{definitionId} AND parent_type_code = #{parentTypeCode} AND status = 'ACTIVE' ORDER BY sort_order")
    List<OntEpisodeTypeDO> selectByParentTypeCode(@Param("definitionId") Long definitionId, @Param("parentTypeCode") String parentTypeCode);

    @Select("SELECT * FROM ont_episode_type WHERE definition_id = #{definitionId} AND parent_type_code IS NULL AND status = 'ACTIVE' ORDER BY sort_order")
    List<OntEpisodeTypeDO> selectRootTypes(@Param("definitionId") Long definitionId);

    @Select("SELECT COUNT(*) FROM episode WHERE graph_id = #{graphId} AND episode_type = #{typeCode}")
    long countEpisodeInstances(@Param("graphId") String graphId, @Param("typeCode") String typeCode);

    int batchUpdateSortOrder(@Param("list") List<OntEpisodeTypeDO> types);
}
```

---

## 四、后端 API 设计

### 4.1 OntMetadataController — Episode Type 接口

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| GET | `/episode-types` | 平铺列表（支持 processType 过滤） | 原有 |
| GET | `/episode-types/tree` | **新增**：树形结构 | 新增 |
| GET | `/episode-types/{id}` | 详情（扩展 instanceCount） | 改造 |
| POST | `/episode-types` | 创建（增加层级校验） | 原有 |
| POST | `/episode-types/batch` | 批量创建 | 原有 |
| PUT | `/episode-types/{id}` | 更新 | 原有 |
| GET | `/episode-types/{id}/delete-check` | **新增**：删除前依赖检查 | 新增 |
| DELETE | `/episode-types/{id}` | 删除（增加依赖校验） | 改造 |
| POST | `/episode-types/reorder` | **新增**：批量更新排序 | 新增 |
| POST | `/episode-types/import` | **新增**：批量导入 | 新增 |
| GET | `/episode-types/{id}/instances` | **新增**：实例列表分页 | 新增 |

### 4.2 GraphIDEController — 可视化接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/{graphId}/episodes/visualization/by-type` | **新增**：按类型获取可视化数据 |

参数：`typeCode`（必填）、`includeChildren`（默认 false）、`limit`（默认 100）

### 4.3 新增 VO

```java
/** 删除检查响应 */
@Data
@Builder
public class EpisodeTypeDeleteCheckVO {
    private Boolean canDelete;
    private String reason;
    private Long childCount;
    private Long instanceCount;
}

/** 排序项 */
@Data
public class EpisodeTypeReorderItemVO {
    @NotNull private Long id;
    @NotNull private Integer sortOrder;
    private String parentTypeCode;
}

/** 批量导入结果 */
@Data
@Builder
public class EpisodeTypeImportResultVO {
    private Integer total;
    private Integer success;
    private Integer failed;
    private List<String> errors;
}
```

### 4.4 Service 关键逻辑

```java
// 构建类型树
public List<OntEpisodeTypeRespVO> getEpisodeTypeTree(Long definitionId) {
    List<OntEpisodeTypeDO> all = episodeTypeMapper.selectByDefinitionId(definitionId);
    Map<String, List<OntEpisodeTypeDO>> parentMap = all.stream()
        .filter(t -> t.getParentTypeCode() != null)
        .collect(Collectors.groupingBy(OntEpisodeTypeDO::getParentTypeCode));
    return all.stream()
        .filter(t -> t.getParentTypeCode() == null)
        .sorted(Comparator.comparing(OntEpisodeTypeDO::getSortOrder))
        .map(root -> buildTreeNode(root, parentMap))
        .collect(Collectors.toList());
}

// 删除前依赖检查
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
            .reason("该类型下存在 " + children.size() + " 个子类型")
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
```

---

## 五、前端组件设计

### 5.1 组件清单

| 组件 | 路径 | 类型 | 职责 |
|------|------|------|------|
| `EpisodeTypeExplorer` | `@/components/Ontology/EpisodeTypeExplorer.vue` | 新 | 侧边栏：类型树/列表、搜索、工具栏 |
| `EpisodeTypeDetailPanel` | `@/components/Ontology/EpisodeTypeDetailPanel.vue` | 新 | 右侧面板：类型详情 + 实例列表 |
| `EpisodeTypeEditModal` | `@/components/Ontology/EpisodeTypeEditModal.vue` | 新 | 新增/编辑类型弹窗 |
| `ide.vue` | `@/views/graph/ide.vue` | 改 | 集成新组件 |

### 5.2 组件关系与数据流

```
ide.vue
├── Sidebar
│   └── EpisodeTypeExplorer
│       ├── Props: graphId
│       ├── Emits: @select-type, @create-type
│       └── 内部: 搜索、刷新、导入、视图切换、拖拽排序
│
├── Canvas
│   └── GraphCanvas (不变)
│       └── 显示：选中类型及其子类型下的 Episode 实例 + 关联实体/关系
│
└── Right Panel
    └── EpisodeTypeDetailPanel
        ├── Props: graphId, typeId, typeData
        ├── Emits: @edit-type, @delete-type, @close, @navigate-to-instance
        └── 内部: 类型详情 Tab + 实例列表 Tab

独立弹窗: EpisodeTypeEditModal
    ├── Props: visible, graphId, definitionId, typeData, allTypes
    ├── Emits: @update:visible, @success
    └── 内部: 表单、父类型级联选择、层级自动计算
```

### 5.3 EpisodeTypeExplorer Props / Emits

```typescript
// Props
interface Props {
  graphId: string
}

// Emits
interface Emits {
  (e: 'select-type', payload: { typeId: number; typeCode: string; typeName: string }): void
  (e: 'create-type'): void
}

// 内部状态
const searchKeyword = ref('')
const viewMode = ref<'tree' | 'list'>('tree')
const loading = ref(false)
const selectedKeys = ref<string[]>([])
const expandedKeys = ref<string[]>([])
const typeList = ref<OntEpisodeTypeVO[]>([])
```

### 5.4 EpisodeTypeDetailPanel Props / Emits

```typescript
// Props
interface Props {
  graphId: string
  typeId: number
  typeData?: OntEpisodeTypeVO
}

// Emits
interface Emits {
  (e: 'edit-type', typeId: number): void
  (e: 'delete-type', typeId: number): void
  (e: 'close'): void
  (e: 'navigate-to-instance', uuid: string): void
}

// 内部状态
const activeTab = ref<'info' | 'instances'>('info')
const deleteCheck = ref<EpisodeTypeDeleteCheckVO | null>(null)
const instanceList = ref<EpisodeListItem[]>([])
const instancePagination = ref({ current: 1, pageSize: 10, total: 0 })
```

### 5.5 EpisodeTypeEditModal Props / Emits

```typescript
// Props
interface Props {
  visible: boolean
  graphId: string
  definitionId: number
  typeData?: OntEpisodeTypeVO       // 编辑时传入，新增时 undefined
  allTypes: OntEpisodeTypeVO[]      // 父类型选择用
}

// Emits
interface Emits {
  (e: 'update:visible', val: boolean): void
  (e: 'success'): void
}

// 关键逻辑：父类型循环依赖防护
const parentOptions = computed(() => {
  if (!props.typeData?.id) return props.allTypes
  const excludeSet = new Set<string>()
  const collect = (code: string) => {
    excludeSet.add(code)
    props.allTypes.filter(t => t.parentTypeCode === code).forEach(t => collect(t.typeCode))
  }
  collect(props.typeData.typeCode)
  return props.allTypes.filter(t => !excludeSet.has(t.typeCode))
})
```

### 5.6 ide.vue 集成修改

```vue
<!-- Sidebar 剧集 Tab -->
<EpisodeTypeExplorer
  v-else-if="sidebarTab === 'episodes'"
  :graph-id="effectiveGraphId"
  @select-type="handleEpisodeTypeSelect"
  @create-type="handleEpisodeTypeCreate"
/>

<!-- Right Panel：新增 EpisodeTypeDetailPanel 条件渲染 -->
<template v-else-if="sidebarTab === 'episodes' && selectedEpisodeType">
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

```typescript
// ide.vue 新增方法
const handleEpisodeTypeSelect = async (payload: { typeId: number; typeCode: string }) => {
  ontologyMode.value = 'episodes'
  showPanel.value = true
  loading.value = true
  try {
    const [detail, visData] = await Promise.all([
      episodeTypeApi.get(effectiveGraphId.value, payload.typeId),
      graphApi.getEpisodesVisualizationByType(effectiveGraphId.value, payload.typeCode, true, 100)
    ])
    selectedEpisodeType.value = detail.data
    nodes.value = dedupeNodes(visData.data?.nodes || [])
    edges.value = dedupeEdges(visData.data?.edges || [])
  } finally {
    loading.value = false
  }
}
```

---

## 六、关键交互流程

### 6.1 类型选择 → 图谱联动

1. 用户点击类型树节点
2. `EpisodeTypeExplorer` emit `@select-type`
3. `ide.vue` 并行请求：
   - `episodeTypeApi.get()` → 更新 `selectedEpisodeType` → 右侧面板渲染
   - `graphApi.getEpisodesVisualizationByType()` → 更新 nodes/edges → GraphCanvas 渲染
   - `EpisodeTypeDetailPanel.loadInstances()` → 实例列表 Tab 就绪

### 6.2 删除类型（含依赖检查）

1. `EpisodeTypeDetailPanel` 加载时自动调用 `checkDelete`
2. 用户点击删除按钮：
   - 若 `canDelete === false`：禁用确认，hover 显示原因
   - 若 `canDelete === true`：弹出 `a-popconfirm` 确认
3. 确认后 emit `@delete-type` → `ide.vue` 调用 `episodeTypeApi.delete()`
4. Service 层二次校验 → 执行删除 → 前端刷新状态

### 6.3 拖拽排序

1. 用户在树形视图中拖拽节点
2. `a-tree @drop` 触发 `handleDrop`
3. 计算新的 `parent_type_code` 和 `sort_order`
4. 调用 `episodeTypeApi.reorder()` 批量更新
5. 乐观锁防止并发冲突（`@Version` 字段）
6. 刷新树数据

---

## 七、错误处理

| 错误场景 | 前端处理 | 后端处理 |
|----------|----------|----------|
| 删除时存在子类型 | `a-popconfirm` 禁用，显示原因 | `ServiceException`，返回 400 |
| 删除时存在实例引用 | 同上，显示引用数量 | `ServiceException`，返回 400 |
| 类型编码重复 | 表单校验错误提示 | `DuplicateKeyException` → 400 |
| 父类型循环依赖 | 编辑弹窗排除自身及子类型选项 | 保存时递归检查 parent chain |
| 拖拽排序并发冲突 | 自动重试（重新加载后重试） | 乐观锁 `@Version`，冲突抛异常 |
| 网络请求失败 | `message.error()` + 加载状态恢复 | 标准异常处理器统一返回 |
| 画布数据量过大 | 显示"已显示前100条"提示 | 查询 limit=100 |

---

## 八、边界情况

| 场景 | 处理方案 |
|------|----------|
| 类型层级过深（>5层） | 前端树组件截断，后端 `CHECK (level BETWEEN 1 AND 5)` |
| 批量导入格式错误 | 返回 `EpisodeTypeImportResultVO`，逐行报告错误 |
| 父类型删除后子类型孤儿化 | 删除前强制先删子类型 |
| INACTIVE 类型仍被引用 | 允许历史引用，新建时过滤 INACTIVE |
| 切换图谱 | ide.vue watch graphId → 重置所有状态 → 重新加载 |

---

## 九、性能考虑

| 优化点 | 方案 |
|--------|------|
| 类型树大数据量 | 元数据表数据量小，一次性返回全量 |
| 实例列表分页 | 右侧面板独立分页（pageSize=10） |
| 画布数据量 | `limit=100`，超出提示 |
| 重复请求避免 | ide.vue 缓存 `selectedEpisodeType` |
| 类型列表缓存 | EpisodeTypeExplorer 本地缓存，显式刷新时重载 |

---

## 十、前端 API 封装

```typescript
// ontograph-web/src/api/metadata.ts
export const episodeTypeApi = {
  list: (graphId: string, definitionId: number, processType?: string) =>
    request.get<OntEpisodeTypeVO[]>(`/ontology/${graphId}/episode-types`, { params: { definitionId, ...(processType ? { processType } : {}) } }),

  getTree: (graphId: string, definitionId: number) =>
    request.get<OntEpisodeTypeVO[]>(`/ontology/${graphId}/episode-types/tree`, { params: { definitionId } }),

  get: (graphId: string, id: number) =>
    request.get<OntEpisodeTypeVO>(`/ontology/${graphId}/episode-types/${id}`),

  create: (graphId: string, data: Partial<OntEpisodeTypeVO>) =>
    request.post<number>(`/ontology/${graphId}/episode-types`, data),

  update: (graphId: string, id: number, data: Partial<OntEpisodeTypeVO>) =>
    request.put(`/ontology/${graphId}/episode-types/${id}`, data),

  delete: (graphId: string, id: number) =>
    request.delete(`/ontology/${graphId}/episode-types/${id}`),

  checkDelete: (graphId: string, id: number) =>
    request.get<EpisodeTypeDeleteCheckVO>(`/ontology/${graphId}/episode-types/${id}/delete-check`),

  reorder: (graphId: string, items: EpisodeTypeReorderItem[]) =>
    request.post(`/ontology/${graphId}/episode-types/reorder`, items),

  import: (graphId: string, definitionId: number, items: Partial<OntEpisodeTypeVO>[]) =>
    request.post<EpisodeTypeImportResultVO>(`/ontology/${graphId}/episode-types/import`, items, { params: { definitionId } }),

  getInstances: (graphId: string, id: number, page: number, pageSize: number, keyword?: string) =>
    request.get<PageResult<EpisodeListItem>>(`/ontology/${graphId}/episode-types/${id}/instances`, {
      params: { page, pageSize, ...(keyword ? { keyword } : {}) }
    }),
}
```

---

## 十一、实施范围

### 需新建的文件

1. `ontograph-web/src/components/Ontology/EpisodeTypeExplorer.vue`
2. `ontograph-web/src/components/Ontology/EpisodeTypeDetailPanel.vue`
3. `ontograph-web/src/components/Ontology/EpisodeTypeEditModal.vue`
4. `sql/migrations/v005_episode_type_hierarchy.sql`

### 需修改的文件

1. `ontograph-web/src/views/graph/ide.vue` — 集成新组件
2. `ontograph-web/src/api/metadata.ts` — 扩展 episodeTypeApi
3. `ontograph-module-core/.../OntEpisodeTypeDO.java` — 重构字段
4. `ontograph-module-core/.../OntEpisodeTypeMapper.java` — 扩展方法
5. `ontograph-module-core/.../OntEpisodeTypeReqVO.java` — 删除旧字段，新增层级字段
6. `ontograph-module-core/.../OntEpisodeTypeRespVO.java` — 同上
7. `ontograph-module-core/.../OntMetadataController.java` — 扩展接口
8. `ontograph-module-core/.../OntMetadataService.java` — 扩展方法
9. `ontograph-module-core/.../OntMetadataServiceImpl.java` — 实现新业务逻辑
10. `ontograph-module-core/.../GraphIDEController.java` — 新增可视化接口

### 可删除的文件

1. `ontograph-web/src/components/Ontology/EpisodeExplorer.vue` — 被 EpisodeTypeExplorer 替代
