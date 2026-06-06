<template>
  <div class="detail-page">
    <el-page-header @back="router.back()" :title="detail?.taskName || '任务详情'" />

    <el-tabs v-model="activeTab" class="detail-tabs">
      <el-tab-pane label="概览" name="overview">
        <OverviewTab :detail="detail" :review="review" :agentRuns="agentRuns" />
      </el-tab-pane>
      <el-tab-pane label="报告" name="report">
        <ReportTab :report="report" :status="detail?.status" />
      </el-tab-pane>
      <el-tab-pane label="证据" name="evidence">
        <EvidenceTab :evidence="evidence" />
      </el-tab-pane>
      <el-tab-pane label="质检" name="review">
        <ReviewTab :review="review" :status="detail?.status" />
      </el-tab-pane>
      <el-tab-pane label="Agent 轨迹" name="agents">
        <AgentRunsTab :agentRuns="agentRuns" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { getTaskDetail, getReport, getEvidence, getReview, getAgentRuns } from '@/api/tasks';
import type { TaskDetailResponse, ReportResponse, Evidence, ReviewResult, AgentRunResponse } from '@/types';
import OverviewTab from '@/components/OverviewTab.vue';
import ReportTab from '@/components/ReportTab.vue';
import EvidenceTab from '@/components/EvidenceTab.vue';
import ReviewTab from '@/components/ReviewTab.vue';
import AgentRunsTab from '@/components/AgentRunsTab.vue';

const props = defineProps<{ taskId: string }>();
const router = useRouter();

const activeTab = ref('overview');
const detail = ref<TaskDetailResponse | null>(null);
const report = ref<ReportResponse | null>(null);
const evidence = ref<Evidence[]>([]);
const review = ref<ReviewResult | null>(null);
const agentRuns = ref<AgentRunResponse[]>([]);
const pollingTimer = ref<ReturnType<typeof setInterval> | null>(null);

const RUNNING_STATUSES = new Set([
  'CREATED', 'PLANNING', 'COLLECTING', 'EXTRACTING',
  'ANALYZING', 'WRITING', 'REVIEWING', 'REPAIRING',
]);



function isRunning(status?: string) {
  return status ? RUNNING_STATUSES.has(status) : false;
}

async function fetchAll() {
  try {
    const [d, r, e, rev, ar] = await Promise.all([
      getTaskDetail(props.taskId),
      getReport(props.taskId).catch(() => null),
      getEvidence(props.taskId).catch(() => []),
      getReview(props.taskId).catch(() => null),
      getAgentRuns(props.taskId).catch(() => []),
    ]);
    detail.value = d;
    report.value = r;
    evidence.value = e;
    review.value = rev;
    agentRuns.value = ar;

    if (!isRunning(d.status) && pollingTimer.value) {
      clearInterval(pollingTimer.value);
      pollingTimer.value = null;
    }
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '加载失败';
    if (msg.includes('not found') || msg.includes('404')) {
      detail.value = null;
    }
  }
}

onMounted(() => {
  fetchAll();
  pollingTimer.value = setInterval(() => {
    if (detail.value && isRunning(detail.value.status)) {
      fetchAll();
    }
  }, 3000);
});

onUnmounted(() => {
  if (pollingTimer.value) clearInterval(pollingTimer.value);
});
</script>

<style scoped>
.detail-page {
  max-width: 1200px;
  margin: 20px auto;
  padding: 0 16px;
}
.detail-tabs {
  margin-top: 20px;
}
</style>
