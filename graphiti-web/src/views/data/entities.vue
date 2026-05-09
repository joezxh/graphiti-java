<template>
  <div class="entities-page">
    <div class="page-header">
      <h1 class="page-title">实体管理</h1>
      <p class="page-desc">查看、搜索、编辑和删除图谱中的实体节点</p>
    </div>

    <a-card class="filter-card">
      <a-row :gutter="16" align="middle">
        <a-col :span="6">
          <a-input-search
            v-model:value="searchKeyword"
            placeholder="搜索实体名称"
            allow-clear
            @search="handleSearch"
          />
        </a-col>
        <a-col :span="4">
          <a-select v-model:value="filterType" placeholder="实体类型" allow-clear style="width: 100%" @change="handleSearch">
            <a-select-option v-for="t in entityTypes" :key="t" :value="t">{{ t }}</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="4">
          <a-select v-model:value="filterGraph" placeholder="选择图谱" allow-clear style="width: 100%" @change="handleSearch">
            <a-select-option v-for="g in graphOptions" :key="g.id" :value="g.id">{{ g.name }}</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="10" style="text-align: right">
          <a-button type="primary" @click="handleSearch">
            <SearchOutlined /> 查询
          </a-button>
          <a-button style="margin-left: 8px" @click="resetFilter">
            <ReloadOutlined /> 重置
          </a-button>
        </a-col>
      </a-row>
    </a-card>

    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="entityList"
        :loading="loading"
        row-key="uuid"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'properties'">
            <div class="prop-tags">
              <a-tag v-for="(val, key) in getDisplayProps(record.properties)" :key="key" size="small">
                {{ key }}: {{ String(val).slice(0, 30) }}
              </a-tag>
            </div>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEditModal(record)">
                <EditOutlined /> 编辑
              </a-button>
              <a-popconfirm title="确定删除该实体？此操作不可恢复。" @confirm="deleteEntity(record.uuid)">
                <a-button type="link" size="small" danger>
                  <DeleteOutlined /> 删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 编辑模态框 -->
    <a-modal
      v-model:open="editVisible"
      title="编辑实体属性"
      @ok="saveEntity"
      :confirm-loading="editSaving"
    >
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="UUID">
          <a-input v-model:value="editForm.uuid" disabled />
        </a-form-item>
        <a-form-item label="名称">
          <a-input v-model:value="editForm.name" />
        </a-form-item>
        <a-form-item label="类型">
          <a-input v-model:value="editForm.type" disabled />
        </a-form-item>
        <a-divider orientation="left">属性</a-divider>
        <div v-for="(_val, key) in editForm.properties" :key="key" class="prop-edit-row">
          <span class="prop-key">{{ key }}</span>
          <a-input v-model:value="editForm.properties[key]" />
        </div>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, ReloadOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { dataApi, type EntityItem, type EntityListParams } from '@/api/data'
import { graphApi, type Graph } from '@/api/graph'

const searchKeyword = ref('')
const filterType = ref<string | undefined>(undefined)
const filterGraph = ref<string | undefined>(undefined)
const entityTypes = ref<string[]>([])
const graphOptions = ref<Graph[]>([])

const loading = ref(false)
const entityList = ref<EntityItem[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  { title: 'UUID', dataIndex: 'uuid', key: 'uuid', width: 200 },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '类型', dataIndex: 'type', key: 'type', width: 120 },
  { title: '属性', key: 'properties' },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

const loadGraphs = async () => {
  try {
    graphOptions.value = await graphApi.getList()
  } catch (err) {
    console.error('加载图谱列表失败', err)
  }
}

const loadEntityTypes = async () => {
  try {
    entityTypes.value = await dataApi.getEntityTypes()
  } catch (err) {
    console.error('加载实体类型失败', err)
  }
}

const loadEntities = async () => {
  loading.value = true
  try {
    const params: EntityListParams = {
      page: pagination.current,
      pageSize: pagination.pageSize,
      keyword: searchKeyword.value || undefined,
      type: filterType.value,
      graphId: filterGraph.value
    }
    const res = await dataApi.listEntities(params)
    entityList.value = res.list
    pagination.total = res.total
  } catch (err: any) {
    message.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadEntities()
}

const resetFilter = () => {
  searchKeyword.value = ''
  filterType.value = undefined
  filterGraph.value = undefined
  pagination.current = 1
  loadEntities()
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadEntities()
}

const getDisplayProps = (props: Record<string, any>) => {
  // 排除 name 属性，已在表格中单独展示
  const { name, ...rest } = props
  return rest
}

// 编辑
const editVisible = ref(false)
const editSaving = ref(false)
const editForm = reactive<{ uuid: string; name: string; type: string; properties: Record<string, any> }>({
  uuid: '',
  name: '',
  type: '',
  properties: {}
})

const openEditModal = (record: EntityItem) => {
  editForm.uuid = record.uuid
  editForm.name = record.name
  editForm.type = record.type
  editForm.properties = JSON.parse(JSON.stringify(record.properties))
  editVisible.value = true
}

const saveEntity = async () => {
  editSaving.value = true
  try {
    await dataApi.updateEntity(editForm.uuid, editForm.properties)
    message.success('保存成功')
    editVisible.value = false
    loadEntities()
  } catch (err: any) {
    message.error(err.message || '保存失败')
  } finally {
    editSaving.value = false
  }
}

const deleteEntity = async (uuid: string) => {
  try {
    await dataApi.deleteEntity(uuid)
    message.success('删除成功')
    loadEntities()
  } catch (err: any) {
    message.error(err.message || '删除失败')
  }
}

onMounted(() => {
  loadGraphs()
  loadEntityTypes()
  loadEntities()
})
</script>

<style scoped lang="less">
@import '@/assets/styles/dark.less';

.entities-page {
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

.filter-card, .table-card {
  background: @bg-container;
  border: 1px solid @border-color;
  margin-bottom: 16px;
}

.prop-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.prop-edit-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;

  .prop-key {
    width: 80px;
    color: @text-secondary;
    font-size: 13px;
    flex-shrink: 0;
  }
}
</style>
