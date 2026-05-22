<template>
  <a-modal
    v-model:open="visible"
    :title="episode ? '编辑剧集' : '新建剧集'"
    :confirm-loading="loading"
    width="560px"
    :destroy-on-close="true"
    @ok="handleSubmit"
    @cancel="visible = false"
  >
    <a-form
      ref="formRef"
      :model="formData"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 18 }"
      layout="horizontal"
    >
      <a-form-item label="名称" name="name">
        <a-input v-model:value="formData.name" placeholder="输入剧集名称（可选）" />
      </a-form-item>
      <a-form-item label="内容" name="content" :rules="[{ required: true, message: '请输入剧集内容' }]">
        <a-textarea v-model:value="formData.content" :rows="3" placeholder="输入剧集内容" />
      </a-form-item>
      <a-form-item label="来源" name="source">
        <a-input v-model:value="formData.source" placeholder="如: text, document" />
      </a-form-item>
      <a-divider />
      <a-form-item label="Episode类型" name="episode_type">
        <a-select v-model:value="formData.episode_type" placeholder="选择类型" allow-clear>
          <a-select-option value="EP_TRIAL_1ST">一审</a-select-option>
          <a-select-option value="EP_TRIAL_2ND">二审</a-select-option>
          <a-select-option value="EP_MEDIATION">调解</a-select-option>
          <a-select-option value="EP_NEGOTIATION">协商</a-select-option>
          <a-select-option value="EP_EXECUTION">执行</a-select-option>
          <a-select-option value="EP_FILING">立案</a-select-option>
          <a-select-option value="EP_ARBITRATION">仲裁</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="流程类型" name="process_type">
        <a-select v-model:value="formData.process_type" placeholder="选择流程类型" allow-clear>
          <a-select-option value="litigation">诉讼</a-select-option>
          <a-select-option value="mediation">调解</a-select-option>
          <a-select-option value="arbitration">仲裁</a-select-option>
          <a-select-option value="execution">执行</a-select-option>
          <a-select-option value="business_process">业务流程</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="阶段标签" name="stage_label">
        <a-select v-model:value="formData.stage_label" placeholder="选择阶段标签" allow-clear>
          <a-select-option value="立案">立案</a-select-option>
          <a-select-option value="调解">调解</a-select-option>
          <a-select-option value="庭审">庭审</a-select-option>
          <a-select-option value="判决">判决</a-select-option>
          <a-select-option value="执行">执行</a-select-option>
          <a-select-option value="归档">归档</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="阶段级别" name="stage_level">
        <a-select v-model:value="formData.stage_level" placeholder="选择阶段级别" allow-clear>
          <a-select-option value="一审">一审</a-select-option>
          <a-select-option value="二审">二审</a-select-option>
          <a-select-option value="再审">再审</a-select-option>
          <a-select-option value="终审">终审</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="审查阶段" name="is_review_stage">
        <a-switch v-model:checked="formData.is_review_stage" />
      </a-form-item>
      <a-divider />
      <a-form-item label="开始时间" name="start_time">
        <a-date-picker v-model:value="formData.start_time" show-time format="YYYY-MM-DD HH:mm" style="width: 100%" />
      </a-form-item>
      <a-form-item label="结束时间" name="end_time">
        <a-date-picker v-model:value="formData.end_time" show-time format="YYYY-MM-DD HH:mm" style="width: 100%" />
      </a-form-item>
      <a-form-item label="关联案件" name="case_id">
        <a-input v-model:value="formData.case_id" placeholder="输入关联案件ID（可选）" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { graphApi } from '@/api/graph'

function formatDateForSubmit(value: any): string | undefined {
  if (!value) return undefined
  if (typeof value === 'string') return value
  if (typeof value === 'number') return new Date(value).toISOString().replace('T', ' ').slice(0, 19)
  if (typeof value === 'object' && value.format) {
    return value.format('YYYY-MM-DDTHH:mm:ss')
  }
  return new Date(value).toISOString().replace('T', ' ').slice(0, 19)
}

function parseDate(value: any): any {
  if (!value) return null
  if (typeof value === 'object' && value !== null && 'isValid' in value) return value
  const d = new Date(value)
  return d
}

interface Props {
  visible: boolean
  episode?: any
  graphId: string
}

interface Emits {
  (e: 'update:visible', val: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = ref(props.visible)
const loading = ref(false)
const formRef = ref()

const formData = reactive({
  name: '',
  content: '',
  source: '',
  episode_type: undefined as string | undefined,
  process_type: undefined as string | undefined,
  stage_label: undefined as string | undefined,
  stage_level: undefined as string | undefined,
  is_review_stage: false,
  start_time: null as any,
  end_time: null as any,
  case_id: ''
})

watch(() => props.visible, (val) => {
  visible.value = val
  if (val) {
    resetForm()
    if (props.episode) {
      formData.name = props.episode.name || ''
      formData.content = props.episode.content || ''
      formData.source = props.episode.source || ''
      formData.episode_type = props.episode.episodeType
      formData.process_type = props.episode.processType || props.episode.legalProcess
      formData.stage_label = props.episode.stageLabel
      formData.stage_level = props.episode.stageLevel || props.episode.courtLevel
      formData.is_review_stage = props.episode.isReviewStage || props.episode.isTrialStage || false
      formData.start_time = parseDate(props.episode.startTime)
      formData.end_time = parseDate(props.episode.endTime)
      formData.case_id = props.episode.caseId || ''
    }
  }
})

watch(visible, (val) => {
  emit('update:visible', val)
})

function resetForm() {
  formData.name = ''
  formData.content = ''
  formData.source = ''
  formData.episode_type = undefined
  formData.process_type = undefined
  formData.stage_label = undefined
  formData.stage_level = undefined
  formData.is_review_stage = false
  formData.start_time = null
  formData.end_time = null
  formData.case_id = ''
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const payload: any = {
      name: formData.name,
      content: formData.content,
      source: formData.source || 'text',
      episode_type: formData.episode_type,
      process_type: formData.process_type,
      stage_label: formData.stage_label,
      stage_level: formData.stage_level,
      is_review_stage: formData.is_review_stage,
      start_time: formatDateForSubmit(formData.start_time),
      end_time: formatDateForSubmit(formData.end_time),
      case_id: formData.case_id || undefined
    }

    if (props.episode?.uuid) {
      await graphApi.updateEpisode(props.graphId, props.episode.uuid, payload)
    } else {
      await graphApi.createEpisode(props.graphId, payload)
    }

    visible.value = false
    emit('success')
  } catch (e: any) {
    console.error('保存剧集失败:', e)
    // re-throw so parent can handle
    throw e
  } finally {
    loading.value = false
  }
}
</script>
