<template>
  <div class="overview-tab">
    <!-- 任务摘要 -->
    <div class="section-card">
      <div class="section-header">
        <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
        <span>任务摘要</span>
      </div>
      <dl class="desc-list">
        <div class="desc-item">
          <dt>任务名称</dt>
          <dd>{{ task?.taskName || '—' }}</dd>
        </div>
        <div class="desc-item">
          <dt>领域</dt>
          <dd>{{ task?.domain || '—' }}</dd>
        </div>
        <div class="desc-item">
          <dt>目标产品</dt>
          <dd>
            <span v-for="p in task?.targetProducts" :key="p" class="product-chip">{{ p }}</span>
            <span v-if="!task?.targetProducts?.length">—</span>
          </dd>
        </div>
        <div class="desc-item full">
          <dt>分析目标</dt>
          <dd>{{ task?.analysisGoal || '—' }}</dd>
        </div>
      </dl>
    </div>

    <!-- 执行摘要 -->
    <div class="section-card">
      <div class="section-header">
        <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
        <span>执行摘要</span>
      </div>
      <div class="exec-metrics">
        <div class="exec-metric">
          <div class="exec-value">
            <span class="exec-badge" :class="'badge-' + statusColor">{{ statusText }}</span>
          </div>
          <div class="exec-label">当前状态</div>
        </div>
        <div class="exec-metric">
          <div class="exec-value">{{ currentAgent }}</div>
          <div class="exec-label">当前阶段</div>
        </div>
        <div class="exec-metric">
          <div class="exec-value">{{ task?.iterationCount }} / {{ task?.maxIterations }}</div>
          <div class="exec-label">修复轮次</div>
        </div>
        <div class="exec-metric">
          <div class="exec-value">{{ agentRuns.length }}</div>
          <div class="exec-label">Agent 执行数</div>
        </div>
        <div class="exec-metric">
          <div class="exec-value success-text">{{ successCount }}</div>
          <div class="exec-label">成功</div>
        </div>
        <div class="exec-metric">
          <div class="exec-value" :class="{ 'error-text': failedCount > 0 }">{{ failedCount }}</div>
          <div class="exec-label">失败</div>
        </div>
        <div class="exec-metric">
          <div class="exec-value">{{ totalDuration }}</div>
          <div class="exec-label">总耗时</div>
        </div>
      </div>
    </div>

    <!-- 质量摘要 -->
    <div class="section-card">
      <div class="section-header">
        <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        <span>质量摘要</span>
      </div>
      <template v-if="review">
        <div class="quality-row">
          <div class="quality-score-block">
            <div class="quality-big-score" :class="scoreColorClass">
              {{ review.score ?? 0 }}
            </div>
            <div class="quality-max">/ 100</div>
            <span class="quality-badge" :class="review.passed ? 'pass' : 'fail'">
              {{ review.passed ? '通过' : '未通过' }}
            </span>
          </div>
          <div class="quality-summary" v-if="review.summary">
            {{ review.summary }}
          </div>
        </div>
        <div v-if="review.nextAction" class="quality-next">
          <span class="next-label">下一步：</span>
          {{ review.nextAction.action }} → {{ review.nextAction.targetAgent }}
        </div>
      </template>
      <div v-else class="empty-state">
        {{ isRunning ? '质检尚未完成' : '暂无质检结果' }}
      </div>
    </div>

    <!-- 关键产物统计 -->
    <div class="section-card">
      <div class="section-header">
        <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
        <span>关键产物统计</span>
      </div>
      <div class="artifact-grid">
        <div class="artifact-card">
          <div class="artifact-value">{{ report?.sections?.length ?? 0 }}</div>
          <div class="artifact-label">报告章节</div>
        </div>
        <div class="artifact-card">
          <div class="artifact-value">{{ evidence.length }}</div>
          <div class="artifact-label">证据数量</div>
        </div>
        <div class="artifact-card">
          <div class="artifact-value" :class="{ 'warn-text': (review?.issues?.length ?? 0) > 0 }">{{ review?.issues?.length ?? 0 }}</div>
          <div class="artifact-label">质检问题</div>
        </div>
        <div class="artifact-card">
          <div class="artifact-value">{{ agentRuns.length }}</div>
          <div class="artifact-label">Agent 执行数</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { TaskDetailResponse, AgentRunResponse, ReportResponse, Evidence, ReviewResult } from '@/types';
