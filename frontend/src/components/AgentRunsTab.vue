<template>
  <div class="runs-tab">
    <template v-if="agentRuns.length > 0">
      <!-- 统计卡片 -->
      <div class="runs-stats">
        <div class="runs-stat">
          <div class="stat-val">{{ agentRuns.length }}</div>
          <div class="stat-lbl">总执行数</div>
        </div>
        <div class="runs-stat">
          <div class="stat-val success-text">{{ successCount }}</div>
          <div class="stat-lbl">成功</div>
        </div>
        <div class="runs-stat">
          <div class="stat-val" :class="{ 'error-text': failedCount > 0 }">{{ failedCount }}</div>
          <div class="stat-lbl">失败</div>
        </div>
        <div class="runs-stat">
          <div class="stat-val">{{ totalTokens.toLocaleString() }}</div>
          <div class="stat-lbl">Token 总量</div>
        </div>
        <div class="runs-stat">
          <div class="stat-val">{{ totalDuration }}</div>
          <div class="stat-lbl">总耗时</div>
        </div>
      </div>

      <!-- 多次执行提示 -->
      <div v-if="hasMultipleRuns" class="multi-run-tip">
        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
        检测到部分 Agent 多次执行，可能由 Reviewer 质检回退触发。
      </div>

      <!-- 时间线 -->
      <div class="timeline">
        <div
          v-for="run in agentRuns"
          :key="run.runId"
          class="timeline-item"
          :class="'tl-' + (run.status === 'SUCCESS' ? 'success' : 'failed')"
        >
          <div class="tl-dot"></div>
          <div class="tl-card" @click="toggleExpand(run.runId)">
            <div class="tl-header">
              <div class="tl-left">
                <span class="tl-agent">{{ run.agentType }}</span>
                <span class="tl-status-tag" :class="run.status === 'SUCCESS' ? 'tag-success' : 'tag-failed'">{{ run.status }}</span>
              </div>
              <div class="tl-right">
                <span v-if="run.totalTokens > 0" class="tl-tokens">{{ run.totalTokens.toLocaleString() }} tokens</span>
                <span class="tl-duration">{{ formatDuration(run.durationMs) }}</span>
                <span class="tl-time">{{ formatTime(run.startTime) }}</span>
                <svg class="tl-chevron" :class="{ rotated: expanded.has(run.runId) }" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
              </div>
            </div>

            <!-- 展开详情 -->
            <div v-if="expanded.has(run.runId)" class="tl-detail" @click.stop>
              <div class="detail-grid">
                <div class="dg-item"><span class="dg-label">Agent</span><span class="dg-value">{{ run.agentType }}</span></div>
                <div class="dg-item"><span class="dg-label">状态</span><span class="dg-value">{{ run.status }}</span></div>
                <div class="dg-item"><span class="dg-label">输入类型</span><span class="dg-value">{{ run.inputType || '—' }}</span></div>
                <div class="dg-item"><span class="dg-label">输出类型</span><span class="dg-value">{{ run.outputType || '—' }}</span></div>
                <div class="dg-item"><span class="dg-label">开始时间</span><span class="dg-value">{{ formatTime(run.startTime) }}</span></div>
                <div class="dg-item"><span class="dg-label">结束时间</span><span class="dg-value">{{ formatTime(run.endTime) }}</span></div>
                <div class="dg-item"><span class="dg-label">耗时</span><span class="dg-value">{{ formatDuration(run.durationMs) }}</span></div>
                <div v-if="run.totalTokens > 0" class="dg-item">
                  <span class="dg-label">Token</span>
                  <span class="dg-value">Prompt: {{ run.promptTokens.toLocaleString() }} · Completion: {{ run.completionTokens.toLocaleString() }} · Total: {{ run.totalTokens.toLocaleString() }}</span>
                </div>
              </div>

              <div v-if="run.errorMessage" class="error-block">
                <span class="error-label">错误信息</span>
                <div class="error-text">{{ run.errorMessage }}</div>
              </div>

              <!-- LLM 调用记录 -->
              <div v-if="run.llmCalls && run.llmCalls.length > 0" class="llm-section" @click.stop>
                <div class="llm-header">LLM 调用记录 ({{ run.llmCalls.length }} 次)</div>
                <div v-for="(call, idx) in run.llmCalls" :key="idx" class="llm-call">
                  <div class="llm-call-header" @click.stop="toggleLlm(run.runId + '-' + idx)">
                    <span class="llm-call-title">#{{ idx + 1 }} · {{ call.promptTokens + call.completionTokens }} tokens · {{ call.durationMs }}ms</span>
                    <svg class="tl-chevron" :class="{ rotated: expandedLlm.has(run.runId + '-' + idx) }" xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
                  </div>
                  <div v-if="expandedLlm.has(run.runId + '-' + idx)" class="llm-call-body">
                    <div v-if="call.systemPrompt" class="prompt-block">
                      <div class="prompt-top">
                        <span class="prompt-label">System Prompt</span>
                        <button class="copy-btn" @click.stop="copyText(call.systemPrompt)">复制</button>
                      </div>
                      <pre class="prompt-content prompt-bg">{{ call.systemPrompt }}</pre>
                    </div>
                    <div class="prompt-block">
                      <div class="prompt-top">
                        <span class="prompt-label">User Prompt</span>
                        <button class="copy-btn" @click.stop="copyText(call.userPrompt)">复制</button>
                      </div>
                      <pre class="prompt-content prompt-bg">{{ call.userPrompt }}</pre>
                    </div>
                    <div class="prompt-block">
                      <div class="prompt-top">
                        <span class="prompt-label">Response</span>
                        <button class="copy-btn" @click.stop="copyText(call.response)">复制</button>
                      </div>
                      <pre class="prompt-content response-bg">{{ call.response }}</pre>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="empty-state">
      <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#d1d5db" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
      <p>暂无 Agent 执行记录</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import type { AgentRunResponse } from '@/types';
