<template>
  <div class="create-page">
    <div class="create-grid">
      <!-- 左侧栏 -->
      <aside class="left-col">
        <!-- Hero 卡片 -->
        <div class="panel hero-panel">
          <h1 class="hero-title">CA Agent</h1>
          <p class="hero-subtitle">多 Agent 竞品分析工作台</p>
          <p class="hero-desc">
            通过 Planner、Collector、Extractor、Analyzer、Writer、Reviewer
            六个专职 Agent，完成公开信息采集、结构化抽取、竞品分析、报告生成与质检回退。
          </p>
          <div class="hero-tags">
            <span class="tag">多 Agent 协作</span>
            <span class="tag">证据链追溯</span>
            <span class="tag">质检回退闭环</span>
          </div>
        </div>

        <!-- Agent 工作流卡片 -->
        <div class="panel workflow-panel">
          <h3 class="panel-title">Agent 工作流</h3>
          <ul class="agent-list">
            <li v-for="(agent, idx) in agents" :key="idx" class="agent-item">
              <span class="agent-step">{{ idx + 1 }}</span>
              <div class="agent-info">
                <span class="agent-name">{{ agent.name }}</span>
                <span class="agent-desc">{{ agent.desc }}</span>
              </div>
            </li>
          </ul>
        </div>

        <!-- 快捷入口卡片 -->
        <div class="panel recent-panel">
          <h3 class="panel-title">快捷入口</h3>
          <div class="manual-entry">
            <el-input
              v-model="manualTaskId"
              placeholder="输入 Task ID 直接打开"
              clearable
              @keyup.enter="goToManualTask"
            >
              <template #append>
                <el-button @click="goToManualTask" :disabled="!manualTaskId.trim()">打开</el-button>
              </template>
            </el-input>
          </div>
          <div v-if="recentTasks.length" class="recent-list">
            <h4 class="recent-header">最近任务</h4>
            <div
              v-for="task in recentTasks.slice(0, 5)"
              :key="task.taskId"
              class="recent-item"
              @click="goToTask(task.taskId)"
            >
              <span class="recent-name">{{ task.taskName }}</span>
              <span class="recent-id">{{ task.taskId.slice(-8) }}</span>
            </div>
          </div>
          <div v-else class="empty-state">
            <span>暂无最近任务</span>
          </div>
        </div>
      </aside>

      <!-- 右侧表单 -->
      <section class="right-col">
        <div class="panel form-panel">
          <div class="form-header">
            <h2 class="form-title">创建竞品分析任务</h2>
            <p class="form-subtitle">
              配置一次多 Agent 竞品分析流程，创建后将进入任务详情页查看执行轨迹和报告。
            </p>
          </div>

          <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @submit.prevent>
            <!-- 基础信息 -->
            <div class="form-section">
              <h4 class="section-label">基础信息</h4>
              <el-form-item label="任务名称" prop="taskName">
                <el-input v-model="form.taskName" placeholder="如：AI 编程工具竞品分析" />
              </el-form-item>
              <el-form-item label="分析领域" prop="domain">
                <el-input v-model="form.domain" placeholder="如：AI_CODING_TOOLS、电动汽车、云存储" />
              </el-form-item>
              <el-form-item label="目标产品" prop="targetProducts">
                <el-select-v2
                  v-model="form.targetProducts"
                  :options="productOptions"
                  placeholder="选择或输入要分析的产品（至少 2 个）"
                  multiple
                  filterable
                  allow-create
                  clearable
                  style="width: 100%"
                />
              </el-form-item>
            </div>

            <!-- 分析配置 -->
            <div class="form-section">
              <h4 class="section-label">分析配置</h4>
              <el-form-item label="分析目标" prop="analysisGoal">
                <el-input
                  v-model="form.analysisGoal"
                  type="textarea"
                  :rows="3"
                  placeholder="如：生成面向产品团队的竞品分析报告，重点关注功能差异和定价策略"
                />
              </el-form-item>
              <div class="form-row">
                <el-form-item label="输出格式" class="form-row-item">
                  <el-select v-model="form.outputFormat" style="width: 100%">
                    <el-option label="Markdown" value="markdown" />
                  </el-select>
                </el-form-item>
                <el-form-item label="输出语言" class="form-row-item">
                  <el-select v-model="form.language" style="width: 100%">
                    <el-option label="中文" value="zh-CN" />
                    <el-option label="English" value="en" />
                  </el-select>
                </el-form-item>
              </div>
            </div>

            <!-- 运行设置 -->
            <div class="form-section">
              <h4 class="section-label">运行设置</h4>
              <el-form-item label="最大修复轮次">
                <div class="iterations-row">
                  <el-input-number v-model="form.maxIterations" :min="0" :max="3" />
                  <span class="hint">建议 1-2 轮，轮次越高耗时越长</span>
                </div>
              </el-form-item>
            </div>

            <!-- 错误提示 -->
            <el-alert v-if="errorMsg" :title="errorMsg" type="error" :closable="false" show-icon style="margin-bottom: 20px" />

            <!-- 按钮 -->
            <div class="form-actions">
              <el-button size="large" @click="fillDemo">一键填充 Demo</el-button>
              <el-button type="primary" size="large" :loading="loading" @click="onSubmit" class="submit-btn">
                {{ loading ? '分析中...' : '创建任务并启动分析' }}
              </el-button>
            </div>

            <!-- loading 提示 -->
            <p v-if="loading" class="loading-hint">
              CA Agent 正在执行多 Agent 分析流程，真实模型模式下可能需要几十秒，请勿重复提交。
            </p>
          </el-form>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { createTask } from '@/api/tasks';
