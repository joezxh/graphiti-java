/**
 * 本体工作台 — Navicat风格多标签页工作区
 * 动态加载对应编辑器组件
 */
<template>
  <div class="ontology-workbench">
    <div class="workbench-header">
      <OntologyTabBar
        :tabs="store.openTabs"
        :active-tab-id="store.activeTabId"
        @update:active-tab-id="store.activeTabId = $event"
        @close-tab="store.closeTab"
        @close-other-tabs="store.closeOtherTabs"
        @close-tabs-to-right="store.closeTabsToRight"
        @close-all-tabs="store.closeAllTabs"
        @add-tab="showAddMenu = true"
      />

      <!-- 新建下拉菜单 -->
      <a-dropdown 
        v-model:open="showAddMenu" 
        :trigger="['click']" 
        overlay-class-name="ontology-add-menu"
      >
        <div v-if="!store.activeTabId" class="add-menu-trigger">
          <PlusOutlined /> 新建
        </div>
        <template #overlay>
          <a-menu @click="handleAddMenuClick">
            <a-menu-divider />
            <a-menu-item-group title="定义管理">
              <a-menu-item key="definition-editor">
                <span>📋</span> 本体定义
              </a-menu-item>
              <a-menu-item key="class-editor">
                <span>◉</span> 新建类
              </a-menu-item>
              <a-menu-item key="property-editor">
                <span>◆</span> 新建属性
              </a-menu-item>
              <a-menu-item key="constraint-list">
                <span>◇</span> 新建约束
              </a-menu-item>
              <a-menu-item key="domain-rule-list">
                <span>⚙️</span> 域规则列表
              </a-menu-item>
            </a-menu-item-group>
            <a-menu-divider />
            <a-menu-item-group title="数据管理">
              <a-menu-item key="instance-table">
                <span>◈</span> 实例数据表
              </a-menu-item>
            </a-menu-item-group>
            <a-menu-divider />
            <a-menu-item-group title="工具">
              <a-menu-item key="version-history">
                <span>📋</span> 版本历史
              </a-menu-item>
              <a-menu-item key="consistency-check">
                <span>✅</span> 一致性检查
              </a-menu-item>
              <a-menu-item key="batch-validation">
                <span>🔍</span> 批量验证
              </a-menu-item>
              <a-menu-item key="ontology-graph">
                <span>📊</span> 本体可视化
              </a-menu-item>
            </a-menu-item-group>
          </a-menu>
        </template>
      </a-dropdown>
    </div>

    <!-- 空状态 -->
    <div v-if="!store.activeTab" class="workbench-empty">
      <div class="empty-icon">📦</div>
      <div class="empty-title">本体管理控制台</div>
      <div class="empty-desc">从左侧对象浏览器选择一个节点，或点击右上角「+ 新建」</div>
      <div class="empty-quick-actions">
        <a-button size="small" @click="quickAdd('class-editor', '新建类')">+ 新建类</a-button>
        <a-button size="small" @click="quickAdd('property-editor', '新建属性')">+ 新建属性</a-button>
        <a-button size="small" @click="quickAdd('instance-table', '实例数据')">+ 实例数据</a-button>
      </div>
    </div>

    <!-- 工作区内容 -->
    <div v-else class="workbench-content">
      <ClassEditor
        v-if="store.activeTab.type === 'class-editor'"
        :graph-id="graphId"
        :class-id="store.activeTab.classId"
        @saved="handleSaved"
      />
      <PropertyEditor
        v-else-if="store.activeTab.type === 'property-editor'"
        :graph-id="graphId"
        :property-id="store.activeTab.propertyId"
        @saved="handleSaved"
      />
      <ClassListPanel
        v-else-if="store.activeTab.type === 'class-list'"
        :graph-id="graphId"
        @open-class="openClassEditor"
      />
      <PropertyListPanel
        v-else-if="store.activeTab.type === 'property-list'"
        :graph-id="graphId"
        @open-property="openPropertyEditor"
      />
      <ConstraintListPanel
        v-else-if="store.activeTab.type === 'constraint-list'"
        :graph-id="graphId"
      />
      <DomainRuleListPanel
        v-else-if="store.activeTab.type === 'domain-rule-list'"
        :graph-id="graphId"
      />
      <DefinitionEditor
        v-else-if="store.activeTab.type === 'definition-editor'"
        :graph-id="graphId"
        @saved="handleSaved"
      />
      <InstanceDataTable
        v-else-if="store.activeTab.type === 'instance-table'"
        :graph-id="graphId"
        :class-type="store.activeTab.classType"
        @edit-instance="handleEditInstance"
      />
      <VersionHistoryPanel
        v-else-if="store.activeTab.type === 'version-history'"
        :graph-id="graphId"
      />
      <VersionDiffViewer
        v-else-if="store.activeTab.type === 'version-diff'"
        :graph-id="graphId"
      />
      <ConsistencyCheckPanel
        v-else-if="store.activeTab.type === 'consistency-check'"
        :graph-id="graphId"
      />
      <BatchValidationPanel
        v-else-if="store.activeTab.type === 'batch-validation'"
        :graph-id="graphId"
      />
      <OntologyVisualizer
        v-else-if="store.activeTab.type === 'ontology-graph'"
        :graph-id="graphId"
      />
      <InstanceForm
        v-else-if="store.activeTab.type === 'instance-editor'"
        :graph-id="graphId"
        :instance-data="editingInstance"
        @saved="handleInstanceSaved"
      />
      <div v-else class="panel-placeholder">
        <div class="empty-desc">暂未实现: {{ store.activeTab.type }}</div>
      </div>
    </div>

    <!-- 数据导入导出弹窗 -->
    <DataImportExportModal
      ref="importExportRef"
      :graph-id="graphId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { defineAsyncComponent } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { useOntologyStore, type OntologyTabType } from '@/store/modules/ontology'
