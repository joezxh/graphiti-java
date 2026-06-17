<template>
  <aside class="graphiti-sidebar">
    <div class="sidebar-header">
      <AppstoreOutlined class="sidebar-icon" />
      <span class="sidebar-title">{{ $t('nav.navigation') }}</span>
    </div>

    <!-- 动态菜单 -->
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

    <!-- 动态菜单分组 -->
    <div v-for="section in menuSections" :key="String(section.id)" class="menu-section">
      <div class="menu-section-title" @click="toggleSection(String(section.id))">
        <component :is="getIcon(section.icon)" class="menu-section-icon" />
        <span class="menu-section-text">{{ section.name }}</span>
        <DownOutlined :class="['menu-section-arrow', { collapsed: !openSections.includes(String(section.id)) }]" />
      </div>
      <div v-show="openSections.includes(String(section.id))" class="menu-section-content">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="inline"
          class="sidebar-menu nested-menu"
          @click="handleMenuClick"
        >
          <template v-for="menu in section.children" :key="menu.path">
            <a-menu-item v-if="!menu.children?.length" :key="menu.path">
              <template #icon>
                <component :is="getIcon(menu.icon)" />
              </template>
              {{ menu.name }}
            </a-menu-item>
            <!-- 子菜单分组 -->
            <a-sub-menu v-else :key="menu.path">
              <template #title>
                <span>
                  <component :is="getIcon(menu.icon)" />
                  <span>{{ menu.name }}</span>
                </span>
              </template>
              <a-menu-item v-for="child in menu.children" :key="child.path">
                <template #icon>
                  <component :is="getIcon(child.icon)" />
                </template>
                {{ child.name }}
              </a-menu-item>
            </a-sub-menu>
          </template>
        </a-menu>
      </div>
    </div>

    <!-- 备用: 静态菜单 (当动态菜单未加载时显示) -->
    <template v-if="!hasDynamicMenus">
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
    </template>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed, watch, markRaw } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePermissionStore } from '@/store/modules/permission'
import type { MenuItem } from '@/api/menu'
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
const permissionStore = usePermissionStore()

const selectedKeys = ref<string[]>([])
const openSections = ref<string[]>(['graph-management', 'data-management', 'tools', 'system-management'])

// 图标映射
const iconMap: Record<string, any> = {
  DashboardOutlined: markRaw(DashboardOutlined),
  ShareAltOutlined: markRaw(ShareAltOutlined),
  DatabaseOutlined: markRaw(DatabaseOutlined),
  SearchOutlined: markRaw(SearchOutlined),
  AppstoreOutlined: markRaw(AppstoreOutlined),
  UnorderedListOutlined: markRaw(UnorderedListOutlined),
  ImportOutlined: markRaw(ImportOutlined),
  ExportOutlined: markRaw(ExportOutlined),
  NodeIndexOutlined: markRaw(NodeIndexOutlined),
  MonitorOutlined: markRaw(MonitorOutlined),
  SettingOutlined: markRaw(SettingOutlined),
  UserOutlined: markRaw(UserOutlined),
  TeamOutlined: markRaw(TeamOutlined),
  MenuOutlined: markRaw(MenuOutlined),
  ToolOutlined: markRaw(ToolOutlined),
  FileTextOutlined: markRaw(FileTextOutlined),
  HistoryOutlined: markRaw(HistoryOutlined),
  ClusterOutlined: markRaw(ClusterOutlined),
  LinkOutlined: markRaw(LinkOutlined),
  EditOutlined: markRaw(EditOutlined),
  AuditOutlined: markRaw(AuditOutlined),
  MessageOutlined: markRaw(MessageOutlined),
  CodeOutlined: markRaw(CodeOutlined),
  TagOutlined: markRaw(TagOutlined),
  SafetyCertificateOutlined: markRaw(SafetyCertificateOutlined)
}

// 获取图标组件
const getIcon = (iconName?: string) => {
  if (!iconName) return DashboardOutlined
  return iconMap[iconName] || AppstoreOutlined
}

// 判断是否有动态菜单
const hasDynamicMenus = computed(() => {
  return permissionStore.menuList.length > 0
})

// 将后端菜单树转换为侧边栏分组
const menuSections = computed(() => {
  const menuList = permissionStore.menuList

  // 按 parentId 分组
  const sections: Map<string | number, MenuItem[]> = new Map()
  const rootMenus: MenuItem[] = []

  for (const menu of menuList) {
    if (menu.parentId === 0 || menu.parentId === null) {
      rootMenus.push(menu)
    } else {
      const existing = sections.get(menu.parentId) || []
      existing.push(menu)
      sections.set(menu.parentId, existing)
    }
  }

  // 递归附加子菜单
  const attachChildren = (menus: MenuItem[]): MenuItem[] => {
    return menus.map(menu => ({
      ...menu,
      children: sections.get(menu.id) ? attachChildren(sections.get(menu.id)!) : undefined
    }))
  }

  return attachChildren(rootMenus).sort((a, b) => a.sort - b.sort)
})

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
