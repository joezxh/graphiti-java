---
name: Business Info Module Plan
overview: 设计并实现"业务信息管理"模块，包含三个子功能：(1) 业务信息本体定义生成与导入，(2) 业务描述 AI 优化，(3) 业务数据模拟生成。模块通过 LLM 智能生成符合项目规范的本体定义，支持草稿暂存和直接导入两种模式。提供科幻未来感 UI 界面，具备本体元数据可视化查看和模拟数据图谱交互式探索能力。
todos:
  - id: backend-infra
    content: "后端基础设施: 数据库表 + DO + Mapper + 错误码"
    status: completed
  - id: backend-service
    content: "后端 Service 层: BusinessInfoService + OntologyDraftService"
    status: completed
  - id: backend-controller
    content: "后端 Controller: BusinessInfoController REST API"
    status: completed
  - id: prompt-templates
    content: "LLM 提示词模板: generate_ontology + optimize_desc + generate_data"
    status: completed
  - id: frontend-api
    content: "前端 API Client: business-info.ts"
    status: in_progress
  - id: frontend-vis-core
    content: "前端可视化核心: SciFiGraph 组件 + 样式系统"
    status: pending
  - id: frontend-ontology-viewer
    content: "前端页面: ontology-metadata-viewer.vue — 本体元数据可视化"
    status: pending
  - id: frontend-mock-graph
    content: "前端页面: mock-data-graph.vue — 模拟数据图查看器"
    status: pending
  - id: frontend-page
    content: "前端页面: business-info/index.vue — 主集成页面"
    status: pending
  - id: frontend-ontology-integration
    content: "前端集成: ontology/index.vue 内嵌优化按钮"
    status: pending
  - id: i18n-menu
    content: 国际化文本 + 路由菜单注册
    status: pending
isProject: false
---

# 业务信息管理模块 — 技术方案（v2）

## 一、总体架构

### 1.1 后端模块结构

```
ontograph-module-core/
├── controller/admin/
│   └── BusinessInfoController.java        # REST 端点
├── service/
│   ├── BusinessInfoService.java           # 接口定义
│   ├── OntologyDraftService.java          # 本体草稿管理
│   ├── OntologyMetadataService.java       # 元数据查看服务
│   └── impl/
│       ├── BusinessInfoServiceImpl.java   # 核心业务逻辑
│       ├── OntologyDraftServiceImpl.java  # 草稿 CRUD
│       └── OntologyMetadataServiceImpl.java # 元数据查询
├── vo/
│   ├── BusinessInfoVO.java               # 主视图对象
│   ├── req/
│   │   ├── GenerateOntologyReqVO.java
│   │   ├── OptimizeDescReqVO.java
│   │   └── GenerateDataReqVO.java
│   └── resp/
│       ├── GenerateOntologyRespVO.java
│       ├── OptimizeDescRespVO.java
│       ├── GenerateDataRespVO.java
│       └── OntologyGraphVO.java           # 可视化图数据
└── dal/
    ├── dataobject/
    │   └── ont/OntDraftDO.java           # 本体草稿持久化
    └── mysql/
        └── ont/OntDraftMapper.java

ontograph-module-core/src/main/resources/prompts/
├── business_info/generate_ontology.txt
├── business_info/optimize_desc.txt
└── business_info/generate_data.txt
```

### 1.2 前端模块结构

```
ontograph-web/src/
├── api/
│   └── business-info.ts                   # API Client
├── components/
│   └── sci-fi/                          # 科幻可视化组件库
│       ├── SciFiGraph.vue              # 核心图可视化组件
│       ├── SciFiNode.vue               # 节点组件（脉冲动画）
│       ├── SciFiEdge.vue               # 边组件（流动效果）
│       ├── NodeDetailPanel.vue         # 节点详情面板
│       └── mini/
│           ├── OntologyMiniGraph.vue   # 小型本体关系图
│           └── MockDataMiniGraph.vue   # 小型模拟数据图
├── views/business-info/
│   ├── index.vue                        # 主页面（Tab 整合）
│   ├── ontology-metadata-viewer.vue    # 本体元数据查看器（独立路由）
│   └── mock-data-graph.vue             # 模拟数据图查看器（独立路由）
└── styles/
    └── sci-fi/                         # 科幻主题样式
        ├── variables.less              # CSS 变量定义
        ├── glass.less                 # 玻璃态样式
        ├── glow.less                  # 发光效果
        └── animation.less             # 动态效果
```

---

## 二、后端详细设计

### 2.1 数据库表：`ont_draft`

