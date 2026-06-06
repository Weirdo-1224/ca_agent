<template>
  <div v-if="review" class="review-tab">
    <el-card shadow="never" class="review-summary">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="8">
          <div class="summary-item">
            <div class="label">质检结果</div>
            <el-tag :type="review.passed ? 'success' : 'danger'" size="large">
              {{ review.passed ? '通过' : '未通过' }}
            </el-tag>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8">
          <div class="summary-item">
            <div class="label">质检得分</div>
            <div class="score">{{ review.score ?? '-' }}</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8">
          <div class="summary-item">
            <div class="label">下一步</div>
            <div v-if="review.nextAction">
              <el-tag size="small">{{ review.nextAction.action }}</el-tag>
              <div class="reason">{{ review.nextAction.reason }}</div>
            </div>
            <div v-else>-</div>
          </div>
        </el-col>
      </el-row>
      <div v-if="review.summary" class="review-summary-text">
        <strong>摘要：</strong>{{ review.summary }}
      </div>
    </el-card>

    <h4 v-if="review.issues?.length">问题列表 ({{ review.issues.length }})</h4>
    <el-empty v-else-if="review.passed" description="质检通过，未发现阻塞问题" />

    <el-table v-if="review.issues?.length" :data="review.issues" stripe border>
      <el-table-column prop="issueId" label="ID" width="100" />
      <el-table-column prop="severity" label="严重度" width="100">
        <template #default="{ row }">
          <el-tag :type="severityType(row.severity)" size="small">{{ row.severity }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="160" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="targetAgent" label="目标 Agent" width="140" />
      <el-table-column prop="targetProduct" label="目标产品" width="120" />
      <el-table-column prop="repairInstruction" label="修复建议" min-width="150" show-overflow-tooltip />
    </el-table>
  </div>
  <el-empty v-else :description="isRunning(status) ? '质检尚未完成' : '暂无质检结果'" />
</template>

<script setup lang="ts">
import type { ReviewResult } from '@/types';

defineProps<{
  review: ReviewResult | null;
  status?: string;
}>();

const RUNNING = new Set(['CREATED', 'PLANNING', 'COLLECTING', 'EXTRACTING', 'ANALYZING', 'WRITING', 'REVIEWING', 'REPAIRING']);
function isRunning(s?: string) {
  return s ? RUNNING.has(s) : false;
}

function severityType(s: string) {
  if (s === 'HIGH') return 'danger';
  if (s === 'MEDIUM') return 'warning';
  return 'info';
}
</script>

<style scoped>
.review-tab { padding: 8px 0; }
.review-summary { margin-bottom: 20px; }
.summary-item { text-align: center; padding: 8px 0; }
.label { color: #999; font-size: 13px; margin-bottom: 6px; }
.score { font-size: 24px; font-weight: bold; color: #333; }
.reason { font-size: 12px; color: #666; margin-top: 4px; }
.review-summary-text { margin-top: 16px; padding-top: 12px; border-top: 1px solid #eee; }
</style>