import OntologyTabBar from './OntologyTabBar.vue'

const ClassEditor = defineAsyncComponent(() => import('./ClassEditor.vue'))
const PropertyEditor = defineAsyncComponent(() => import('./PropertyEditor.vue'))
const InstanceDataTable = defineAsyncComponent(() => import('./InstanceDataTable.vue'))
const InstanceForm = defineAsyncComponent(() => import('./InstanceForm.vue'))
const VersionHistoryPanel = defineAsyncComponent(() => import('./VersionHistoryPanel.vue'))
const VersionDiffViewer = defineAsyncComponent(() => import('./VersionDiffViewer.vue'))
const ConsistencyCheckPanel = defineAsyncComponent(() => import('./ConsistencyCheckPanel.vue'))
const BatchValidationPanel = defineAsyncComponent(() => import('./BatchValidationPanel.vue'))
const OntologyVisualizer = defineAsyncComponent(() => import('./OntologyVisualizer.vue'))
const ClassListPanel = defineAsyncComponent(() => import('./ClassListPanel.vue'))
const PropertyListPanel = defineAsyncComponent(() => import('./PropertyListPanel.vue'))
const ConstraintListPanel = defineAsyncComponent(() => import('./ConstraintListPanel.vue'))
const DomainRuleListPanel = defineAsyncComponent(() => import('./DomainRuleListPanel.vue'))
const DefinitionEditor = defineAsyncComponent(() => import('./DefinitionEditor.vue'))
const DataImportExportModal = defineAsyncComponent(() => import('./DataImportExportModal.vue'))

const props = defineProps<{ graphId: string; selectedClassId?: number | null }>()

const store = useOntologyStore()
const showAddMenu = ref(false)
const importExportRef = ref()
const editingInstance = ref<any>()

function quickAdd(type: OntologyTabType, title: string) {
  store.openTab({ id: `tab-${type}-${Date.now()}`, type, title })
  showAddMenu.value = false
}