```sql
CREATE TABLE ont_draft (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    graph_id        VARCHAR(64) NOT NULL COMMENT '图谱ID',
    draft_name      VARCHAR(255) NOT NULL COMMENT '草稿名称',
    draft_type      VARCHAR(32) NOT NULL COMMENT 'DRAFT | OPTIMIZED | GENERATED',
    source_info     TEXT COMMENT '原始业务信息（JSON）',
    generated_info  TEXT COMMENT '生成内容（JSON）',
    mock_data      TEXT COMMENT '生成的模拟数据（JSON）',
    status          VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING | APPROVED | REJECTED | APPLIED',
    created_by      VARCHAR(64),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_graph_id (graph_id),
    INDEX idx_status (status)
);
```

### 2.2 LLM 提示词设计

#### generate_ontology.txt — 本体定义生成

```
你是一位知识图谱本体工程师。根据用户提供的业务描述，生成符合 OWL 2 RL 规范的本体定义。

业务场景：{{businessScenario}}
领域类型：{{domainHint}}
用户输入：{{userInput}}

输出要求（严格 JSON）：
{
  "definition": {
    "name": "本体名称",
    "namespace": "http://your-project/ontology",
    "version": "1.0.0",
    "description": "本体描述"
  },
  "classes": [
    {
      "localName": "类名称",
      "classUri": "完整 URI",
      "parentClass": "父类（可选）",
      "description": "类描述",
      "example": "典型实例",
      "domainHint": "FINANCIAL|MEDICAL|ECOMMERCE|LEGAL|KNOWLEDGE|GENERAL"
    }
  ],
  "properties": [
    {
      "localName": "属性名称",
      "propertyUri": "完整 URI",
      "propertyType": "DATATYPE|OBJECT|ANNOTATION",
      "domainClass": "所属类",
      "rangeClass": "目标类（OBJECT 属性）",
      "rangeDataType": "string|integer|date（DATATYPE 属性）",
      "isRequired": true|false,
      "isMultiple": true|false,
      "description": "属性描述"
    }
  ],
  "relationships": [
    {
      "sourceClass": "源类",
      "targetClass": "目标类",
      "relationshipType": "关系类型",
      "description": "关系描述"
    }
  ]
}

要求：
1. 类名使用 PascalCase（如 Product、OrderItem）
2. 属性名使用 camelCase（如 productName、orderDate）
3. 优先使用已有本体（Schema.org）的类/属性
4. 必要时创建子类和子属性表示细粒度概念
5. 关系类型使用有意义的动词或介词短语（如 HAS_MEMBER, LOCATED_IN）
6. 仅输出 JSON，不要有其他文字
```

#### optimize_desc.txt — 描述优化

```
你是一位专业的业务分析师。请优化以下业务描述，使其更准确、完整、规范。

原始描述：{{originalDescription}}
上下文：{{context}}（可选，如所属类名、属性名等）
语言：{{language}}

要求：
1. 保持原意，不添加虚假信息
2. 使用规范的业务术语
3. 长度控制在 50-300 字
4. 包含必要的限定和约束条件
5. 提供 2-3 个优化版本供选择

输出格式（严格 JSON）：
{
  "original": "原始描述",
  "optimizations": [
    {
      "version": "v1",
      "description": "优化后的描述",
      "highlights": ["亮点1", "亮点2"]
    }
  ]
}
```

#### generate_data.txt — 数据模拟生成

```
你是测试数据生成专家。根据以下本体定义，生成符合业务逻辑的模拟数据。

本体定义：{{ontologyDefinition}}
数据规模：{{count}} 条
格式要求：{{format}}（JSON/CSV/N-Triples）

生成数据要求：
1. 数据符合各字段的类型约束（rangeDataType）
2. 实体名称和关系符合业务逻辑
3. 数据具有多样性和代表性
4. 避免敏感信息和真实个人数据
5. 必要时生成关联数据（如订单对应的客户、商品等）
6. 生成的数据节点之间应有丰富的关联关系

输出格式（严格 JSON）：
{
  "entities": [
    {
      "type": "实体类型",
      "name": "实体名称",
      "properties": { "属性名": "值" }
    }
  ],
  "relationships": [
    {
      "source": "源实体名",
      "target": "目标实体名",
      "type": "关系类型",
      "fact": "事实陈述"
    }
  ]
}
```

### 2.3 核心 Service 类

#### BusinessInfoService.java

```java
public interface BusinessInfoService {

    // --- Feature 1: 本体定义生成 ---
    GenerateOntologyRespVO generateOntology(String graphId, GenerateOntologyReqVO reqVO);
    void saveAsDraft(String graphId, GenerateOntologyReqVO reqVO);
    List<BusinessInfoVO.OntDraftVO> listDrafts(String graphId);
    void deleteDraft(Long draftId);

    // --- Feature 2: 描述优化 ---
    OptimizeDescRespVO optimizeDescription(OptimizeDescReqVO reqVO);
    OptimizeDescRespVO optimizeBatch(OptimizeDescReqVO reqVO);

    // --- Feature 3: 数据模拟生成 ---
    GenerateDataRespVO generateMockData(String graphId, GenerateDataReqVO reqVO);
    GenerateDataRespVO generateFromDraft(String graphId, Long draftId, int count);
}
```

