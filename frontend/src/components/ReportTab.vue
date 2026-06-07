<template>
  <div class="report-tab">
    <template v-if="report && report.sections && report.sections.length > 0">
      <div class="report-layout">
        <!-- 左侧章节目录 -->
        <nav class="section-nav">
          <h4>章节目录</h4>
          <ul>
            <li
              v-for="(s, i) in report.sections"
              :key="s.sectionId"
              :class="{ active: activeIndex === i }"
              @click="scrollTo(i)"
            >
              {{ s.title }}
            </li>
          </ul>
        </nav>

        <!-- 右侧正文 -->
        <div class="section-content">
          <div class="report-header">
            <h3>{{ report.reportTitle }}</h3>
            <el-tag size="small" type="info">{{ report.reportFormat }}</el-tag>
          </div>
          <div
            v-for="(s, i) in report.sections"
            :key="s.sectionId"
            :ref="(el) => setSectionRef(i, el)"
            class="section-block"
          >
            <h4 class="section-heading">{{ s.title }}</h4>
            <pre class="section-text">{{ s.content }}</pre>

            <!-- 关联证据 & Claim -->
            <div class="section-refs" v-if="s.evidenceIds?.length || s.relatedClaimIds?.length">
              <div v-if="s.evidenceIds?.length" class="ref-row">
                <span class="ref-label">关联证据：</span>
                <el-tag
                  v-for="eid in s.evidenceIds"
                  :key="eid"
                  size="small"
                  class="ref-tag"
                  @click="$emit('searchEvidence', eid)"
                  style="cursor:pointer"
                >{{ eid }}</el-tag>
              </div>
              <div v-if="s.relatedClaimIds?.length" class="ref-row">
                <span class="ref-label">关联结论：</span>
                <el-tag v-for="cid in s.relatedClaimIds" :key="cid" size="small" type="warning" class="ref-tag">{{ cid }}</el-tag>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 空状态 -->
    <el-empty v-else :image-size="80" :description="emptyText" />
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
  gap: 16px;
}
.section-nav {
  width: 180px;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}
.section-nav h4 {
  margin: 0 0 8px;
  font-size: 13px;
  color: #909399;
}
.section-nav ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
.section-nav li {
  padding: 6px 10px;
  font-size: 13px;
  border-radius: 4px;
  cursor: pointer;
  color: #606266;
  border-left: 2px solid transparent;
  margin-bottom: 2px;
}
.section-nav li:hover {
  background: #f5f7fa;
}
.section-nav li.active {
  color: #409eff;
  border-left-color: #409eff;
  background: #ecf5ff;
}
.section-content {
  flex: 1;
  min-width: 0;
}
.report-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.report-header h3 {
  margin: 0;
}
.section-block {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}
.section-heading {
  margin: 0 0 8px;
  font-size: 15px;
  color: #303133;
}
.section-text {
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.7;
  color: #4a4a4a;
  margin: 0;
  background: #fafafa;
  padding: 12px;
  border-radius: 4px;
}
.section-refs {
  margin-top: 10px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
}
.ref-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 4px;
}
.ref-label {
  font-size: 12px;
  color: #909399;
}
.ref-tag {
  margin-right: 0;
}
</style>
