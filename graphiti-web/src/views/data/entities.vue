<template>
  <DataManagerLayout title="实体管理">
    <template #main-table>
      <InstanceDataTable
        :graph-id="graphId"
        :class-type="selectedClassType"
        @edit-instance="handleEditInstance"
      />
    </template>
    <template #right-panel>
      <InstanceForm
        v-if="selectedInstance"
        :graph-id="graphId"
        :instance-data="selectedInstance"
        @saved="handleSaved"
      />
      <div v-else class="empty-state">
        <InboxOutlined class="empty-icon" />
        <div class="empty-title">选择一个实体</div>
        <div class="empty-desc">点击表格中的实体进行编辑</div>
      </div>
    </template>
  </DataManagerLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { InboxOutlined } from '@ant-design/icons-vue'
import DataManagerLayout from '@/components/Layout/DataManagerLayout.vue'
import InstanceDataTable from '@/components/Ontology/InstanceDataTable.vue'
import InstanceForm from '@/components/Ontology/InstanceForm.vue'

const route = useRoute()
const graphId = ref(route.query.graphId as string || '')
const selectedClassType = ref<string | undefined>(undefined)
const selectedInstance = ref<any | null>(null)

function handleEditInstance(instance: any) {
  selectedInstance.value = instance
}

function handleSaved() {
  selectedInstance.value = null
}
</script>

<style scoped lang="less">
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;

  .empty-icon {
    font-size: 48px;
    color: #30363d;
    margin-bottom: 16px;
  }

  .empty-title {
    font-size: 16px;
    font-weight: 500;
    color: #e6edf3;
    margin-bottom: 8px;
  }

  .empty-desc {
    font-size: 13px;
    color: #6e7681;
  }
}
</style>
