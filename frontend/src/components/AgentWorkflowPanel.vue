<template>
  <div class="workflow-panel">
    <div class="panel-header">
      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
      <span>分析流程</span>
    </div>
    <div class="agent-list">
      <div
        v-for="(agent, idx) in agents"
        :key="agent.type"
        class="agent-card"
        :class="'state-' + agent.state"
      >
        <!-- 连接线 -->
        <div v-if="idx > 0" class="connector-line" :class="'line-' + agents[idx-1].state"></div>

        <div class="agent-row">
          <div class="agent-icon-wrap" :class="'icon-' + agent.state">
            <svg v-if="agent.state === 'success'" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
            <svg v-else-if="agent.state === 'failed'" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            <svg v-else-if="agent.state === 'running'" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spinning"><path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/></svg>
            <svg v-else xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/></svg>
          </div>
          <div class="agent-content">
            <div class="agent-name">{{ agent.label }}</div>
            <div class="agent-desc">{{ agent.description }}</div>
          </div>
        </div>
        <div v-if="agent.runCount > 0" class="agent-stats">
          执行 {{ agent.runCount }} 次
          <span v-if="agent.lastDuration != null"> · {{ formatDuration(agent.lastDuration) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { AgentRunResponse } from '@/types';
import { getCurrentAgentByStatus } from '@/utils/status';
import { formatDuration } from '@/utils/time';

type AgentState = 'waiting' | 'running' | 'success' | 'failed';

interface AgentInfo {
  type: string;
  label: string;
  description: string;
  state: AgentState;
  runCount: number;
  lastDuration: number | null;
}

const AGENT_DEFS = [
  { type: 'PLANNER_AGENT', matchKey: 'Planner', label: '规划分析', description: '制定采集计划与分析框架' },
  { type: 'COLLECTOR_AGENT', matchKey: 'Collector', label: '信息采集', description: '搜索公开资料并构建证据池' },
  { type: 'EXTRACTOR_AGENT', matchKey: 'Extractor', label: '特征提取', description: '结构化抽取产品画像与关键特征' },
  { type: 'ANALYZER_AGENT', matchKey: 'Analyzer', label: '对比分析', description: '多维度横向对比与 SWOT 分析' },
  { type: 'WRITER_AGENT', matchKey: 'Writer', label: '报告生成', description: '输出专业 Markdown 竞品分析报告' },
  { type: 'REVIEWER_AGENT', matchKey: 'Reviewer', label: '质量校验', description: '智能评分与质量闭环校验' },
];

const props = defineProps<{
  status?: string;
  agentRuns: AgentRunResponse[];
}>();

const agents = computed<AgentInfo[]>(() => {
  const currentAgent = getCurrentAgentByStatus(props.status);

  return AGENT_DEFS.map((def) => {
    const runs = props.agentRuns.filter((r) => r.agentType === def.type);
    const lastRun = runs.length > 0 ? runs[runs.length - 1] : null;

    let state: AgentState = 'waiting';
    if (lastRun) {
      state = lastRun.status === 'SUCCESS' ? 'success' : 'failed';
    }
    if (currentAgent === def.matchKey + 'Agent' || currentAgent === def.matchKey) {
      state = 'running';
    }

    return {
      type: def.type,
      label: def.label,
      description: def.description,
      state,
      runCount: runs.length,
      lastDuration: lastRun?.durationMs ?? null,
    };
  });
});
</script>

<style scoped>
.workflow-panel {
  background: #ffffff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04), 0 4px 12px rgba(0,0,0,0.03);
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 18px;
}

.agent-list {
  display: flex;
  flex-direction: column;
}

.agent-card {
  position: relative;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background: #fafbfc;
  transition: all 0.2s;
}
.agent-card + .agent-card {
  margin-top: 8px;
}

.agent-card.state-success { background: #f0fdf4; border-color: #bbf7d0; }
.agent-card.state-running { background: #eff6ff; border-color: #bfdbfe; }
.agent-card.state-failed { background: #fef2f2; border-color: #fecaca; }
.agent-card.state-waiting { background: #fafbfc; border-color: #e5e7eb; }

.connector-line {
  position: absolute;
  top: -8px;
  left: 25px;
  width: 2px;
  height: 8px;
  background: #e5e7eb;
}
.connector-line.line-success { background: #22c55e; }
.connector-line.line-running { background: #2563eb; }

.agent-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.agent-icon-wrap {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.agent-icon-wrap.icon-success { background: #dcfce7; color: #16a34a; }
.agent-icon-wrap.icon-failed { background: #fee2e2; color: #dc2626; }
.agent-icon-wrap.icon-running { background: #dbeafe; color: #2563eb; }
.agent-icon-wrap.icon-waiting { background: #f3f4f6; color: #9ca3af; }

.agent-content { flex: 1; min-width: 0; }
.agent-name { font-size: 13px; font-weight: 600; color: #111827; }
.agent-desc { font-size: 11px; color: #6b7280; margin-top: 2px; }

.agent-stats {
  margin-top: 6px;
  padding-left: 38px;
  font-size: 11px;
  color: #6b7280;
}

.spinning {
  animation: spin 1.2s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
