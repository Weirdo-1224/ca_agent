<template>
  <div class="evidence-tab">
    <template v-if="evidence.length > 0">
      <!-- 筛选区 -->
      <div class="filter-bar">
        <el-select v-model="filterProduct" placeholder="按产品筛选" clearable size="small" style="width:140px">
          <el-option v-for="p in products" :key="p" :label="p" :value="p" />
        </el-select>
        <el-select v-model="filterSource" placeholder="按来源类型" clearable size="small" style="width:140px">
          <el-option v-for="s in sourceTypes" :key="s" :label="s" :value="s" />
        </el-select>
        <el-select v-model="filterReliability" placeholder="按可靠性" clearable size="small" style="width:120px">
          <el-option v-for="r in reliabilities" :key="r" :label="r" :value="r" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索 ID/标题/摘要" clearable size="small" style="width:200px" />
      </div>

      <!-- 表格 -->
      <el-table :data="filtered" stripe style="width:100%" size="small" max-height="500">
        <el-table-column prop="evidenceId" label="ID" width="100">
          <template #default="{ row }">
            <span class="mono-text">{{ row.evidenceId?.slice(-6) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="产品" width="110" />
        <el-table-column prop="sourceType" label="来源类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.sourceType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sourceTitle" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="URL" width="60">
          <template #default="{ row }">
            <a v-if="row.url" :href="row.url" target="_blank" class="url-link">链接</a>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="contentSnippet" label="摘要" min-width="200" show-overflow-tooltip />
        <el-table-column prop="reliability" label="可靠性" width="80">
          <template #default="{ row }">
            <el-tag :type="reliabilityType(row.reliability)" size="small">{{ row.reliability }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="引用" width="70">
          <template #default="{ row }">
            <el-tag :type="isReferenced(row.evidenceId) ? 'success' : 'info'" size="small">
              {{ isReferenced(row.evidenceId) ? '已引用' : '未引用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="collectedAt" label="采集时间" width="100">
          <template #default="{ row }">
            {{ formatTime(row.collectedAt) }}
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        共 {{ filtered.length }} / {{ evidence.length }} 条证据
      </div>
    </template>

    <el-empty v-else :image-size="80" :description="emptyText" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import type { Evidence, ReportResponse } from '@/types';
import { isRunningStatus } from '@/utils/status';
import { formatTime } from '@/utils/time';

const props = defineProps<{
  evidence: Evidence[];
  report: ReportResponse | null;
  taskStatus?: string;
}>();

// Filters
const filterProduct = ref('');
const filterSource = ref('');
const filterReliability = ref('');
const keyword = ref('');

const products = computed(() => [...new Set(props.evidence.map((e) => e.productName))]);
const sourceTypes = computed(() => [...new Set(props.evidence.map((e) => e.sourceType))]);
const reliabilities = computed(() => [...new Set(props.evidence.map((e) => e.reliability))]);

const filtered = computed(() => {
  return props.evidence.filter((e) => {
    if (filterProduct.value && e.productName !== filterProduct.value) return false;
    if (filterSource.value && e.sourceType !== filterSource.value) return false;
    if (filterReliability.value && e.reliability !== filterReliability.value) return false;
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

// Referenced by report
const referencedIds = computed(() => {
  const ids = new Set<string>();
  if (props.report?.sections) {
    for (const s of props.report.sections) {
      s.evidenceIds?.forEach((id) => ids.add(id));
    }
  }
  return ids;
});

function isReferenced(eid: string): boolean {
  return referencedIds.value.has(eid);
}

function reliabilityType(r: string) {
  switch (r) {
    case 'HIGH': return 'success';
    case 'MEDIUM': return 'warning';
    case 'LOW': return 'danger';
    default: return 'info';
  }
}

const emptyText = computed(() => {
  if (isRunningStatus(props.taskStatus)) return '证据采集中...';
  return '暂无证据数据';
});

// Exposed: allow parent to set keyword for evidence search
function setSearchKeyword(kw: string) {
  keyword.value = kw;
}
defineExpose({ setSearchKeyword });
</script>

<style scoped>
.evidence-tab {
  min-height: 200px;
}
.filter-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.mono-text {
  font-family: monospace;
  font-size: 12px;
}
.url-link {
  color: #409eff;
  text-decoration: none;
  font-size: 12px;
}
.table-footer {
  margin-top: 8px;
  text-align: right;
  font-size: 12px;
  color: #909399;
}
</style>