import type { TaskCreateRequest } from '@/types';
import { getRecentTasks, addRecentTask, type RecentTask } from '@/utils/recent-tasks';

const router = useRouter();
const formRef = ref<FormInstance>();
const loading = ref(false);
const errorMsg = ref('');
const manualTaskId = ref('');

const agents = [
  { name: 'PlannerAgent', desc: '生成采集任务计划' },
  { name: 'CollectorAgent', desc: '搜索公开资料并生成证据池' },
  { name: 'ExtractorAgent', desc: '结构化抽取产品画像和 Claim' },
  { name: 'AnalyzerAgent', desc: '横向对比分析与 SWOT' },
  { name: 'WriterAgent', desc: '生成 Markdown 竞品分析报告' },
  { name: 'ReviewerAgent', desc: '质检评分并判断是否回退' },
];

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
  { value: 'Codeium', label: 'Codeium' },
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
  ElMessage.success('已填充 Demo 参数');
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
  if (!id) {
    ElMessage.warning('请输入 Task ID');
    return;
  }
  router.push(`/tasks/${id}`);
}
</script>

<style scoped>
.create-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 40px 24px;
}

.create-grid {
  display: grid;
  grid-template-columns: 40% 60%;
  gap: 28px;
  align-items: start;
}

/* -- 通用面板 -- */
.panel {
  background: #ffffff;
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 4px 12px rgba(0, 0, 0, 0.03);
  margin-bottom: 20px;
}
.panel-title {
  margin: 0 0 16px;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

/* -- 左侧 Hero -- */
.hero-panel {
  background: linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%);
  border: 1px solid #c7d2fe;
}
.hero-title {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: #1e1b4b;
}
.hero-subtitle {
  margin: 4px 0 12px;
  font-size: 14px;
  font-weight: 500;
  color: #4338ca;
}
.hero-desc {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.7;
  color: #374151;
}
.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tag {
  display: inline-block;
  padding: 4px 10px;
  background: rgba(79, 70, 229, 0.1);
  color: #4f46e5;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

/* -- Agent 工作流 -- */
.agent-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.agent-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f3f4f6;
}
.agent-item:last-child {
  border-bottom: none;
}
.agent-step {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #eef2ff;
  color: #4f46e5;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}
.agent-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.agent-name {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}
.agent-desc {
  font-size: 12px;
  color: #6b7280;
}

/* -- 快捷入口 -- */
.manual-entry {
  margin-bottom: 16px;
}
.recent-header {
  margin: 0 0 8px;
  font-size: 13px;
  color: #6b7280;
  font-weight: 500;
}
.recent-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.recent-item:hover {
  background: #eef2ff;
}
.recent-name {
  font-size: 13px;
  color: #374151;
}
.recent-id {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 11px;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
}
.empty-state {
  text-align: center;
  padding: 20px 0;
  color: #9ca3af;
  font-size: 13px;
}

/* -- 右侧表单 -- */
.form-panel {
  margin-bottom: 0;
}
.form-header {
  margin-bottom: 24px;
}
.form-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}
.form-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
}
.form-section {
  margin-bottom: 8px;
}
.section-label {
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 600;
  color: #4f46e5;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.form-row-item {
  margin-bottom: 18px;
}
.iterations-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.hint {
  color: #9ca3af;
  font-size: 12px;
}
.form-actions {
  display: flex;
  gap: 12px;
  padding-top: 8px;
}
.submit-btn {
  flex: 1;
}
.loading-hint {
  margin: 12px 0 0;
  font-size: 12px;
  color: #f59e0b;
  line-height: 1.5;
}

/* -- 响应式 -- */
@media (max-width: 900px) {
  .create-grid {
    grid-template-columns: 1fr;
  }
  .left-col {
    order: 2;
  }
  .right-col {
    order: 1;
  }
}
</style>
