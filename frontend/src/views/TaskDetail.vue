<template>
  <div class="detail-page" v-loading="pageLoading">
    <!-- 404 / 错误 -->
    <div v-if="pageError" class="page-error">
      <el-empty :description="pageError">
        <el-button type="primary" @click="$router.push('/')">返回创建页</el-button>
      </el-empty>
    </div>

    <template v-else-if="task">
      <!-- 顶部状态卡片 -->
      <TaskStatusHeader
        :detail="task"
        :loading="refreshing"
        :agent-runs="agentRuns"
        :report="report"
        :evidence="evidence"
        :review="review"
        @refresh="refreshAll"
        @back="$router.push('/')"
      />

      <!-- 主体：左侧工作流 + 右侧内容 -->
      <div class="detail-body">
        <aside class="detail-aside">
          <AgentWorkflowPanel :status="task.status" :agent-runs="agentRuns" />
        </aside>

        <main class="detail-main">
          <div class="tabs-card">
            <el-tabs v-model="activeTab">
              <el-tab-pane label="概览" name="overview">
                <OverviewTab :task="task" :agent-runs="agentRuns" :report="report" :evidence="evidence" :review="review" />
              </el-tab-pane>
              <el-tab-pane label="报告" name="report">
                <ReportTab :report="report" :task-status="task.status" @search-evidence="searchEvidence" />
              </el-tab-pane>
              <el-tab-pane label="证据" name="evidence">
                <EvidenceTab :evidence="evidence" :report="report" :task-status="task.status" ref="evidenceTabRef" />
              </el-tab-pane>
              <el-tab-pane label="质检" name="review">
                <ReviewTab :review="review" :task-status="task.status" />
              </el-tab-pane>
              <el-tab-pane label="Agent 轨迹" name="runs">
                <AgentRunsTab :agent-runs="agentRuns" />
              </el-tab-pane>
            </el-tabs>
          </div>
        </main>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getTaskDetail, getReport, getEvidence, getReview, getAgentRuns } from '@/api/tasks';
import { isTerminalStatus, isRunningStatus } from '@/utils/status';
import type { TaskDetailResponse, ReportResponse, Evidence, ReviewResult, AgentRunResponse } from '@/types';

import TaskStatusHeader from '@/components/TaskStatusHeader.vue';
import AgentWorkflowPanel from '@/components/AgentWorkflowPanel.vue';
import OverviewTab from '@/components/OverviewTab.vue';
import ReportTab from '@/components/ReportTab.vue';
import EvidenceTab from '@/components/EvidenceTab.vue';
import ReviewTab from '@/components/ReviewTab.vue';
import AgentRunsTab from '@/components/AgentRunsTab.vue';

const route = useRoute();
const router = useRouter();
const taskId = route.params.taskId as string;

const pageLoading = ref(true);
const pageError = ref('');
const refreshing = ref(false);
const activeTab = ref('overview');

const task = ref<TaskDetailResponse | null>(null);
const agentRuns = ref<AgentRunResponse[]>([]);
const report = ref<ReportResponse | null>(null);
const evidence = ref<Evidence[]>([]);
const review = ref<ReviewResult | null>(null);

const evidenceTabRef = ref<InstanceType<typeof EvidenceTab> | null>(null);

// --- Polling timers ---
let taskTimer: ReturnType<typeof setInterval> | null = null;
let runsTimer: ReturnType<typeof setInterval> | null = null;
let evidenceTimer: ReturnType<typeof setInterval> | null = null;

async function loadTask() {
  try {
    task.value = await getTaskDetail(taskId);
  } catch (e: unknown) {
    if (!task.value) {
      pageError.value = e instanceof Error ? e.message : '任务不存在或加载失败';
    }
  }
}

async function loadAgentRuns() {
  try {
    agentRuns.value = await getAgentRuns(taskId);
  } catch { /* silent */ }
}

async function loadReport() {
  try {
    report.value = await getReport(taskId);
  } catch { /* silent */ }
}

async function loadEvidence() {
  try {
    evidence.value = await getEvidence(taskId);
  } catch { /* silent */ }
}

async function loadReview() {
  try {
    review.value = await getReview(taskId);
  } catch { /* silent */ }
}

async function refreshAll() {
  refreshing.value = true;
  await Promise.all([loadTask(), loadAgentRuns(), loadReport(), loadEvidence(), loadReview()]);
  refreshing.value = false;
}

async function initialLoad() {
  pageLoading.value = true;
  pageError.value = '';
  await loadTask();
  if (!pageError.value) {
    await Promise.all([loadAgentRuns(), loadReport(), loadEvidence(), loadReview()]);
  }
  pageLoading.value = false;
}

function startPolling() {
  stopPolling();
  taskTimer = setInterval(async () => {
    await loadTask();
    await loadAgentRuns();
  }, 3000);
  evidenceTimer = setInterval(() => {
    loadEvidence();
  }, 6000);
}

function stopPolling() {
  if (taskTimer) { clearInterval(taskTimer); taskTimer = null; }
  if (runsTimer) { clearInterval(runsTimer); runsTimer = null; }
  if (evidenceTimer) { clearInterval(evidenceTimer); evidenceTimer = null; }
}

watch(() => task.value?.status, (newStatus, oldStatus) => {
  if (!newStatus) return;
  const loadReportStages = ['WRITING', 'REVIEWING', 'REPAIRING'];
  if (loadReportStages.includes(newStatus) || isTerminalStatus(newStatus)) {
    loadReport();
    loadReview();
  }
  if (isTerminalStatus(newStatus) && !isTerminalStatus(oldStatus)) {
    stopPolling();
    refreshAll();
  }
});

function searchEvidence(evidenceId: string) {
  activeTab.value = 'evidence';
  setTimeout(() => {
    evidenceTabRef.value?.setSearchKeyword(evidenceId);
  }, 100);
}

onMounted(async () => {
  await initialLoad();
  if (task.value && isRunningStatus(task.value.status)) {
    startPolling();
  }
});

onUnmounted(() => {
  stopPolling();
});
</script>

<style scoped>
.detail-page {
  max-width: 1320px;
  margin: 0 auto;
  padding: 24px;
}
.page-error {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}
.detail-body {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}
.detail-aside {
  width: 280px;
  flex-shrink: 0;
  position: sticky;
  top: 88px;
}
.detail-main {
  flex: 1;
  min-width: 0;
}
.tabs-card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04), 0 4px 12px rgba(0,0,0,0.03);
  overflow: hidden;
}
.tabs-card :deep(.el-tabs__header) {
  background: #f8fafc;
  margin: 0;
  padding: 0 24px;
  border-bottom: 1px solid #e5e7eb;
}
.tabs-card :deep(.el-tabs__nav-wrap::after) {
  display: none;
}
.tabs-card :deep(.el-tabs__item) {
  height: 48px;
  line-height: 48px;
  font-size: 14px;
  color: #6b7280;
}
.tabs-card :deep(.el-tabs__item.is-active) {
  color: #2563eb;
  font-weight: 600;
}
.tabs-card :deep(.el-tabs__active-bar) {
  background-color: #2563eb;
}
.tabs-card :deep(.el-tabs__content) {
  padding: 24px;
}

@media (max-width: 1000px) {
  .detail-body {
    flex-direction: column;
  }
  .detail-aside {
    width: 100%;
    position: static;
  }
}
</style>
