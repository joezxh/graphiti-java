<template>
  <div
    class="node-detail-panel glass-panel"
    :style="{ left: position.x + 'px', top: position.y + 'px' }"
  >
    <!-- 头部 -->
    <div class="panel-header">
      <span class="node-icon" :style="{ color: nodeColor }">{{ nodeIcon }}</span>
      <span class="node-title glow-text-cyan">{{ node.label }}</span>
    </div>

    <!-- 基础信息 -->
    <div class="panel-body">
      <div class="detail-row">
        <span class="label">类型</span>
        <a-tag :color="nodeColor">{{ node.type }}</a-tag>
      </div>
      <div v-if="node.category" class="detail-row">
        <span class="label">分类</span>
        <span class="value">{{ node.category }}</span>
      </div>
      <div v-if="node.description" class="detail-row">
        <span class="label">描述</span>
        <span class="value description">{{ node.description }}</span>
      </div>
      <div v-if="node.example" class="detail-row">
        <span class="label">示例</span>
        <span class="value">{{ node.example }}</span>
      </div>

      <!-- 属性列表 -->
      <template v-if="node.data && Object.keys(node.data).length > 0">
        <div class="detail-row" v-for="(value, key) in node.data" :key="key">
          <span class="label">{{ key }}</span>
          <span class="value">{{ formatValue(value) }}</span>
        </div>
      </template>
    </div>

    <!-- 关联关系 -->
    <div v-if="edges && edges.length > 0" class="panel-connections">
      <div class="connections-title">
        <span class="title-text">关联关系</span>
        <span class="count">({{ edges.length }})</span>
      </div>
      <div
        v-for="edge in edges.slice(0, 5)"
        :key="edge.id"
        class="connection-item"
      >
        <span class="conn-type" :style="{ color: getEdgeColor(edge.type) }">
          {{ edge.type }}
        </span>
        <span class="conn-arrow">&rarr;</span>
        <span class="conn-target">{{ getOtherNodeLabel(edge) }}</span>
      </div>
      <div v-if="edges.length > 5" class="more-connections">
        +{{ edges.length - 5 }} more
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { GraphNodeVO, GraphEdgeVO } from '@/api/business-info'

const props = defineProps<{
  node: GraphNodeVO
  position: { x: number; y: number }
  edges?: GraphEdgeVO[]
}>()

const nodeColor = computed(() => {
  const colors: Record<string, string> = {
    CLASS: '#00f0ff',
    PROPERTY: '#bf5fff',
    ENTITY: '#00ffcc',
  }
  return colors[props.node.type] || '#00f0ff'
})

const nodeIcon = computed(() => {
  const icons: Record<string, string> = {
    CLASS: '\u25C8',
    PROPERTY: '\u25C6',
    ENTITY: '\u25C9',
  }
  return icons[props.node.type] || '\u25CF'
})

function formatValue(value: any): string {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'boolean') return value ? '是' : '否'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function getEdgeColor(type: string): string {
  const colors: Record<string, string> = {
    INHERITS: '#bf5fff',
    HAS_PROPERTY: '#00f0ff',
    HAS_RANGE: '#00f0ff',
    RELATES_TO: '#ffe066',
    INSTANCE_OF: '#00ffcc',
  }
  return colors[type] || '#00f0ff'
}

function getOtherNodeLabel(edge: GraphEdgeVO): string {
  const otherId = edge.source === props.node.id ? edge.target : edge.source
  return otherId.split('/').pop() || otherId
}
</script>

<style scoped lang="less">
@import '@/assets/styles/scifi-variables.less';
@import '@/assets/styles/scifi-glass.less';

.node-detail-panel {
  min-width: 220px;
  max-width: 320px;
  padding: 0;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(0, 240, 255, 0.1);
  background: rgba(0, 240, 255, 0.03);

  .node-icon {
    font-size: 18px;
    filter: drop-shadow(0 0 4px currentColor);
  }

  .node-title {
    font-size: 14px;
    font-weight: 600;
    color: #e8f4f8;
  }
}

.panel-body {
  padding: 12px 16px;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 8px;
  gap: 8px;

  .label {
    flex-shrink: 0;
    width: 56px;
    font-size: 11px;
    color: rgba(232, 244, 248, 0.5);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .value {
    flex: 1;
    font-size: 12px;
    color: #e8f4f8;
    word-break: break-all;

    &.description {
      line-height: 1.5;
      color: rgba(232, 244, 248, 0.75);
    }
  }
}

.panel-connections {
  padding: 12px 16px;
  border-top: 1px solid rgba(0, 240, 255, 0.1);
  background: rgba(0, 0, 0, 0.2);
}

.connections-title {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;

  .title-text {
    font-size: 11px;
    color: rgba(232, 244, 248, 0.5);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .count {
    font-size: 11px;
    color: rgba(232, 244, 248, 0.3);
  }
}

.connection-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 0;
  font-size: 11px;

  .conn-type {
    font-size: 10px;
    font-weight: 600;
    text-transform: uppercase;
  }

  .conn-arrow {
    color: rgba(232, 244, 248, 0.3);
  }

  .conn-target {
    color: #e8f4f8;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.more-connections {
  font-size: 11px;
  color: rgba(232, 244, 248, 0.4);
  padding-top: 4px;
}
</style>
