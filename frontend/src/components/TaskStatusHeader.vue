<template>
  <div v-if="detail" class="status-header-card">
    <!-- 顶行：标题 + 状态 + 操作 -->
    <div class="header-top">
      <div class="header-left">
        <h2 class="task-title">{{ detail.taskName }}</h2>
        <span class="status-badge" :class="'status-' + statusColor">{{ statusText }}</span>
      </div>
      <div class="header-actions">
        <button class="btn-icon" @click="$emit('refresh')" :disabled="loading" title="刷新数据">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" :class="{ spinning: loading }"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
        </button>
        <button class="btn-secondary" @click="$emit('back')">
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>
          返回
        </button>
      </div>
    </div>

    <!-- 中间：元信息 -->
    <div class="header-meta">
      <div class="meta-item">
        <span class="meta-label">Task ID</span>
        <span class="meta-value mono" @click="copyId" title="点击复制">
          {{ detail.taskId }}
          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
        </span>
      </div>
      <div class="meta-item">
        <span class="meta-label">领域</span>
        <span class="meta-value">{{ detail.domain }}</span>
      </div>
      <div class="meta-item">
        <span class="meta-label">目标产品</span>
        <span class="meta-value">
          <span v-for="p in detail.targetProducts" :key="p" class="product-chip">{{ p }}</span>
        </span>
      </div>
    </div>

    <!-- 底部：指标卡片 -->
    <div class="metrics-row">
      <div class="metric-card">
        <div class="metric-value">{{ detail.iterationCount }} / {{ detail.maxIterations }}</div>
        <div class="metric-label">修复轮次</div>
      </div>
      <div class="metric-card">
        <div class="metric-value">{{ report?.sections?.length ?? 0 }}</div>
        <div class="metric-label">报告章节</div>
      </div>
      <div class="metric-card">
        <div class="metric-value">{{ evidenceCount }}</div>
        <div class="metric-label">证据数量</div>
      </div>
      <div class="metric-card">
        <div class="metric-value" :class="scoreClass">{{ review?.score ?? '—' }}</div>
        <div class="metric-label">质检分数</div>
      </div>
      <div class="metric-card">
        <div class="metric-value">{{ totalDuration }}</div>
        <div class="metric-label">总耗时</div>
      </div>
    </div>

    <!-- 运行中提示 -->
    <div v-if="isRunning" class="running-banner">
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
      任务正在执行，当前阶段：<strong>{{ currentAgent }}</strong>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { ElMessage } from 'element-plus';
import type { TaskDetailResponse, ReportResponse, Evidence, ReviewResult, AgentRunResponse } from '@/types';
import { getStatusText, getCurrentAgentByStatus, isRunningStatus } from '@/utils/status';
import { formatDuration } from '@/utils/time';

const props = defineProps<{
  detail: TaskDetailResponse | null;
  loading?: boolean;
  agentRuns?: AgentRunResponse[];
  report?: ReportResponse | null;
  evidence?: Evidence[];
  review?: ReviewResult | null;
}>();

defineEmits<{
  refresh: [];
  back: [];
}>();

const statusText = computed(() => getStatusText(props.detail?.status));
const currentAgent = computed(() => getCurrentAgentByStatus(props.detail?.status));
const isRunning = computed(() => isRunningStatus(props.detail?.status));
const evidenceCount = computed(() => props.evidence?.length ?? 0);

const statusColor = computed(() => {
  const s = props.detail?.status;
  if (s === 'COMPLETED') return 'success';
  if (s === 'FAILED') return 'error';
  if (s === 'COMPLETED_WITH_WARNINGS' || s === 'WAITING_HUMAN_REVIEW') return 'warning';
  if (isRunning.value) return 'primary';
  return 'info';
});

const scoreClass = computed(() => {
  const score = props.review?.score;
  if (score == null) return '';
  if (score >= 70) return 'score-good';
  if (score >= 40) return 'score-warn';
  return 'score-bad';
});

const totalDuration = computed(() => {
  const total = (props.agentRuns || []).reduce((sum, r) => sum + (r.durationMs || 0), 0);
  return formatDuration(total || undefined);
});

function copyId() {
  if (props.detail?.taskId) {
    navigator.clipboard.writeText(props.detail.taskId);
    ElMessage.success('Task ID 已复制');
  }
}
</script>

<style scoped>
.status-header-card {
  background: #ffffff;
  border-radius: 18px;
  padding: 24px 28px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04), 0 4px 12px rgba(0,0,0,0.03);
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.task-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}
.status-badge.status-success { background: #dcfce7; color: #166534; }
.status-badge.status-error { background: #fee2e2; color: #991b1b; }
.status-badge.status-warning { background: #fef3c7; color: #92400e; }
.status-badge.status-primary { background: #dbeafe; color: #1e40af; }
.status-badge.status-info { background: #f3f4f6; color: #4b5563; }

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background: #fff;
  cursor: pointer;
  color: #6b7280;
  transition: all 0.15s;
}
.btn-icon:hover { border-color: #2563eb; color: #2563eb; }
.btn-icon:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-secondary {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background: #fff;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-secondary:hover { border-color: #2563eb; color: #2563eb; }

.spinning {
  animation: spin 1s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.header-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f3f4f6;
}
.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.meta-label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}
.meta-value {
  font-size: 14px;
  color: #111827;
  font-weight: 500;
}
.meta-value.mono {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  color: #4b5563;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.meta-value.mono:hover { color: #2563eb; }
.product-chip {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 500;
  margin-right: 6px;
}

.metrics-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}
.metric-card {
  text-align: center;
  padding: 14px 8px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
}
.metric-value {
  font-size: 22px;
  font-weight: 700;
  color: #111827;
}
.metric-value.score-good { color: #22c55e; }
.metric-value.score-warn { color: #f59e0b; }
.metric-value.score-bad { color: #ef4444; }
.metric-label {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}

.running-banner {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: 10px;
  background: #eff6ff;
  color: #1e40af;
  font-size: 13px;
}

@media (max-width: 1000px) {
  .metrics-row {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (max-width: 600px) {
  .metrics-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .header-top {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
