<template>
  <a-modal
    :open="visible"
    :title="isEdit ? '编辑剧集类型' : '新建剧集类型'"
    :confirm-loading="submitting"
    :ok-text="isEdit ? '保存' : '创建'"
    @ok="handleSubmit"
    @update:open="handleVisibleChange"
    width="560px"
  >
    <a-form
      ref="formRef"
      :model="form"
      :rules="rules"
      layout="horizontal"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 18 }"
    >
      <a-form-item :label="$t('episodeType.typeCode')" name="typeCode">
        <a-input v-model:value="form.typeCode" :placeholder="$t('episodeType.placeholderTypeCode')" :disabled="isEdit" />
      </a-form-item>

      <a-form-item :label="$t('episodeType.typeName')" name="typeName">
        <a-input v-model:value="form.typeName" :placeholder="$t('episodeType.placeholderTypeName')" />
      </a-form-item>

      <a-form-item :label="$t('episodeType.typeNameEn')" name="typeNameEn">
        <a-input v-model:value="form.typeNameEn" :placeholder="$t('episodeType.placeholderTypeNameEn')" />
      </a-form-item>

      <a-form-item :label="$t('episodeType.parentType')" name="parentTypeCode">
        <a-tree-select
          v-model:value="form.parentTypeCode"
          :tree-data="parentOptions"
          :placeholder="$t('episodeType.placeholderParentType')"
          allow-clear
          tree-default-expand-all
          @change="handleParentChange"
        />
      </a-form-item>

      <a-form-item :label="$t('episodeType.level')" name="level">
        <a-input-number v-model:value="form.level" :min="1" :max="5" style="width: 100%" />
      </a-form-item>

      <a-form-item :label="$t('episodeType.processType')" name="processType">
        <a-input v-model:value="form.processType" :placeholder="$t('episodeType.placeholderProcessType')" />
      </a-form-item>

      <a-form-item :label="$t('episodeType.stageLabel')" name="stageLabel">
        <a-input v-model:value="form.stageLabel" :placeholder="$t('episodeType.placeholderStageLabel')" />
      </a-form-item>

      <a-form-item :label="$t('episodeType.stageLevel')" name="stageLevel">
        <a-input v-model:value="form.stageLevel" :placeholder="$t('episodeType.placeholderStageLevel')" />
      </a-form-item>

      <a-form-item :label="$t('episodeType.isReviewStage')" name="isReviewStage">
        <a-switch v-model:checked="form.isReviewStage" />
      </a-form-item>

      <a-form-item label="排序" name="sortOrder">
        <a-input-number v-model:value="form.sortOrder" style="width: 100%" />
      </a-form-item>

      <a-form-item label="状态" name="status">
        <a-select v-model:value="form.status">
          <a-select-option value="ACTIVE">启用</a-select-option>
          <a-select-option value="INACTIVE">禁用</a-select-option>
          <a-select-option value="DEPRECATED">废弃</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="描述" name="description">
        <a-textarea v-model:value="form.description" :rows="3" placeholder="类型描述（可选）" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { message } from 'ant-design-vue'
import { episodeTypeApi } from '@/api/metadata'
import type { OntEpisodeTypeVO } from '@/api/metadata'

interface FormState {
  typeCode: string
  typeName: string
  typeNameEn: string
  parentTypeCode: string | undefined
  level: number
  processType: string
  stageLabel: string
  stageLevel: string
  isReviewStage: boolean
  sortOrder: number
  status: string
  description: string
}

const props = defineProps<{
  visible: boolean
  graphId: string
  definitionId?: number
  typeData?: OntEpisodeTypeVO
  allTypes?: OntEpisodeTypeVO[]
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'success'): void
}>()

const formRef = ref<any>(null)
const submitting = ref(false)

const isEdit = computed(() => !!props.typeData?.id)

