<template>
  <div class="evidence-tab">
    <el-row :gutter="16" class="filters">
      <el-col :xs="24" :sm="6">
        <el-select v-model="filterProduct" placeholder="按产品筛选" clearable style="width: 100%">
          <el-option v-for="p in productOptions" :key="p" :label="p" :value="p" />
        </el-select>
      </el-col>
      <el-col :xs="24" :sm="6">
        <el-select v-model="filterType" placeholder="按来源类型筛选" clearable style="width: 100%">
          <el-option v-for="t in typeOptions" :key="t" :label="t" :value="t" />
        </el-select>
      </el-col>
      <el-col :xs="24" :sm="6">
        <el-select v-model="filterReliability" placeholder="按可靠性筛选" clearable style="width: 100%">
          <el-option label="高" value="HIGH" />
          <el-option label="中" value="MEDIUM" />
          <el-option label="低" value="LOW" />
        </el-select>
      </el-col>
      <el-col :xs="24" :sm="6">
        <el-input v-model="searchKeyword" placeholder="搜索标题或摘要" clearable />
      </el-col>
    </el-row>

    <el-table :data="filteredEvidence" stripe border style="margin-top: 16px">
      <el-table-column prop="evidenceId" label="ID" width="120" />
      <el-table-column prop="productName" label="产品" width="120" />
      <el-table-column prop="sourceType" label="来源类型" width="140">
        <template #default="{ row }">
          <el-tag size="small">{{ row.sourceType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sourceTitle" label="标题" min-width="180" />
      <el-table-column prop="url" label="链接" min-width="200">
        <template #default="{ row }">
          <a :href="row.url" target="_blank" class="link">{{ row.url }}</a>
        </template>
      </el-table-column>
      <el-table-column prop="contentSnippet" label="摘要" min-width="200" show-overflow-tooltip />
      <el-table-column prop="reliability" label="可靠性" width="100">
        <template #default="{ row }">
          <el-tag :type="reliabilityType(row.reliability)" size="small">{{ row.reliability }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="collectedAt" label="采集时间" width="160">
        <template #default="{ row }">
          {{ new Date(row.collectedAt).toLocaleString('zh-CN') }}
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import type { Evidence } from '@/types';

const props = defineProps<{ evidence: Evidence[] }>();

const filterProduct = ref('');
const filterType = ref('');
const filterReliability = ref('');
const searchKeyword = ref('');

const productOptions = computed(() => [...new Set(props.evidence.map((e) => e.productName))]);
const typeOptions = computed(() => [...new Set(props.evidence.map((e) => e.sourceType))]);

const filteredEvidence = computed(() => {
  return props.evidence.filter((e) => {
    if (filterProduct.value && e.productName !== filterProduct.value) return false;
    if (filterType.value && e.sourceType !== filterType.value) return false;
    if (filterReliability.value && e.reliability !== filterReliability.value) return false;
    if (searchKeyword.value) {
      const kw = searchKeyword.value.toLowerCase();
      return (
        e.sourceTitle?.toLowerCase().includes(kw) ||
        e.contentSnippet?.toLowerCase().includes(kw)
      );
    }
    return true;
  });
});

function reliabilityType(r: string) {
  if (r === 'HIGH') return 'success';
  if (r === 'MEDIUM') return 'warning';
  return 'danger';
}
</script>

<style scoped>
.evidence-tab { padding: 8px 0; }
.filters { margin-bottom: 8px; }
.link { color: #409eff; word-break: break-all; }
</style>
