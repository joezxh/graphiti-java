<template>
  <div class="stats-card" :class="{ 'is-hoverable': hoverable }" @click="handleClick">
    <div class="stats-card-icon" :style="{ background: iconBg }">
      <component :is="icon" :style="{ color: iconColor }" />
    </div>
    <div class="stats-card-content">
      <div class="stats-card-value" :style="{ color: valueColor }">
        {{ formattedValue }}
      </div>
      <div class="stats-card-label">{{ label }}</div>
    </div>
    <div v-if="trend !== undefined" class="stats-card-trend" :class="trendClass">
      <span class="trend-arrow">{{ trend >= 0 ? '↑' : '↓' }}</span>
      <span class="trend-value">{{ Math.abs(trend) }}%</span>
      <span class="trend-period">{{ trendPeriod }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Component } from 'vue'

interface Props {
  icon: Component
  label: string
  value: number
  iconBg?: string
  iconColor?: string
  valueColor?: string
  trend?: number
  trendPeriod?: string
  hoverable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  iconBg: 'rgba(94, 106, 210, 0.1)',
  iconColor: '#5e6ad2',
  valueColor: '#f7f8f8',
  trendPeriod: '本周',
  hoverable: true
})

const emit = defineEmits<{
  (e: 'click'): void
}>()

const formattedValue = computed(() => {
  const val = props.value
  if (val >= 10000) {
    return (val / 10000).toFixed(1) + 'w'
  }
  if (val >= 1000) {
    return (val / 1000).toFixed(1) + 'k'
  }
  return val.toString()
})

const trendClass = computed(() => {
  if (props.trend === undefined) return ''
  return props.trend >= 0 ? 'trend-up' : 'trend-down'
})

const handleClick = () => {
  emit('click')
}
</script>

<style scoped lang="less">
.stats-card {
  background: #0f1011;
  border: 1px solid #23252a;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.25s cubic-bezier(0.2, 0.65, 0.2, 1);
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(120deg, rgba(94, 106, 210, 0.06) 0%, transparent 50%);
    opacity: 0;
    transition: opacity 0.25s ease;
  }
  
  &.is-hoverable {
    cursor: pointer;
    
    &:hover {
      border-color: #34343a;
      transform: translateY(-2px);
      
      &::before {
        opacity: 1;
      }
    }
  }
}

.stats-card-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.stats-card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.stats-card-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: -0.5px;
}

.stats-card-label {
  font-size: 13px;
  color: #8a8f98;
  margin-top: 4px;
}

.stats-card-trend {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 4px;
  white-space: nowrap;
  flex-shrink: 0;
  
  &.trend-up {
    background: rgba(39, 166, 68, 0.1);
    color: #27a644;
  }
  
  &.trend-down {
    background: rgba(255, 107, 107, 0.1);
    color: #ff6b6b;
  }
}

.trend-period {
  color: #8a8f98;
  margin-left: 2px;
}
</style>
