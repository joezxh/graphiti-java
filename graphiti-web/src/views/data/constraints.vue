<template>
  <DataManagerLayout title="约束管理">
    <template #main-table>
      <ConstraintListPanel :graph-id="graphId" />
    </template>
    <template #right-panel>
      <div class="empty-state">
        <InboxOutlined class="empty-icon" />
        <div class="empty-title">约束管理</div>
        <div class="empty-desc">在中间表格中操作约束的增删改查</div>
      </div>
    </template>
  </DataManagerLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { InboxOutlined } from '@ant-design/icons-vue'
import DataManagerLayout from '@/components/Layout/DataManagerLayout.vue'
import ConstraintListPanel from '@/components/Ontology/ConstraintListPanel.vue'

const route = useRoute()
const graphId = ref(route.query.graphId as string || '')
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