import { getStatusText, getCurrentAgentByStatus, isRunningStatus } from '@/utils/status';
import { formatDuration } from '@/utils/time';

const props = defineProps<{
  task: TaskDetailResponse | null;
  agentRuns: AgentRunResponse[];
  report: ReportResponse | null;
  evidence: Evidence[];
  review: ReviewResult | null;
}>();

const statusText = computed(() => getStatusText(props.task?.status));
const currentAgent = computed(() => getCurrentAgentByStatus(props.task?.status));
const isRunning = computed(() => isRunningStatus(props.task?.status));

const statusColor = computed(() => {
  const s = props.task?.status;
  if (s === 'COMPLETED') return 'success';
  if (s === 'FAILED') return 'error';
  if (s === 'COMPLETED_WITH_WARNINGS' || s === 'WAITING_HUMAN_REVIEW') return 'warning';
  if (isRunning.value) return 'primary';
  return 'info';
});

const scoreColorClass = computed(() => {
  const score = props.review?.score;
  if (score == null) return '';
  if (score >= 70) return 'score-good';
  if (score >= 40) return 'score-warn';
  return 'score-bad';
});

const successCount = computed(() => props.agentRuns.filter((r) => r.status === 'SUCCESS').length);
const failedCount = computed(() => props.agentRuns.filter((r) => r.status !== 'SUCCESS').length);
const totalDuration = computed(() => {
  const total = props.agentRuns.reduce((sum, r) => sum + (r.durationMs || 0), 0);
  return formatDuration(total || undefined);
});
</script>

<style scoped>
.overview-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-card {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #f1f5f9;
  padding: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 16px;
}

/* Description List */
.desc-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin: 0;
}
.desc-item { display: flex; flex-direction: column; gap: 4px; }
.desc-item.full { grid-column: 1 / -1; }
.desc-item dt { font-size: 12px; color: #6b7280; font-weight: 500; }
.desc-item dd { font-size: 14px; color: #111827; margin: 0; }
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

/* Execution Metrics */
.exec-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
}
.exec-metric {
  text-align: center;
  padding: 12px 8px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
}
.exec-value { font-size: 16px; font-weight: 700; color: #111827; }
.exec-label { font-size: 11px; color: #6b7280; margin-top: 4px; }
.exec-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}
.exec-badge.badge-success { background: #dcfce7; color: #166534; }
.exec-badge.badge-error { background: #fee2e2; color: #991b1b; }
.exec-badge.badge-warning { background: #fef3c7; color: #92400e; }
.exec-badge.badge-primary { background: #dbeafe; color: #1e40af; }
.exec-badge.badge-info { background: #f3f4f6; color: #4b5563; }
.success-text { color: #22c55e; }
.error-text { color: #ef4444; }

/* Quality Summary */
.quality-row {
  display: flex;
  align-items: center;
  gap: 24px;
}
.quality-score-block {
  display: flex;
  align-items: baseline;
  gap: 4px;
  flex-shrink: 0;
}
.quality-big-score {
  font-size: 42px;
  font-weight: 800;
  line-height: 1;
  color: #111827;
}
.quality-big-score.score-good { color: #22c55e; }
.quality-big-score.score-warn { color: #f59e0b; }
.quality-big-score.score-bad { color: #ef4444; }
.quality-max {
  font-size: 16px;
  color: #9ca3af;
  font-weight: 500;
}
.quality-badge {
  margin-left: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}
.quality-badge.pass { background: #dcfce7; color: #166534; }
.quality-badge.fail { background: #fee2e2; color: #991b1b; }
.quality-summary {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.6;
}
.quality-next {
  margin-top: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #fffbeb;
  font-size: 13px;
  color: #92400e;
}
.next-label { font-weight: 600; }

.empty-state {
  text-align: center;
  padding: 24px;
  color: #9ca3af;
  font-size: 13px;
}

/* Artifact Grid */
.artifact-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.artifact-card {
  text-align: center;
  padding: 16px 8px;
  border-radius: 12px;
  background: #f0f7ff;
  border: 1px solid #e0edff;
}
.artifact-value {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
}
.artifact-value.warn-text { color: #f59e0b; }
.artifact-label {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}

@media (max-width: 600px) {
  .desc-list { grid-template-columns: 1fr; }
  .artifact-grid { grid-template-columns: repeat(2, 1fr); }
  .exec-metrics { grid-template-columns: repeat(2, 1fr); }
}
</style>
