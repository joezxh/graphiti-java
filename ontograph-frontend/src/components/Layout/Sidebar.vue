<template>
  <aside class="graphiti-sidebar">
    <div class="sidebar-header">
      <AppstoreOutlined class="sidebar-icon" />
      <span class="sidebar-title">{{ $t('nav.navigation') }}</span>
    </div>

    <!-- 动态菜单分组 -->
    <div v-for="section in menuSections" :key="String(section.id)" class="menu-section">
      <div class="menu-section-title" @click="toggleSection(String(section.id))">
        <component :is="getIcon(section.icon)" class="menu-section-icon" />
        <span class="menu-section-text">{{ translateMenuName(section.name) }}</span>
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
            <!-- type=2 或无子菜单：直接渲染为菜单项 -->
            <a-menu-item v-if="menu.type === 2 || !menu.children?.length" :key="menu.path">
              <template #icon>
                <component :is="getIcon(menu.icon)" />
              </template>
              {{ translateMenuName(menu.name) }}
            </a-menu-item>
            <!-- type=1 且有子菜单：渲染为子菜单分组 -->
            <a-sub-menu v-else :key="menu.path">
              <template #title>
                <span>
                  <component :is="getIcon(menu.icon)" />
                  <span>{{ translateMenuName(menu.name) }}</span>
                </span>
              </template>
              <a-menu-item v-for="child in menu.children" :key="child.path">
                <template #icon>
                  <component :is="getIcon(child.icon)" />
                </template>
                {{ translateMenuName(child.name) }}
              </a-menu-item>
            </a-sub-menu>
          </template>
        </a-menu>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed, watch, markRaw } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { usePermissionStore } from '@/store/modules/permission'
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
const { t, te } = useI18n()
const permissionStore = usePermissionStore()

const selectedKeys = ref<string[]>([])

// 初始展开所有已有的菜单分组
const openSections = ref<string[]>([])

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

// 翻译菜单名称：如果 name 是 i18n key 则翻译，否则原样显示
const translateMenuName = (name: string): string => {
  if (name && te(name)) {
    return t(name)
  }
  return name || ''
}

// 使用后端已构建好的树结构，直接使用根级菜单作为分组
const menuSections = computed(() => {
  const menuList = permissionStore.menuList
  // 后端 buildMenuTree 已返回嵌套树结构，根级 items 即为分组
  // 确保每个分组都有 children
  const result = menuList
    .filter(m => m.type === 1 || m.children?.length) // 仅显示目录类型或有子菜单的
    .map(menu => ({
      ...menu,
      children: menu.children || []
    }))
    .sort((a, b) => a.sort - b.sort)

  // 首次加载时默认展开所有分组
  if (openSections.value.length === 0 && result.length > 0) {
    openSections.value = result.map(s => String(s.id))
  }

  return result
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
