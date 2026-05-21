import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ontologyApi } from '@/api/ontology'
import type {
  OntDefinitionVO,
  OntClassVO,
  ClassHierarchyVO,
  OntPropertyVO,
  OntConstraintVO,
  OntVersionHistoryVO
} from '@/api/ontology'

// ============================================================
// Types
// ============================================================

export type OntologyTabType =
  | 'class-list'
  | 'class-editor'
  | 'property-list'
  | 'property-editor'
  | 'constraint-list'
  | 'instance-table'
  | 'instance-editor'
  | 'version-history'
  | 'version-diff'
  | 'consistency-check'
  | 'batch-validation'
  | 'ontology-graph'

export interface OntologyTab {
  id: string
  type: OntologyTabType
  title: string
  dirty?: boolean           // 未保存标记
  classId?: number           // 用于 class-editor
  propertyId?: number       // 用于 property-editor
  classType?: string        // 用于 instance-table
}

export interface OntologyExplorerNode {
  key: string
  title: string
  icon?: string
  type: 'root' | 'class-group' | 'property-group' | 'constraint-group' |
        'instance-group' | 'class' | 'property' | 'constraint' |
        'instance-class' | 'version-history' | 'tool-consistency' |
        'tool-validation' | 'tool-graph' | 'tool'
  classId?: number
  propertyId?: number
  classType?: string
  count?: number
  children?: OntologyExplorerNode[]
}

// ============================================================
// Store
// ============================================================

