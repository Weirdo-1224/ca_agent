<template>
  <div class="create-page">
    <el-card class="create-card" shadow="never">
      <template #header>
        <div class="card-header">
          <h2>创建竞品分析任务</h2>
          <span class="card-subtitle">创建一次多 Agent 竞品分析流程</span>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px" @submit.prevent>
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="form.taskName" placeholder="AI 编程工具竞品分析" />
        </el-form-item>

        <el-form-item label="分析领域" prop="domain">
          <el-input v-model="form.domain" placeholder="AI_CODING_TOOLS" />
        </el-form-item>

        <el-form-item label="目标产品" prop="targetProducts">
          <el-select-v2
            v-model="form.targetProducts"
            :options="productOptions"
            placeholder="选择要分析的产品"
            multiple
            filterable
            allow-create
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

        <el-form-item label="输出格式">
          <el-select v-model="form.outputFormat" style="width: 100%">
            <el-option label="Markdown" value="markdown" />
          </el-select>
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
          <el-button type="primary" size="large" :loading="loading" @click="onSubmit" style="width: 180px">
            {{ loading ? '创建中...' : '创建任务' }}
          </el-button>
          <el-button size="large" @click="fillDemo">一键填充 Demo</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 最近任务 & 手动打开 -->
    <el-card class="recent-card" shadow="never">
      <template #header>
        <span>快捷入口</span>
      </template>
      <div class="quick-entry">
        <div class="manual-entry">
          <el-input v-model="manualTaskId" placeholder="输入 Task ID 直接打开" clearable>
            <template #append>
              <el-button @click="goToManualTask" :disabled="!manualTaskId.trim()">打开</el-button>
            </template>
          </el-input>
        </div>
        <div v-if="recentTasks.length" class="recent-list">
          <h4>最近任务</h4>
          <div
            v-for="task in recentTasks"
            :key="task.taskId"
            class="recent-item"
            @click="goToTask(task.taskId)"
          >
            <span class="recent-name">{{ task.taskName }}</span>
            <span class="recent-id">{{ task.taskId.slice(-8) }}</span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import type { FormInstance, FormRules } from 'element-plus';
import { createTask } from '@/api/tasks';
import type { TaskCreateRequest } from '@/types';
import { getRecentTasks, addRecentTask, type RecentTask } from '@/utils/recent-tasks';

const router = useRouter();
const formRef = ref<FormInstance>();
const loading = ref(false);
const errorMsg = ref('');
const manualTaskId = ref('');

const form = reactive<TaskCreateRequest>({
  taskName: '',
  domain: '',
  targetProducts: [],
  analysisGoal: '',
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
  domain: [{ required: true, message: '请输入分析领域', trigger: 'blur' }],
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

const recentTasks = ref<RecentTask[]>([]);

onMounted(() => {
  recentTasks.value = getRecentTasks();
});

function fillDemo() {
  form.taskName = 'AI 编程工具竞品分析';
  form.domain = 'AI_CODING_TOOLS';
  form.targetProducts = ['Cursor', 'GitHub Copilot'];
  form.analysisGoal = '生成面向产品团队的 AI 编程工具竞品分析报告';
  form.outputFormat = 'markdown';
  form.language = 'zh-CN';
  form.maxIterations = 1;
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  loading.value = true;
  errorMsg.value = '';

  try {
    const res = await createTask({ ...form });
    recentTasks.value = addRecentTask(res.taskId, res.taskName);
    router.push(`/tasks/${res.taskId}`);
  } catch (e: unknown) {
    errorMsg.value = e instanceof Error ? e.message : '创建失败';
  } finally {
    loading.value = false;
  }
}

function goToTask(taskId: string) {
  router.push(`/tasks/${taskId}`);
}

function goToManualTask() {
  const id = manualTaskId.value.trim();
  if (id) router.push(`/tasks/${id}`);
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
  margin-bottom: 20px;
}
.card-header h2 {
  margin: 0;
  font-size: 20px;
}
.card-subtitle {
  font-size: 13px;
  color: #909399;
}
.hint {
  margin-left: 12px;
  color: #999;
  font-size: 13px;
}
.error-item {
  margin-bottom: 18px;
}
.recent-card {
  border-radius: 8px;
}
.quick-entry {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.manual-entry {
  max-width: 400px;
}
.recent-list h4 {
  margin: 0 0 8px;
  font-size: 14px;
  color: #606266;
}
.recent-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
}
.recent-item:hover {
  background: #f5f7fa;
}
.recent-name {
  font-size: 13px;
  color: #303133;
}
.recent-id {
  font-family: monospace;
  font-size: 12px;
  color: #909399;
}
</style>
