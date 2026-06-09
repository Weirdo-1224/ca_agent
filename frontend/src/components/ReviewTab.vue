<template>
  <div class="review-tab">
    <template v-if="review">
      <!-- 质检结果卡片 -->
      <div class="review-hero">
        <div class="hero-left">
          <div class="score-block">
            <div class="big-score" :class="scoreColorClass">{{ review.score ?? 0 }}</div>
            <div class="score-max">/ 100</div>
          </div>
          <div class="pass-badge" :class="review.passed ? 'pass' : 'fail'">
            {{ review.passed ? '质检通过' : '质检未通过' }}
          </div>
        </div>
        <div class="hero-right" v-if="review.summary">
          <div class="summary-label">评审摘要</div>
          <div class="summary-text">{{ review.summary }}</div>
        </div>
      </div>

      <!-- 下一步动作 -->
      <div v-if="review.nextAction" class="next-action-card" :class="review.passed ? 'action-info' : 'action-warning'">
        <div class="action-icon">
          <svg v-if="!review.passed" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 16 16 12 12 8"/><line x1="8" y1="12" x2="16" y2="12"/></svg>
        </div>
        <div class="action-body">
          <div class="action-title">
            <template v-if="taskStatus === 'WAITING_HUMAN_REVIEW'">
              已达到最大自动修复轮次，等待人工审核
            </template>
            <template v-else-if="!review.passed">
              报告未通过质检，将回退到 <strong>{{ review.nextAction.targetAgent }}</strong> 进行修复
            </template>
            <template v-else>
              质检通过，流程正常结束
            </template>
          </div>
          <div class="action-details">
            <span class="detail-item"><span class="detail-key">动作：</span>{{ review.nextAction.action }}</span>
            <span class="detail-item"><span class="detail-key">目标：</span>{{ review.nextAction.targetAgent }}</span>
            <span v-if="review.nextAction.reason" class="detail-item"><span class="detail-key">原因：</span>{{ review.nextAction.reason }}</span>
          </div>
        </div>
      </div>

      <!-- 问题列表 -->
      <div class="issues-section">
        <div class="issues-header">
          <span class="issues-title">问题列表</span>
          <span class="issues-count" v-if="review.issues?.length">{{ review.issues.length }} 个问题</span>
        </div>
        <template v-if="review.issues && review.issues.length > 0">
          <div class="issue-card" v-for="issue in review.issues" :key="issue.issueId">
            <div class="issue-top">
              <span class="severity-tag" :class="'sev-' + issue.severity?.toLowerCase()">{{ issue.severity }}</span>
              <span class="issue-type">{{ issue.type }}</span>
              <span v-if="issue.targetAgent" class="issue-agent">→ {{ issue.targetAgent }}</span>
            </div>
            <div class="issue-desc">{{ issue.description }}</div>
            <div class="issue-meta" v-if="issue.targetProduct || issue.targetDimension">
              <span v-if="issue.targetProduct" class="meta-chip">产品: {{ issue.targetProduct }}</span>
              <span v-if="issue.targetDimension" class="meta-chip">维度: {{ issue.targetDimension }}</span>
            </div>
            <div v-if="issue.repairInstruction" class="repair-box">
              <span class="repair-label">修复建议：</span>{{ issue.repairInstruction }}
            </div>
          </div>
        </template>
        <div v-else class="no-issues">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#22c55e" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
          <span>质检通过，未发现阻塞问题</span>
        </div>
      </div>
    </template>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#d1d5db" stroke-width="1.5"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
      <p>{{ emptyText }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { ReviewResult } from '@/types';
import { isRunningStatus } from '@/utils/status';

const props = defineProps<{
  review: ReviewResult | null;
  taskStatus?: string;
}>();

const scoreColorClass = computed(() => {
  const score = props.review?.score;
  if (score == null) return '';
  if (score >= 70) return 'score-good';
  if (score >= 40) return 'score-warn';
  return 'score-bad';
});

const emptyText = computed(() => {
  if (isRunningStatus(props.taskStatus)) return '质检尚未完成';
  return '暂无质检结果';
});
</script>

<style scoped>
.review-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Hero Card */
.review-hero {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 24px;
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #f1f5f9;
}
.hero-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}
.score-block {
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.big-score {
  font-size: 48px;
  font-weight: 800;
  line-height: 1;
  color: #111827;
}
.big-score.score-good { color: #22c55e; }
.big-score.score-warn { color: #f59e0b; }
.big-score.score-bad { color: #ef4444; }
.score-max {
  font-size: 18px;
  color: #9ca3af;
  font-weight: 500;
}
.pass-badge {
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}
.pass-badge.pass { background: #dcfce7; color: #166534; }
.pass-badge.fail { background: #fee2e2; color: #991b1b; }

.hero-right {
  flex: 1;
  min-width: 0;
}
.summary-label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 600;
  margin-bottom: 6px;
}
.summary-text {
  font-size: 14px;
  color: #374151;
  line-height: 1.6;
}

/* Next Action */
.next-action-card {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  border-radius: 12px;
}
.next-action-card.action-warning {
  background: #fffbeb;
  border: 1px solid #fde68a;
}
.next-action-card.action-info {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
}
.action-icon {
  flex-shrink: 0;
  margin-top: 2px;
}
.action-warning .action-icon { color: #d97706; }
.action-info .action-icon { color: #16a34a; }
.action-body { flex: 1; }
.action-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 8px;
}
.action-details {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.detail-item {
  font-size: 13px;
  color: #4b5563;
}
.detail-key {
  font-weight: 600;
  color: #6b7280;
}

/* Issues */
.issues-section {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #f1f5f9;
  padding: 20px;
}
.issues-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.issues-title {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}
.issues-count {
  font-size: 12px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 2px 10px;
  border-radius: 10px;
}

.issue-card {
  padding: 14px 16px;
  border-radius: 10px;
  background: #fafbfc;
  border: 1px solid #e5e7eb;
  margin-bottom: 10px;
}
.issue-card:last-child { margin-bottom: 0; }
.issue-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.severity-tag {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}
.severity-tag.sev-critical,
.severity-tag.sev-high { background: #fee2e2; color: #991b1b; }
.severity-tag.sev-medium { background: #fef3c7; color: #92400e; }
.severity-tag.sev-low { background: #f3f4f6; color: #4b5563; }
.issue-type {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}
.issue-agent {
  font-size: 12px;
  color: #2563eb;
  font-weight: 500;
}
.issue-desc {
  font-size: 13px;
  color: #374151;
  line-height: 1.6;
}
.issue-meta {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.meta-chip {
  padding: 2px 8px;
  border-radius: 8px;
  background: #f3f4f6;
  font-size: 11px;
  color: #6b7280;
}
.repair-box {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #eff6ff;
  font-size: 12px;
  color: #1e40af;
  line-height: 1.6;
}
.repair-label { font-weight: 600; }

.no-issues {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  color: #16a34a;
  font-size: 14px;
}

/* Empty */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #9ca3af;
}
.empty-state p { margin: 12px 0 0; font-size: 14px; }
</style>
