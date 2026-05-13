<template>
  <aside class="graphiti-sidebar">
    <div class="sidebar-header">
      <AppstoreOutlined class="sidebar-icon" />
      <span class="sidebar-title">导航菜单</span>
    </div>

    <a-menu
      v-model:selectedKeys="selectedKeys"
      v-model:openKeys="openKeys"
      mode="inline"
      class="sidebar-menu"
      @click="handleMenuClick"
    >
      <a-menu-item key="/dashboard">
        <template #icon>
          <DashboardOutlined />
        </template>
        仪表盘
      </a-menu-item>

      <a-sub-menu key="graph-management">
        <template #icon>
          <ShareAltOutlined />
        </template>
        <template #title>图谱管理</template>
        <a-menu-item key="/graph/list">
          <template #icon>
            <UnorderedListOutlined />
          </template>
          图谱列表
        </a-menu-item>
        <a-menu-item key="/graph/temporal">
          <template #icon>
            <HistoryOutlined />
          </template>
          时序历史
        </a-menu-item>
        <a-menu-item key="/communities">
          <template #icon>
            <ClusterOutlined />
          </template>
          社区检测
        </a-menu-item>
        <a-menu-item key="/ontology">
          <template #icon>
            <ApartmentOutlined />
          </template>
          本体配置
        </a-menu-item>
      </a-sub-menu>

      <a-sub-menu key="data-management">
        <template #icon>
          <DatabaseOutlined />
        </template>
        <template #title>数据管理</template>
        <a-menu-item key="/data/import">
          <template #icon>
            <ImportOutlined />
          </template>
          数据导入
        </a-menu-item>
        <a-menu-item key="/data/export">
          <template #icon>
            <ExportOutlined />
          </template>
          数据导出
        </a-menu-item>
        <a-menu-item key="/data/entities">
          <template #icon>
            <NodeIndexOutlined />
          </template>
          实体管理
        </a-menu-item>
        <a-menu-item key="/edges">
          <template #icon>
            <LinkOutlined />
          </template>
          边管理
        </a-menu-item>
        <a-menu-item key="/episodes">
          <template #icon>
            <FileTextOutlined />
          </template>
          Episode 管理
        </a-menu-item>
      </a-sub-menu>

      <a-sub-menu key="tools">
        <template #icon>
          <ToolOutlined />
        </template>
        <template #title>工具</template>
        <a-menu-item key="/search">
          <template #icon>
            <SearchOutlined />
          </template>
          混合检索
        </a-menu-item>
        <a-menu-item key="/custom-instructions">
          <template #icon>
            <EditOutlined />
          </template>
          自定义指令
        </a-menu-item>
        <a-menu-item key="/prompt">
          <template #icon>
            <MessageOutlined />
          </template>
          提示词管理
        </a-menu-item>
        <a-menu-item key="/legal-kg">
          <template #icon>
            <AuditOutlined />
          </template>
          法律知识图谱
        </a-menu-item>
      </a-sub-menu>

      <a-sub-menu key="system-management">
        <template #icon>
          <SettingOutlined />
        </template>
        <template #title>系统管理</template>
        <a-menu-item key="/system/user">
          <template #icon>
            <UserOutlined />
          </template>
          用户管理
        </a-menu-item>
        <a-menu-item key="/system/role">
          <template #icon>
            <TeamOutlined />
          </template>
          角色管理
        </a-menu-item>
        <a-menu-item key="/system/menu">
          <template #icon>
            <MenuOutlined />
          </template>
          菜单管理
        </a-menu-item>
        <a-menu-item key="/system/config">
          <template #icon>
            <ToolOutlined />
          </template>
          系统配置
        </a-menu-item>
        <a-menu-item key="/system/log">
          <template #icon>
            <FileTextOutlined />
          </template>
          操作日志
        </a-menu-item>
        <a-menu-item key="/monitor">
          <template #icon>
            <MonitorOutlined />
          </template>
          系统监控
        </a-menu-item>
      </a-sub-menu>
    </a-menu>
  </aside>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  DashboardOutlined,
  ShareAltOutlined,
  ApartmentOutlined,
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
  // SafetyOutlined, // 未使用
  AuditOutlined,
  MessageOutlined
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()

const selectedKeys = ref<string[]>([])
const openKeys = ref<string[]>(['graph-management', 'data-management', 'tools'])

const updateMenuState = () => {
  const path = route.path
  
  // 直接根据路径设置 selectedKeys
  selectedKeys.value = [path]
  
  // 根据路径展开对应的子菜单
  if (path.includes('/graph') && !openKeys.value.includes('graph-management')) {
    openKeys.value = [...openKeys.value, 'graph-management']
  }
  if (path.includes('/data') && !openKeys.value.includes('data-management')) {
    openKeys.value = [...openKeys.value, 'data-management']
  }
  if (path.includes('/search') || path.includes('/custom-instructions') || path.includes('/prompt')) {
    if (!openKeys.value.includes('tools')) {
      openKeys.value = [...openKeys.value, 'tools']
    }
  }
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
  flex: 1;
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
  
  :deep(.ant-menu-submenu-title) {
    color: #a4aab8;
    margin: 4px 8px;
    border-radius: 6px;
    
    &:hover {
      color: #eceff6 !important;
      background: rgba(94, 106, 210, 0.1) !important;
    }
  }
  
  :deep(.ant-menu-sub) {
    background: transparent !important;
    
    .ant-menu-item {
      padding-left: 48px !important;
      
      .ant-menu-item-icon {
        margin-right: 8px;
      }
    }
  }
  
  :deep(.ant-menu-item-icon) {
    color: inherit;
  }
}
</style>