function handleAddMenuClick({ key }: { key: string }) {
  showAddMenu.value = false
  const menuMap: Record<string, { type: OntologyTabType; title: string }> = {
    'definition-editor': { type: 'definition-editor', title: '本体定义' },
    'class-editor': { type: 'class-editor', title: '新建类' },
    'property-editor': { type: 'property-editor', title: '新建属性' },
    'constraint-list': { type: 'constraint-list', title: '约束列表' },
    'domain-rule-list': { type: 'domain-rule-list', title: '域规则列表' },
    'instance-table': { type: 'instance-table', title: '实例数据' },
    'version-history': { type: 'version-history', title: '版本历史' },
    'consistency-check': { type: 'consistency-check', title: '一致性检查' },
    'batch-validation': { type: 'batch-validation', title: '批量验证' },
    'ontology-graph': { type: 'ontology-graph', title: '本体可视化' }
  }
  const item = menuMap[key]
  if (item) quickAdd(item.type, item.title)
}

function handleSaved() {
  store.loadFullOntology(props.graphId)
}

function handleInstanceSaved() {
  editingInstance.value = undefined
}

function handleEditInstance(data: any) {
  editingInstance.value = data
  store.openTab({
    id: `instance-editor-${data.uuid || 'new'}`,
    type: 'instance-editor',
    title: `实例: ${data.name || '新建'}`,
    classType: data.type
  })
}

function openClassEditor(classId: number) {
  const cls = store.classes.find(c => c.id === classId)
  store.openTab({
    id: `class-editor-${classId}`,
    type: 'class-editor',
    title: `类: ${cls?.localName ?? classId}`,
    classId
  })
}

function openPropertyEditor(propertyId: number, propertyName: string) {
  store.openTab({
    id: `property-editor-${propertyId}`,
    type: 'property-editor',
    title: `属性: ${propertyName}`,
    propertyId
  })
}
</script>

<style scoped lang="less">
.ontology-workbench {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #0d1117;
  overflow: hidden;

  .workbench-header {
    position: relative;
    flex-shrink: 0;
  }

  .add-menu-trigger {
    position: absolute;
    right: 8px;
    top: 50%;
    transform: translateY(-50%);
    color: #8b949e;
    font-size: 12px;
    cursor: pointer;
    padding: 4px 8px;
    border-radius: 4px;
    z-index: 10;
    display: flex;
    align-items: center;
    gap: 4px;
    transition: color 0.15s, background 0.15s;

    &:hover {
      color: #e6edf3;
      background: #21262d;
    }
  }

  .workbench-empty {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 12px;
    color: #8b949e;

    .empty-icon { font-size: 48px; }
    .empty-title { font-size: 18px; font-weight: 600; color: #e6edf3; }
    .empty-desc { font-size: 13px; }
    .empty-quick-actions {
      display: flex;
      gap: 8px;
      margin-top: 8px;
    }
  }

  .workbench-content {
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }

  .panel-placeholder {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #8b949e;
    font-size: 14px;
  }
}

// 使用 :deep() 但不在 scoped 内,确保能影响到 body 下的 dropdown
:deep(.ontology-add-menu) {
  .ant-dropdown {
    left: 50% !important;
    transform: translateX(-50%) !important;
  }

  .ant-dropdown-menu {
    background: #161b22 !important;
    border: 1px solid #30363d !important;
    border-radius: 8px !important;
    box-shadow: 0 8px 24px rgba(0,0,0,0.4) !important;
    min-width: 480px !important;
    max-width: 600px !important;
    padding: 8px 0 !important;

    .ant-dropdown-menu-item-group-title {
      color: #6e7681 !important;
      font-size: 11px !important;
      text-transform: uppercase !important;
      letter-spacing: 0.5px !important;
      padding: 8px 16px 4px !important;
      font-weight: 600 !important;
    }

    .ant-dropdown-menu-item {
      color: #e6edf3 !important;
      font-size: 13px !important;
      padding: 10px 16px !important;
      min-height: 40px !important;

      &:hover {
        background: #21262d !important;
        color: #58a6ff !important;
      }

      span { 
        margin-right: 10px !important;
        font-size: 14px !important;
      }
    }

    .ant-dropdown-menu-item-group-list {
      .ant-dropdown-menu-item {
        padding-left: 24px !important;
      }
    }

    .ant-menu-item-divider {
      margin: 8px 0 !important;
      background-color: #30363d !important;
    }
  }
}
</style>