#### OntologyMetadataService.java

```java
public interface OntologyMetadataService {

    // 本体元数据图（用于可视化）
    OntologyGraphVO getOntologyGraph(String graphId);

    // 模拟数据图（用于可视化）
    OntologyGraphVO getMockDataGraph(String graphId, Long draftId);

    // 图统计信息
    Map<String, Object> getGraphStats(String graphId);
}
```

### 2.4 Controller 端点

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/v1/business-info/{graphId}/generate` | 生成本体定义 |
| GET | `/api/v1/business-info/{graphId}/drafts` | 列出草稿 |
| POST | `/api/v1/business-info/{graphId}/drafts/{id}/apply` | 确认导入 |
| DELETE | `/api/v1/business-info/{graphId}/drafts/{id}` | 删除草稿 |
| POST | `/api/v1/business-info/optimize` | 优化描述 |
| POST | `/api/v1/business-info/optimize/batch` | 批量优化 |
| POST | `/api/v1/business-info/{graphId}/mock-data` | 生成模拟数据 |
| GET | `/api/v1/business-info/{graphId}/metadata/graph` | 获取本体元数据图 |
| GET | `/api/v1/business-info/{graphId}/mock-graph` | 获取模拟数据图 |
| GET | `/api/v1/business-info/{graphId}/stats` | 图统计 |

### 2.5 OntologyGraphVO — 可视化图数据格式

```java
@Data @Builder
public class OntologyGraphVO {
    private List<NodeVO> nodes;    // 节点列表
    private List<EdgeVO> edges;    // 边列表
    private GraphMetaVO meta;       // 图元信息

    @Data @Builder
    public static class NodeVO {
        private String id;
        private String label;
        private String type;           // CLASS | ENTITY | PROPERTY
        private String category;        // 领域分类
        private Map<String, Object> data;  // 附加数据
        private String color;           // 节点颜色（用于可视化）
    }

    @Data @Builder
    public static class EdgeVO {
        private String id;
        private String source;
        private String target;
        private String label;
        private String type;           // INHERITS | HAS_PROPERTY | RELATES_TO
        private Map<String, Object> data;
    }

    @Data @Builder
    public static class GraphMetaVO {
        private int nodeCount;
        private int edgeCount;
        private int entityTypeCount;
        private int relationTypeCount;
        private List<String> entityTypes;
        private List<String> relationTypes;
    }
}
```

---

## 三、前端科幻主题样式系统

### 3.1 CSS 变量定义 — variables.less

```less
// 科幻霓虹色彩系统
@neon-blue: #00f0ff;
@neon-cyan: #00ffcc;
@neon-purple: #bf5fff;
@neon-pink: #ff3dcc;
@neon-yellow: #ffe066;

// 深色背景层级
@bg-deepest: #03050a;
@bg-deep: #070b12;
@bg-base: #0a1020;
@bg-elevated: #0f1628;
@bg-overlay: rgba(10, 16, 32, 0.85);

// 玻璃态
@glass-bg: rgba(15, 22, 40, 0.6);
@glass-border: rgba(0, 240, 255, 0.15);
@glass-highlight: rgba(0, 240, 255, 0.08);

// 发光效果
@glow-cyan: 0 0 10px rgba(0, 240, 255, 0.5), 0 0 20px rgba(0, 240, 255, 0.25);
@glow-purple: 0 0 10px rgba(191, 95, 255, 0.5), 0 0 20px rgba(191, 95, 255, 0.25);
@glow-blue: 0 0 8px rgba(0, 150, 255, 0.4), 0 0 16px rgba(0, 150, 255, 0.2);

// 文字
@text-primary: #e8f4f8;
@text-secondary: rgba(232, 244, 248, 0.65);
@text-dim: rgba(232, 244, 248, 0.35);
```

### 3.2 玻璃态样式 — glass.less

```less
.glass-panel {
  background: @glass-bg;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid @glass-border;
  border-radius: 12px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.05),
    @glow-cyan;
}

.glass-card {
  background: linear-gradient(
    135deg,
    rgba(15, 22, 40, 0.7) 0%,
    rgba(10, 16, 32, 0.85) 100%
  );
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(0, 240, 255, 0.1);
  border-radius: 16px;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(
      90deg,
      transparent,
      @neon-cyan,
      transparent
    );
    opacity: 0.6;
  }
}

