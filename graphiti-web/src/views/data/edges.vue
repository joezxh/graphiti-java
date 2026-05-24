<template>
  <DataManagerLayout title="关系边管理">
    <template #main-table>
      <EdgeListPanel :graph-id="graphId" />
    </template>
  </DataManagerLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import DataManagerLayout from '@/components/Layout/DataManagerLayout.vue'
import EdgeListPanel from '@/components/Ontology/EdgeListPanel.vue'

const route = useRoute()
const graphId = ref(route.query.graphId as string || '')
</script>
