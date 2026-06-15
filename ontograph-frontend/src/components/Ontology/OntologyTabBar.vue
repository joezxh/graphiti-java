/**
 * 本体管理控制台 — Navicat风格多标签页 TabBar
 */
<template>
  <div class="ontology-tabbar">
    <div class="tabbar-scroll">
      <div class="tabbar-tabs">
        <div
          v-for="tab in tabs"
          :key="tab.id"
          class="tab-item"
          :class="{ active: tab.id === activeTabId, dirty: tab.dirty }"
          @click="handleTabClick(tab.id)"
          @dblclick="handleTabDblClick(tab.id)"
          @contextmenu.prevent="handleContextMenu($event, tab.id)"
        >
          <span class="tab-icon">{{ getTabIcon(tab.type) }}</span>
          <span class="tab-title">{{ tab.title }}</span>
          <span v-if="tab.dirty" class="tab-dirty-dot" />
          <span class="tab-close" @click.stop="handleCloseTab(tab.id)">×</span>
        </div>
        <div class="tab-add-btn" @click="emit('add-tab')">
          <PlusOutlined />
        </div>
      </div>
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="contextMenu.visible"
      class="ctx-menu"
      :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
      @click.stop
    >
      <a-menu @click="handleContextMenuAction">
        <a-menu-item key="close">{{ $t('ontology.closeCurrent') }}</a-menu-item>
        <a-menu-item key="close-others">{{ $t('ontology.closeOthers') }}</a-menu-item>
        <a-menu-item key="close-right">{{ $t('ontology.closeRight') }}</a-menu-item>
        <a-menu-item key="close-all">{{ $t('ontology.closeAll') }}</a-menu-item>
      </a-menu>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted, onBeforeUnmount } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { OntologyTab } from '@/store/modules/ontology'
import type { OntologyTabType } from '@/store/modules/ontology'

defineProps<{
  tabs: OntologyTab[]
  activeTabId: string | null
}>()

const emit = defineEmits<{
  (e: 'update:activeTabId', id: string): void
  (e: 'close-tab', id: string): void
  (e: 'close-other-tabs', keepId: string): void
  (e: 'close-tabs-to-right', id: string): void
  (e: 'close-all-tabs'): void
  (e: 'add-tab'): void
}>()

const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  targetTabId: ''
})

function getTabIcon(type: OntologyTabType): string {
  const iconMap: Record<string, string> = {
    'class-list': '📄',
    'class-editor': '◉',
    'property-list': '◆',
    'property-editor': '◆',
    'constraint-list': '◇',
    'instance-table': '◈',
    'version-history': '📋',
    'version-diff': '⚖',
    'consistency-check': '✅',
    'batch-validation': '🔍',
    'ontology-graph': '📊'
  }
  return iconMap[type] ?? '📄'
}

function handleTabClick(id: string) {
  emit('update:activeTabId', id)
}

function handleTabDblClick(id: string) {
  emit('close-tab', id)
}

function handleCloseTab(id: string) {
  emit('close-tab', id)
}

function handleContextMenu(e: MouseEvent, tabId: string) {
  contextMenu.targetTabId = tabId
  contextMenu.x = e.clientX
  contextMenu.y = e.clientY
  contextMenu.visible = true
}

function handleContextMenuAction({ key }: { key: string }) {
  contextMenu.visible = false
  const id = contextMenu.targetTabId
  switch (key) {
    case 'close': emit('close-tab', id); break
    case 'close-others': emit('close-other-tabs', id); break
    case 'close-right': emit('close-tabs-to-right', id); break
    case 'close-all': emit('close-all-tabs'); break
  }
}

function handleClickOutside(_e: MouseEvent) {
  if (contextMenu.visible) {
    contextMenu.visible = false
  }
}

onMounted(() => document.addEventListener('click', handleClickOutside))
onBeforeUnmount(() => document.removeEventListener('click', handleClickOutside))
</script>

<style scoped lang="less">
.ontology-tabbar {
  height: 38px;
  background: #161b22;
  border-bottom: 1px solid #30363d;
  display: flex;
  align-items: stretch;
  flex-shrink: 0;
  user-select: none;

  .tabbar-scroll {
    flex: 1;
    overflow-x: auto;
    overflow-y: hidden;
    scrollbar-width: none;

    &::-webkit-scrollbar { display: none; }
  }

  .tabbar-tabs {
    display: flex;
    align-items: stretch;
    height: 100%;
    white-space: nowrap;
  }

  .tab-item {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 0 12px;
    border-right: 1px solid #30363d;
    cursor: pointer;
    font-size: 13px;
    color: #8b949e;
    transition: color 0.15s, background 0.15s;
    position: relative;
    max-width: 200px;
    min-width: 80px;

    &:hover {
      color: #e6edf3;
      background: #21262d;
    }

    &.active {
      color: #e6edf3;
      background: #0d1117;
      border-bottom: 2px solid #58a6ff;
      padding-bottom: 1px;

      .tab-icon { color: #58a6ff; }
    }

    &.dirty .tab-title {
      font-style: italic;
    }

    .tab-icon { font-size: 12px; flex-shrink: 0; }
    .tab-title {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .tab-dirty-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #f0883e;
      flex-shrink: 0;
    }
    .tab-close {
      font-size: 16px;
      line-height: 1;
      color: #6e7681;
      flex-shrink: 0;
      padding: 2px 4px;
      border-radius: 3px;
      transition: all 0.15s;

      &:hover {
        background: rgba(240, 136, 62, 0.3);
        color: #f0883e;
      }
    }
  }

  .tab-add-btn {
    display: flex;
    align-items: center;
    padding: 0 12px;
    color: #6e7681;
    cursor: pointer;
    transition: color 0.15s;

    &:hover { color: #e6edf3; }
  }
}
</style>

<style lang="less">
.ctx-menu {
  position: fixed;
  z-index: 9999;

  .ant-menu {
    background: #161b22;
    border: 1px solid #30363d;
    border-radius: 8px;
    padding: 4px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
  }

  .ant-menu-item {
    color: #e6edf3;
    font-size: 13px;
    border-radius: 4px;
    margin: 2px 0;
    height: 32px;
    line-height: 32px;

    &:hover { background: #21262d; }
  }
}
</style>
