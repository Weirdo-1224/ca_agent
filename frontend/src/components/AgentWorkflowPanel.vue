<template>
  <div class="workflow-panel">
    <h4 class="panel-title">Agent 工作流</h4>
    <div class="agent-list">
      <div
        v-for="agent in agents"
        :key="agent.type"
        class="agent-item"
        :class="agentClass(agent)"
      >
        <div class="agent-icon">
          <el-icon v-if="agent.state === 'success'" color="#67c23a"><CircleCheckFilled /></el-icon>
          <el-icon v-else-if="agent.state === 'failed'" color="#f56c6c"><CircleCloseFilled /></el-icon>
          <el-icon v-else-if="agent.state === 'running'" color="#409eff"><Loading /></el-icon>
          <el-icon v-else color="#c0c4cc"><Clock /></el-icon>
        </div>
        <div class="agent-info">
          <div class="agent-name">{{ agent.label }}</div>
          <div class="agent-desc">{{ agent.description }}</div>
          <div v-if="agent.runCount > 0" class="agent-stats">
            执行 {{ agent.runCount }} 次
            <span v-if="agent.lastDuration != null"> · {{ formatDuration(agent.lastDuration) }}</span>
          </div>
        </div>
        <div class="agent-state-tag">
          <el-tag :type="stateTagType(agent.state)" size="small" round>{{ stateText(agent.state) }}</el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { CircleCheckFilled, CircleCloseFilled, Loading, Clock } from '@element-plus/icons-vue';
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
  { type: 'PLANNER_AGENT', label: 'Planner', description: '生成采集任务计划' },
  { type: 'COLLECTOR_AGENT', label: 'Collector', description: '搜索公开资料并生成证据池' },
  { type: 'EXTRACTOR_AGENT', label: 'Extractor', description: '结构化抽取产品画像和 Claim' },
  { type: 'ANALYZER_AGENT', label: 'Analyzer', description: '横向对比分析与 SWOT' },
  { type: 'WRITER_AGENT', label: 'Writer', description: '生成 Markdown 竞品分析报告' },
  { type: 'REVIEWER_AGENT', label: 'Reviewer', description: '质检评分并判断是否回退' },
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
    // If current task status maps to this agent, it's running
    if (currentAgent === def.label + 'Agent' || currentAgent === def.label) {
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

function agentClass(agent: AgentInfo) {
  return `agent-${agent.state}`;
}

function stateTagType(state: AgentState) {
  switch (state) {
    case 'success': return 'success';
    case 'failed': return 'danger';
    case 'running': return 'primary';
    default: return 'info';
  }
}

function stateText(state: AgentState) {
  switch (state) {
    case 'success': return '完成';
    case 'failed': return '失败';
    case 'running': return '运行中';
    default: return '等待';
  }
}
</script>

<style scoped>
.workflow-panel {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  padding: 16px;
  height: 100%;
}
.panel-title {
  margin: 0 0 16px;
  font-size: 15px;
  color: #303133;
}
.agent-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.agent-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  transition: all 0.2s;
}
.agent-item.agent-running {
  border-color: #409eff;
  background: #ecf5ff;
}
.agent-item.agent-success {
  border-color: #e1f3d8;
  background: #f0f9eb;
}
.agent-item.agent-failed {
  border-color: #fde2e2;
  background: #fef0f0;
}
.agent-icon {
  flex-shrink: 0;
  font-size: 20px;
}
.agent-info {
  flex: 1;
  min-width: 0;
}
.agent-name {
  font-weight: 600;
  font-size: 13px;
  color: #303133;
}
.agent-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
.agent-stats {
  font-size: 11px;
  color: #606266;
  margin-top: 3px;
}
.agent-state-tag {
  flex-shrink: 0;
}
</style>
