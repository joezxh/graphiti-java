<template>
  <a-modal
    v-model:open="visible"
    :title="episode ? t('episodeEdit.titleEdit') : t('episodeEdit.titleNew')"
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
      <a-form-item :label="t('episodeEdit.labelName')" name="name">
        <a-input v-model:value="formData.name" :placeholder="t('episodeEdit.placeholderName')" />
      </a-form-item>
      <a-form-item :label="t('episodeEdit.labelContent')" name="content" :rules="[{ required: true, message: t('episodeEdit.requiredContent') }]">
        <a-textarea v-model:value="formData.content" :rows="3" :placeholder="t('episodeEdit.placeholderContent')" />
      </a-form-item>
      <a-form-item :label="t('episodeEdit.labelSource')" name="source">
        <a-input v-model:value="formData.source" placeholder="text, document" />
      </a-form-item>
      <a-divider />
      <a-form-item :label="t('episodeEdit.labelEpisodeType')" name="episode_type">
        <a-select v-model:value="formData.episode_type" :placeholder="t('episodeEdit.placeholderSelectType')" allow-clear>
          <a-select-option value="EP_TRIAL_1ST">{{ t('episodeEdit.typeTrial1st') }}</a-select-option>
          <a-select-option value="EP_TRIAL_2ND">{{ t('episodeEdit.typeTrial2nd') }}</a-select-option>
          <a-select-option value="EP_MEDIATION">{{ t('episodeEdit.typeMediation') }}</a-select-option>
          <a-select-option value="EP_NEGOTIATION">{{ t('episodeEdit.typeNegotiation') }}</a-select-option>
          <a-select-option value="EP_EXECUTION">{{ t('episodeEdit.typeExecution') }}</a-select-option>
          <a-select-option value="EP_FILING">{{ t('episodeEdit.typeFiling') }}</a-select-option>
          <a-select-option value="EP_ARBITRATION">{{ t('episodeEdit.typeArbitration') }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item :label="t('episodeEdit.labelProcessType')" name="process_type">
        <a-select v-model:value="formData.process_type" :placeholder="t('episodeEdit.placeholderSelectProcess')" allow-clear>
          <a-select-option value="litigation">{{ t('episodeEdit.processLitigation') }}</a-select-option>
          <a-select-option value="mediation">{{ t('episodeEdit.processMediation') }}</a-select-option>
          <a-select-option value="arbitration">{{ t('episodeEdit.processArbitration') }}</a-select-option>
          <a-select-option value="execution">{{ t('episodeEdit.processExecution') }}</a-select-option>
          <a-select-option value="business_process">{{ t('episodeEdit.processBusiness') }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item :label="t('episodeEdit.labelStageLabel')" name="stage_label">
        <a-select v-model:value="formData.stage_label" :placeholder="t('episodeEdit.placeholderSelectStage')" allow-clear>
          <a-select-option value="立案">{{ t('episodeEdit.stageFiling') }}</a-select-option>
          <a-select-option value="调解">{{ t('episodeEdit.stageMediation') }}</a-select-option>
          <a-select-option value="庭审">{{ t('episodeEdit.stageTrial') }}</a-select-option>
          <a-select-option value="判决">{{ t('episodeEdit.stageJudgment') }}</a-select-option>
          <a-select-option value="执行">{{ t('episodeEdit.stageExecution') }}</a-select-option>
          <a-select-option value="归档">{{ t('episodeEdit.stageArchive') }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item :label="t('episodeEdit.labelStageLevel')" name="stage_level">
        <a-select v-model:value="formData.stage_level" :placeholder="t('episodeEdit.placeholderSelectLevel')" allow-clear>
          <a-select-option value="一审">{{ t('episodeEdit.level1st') }}</a-select-option>
          <a-select-option value="二审">{{ t('episodeEdit.level2nd') }}</a-select-option>
          <a-select-option value="再审">{{ t('episodeEdit.levelRetrial') }}</a-select-option>
          <a-select-option value="终审">{{ t('episodeEdit.levelFinal') }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item :label="t('episodeEdit.labelReviewStage')" name="is_review_stage">
        <a-switch v-model:checked="formData.is_review_stage" />
      </a-form-item>
      <a-divider />
      <a-form-item :label="t('episodeEdit.labelStartTime')" name="start_time">
        <a-date-picker v-model:value="formData.start_time" show-time format="YYYY-MM-DD HH:mm" style="width: 100%" />
      </a-form-item>
      <a-form-item :label="t('episodeEdit.labelEndTime')" name="end_time">
        <a-date-picker v-model:value="formData.end_time" show-time format="YYYY-MM-DD HH:mm" style="width: 100%" />
      </a-form-item>
      <a-form-item :label="t('episodeEdit.labelCaseId')" name="case_id">
        <a-input v-model:value="formData.case_id" :placeholder="t('episodeEdit.placeholderCaseId')" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { graphApi } from '@/api/graph'

const { t } = useI18n()

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
