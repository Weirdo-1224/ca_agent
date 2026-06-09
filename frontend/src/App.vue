<script setup lang="ts">
import { useRouter } from 'vue-router';
const router = useRouter();
</script>

<template>
  <div class="app-layout">
    <header class="app-header">
      <div class="header-inner">
        <div class="header-left" @click="router.push('/')" style="cursor: pointer">
          <span class="logo">CA Agent</span>
          <span class="slogan">多 Agent 竞品分析工作台</span>
        </div>
        <div class="header-right">
          <span class="env-badge">Local Demo</span>
        </div>
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
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  background: linear-gradient(160deg, #f5f7fb 0%, #eef3ff 50%, #f0f4ff 100%);
  min-height: 100vh;
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
  position: sticky;
  top: 0;
  z-index: 100;
  height: 64px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  padding: 0 24px;
  flex-shrink: 0;
}
.header-inner {
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.logo {
  font-size: 18px;
  font-weight: 700;
  color: #1e1b4b;
  letter-spacing: -0.3px;
}
.slogan {
  font-size: 13px;
  color: #6b7280;
  font-weight: 400;
}
.header-right {
  display: flex;
  align-items: center;
}
.env-badge {
  display: inline-block;
  padding: 4px 10px;
  background: #f0fdf4;
  color: #15803d;
  border: 1px solid #bbf7d0;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}
.app-main {
  flex: 1;
}
</style>
