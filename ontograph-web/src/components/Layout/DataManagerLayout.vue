<template>
  <div class="data-manager-layout">
    <!-- Header -->
    <div class="dm-header">
      <div class="dm-header-left">
        <h2 class="dm-title">{{ title }}</h2>
      </div>
      <div class="dm-header-right">
        <slot name="header-actions" />
      </div>
    </div>

    <!-- Main Content -->
    <div class="dm-main">
      <!-- Left Panel (optional) -->
      <div v-if="$slots['left-panel']" class="dm-left-panel">
        <slot name="left-panel" />
      </div>

      <!-- Center Panel -->
      <div class="dm-center-panel">
        <slot name="main-table" />
      </div>

      <!-- Right Panel (optional) -->
      <div v-if="$slots['right-panel']" class="dm-right-panel" :class="{ collapsed: rightCollapsed }">
        <div class="dm-right-toggle" @click="rightCollapsed = !rightCollapsed">
          <RightOutlined v-if="rightCollapsed" />
          <LeftOutlined v-else />
        </div>
        <div v-show="!rightCollapsed" class="dm-right-content">
          <slot name="right-panel" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { LeftOutlined, RightOutlined } from '@ant-design/icons-vue'

defineProps<{
  title: string
}>()

const rightCollapsed = ref(false)
</script>

<style scoped lang="less">
.data-manager-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #0d1117;
  color: #e6edf3;
}

.dm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #161b22;
  border-bottom: 1px solid #30363d;
  flex-shrink: 0;

  .dm-title {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: #e6edf3;
  }
}

.dm-main {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.dm-left-panel {
  width: 240px;
  background: #161b22;
  border-right: 1px solid #30363d;
  overflow-y: auto;
  flex-shrink: 0;
}

.dm-center-panel {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.dm-right-panel {
  width: 420px;
  background: #161b22;
  border-left: 1px solid #30363d;
  display: flex;
  flex-shrink: 0;
  position: relative;

  &.collapsed {
    width: 32px;
  }

  .dm-right-toggle {
    width: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #8b949e;
    border-right: 1px solid #30363d;

    &:hover {
      color: #e6edf3;
      background: #21262d;
    }
  }

  .dm-right-content {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
  }
}
</style>
