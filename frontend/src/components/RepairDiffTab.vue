<template>
  <div class="repair-diff-tab">
    <!-- 空状态 -->
    <div v-if="!repairDiffs.length" class="empty-state">
      <el-empty description="暂无修复记录">
        <template #default>
          <div class="empty-hint">
            <p v-if="taskStatus === 'COMPLETED' || taskStatus === 'COMPLETED_WITH_WARNINGS'">
              本次任务一次通过质检，未触发修复流程
            </p>
            <p v-else>
              任务运行中或尚未触发修复，请稍后查看
            </p>
          </div>
        </template>
      </el-empty>
    </div>

    <!-- 时间线展示 -->
    <template v-else>
      <!-- 统计概览 -->
      <div class="diff-overview">
        <div class="overview-card">
          <div class="ov-number">{{ repairDiffs.length }}</div>
          <div class="ov-label">修复轮次</div>
        </div>
        <div class="overview-card">
          <div class="ov-number score-up">
            {{ totalScoreDelta > 0 ? '+' : '' }}{{ totalScoreDelta }}
          </div>
          <div class="ov-label">总分提升</div>
        </div>
        <div class="overview-card">
          <div class="ov-number">{{ totalFixedIssues }}</div>
          <div class="ov-label">累计修复问题</div>
        </div>
        <div class="overview-card">
          <div class="ov-number">{{ totalAddedEvidence }}</div>
          <div class="ov-label">累计新增证据</div>
        </div>
      </div>

      <!-- 时间线 -->
      <el-timeline class="diff-timeline">
        <el-timeline-item
          v-for="diff in repairDiffs"
          :key="diff.iteration"
          :timestamp="`第 ${diff.iteration} 轮修复`"
          placement="top"
          :color="getTimelineColor(diff)"
          :hollow="false"
          size="large"
        >
          <div class="diff-card">
            <!-- 卡片头部 -->
            <div class="diff-card-header">
              <div class="diff-agent-tag">
                <el-tag :type="getAgentTagType(diff.targetAgent)" effect="dark" size="small">
                  {{ formatAgentName(diff.targetAgent) }}
                </el-tag>
              </div>
              <div class="diff-score-change" v-if="diff.beforeScore != null && diff.afterScore != null">
                <span class="score-before">{{ diff.beforeScore }}</span>
                <span class="score-arrow">→</span>
                <span class="score-after" :class="diff.afterScore >= diff.beforeScore ? 'up' : 'down'">
                  {{ diff.afterScore }}
                </span>
                <span class="score-delta" :class="diff.afterScore >= diff.beforeScore ? 'up' : 'down'">
                  ({{ diff.afterScore - diff.beforeScore > 0 ? '+' : '' }}{{ diff.afterScore - diff.beforeScore }})
                </span>
              </div>
            </div>

            <!-- 指标网格 -->
            <div class="diff-metrics">
              <div class="metric-item">
                <span class="metric-label">问题数</span>
                <span class="metric-value">
                  {{ diff.beforeIssueCount ?? '-' }}
                  <span class="metric-arrow">→</span>
                  {{ diff.afterIssueCount ?? '-' }}
                  <span v-if="diff.fixedIssueCount != null && diff.fixedIssueCount > 0" class="metric-fix">
                    (修复 {{ diff.fixedIssueCount }})
                  </span>
                </span>
              </div>
              <div class="metric-item">
                <span class="metric-label">新增证据</span>
                <span class="metric-value" :class="{ highlight: diff.addedEvidenceIds?.length }">
                  {{ diff.addedEvidenceIds?.length ?? 0 }} 条
                </span>
              </div>
              <div class="metric-item">
                <span class="metric-label">新增结论</span>
                <span class="metric-value" :class="{ highlight: diff.addedClaimIds?.length }">
                  {{ diff.addedClaimIds?.length ?? 0 }} 条
                </span>
              </div>
              <div class="metric-item">
                <span class="metric-label">修改章节</span>
                <span class="metric-value" :class="{ highlight: diff.changedSectionTitles?.length }">
                  {{ diff.changedSectionTitles?.length ?? 0 }} 个
                </span>
              </div>
            </div>

            <!-- 新增证据 IDs (可点击) -->
            <div v-if="diff.addedEvidenceIds?.length" class="diff-evidence-section">
              <div class="section-label">新增证据</div>
              <div class="evidence-tags">
                <el-tag
                  v-for="evId in diff.addedEvidenceIds"
                  :key="evId"
                  size="small"
                  type="success"
                  class="clickable-tag"
                  @click="$emit('searchEvidence', evId)"
                >
                  {{ evId }}
                </el-tag>
              </div>
            </div>

            <!-- 修改章节 (可点击) -->
            <div v-if="diff.changedSectionTitles?.length" class="diff-sections-section">
              <div class="section-label">修改章节</div>
              <div class="section-tags">
                <el-tag
                  v-for="title in diff.changedSectionTitles"
                  :key="title"
                  size="small"
                  type="warning"
                  class="clickable-tag"
                  @click="$emit('goToSection', title)"
                >
                  {{ title }}
                </el-tag>
              </div>
            </div>

            <!-- 涉及产品 -->
            <div v-if="diff.changedProducts?.length" class="diff-products">
              <span class="section-label">涉及产品：</span>
              <el-tag v-for="p in diff.changedProducts" :key="p" size="small" effect="plain">{{ p }}</el-tag>
            </div>

            <!-- 总结 -->
            <div v-if="diff.summary" class="diff-summary">
              {{ diff.summary }}
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { RepairDiff } from '@/types';

