<template>
  <div class="create-page">
    <el-card class="create-card" shadow="never">
      <template #header>
        <div class="card-header">
          <h2>创建竞品分析任务</h2>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px" @submit.prevent>
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="form.taskName" placeholder="AI 编程工具竞品分析" />
        </el-form-item>

        <el-form-item label="分析领域" prop="domain">
          <el-select v-model="form.domain" style="width: 100%">
            <el-option label="AI 编程工具" value="AI_CODING_TOOLS" />
          </el-select>
        </el-form-item>

        <el-form-item label="目标产品" prop="targetProducts">
          <el-select-v2
            v-model="form.targetProducts"
            :options="productOptions"
            placeholder="选择要分析的产品"
            multiple
            clearable
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="分析目标" prop="analysisGoal">
          <el-input
            v-model="form.analysisGoal"
            type="textarea"
            :rows="3"
            placeholder="生成面向产品团队的竞品分析报告"
          />
        </el-form-item>

        <el-form-item label="输出语言">
          <el-select v-model="form.language" style="width: 100%">
            <el-option label="中文" value="zh-CN" />
            <el-option label="English" value="en" />
          </el-select>
        </el-form-item>

        <el-form-item label="最大修复轮次">
          <el-input-number v-model="form.maxIterations" :min="0" :max="3" />
          <span class="hint">建议 1~2 轮</span>
        </el-form-item>

        <el-form-item v-if="errorMsg" class="error-item">
          <el-alert :title="errorMsg" type="error" :closable="false" show-icon />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="onSubmit"
            style="width: 200px"
          >
            {{ loading ? '创建中...' : '创建任务' }}
          </el-button>
          <el-button v-if="recentTasks.length" size="large" @click="showRecent = true">
            最近任务
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog v-model="showRecent" title="最近任务" width="400px">
      <el-table :data="recentTasks" @row-click="goToTask" style="cursor: pointer">
        <el-table-column prop="taskName" label="任务名" />
        <el-table-column prop="taskId" label="ID" width="120">
          <template #default="{ row }">
            <span class="task-id">{{ row.taskId.slice(-8) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import type { FormInstance, FormRules } from 'element-plus';
import { createTask } from '@/api/tasks';
import type { TaskCreateRequest } from '@/types';

const router = useRouter();
const formRef = ref<FormInstance>();
const loading = ref(false);
const errorMsg = ref('');
const showRecent = ref(false);

const form = reactive<TaskCreateRequest>({
  taskName: 'AI 编程工具竞品分析',
  domain: 'AI_CODING_TOOLS',
  targetProducts: ['Cursor', 'GitHub Copilot'],
  analysisGoal: '生成面向产品团队的 AI 编程工具竞品分析报告',
  outputFormat: 'markdown',
  language: 'zh-CN',
  maxIterations: 1,
});

const productOptions = [
  { value: 'Cursor', label: 'Cursor' },
  { value: 'GitHub Copilot', label: 'GitHub Copilot' },
  { value: 'Windsurf', label: 'Windsurf' },
  { value: '通义灵码', label: '通义灵码' },
];

const rules: FormRules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  domain: [{ required: true, message: '请选择分析领域', trigger: 'change' }],
  targetProducts: [
    { required: true, message: '请至少选择 2 个目标产品', trigger: 'change' },
    {
      validator: (_rule: unknown, value: string[], callback: (error?: Error) => void) => {
        if (!value || value.length < 2) callback(new Error('至少选择 2 个产品'));
        else callback();
      },
      trigger: 'change',
    },
  ],
  analysisGoal: [{ required: true, message: '请输入分析目标', trigger: 'blur' }],
};

interface RecentTask {
  taskId: string;
  taskName: string;
}

const recentTasks = ref<RecentTask[]>([]);

onMounted(() => {
  try {
    const raw = localStorage.getItem('ca-agent.recent-task-ids');
    if (raw) recentTasks.value = JSON.parse(raw);
  } catch { /* ignore */ }
});

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  loading.value = true;
  errorMsg.value = '';

  try {
    const res = await createTask({ ...form });
    addRecentTask(res.taskId, res.taskName);
    router.push(`/tasks/${res.taskId}`);
  } catch (e: unknown) {
    errorMsg.value = e instanceof Error ? e.message : '创建失败';
  } finally {
    loading.value = false;
  }
}

function addRecentTask(taskId: string, taskName: string) {
  const list = recentTasks.value.filter((t) => t.taskId !== taskId);
  list.unshift({ taskId, taskName });
  recentTasks.value = list.slice(0, 10);
  localStorage.setItem('ca-agent.recent-task-ids', JSON.stringify(recentTasks.value));
}

function goToTask(row: RecentTask) {
  router.push(`/tasks/${row.taskId}`);
}
</script>

<style scoped>
.create-page {
  max-width: 720px;
  margin: 40px auto;
  padding: 0 16px;
}
.create-card {
  border-radius: 8px;
}
.card-header h2 {
  margin: 0;
  font-size: 20px;
}
.hint {
  margin-left: 12px;
  color: #999;
  font-size: 13px;
}
.error-item {
  margin-bottom: 18px;
}
.task-id {
  font-family: monospace;
  color: #666;
}
</style>