export const useOntologyStore = defineStore('ontology', () => {
  // ---- 本体定义缓存 ----
  const definition = ref<OntDefinitionVO | null>(null)
  const classes = ref<OntClassVO[]>([])
  const classHierarchy = ref<ClassHierarchyVO[]>([])
  const properties = ref<OntPropertyVO[]>([])
  const constraints = ref<OntConstraintVO[]>([])
  const versionHistory = ref<OntVersionHistoryVO[]>([])

  // ---- UI状态 ----
  const loading = ref(false)
  const selectedClassId = ref<number | null>(null)
  const selectedPropertyId = ref<number | null>(null)
  const expandedTreeKeys = ref<Set<string>>(new Set(['ontology-root', 'classes', 'instances']))

  // ---- 工作台状态（Navicat风格多标签页）----
  const openTabs = ref<OntologyTab[]>([])
  const activeTabId = ref<string | null>(null)

  // ---- Getters ----
  const activeTab = computed(() =>
    openTabs.value.find(t => t.id === activeTabId.value) ?? null
  )

  const classMap = computed(() => {
    const map = new Map<number, OntClassVO>()
    classes.value.forEach(c => map.set(c.id, c))
    return map
  })

  const propertyMap = computed(() => {
    const map = new Map<number, OntPropertyVO>()
    properties.value.forEach(p => map.set(p.id, p))
    return map
  })

  const instanceClassTypes = computed(() => {
    // 从 classHierarchy 提取所有叶子节点的 classType
    const types = new Set<string>()
    classes.value.forEach(c => types.add(c.localName))
    return Array.from(types)
  })

  // ---- Actions ----

  async function loadFullOntology(graphId: string) {
    loading.value = true
    try {
      const data = await ontologyApi.getFullOntology(graphId)
      definition.value = data.definition
      classes.value = data.classes ?? []
      classHierarchy.value = data.classHierarchy ?? []
      properties.value = data.properties ?? []
      constraints.value = data.constraints ?? []
    } catch (e) {
      console.error('[OntologyStore] loadFullOntology failed', e)
    } finally {
      loading.value = false
    }
  }

  async function loadVersionHistory(graphId: string) {
    try {
      versionHistory.value = await ontologyApi.getVersionHistory(graphId)
    } catch (e) {
      console.error('[OntologyStore] loadVersionHistory failed', e)
    }
  }

  function refreshClasses(newClasses: OntClassVO[]) {
    classes.value = newClasses
  }

  function refreshProperties(newProperties: OntPropertyVO[]) {
    properties.value = newProperties
  }

  function refreshConstraints(newConstraints: OntConstraintVO[]) {
    constraints.value = newConstraints
  }

  // ---- Tab 管理 ----

  function openTab(tab: OntologyTab) {
    const existing = openTabs.value.find(t => t.id === tab.id)
    if (existing) {
      activeTabId.value = tab.id
      return
    }
    openTabs.value.push(tab)
    activeTabId.value = tab.id
  }

  function closeTab(tabId: string) {
    const idx = openTabs.value.findIndex(t => t.id === tabId)
    if (idx === -1) return
    openTabs.value.splice(idx, 1)
    if (activeTabId.value === tabId) {
      activeTabId.value = openTabs.value[Math.max(0, idx - 1)]?.id ?? null
    }
  }

  function closeOtherTabs(keepTabId: string) {
    openTabs.value = openTabs.value.filter(t => t.id === keepTabId)
    activeTabId.value = keepTabId
  }

  function closeTabsToRight(tabId: string) {
    const idx = openTabs.value.findIndex(t => t.id === tabId)
    if (idx === -1) return
    openTabs.value = openTabs.value.slice(0, idx + 1)
    activeTabId.value = tabId
  }

  function closeAllTabs() {
    openTabs.value = []
    activeTabId.value = null
  }

  function markTabDirty(tabId: string, dirty: boolean) {
    const tab = openTabs.value.find(t => t.id === tabId)
    if (tab) tab.dirty = dirty
  }

  function renameTab(tabId: string, title: string) {
    const tab = openTabs.value.find(t => t.id === tabId)
    if (tab) tab.title = title
  }

  // ---- Tree 节点 ----

  function toggleTreeNode(key: string) {
    if (expandedTreeKeys.value.has(key)) {
      expandedTreeKeys.value.delete(key)
    } else {
      expandedTreeKeys.value.add(key)
    }
  }

  function expandTreeNode(key: string) {
    expandedTreeKeys.value.add(key)
  }

  function collapseTreeNode(key: string) {
    expandedTreeKeys.value.delete(key)
  }

  function buildExplorerTree(): OntologyExplorerNode {
    const classCount = classes.value.length
    const propertyCount = properties.value.length
    const constraintCount = constraints.value.length

    return {
      key: 'ontology-root',
      title: '本体定义',
      icon: '📁',
      type: 'root',
      children: [
        {
          key: 'classes',
          title: '类',
          icon: '📂',
          type: 'class-group',
          count: classCount,
          children: classes.value.map(cls => ({
            key: `class-${cls.id}`,
            title: cls.localName,
            icon: '◉',
            type: 'class' as const,
            classId: cls.id,
            classType: cls.localName,
            children: []
          }))
        },
        {
          key: 'properties',
          title: '属性',
          icon: '📂',
          type: 'property-group',
          count: propertyCount,
          children: properties.value.map(prop => ({
            key: `property-${prop.id}`,
            title: prop.localName,
            icon: '◆',
            type: 'property' as const,
            propertyId: prop.id,
            children: []
          }))
        },
        {
          key: 'constraints',
          title: '约束',
          icon: '📂',
          type: 'constraint-group',
          count: constraintCount,
          children: constraints.value.map(c => ({
            key: `constraint-${c.id}`,
            title: c.constraintType,
            icon: '◇',
            type: 'constraint' as const,
            children: []
          }))
        },
        {
          key: 'instances',
          title: '实例数据',
          icon: '📂',
          type: 'instance-group',
          children: classes.value.map(cls => ({
            key: `instance-${cls.localName}`,
            title: cls.localName,
            icon: '◉',
            type: 'instance-class' as const,
            classType: cls.localName,
            children: []
          }))
        },
        {
          key: 'version-history',
          title: '版本历史',
          icon: '📂',
          type: 'version-history',
          children: []
        },
        {
          key: 'tools',
          title: '工具',
          icon: '📂',
          type: 'tool',
          children: [
            {
              key: 'tool-consistency',
              title: '一致性检查',
              icon: '🔧',
              type: 'tool-consistency',
              children: []
            },
            {
              key: 'tool-validation',
              title: '批量验证',
              icon: '🔧',
              type: 'tool-validation',
              children: []
            },
            {
              key: 'tool-graph',
              title: '本体可视化',
              icon: '📊',
              type: 'tool-graph',
              children: []
            }
          ]
        }
      ]
    }
  }

  function reset() {
    definition.value = null
    classes.value = []
    classHierarchy.value = []
    properties.value = []
    constraints.value = []
    versionHistory.value = []
    selectedClassId.value = null
    selectedPropertyId.value = null
    expandedTreeKeys.value = new Set(['ontology-root', 'classes', 'instances'])
    openTabs.value = []
    activeTabId.value = null
  }

  return {
    // State
    definition,
    classes,
    classHierarchy,
    properties,
    constraints,
    versionHistory,
    loading,
    selectedClassId,
    selectedPropertyId,
    expandedTreeKeys,
    openTabs,
    activeTabId,
    // Getters
    activeTab,
    classMap,
    propertyMap,
    instanceClassTypes,
    // Actions
    loadFullOntology,
    loadVersionHistory,
    refreshClasses,
    refreshProperties,
    refreshConstraints,
    openTab,
    closeTab,
    closeOtherTabs,
    closeTabsToRight,
    closeAllTabs,
    markTabDirty,
    renameTab,
    toggleTreeNode,
    expandTreeNode,
    collapseTreeNode,
    buildExplorerTree,
    reset
  }
})

export default useOntologyStore
