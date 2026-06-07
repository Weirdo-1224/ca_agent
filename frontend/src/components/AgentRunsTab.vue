<template>
  <div class="runs-tab">
    <template v-if="agentRuns.length > 0">
      <!-- 多次执行提示 -->
      <el-alert
        v-if="hasMultipleRuns"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom:12px"
      >
        <template #title>检测到部分 Agent 多次执行，可能由 Reviewer 质检回退触发。</template>
      </el-alert>

      <el-timeline>
        <el-timeline-item
          v-for="run in agentRuns"
          :key="run.runId"
          :type="run.status === 'SUCCESS' ? 'success' : 'danger'"
          :hollow="false"
          :timestamp="formatTime(run.startTime)"
          placement="top"
        >
          <el-card shadow="never" class="run-card" @click="toggleExpand(run.runId)">
            <div class="run-header">
              <div class="run-title">
                <strong>{{ run.agentType }}</strong>
                <el-tag :type="run.status === 'SUCCESS' ? 'success' : 'danger'" size="small">{{ run.status }}</el-tag>
              </div>
              <span class="run-duration">{{ formatDuration(run.durationMs) }}</span>
            </div>

            <!-- 展开详情 -->
            <div v-if="expanded.has(run.runId)" class="run-detail">
              <el-descriptions :column="1" size="small" border>
                <el-descriptions-item label="Agent">{{ run.agentType }}</el-descriptions-item>
                <el-descriptions-item label="状态">{{ run.status }}</el-descriptions-item>
                <el-descriptions-item label="输入类型">{{ run.inputType || '—' }}</el-descriptions-item>
                <el-descriptions-item label="输出类型">{{ run.outputType || '—' }}</el-descriptions-item>
                <el-descriptions-item label="开始时间">{{ formatTime(run.startTime) }}</el-descriptions-item>
                <el-descriptions-item label="结束时间">{{ formatTime(run.endTime) }}</el-descriptions-item>
                <el-descriptions-item label="耗时">{{ formatDuration(run.durationMs) }}</el-descriptions-item>
                <el-descriptions-item v-if="run.errorMessage" label="错误信息">
                  <span class="error-text">{{ run.errorMessage }}</span>
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </template>

    <el-empty v-else :image-size="80" description="暂无 Agent 执行记录" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import type { AgentRunResponse } from '@/types';
import { formatTime } from '@/utils/time';
import { formatDuration } from '@/utils/time';

const props = defineProps<{
  agentRuns: AgentRunResponse[];
}>();

const expanded = ref<Set<string>>(new Set());

function toggleExpand(runId: string) {
  if (expanded.value.has(runId)) {
    expanded.value.delete(runId);
  } else {
    expanded.value.add(runId);
  }
}

// Check if any agent type has multiple runs
const hasMultipleRuns = computed(() => {
  const counts: Record<string, number> = {};
  for (const run of props.agentRuns) {
    counts[run.agentType] = (counts[run.agentType] || 0) + 1;
  }
  return Object.values(counts).some((c) => c > 1);
});
</script>

<style scoped>
.runs-tab {
  min-height: 200px;
}
.run-card {
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.run-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.run-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.run-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.run-duration {
  font-size: 13px;
  color: #909399;
}
.run-detail {
  margin-top: 12px;
}
.error-text {
  color: #f56c6c;
  font-size: 13px;
}
</style>
