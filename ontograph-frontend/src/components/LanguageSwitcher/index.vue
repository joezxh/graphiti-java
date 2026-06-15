<template>
  <a-dropdown :trigger="['click']" placement="bottomRight">
    <div class="lang-trigger">
      <GlobalOutlined class="lang-icon" />
      <span class="lang-label">{{ currentLabel }}</span>
      <DownOutlined class="lang-arrow" />
    </div>
    <template #overlay>
      <a-menu class="lang-menu" @click="handleSelect">
        <a-menu-item
          v-for="locale in SUPPORTED_LOCALES"
          :key="locale.key"
          :class="{ 'lang-item-active': locale.key === currentLocale }"
        >
          <div class="lang-option">
            <span class="lang-flag">{{ locale.flag }}</span>
            <span class="lang-name">{{ locale.label }}</span>
            <CheckOutlined v-if="locale.key === currentLocale" class="lang-check" />
          </div>
        </a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { DownOutlined, CheckOutlined, GlobalOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import { SUPPORTED_LOCALES, setLocale, getCurrentLocale, type LocaleKey } from '@/i18n'

const { locale } = useI18n()

const currentLocale = computed(() => getCurrentLocale())

const currentLabel = computed(() => {
  const found = SUPPORTED_LOCALES.find(l => l.key === currentLocale.value)
  return found?.flag + ' ' + found?.label
})

const handleSelect = ({ key }: { key: string }) => {
  setLocale(key as LocaleKey)
  locale.value = key
}
</script>

<style scoped lang="less">
.lang-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 6px;
  transition: background 0.2s;
  color: #a4aab8;
  font-size: 13px;

  &:hover {
    background: rgba(94, 106, 210, 0.1);
    color: #eceff6;
  }
}

.lang-icon {
  font-size: 15px;
}

.lang-label {
  font-size: 13px;
}

.lang-arrow {
  font-size: 10px;
  opacity: 0.6;
}

.lang-menu {
  min-width: 160px;
  background: #0f1011;
  border: 1px solid #2a2a30;

  :deep(.ant-dropdown-menu-item) {
    padding: 8px 12px;
    color: #a4aab8;
    border-radius: 4px;
    margin: 2px 4px;

    &:hover {
      background: rgba(94, 106, 210, 0.15);
      color: #eceff6;
    }

    &.lang-item-active {
      background: rgba(94, 106, 210, 0.2);
      color: #eceff6;
    }
  }
}

.lang-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.lang-flag {
  font-size: 16px;
}

.lang-name {
  flex: 1;
  font-size: 13px;
}

.lang-check {
  font-size: 12px;
  color: #5e6ad2;
}
</style>
