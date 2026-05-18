<template>
  <a-modal
    :open="visible"
    :title="isEdit ? '编辑类' : '创建类'"
    width="800px"
    :footer="null"
    @cancel="handleClose"
  >
    <div class="schema-editor-modal">
      <!-- Tab Navigation -->
      <div class="editor-tabs">
        <div
          v-for="tab in tabs"
          :key="tab.key"
          class="editor-tab"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </div>
      </div>

      <!-- Basic Info Tab -->
      <div v-show="activeTab === 'basic'" class="tab-content">
        <a-form layout="vertical">
          <a-form-item label="类名称">
            <a-input
              v-model:value="classForm.localName"
              placeholder="请输入类名称"
            />
          </a-form-item>
          
          <a-form-item label="类描述">
            <a-textarea
              v-model:value="classForm.description"
              placeholder="请输入类描述"
              :rows="3"
            />
          </a-form-item>
          
          <a-form-item label="父类 (多继承)">
            <div class="parent-classes">
              <div
                v-for="(parent, index) in classForm.parentClassIds"
                :key="index"
                class="parent-item"
              >
                <a-select
                  v-model:value="classForm.parentClassIds[index]"
                  placeholder="选择父类"
                  style="flex: 1"
                  size="small"
                >
                  <a-select-option
                    v-for="cls in availableParents"
                    :key="cls.id"
                    :value="cls.id"
                  >
                    {{ cls.localName }}
                  </a-select-option>
                </a-select>
                <a-button
                  type="text"
                  size="small"
                  danger
                  @click="removeParent(index)"
                >
                  <template #icon><MinusOutlined /></template>
                </a-button>
              </div>
              <a-button type="link" size="small" @click="addParent">
                <template #icon><PlusOutlined /></template>
                添加父类
              </a-button>
            </div>
          </a-form-item>
        </a-form>
      </div>

      <!-- Inheritance Graph Tab -->
      <div v-show="activeTab === 'inheritance'" class="tab-content inheritance-tab">
        <svg class="inheritance-graph" ref="inheritanceRef">
          <!-- Will be rendered by JS -->
          <g v-if="selectedClass" :transform="`translate(${graphWidth/2}, 40)`">
            <!-- Root node -->
            <g class="class-node" @click="selectInheritanceNode('Thing')">
              <rect x="-50" y="-15" width="100" height="30" rx="6" />
              <text x="0" y="5" text-anchor="middle">Thing</text>
            </g>
          </g>
          
          <g v-if="selectedClass" :transform="`translate(${graphWidth/2 - 100}, 120)`">
            <!-- Second level -->
            <g class="class-node">
              <rect x="-50" y="-15" width="100" height="30" rx="6" />
              <text x="0" y="5" text-anchor="middle">Agent</text>
            </g>
          </g>
          
          <g v-if="selectedClass" :transform="`translate(${graphWidth/2 + 100}, 120)`">
            <!-- Second level -->
            <g class="class-node">
              <rect x="-50" y="-15" width="100" height="30" rx="6" />
              <text x="0" y="5" text-anchor="middle">Entity</text>
            </g>
          </g>
          
          <g v-if="selectedClass" :transform="`translate(${graphWidth/2}, 200)`">
            <!-- Current class -->
            <g class="class-node selected">
              <rect x="-60" y="-15" width="120" height="30" rx="6" />
              <text x="0" y="5" text-anchor="middle">{{ selectedClass.localName }}</text>
            </g>
          </g>
          
          <!-- Lines -->
          <line v-if="selectedClass" x1="400" y1="55" x2="400" y2="105" />
          <line v-if="selectedClass" x1="350" y1="120" x2="400" y2="120" />
          <line v-if="selectedClass" x1="450" y1="120" x2="400" y2="120" />
          <line v-if="selectedClass" x1="400" y1="135" x2="400" y2="185" />
        </svg>
      </div>

      <!-- Properties Tab -->
      <div v-show="activeTab === 'properties'" class="tab-content">
        <div class="properties-header">
          <span class="properties-title">属性列表</span>
          <a-button type="primary" size="small" @click="showAddProperty = true">
            <template #icon><PlusOutlined /></template>
            添加属性
          </a-button>
        </div>
        
        <div class="properties-table">
          <div class="table-header">
            <span class="col-name">名称</span>
            <span class="col-type">类型</span>
            <span class="col-required">必填</span>
            <span class="col-actions">操作</span>
          </div>
          
          <div
            v-for="prop in properties"
            :key="prop.id"
            class="table-row"
          >
            <span class="col-name">{{ prop.localName }}</span>
            <span class="col-type">
              <a-tag :color="getPropertyTypeColor(prop.propertyType)">
                {{ prop.rangeDataType || prop.propertyType }}
              </a-tag>
            </span>
            <span class="col-required">
              <a-tag :color="prop.isRequired ? 'red' : 'default'">
                {{ prop.isRequired ? '是' : '否' }}
              </a-tag>
            </span>
            <span class="col-actions">
              <a-button type="link" size="small" @click="editProperty(prop)">
                编辑
              </a-button>
              <a-popconfirm
                title="确定删除此属性？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="deleteProperty(prop.id)"
              >
                <a-button type="link" size="small" danger>
                  删除
                </a-button>
              </a-popconfirm>
            </span>
          </div>
          
          <div v-if="properties.length === 0" class="empty-properties">
            暂无属性定义
          </div>
        </div>
      </div>

      <!-- Actions -->
      <div class="modal-actions">
        <a-button @click="handleClose">取消</a-button>
        <a-button type="primary" :loading="loading" @click="handleSave">
          {{ isEdit ? '保存' : '创建' }}
        </a-button>
      </div>
    </div>

    <!-- Add Property Modal -->
    <a-modal
      v-model:open="showAddProperty"
      :title="editingProperty ? '编辑属性' : '添加属性'"
      width="480px"
      @ok="handleSaveProperty"
      @cancel="showAddProperty = false; editingProperty = null"
    >
      <a-form layout="vertical">
        <a-form-item label="属性名称" required>
          <a-input v-model:value="propertyForm.localName" placeholder="请输入属性名称" />
        </a-form-item>
        
        <a-form-item label="属性类型">
          <a-select v-model:value="propertyForm.propertyType">
            <a-select-option value="DATATYPE">数据类型属性</a-select-option>
            <a-select-option value="OBJECT">对象属性</a-select-option>
            <a-select-option value="ANNOTATION">注解属性</a-select-option>
          </a-select>
        </a-form-item>
        
        <a-form-item label="数据类型" v-if="propertyForm.propertyType === 'DATATYPE'">
          <a-select v-model:value="propertyForm.rangeDataType">
            <a-select-option value="string">字符串</a-select-option>
            <a-select-option value="integer">整数</a-select-option>
            <a-select-option value="float">浮点数</a-select-option>
            <a-select-option value="boolean">布尔值</a-select-option>
            <a-select-option value="date">日期</a-select-option>
            <a-select-option value="json">JSON</a-select-option>
          </a-select>
        </a-form-item>
        
        <a-form-item label="必填">
          <a-switch v-model:checked="propertyForm.isRequired" />
        </a-form-item>
        
        <a-form-item label="默认值">
          <a-input v-model:value="propertyForm.defaultValue" placeholder="可选" />
        </a-form-item>
        
        <a-form-item label="枚举值" v-if="propertyForm.propertyType === 'DATATYPE'">
          <a-select
            v-model:value="propertyForm.allowedValues"
            mode="tags"
            placeholder="输入枚举值后按回车"
          />
        </a-form-item>
        
        <a-form-item label="数值范围" v-if="['integer', 'float'].includes(propertyForm.rangeDataType || '')">
          <a-space>
            <a-input-number v-model:value="propertyForm.minValue" placeholder="最小值" style="width: 100px" />
            <span>~</span>
            <a-input-number v-model:value="propertyForm.maxValue" placeholder="最大值" style="width: 100px" />
          </a-space>
        </a-form-item>
        
        <a-form-item label="正则表达式" v-if="propertyForm.rangeDataType === 'string'">
          <a-input v-model:value="propertyForm.pattern" placeholder="如: ^[a-zA-Z]+$" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, MinusOutlined } from '@ant-design/icons-vue'