const form = ref<FormState>({
  typeCode: '',
  typeName: '',
  typeNameEn: '',
  parentTypeCode: undefined,
  level: 1,
  processType: '',
  stageLabel: '',
  stageLevel: '',
  isReviewStage: false,
  sortOrder: 0,
  status: 'ACTIVE',
  description: ''
})

const rules = {
  typeCode: [
    { required: true, message: '类型代码不能为空', trigger: 'blur' },
    { max: 50, message: '最多50个字符', trigger: 'blur' }
  ],
  typeName: [
    { required: true, message: '类型名称不能为空', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '状态不能为空', trigger: 'change' }
  ]
}

// 构建父类型选择树（排除自身及子类型，防止循环依赖）
const parentOptions = computed(() => {
  const excludeIds = new Set<number>()
  if (props.typeData?.id) {
    excludeIds.add(props.typeData.id)
    // 收集所有子类型ID
    function collectChildren(nodes: OntEpisodeTypeVO[]) {
      for (const n of nodes) {
        excludeIds.add(n.id)
        if (n.children?.length) collectChildren(n.children)
      }
    }
    if (props.typeData.children?.length) {
      collectChildren(props.typeData.children)
    }
  }

  function buildOptions(nodes: OntEpisodeTypeVO[]): any[] {
    return nodes
      .filter(n => !excludeIds.has(n.id))
      .map(n => ({
        title: n.typeName || n.typeCode,
        value: n.typeCode,
        key: `p-${n.id}`,
        children: n.children?.length ? buildOptions(n.children) : undefined
      }))
  }

  return buildOptions(props.allTypes || [])
})

function handleParentChange(val: string | undefined) {
  if (val && val !== '') {
    const parent = findTypeByCode(props.allTypes || [], val)
    form.value.level = (parent?.level || 1) + 1
  } else {
    form.value.level = 1
  }
}

function findTypeByCode(nodes: OntEpisodeTypeVO[], code: string): OntEpisodeTypeVO | undefined {
  for (const n of nodes) {
    if (n.typeCode === code) return n
    if (n.children?.length) {
      const found = findTypeByCode(n.children, code)
      if (found) return found
    }
  }
  return undefined
}

function resetForm() {
  form.value = {
    typeCode: '',
    typeName: '',
    typeNameEn: '',
    parentTypeCode: undefined,
    level: 1,
    processType: '',
    stageLabel: '',
    stageLevel: '',
    isReviewStage: false,
    sortOrder: 0,
    status: 'ACTIVE',
    description: ''
  }
}

function fillForm(data: OntEpisodeTypeVO) {
  form.value = {
    typeCode: data.typeCode || '',
    typeName: data.typeName || '',
    typeNameEn: data.typeNameEn || '',
    parentTypeCode: data.parentTypeCode || undefined,
    level: data.level || 1,
    processType: data.processType || '',
    stageLabel: data.stageLabel || '',
    stageLevel: data.stageLevel || '',
    isReviewStage: data.isReviewStage || false,
    sortOrder: data.sortOrder || 0,
    status: data.status || 'ACTIVE',
    description: data.description || ''
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  if (!props.definitionId) {
    message.error('缺少本体定义ID')
    return
  }

  submitting.value = true
  try {
    const payload = {
      ...form.value,
      definitionId: props.definitionId
    }
    if (isEdit.value && props.typeData?.id) {
      await episodeTypeApi.update(props.graphId, props.typeData.id, payload)
      message.success('类型已更新')
    } else {
      await episodeTypeApi.create(props.graphId, payload)
      message.success('类型已创建')
    }
    emit('success')
    emit('update:visible', false)
    resetForm()
  } catch (e: any) {
    message.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

function handleVisibleChange(val: boolean) {
  emit('update:visible', val)
  if (!val) resetForm()
}

watch(() => props.visible, (val: boolean) => {
  if (val && props.typeData) {
    fillForm(props.typeData)
  } else if (val) {
    resetForm()
  }
})
</script>

<style scoped lang="less">
:deep(.ant-form-item) {
  margin-bottom: 12px;
}
</style>
