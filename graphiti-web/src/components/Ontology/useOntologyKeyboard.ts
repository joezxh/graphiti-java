/**
 * 本体管理控制台快捷键
 * 使用 Composition API，支持 Ctrl+S 保存、Ctrl+N 新建、Ctrl+W 关闭Tab等
 */
import { onMounted, onBeforeUnmount } from 'vue'
import { useOntologyStore } from '@/store/modules/ontology'

interface KeyboardShortcut {
  key: string
  ctrl?: boolean
  shift?: boolean
  alt?: boolean
  handler: () => void
  description: string
}

export function useOntologyKeyboard() {
  const store = useOntologyStore()

  const shortcuts: KeyboardShortcut[] = [
    {
      key: 's',
      ctrl: true,
      handler: handleSave,
      description: '保存当前Tab (Ctrl+S)'
    },
    {
      key: 'n',
      ctrl: true,
      handler: handleNew,
      description: '新建 (Ctrl+N)'
    },
    {
      key: 'f',
      ctrl: true,
      handler: handleGlobalSearch,
      description: '全局搜索 (Ctrl+F)'
    },
    {
      key: 'w',
      ctrl: true,
      handler: handleCloseTab,
      description: '关闭当前Tab (Ctrl+W)'
    },
    {
      key: 'Tab',
      ctrl: true,
      handler: handleNextTab,
      description: '下一个Tab (Ctrl+Tab)'
    },
    {
      key: 'Tab',
      ctrl: true,
      shift: true,
      handler: handlePrevTab,
      description: '上一个Tab (Ctrl+Shift+Tab)'
    },
    {
      key: 'Delete',
      handler: handleDelete,
      description: '删除选中项 (Delete)'
    },
    {
      key: 'F5',
      handler: handleRefresh,
      description: '刷新当前面板 (F5)'
    },
    {
      key: 'Escape',
      handler: handleEscape,
      description: '取消/关闭 (Escape)'
    }
  ]

  function matchShortcut(event: KeyboardEvent): KeyboardShortcut | null {
    for (const shortcut of shortcuts) {
      const keyMatch = event.key.toLowerCase() === shortcut.key.toLowerCase()
      const ctrlMatch = !!shortcut.ctrl === (event.ctrlKey || event.metaKey)
      const shiftMatch = !!shortcut.shift === event.shiftKey
      const altMatch = !!shortcut.alt === event.altKey

      if (keyMatch && ctrlMatch && shiftMatch && altMatch) {
        return shortcut
      }
    }
    return null
  }

  function handleKeydown(event: KeyboardEvent) {
    // 忽略输入框内的快捷键
    const target = event.target as HTMLElement
    if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable) {
      return
    }

    const shortcut = matchShortcut(event)
    if (shortcut) {
      event.preventDefault()
      event.stopPropagation()
      try {
        shortcut.handler()
      } catch (e) {
        console.warn('[OntologyKeyboard] Shortcut failed:', shortcut.description, e)
      }
    }
  }

  function handleSave() {
    // 通知当前活跃的编辑器Tab保存
    const activeTab = store.activeTab
    if (!activeTab) return

    // 通过自定义事件通知编辑器组件
    window.dispatchEvent(new CustomEvent('ontology:save', { detail: { tabId: activeTab.id } }))
  }

  function handleNew() {
    // 触发新建菜单
    window.dispatchEvent(new CustomEvent('ontology:new'))
  }

  function handleGlobalSearch() {
    // 聚焦搜索框
    window.dispatchEvent(new CustomEvent('ontology:search'))
  }

  function handleCloseTab() {
    if (store.activeTabId) {
      store.closeTab(store.activeTabId)
    }
  }

  function handleNextTab() {
    if (store.openTabs.length < 2) return
    const currentIdx = store.openTabs.findIndex(t => t.id === store.activeTabId)
    const nextIdx = (currentIdx + 1) % store.openTabs.length
    store.openTabs[nextIdx] && (store.activeTabId = store.openTabs[nextIdx].id)
  }

  function handlePrevTab() {
    if (store.openTabs.length < 2) return
    const currentIdx = store.openTabs.findIndex(t => t.id === store.activeTabId)
    const prevIdx = (currentIdx - 1 + store.openTabs.length) % store.openTabs.length
    store.openTabs[prevIdx] && (store.activeTabId = store.openTabs[prevIdx].id)
  }

  function handleDelete() {
    // 触发删除确认
    window.dispatchEvent(new CustomEvent('ontology:delete'))
  }

  function handleRefresh() {
    // 触发刷新
    window.dispatchEvent(new CustomEvent('ontology:refresh'))
  }

  function handleEscape() {
    // 触发ESC关闭行为（如关闭弹窗、右键菜单等）
    window.dispatchEvent(new CustomEvent('ontology:escape'))
  }

  onMounted(() => {
    document.addEventListener('keydown', handleKeydown, true)
  })

  onBeforeUnmount(() => {
    document.removeEventListener('keydown', handleKeydown, true)
  })

  return {
    shortcuts
  }
}
