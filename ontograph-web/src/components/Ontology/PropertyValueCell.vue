<template>
  <div v-if="!editing" class="cell-display" @dblclick="startEdit">
    {{ displayValue }}
  </div>
  <div v-else class="cell-editor">
    <a-switch
      v-if="isBool"
      v-model:checked="editValue"
      size="small"
      @change="save"
    />
    <a-input-number
      v-else-if="isNumeric"
      v-model:value="editValue"
      size="small"
      style="width: 100%"
      @pressEnter="save"
      @blur="save"
    />
    <a-date-picker
      v-else-if="isDate"
      v-model:value="editValue"
      size="small"
      style="width: 100%"
      @change="save"
    />
    <a-select
      v-else-if="hasEnum"
      v-model:value="editValue"
      size="small"
      style="width: 100%"
      :options="enumOptions"
      @change="save"
    />
    <a-input
      v-else
      v-model:value="editValue"
      size="small"
      @pressEnter="save"
      @blur="save"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import dayjs from 'dayjs'
import {
  isBoolType,
  isNumericType,
  isDateType,
  formatPropertyValue,
  parsePropertyValue,
  serializePropertyValue
} from '@/composables/usePropertyType'
import type { OntPropertyVO } from '@/api/ontology'

const props = defineProps<{
  value: any
  propDef: OntPropertyVO
  editing?: boolean
}>()

const emit = defineEmits<{
  (e: 'update', value: any): void
  (e: 'start-edit'): void
}>()

const editValue = ref<any>(null)

const isBool = computed(() => isBoolType(props.propDef.rangeDataType))
const isNumeric = computed(() => isNumericType(props.propDef.rangeDataType))
const isDate = computed(() => isDateType(props.propDef.rangeDataType))
const hasEnum = computed(() => props.propDef.allowedValues && props.propDef.allowedValues.length > 0)

const enumOptions = computed(() =>
  (props.propDef.allowedValues || []).map(v => ({ label: v, value: v }))
)

const displayValue = computed(() =>
  formatPropertyValue(props.value, props.propDef.propertyType, props.propDef.rangeDataType)
)

watch(() => props.editing, (editing) => {
  if (editing) {
    editValue.value = parsePropertyValue(props.value, props.propDef.rangeDataType)
  }
})

function startEdit() {
  emit('start-edit')
}

function save() {
  const serialized = serializePropertyValue(editValue.value, props.propDef.rangeDataType)
  emit('update', serialized)
}
</script>

<style scoped lang="less">
.cell-display {
  cursor: text;
  padding: 2px 4px;
  border-radius: 3px;
  min-height: 24px;
  transition: background 0.15s;

  &:hover {
    background: rgba(88, 166, 255, 0.1);
  }
}

.cell-editor {
  padding: 0;
}
</style>
