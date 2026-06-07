<template>
  <div class="overview-tab">
    <!-- 任务摘要 -->
    <el-card shadow="never" class="section-card">
      <template #header><span class="section-title">任务摘要</span></template>
      <el-descriptions :column="2" size="small" border>
        <el-descriptions-item label="任务名称">{{ task?.taskName }}</el-descriptions-item>
        <el-descriptions-item label="领域">{{ task?.domain }}</el-descriptions-item>
        <el-descriptions-item label="目标产品">
          <el-tag v-for="p in task?.targetProducts" :key="p" size="small" style="margin-right:4px">{{ p }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="分析目标">{{ task?.analysisGoal }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 执行摘要 -->
    <el-card shadow="never" class="section-card">
      <template #header><span class="section-title">执行摘要</span></template>
      <el-descriptions :column="2" size="small" border>
        <el-descriptions-item label="当前状态">
          <el-tag :type="statusTagType" size="small">{{ statusText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="当前阶段">{{ currentAgent }}</el-descriptions-item>
        <el-descriptions-item label="修复轮次">{{ task?.iterationCount }} / {{ task?.maxIterations }}</el-descriptions-item>
        <el-descriptions-item label="Agent 执行次数">{{ agentRuns.length }}</el-descriptions-item>
        <el-descriptions-item label="成功数">{{ successCount }}</el-descriptions-item>
        <el-descriptions-item label="失败数">{{ failedCount }}</el-descriptions-item>
        <el-descriptions-item label="总耗时">{{ totalDuration }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 质量摘要 -->
    <el-card shadow="never" class="section-card">
      <template #header><span class="section-title">质量摘要</span></template>
      <template v-if="review">
        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item label="质检结果">
            <el-tag :type="review.passed ? 'success' : 'warning'" size="small">
              {{ review.passed ? '通过' : '未通过' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="评分">{{ review.score ?? '—' }}</el-descriptions-item>
          <el-descriptions-item :span="2" label="摘要">{{ review.summary || '—' }}</el-descriptions-item>
          <el-descriptions-item v-if="review.nextAction" :span="2" label="下一步">
            {{ review.nextAction.action }} → {{ review.nextAction.targetAgent }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <el-empty v-else :image-size="60" :description="isRunning ? '质检尚未完成' : '暂无质检结果'" />
    </el-card>

    <!-- 关键产物统计 -->
    <el-card shadow="never" class="section-card">
      <template #header><span class="section-title">关键产物统计</span></template>
      <div class="stats-grid">
        <div class="stat-item">
          <div class="stat-value">{{ report?.sections?.length ?? 0 }}</div>
          <div class="stat-label">报告章节</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ evidence.length }}</div>
          <div class="stat-label">证据数量</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ review?.issues?.length ?? 0 }}</div>
          <div class="stat-label">质检问题</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ agentRuns.length }}</div>
          <div class="stat-label">Agent 执行数</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { TaskDetailResponse, AgentRunResponse, ReportResponse, Evidence, ReviewResult } from '@/types';
import { getStatusTagType, getStatusText, getCurrentAgentByStatus, isRunningStatus } from '@/utils/status';
import { formatDuration } from '@/utils/time';

const props = defineProps<{
  task: TaskDetailResponse | null;
  agentRuns: AgentRunResponse[];
  report: ReportResponse | null;
  evidence: Evidence[];
  review: ReviewResult | null;
}>();

const statusTagType = computed(() => getStatusTagType(props.task?.status));
const statusText = computed(() => getStatusText(props.task?.status));
const currentAgent = computed(() => getCurrentAgentByStatus(props.task?.status));
const isRunning = computed(() => isRunningStatus(props.task?.status));

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
  gap: 16px;
}
.section-card {
  border-radius: 6px;
}
.section-title {
  font-weight: 600;
  font-size: 14px;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  text-align: center;
}
.stat-item {
  padding: 16px 8px;
  border-radius: 6px;
  background: #f5f7fa;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}
.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