import { formatTime, formatDuration } from '@/utils/time';

const props = defineProps<{
  agentRuns: AgentRunResponse[];
}>();

const expanded = ref<Set<string>>(new Set());
const expandedLlm = ref<Set<string>>(new Set());

function toggleExpand(runId: string) {
  if (expanded.value.has(runId)) {
    expanded.value.delete(runId);
  } else {
    expanded.value.add(runId);
  }
}

function toggleLlm(key: string) {
  if (expandedLlm.value.has(key)) {
    expandedLlm.value.delete(key);
  } else {
    expandedLlm.value.add(key);
  }
}

function copyText(text: string) {
  navigator.clipboard.writeText(text);
  ElMessage.success('已复制到剪贴板');
}

const successCount = computed(() => props.agentRuns.filter((r) => r.status === 'SUCCESS').length);
const failedCount = computed(() => props.agentRuns.filter((r) => r.status !== 'SUCCESS').length);
const totalTokens = computed(() => props.agentRuns.reduce((sum, r) => sum + (r.totalTokens || 0), 0));
const totalDuration = computed(() => {
  const ms = props.agentRuns.reduce((sum, r) => sum + (r.durationMs || 0), 0);
  return formatDuration(ms || undefined);
});

const hasMultipleRuns = computed(() => {
  const counts: Record<string, number> = {};
  for (const run of props.agentRuns) {
    counts[run.agentType] = (counts[run.agentType] || 0) + 1;
  }
  return Object.values(counts).some((c) => c > 1);
});
</script>

<style scoped>
.runs-tab { min-height: 200px; }

/* Stats */
.runs-stats {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.runs-stat {
  text-align: center;
  padding: 14px 8px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
}
.stat-val { font-size: 20px; font-weight: 700; color: #111827; }
.stat-lbl { font-size: 11px; color: #6b7280; margin-top: 4px; }
.success-text { color: #22c55e; }
.error-text { color: #ef4444; }

.multi-run-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 10px;
  background: #eff6ff;
  color: #1e40af;
  font-size: 13px;
  margin-bottom: 16px;
}

/* Timeline */
.timeline {
  position: relative;
  padding-left: 20px;
}
.timeline::before {
  content: '';
  position: absolute;
  left: 7px;
  top: 0;
  bottom: 0;
  width: 2px;
  background: #e5e7eb;
}
.timeline-item {
  position: relative;
  margin-bottom: 12px;
}
.tl-dot {
  position: absolute;
  left: -20px;
  top: 16px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #e5e7eb;
  border: 2px solid #fff;
  z-index: 1;
}
.tl-success .tl-dot { background: #22c55e; }
.tl-failed .tl-dot { background: #ef4444; }

.tl-card {
  padding: 14px 18px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  cursor: pointer;
  transition: all 0.15s;
}
.tl-card:hover { border-color: #cbd5e1; box-shadow: 0 2px 6px rgba(0,0,0,0.04); }

.tl-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.tl-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.tl-agent { font-size: 14px; font-weight: 600; color: #111827; }
.tl-status-tag {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}
.tl-status-tag.tag-success { background: #dcfce7; color: #166534; }
.tl-status-tag.tag-failed { background: #fee2e2; color: #991b1b; }

.tl-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.tl-tokens { font-size: 12px; color: #22c55e; font-weight: 500; }
.tl-duration { font-size: 12px; color: #6b7280; }
.tl-time { font-size: 11px; color: #9ca3af; }
.tl-chevron { transition: transform 0.2s; }
.tl-chevron.rotated { transform: rotate(180deg); }

/* Detail */
.tl-detail {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #f3f4f6;
}
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 14px;
}
.dg-item { display: flex; flex-direction: column; gap: 2px; }
.dg-label { font-size: 11px; color: #6b7280; font-weight: 600; }
.dg-value { font-size: 13px; color: #111827; }

.error-block {
  margin-bottom: 14px;
  padding: 10px 14px;
  border-radius: 8px;
  background: #fef2f2;
  border: 1px solid #fecaca;
}
.error-label { font-size: 11px; color: #991b1b; font-weight: 600; }
.error-text { font-size: 13px; color: #991b1b; margin-top: 4px; }

/* LLM */
.llm-section {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #f3f4f6;
}
.llm-header {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 10px;
}
.llm-call {
  margin-bottom: 8px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}
.llm-call-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f8fafc;
  cursor: pointer;
}
.llm-call-title { font-size: 12px; color: #4b5563; }
.llm-call-body { padding: 12px; }

.prompt-block { margin-bottom: 12px; }
.prompt-block:last-child { margin-bottom: 0; }
.prompt-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.prompt-label {
  font-size: 11px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}
.copy-btn {
  padding: 2px 8px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  background: #fff;
  font-size: 11px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
}
.copy-btn:hover { border-color: #2563eb; color: #2563eb; }

.prompt-content {
  border-radius: 8px;
  padding: 12px 14px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 280px;
  overflow-y: auto;
  margin: 0;
  font-family: 'JetBrains Mono', Consolas, monospace;
}
.prompt-bg { background: #f8fafc; border: 1px solid #e5e7eb; }
.response-bg { background: #f0fdf4; border: 1px solid #bbf7d0; }

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

@media (max-width: 600px) {
  .runs-stats { grid-template-columns: repeat(3, 1fr); }
  .detail-grid { grid-template-columns: 1fr; }
  .tl-right { flex-wrap: wrap; }
}
</style>
