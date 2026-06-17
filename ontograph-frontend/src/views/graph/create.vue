<template>
  <div class="graph-create-container">
    <div class="page-header">
      <a-button type="text" @click="goBack">
        <template #icon><ArrowLeftOutlined /></template>
        {{ $t("common.back") }}
      </a-button>
      <h2 class="page-title">{{ $t("graph.newGraph") }}</h2>
    </div>

    <div class="create-form-section">
      <a-card :bordered="false" class="form-card">
        <a-form
          ref="formRef"
          :model="formState"
          :rules="formRules"
          layout="vertical"
          @finish="handleSubmit"
        >
          <a-form-item :label="$t('graph.graphName')" name="name">
            <a-input
              v-model:value="formState.name"
              :placeholder="$t('graph.enterGraphName')"
              size="large"
            />
          </a-form-item>

          <a-form-item :label="$t('graph.graphDesc')" name="description">
            <a-textarea
              v-model:value="formState.description"
              :placeholder="$t('graph.graphDesc')"
              :rows="4"
            />
          </a-form-item>

          <a-form-item>
            <div class="form-actions">
              <a-button @click="goBack">{{ $t("common.cancel") }}</a-button>
              <a-button type="primary" html-type="submit" :loading="submitting">
                {{ $t("graph.createGraph") }}
              </a-button>
            </div>
          </a-form-item>
        </a-form>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, getCurrentInstance } from "vue"
import { useRouter } from "vue-router"
import { message } from "ant-design-vue"
import { ArrowLeftOutlined } from "@ant-design/icons-vue"
import { graphApi, type Graph } from "@/api/graph"

const router = useRouter()
const formRef = ref()
const submitting = ref(false)

const formState = reactive({
  name: "",
  description: ""
})

const formRules = computed(() => {
  const instance = getCurrentInstance()
  const proxy = instance?.proxy
  return {
    name: [
      { required: true, message: proxy?.$t("graph.enterGraphName") || '请输入图谱名称', trigger: "blur" },
      { min: 2, max: 50, message: proxy?.$t("graph.graphNameLength") || '名称长度2-50字符', trigger: "blur" }
    ]
  }
})

const handleSubmit = async () => {
  submitting.value = true
  try {
    await graphApi.create(formState as Graph)
    const proxy = getCurrentInstance()?.proxy
    message.success(proxy?.$t("graph.createSuccess") as string || '创建成功')
    router.push("/graph/list")
  } catch (err: any) {
    const proxy = getCurrentInstance()?.proxy
    message.error(err.message || proxy?.$t("common.error") as string || '操作失败')
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  router.back()
}
</script>

<style scoped lang="less">
@import "@/assets/styles/dark.less";

.graph-create-container {
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: @text-primary;
  margin: 0;
}

.create-form-section {
  max-width: 600px;
}

.form-card {
  background: @bg-container;
  border: 1px solid @border-color;
}

.form-actions {
  display: flex;
  gap: 12px;
}
</style>
