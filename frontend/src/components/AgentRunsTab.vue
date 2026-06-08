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
              <div class="run-meta">
                <span v-if="run.totalTokens > 0" class="run-tokens">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v12M6 12h12"/></svg>
                  {{ run.totalTokens.toLocaleString() }} tokens
                </span>
                <span class="run-duration">{{ formatDuration(run.durationMs) }}</span>
              </div>
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
                <el-descriptions-item v-if="run.totalTokens > 0" label="Token 用量">
                  <span class="token-detail">
                    Prompt: {{ run.promptTokens.toLocaleString() }} · Completion: {{ run.completionTokens.toLocaleString() }} · 合计: {{ run.totalTokens.toLocaleString() }}
                  </span>
                </el-descriptions-item>
                <el-descriptions-item v-if="run.errorMessage" label="错误信息">
                  <span class="error-text">{{ run.errorMessage }}</span>
                </el-descriptions-item>
              </el-descriptions>

              <!-- LLM 调用详情 -->
              <div v-if="run.llmCalls && run.llmCalls.length > 0" class="llm-calls-section" @click.stop>
                <div class="llm-calls-header">LLM 调用记录 ({{ run.llmCalls.length }} 次)</div>
                <el-collapse accordion>
                  <el-collapse-item
                    v-for="(call, idx) in run.llmCalls"
                    :key="idx"
                    :name="idx"
                  >
                    <template #title>
                      <span class="llm-call-title">
                        #{{ idx + 1 }} · {{ call.promptTokens + call.completionTokens }} tokens · {{ call.durationMs }}ms
                      </span>
                    </template>
                    <div class="llm-call-detail">
                      <div v-if="call.systemPrompt" class="prompt-block">
                        <div class="prompt-label">System Prompt</div>
                        <pre class="prompt-content">{{ call.systemPrompt }}</pre>
                      </div>
                      <div class="prompt-block">
                        <div class="prompt-label">User Prompt</div>
                        <pre class="prompt-content">{{ call.userPrompt }}</pre>
                      </div>
                      <div class="prompt-block">
                        <div class="prompt-label">Response (决策输出)</div>
                        <pre class="prompt-content response-content">{{ call.response }}</pre>
                      </div>
                    </div>
                  </el-collapse-item>
                </el-collapse>
              </div>
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
import { formatTime, formatDuration } from '@/utils/time';

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
.run-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}
.run-tokens {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #67c23a;
  font-weight: 500;
}
.run-duration {
  font-size: 13px;
  color: #909399;
}
.token-detail {
  font-size: 13px;
  color: #606266;
}
.run-detail {
  margin-top: 12px;
}
.error-text {
  color: #f56c6c;
  font-size: 13px;
}
.llm-calls-section {
  margin-top: 16px;
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
}
.llm-calls-header {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}
.llm-call-title {
  font-size: 13px;
  color: #606266;
}
.llm-call-detail {
  padding: 8px 0;
}
.prompt-block {
  margin-bottom: 12px;
}
.prompt-label {
  font-size: 12px;
  font-weight: 600;
  color: #909399;
  margin-bottom: 4px;
  text-transform: uppercase;
}
.prompt-content {
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 10px 12px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 300px;
  overflow-y: auto;
  margin: 0;
}
.response-content {
  background: #f0f9eb;
  border-color: #e1f3d8;
}
</style>