.neon-border {
  border: 1px solid rgba(0, 240, 255, 0.3);
  box-shadow:
    inset 0 0 20px rgba(0, 240, 255, 0.05),
    0 0 15px rgba(0, 240, 255, 0.1);
}
```

### 3.3 发光效果 — glow.less

```less
.glow-text-cyan {
  color: @neon-cyan;
  text-shadow: 0 0 10px rgba(0, 240, 255, 0.8);
}

.glow-text-purple {
  color: @neon-purple;
  text-shadow: 0 0 10px rgba(191, 95, 255, 0.8);
}

.glow-border-cyan {
  border-color: @neon-cyan;
  box-shadow: @glow-cyan;
}

.neon-divider {
  height: 1px;
  background: linear-gradient(
    90deg,
    transparent,
    @neon-cyan,
    @neon-purple,
    transparent
  );
  border: none;
  margin: 24px 0;
}
```

### 3.4 动态效果 — animation.less

```less
// 节点脉冲动画（用于图谱节点）
@keyframes nodePulse {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 0 8px currentColor;
  }
  50% {
    transform: scale(1.08);
    box-shadow: 0 0 16px currentColor, 0 0 24px currentColor;
  }
}

.pulse-node {
  animation: nodePulse 2.5s ease-in-out infinite;
}

// 连线流动效果（用于图谱边）
@keyframes edgeFlow {
  0% { stroke-dashoffset: 20; }
  100% { stroke-dashoffset: 0; }
}

.flowing-edge {
  stroke-dasharray: 8 4;
  animation: edgeFlow 1s linear infinite;
}

// 扫描线效果（背景装饰）
@keyframes scanLine {
  0% { transform: translateY(-100%); }
  100% { transform: translateY(100vh); }
}

.scan-line {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(0, 240, 255, 0.15),
    transparent
  );
  animation: scanLine 8s linear infinite;
  pointer-events: none;
  z-index: 0;
}

