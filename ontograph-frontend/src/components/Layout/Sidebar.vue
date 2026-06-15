<template>
  <aside class="graphiti-sidebar">
    <div class="sidebar-header">
      <AppstoreOutlined class="sidebar-icon" />
      <span class="sidebar-title">{{ $t('nav.navigation') }}</span>
    </div>

    <a-menu
      v-model:selectedKeys="selectedKeys"
      mode="inline"
      class="sidebar-menu"
      @click="handleMenuClick"
    >
      <a-menu-item key="/dashboard">
        <template #icon>
          <DashboardOutlined />
        </template>
        {{ $t('nav.dashboard') }}
      </a-menu-item>
    </a-menu>

    <!-- graph-management section -->
    <div class="menu-section">
      <div class="menu-section-title" @click="toggleSection('graph-management')">
        <ShareAltOutlined class="menu-section-icon" />
        <span class="menu-section-text">{{ $t('nav.graphManagement') }}</span>
        <DownOutlined :class="['menu-section-arrow', { collapsed: !openSections.includes('graph-management') }]" />
      </div>
      <div v-show="openSections.includes('graph-management')" class="menu-section-content">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="inline"
          class="sidebar-menu nested-menu"
          @click="handleMenuClick"
        >
          <a-menu-item key="/graph/list">
            <template #icon>
              <UnorderedListOutlined />
            </template>
            {{ $t('nav.graphList') }}
          </a-menu-item>
          <a-menu-item key="/graph/ide">
            <template #icon>
              <CodeOutlined />
            </template>
            {{ $t('nav.graphIDE') }}
          </a-menu-item>
          <a-menu-item key="/graph/temporal">
            <template #icon>
              <HistoryOutlined />
            </template>
            {{ $t('nav.temporalHistory') }}
          </a-menu-item>
          <a-menu-item key="/data/communities">
            <template #icon>
              <ClusterOutlined />
            </template>
            {{ $t('nav.communityDetection') }}
          </a-menu-item>
        </a-menu>
      </div>
    </div>

    <!-- data-management section -->
    <div class="menu-section">
      <div class="menu-section-title" @click="toggleSection('data-management')">
        <DatabaseOutlined class="menu-section-icon" />
        <span class="menu-section-text">{{ $t('nav.dataManagement') }}</span>
        <DownOutlined :class="['menu-section-arrow', { collapsed: !openSections.includes('data-management') }]" />
      </div>
      <div v-show="openSections.includes('data-management')" class="menu-section-content">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="inline"
          class="sidebar-menu nested-menu"
          @click="handleMenuClick"
        >
          <a-menu-item key="/data/classes">
            <template #icon>
              <AppstoreOutlined />
            </template>
            {{ $t('nav.classManagement') }}
          </a-menu-item>
          <a-menu-item key="/data/properties">
            <template #icon>
              <TagOutlined />
            </template>
            {{ $t('nav.propertyManagement') }}
          </a-menu-item>
          <a-menu-item key="/data/constraints">
            <template #icon>
              <SafetyCertificateOutlined />
            </template>
            {{ $t('nav.constraintManagement') }}
          </a-menu-item>
          <a-menu-item key="/data/entities">
            <template #icon>
              <NodeIndexOutlined />
            </template>
            {{ $t('nav.entityManagement') }}
          </a-menu-item>
          <a-menu-item key="/data/edges">
            <template #icon>
              <LinkOutlined />
            </template>
            {{ $t('nav.edgeManagement') }}
          </a-menu-item>
          <a-menu-item key="/data/community-episode">
            <template #icon>
              <ClusterOutlined />
            </template>
            {{ $t('nav.communityEpisodeManagement') }}
          </a-menu-item>
          <a-menu-item key="/data/episodes">
            <template #icon>
              <FileTextOutlined />
            </template>
            {{ $t('nav.episodeManagement') }}
          </a-menu-item>
          <a-menu-item key="/data/import">
            <template #icon>
              <ImportOutlined />
            </template>
            {{ $t('nav.dataImport') }}
          </a-menu-item>
          <a-menu-item key="/data/export">
            <template #icon>
              <ExportOutlined />
            </template>
            {{ $t('nav.dataExport') }}
          </a-menu-item>
          <a-menu-item key="/legal-kg">
            <template #icon>
              <AuditOutlined />
            </template>
            {{ $t('nav.legalKnowledgeGraph') }}
          </a-menu-item>
        </a-menu>
      </div>
    </div>

    <!-- tools section -->
    <div class="menu-section">
      <div class="menu-section-title" @click="toggleSection('tools')">
        <ToolOutlined class="menu-section-icon" />
        <span class="menu-section-text">{{ $t('nav.tools') }}</span>
        <DownOutlined :class="['menu-section-arrow', { collapsed: !openSections.includes('tools') }]" />
      </div>
      <div v-show="openSections.includes('tools')" class="menu-section-content">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="inline"
          class="sidebar-menu nested-menu"
          @click="handleMenuClick"
        >
          <a-menu-item key="/search">
            <template #icon>
              <SearchOutlined />
            </template>
            {{ $t('nav.hybridSearch') }}
          </a-menu-item>
          <a-menu-item key="/custom-instructions">
            <template #icon>
              <EditOutlined />
            </template>
            {{ $t('nav.customInstructions') }}
          </a-menu-item>
          <a-menu-item key="/prompt">
            <template #icon>
              <MessageOutlined />
            </template>
            {{ $t('nav.promptManagement') }}
          </a-menu-item>
        </a-menu>
      </div>
    </div>

    <!-- system-management section -->
    <div class="menu-section">
      <div class="menu-section-title" @click="toggleSection('system-management')">
        <SettingOutlined class="menu-section-icon" />
        <span class="menu-section-text">{{ $t('nav.systemManagement') }}</span>
        <DownOutlined :class="['menu-section-arrow', { collapsed: !openSections.includes('system-management') }]" />
      </div>
      <div v-show="openSections.includes('system-management')" class="menu-section-content">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="inline"
          class="sidebar-menu nested-menu"
          @click="handleMenuClick"
        >
          <a-menu-item key="/system/user">
            <template #icon>
              <UserOutlined />
            </template>
            {{ $t('nav.userManagement') }}
          </a-menu-item>
          <a-menu-item key="/system/role">
            <template #icon>
              <TeamOutlined />
            </template>
            {{ $t('nav.roleManagement') }}
          </a-menu-item>
          <a-menu-item key="/system/menu">
            <template #icon>
              <MenuOutlined />
            </template>
            {{ $t('nav.menuManagement') }}
          </a-menu-item>
          <a-menu-item key="/system/config">
            <template #icon>
              <ToolOutlined />
            </template>
            {{ $t('nav.systemConfig') }}
          </a-menu-item>
          <a-menu-item key="/system/log">
            <template #icon>
              <FileTextOutlined />
            </template>
            {{ $t('nav.operationLog') }}
          </a-menu-item>
          <a-menu-item key="/monitor">
            <template #icon>
              <MonitorOutlined />
            </template>
            {{ $t('nav.systemMonitor') }}
          </a-menu-item>
        </a-menu>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  DashboardOutlined,
  ShareAltOutlined,
  DatabaseOutlined,
  SearchOutlined,
  AppstoreOutlined,
  UnorderedListOutlined,
  ImportOutlined,
  ExportOutlined,
  NodeIndexOutlined,
  MonitorOutlined,
  SettingOutlined,
  UserOutlined,
  TeamOutlined,
  MenuOutlined,
  ToolOutlined,
  FileTextOutlined,
  HistoryOutlined,
  ClusterOutlined,
  LinkOutlined,
  EditOutlined,
  AuditOutlined,
  MessageOutlined,
  CodeOutlined,
  TagOutlined,
  SafetyCertificateOutlined,
  DownOutlined
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()

