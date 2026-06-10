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
            <div class="report-header-left">
              <h3 class="report-title">{{ report.reportTitle }}</h3>
              <span class="format-badge">{{ report.reportFormat }}</span>
            </div>
            <div class="reading-mode-switch">
              <span
                class="mode-option"
                :class="{ active: readingMode === 'clean' }"
                @click="readingMode = 'clean'"
              >简洁阅读</span>
              <span class="mode-divider">/</span>
              <span
                class="mode-option"
                :class="{ active: readingMode === 'evidence' }"
                @click="readingMode = 'evidence'"
              >显示证据</span>
            </div>
          </div>
          <div
            v-for="(s, i) in report.sections"
            :key="s.sectionId"
            :ref="(el) => setSectionRef(i, el)"
            class="section-card"
          >
            <!-- 章节标题行 + 右上角证据链按钮 -->
            <div class="section-top">
              <h4 class="section-heading">{{ s.title }}</h4>
              <div v-if="s.evidenceIds?.length || s.relatedClaimIds?.length" class="section-top-tags">
                <span v-if="s.evidenceIds?.length" class="top-tag tag-evidence" @click="openDrawer(s)">证据 {{ s.evidenceIds.length }}</span>
                <span v-if="s.relatedClaimIds?.length" class="top-tag tag-claim" @click="openDrawer(s)">结论 {{ s.relatedClaimIds.length }}</span>
              </div>
            </div>

            <!-- 正文（Markdown 渲染） -->
            <div class="section-text markdown-body" v-html="renderMarkdown(s.content)"></div>

            <!-- 简洁模式：底部轻量证据摘要栏 -->
            <div v-if="readingMode === 'clean' && (s.evidenceIds?.length || s.relatedClaimIds?.length)" class="evidence-summary-bar" @click="openDrawer(s)">
              <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
              <span class="summary-text">
                证据链：{{ s.evidenceIds?.length || 0 }} 条证据，{{ s.relatedClaimIds?.length || 0 }} 条结论
              </span>
              <span class="summary-action">查看详情</span>
            </div>

            <!-- 显示证据模式：展开证据和结论列表 -->
            <div v-if="readingMode === 'evidence' && (s.evidenceIds?.length || s.relatedClaimIds?.length)" class="inline-evidence-block">
              <div v-if="s.evidenceIds?.length" class="inline-ref-row">
                <span class="inline-ref-label">关联证据</span>
                <span
                  v-for="eid in s.evidenceIds"
                  :key="eid"
                  class="inline-badge evidence-badge"
                  @click="$emit('searchEvidence', eid)"
                  title="点击跳转到证据 Tab"
                >{{ eid }}</span>
              </div>
              <div v-if="s.relatedClaimIds?.length" class="inline-ref-row">
                <span class="inline-ref-label">关联结论</span>
                <span
                  v-for="cid in s.relatedClaimIds"
                  :key="cid"
                  class="inline-badge claim-badge"
                  @click="copyId(cid)"
                  title="点击复制 ID"
                >{{ cid }}</span>
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

    <!-- 证据链详情 Drawer -->
    <el-drawer v-model="drawerVisible" title="证据链详情" size="480px" direction="rtl" :append-to-body="true">
      <template v-if="drawerSection">
        <div class="drawer-body">
          <!-- 章节标题 -->
          <div class="drawer-section-title">{{ drawerSection.title }}</div>

          <!-- 关联证据 -->
          <div class="drawer-group">
            <div class="drawer-group-header">
              <span class="group-label">关联证据</span>
              <span class="group-count">{{ drawerSection.evidenceIds?.length || 0 }}</span>
            </div>
            <div v-if="drawerSection.evidenceIds?.length" class="badge-wrap">
              <span
                v-for="eid in drawerSection.evidenceIds"
                :key="eid"
                class="drawer-badge evidence-badge"
                @click="copyId(eid)"
                title="点击复制 ID"
              >
                {{ eid }}
                <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              </span>
            </div>
            <div v-else class="no-data">暂无关联证据</div>
          </div>

          <!-- 关联结论 -->
          <div class="drawer-group">
            <div class="drawer-group-header">
              <span class="group-label">关联结论</span>
              <span class="group-count">{{ drawerSection.relatedClaimIds?.length || 0 }}</span>
            </div>
            <div v-if="drawerSection.relatedClaimIds?.length" class="badge-wrap">
              <span
                v-for="cid in drawerSection.relatedClaimIds"
                :key="cid"
                class="drawer-badge claim-badge"
                @click="copyId(cid)"
                title="点击复制 ID"
              >
                {{ cid }}
                <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              </span>
            </div>
            <div v-else class="no-data">暂无关联结论</div>
          </div>

          <!-- 跳转操作 -->
          <div v-if="drawerSection.evidenceIds?.length" class="drawer-actions">
            <button class="jump-btn" @click="jumpToEvidence">
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
              跳转到证据 Tab
            </button>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { marked } from 'marked';