const props = defineProps<{
  repairDiffs: RepairDiff[];
  taskStatus?: string;
}>();

defineEmits<{
  (e: 'searchEvidence', evidenceId: string): void;
  (e: 'goToSection', sectionTitle: string): void;
}>();

/** 总分提升 = 最后一轮 afterScore - 第一轮 beforeScore */
const totalScoreDelta = computed(() => {
  const diffs = props.repairDiffs;
  if (!diffs.length) return 0;
  const first = diffs[0];
  const last = diffs[diffs.length - 1];
  return (last.afterScore ?? 0) - (first.beforeScore ?? 0);
});

const totalFixedIssues = computed(() =>
  props.repairDiffs.reduce((sum, d) => sum + (d.fixedIssueCount ?? 0), 0)
);

const totalAddedEvidence = computed(() =>
  props.repairDiffs.reduce((sum, d) => sum + (d.addedEvidenceIds?.length ?? 0), 0)
);

function getTimelineColor(diff: RepairDiff): string {
  if (diff.afterScore == null || diff.beforeScore == null) return '#909399';
  if (diff.afterScore > diff.beforeScore) return '#67c23a';
  if (diff.afterScore === diff.beforeScore) return '#e6a23c';
  return '#f56c6c';
}

function getAgentTagType(agent: string): string {
  const map: Record<string, string> = {
    COLLECTOR_AGENT: '',
    EXTRACTOR_AGENT: 'success',
    ANALYZER_AGENT: 'warning',
    WRITER_AGENT: 'info',
  };
  return map[agent] ?? '';
}

function formatAgentName(agent: string): string {
  const map: Record<string, string> = {
    PLANNER_AGENT: 'Planner',
    COLLECTOR_AGENT: 'Collector',
    EXTRACTOR_AGENT: 'Extractor',
    ANALYZER_AGENT: 'Analyzer',
    WRITER_AGENT: 'Writer',
    REVIEWER_AGENT: 'Reviewer',
  };
  return map[agent] ?? agent;
}
</script>

<style scoped>
.repair-diff-tab {
  padding: 0;
}

/* 空状态 */
.empty-state {
  padding: 48px 0;
}
.empty-hint {
  color: #9ca3af;
  font-size: 14px;
}

/* 统计概览 */
.diff-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}
.overview-card {
  background: #f8fafc;
  border-radius: 12px;
  padding: 16px;
  text-align: center;
  border: 1px solid #e5e7eb;
}
.ov-number {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}
.ov-number.score-up {
  color: #16a34a;
}
.ov-label {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}

/* 时间线 */
.diff-timeline {
  padding: 0 4px;
}
.diff-timeline :deep(.el-timeline-item__timestamp) {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

/* 卡片 */
.diff-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
}
.diff-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

/* 分数变化 */
.diff-score-change {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 600;
}
.score-before {
  color: #6b7280;
}
.score-arrow {
  color: #9ca3af;
  font-size: 14px;
}
.score-after.up {
  color: #16a34a;
}
.score-after.down {
  color: #dc2626;
}
.score-delta {
  font-size: 13px;
  font-weight: 500;
}
.score-delta.up {
  color: #16a34a;
}
.score-delta.down {
  color: #dc2626;
}

/* 指标网格 */
.diff-metrics {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}
.metric-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 10px;
  background: #f9fafb;
  border-radius: 8px;
}
.metric-label {
  font-size: 11px;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.metric-value {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}
.metric-value.highlight {
  color: #2563eb;
  font-weight: 600;
}
.metric-arrow {
  color: #9ca3af;
  margin: 0 2px;
}
.metric-fix {
  font-size: 12px;
  color: #16a34a;
  font-weight: 400;
}

/* 证据/章节标签 */
.diff-evidence-section,
.diff-sections-section {
  margin-bottom: 10px;
}
.section-label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 6px;
  font-weight: 500;
}
.evidence-tags,
.section-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.clickable-tag {
  cursor: pointer;
  transition: opacity 0.15s;
}
.clickable-tag:hover {
  opacity: 0.75;
}

/* 涉及产品 */
.diff-products {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}
.diff-products .section-label {
  margin-bottom: 0;
  margin-right: 4px;
}

/* 总结 */
.diff-summary {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.6;
  padding: 10px 12px;
  background: #f0fdf4;
  border-radius: 8px;
  border-left: 3px solid #22c55e;
}

@media (max-width: 600px) {
  .diff-overview {
    grid-template-columns: repeat(2, 1fr);
  }
  .diff-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
