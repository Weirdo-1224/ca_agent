<template>
  <div class="review-tab">
    <template v-if="review">
      <!-- 质检总览 -->
      <el-card shadow="never" class="review-card">
        <template #header><span class="section-title">质检总览</span></template>
        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item label="质检结果">
            <el-tag :type="review.passed ? 'success' : 'danger'" effect="dark" size="small">
              {{ review.passed ? '通过' : '未通过' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="评分">
            <span class="score">{{ review.score ?? '—' }}</span>
          </el-descriptions-item>
          <el-descriptions-item :span="2" label="摘要">{{ review.summary || '—' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 下一步动作 -->
      <el-card v-if="review.nextAction" shadow="never" class="review-card next-action-card" :class="{ 'action-warning': !review.passed }">
        <template #header><span class="section-title">下一步动作</span></template>
        <div class="action-content">
          <div class="action-main">
            <el-icon :size="20"><Warning /></el-icon>
            <div class="action-text">
              <template v-if="taskStatus === 'WAITING_HUMAN_REVIEW'">
                已达到最大自动修复轮次，等待人工审核。
              </template>
              <template v-else>
                Reviewer 判定当前报告未通过，系统将根据修复建议回退到 <strong>{{ review.nextAction.targetAgent }}</strong>。
              </template>
            </div>
          </div>
          <el-descriptions :column="1" size="small" border style="margin-top:12px">
            <el-descriptions-item label="动作">{{ review.nextAction.action }}</el-descriptions-item>
            <el-descriptions-item label="目标 Agent">{{ review.nextAction.targetAgent }}</el-descriptions-item>
            <el-descriptions-item label="原因">{{ review.nextAction.reason }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </el-card>

      <!-- issues 表格 -->
      <el-card shadow="never" class="review-card">
        <template #header><span class="section-title">问题列表</span></template>
        <template v-if="review.issues && review.issues.length > 0">
          <el-table :data="review.issues" stripe size="small">
            <el-table-column prop="severity" label="严重程度" width="90">
              <template #default="{ row }">
                <el-tag :type="severityType(row.severity)" size="small">{{ row.severity }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="120" />
            <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
            <el-table-column prop="targetAgent" label="目标 Agent" width="130" />
            <el-table-column prop="targetProduct" label="目标产品" width="100" />
            <el-table-column prop="targetDimension" label="目标维度" width="100" />
            <el-table-column prop="repairInstruction" label="修复建议" min-width="180" show-overflow-tooltip />
          </el-table>
        </template>
        <el-empty v-else :image-size="50" description="质检通过，未发现阻塞问题。" />
      </el-card>
    </template>

    <!-- 空状态 -->
    <el-empty v-else :image-size="80" :description="emptyText" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Warning } from '@element-plus/icons-vue';
import type { ReviewResult } from '@/types';
import { isRunningStatus } from '@/utils/status';

const props = defineProps<{
  review: ReviewResult | null;
  taskStatus?: string;
}>();

function severityType(severity: string) {
  switch (severity?.toUpperCase()) {
    case 'HIGH':
    case 'CRITICAL':
      return 'danger';
    case 'MEDIUM':
      return 'warning';
    default:
      return 'info';
  }
}

const emptyText = computed(() => {
  if (isRunningStatus(props.taskStatus)) return '质检尚未完成';
  return '暂无质检结果';
});
</script>

<style scoped>
.review-tab {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.review-card {
  border-radius: 6px;
}
.section-title {
  font-weight: 600;
  font-size: 14px;
}
.score {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}
.next-action-card.action-warning {
  border-color: #e6a23c;
}
.action-content {
  padding: 4px 0;
}
.action-main {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  color: #e6a23c;
}
.action-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}
</style>