const selectedKeys = ref<string[]>([])
const openSections = ref<string[]>(['graph-management', 'data-management', 'tools', 'system-management'])

const toggleSection = (sectionKey: string) => {
  const index = openSections.value.indexOf(sectionKey)
  if (index > -1) {
    openSections.value.splice(index, 1)
  } else {
    openSections.value.push(sectionKey)
  }
}

const updateMenuState = () => {
  const path = route.path
  selectedKeys.value = [path]
}

watch(() => route.path, updateMenuState, { immediate: true })

const handleMenuClick = ({ key }: { key: string }) => {
  router.push(key)
}
</script>

<style scoped lang="less">
.graphiti-sidebar {
  width: 240px;
  height: 100%;
  background: @bg-sidebar;
  border-right: 1px solid @border-color;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.sidebar-header {
  height: 48px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(94, 106, 210, 0.15);
  color: #8b92a8;
  font-size: 13px;
  font-weight: 600;
}

.sidebar-icon {
  font-size: 14px;
}

.sidebar-title {
  font-size: 13px;
}

.sidebar-menu {
  flex: none;
  border-right: none;
  background: transparent !important;
  
  :deep(.ant-menu-item) {
    color: #a4aab8;
    margin: 4px 8px;
    border-radius: 6px;
    
    &:hover {
      color: #eceff6 !important;
      background: rgba(94, 106, 210, 0.1) !important;
    }
    
    &.ant-menu-item-selected {
      color: #eceff6 !important;
      background: rgba(94, 106, 210, 0.2) !important;
      border-right: 2px solid #5e6ad2;
    }
  }
}

.nested-menu {
  padding: 4px 0;
}

.menu-section {
  margin: 4px 0;
}

.menu-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  height: 40px;
  color: #a4aab8;
  cursor: pointer;
  border-radius: 6px;
  margin: 0 8px;
  transition: all 0.2s;
  
  &:hover {
    color: #eceff6;
    background: rgba(94, 106, 210, 0.1);
  }
}

.menu-section-icon {
  font-size: 14px;
  flex-shrink: 0;
}

.menu-section-text {
  flex: 1;
  font-size: 14px;
}

.menu-section-arrow {
  font-size: 12px;
  transition: transform 0.2s;
  flex-shrink: 0;
  
  &.collapsed {
    transform: rotate(-90deg);
  }
}

.menu-section-content {
  :deep(.ant-menu-item) {
    padding-left: 48px !important;
    
    .anticon {
      margin-right: 8px;
    }
  }
}
</style>
