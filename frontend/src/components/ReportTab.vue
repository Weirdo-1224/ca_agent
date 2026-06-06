<template>
  <div v-if="report && report.sections?.length" class="report-container">
    <h2 class="report-title">{{ report.reportTitle }}</h2>
    <el-row :gutter="24">
      <el-col :xs="24" :sm="8" :md="6" class="toc-col">
        <el-anchor :offset="70">
          <el-anchor-link v-for="s in report.sections" :key="s.sectionId" :href="`#${s.sectionId}`" :title="s.title" />
        </el-anchor>
      </el-col>
      <el-col :xs="24" :sm="16" :md="18">
        <div v-for="s in report.sections" :key="s.sectionId" :id="s.sectionId" class="section">
          <h3 class="section-title">{{ s.title }}</h3>
          <pre class="section-content">{{ s.content }}</pre>
          <div v-if="s.evidenceIds?.length" class="section-meta">
            <el-tag size="small" type="info">证据: {{ s.evidenceIds.join(', ') }}</el-tag>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
  <el-empty v-else :description="isRunning(status) ? '报告生成中...' : '报告尚未生成'" />
</template>

<script setup lang="ts">
import type { ReportResponse } from '@/types';

defineProps<{
  report: ReportResponse | null;
  status?: string;
}>();

const RUNNING = new Set(['CREATED', 'PLANNING', 'COLLECTING', 'EXTRACTING', 'ANALYZING', 'WRITING', 'REVIEWING', 'REPAIRING']);
function isRunning(s?: string) {
  return s ? RUNNING.has(s) : false;
}
</script>

<style scoped>
.report-container { padding: 8px 0; }
.report-title { text-align: center; margin-bottom: 24px; font-size: 22px; }
.toc-col { max-height: 80vh; overflow-y: auto; position: sticky; top: 80px; }
.section { margin-bottom: 32px; padding-bottom: 16px; border-bottom: 1px solid #eee; }
.section-title { font-size: 18px; margin-bottom: 12px; color: #333; }
.section-content { white-space: pre-wrap; line-height: 1.8; color: #444; font-family: inherit; font-size: 14px; }
.section-meta { margin-top: 10px; }
</style>
