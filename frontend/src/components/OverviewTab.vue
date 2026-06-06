<template>
  <div v-if="detail" class="overview">
    <el-row :gutter="20">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>任务信息</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="任务名称">{{ detail.taskName }}</el-descriptions-item>
            <el-descriptions-item label="领域">{{ detail.domain }}</el-descriptions-item>
            <el-descriptions-item label="目标产品">
              <el-tag v-for="p in detail.targetProducts" :key="p" size="small" class="product-tag">{{ p }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="分析目标">{{ detail.analysisGoal }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>执行状态</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="状态">
              <StatusTag :status="detail.status" />
            </el-descriptions-item>
            <el-descriptions-item label="迭代次数">{{ detail.iterationCount }} / {{ detail.maxIterations }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatTime(detail.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatTime(detail.updatedAt) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mt-20">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>质检结果</template>
          <div v-if="review">
            <p><strong>通过状态：</strong><el-tag :type="review.passed ? 'success' : 'danger'">{{ review.passed ? '通过' : '未通过' }}</el-tag></p>
            <p v-if="review.score !== null"><strong>得分：</strong>{{ review.score }}</p>
            <p v-if="review.summary"><strong>摘要：</strong>{{ review.summary }}</p>
          </div>
          <el-empty v-else description="质检尚未完成" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <template #header>Agent 执行统计</template>
          <div v-if="agentRuns.length">
            <p><strong>总执行数：</strong>{{ agentRuns.length }}</p>
            <p><strong>成功：</strong>{{ successCount }}</p>
            <p><strong>失败：</strong>{{ failCount }}</p>
            <p><strong>总耗时：</strong>{{ totalDuration }} ms</p>
          </div>
          <el-empty v-else description="暂无执行记录" />
        </el-card>
      </el-col>
    </el-row>
  </div>
  <el-empty v-else description="任务不存在或加载失败" />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { TaskDetailResponse, ReviewResult, AgentRunResponse } from '@/types';
import StatusTag from './StatusTag.vue';

const props = defineProps<{
  detail: TaskDetailResponse | null;
  review: ReviewResult | null;
  agentRuns: AgentRunResponse[];
}>();

const successCount = computed(() => props.agentRuns.filter((a) => a.status === 'SUCCESS').length);
const failCount = computed(() => props.agentRuns.filter((a) => a.status === 'FAILED').length);
const totalDuration = computed(() => props.agentRuns.reduce((sum, a) => sum + (a.durationMs || 0), 0));

function formatTime(t?: string) {
  if (!t) return '-';
  return new Date(t).toLocaleString('zh-CN');
}
</script>

<style scoped>
.overview { padding: 8px 0; }
.product-tag { margin-right: 6px; }
.mt-20 { margin-top: 20px; }
</style>