// 渐入动画
@keyframes fadeSlideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.fade-slide-up {
  animation: fadeSlideUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

// 网格背景
.grid-bg {
  background-image:
    linear-gradient(rgba(0, 240, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 240, 255, 0.03) 1px, transparent 1px);
  background-size: 40px 40px;
}
```

---

## 四、前端可视化核心组件

### 4.1 SciFiGraph.vue — 核心图可视化组件

基于 D3.js 力导向图实现，集成科幻视觉效果：

```vue
<template>
  <div ref="containerRef" class="scifi-graph-container">
    <!-- SVG 图谱区域 -->
    <svg ref="svgRef" class="scifi-graph-svg">
      <defs>
        <!-- 节点发光滤镜 -->
        <filter id="nodeGlow" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur stdDeviation="4" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
        <!-- 边流动渐变 -->
        <linearGradient id="edgeGradient" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stop-color="#00f0ff" stop-opacity="0.8" />
          <stop offset="50%" stop-color="#bf5fff" stop-opacity="1" />
          <stop offset="100%" stop-color="#00f0ff" stop-opacity="0.8" />
        </linearGradient>
        <!-- 节点渐变 -->
        <radialGradient id="nodeGradient" cx="30%" cy="30%">
          <stop offset="0%" stop-color="#00ffcc" stop-opacity="1" />
          <stop offset="100%" stop-color="#004455" stop-opacity="0.8" />
        </radialGradient>
      </defs>

      <!-- 边层 -->
      <g class="edges-layer">
        <g v-for="edge in edges" :key="edge.id" class="edge-group">
          <path
            :d="edge.path"
            class="scifi-edge"
            :class="{ 'flowing-edge': edge.type !== 'INHERITS' }"
            :stroke="getEdgeColor(edge.type)"
            stroke-width="1.5"
            fill="none"
            marker-end="url(#arrowhead)"
          />
          <text
            v-if="edge.label && showEdgeLabels"
            :x="edge.midX"
            :y="edge.midY"
            class="edge-label"
            fill="rgba(0, 240, 255, 0.6)"
            font-size="10"
            text-anchor="middle"
          >
            {{ edge.label }}
          </text>
        </g>
      </g>

      <!-- 节点层 -->
      <g class="nodes-layer">
        <g
          v-for="node in nodes"
          :key="node.id"
          class="node-group"
          :transform="`translate(${node.x}, ${node.y})`"
          @mouseenter="onNodeHover(node, $event)"
          @mouseleave="onNodeLeave"
          @click="onNodeClick(node)"
        >
          <!-- 外圈脉冲 -->
          <circle
            r="32"
            class="node-pulse-ring"
            :fill="getNodeColor(node.type)"
            :style="{ animationDelay: `${Math.random() * 2}s` }"
          />
          <!-- 节点主体 -->
          <circle
            r="20"
            :fill="getNodeColor(node.type)"
            filter="url(#nodeGlow)"
            class="node-body"
          />
          <!-- 节点图标 -->
          <text
            y="5"
            text-anchor="middle"
            font-size="14"
            fill="white"
            class="node-icon"
          >
            {{ getNodeIcon(node.type) }}
          </text>
          <!-- 节点标签 -->
          <text
            y="38"
            text-anchor="middle"
            font-size="11"
            fill="#e8f4f8"
            class="node-label"
          >
            {{ node.label }}
          </text>
        </g>
      </g>
    </svg>

    <!-- 节点详情悬浮面板 -->
    <NodeDetailPanel
      v-if="hoveredNode"
      :node="hoveredNode"
      :position="panelPosition"
      class="node-detail-float"
    />

    <!-- 图例 -->
    <div class="graph-legend glass-panel">
      <div v-for="type in nodeTypes" :key="type.value" class="legend-item">
        <span class="legend-dot" :style="{ background: type.color }"></span>
        <span class="legend-text">{{ type.label }}</span>
      </div>
    </div>

    <!-- 控制栏 -->
    <div class="graph-controls glass-panel">
      <a-button size="small" @click="zoomIn" title="放大">
        <ZoomInOutlined />
      </a-button>
      <a-button size="small" @click="zoomOut" title="缩小">
        <ZoomOutOutlined />
      </a-button>
      <a-button size="small" @click="resetView" title="重置视图">
        <AimOutlined />
      </a-button>
      <a-button size="small" @click="toggleLabels" title="切换标签">
        <FontSizeOutlined />
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as d3 from 'd3'

interface GraphNode {
  id: string
  label: string
  type: 'CLASS' | 'ENTITY' | 'PROPERTY'
  category?: string
  data?: Record<string, any>
  x?: number
  y?: number
  fx?: number | null
  fy?: number | null
}

interface GraphEdge {
  id: string
  source: string
  target: string
  label?: string
  type: 'INHERITS' | 'HAS_PROPERTY' | 'RELATES_TO' | 'INSTANCE_OF'
}

const props = defineProps<{
  nodes: GraphNode[]
  edges: GraphEdge[]
  height?: number
}>()

const emit = defineEmits<{
  (e: 'node-click', node: GraphNode): void
  (e: 'node-hover', node: GraphNode, event: MouseEvent): void
  (e: 'node-leave'): void
}>()

// D3 力导向模拟
let simulation: d3.Simulation<GraphNode, GraphEdge>
const svgRef = ref<SVGSVGElement>()
const containerRef = ref<HTMLDivElement>()
const hoveredNode = ref<GraphNode | null>(null)
const panelPosition = ref({ x: 0, y: 0 })
const showEdgeLabels = ref(true)

const nodeTypes = [
  { value: 'CLASS', label: '实体类型', color: '#00f0ff' },
  { value: 'PROPERTY', label: '属性', color: '#bf5fff' },
  { value: 'ENTITY', label: '数据实体', color: '#00ffcc' },
]

function getNodeColor(type: string) {
  const colors: Record<string, string> = {
    CLASS: '#00f0ff',
    PROPERTY: '#bf5fff',
    ENTITY: '#00ffcc',
  }
  return colors[type] || '#00f0ff'
}

function getNodeIcon(type: string) {
  const icons: Record<string, string> = {
    CLASS: '◈',
    PROPERTY: '◆',
    ENTITY: '◉',
  }
  return icons[type] || '●'
}

function getEdgeColor(type: string) {
  const colors: Record<string, string> = {
    INHERITS: '#bf5fff',
    HAS_PROPERTY: '#00f0ff',
    RELATES_TO: '#ffe066',
    INSTANCE_OF: '#00ffcc',
  }
  return colors[type] || '#00f0ff'
}

function initSimulation() {
  simulation = d3.forceSimulation(props.nodes as any)
    .force('link', d3.forceLink(props.edges)
      .id((d: any) => d.id)
      .distance(120))
    .force('charge', d3.forceManyBody().strength(-300))
    .force('center', d3.forceCenter(400, 300))
    .force('collision', d3.forceCollide().radius(50))
    .on('tick', () => {
      // 更新边的路径
      updateEdgePaths()
    })
}

function updateEdgePaths() {
  // D3 tick 会自动更新 node.x, node.y
  // 边路径计算在 nextTick 中执行
}
</script>

<style scoped lang="less">
@import '@/styles/sci-fi/variables.less';
@import '@/styles/sci-fi/glass.less';

.scifi-graph-container {
  position: relative;
  width: 100%;
  height: v-bind('props.height ? props.height + "px" : "600px"');
  background: @bg-deep;
  border-radius: 16px;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background-image:
      linear-gradient(rgba(0, 240, 255, 0.02) 1px, transparent 1px),
      linear-gradient(90deg, rgba(0, 240, 255, 0.02) 1px, transparent 1px);
    background-size: 30px 30px;
    pointer-events: none;
  }
}

