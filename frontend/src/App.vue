<script setup lang="ts">
import { useRouter } from 'vue-router';
const router = useRouter();
</script>

<template>
  <div class="app-layout">
    <header class="app-header">
      <div class="header-left" @click="router.push('/')" style="cursor:pointer">
        <span class="logo">CA Agent</span>
        <span class="slogan">多 Agent 竞品分析工作台</span>
      </div>
    </header>
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<style>
body {
  margin: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
  background-color: #f5f7fa;
}
* { box-sizing: border-box; }
</style>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.app-header {
  height: 52px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  padding: 0 24px;
  flex-shrink: 0;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.logo {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}
.slogan {
  font-size: 13px;
  color: #909399;
}
.app-main {
  flex: 1;
}
</style>
