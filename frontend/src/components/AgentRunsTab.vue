<template>
  <div class="agent-runs">
    <el-timeline v-if="agentRuns.length">
      <el-timeline-item
        v-for="run in agentRuns"
        :key="run.runId"
        :type="run.status === 'SUCCESS' ? 'success' : 'danger'"
        :icon="run.status === 'SUCCESS' ? Check : Close"
        :timestamp="`${formatTime(run.startTime)} (${run.durationMs}ms)`"
      >
        <div class="run-card">
          <div class="run-header">
            <strong>{{ formatAgent(run.agentType) }}</strong>
            <el-tag :type="run.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ run.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </div>
          <div class="run-body">
            <div>输入: {{ run.inputType }}</div>
            <div>输出: {{ run.outputType }}</div>
            <div v-if="run.errorMessage" class="error-msg">错误: {{ run.errorMessage }}</div>
          </div>
        </div>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-else description="暂无执行记录" />
  </div>
</template>

<script setup lang="ts">
import { Check, Close } from '@element-plus/icons-vue';
import type { AgentRunResponse } from '@/types';

defineProps<{ agentRuns: AgentRunResponse[] }>();

const AGENT_LABELS: Record<string, string> = {
  PLANNER_AGENT: 'Planner',
  COLLECTOR_AGENT: 'Collector',
  EXTRACTOR_AGENT: 'Extractor',
  ANALYZER_AGENT: 'Analyzer',
  WRITER_AGENT: 'Writer',
  REVIEWER_AGENT: 'Reviewer',
};

function formatAgent(t?: string) {
  return AGENT_LABELS[t || ''] || t || '未知';
}

function formatTime(t?: string) {
  if (!t) return '-';
  return new Date(t).toLocaleString('zh-CN');
}
</script>

<style scoped>
.agent-runs { padding: 8px 0; }
.run-card { padding: 8px 12px; background: #f8f9fa; border-radius: 6px; }
.run-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.run-body { font-size: 13px; color: #555; line-height: 1.8; }
.error-msg { color: #f56c6c; margin-top: 4px; }
</style>