.scifi-graph-svg {
  width: 100%;
  height: 100%;
  cursor: grab;

  &:active { cursor: grabbing; }
}

.node-group {
  cursor: pointer;
  transition: transform 0.2s;

  &:hover .node-body {
    filter: url(#nodeGlow) brightness(1.3);
  }
}

.node-pulse-ring {
  opacity: 0;
  animation: nodePulseRing 2.5s ease-out infinite;
}

@keyframes nodePulseRing {
  0% { r: 20; opacity: 0.6; }
  100% { r: 35; opacity: 0; }
}

.scifi-edge {
  stroke-opacity: 0.6;
  transition: stroke-opacity 0.2s;

  &:hover {
    stroke-opacity: 1;
    stroke-width: 2.5;
  }
}

.flowing-edge {
  stroke-dasharray: 8 4;
  animation: edgeFlow 1.5s linear infinite;
}

@keyframes edgeFlow {
  0% { stroke-dashoffset: 24; }
  100% { stroke-dashoffset: 0; }
}

.node-detail-float {
  position: absolute;
  z-index: 100;
  pointer-events: none;
}

.graph-legend {
  position: absolute;
  bottom: 16px;
  left: 16px;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;

  .legend-item {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .legend-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    box-shadow: 0 0 6px currentColor;
  }

  .legend-text {
    color: @text-secondary;
    font-size: 12px;
  }
}

.graph-controls {
  position: absolute;
  top: 16px;
  right: 16px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
</style>
```

### 4.2 NodeDetailPanel.vue — 节点详情面板

```vue
<template>
  <div
    class="node-detail-panel glass-panel"
    :style="{ left: position.x + 'px', top: position.y + 'px' }"
  >
    <div class="panel-header">
      <span class="node-icon" :style="{ color: nodeColor }">{{ icon }}</span>
      <span class="node-title glow-text-cyan">{{ node.label }}</span>
    </div>
    <div class="panel-body">
      <div class="detail-row">
        <span class="label">类型</span>
        <a-tag :color="nodeColor">{{ node.type }}</a-tag>
      </div>
      <div v-if="node.category" class="detail-row">
        <span class="label">领域</span>
        <span class="value">{{ node.category }}</span>
      </div>
      <div v-for="(value, key) in node.data" :key="key" class="detail-row">
        <span class="label">{{ key }}</span>
        <span class="value">{{ formatValue(value) }}</span>
      </div>
    </div>
    <div class="panel-connections" v-if="connections.length">
      <div class="connections-title">关联关系</div>
      <div v-for="conn in connections" :key="conn.id" class="connection-item">
        <span class="conn-type" :style="{ color: getEdgeColor(conn.type) }">
          {{ conn.type }}
        </span>
        <span class="conn-target">{{ conn.target }}</span>
      </div>
    </div>
  </div>
</template>
```

### 4.3 配色主题扩展

扩展 Ant Design Vue 的暗色主题变量：

```less
// src/styles/sci-fi/theme-overrides.less
@primary-color: #00f0ff;
@success-color: #00ffcc;
@warning-color: #ffe066;
@error-color: #ff3dcc;
@info-color: #00f0ff;

@body-background: #03050a;
@component-background: #0a1020;
@text-color: #e8f4f8;
@text-color-secondary: rgba(232, 244, 248, 0.65);
@border-color-base: rgba(0, 240, 255, 0.15);

@border-radius-base: 12px;
@border-radius-lg: 16px;
```

---

## 五、前端页面设计

### 5.1 页面一：ontology-metadata-viewer.vue（本体元数据查看器）

独立路由：`/business-info/ontology/:graphId`

```
┌──────────────────────────────────────────────────────────────────┐
│  [扫描线动画背景]                                                 │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ 本体元数据查看器                        [返回] [图谱选择 ▾] │ │
│  │ ─────────────────────────────────────────────────────────  │ │
│  │  实体类型: 5    关系类型: 8    节点: 13    边: 24           │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌────────────────────────────┐ ┌──────────────────────────────┐ │
│  │                            │ │  关系类型分布                 │ │
│  │    本体关系图谱             │ │  ╭─── HAS_PROPERTY (6)      │ │
│  │    ┌───┐                   │ │  ├── INHERITS (3)           │ │
│  │    │Person│───►│Company│   │ │  └── RELATES_TO (4)         │ │
│  │    └───┘      └──────┘    │ ├──────────────────────────────┤ │
│  │       │           │        │ │  实体类型列表                 │ │
│  │    ┌──▼──┐    ┌──▼──┐     │ │  ◈ Person (5)               │ │
│  │    │Employee│ │Product│   │ │  ◈ Company (3)               │ │
│  │    └───┬──┘    └───┬──┘     │ │  ◈ Event (2)                │ │
│  │        │           │        │ │  ...                        │ │
│  │    ┌───▼───────────▼───┐   │ ├──────────────────────────────┤ │
│  │    │    Employment │     │   │ │  关系类型列表                 │ │
│  │    │    ProductOf  │     │   │ │  → HAS_MEMBER (2)           │ │
│  │    └─────────────────────┘   │ │  → WORKS_AT (3)             │ │
│  └────────────────────────────┘ └──────────────────────────────┘ │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  属性定义表                                                 │ │
│  │  ┌──────────┬─────────────┬───────┬────────┬──────────┐   │ │
│  │  │ 属性名    │ 类型        │ 域    │ 范围   │ 必填     │   │ │
│  │  ├──────────┼─────────────┼───────┼────────┼──────────┤   │ │
│  │  │ name     │ DATATYPE    │ Person│ string │ ✓        │   │ │
│  │  │ worksAt  │ OBJECT      │ Person│ Company│          │   │ │
│  │  └──────────┴─────────────┴───────┴────────┴──────────┘   │ │
│  └────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

功能要点：
- 力导向图展示实体类型间的继承(HAS_SUPERCLASS)、属性关联(HAS_PROPERTY)、关系(RELATES_TO)
- 左侧为 SciFiGraph 组件，右侧为统计面板
- 底部为属性定义表格
- 节点悬浮显示详情面板，包含属性列表和关系
- 支持节点放大、缩小、重置视角、标签切换

### 5.2 页面二：mock-data-graph.vue（模拟数据图查看器）

独立路由：`/business-info/mock-data/:graphId`

```
┌──────────────────────────────────────────────────────────────────┐
│  模拟数据图查看器                               [返回] [导出数据]  │
│  ──────────────────────────────────────────────────────────────  │
│  数据规模: 50 条实体  关系: 120 条   [放大] [缩小] [重置] [标签]  │
│  ──────────────────────────────────────────────────────────────  │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                                                              │ │
│  │   ◉ 张三 ─── WORKS_AT ──→ ◈ 阿里巴巴                       │ │
│  │     │                    ↑                                  │ │
│  │     │ HAS_SKILL         │                                   │ │
│  │     ▼                  PARTICIPATES_IN                       │ │
│  │   ◆ Java开发 ────────► ◉ 电商平台项目                       │ │
│  │                              │                              │ │
│  │                    PURCHASES ──► ◉ 商品A                    │ │
│  │                                                              │ │
│  │  [节点详情悬浮面板，当鼠标悬浮时显示]                          │ │
│  │  ┌─────────────────────────────┐                           │ │
│  │  │ ◈ 阿里巴巴                   │                           │ │
│  │  │ 类型: Company                │                           │ │
│  │  │ 行业: 互联网                 │                           │ │
│  │  │ 员工数: 10000+               │                           │ │
│  │  │ ─── 关联关系 ───             │                           │ │
│  │  │ → HAS_MEMBER → 张三         │                           │ │
│  │  │ → LOCATED_IN → 杭州         │                           │ │
│  │  └─────────────────────────────┘                           │ │
│  │                                                              │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ 数据列表视图  [图视图] [表格视图]                            │ │
│  │ ┌────┬─────────┬──────────┬────────────┬────────────────┐  │ │
│  │ │ 名称│ 类型   │ 状态     │ 创建时间   │ 操作            │  │ │
│  │ ├────┼─────────┼──────────┼────────────┼────────────────┤  │ │
│  │ │张三 │ Person │ 活跃     │ 2024-01-01│ [查看] [关系]   │  │ │
│  │ └────┴─────────┴──────────┴────────────┴────────────────┘  │ │
│  └────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

功能要点：
- 基于 LLM 生成的模拟数据（节点+边）渲染力导向图
- 节点颜色按实体类型区分，边颜色按关系类型区分
- 节点悬浮显示详情（包含所有属性和关联关系列表）
- 点击节点可高亮所有关联路径（路径追踪）
- 底部支持切换图视图和表格视图
- 支持导出数据（JSON/CSV/N-Triples）

### 5.3 页面三：business-info/index.vue（主页面）

整合三个 Tab + 科幻主题：

```
┌──────────────────────────────────────────────────────────────────┐
│  ┌─ Tab 1: 本体生成 ─┬─ Tab 2: 描述优化 ─┬─ Tab 3: 数据模拟 ─┐ │
│  │                                                          │   │
│  │  左：输入区                    右：预览/图查看区          │   │
│  │  ┌──────────────────┐        ┌────────────────────────┐ │   │
│  │  │ 业务场景描述      │        │ SciFiGraph 可视化区域   │ │   │
│  │  │ ┌──────────────┐ │        │  ┌───┐                  │ │   │
│  │  │ │ 多行文本输入  │ │        │  │类A│──HAS_PROPERTY──►│类B│ │   │
│  │  └──────────────────┘ │        │  └───┘                  │ │   │
│  │                      │        │      │                    │ │   │
│  │  领域: [下拉选择]     │        │      └──►│属性1│          │ │   │
│  │                      │        └────────────────────────┘ │   │
│  │  [生成预览] [保存草稿] │        [确认导入]                  │   │
│  │                      │                                    │   │
│  │  ┌──────────────────────────────────────────────────────┐│   │
│  │  │ 草稿管理折叠面板                                      ││   │
│  │  │ v 草稿列表 → 预览 | 应用 | 删除                       ││   │
│  │  └──────────────────────────────────────────────────────┘│   │
│  └──────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

---

## 六、菜单与路由注册

```typescript
// ontograph-web/src/router/index.ts 中新增
{
  path: '/business-info',
  component: Layout,
  children: [
    {
      path: '',
      component: () => import('@/views/business-info/index.vue'),
      meta: { title: 'businessInfo.title', icon: 'RobotOutlined' }
    },
    {
      path: 'ontology/:graphId',
      component: () => import('@/views/business-info/ontology-metadata-viewer.vue'),
      meta: { title: 'businessInfo.ontologyViewer' }
    },
    {
      path: 'mock-data/:graphId',
      component: () => import('@/views/business-info/mock-data-graph.vue'),
      meta: { title: 'businessInfo.mockDataViewer' }
    }
  ]
}
```

---

## 七、错误码扩展

在 `ResultCode.java` 中扩展 Graphiti 业务错误码段 (2000-2099)：

```java
int ONT_DRAFT_NOT_FOUND = 2000;
int ONT_DRAFT_ALREADY_APPLIED = 2001;
int ONT_GENERATION_FAILED = 2002;
int DATA_GENERATION_FAILED = 2003;
int DESCRIPTION_OPTIMIZATION_FAILED = 2004;
int INVALID_DRAFT_TYPE = 2005;
int GRAPH_METADATA_NOT_FOUND = 2006;
int MOCK_DATA_NOT_FOUND = 2007;
```

---

## 八、实施步骤

### 阶段一：后端基础设施

1. 新建数据库表 `ont_draft`（增加 `mock_data` 字段）
2. 新建 `OntDraftDO` + `OntDraftMapper`
3. 新建 `OntologyGraphVO` 响应类
4. 新建 `ResultCode` 错误码扩展
5. 新建 `OntologyMetadataService` 接口 + 实现类
6. 新建 `BusinessInfoService` 接口 + 实现类
7. 新建 `OntologyDraftService` 接口 + 实现类
8. 新建所有 VO 类（req/resp）

### 阶段二：后端 LLM 集成

1. 编写三个提示词模板文件（存放在 `resources/prompts/business_info/`）
2. 实现 `BusinessInfoServiceImpl` 中的 LLM 调用逻辑
3. 实现草稿预览和应用逻辑
4. 实现描述优化和批量优化
5. 实现数据模拟生成
6. 实现 `OntologyMetadataServiceImpl` 的图数据查询

### 阶段三：后端 API 层

1. 新建 `BusinessInfoController`

### 阶段四：前端样式系统

1. 新建 `src/styles/sci-fi/variables.less`
2. 新建 `src/styles/sci-fi/glass.less`
3. 新建 `src/styles/sci-fi/glow.less`
4. 新建 `src/styles/sci-fi/animation.less`
5. 新建 `src/styles/sci-fi/theme-overrides.less`
6. 在 `src/styles/dark.less` 中引入科幻主题变量

### 阶段五：前端可视化核心

1. 新建 `src/components/sci-fi/SciFiGraph.vue`
2. 新建 `src/components/sci-fi/NodeDetailPanel.vue`
3. 新建 `src/components/sci-fi/mini/OntologyMiniGraph.vue`
4. 新建 `src/components/sci-fi/mini/MockDataMiniGraph.vue`

### 阶段六：前端页面

1. 新建 `src/api/business-info.ts`
2. 新建 `src/views/business-info/ontology-metadata-viewer.vue`
3. 新建 `src/views/business-info/mock-data-graph.vue`
4. 新建 `src/views/business-info/index.vue`（主页面）

### 阶段七：集成与路由

1. 在 `ontology/index.vue` 中集成内嵌优化按钮
2. 注册路由和菜单
3. 添加国际化文本（zh-CN.ts, en-US.ts, ja-JP.ts）
4. 添加单元测试
