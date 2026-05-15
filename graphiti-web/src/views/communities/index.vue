<template>
  <div class="communities-page">
    <div class="page-header">
      <h1 class="page-title">{{ $t("community.title") }}</h1>
      <p class="page-desc">{{ $t("community.titleDesc") }}</p>
    </div>

    <a-card class="filter-card">
      <a-row :gutter="16" align="middle">
        <a-col :span="6">
          <a-select
            v-model:value="selectedGraphId"
            :placeholder="$t('ontology.selectGraph')"
            style="width: 100%"
            allow-clear
            @change="onGraphChange"
          >
            <a-select-option v-for="g in graphOptions" :key="g.graphId" :value="g.graphId">
              {{ g.name }}
            </a-select-option>
          </a-select>
        </a-col>
        <a-col :span="8">
          <a-input-search
            v-model:value="searchQuery"
            :placeholder="$t('common.search')"
            allow-clear
            @search="handleSearch"
          />
        </a-col>
        <a-col :span="6">
          <a-space>
            <a-button type="primary" @click="loadCommunities">
              {{ $t("common.view") }}
            </a-button>
            <a-button @click="buildCommunity">
              <ReloadOutlined /> {{ $t("common.refresh") }}
            </a-button>
          </a-space>
        </a-col>
      </a-row>
    </a-card>

    <a-row :gutter="16" class="result-area">
      <a-col :span="10">
        <a-card :title="$t('community.list') || 'Community List'" class="list-card">
          <a-empty v-if="!communityList.length && !loading" :description="$t('common.noData')" />
          <a-list v-else :data-source="communityList" size="small">
            <template #renderItem="{ item, index }">
              <a-list-item
                :class="['community-item', selectedCommunity?.id === item.id ? 'selected' : '']"
                @click="selectCommunity(item)"
              >
                <a-list-item-meta>
                  <template #title>
                    <span class="community-name">{{ item.name || ("common.node" + " " + (index + 1)) }}</span>
                  </template>
                  <template #description>
                    <a-space size="small">
                      <a-tag color="blue">{{ item.nodeCount }} {{ $t("data.nodes") || "Nodes" }}</a-tag>
                      <a-tag color="purple">{{ item.edgeCount }} {{ $t("data.edges") || "Edges" }}</a-tag>
                    </a-space>
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>

      <a-col :span="14">
        <a-card :title="$t('community.detail') || 'Community Detail'" class="detail-card">
          <template v-if="selectedCommunity">
            <a-descriptions :column="2" bordered size="small">
              <a-descriptions-item label="Community ID" :span="2">{{ selectedCommunity.id }}</a-descriptions-item>
              <a-descriptions-item :label="$t('data.nodes')">{{ selectedCommunity.nodeCount }}</a-descriptions-item>
              <a-descriptions-item :label="$t('data.edges')">{{ selectedCommunity.edgeCount }}</a-descriptions-item>
            </a-descriptions>

            <a-divider>{{ $t("community.members") || "Node Members" }}</a-divider>
            <div class="member-tags">
              <a-tag
                v-for="nodeId in (selectedCommunity as any).nodes?.slice(0, 20)"
                :key="nodeId"
                color="blue"
                class="member-tag"
              >
                {{ truncate(nodeId, 12) }}
              </a-tag>
              <span v-if="(selectedCommunity as any).nodes?.length > 20" class="more-count">
                +{{ (selectedCommunity as any).nodes.length - 20 }} {{ $t("common.more") }}
              </span>
            </div>

            <a-divider>{{ $t("community.edgeMembers") || "Edge Members" }}</a-divider>
            <div class="member-tags">
              <a-tag
                v-for="edgeId in (selectedCommunity as any).edges?.slice(0, 10)"
                :key="edgeId"
                color="purple"
                class="member-tag"
              >
                {{ truncate(edgeId, 12) }}
              </a-tag>
              <span v-if="(selectedCommunity as any).edges?.length > 10" class="more-count">
                +{{ (selectedCommunity as any).edges.length - 10 }} {{ $t("common.more") }}
              </span>
            </div>
          </template>
          <a-empty v-else :description="$t('common.noData')" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import { message } from "ant-design-vue"
import { ReloadOutlined } from "@ant-design/icons-vue"
import { graphApi } from "@/api/graph"

const graphOptions = ref<any[]>([])
const selectedGraphId = ref<string | undefined>(undefined)
const searchQuery = ref("")
const communityList = ref<any[]>([])
const selectedCommunity = ref<any | null>(null)
const loading = ref(false)

const loadGraphs = async () => {
  try {
    graphOptions.value = await graphApi.getList()
  } catch (err) {
    console.error("data.loadFailed", err)
  }
}

const loadCommunities = async () => {
  if (!selectedGraphId.value) return
  loading.value = true
  try {
    const resp = await graphApi.getCommunities(selectedGraphId.value)
    communityList.value = resp || []
    selectedCommunity.value = null
  } catch (err: any) {
    message.error(err.message || "common.error")
  } finally {
    loading.value = false
  }
}

const buildCommunity = async () => {
  if (!selectedGraphId.value) return
  loading.value = true
  try {
    const resp = await graphApi.buildCommunity(selectedGraphId.value)
    message.success(resp.message || "common.success")
    loadCommunities()
  } catch (err: any) {
    message.error(err.message || "common.error")
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  if (!selectedGraphId.value || !searchQuery.value.trim()) {
    loadCommunities()
    return
  }
  loading.value = true
  try {
    const resp = await graphApi.searchCommunities(selectedGraphId.value, searchQuery.value)
    communityList.value = resp || []
  } catch (err: any) {
    message.error(err.message || "common.error")
  } finally {
    loading.value = false
  }
}

const selectCommunity = (item: any) => {
  selectedCommunity.value = item
}

const onGraphChange = () => {
  communityList.value = []
  selectedCommunity.value = null
}

const truncate = (str: string | undefined, len: number): string => {
  if (!str) return "-"
  return str.length > len ? str.slice(0, len) + "..." : str
}

onMounted(() => {
  loadGraphs()
})
</script>

<style scoped lang="less">
@import "@/assets/styles/dark.less";

.communities-page {
  padding: 24px;
}

.page-header {
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

.filter-card {
  background: @bg-container;
  border: 1px solid @border-color;
  margin-bottom: 16px;
}

.result-area {
  min-height: 400px;
}

.list-card, .detail-card {
  background: @bg-container;
  border: 1px solid @border-color;
  height: 100%;
}

.community-item {
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

.community-name {
  font-weight: 500;
  color: @text-primary;
}

.member-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.member-tag {
  font-size: 12px;
}

.more-count {
  color: @text-tertiary;
  font-size: 12px;
  align-self: center;
}
</style>
