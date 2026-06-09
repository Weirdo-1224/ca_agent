<template>
  <div class="evidence-tab">
    <template v-if="evidence.length > 0">
      <!-- 统计条 -->
      <div class="stats-bar">
        <div class="stat-chip" :class="{ active: filterQuick === '' }" @click="setQuickFilter('')">
          全部 <strong>{{ evidence.length }}</strong>
        </div>
        <div class="stat-chip" :class="{ active: filterQuick === 'referenced' }" @click="setQuickFilter('referenced')">
          已引用 <strong>{{ referencedCount }}</strong>
        </div>
        <div class="stat-chip" :class="{ active: filterQuick === 'HIGH' }" @click="setQuickFilter('HIGH')">
          高可靠 <strong>{{ highCount }}</strong>
        </div>
        <div class="stat-chip" :class="{ active: filterQuick === 'MEDIUM' }" @click="setQuickFilter('MEDIUM')">
          中可靠 <strong>{{ mediumCount }}</strong>
        </div>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-toolbar">
        <el-select v-model="filterProduct" placeholder="产品" clearable size="default" style="width:140px">
          <el-option v-for="p in products" :key="p" :label="p" :value="p" />
        </el-select>
        <el-select v-model="filterSource" placeholder="来源类型" clearable size="default" style="width:140px">
          <el-option v-for="s in sourceTypes" :key="s" :label="s" :value="s" />
        </el-select>
        <el-select v-model="filterReliability" placeholder="可靠性" clearable size="default" style="width:120px">
          <el-option v-for="r in reliabilities" :key="r" :label="r" :value="r" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索 ID / 标题 / 摘要" clearable size="default" style="width:220px" :prefix-icon="Search" />
      </div>

      <!-- 表格 -->
      <div class="table-wrap">
        <el-table
          :data="filtered"
          style="width:100%"
          :header-cell-style="{ background: '#f8fafc', color: '#374151', fontWeight: 600 }"
          :row-style="{ cursor: 'pointer' }"
          highlight-current-row
          @row-click="openDrawer"
        >
          <el-table-column prop="evidenceId" label="ID" width="90">
            <template #default="{ row }">
              <span class="mono-id">{{ row.evidenceId?.slice(-6) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="productName" label="产品" width="110" />
          <el-table-column prop="sourceType" label="来源" width="100">
            <template #default="{ row }">
              <span class="source-chip">{{ row.sourceType }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="sourceTitle" label="标题" min-width="180" show-overflow-tooltip />
          <el-table-column prop="contentSnippet" label="摘要" min-width="200" show-overflow-tooltip />
          <el-table-column label="链接" width="70" align="center">
            <template #default="{ row }">
              <a v-if="row.url" :href="row.url" target="_blank" class="link-btn" @click.stop>打开</a>
              <span v-else class="text-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column prop="reliability" label="可靠性" width="80" align="center">
            <template #default="{ row }">
              <span class="reliability-tag" :class="'r-' + row.reliability?.toLowerCase()">{{ row.reliability }}</span>
            </template>
          </el-table-column>
          <el-table-column label="引用" width="70" align="center">
            <template #default="{ row }">
              <span class="ref-tag" :class="isReferenced(row.evidenceId) ? 'ref-yes' : 'ref-no'">
                {{ isReferenced(row.evidenceId) ? '已引用' : '未引用' }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="table-footer">
        显示 {{ filtered.length }} / {{ evidence.length }} 条证据
      </div>
    </template>

    <div v-else class="empty-state">
      <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#d1d5db" stroke-width="1.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
      <p>{{ emptyText }}</p>
    </div>

    <!-- 详情 Drawer -->
    <el-drawer v-model="drawerVisible" title="证据详情" size="480px" direction="rtl">
      <template v-if="selectedEvidence">
        <div class="drawer-content">
          <div class="drawer-section">
            <div class="drawer-field">
              <span class="field-label">Evidence ID</span>
              <span class="field-value mono-id">{{ selectedEvidence.evidenceId }}</span>
            </div>
            <div class="drawer-field">
              <span class="field-label">产品</span>
              <span class="field-value">{{ selectedEvidence.productName }}</span>
            </div>
            <div class="drawer-field">
              <span class="field-label">来源类型</span>
              <span class="field-value">{{ selectedEvidence.sourceType }}</span>
            </div>
            <div class="drawer-field">
              <span class="field-label">标题</span>
              <span class="field-value">{{ selectedEvidence.sourceTitle }}</span>
            </div>
            <div class="drawer-field">
              <span class="field-label">URL</span>
              <span class="field-value">
                <a v-if="selectedEvidence.url" :href="selectedEvidence.url" target="_blank" class="link-btn">{{ selectedEvidence.url }}</a>
                <span v-else class="text-muted">—</span>
              </span>
            </div>
            <div class="drawer-field">
              <span class="field-label">可靠性</span>
              <span class="reliability-tag" :class="'r-' + selectedEvidence.reliability?.toLowerCase()">{{ selectedEvidence.reliability }}</span>
            </div>
            <div class="drawer-field">
              <span class="field-label">是否被引用</span>
              <span class="ref-tag" :class="isReferenced(selectedEvidence.evidenceId) ? 'ref-yes' : 'ref-no'">
                {{ isReferenced(selectedEvidence.evidenceId) ? '已引用' : '未引用' }}
              </span>
            </div>
          </div>
          <div class="drawer-section">
            <span class="field-label">摘要全文</span>
            <div class="snippet-box">{{ selectedEvidence.contentSnippet || '—' }}</div>
          </div>
          <div v-if="relatedSections.length > 0" class="drawer-section">
            <span class="field-label">关联报告章节</span>
            <div class="related-chips">
              <span v-for="sec in relatedSections" :key="sec" class="related-chip">{{ sec }}</span>
            </div>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { Search } from '@element-plus/icons-vue';
import type { Evidence, ReportResponse } from '@/types';
import { isRunningStatus } from '@/utils/status';

const props = defineProps<{
  evidence: Evidence[];
  report: ReportResponse | null;
  taskStatus?: string;
}>();

// Filters
const filterProduct = ref('');
const filterSource = ref('');
const filterReliability = ref('');
const filterQuick = ref('');
const keyword = ref('');

const products = computed(() => [...new Set(props.evidence.map((e) => e.productName))]);
const sourceTypes = computed(() => [...new Set(props.evidence.map((e) => e.sourceType))]);
const reliabilities = computed(() => [...new Set(props.evidence.map((e) => e.reliability))]);

// Stats
const referencedIds = computed(() => {
  const ids = new Set<string>();
  if (props.report?.sections) {
    for (const s of props.report.sections) {
      s.evidenceIds?.forEach((id) => ids.add(id));
    }
  }
  return ids;
});
const referencedCount = computed(() => props.evidence.filter(e => referencedIds.value.has(e.evidenceId)).length);
const highCount = computed(() => props.evidence.filter(e => e.reliability === 'HIGH').length);
const mediumCount = computed(() => props.evidence.filter(e => e.reliability === 'MEDIUM').length);

function setQuickFilter(val: string) {
  filterQuick.value = val;
  // Reset specific filters when using quick filter
  if (val === 'HIGH' || val === 'MEDIUM') {
    filterReliability.value = val;
    filterQuick.value = val;
  } else if (val === 'referenced') {
    filterReliability.value = '';
  } else {
    filterReliability.value = '';
  }
}

const filtered = computed(() => {
  return props.evidence.filter((e) => {
    if (filterProduct.value && e.productName !== filterProduct.value) return false;
    if (filterSource.value && e.sourceType !== filterSource.value) return false;
    if (filterReliability.value && e.reliability !== filterReliability.value) return false;
    if (filterQuick.value === 'referenced' && !referencedIds.value.has(e.evidenceId)) return false;
    if (keyword.value) {
      const kw = keyword.value.toLowerCase();
      const match =
        e.evidenceId?.toLowerCase().includes(kw) ||
        e.sourceTitle?.toLowerCase().includes(kw) ||
        e.contentSnippet?.toLowerCase().includes(kw);
      if (!match) return false;
    }
    return true;
  });
});

function isReferenced(eid: string): boolean {
  return referencedIds.value.has(eid);
}

const emptyText = computed(() => {
  if (isRunningStatus(props.taskStatus)) return '证据采集中...';
  return '暂无证据数据';
});

// Drawer
const drawerVisible = ref(false);
const selectedEvidence = ref<Evidence | null>(null);

function openDrawer(row: Evidence) {
  selectedEvidence.value = row;
  drawerVisible.value = true;
}

const relatedSections = computed(() => {
  if (!selectedEvidence.value || !props.report?.sections) return [];
  const eid = selectedEvidence.value.evidenceId;
  return props.report.sections
    .filter(s => s.evidenceIds?.includes(eid))
    .map(s => s.title);
});

// Exposed: allow parent to set keyword for evidence search
function setSearchKeyword(kw: string) {
  keyword.value = kw;
}
defineExpose({ setSearchKeyword });
</script>

<style scoped>
.evidence-tab { min-height: 200px; }

/* Stats Bar */
.stats-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}
.stat-chip {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  color: #6b7280;
  background: #f3f4f6;
  cursor: pointer;
  transition: all 0.15s;
}
.stat-chip:hover { background: #e5e7eb; }
.stat-chip.active { background: #dbeafe; color: #1e40af; }
.stat-chip strong { margin-left: 4px; }

/* Filter Toolbar */
.filter-toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  padding: 12px 14px;
  background: #f8fafc;
  border-radius: 10px;
}

/* Table */
.table-wrap {
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}
.mono-id {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  color: #6b7280;
}
.source-chip {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  background: #f3f4f6;
  color: #4b5563;
  font-size: 12px;
}
.link-btn {
  color: #2563eb;
  text-decoration: none;
  font-size: 12px;
  font-weight: 500;
}
.link-btn:hover { text-decoration: underline; }
.text-muted { color: #d1d5db; }

.reliability-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}
.reliability-tag.r-high { background: #dcfce7; color: #166534; }
.reliability-tag.r-medium { background: #fef3c7; color: #92400e; }
.reliability-tag.r-low { background: #fee2e2; color: #991b1b; }

.ref-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
}
.ref-tag.ref-yes { background: #dcfce7; color: #166534; }
.ref-tag.ref-no { background: #f3f4f6; color: #6b7280; }

.table-footer {
  margin-top: 10px;
  text-align: right;
  font-size: 12px;
  color: #6b7280;
}

/* Drawer */
.drawer-content {
  padding: 0 4px;
}
.drawer-section {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f3f4f6;
}
.drawer-section:last-child { border-bottom: none; }
.drawer-field {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}
.field-label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 600;
  min-width: 80px;
  flex-shrink: 0;
}
.field-value {
  font-size: 14px;
  color: #111827;
  word-break: break-all;
}
.snippet-box {
  margin-top: 8px;
  padding: 12px 14px;
  background: #f8fafc;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: #374151;
  white-space: pre-wrap;
  word-break: break-word;
}
.related-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}
.related-chip {
  padding: 3px 10px;
  border-radius: 10px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
}

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
