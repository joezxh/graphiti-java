<template>
  <a-layout class="graphiti-layout">
    <a-layout-header class="header">
      <Header />
    </a-layout-header>
    <a-layout>
      <a-layout-sider class="sidebar">
        <Sidebar />
      </a-layout-sider>
      <a-layout-content class="content-area">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import Header from './Header.vue'
import Sidebar from './Sidebar.vue'
</script>

<style scoped lang="less">
.graphiti-layout {
  width: 100%;
  height: 100vh;
  background: @bg-base;
  color: @text-primary;
}

.header {
  height: 56px;
  padding: 0 20px;
  background: @bg-header;
  border-bottom: 1px solid @border-color;
  display: flex;
  align-items: center;
}

.sidebar {
  background: @bg-sidebar !important;
  border-right: 1px solid @border-color;
}

.content-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px 20px 20px 56px;
  background: @bg-base;
}
</style>
