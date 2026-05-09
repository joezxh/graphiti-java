<template>
  <div class="graph-toolbar">
    <div class="toolbar-left">
      <a-tooltip title="缩放至适配">
        <a-button type="text" size="small" @click="handleZoomToFit">
          <template #icon><ExpandOutlined /></template>
        </a-button>
      </a-tooltip>
      
      <a-tooltip title="放大">
        <a-button type="text" size="small" @click="handleZoomIn">
          <template #icon><ZoomInOutlined /></template>
        </a-button>
      </a-tooltip>
      
      <a-tooltip title="缩小">
        <a-button type="text" size="small" @click="handleZoomOut">
          <template #icon><ZoomOutOutlined /></template>
        </a-button>
      </a-tooltip>
      
      <a-divider type="vertical" />
      
      <a-tooltip title="显示标签">
        <a-button 
          type="text" 
          size="small" 
          :class="{ active: showLabels }"
          @click="toggleLabels"
        >
          <template #icon><FontSizeOutlined /></template>
        </a-button>
      </a-tooltip>
      
      <a-tooltip title="布局切换">
        <a-dropdown :trigger="['click']">
          <a-button type="text" size="small">
            <template #icon><AppstoreOutlined /></template>
          </a-button>
          <template #overlay>
            <a-menu @click="handleLayoutChange">
              <a-menu-item key="force">力导向布局</a-menu-item>
              <a-menu-item key="circular">环形布局</a-menu-item>
              <a-menu-item key="tree">树形布局</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </a-tooltip>
    </div>
    
    <div class="toolbar-right">
      <a-tooltip title="刷新">
        <a-button type="text" size="small" @click="handleRefresh">
          <template #icon><ReloadOutlined /></template>
        </a-button>
      </a-tooltip>
      
      <a-tooltip title="全屏">
        <a-button type="text" size="small" @click="handleFullscreen">
          <template #icon><FullscreenOutlined /></template>
        </a-button>
      </a-tooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  ExpandOutlined,
  ZoomInOutlined,
  ZoomOutOutlined,
  FontSizeOutlined,
  AppstoreOutlined,
  ReloadOutlined,
  FullscreenOutlined
} from '@ant-design/icons-vue'

interface Props {
  showLabels?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showLabels: true
})

const emit = defineEmits<{
  (e: 'update:showLabels', value: boolean): void
  (e: 'layout-change', layout: string): void
  (e: 'refresh'): void
  (e: 'zoom-to-fit'): void
  (e: 'zoom-in'): void
  (e: 'zoom-out'): void
  (e: 'fullscreen'): void
}>()

const toggleLabels = () => {
  emit('update:showLabels', !props.showLabels)
}

const handleLayoutChange = ({ key }: { key: string }) => {
  emit('layout-change', key)
}

const handleRefresh = () => {
  emit('refresh')
}

const handleZoomToFit = () => {
  emit('zoom-to-fit')
}

const handleZoomIn = () => {
  emit('zoom-in')
}

const handleZoomOut = () => {
  emit('zoom-out')
}

const handleFullscreen = () => {
  emit('fullscreen')
}
</script>

<style scoped lang="less">
.graph-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: rgba(15, 16, 17, 0.8);
  border-bottom: 1px solid #23252a;
  
  .toolbar-left,
  .toolbar-right {
    display: flex;
    align-items: center;
    gap: 4px;
  }
  
  :deep(.ant-btn-text) {
    color: #f7f8f8;  // 浅色图标，提高对比度
    
    &:hover,
    &.active {
      color: #5e6ad2;
      background: rgba(94, 106, 210, 0.1);
    }
  }
  
  :deep(.ant-divider-vertical) {
    border-color: #23252a;
    margin: 0 8px;
  }
}
</style>
