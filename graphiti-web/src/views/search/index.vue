<template>
  <div class="search-page">
    <div class="search-header">
      <h1 class="page-title">{{ $t('search.title') }}</h1>
      <p class="page-desc">{{ $t('search.titleDesc') }}</p>
    </div>

    <a-card class="search-card">
      <div class="search-input-wrapper">
        <a-input-search
          v-model:value="searchQuery"
          :placeholder="$t('search.placeholder')"
          enter-button
          size="large"
          :loading="searching"
          @search="executeSearch"
        >
          <template #addonBefore>
            <a-select v-model:value="searchMode" style="width: 110px">
              <a-select-option value="semantic">{{ $t('search.semanticSearch') }}</a-select-option>
              <a-select-option value="structured">{{ $t('search.structuredSearch') }}</a-select-option>
              <a-select-option value="hybrid">{{ $t('search.hybridMode') }}</a-select-option>
              <a-select-option value="bfs">{{ $t('search.graphTraversal') }}</a-select-option>
              <a-select-option value="memory">{{ $t('search.memorySearch') }}</a-select-option>
            </a-select>
          </template>
        </a-input-search>
      </div>

      <div v-if="searchMode === 'structured' || searchMode === 'hybrid'" class="filter-panel">
        <a-row :gutter="12" align="middle">
          <a-col>{{ $t('search.filterConditions') }}</a-col>
          <a-col v-for="(filter, idx) in filters" :key="idx">
            <a-space>
              <a-input v-model:value="filter.field" :placeholder="$t('search.field')" style="width: 100px" size="small" />
              <a-select v-model:value="filter.operator" style="width: 90px" size="small">
                <a-select-option value="eq">{{ $t('search.equals') }}</a-select-option>
                <a-select-option value="gt">{{ $t('search.greaterThan') }}</a-select-option>
                <a-select-option value="gte">{{ $t('search.greaterThanOrEqual') }}</a-select-option>
                <a-select-option value="lt">{{ $t('search.lessThan') }}</a-select-option>
                <a-select-option value="lte">{{ $t('search.lessThanOrEqual') }}</a-select-option>
                <a-select-option value="contains">{{ $t('search.contains') }}</a-select-option>
              </a-select>
              <a-input v-model:value="filter.value" :placeholder="$t('search.value')" style="width: 120px" size="small" />
              <a-button type="link" size="small" danger @click="removeFilter(idx)">
                <CloseOutlined />
              </a-button>
            </a-space>
          </a-col>
          <a-col>
            <a-button type="dashed" size="small" @click="addFilter">
              <PlusOutlined /> {{ $t('search.addCondition') }}
            </a-button>
          </a-col>
        </a-row>
      </div>
    </a-card>

    <a-row :gutter="24" class="result-area">
      <a-col :span="10">
        <a-card :title="$t('search.searchResults')" class="result-card">
          <a-empty v-if="!searchResults.length && !searching" :description="$t('search.enterKeywordToSearch')" />
          <a-list v-else :data-source="searchResults" size="small">
            <template #renderItem="{ item }">
              <a-list-item
                :class="['result-item', selectedResult?.id === item.id ? 'selected' : '']"
                @click="selectResult(item)"
              >
                <div class="result-content">
                  <div class="result-header">
                    <a-tag :color="item.type === 'node' ? 'blue' : 'purple'">
                      {{ item.type === 'node' ? $t('search.node') : $t('search.edge') }}
                    </a-tag>
                    <span class="result-name">{{ item.name }}</span>
                    <span class="result-score">{{ (item.score * 100).toFixed(1) }}%</span>
                  </div>
                  <div class="result-type">{{ item.entityType || item.relationType }}</div>
                  <div class="result-props">
                    <span v-for="(val, key) in getDisplayProps(item.properties)" :key="String(key)" class="prop-chip">
                      {{ key }}: {{ String(val).slice(0, 20) }}
                    </span>
                  </div>
                </div>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>

      <a-col :span="14">
        <a-card :title="$t('search.resultVisualization')" class="viz-card">
          <ForceGraph
            v-if="searchResults.length > 0"
            graph-id="search-result"
            :nodes="vizNodes"
            :edges="vizEdges"
            :categories="categories"
            :highlight-node="selectedResult?.id"
            @node-click="onNodeClick"
          />
          <a-empty v-else :description="$t('search.resultsHighlighted')" />
        </a-card>
      </a-col>
    </a-row>

    <a-card v-if="searchHistory.length > 0" :title="$t('search.searchHistory')" class="history-card">
      <a-space wrap>
        <a-tag
          v-for="h in searchHistory.slice(0, 10)"
          :key="h.id"
          class="history-tag"
          @click="quickSearch(h.query, h.mode)"
        >
          {{ h.query }}
          <span class="history-count">({{ h.resultCount }})</span>
        </a-tag>
      </a-space>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, CloseOutlined } from '@ant-design/icons-vue'
import { searchApi, type SearchParams, type SearchResult, type SearchFilter, type SearchHistory } from '@/api/search'
import ForceGraph from '@/components/Graph/ForceGraph.vue'

const searchQuery = ref('')
const searchMode = ref<'semantic' | 'structured' | 'hybrid' | 'bfs' | 'memory'>('hybrid')
const searching = ref(false)
const searchResults = ref<SearchResult[]>([])
const selectedResult = ref<SearchResult | null>(null)
const searchHistory = ref<SearchHistory[]>([])