import type { ReportResponse, ReportSection } from '@/types';
import { isRunningStatus } from '@/utils/status';

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true,
});

const props = defineProps<{
  report: ReportResponse | null;
  taskStatus?: string;
}>();

const emit = defineEmits<{
  searchEvidence: [evidenceId: string];
}>();

const readingMode = ref<'clean' | 'evidence'>('clean');

/** 将 Markdown 内容渲染为 HTML */
function renderMarkdown(content: string | undefined | null): string {
  if (!content) return '';
  return marked.parse(content) as string;
}
const activeIndex = ref(0);
const sectionRefs: Record<number, HTMLElement | null> = {};

function setSectionRef(i: number, el: unknown) {
  sectionRefs[i] = el as HTMLElement | null;
}

function scrollTo(i: number) {
  activeIndex.value = i;
  sectionRefs[i]?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

const emptyText = computed(() => {
  if (isRunningStatus(props.taskStatus)) return '报告生成中...';
  return '报告尚未生成或未保存';
});

// Drawer logic
const drawerVisible = ref(false);
const drawerSection = ref<ReportSection | null>(null);

function openDrawer(section: ReportSection) {
  drawerSection.value = section;
  drawerVisible.value = true;
}

function copyId(id: string) {
  navigator.clipboard.writeText(id);
  ElMessage.success('ID 已复制');
}

function jumpToEvidence() {
  if (drawerSection.value?.evidenceIds?.length) {
    drawerVisible.value = false;
    emit('searchEvidence', drawerSection.value.evidenceIds[0]);
  }
}
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
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 24px;
}
.report-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* Reading mode switch */
.reading-mode-switch {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  flex-shrink: 0;
}
.mode-option {
  font-size: 12px;
  color: #9ca3af;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 6px;
  transition: all 0.15s;
  user-select: none;
}
.mode-option:hover { color: #6b7280; }
.mode-option.active {
  color: #2563eb;
  background: #eff6ff;
  font-weight: 600;
}
.mode-divider {
  font-size: 12px;
  color: #d1d5db;
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

/* Section top: title + right tags */
.section-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}
.section-heading {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}
.section-top-tags {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}
.top-tag {
  display: inline-flex;
  align-items: center;
  padding: 3px 9px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.15s;
}
.top-tag:hover { opacity: 0.7; }
.top-tag.tag-evidence { background: #dbeafe; color: #1e40af; }
.top-tag.tag-claim { background: #ffedd5; color: #c2410c; }

.section-text {
  font-size: 14px;
  line-height: 1.75;
  color: #374151;
  margin: 0;
}

/* Markdown 渲染样式 */
.markdown-body :deep(h1) {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin: 16px 0 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid #f3f4f6;
}
.markdown-body :deep(h2) {
  font-size: 17px;
  font-weight: 600;
  color: #1f2937;
  margin: 14px 0 6px;
}
.markdown-body :deep(h3) {
  font-size: 15px;
  font-weight: 600;
  color: #374151;
  margin: 12px 0 4px;
}
.markdown-body :deep(h4),
.markdown-body :deep(h5),
.markdown-body :deep(h6) {
  font-size: 14px;
  font-weight: 600;
  color: #4b5563;
  margin: 10px 0 4px;
}
.markdown-body :deep(p) {
  margin: 6px 0;
}
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 6px 0;
  padding-left: 24px;
}
.markdown-body :deep(li) {
  margin: 3px 0;
}
.markdown-body :deep(strong) {
  font-weight: 600;
  color: #111827;
}
.markdown-body :deep(em) {
  font-style: italic;
  color: #6b7280;
}
.markdown-body :deep(code) {
  background: #f3f4f6;
  padding: 1px 5px;
  border-radius: 4px;
  font-size: 13px;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  color: #d6336c;
}
.markdown-body :deep(pre) {
  background: #f8f9fa;
  padding: 12px 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
  border: 1px solid #e5e7eb;
}
.markdown-body :deep(pre code) {
  background: none;
  padding: 0;
  color: #374151;
}
.markdown-body :deep(blockquote) {
  border-left: 3px solid #d1d5db;
  padding-left: 12px;
  margin: 8px 0;
  color: #6b7280;
}
.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 8px 0;
  font-size: 13px;
}
.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #e5e7eb;
  padding: 6px 10px;
  text-align: left;
}
.markdown-body :deep(th) {
  background: #f9fafb;
  font-weight: 600;
}
.markdown-body :deep(hr) {
  border: none;
  border-top: 1px solid #e5e7eb;
  margin: 12px 0;
}

