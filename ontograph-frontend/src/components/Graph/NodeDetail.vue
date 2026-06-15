<template>
  <div class="node-detail-panel" v-if="visible">
    <div class="panel-header">
      <span class="panel-title">{{ t('graphIde.panelNodeDetail') }}</span>
      <a-button type="text" size="small" @click="handleClose">
        <template #icon><CloseOutlined /></template>
      </a-button>
    </div>

    <div class="panel-content" v-if="nodeData">
      <!-- 节点基本信息 -->
      <div class="info-section">
        <div class="info-row">
          <span class="info-label">UUID</span>
          <span class="info-value uuid">{{ nodeData.uuid }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">{{ t('graphIde.labelName') }}</span>
          <span class="info-value">{{ nodeData.name || t('common.unknown') }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">{{ t('graphIde.labelType') }}</span>
          <a-tag :color="getNodeTypeColor(nodeData.type)">
            {{ nodeData.type }}
          </a-tag>
        </div>
      </div>

      <!-- 属性列表 -->
      <div class="info-section" v-if="nodeData.properties && Object.keys(nodeData.properties).length > 0">
        <div class="section-title">{{ t('graphIde.sectionProperties') }}</div>
        <div class="properties-list">
          <div
            class="property-item"
            v-for="(value, key) in nodeData.properties"
            :key="key"
          >
            <span class="property-key">{{ key }}</span>
            <span class="property-value">{{ formatValue(value) }}</span>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="info-section">
        <div class="section-title">{{ t('common.actions') }}</div>
        <div class="action-buttons">
          <a-button size="small" @click="handleViewEdges">
            <template #icon><ApiOutlined /></template>
            {{ t('graphIde.viewEdges') }}
          </a-button>
          <a-button size="small" @click="handleViewEpisodes">
            <template #icon><HistoryOutlined /></template>
            {{ t('graphIde.viewEvents') }}
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { CloseOutlined, ApiOutlined, HistoryOutlined } from '@ant-design/icons-vue'
import type { BackendNode } from '@/utils/graph'

const { t } = useI18n()

interface Props {
  visible: boolean
  nodeData: BackendNode | null
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  nodeData: null
})

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'view-edges', nodeData: BackendNode): void
  (e: 'view-episodes', nodeData: BackendNode): void
}>()

const handleClose = () => {
  emit('close')
}

const handleViewEdges = () => {
  if (props.nodeData) {
    emit('view-edges', props.nodeData)
  }
}

const handleViewEpisodes = () => {
  if (props.nodeData) {
    emit('view-episodes', props.nodeData)
  }
}

const getNodeTypeColor = (type: string | null | undefined): string => {
  if (!type) return '#8a8f98'
  const lowerType = type.toLowerCase()
  if (lowerType.includes('entity')) return '#5e6ad2'
  if (lowerType.includes('episode')) return '#00d4ff'
  if (lowerType.includes('event')) return '#ff8c00'
  return '#8a8f98'
}

const formatValue = (value: any): string => {
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}
</script>

<style scoped lang="less">
.node-detail-panel {
  position: absolute;
  top: 56px;
  right: 0;
  width: 320px;
  height: calc(100% - 56px);
  background: rgba(15, 16, 17, 0.95);
  border-left: 1px solid #23252a;
  display: flex;
  flex-direction: column;
  z-index: 100;
  backdrop-filter: blur(10px);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #23252a;
  
  .panel-title {
    font-size: 14px;
    font-weight: 600;
    color: #f7f8f8;
  }
  
  :deep(.ant-btn-text) {
    color: #8a8f98;
    
    &:hover {
      color: #f7f8f8;
    }
  }
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  
  .info-section {
    margin-bottom: 20px;
    
    .section-title {
      font-size: 12px;
      font-weight: 600;
      color: #8a8f98;
      margin-bottom: 8px;
      text-transform: uppercase;
    }
    
    .info-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      
      .info-label {
        font-size: 12px;
        color: #8a8f98;
      }
      
      .info-value {
        font-size: 12px;
        color: #f7f8f8;
        
        &.uuid {
          font-family: 'Courier New', monospace;
          font-size: 11px;
          max-width: 180px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }
    
    .properties-list {
      .property-item {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        padding: 6px 0;
        border-bottom: 1px solid rgba(94, 106, 210, 0.1);
        
        .property-key {
          font-size: 12px;
          color: #8a8f98;
          flex-shrink: 0;
          margin-right: 8px;
        }
        
        .property-value {
          font-size: 12px;
          color: #f7f8f8;
          text-align: right;
          word-break: break-all;
        }
      }
    }
    
    .action-buttons {
      display: flex;
      gap: 8px;
      
      :deep(.ant-btn) {
        flex: 1;
        background: rgba(94, 106, 210, 0.1);
        border-color: rgba(94, 106, 210, 0.3);
        color: #f7f8f8;
        
        &:hover {
          background: rgba(94, 106, 210, 0.2);
          border-color: #5e6ad2;
        }
      }
    }
  }
}
</style>