const filters = reactive<SearchFilter[]>([])

const addFilter = () => {
  filters.push({ field: '', operator: 'eq', value: '' })
}

const removeFilter = (idx: number) => {
  filters.splice(idx, 1)
}

const categories = [
  { name: 'Person', itemStyle: { color: '#5e6ad2' } },
  { name: 'Company', itemStyle: { color: '#00d4ff' } },
  { name: 'Product', itemStyle: { color: '#22c55e' } },
  { name: 'Edge', itemStyle: { color: '#f59e0b' } }
]

const vizNodes = computed(() => {
  return searchResults.value
    .filter((r) => r.type === "node")
    .map((r) => ({
      id: r.id,
      name: r.name,
      category: categories.findIndex((c) => c.name === (r.entityType || "Person")),
      value: String(r.score),
      symbolSize: 20 + r.score * 30,
      itemStyle: { color: "#5e6ad2", borderColor: "#7b7ff0", borderWidth: 2, shadowBlur: 10, shadowColor: "#5e6ad266" },
      label: { show: true, color: "#f7f8f8", fontSize: 12 },
      data: r as any
    }))
})

const vizEdges = computed(() => {
  return searchResults.value
    .filter((r) => r.type === "edge" && r.source && r.target)
    .map((r) => ({
      id: r.id,
      source: r.source!,
      target: r.target!,
      value: String(r.score),
      lineStyle: { width: 2, color: "#5e6ad266", curveness: 0.2, opacity: 0.6 },
      label: { show: false, formatter: r.relationType || "", fontSize: 10, color: "#8a8f98" },
      data: r as any
    }))
})

const executeSearch = async () => {
  if (!searchQuery.value.trim()) {
    message.warning("search.pleaseEnterKeyword")
    return
  }
  searching.value = true
  searchResults.value = []
  selectedResult.value = null
  try {
    let results: any[] = []

    if (searchMode.value === 'bfs') {
      results = await searchApi.bfsSearch('', searchQuery.value, 2, 50)
    } else if (searchMode.value === 'memory') {
      results = await searchApi.memorySearch({ graphId: '', query: searchQuery.value, maxFacts: 50 })
    } else {
      const params: SearchParams = {
        query: searchQuery.value,
        mode: searchMode.value,
        filters: filters.filter(f => f.field && f.value),
        limit: 50
      }
      results = await searchApi.search(params)
    }

    searchResults.value = results
    selectedResult.value = results.length > 0 ? results[0] : null

    await searchApi.saveSearchHistory(searchQuery.value, searchMode.value, results.length)
    loadHistory()
  } catch (err: any) {
    message.error(err.message || "search.searchFailed")
  } finally {
    searching.value = false
  }
}

const selectResult = (item: SearchResult) => {
  selectedResult.value = item
}

const onNodeClick = (node: any) => {
  const found = searchResults.value.find(r => r.id === node.id)
  if (found) {
    selectedResult.value = found
  }
}

const quickSearch = (query: string, mode: string) => {
  searchQuery.value = query
  searchMode.value = mode as any
  executeSearch()
}

const getDisplayProps = (props: Record<string, any>) => {
  const { name, ...rest } = props
  return rest
}

const loadHistory = async () => {
  try {
    const resp = await searchApi.getSearchHistory()
    searchHistory.value = resp.list || []
  } catch (err) {
    console.error("search.loadHistoryFailed", err)
  }
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped lang="less">
@import '@/assets/styles/dark.less';

.search-page {
  padding: 24px;
}

.search-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: @text-primary;
  margin-bottom: 4px;
}

.page-desc {
  color: @text-secondary;
  font-size: 14px;
}

.search-card {
  background: @bg-container;
  border: 1px solid @border-color;
  margin-bottom: 16px;
}

.search-input-wrapper {
  margin-bottom: 12px;
}

.filter-panel {
  padding-top: 8px;
  border-top: 1px solid @border-color;
  color: @text-secondary;
  font-size: 13px;
}

.result-area {
  margin-bottom: 16px;
}

.result-card, .viz-card, .history-card {
  background: @bg-container;
  border: 1px solid @border-color;
  height: 100%;
}

.result-item {
  cursor: pointer;
  padding: 12px;
  border-radius: @border-radius-md;
  transition: background 0.2s;

  &:hover {
    background: @bg-menu-item-hover;
  }

  &.selected {
    background: @bg-menu-item-active;
    border-left: 3px solid @primary-color;
  }
}

.result-content {
  width: 100%;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.result-name {
  font-weight: 500;
  color: @text-primary;
  flex: 1;
}

.result-score {
  color: @primary-color;
  font-size: 12px;
  font-weight: 600;
}

.result-type {
  color: @text-secondary;
  font-size: 12px;
  margin-bottom: 4px;
}

.result-props {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.prop-chip {
  font-size: 11px;
  padding: 2px 6px;
  background: @bg-elevated;
  border-radius: 4px;
  color: @text-tertiary;
}

.history-tag {
  cursor: pointer;

  &:hover {
    color: @primary-color;
    border-color: @primary-color;
  }

  .history-count {
    color: @text-tertiary;
    margin-left: 4px;
  }
}

:deep(.ant-list-item) {
  padding: 0;
  border-bottom: none;
}
</style>
