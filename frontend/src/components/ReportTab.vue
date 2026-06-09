<template>
  <div class="report-tab">
    <template v-if="report && report.sections && report.sections.length > 0">
      <div class="report-layout">
        <!-- 左侧章节目录 -->
        <nav class="section-nav">
          <div class="nav-title">章节目录</div>
          <ul>
            <li
              v-for="(s, i) in report.sections"
              :key="s.sectionId"
              :class="{ active: activeIndex === i }"
              @click="scrollTo(i)"
            >
              <span class="nav-dot"></span>
              {{ s.title }}
            </li>
          </ul>
        </nav>

        <!-- 右侧正文 -->
        <div class="section-content">
          <div class="report-header">
            <h3 class="report-title">{{ report.reportTitle }}</h3>
            <span class="format-badge">{{ report.reportFormat }}</span>
          </div>
          <div
            v-for="(s, i) in report.sections"
            :key="s.sectionId"
            :ref="(el) => setSectionRef(i, el)"
            class="section-card"
          >
            <h4 class="section-heading">{{ s.title }}</h4>
            <pre class="section-text">{{ s.content }}</pre>

            <!-- 关联证据 & Claim -->
            <div class="section-refs" v-if="s.evidenceIds?.length || s.relatedClaimIds?.length">
              <div v-if="s.evidenceIds?.length" class="ref-row">
                <span class="ref-label">关联证据</span>
                <span
                  v-for="eid in s.evidenceIds"
                  :key="eid"
                  class="ref-badge evidence-badge"
                  @click="$emit('searchEvidence', eid)"
                  title="点击跳转到证据 Tab"
                >{{ shortId(eid) }}</span>
              </div>
              <div v-if="s.relatedClaimIds?.length" class="ref-row">
                <span class="ref-label">关联结论</span>
                <span v-for="cid in s.relatedClaimIds" :key="cid" class="ref-badge claim-badge">{{ shortId(cid) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#d1d5db" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
      <p>{{ emptyText }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import type { ReportResponse } from '@/types';
import { isRunningStatus } from '@/utils/status';

const props = defineProps<{
  report: ReportResponse | null;
  taskStatus?: string;
}>();

defineEmits<{
  searchEvidence: [evidenceId: string];
}>();

const activeIndex = ref(0);
const sectionRefs: Record<number, HTMLElement | null> = {};

function setSectionRef(i: number, el: unknown) {
  sectionRefs[i] = el as HTMLElement | null;
}

function scrollTo(i: number) {
  activeIndex.value = i;
  sectionRefs[i]?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function shortId(id: string): string {
  if (!id) return '';
  return id.length > 8 ? '...' + id.slice(-6) : id;
}

const emptyText = computed(() => {
  if (isRunningStatus(props.taskStatus)) return '报告生成中...';
  return '报告尚未生成或未保存';
});
</script>

<style scoped>
.report-tab {
  min-height: 300px;
}
.report-layout {
  display: flex;
  gap: 20px;
}

/* 左侧目录 */
.section-nav {
  width: 180px;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}
.nav-title {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 12px;
}
.section-nav ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
.section-nav li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  font-size: 13px;
  border-radius: 8px;
  cursor: pointer;
  color: #6b7280;
  margin-bottom: 2px;
  transition: all 0.15s;
}
.section-nav li:hover {
  background: #f8fafc;
  color: #374151;
}
.section-nav li.active {
  background: #eff6ff;
  color: #2563eb;
  font-weight: 600;
}
.nav-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #d1d5db;
  flex-shrink: 0;
}
.section-nav li.active .nav-dot {
  background: #2563eb;
}

/* 右侧正文 */
.section-content {
  flex: 1;
  min-width: 0;
  max-width: 860px;
}
.report-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}
.report-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}
.format-badge {
  padding: 2px 10px;
  border-radius: 12px;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 12px;
}

.section-card {
  margin-bottom: 20px;
  padding: 20px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #f1f5f9;
}
.section-heading {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}
.section-text {
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.75;
  color: #374151;
  margin: 0;
}

.section-refs {
  margin-top: 16px;
  padding: 12px 14px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #f1f5f9;
}
.ref-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
}
.ref-row:last-child { margin-bottom: 0; }
.ref-label {
  font-size: 11px;
  color: #6b7280;
  font-weight: 600;
  margin-right: 4px;
}
.ref-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
  font-family: 'JetBrains Mono', monospace;
  cursor: pointer;
  transition: opacity 0.15s;
}
.ref-badge:hover { opacity: 0.75; }
.evidence-badge { background: #dbeafe; color: #1e40af; }
.claim-badge { background: #ffedd5; color: #c2410c; cursor: default; }

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
</style>