import { graphApi } from '@/api/graph'
import type { SchemaClass, SchemaProperty } from '@/api/graph'

interface Props {
  visible: boolean
  graphId: string
  selectedClass: SchemaClass | null
  classes: SchemaClass[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

// Refs
const loading = ref(false)
const activeTab = ref('basic')
const showAddProperty = ref(false)
const editingProperty = ref<SchemaProperty | null>(null)
const properties = ref<SchemaProperty[]>([])
const inheritanceRef = ref<HTMLElement>()
const graphWidth = ref(800)

// Tabs
const tabs = [
  { key: 'basic', label: '基本信息' },
  { key: 'inheritance', label: '继承关系' },
  { key: 'properties', label: '属性配置' }
]

// Form state
const classForm = reactive({
  localName: '',
  description: '',
  parentClassIds: [] as number[]
})

const propertyForm = reactive({
  localName: '',
  propertyType: 'DATATYPE' as SchemaProperty['propertyType'],
  rangeDataType: 'string',
  isRequired: false,
  defaultValue: '',
  allowedValues: [] as string[],
  minValue: undefined as number | undefined,
  maxValue: undefined as number | undefined,
  pattern: ''
})

// Computed
const isEdit = computed(() => !!props.selectedClass)

const availableParents = computed(() => {
  return props.classes.filter(cls => cls.id !== props.selectedClass?.id)
})

// Watch visible
watch(() => props.visible, (val) => {
  if (val) {
    activeTab.value = 'basic'
    if (props.selectedClass) {
      classForm.localName = props.selectedClass.localName
      classForm.description = props.selectedClass.description || ''
      classForm.parentClassIds = [...(props.selectedClass.parentClassIds || [])]
      loadProperties()
    } else {
      classForm.localName = ''
      classForm.description = ''
      classForm.parentClassIds = []
      properties.value = []
    }
    resetPropertyForm()
  }
})

// Load properties
const loadProperties = async () => {
  if (!props.selectedClass) return
  try {
    const result = await graphApi.getClassProperties(props.graphId, props.selectedClass.id)
    properties.value = result
  } catch (error) {
    console.error('加载属性失败:', error)
  }
}

// Methods
const addParent = () => {
  classForm.parentClassIds.push(0)
}

const removeParent = (index: number) => {
  classForm.parentClassIds.splice(index, 1)
}

const getPropertyTypeColor = (type: string): string => {
  const colors: Record<string, string> = {
    DATATYPE: 'blue',
    OBJECT: 'purple',
    ANNOTATION: 'green'
  }
  return colors[type] || 'default'
}

const editProperty = (prop: SchemaProperty) => {
  editingProperty.value = prop
  propertyForm.localName = prop.localName
  propertyForm.propertyType = prop.propertyType
  propertyForm.rangeDataType = prop.rangeDataType || 'string'
  propertyForm.isRequired = prop.isRequired
  propertyForm.defaultValue = prop.defaultValue || ''
  propertyForm.allowedValues = prop.allowedValues || []
  propertyForm.minValue = prop.minValue
  propertyForm.maxValue = prop.maxValue
  propertyForm.pattern = prop.pattern || ''
  showAddProperty.value = true
}

const deleteProperty = async (propertyId: number) => {
  if (!props.selectedClass) return
  
  // 先验证影响
  try {
    const validation = await graphApi.validateSchemaChange(props.graphId, {
      type: 'DELETE_PROPERTY',
      propertyId: propertyId
    })
    
    if (!validation.compatible) {
      const confirmed = await new Promise((resolve) => {
        const msg = `此操作将丢失 ${validation.affectedNodes} 个节点的属性数据。\n\n确定要继续删除吗？`
        resolve(confirm(msg))
      })
      
      if (!confirmed) {
        return
      }
    }
  } catch (error) {
    console.error('验证失败:', error)
  }
  
  try {
    await graphApi.deleteClassProperty(props.graphId, props.selectedClass.id, propertyId)
    properties.value = properties.value.filter(p => p.id !== propertyId)
    message.success('删除成功')
  } catch (error) {
    message.error('删除失败')
  }
}

const resetPropertyForm = () => {
  propertyForm.localName = ''
  propertyForm.propertyType = 'DATATYPE'
  propertyForm.rangeDataType = 'string'
  propertyForm.isRequired = false
  propertyForm.defaultValue = ''
  propertyForm.allowedValues = []
  propertyForm.minValue = undefined
  propertyForm.maxValue = undefined
  propertyForm.pattern = ''
  editingProperty.value = null
}

const handleSaveProperty = async () => {
  if (!propertyForm.localName) {
    message.warning('请输入属性名称')
    return
  }
  
  if (!props.selectedClass) {
    message.warning('请先保存类')
    return
  }
  
  // 如果修改必填属性，先验证影响范围
  if (editingProperty.value && propertyForm.isRequired && !editingProperty.value.isRequired) {
    try {
      const validation = await graphApi.validateSchemaChange(props.graphId, {
        type: 'UPDATE_PROPERTY',
        propertyId: editingProperty.value.id,
        changes: {
          oldIsRequired: editingProperty.value.isRequired,
          newIsRequired: propertyForm.isRequired
        }
      })
      
      if (!validation.compatible) {
        // 显示警告
        const confirmed = await new Promise((resolve) => {
          const msg = `此变更将影响 ${validation.affectedNodes} 个节点。\n\n冲突详情:\n${validation.violations.slice(0, 5).map(v => `- ${v.nodeName}: ${v.reason}`).join('\n')}${validation.violations.length > 5 ? '\n... 还有更多' : ''}\n\n确定要继续吗？`
          resolve(confirm(msg))
        })
        
        if (!confirmed) {
          return
        }
      }
    } catch (error) {
      console.error('验证失败:', error)
      // 验证失败时继续执行
    }
  }
  
  try {
    if (editingProperty.value) {
      // Update property
      const updated = await graphApi.updateClassProperty(
        props.graphId,
        props.selectedClass.id,
        editingProperty.value.id,
        {
          localName: propertyForm.localName,
          propertyType: propertyForm.propertyType,
          rangeDataType: propertyForm.rangeDataType,
          isRequired: propertyForm.isRequired,
          defaultValue: propertyForm.defaultValue,
          allowedValues: propertyForm.allowedValues,
          minValue: propertyForm.minValue,
          maxValue: propertyForm.maxValue,
          pattern: propertyForm.pattern
        }
      )
      const index = properties.value.findIndex(p => p.id === editingProperty.value!.id)
      if (index >= 0) properties.value[index] = updated
      message.success('更新成功')
    } else {
      // 如果是添加必填属性，先验证
      if (propertyForm.isRequired) {
        try {
          // 先创建属性获取 ID
          const created = await graphApi.createClassProperty(
            props.graphId,
            props.selectedClass.id,
            {
              localName: propertyForm.localName,
              propertyType: propertyForm.propertyType,
              rangeDataType: propertyForm.rangeDataType,
              isRequired: propertyForm.isRequired,
              defaultValue: propertyForm.defaultValue,
              allowedValues: propertyForm.allowedValues,
              minValue: propertyForm.minValue,
              maxValue: propertyForm.maxValue,
              pattern: propertyForm.pattern
            }
          )
          
          // 验证影响
          const validation = await graphApi.validateSchemaChange(props.graphId, {
            type: 'ADD_REQUIRED_PROPERTY',
            classId: props.selectedClass.id,
            propertyId: created.id
          })
          
          if (!validation.compatible) {
            message.warning(`注意：此必填属性将在 ${validation.affectedNodes} 个现有节点上缺失`)
          }
          
          properties.value.push(created)
          message.success('添加成功')
        } catch (error) {
          message.error('添加失败')
          return
        }
      } else {
        const created = await graphApi.createClassProperty(
          props.graphId,
          props.selectedClass.id,
          {
            localName: propertyForm.localName,
            propertyType: propertyForm.propertyType,
            rangeDataType: propertyForm.rangeDataType,
            isRequired: propertyForm.isRequired,
            defaultValue: propertyForm.defaultValue,
            allowedValues: propertyForm.allowedValues,
            minValue: propertyForm.minValue,
            maxValue: propertyForm.maxValue,
            pattern: propertyForm.pattern
          }
        )
        properties.value.push(created)
        message.success('添加成功')
      }
    }
    
    showAddProperty.value = false
    resetPropertyForm()
  } catch (error) {
    message.error('操作失败')
  }
}

const handleSave = async () => {
  if (!classForm.localName) {
    message.warning('请输入类名称')
    return
  }
  
  loading.value = true
  try {
    if (isEdit.value && props.selectedClass) {
      // Update class
      await graphApi.updateSchemaClass(props.graphId, props.selectedClass.id, {
        localName: classForm.localName,
        description: classForm.description,
        parentClassIds: classForm.parentClassIds.filter(id => id > 0)
      })
      message.success('更新成功')
    } else {
      // Create class
      await graphApi.createSchemaClass(props.graphId, {
        localName: classForm.localName,
        description: classForm.description,
        parentClassIds: classForm.parentClassIds.filter(id => id > 0)
      })
      message.success('创建成功')
    }
    
    emit('success')
    handleClose()
  } catch (error) {
    message.error('操作失败')
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  emit('update:visible', false)
}
</script>

<style scoped lang="less">
.schema-editor-modal {
  .editor-tabs {
    display: flex;
    gap: 4px;
    margin-bottom: 20px;
    border-bottom: 1px solid #30363d;
    padding-bottom: 12px;
  }
  
  .editor-tab {
    padding: 8px 16px;
    font-size: 13px;
    color: #8b949e;
    cursor: pointer;
    border-radius: 6px;
    transition: all 0.2s;
    
    &:hover {
      color: #e6edf3;
      background: #21262d;
    }
    
    &.active {
      color: #58a6ff;
      background: rgba(88, 166, 255, 0.15);
    }
  }
  
  .tab-content {
    min-height: 300px;
  }
  
  .inheritance-tab {
    background: #0d1117;
    border-radius: 8px;
    padding: 20px;
  }
  
  .inheritance-graph {
    width: 100%;
    height: 280px;
    
    .class-node {
      cursor: pointer;
      
      rect {
        fill: #161b22;
        stroke: #30363d;
        stroke-width: 2;
        transition: all 0.2s;
      }
      
      text {
        fill: #e6edf3;
        font-size: 12px;
      }
      
      &:hover rect {
        stroke: #58a6ff;
      }
      
      &.selected rect {
        stroke: #58a6ff;
        stroke-width: 3;
        filter: drop-shadow(0 0 8px rgba(88, 166, 255, 0.5));
      }
    }
    
    line {
      stroke: #30363d;
      stroke-width: 2;
    }
  }
  
  .properties-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    
    .properties-title {
      font-size: 14px;
      font-weight: 600;
    }
  }
  
  .properties-table {
    background: #0d1117;
    border-radius: 8px;
    overflow: hidden;
    
    .table-header {
      display: flex;
      padding: 10px 12px;
      background: #161b22;
      font-size: 12px;
      font-weight: 600;
      color: #6e7681;
    }
    
    .table-row {
      display: flex;
      padding: 10px 12px;
      border-bottom: 1px solid #21262d;
      align-items: center;
      
      &:last-child {
        border-bottom: none;
      }
    }
    
    .col-name {
      flex: 1;
      color: #58a6ff;
    }
    
    .col-type {
      width: 100px;
    }
    
    .col-required {
      width: 60px;
    }
    
    .col-actions {
      width: 120px;
      text-align: right;
    }
    
    .empty-properties {
      padding: 40px;
      text-align: center;
      color: #6e7681;
    }
  }
  
  .modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    padding-top: 20px;
    border-top: 1px solid #30363d;
    margin-top: 20px;
  }
  
  .parent-classes {
    .parent-item {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
    }
  }
}

:deep(.ant-form-item-label > label) {
  color: #8b949e;
}

:deep(.ant-input),
:deep(.ant-select-selector),
:deep(.ant-input-number),
:deep(.ant-input-textarea) {
  background: #21262d !important;
  border-color: #30363d !important;
  color: #e6edf3 !important;
}

:deep(.ant-select-dropdown) {
  background: #161b22;
}

:deep(.ant-select-item) {
  color: #e6edf3;
}

:deep(.ant-modal-content) {
  background: #161b22;
}

:deep(.ant-modal-header) {
  background: #161b22;
  border-bottom: 1px solid #30363d;
  
  .ant-modal-title {
    color: #e6edf3;
  }
}
</style>
