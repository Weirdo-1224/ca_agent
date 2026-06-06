<template>
  <el-tag :type="tagType" size="small">{{ label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{ status?: string }>();

const STATUS_MAP: Record<string, { label: string; type: '' | 'success' | 'warning' | 'danger' | 'info' }> = {
  CREATED: { label: '已创建', type: 'info' },
  PLANNING: { label: '规划中', type: 'info' },
  COLLECTING: { label: '采集中', type: 'info' },
  EXTRACTING: { label: '提取中', type: 'info' },
  ANALYZING: { label: '分析中', type: 'info' },
  WRITING: { label: '撰写中', type: 'info' },
  REVIEWING: { label: '质检中', type: 'info' },
  REPAIRING: { label: '修复中', type: 'warning' },
  WAITING_HUMAN_REVIEW: { label: '等待人工审核', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  COMPLETED_WITH_WARNINGS: { label: '已完成（有警告）', type: 'warning' },
  FAILED: { label: '已失败', type: 'danger' },
};

const label = computed(() => STATUS_MAP[props.status || '']?.label || props.status || '未知');
const tagType = computed(() => STATUS_MAP[props.status || '']?.type || 'info');
</script>
