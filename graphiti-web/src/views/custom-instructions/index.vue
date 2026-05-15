<template>
  <div class="custom-instructions-page">
    <div class="page-header">
      <h1 class="page-title">{{ $t("customInstructions.title") }}</h1>
      <p class="page-desc">{{ $t("customInstructions.titleDesc") }}</p>
    </div>

    <a-card class="filter-card">
      <a-row :gutter="16" align="middle">
        <a-col :span="6">
          <a-select
            v-model:value="filterGraphId"
            :placeholder="$t('ontology.selectGraph')"
            style="width: 100%"
            allow-clear
            @change="loadInstructions"
          >
            <a-select-option v-for="g in graphOptions" :key="g.graphId" :value="g.graphId">
              {{ g.name }}
            </a-select-option>
          </a-select>
        </a-col>
        <a-col :span="6">
          <a-button type="primary" @click="showCreateModal">
            <PlusOutlined /> {{ $t("common.create") }}
          </a-button>
        </a-col>
        <a-col :span="6">
          <a-button @click="loadInstructions">
            <ReloadOutlined /> {{ $t("common.refresh") }}
          </a-button>
        </a-col>
      </a-row>
    </a-card>

    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="instructionList"
        :loading="loading"
        row-key="id"
        :pagination="{ pageSize: 10 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'instruction'">
            <div class="instruction-text">{{ record.instruction }}</div>
          </template>
          <template v-if="column.key === 'scope'">
            <a-tag :color="record.graphId ? 'blue' : 'green'">
              {{ record.graphId ? "Graph-specific" : "Global" }}
            </a-tag>
            <span v-if="record.graphId" class="graph-name">
              {{ getGraphName(record.graphId) }}
            </span>
          </template>
          <template v-if="column.key === 'createdAt'">
            {{ formatDate(record.createdAt) }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="viewDetail(record)">{{ $t("common.view") }}</a-button>
              <a-popconfirm :title="$t('common.confirm')" @confirm="deleteInstruction(record.id)">
                <a-button type="link" size="small" danger>{{ $t("common.delete") }}</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalVisible"
      :title="isViewing ? 'Instruction Detail' : $t('common.create')"
      @ok="handleSave"
      :confirm-loading="saving"
      width="600px"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="Scope">
          <a-select v-model:value="form.graphId" :placeholder="$t('form.pleaseSelect')" allow-clear>
            <a-select-option v-for="g in graphOptions" :key="g.graphId" :value="g.graphId">
              {{ g.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('customInstructions.content') || 'Instruction Content'" required>
          <a-textarea
            v-model:value="form.instruction"
            placeholder="Enter custom extraction instruction, max 5000 characters"
            :rows="8"
            :maxlength="5000"
            show-count
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue"
import { message } from "ant-design-vue"
import { PlusOutlined, ReloadOutlined } from "@ant-design/icons-vue"
import { graphApi } from "@/api/graph"
import { customInstructionApi, type CustomInstruction, type CreateCustomInstructionReq } from "@/api/customInstruction"

const graphOptions = ref<any[]>([])
const filterGraphId = ref<string | undefined>(undefined)
const instructionList = ref<CustomInstruction[]>([])
const loading = ref(false)

const columns = [
  { title: "common.id", dataIndex: "id", key: "id", width: 200, ellipsis: true },
  { title: "Instruction", key: "instruction" },
  { title: "Scope", key: "scope", width: 180 },
  { title: "common.createdAt", key: "createdAt", width: 160 },
  { title: "common.action", key: "action", width: 150 }
]

const modalVisible = ref(false)
const saving = ref(false)
const isViewing = ref(false)
const form = reactive<CreateCustomInstructionReq>({
  instruction: "",
  graphId: undefined
})

const loadGraphs = async () => {
  try {
    graphOptions.value = await graphApi.getList()
    if (graphOptions.value.length > 0) {
      filterGraphId.value = graphOptions.value[0].graphId
    }
  } catch (err) {
    console.error("data.loadFailed", err)
  }
}

const loadInstructions = async () => {
  loading.value = true
  try {
    instructionList.value = await customInstructionApi.list(filterGraphId.value)
  } catch (err: any) {
    message.error(err.message || "common.error")
  } finally {
    loading.value = false
  }
}

const showCreateModal = () => {
  isViewing.value = false
  form.instruction = ""
  form.graphId = filterGraphId.value
  modalVisible.value = true
}

const viewDetail = (record: CustomInstruction) => {
  isViewing.value = true
  form.instruction = record.instruction
  form.graphId = record.graphId
  modalVisible.value = true
}

const handleSave = async () => {
  if (!form.instruction.trim()) {
    message.error("customInstructions.enterContent")
    return
  }
  saving.value = true
  try {
    await customInstructionApi.create(form)
    message.success("common.success")
    modalVisible.value = false
    loadInstructions()
  } catch (err: any) {
    message.error(err.message || "common.error")
  } finally {
    saving.value = false
  }
}

const deleteInstruction = async (id: string) => {
  try {
    await customInstructionApi.delete(id)
    message.success("common.success")
    loadInstructions()
  } catch (err: any) {
    message.error(err.message || "common.error")
  }
}

const getGraphName = (graphId: string): string => {
  const g = graphOptions.value.find(x => x.graphId === graphId)
  return g?.name || graphId
}

const formatDate = (date: string | undefined): string => {
  if (!date) return "-"
  return new Date(date).toLocaleString()
}

onMounted(async () => {
  await loadGraphs()
  loadInstructions()
})
</script>

<style scoped lang="less">
@import "@/assets/styles/dark.less";

.custom-instructions-page {
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

.instruction-text {
  color: @text-secondary;
  font-size: 13px;
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.graph-name {
  color: @text-tertiary;
  font-size: 12px;
  margin-left: 4px;
}
</style>
