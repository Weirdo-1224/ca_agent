<template>
  <div v-if="detail" class="status-header">
    <div class="header-main">
      <div class="header-title">
        <h2>{{ detail.taskName }}</h2>
        <el-tag :type="statusTagType" size="large" effect="dark">{{ statusText }}</el-tag>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="$emit('refresh')" :loading="loading">刷新</el-button>
        <el-button @click="$emit('back')">返回创建页</el-button>
      </div>
    </div>
    <div class="header-meta">
      <el-descriptions :column="{ xs: 1, sm: 2, md: 4 }" size="small" border>
        <el-descriptions-item label="Task ID">
          <span class="task-id" @click="copyId">{{ detail.taskId }}</span>
          <el-icon class="copy-icon" @click="copyId"><DocumentCopy /></el-icon>
        </el-descriptions-item>
        <el-descriptions-item label="领域">{{ detail.domain }}</el-descriptions-item>
        <el-descriptions-item label="目标产品">
          <el-tag v-for="p in detail.targetProducts" :key="p" size="small" class="product-tag">{{ p }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="修复轮次">{{ detail.iterationCount }} / {{ detail.maxIterations }}</el-descriptions-item>
      </el-descriptions>
    </div>
    <div v-if="isRunning" class="running-tip">
      <el-alert type="info" :closable="false" show-icon>
        <template #title>任务正在执行，当前阶段：{{ currentAgent }}。结果会在完成后逐步展示。</template>
      </el-alert>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Refresh, DocumentCopy } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import type { TaskDetailResponse } from '@/types';
import { getStatusTagType, getStatusText, getCurrentAgentByStatus, isRunningStatus } from '@/utils/status';

const props = defineProps<{
  detail: TaskDetailResponse | null;
  loading?: boolean;
}>();

defineEmits<{
  refresh: [];
  back: [];
}>();

const statusTagType = computed(() => getStatusTagType(props.detail?.status));
const statusText = computed(() => getStatusText(props.detail?.status));
const currentAgent = computed(() => getCurrentAgentByStatus(props.detail?.status));
const isRunning = computed(() => isRunningStatus(props.detail?.status));

function copyId() {
  if (props.detail?.taskId) {
    navigator.clipboard.writeText(props.detail.taskId);
    ElMessage.success('Task ID 已复制');
  }
}
</script>

<style scoped>
.status-header {
  margin-bottom: 16px;
}
.header-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-title h2 {
  margin: 0;
  font-size: 20px;
}
.header-meta {
  margin-bottom: 12px;
}
.task-id {
  font-family: monospace;
  font-size: 12px;
  color: #606266;
  cursor: pointer;
}
.copy-icon {
  margin-left: 6px;
  cursor: pointer;
  color: #409eff;
}
.product-tag {
  margin-right: 4px;
}
.running-tip {
  margin-top: 8px;
}
</style>
