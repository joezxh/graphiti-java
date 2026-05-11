<template>
  <div class="ontology-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">本体配置</h1>
        <p class="page-desc">管理图谱中的实体类型与关系类型定义</p>
      </div>
      <div class="header-right">
        <span class="label">选择图谱：</span>
        <a-select
          v-model:value="selectedGraphId"
          placeholder="请先选择图谱"
          style="width: 200px"
          allow-clear
          @change="onGraphChange"
        >
          <a-select-option v-for="g in graphOptions" :key="g.graphId" :value="g.graphId">
            {{ g.name }}
          </a-select-option>
        </a-select>
      </div>
    </div>

    <a-tabs v-model:activeKey="activeTab" class="ontology-tabs">
      <a-tab-pane key="entity" tab="实体类型">
        <div class="tab-toolbar">
          <a-button type="primary" @click="openEntityModal()">
            <PlusOutlined />
            新增实体类型
          </a-button>
        </div>
        <a-table
          :columns="entityColumns"
          :data-source="entityTypes"
          :loading="entityLoading"
          row-key="id"
          :pagination="{ pageSize: 10 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'properties'">
              <a-tag v-for="p in record.properties" :key="p.name" color="blue">
                {{ p.name }}:{{ p.type }}
              </a-tag>
              <span v-if="!record.properties?.length" class="text-muted">—</span>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="openEntityModal(record)">编辑</a-button>
                <a-popconfirm title="确定删除该实体类型？" @confirm="deleteEntityType(record.id)">
                  <a-button type="link" size="small" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-tab-pane>

      <a-tab-pane key="relation" tab="关系类型">
        <div class="tab-toolbar">
          <a-button type="primary" @click="openRelationModal()">
            <PlusOutlined />
            新增关系类型
          </a-button>
        </div>
        <a-table
          :columns="relationColumns"
          :data-source="relationTypes"
          :loading="relationLoading"
          row-key="id"
          :pagination="{ pageSize: 10 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'direction'">
              <a-tag :color="record.directed ? 'purple' : 'green'">
                {{ record.directed ? '有向' : '无向' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'properties'">
              <a-tag v-for="p in record.properties" :key="p.name" color="blue">
                {{ p.name }}:{{ p.type }}
              </a-tag>
              <span v-if="!record.properties?.length" class="text-muted">—</span>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="openRelationModal(record)">编辑</a-button>
                <a-popconfirm title="确定删除该关系类型？" @confirm="deleteRelationType(record.id)">
                  <a-button type="link" size="small" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>

    <!-- 实体类型模态框 -->
    <a-modal
      v-model:open="entityModalVisible"
      :title="entityEditing ? '编辑实体类型' : '新增实体类型'"
      @ok="saveEntityType"
      :confirm-loading="entitySaving"
    >
      <a-form :model="entityForm" layout="vertical">
        <a-form-item label="名称" required>
          <a-input v-model:value="entityForm.name" placeholder="如：Person" />
        </a-form-item>
        <a-form-item label="描述">
          <a-input v-model:value="entityForm.description" placeholder="类型描述" />
        </a-form-item>
        <a-form-item label="属性定义">
          <div v-for="(prop, idx) in entityForm.properties" :key="idx" class="prop-row">
            <a-input v-model:value="prop.name" placeholder="属性名" style="width: 120px" />
            <a-select v-model:value="prop.type" style="width: 100px">
              <a-select-option value="string">string</a-select-option>
              <a-select-option value="int">int</a-select-option>
              <a-select-option value="float">float</a-select-option>
              <a-select-option value="boolean">boolean</a-select-option>
              <a-select-option value="date">date</a-select-option>
              <a-select-option value="list">list</a-select-option>
            </a-select>
            <a-checkbox v-model:checked="prop.required">必填</a-checkbox>
            <a-button type="link" danger @click="removeEntityProp(idx)">删除</a-button>
          </div>
          <a-button type="dashed" block @click="addEntityProp">
            <PlusOutlined /> 添加属性
          </a-button>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 关系类型模态框 -->
    <a-modal
      v-model:open="relationModalVisible"
      :title="relationEditing ? '编辑关系类型' : '新增关系类型'"
      @ok="saveRelationType"
      :confirm-loading="relationSaving"
    >
      <a-form :model="relationForm" layout="vertical">
        <a-form-item label="名称" required>
          <a-input v-model:value="relationForm.name" placeholder="如：WORKS_AT" />
        </a-form-item>
        <a-form-item label="描述">
          <a-input v-model:value="relationForm.description" placeholder="关系描述" />
        </a-form-item>
        <a-form-item label="源实体类型" required>
          <a-select v-model:value="relationForm.sourceType" placeholder="选择源类型">
            <a-select-option v-for="et in entityTypes" :key="et.name" :value="et.name">{{ et.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="目标实体类型" required>
          <a-select v-model:value="relationForm.targetType" placeholder="选择目标类型">
            <a-select-option v-for="et in entityTypes" :key="et.name" :value="et.name">{{ et.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-checkbox v-model:checked="relationForm.directed">有向关系</a-checkbox>
        </a-form-item>
        <a-form-item label="属性定义">
          <div v-for="(prop, idx) in relationForm.properties" :key="idx" class="prop-row">
            <a-input v-model:value="prop.name" placeholder="属性名" style="width: 120px" />
            <a-select v-model:value="prop.type" style="width: 100px">
              <a-select-option value="string">string</a-select-option>
              <a-select-option value="int">int</a-select-option>
              <a-select-option value="float">float</a-select-option>
              <a-select-option value="boolean">boolean</a-select-option>
              <a-select-option value="date">date</a-select-option>
              <a-select-option value="list">list</a-select-option>
            </a-select>
            <a-checkbox v-model:checked="prop.required">必填</a-checkbox>
            <a-button type="link" danger @click="removeRelationProp(idx)">删除</a-button>
          </div>
          <a-button type="dashed" block @click="addRelationProp">
            <PlusOutlined /> 添加属性
          </a-button>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { ontologyApi, type EntityType, type RelationType, type PropertyDef } from '@/api/ontology'
import { graphApi, type Graph } from '@/api/graph'

const activeTab = ref('entity')
const graphOptions = ref<Graph[]>([])
const selectedGraphId = ref<string | undefined>(undefined)

// 实体类型
const entityTypes = ref<EntityType[]>([])
const entityLoading = ref(false)
const entityColumns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '属性', key: 'properties' },
  { title: '操作', key: 'action', width: 150 }
]

const entityModalVisible = ref(false)
const entitySaving = ref(false)
const entityEditing = ref(false)
const entityForm = reactive<{ id?: string; name: string; description: string; properties: PropertyDef[] }>({
  name: '',
  description: '',
  properties: []
})

const openEntityModal = (record?: EntityType) => {
  if (record) {
    entityEditing.value = true
    entityForm.id = record.id
    entityForm.name = record.name
    entityForm.description = record.description || ''
    entityForm.properties = JSON.parse(JSON.stringify(record.properties))
  } else {
    entityEditing.value = false
    entityForm.id = undefined
    entityForm.name = ''
    entityForm.description = ''
    entityForm.properties = []
  }
  entityModalVisible.value = true
}

const addEntityProp = () => {
  entityForm.properties.push({ name: '', type: 'string', required: false })
}

const removeEntityProp = (idx: number) => {
  entityForm.properties.splice(idx, 1)
}

const saveEntityType = async () => {
  if (!selectedGraphId.value) {
    message.error('请先选择图谱')
    return
  }
  if (!entityForm.name.trim()) {
    message.error('请输入类型名称')
    return
  }
  entitySaving.value = true
  try {
    if (entityEditing.value && entityForm.id) {
      await ontologyApi.updateEntityType(selectedGraphId.value, entityForm.id, {
        name: entityForm.name,
        description: entityForm.description,
        properties: entityForm.properties.filter(p => p.name.trim())
      })
      message.success('更新成功')
    } else {
      await ontologyApi.createEntityType(selectedGraphId.value, {
        name: entityForm.name,
        description: entityForm.description,
        properties: entityForm.properties.filter(p => p.name.trim())
      })
      message.success('创建成功')
    }
    entityModalVisible.value = false
    loadEntityTypes()
  } catch (err: any) {
    message.error(err.message || '保存失败')
  } finally {
    entitySaving.value = false
  }
}

const deleteEntityType = async (id: string) => {
  if (!selectedGraphId.value) return
  try {
    await ontologyApi.deleteEntityType(selectedGraphId.value, id)
    message.success('删除成功')
    loadEntityTypes()
  } catch (err: any) {
    message.error(err.message || '删除失败')
  }
}

const loadEntityTypes = async () => {
  if (!selectedGraphId.value) {
    entityTypes.value = []
    return
  }
  entityLoading.value = true
  try {
    entityTypes.value = await ontologyApi.listEntityTypes(selectedGraphId.value)
  } catch (err: any) {
    message.error(err.message || '加载实体类型失败')
  } finally {
    entityLoading.value = false
  }
}

// 关系类型
const relationTypes = ref<RelationType[]>([])
const relationLoading = ref(false)
const relationColumns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '源 → 目标', key: 'endpoint', customRender: ({ record }: any) => `${record.sourceType} → ${record.targetType}` },
  { title: '方向', key: 'direction' },
  { title: '属性', key: 'properties' },
  { title: '操作', key: 'action', width: 150 }
]

const relationModalVisible = ref(false)
const relationSaving = ref(false)
const relationEditing = ref(false)
const relationForm = reactive<{ id?: string; name: string; description: string; sourceType: string; targetType: string; directed: boolean; properties: PropertyDef[] }>({
  name: '',
  description: '',
  sourceType: '',
  targetType: '',
  directed: true,
  properties: []
})

const openRelationModal = (record?: RelationType) => {
  if (record) {
    relationEditing.value = true
    relationForm.id = record.id
    relationForm.name = record.name
    relationForm.description = record.description || ''
    relationForm.sourceType = record.sourceType
    relationForm.targetType = record.targetType
    relationForm.directed = record.directed
    relationForm.properties = JSON.parse(JSON.stringify(record.properties))
  } else {
    relationEditing.value = false
    relationForm.id = undefined
    relationForm.name = ''
    relationForm.description = ''
    relationForm.sourceType = ''
    relationForm.targetType = ''
    relationForm.directed = true
    relationForm.properties = []
  }
  relationModalVisible.value = true
}

const addRelationProp = () => {
  relationForm.properties.push({ name: '', type: 'string', required: false })
}

const removeRelationProp = (idx: number) => {
  relationForm.properties.splice(idx, 1)
}

const saveRelationType = async () => {
  if (!selectedGraphId.value) {
    message.error('请先选择图谱')
    return
  }
  if (!relationForm.name.trim() || !relationForm.sourceType || !relationForm.targetType) {
    message.error('请填写完整信息')
    return
  }
  relationSaving.value = true
  try {
    if (relationEditing.value && relationForm.id) {
      await ontologyApi.updateRelationType(selectedGraphId.value, relationForm.id, {
        name: relationForm.name,
        description: relationForm.description,
        sourceType: relationForm.sourceType,
        targetType: relationForm.targetType,
        directed: relationForm.directed,
        properties: relationForm.properties.filter(p => p.name.trim())
      })
      message.success('更新成功')
    } else {
      await ontologyApi.createRelationType(selectedGraphId.value, {
        name: relationForm.name,
        description: relationForm.description,
        sourceType: relationForm.sourceType,
        targetType: relationForm.targetType,
        directed: relationForm.directed,
        properties: relationForm.properties.filter(p => p.name.trim())
      })
      message.success('创建成功')
    }
    relationModalVisible.value = false
    loadRelationTypes()
  } catch (err: any) {
    message.error(err.message || '保存失败')
  } finally {
    relationSaving.value = false
  }
}

const deleteRelationType = async (id: string) => {
  if (!selectedGraphId.value) return
  try {
    await ontologyApi.deleteRelationType(selectedGraphId.value, id)
    message.success('删除成功')
    loadRelationTypes()
  } catch (err: any) {
    message.error(err.message || '删除失败')
  }
}

const loadGraphs = async () => {
  try {
    graphOptions.value = await graphApi.getList()
  } catch (err) {
    console.error('加载图谱列表失败', err)
  }
}

const onGraphChange = () => {
  loadEntityTypes()
  loadRelationTypes()
}

onMounted(() => {
  loadGraphs()
})
</script>

<style scoped lang="less">
@import '@/assets/styles/dark.less';

.ontology-page {
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

.tab-toolbar {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
}

.prop-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.text-muted {
  color: @text-tertiary;
}

:deep(.ant-tabs-tab) {
  color: @text-secondary;
}

:deep(.ant-tabs-tab-active) {
  color: @primary-color !important;
}
</style>
