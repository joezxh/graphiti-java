<template>
  <DataManagerLayout title="属性管理">
    <template #main-table>
      <PropertyListPanel :graph-id="graphId" @open-property="handleOpenProperty" />
    </template>
    <template #right-panel>
      <PropertyEditor
        v-if="selectedPropertyId"
        :key="selectedPropertyId"
        :graph-id="graphId"
        :property-id="selectedPropertyId"
        @saved="handleSaved"
        @close="selectedPropertyId = null"
      />
      <div v-else class="empty-state">
        <InboxOutlined class="empty-icon" />
        <div class="empty-title">选择一个属性</div>
        <div class="empty-desc">点击左侧列表中的属性查看详情和编辑</div>
      </div>
    </template>
  </DataManagerLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { InboxOutlined } from '@ant-design/icons-vue'
import DataManagerLayout from '@/components/Layout/DataManagerLayout.vue'
import PropertyListPanel from '@/components/Ontology/PropertyListPanel.vue'
import PropertyEditor from '@/components/Ontology/PropertyEditor.vue'

const route = useRoute()
const graphId = ref(route.query.graphId as string || '')
const selectedPropertyId = ref<number | null>(null)

function handleOpenProperty(propertyId: number, _name: string) {
  selectedPropertyId.value = propertyId
}

function handleSaved() {
  // PropertyEditor saved
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