/* Evidence summary bar */
.evidence-summary-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #f1f5f9;
  cursor: pointer;
  transition: all 0.15s;
  color: #6b7280;
}
.evidence-summary-bar:hover {
  background: #f1f5f9;
  border-color: #e2e8f0;
}
.summary-text {
  font-size: 12px;
  flex: 1;
}
.summary-action {
  font-size: 12px;
  color: #2563eb;
  font-weight: 500;
}

/* Drawer styles */
.drawer-body {
  padding: 0 4px;
}
.drawer-section-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  padding-bottom: 16px;
  margin-bottom: 20px;
  border-bottom: 1px solid #f3f4f6;
}
.drawer-group {
  margin-bottom: 24px;
}
.drawer-group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.group-label {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}
.group-count {
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 10px;
  background: #f3f4f6;
  color: #6b7280;
}
.badge-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.drawer-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 500;
  font-family: 'JetBrains Mono', monospace;
  cursor: pointer;
  transition: opacity 0.15s;
  word-break: break-all;
}
.drawer-badge:hover { opacity: 0.7; }
.drawer-badge.evidence-badge { background: #dbeafe; color: #1e40af; }
.drawer-badge.claim-badge { background: #ffedd5; color: #c2410c; }
.no-data {
  font-size: 13px;
  color: #9ca3af;
  padding: 8px 0;
}

.drawer-actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f3f4f6;
}
.jump-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid #2563eb;
  background: #eff6ff;
  color: #2563eb;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}
.jump-btn:hover {
  background: #dbeafe;
}

/* Inline evidence block (evidence mode) */
.inline-evidence-block {
  margin-top: 16px;
  padding: 12px 14px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #f1f5f9;
}
.inline-ref-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}
.inline-ref-row:last-child { margin-bottom: 0; }
.inline-ref-label {
  font-size: 11px;
  color: #6b7280;
  font-weight: 600;
  margin-right: 4px;
  flex-shrink: 0;
}
.inline-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 500;
  font-family: 'JetBrains Mono', monospace;
  cursor: pointer;
  transition: opacity 0.15s;
  word-break: break-all;
}
.inline-badge:hover { opacity: 0.7; }
.inline-badge.evidence-badge { background: #dbeafe; color: #1e40af; }
.inline-badge.claim-badge { background: #ffedd5; color: #c2410c; }

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
