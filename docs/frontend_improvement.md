下面这份可以直接交给 AI 编程工具，让它按这个方案优化你的前端。重点是：**不要重做成复杂后台，而是在现有 Vue 3 + Vite + Element Plus 项目基础上，把页面优化成“CA Agent 多 Agent 工作流演示台”。**

---

# CA Agent 前端优化改造方案

## 一、项目背景

当前项目是一个 **AI 驱动的多 Agent 竞品分析系统 CA Agent**，后端已有接口支持：

```text
POST /api/tasks
GET /api/tasks/{taskId}
GET /api/tasks/{taskId}/report
GET /api/tasks/{taskId}/evidence
GET /api/tasks/{taskId}/review
GET /api/tasks/{taskId}/agent-runs
```

前端当前技术栈为：

```text
Vue 3 + TypeScript + Vite + Element Plus
```

第一版前端不做完整后台管理系统，不做登录，不做任务列表分页，不做导出，不做 SSE / WebSocket。
优化目标是在现有接口能力范围内，把前端做成一个**可演示、可观测、可追溯的多 Agent 竞品分析工作台**。

---

# 二、整体优化目标

前端需要围绕一次竞品分析任务形成完整闭环：

```text
创建任务
  -> 进入任务详情页
  -> 查看任务状态
  -> 查看 Agent 工作流进度
  -> 查看 Agent 执行轨迹
  -> 查看最终报告
  -> 查看证据来源
  -> 查看质检结果和回退建议
```

前端核心卖点不是普通表单和结果展示，而是要突出：

```text
1. 多 Agent 协作流程可视化
2. AgentRun 执行过程可观测
3. Report -> Evidence -> URL 证据链可追溯
4. Reviewer 质检与自动回退逻辑可展示
5. 当前同步后端兼容未来异步任务执行
```

---

# 三、页面范围

第一版只保留两个主要页面：

| 页面    | 路由               | 说明                             |
| ----- | ---------------- | ------------------------------ |
| 任务创建页 | `/`              | 创建竞品分析任务，同时展示最近任务入口            |
| 任务详情页 | `/tasks/:taskId` | 展示任务状态、Agent 工作流、报告、证据、质检和执行轨迹 |

不要新增完整任务列表页、用户登录页、系统设置页、报告中心页。

---

# 四、推荐目录结构

请在现有 `src` 基础上整理为以下结构。已有文件可以保留并重构，不要无意义大规模删除。

```text
src
├── api
│   ├── request.ts
│   └── task-api.ts
│
├── types
│   ├── common.ts
│   ├── task.ts
│   ├── report.ts
│   ├── evidence.ts
│   ├── review.ts
│   └── agent-run.ts
│
├── utils
│   ├── status.ts
│   ├── time.ts
│   └── recent-tasks.ts
│
├── components
│   ├── TaskStatusHeader.vue
│   ├── AgentWorkflowPanel.vue
│   ├── ReportSectionViewer.vue
│   ├── EvidenceTable.vue
│   ├── ReviewResultPanel.vue
│   └── AgentRunTimeline.vue
│
├── views
│   ├── TaskCreate.vue
│   └── TaskDetail.vue
│
├── router
│   └── index.ts
│
├── App.vue
└── main.ts
```

---

# 五、全局布局要求

不做复杂左侧后台导航。第一版使用简洁工作台布局：

```text
┌──────────────────────────────────────────────┐
│ 顶部栏：CA Agent / 当前任务 / 返回创建页        │
├──────────────────────────────────────────────┤
│ 主内容区                                      │
└──────────────────────────────────────────────┘
```

页面风格要求：

```text
1. 使用 Element Plus 组件。
2. 主色克制，偏企业级工具风格。
3. 状态色只用于任务状态、Agent 状态、质检状态。
4. 不要使用大面积动画和营销式首页。
5. 以桌面演示体验为优先，移动端可自然降级。
```

状态色建议：

| 状态                      | UI 类型   |
| ----------------------- | ------- |
| COMPLETED               | success |
| COMPLETED_WITH_WARNINGS | warning |
| WAITING_HUMAN_REVIEW    | warning |
| FAILED                  | danger  |
| 运行中状态                   | primary |
| CREATED / PENDING       | info    |

---

# 六、任务创建页优化要求

## 6.1 页面目标

任务创建页不是营销首页，而是一个**任务配置入口**。

页面结构：

```text
┌──────────────────────────────────────────────┐
│ CA Agent 竞品分析任务创建                     │
│ 说明：创建一次多 Agent 竞品分析流程             │
├──────────────────────────────────────────────┤
│ 表单区域                                      │
│ - taskName                                   │
│ - domain                                     │
│ - targetProducts                             │
│ - analysisGoal                               │
│ - outputFormat                               │
│ - language                                   │
│ - maxIterations                              │
├──────────────────────────────────────────────┤
│ [一键填充 Demo] [创建任务]                     │
├──────────────────────────────────────────────┤
│ 最近任务 / 手动打开 taskId                     │
└──────────────────────────────────────────────┘
```

---

## 6.2 表单字段

对应后端 `TaskCreateRequest`：

| 字段             | 控件                   | 默认值                     | 校验        |
| -------------- | -------------------- | ----------------------- | --------- |
| taskName       | el-input             | AI 编程工具竞品分析             | 非空        |
| domain         | el-input 或 el-select | AI_CODING_TOOLS         | 非空        |
| targetProducts | 标签输入或多行输入            | Cursor, GitHub Copilot  | 至少 2 个    |
| analysisGoal   | el-input textarea    | 生成面向产品团队的 AI 编程工具竞品分析报告 | 非空        |
| outputFormat   | el-select            | markdown                | 非空        |
| language       | el-select            | zh-CN                   | 非空        |
| maxIterations  | el-input-number      | 1                       | 0 到 3 的整数 |

---

## 6.3 Demo 默认值

增加“一键填充 Demo”按钮，点击后填入：

```json
{
  "taskName": "AI 编程工具竞品分析",
  "domain": "AI_CODING_TOOLS",
  "targetProducts": ["Cursor", "GitHub Copilot"],
  "analysisGoal": "生成面向产品团队的 AI 编程工具竞品分析报告",
  "outputFormat": "markdown",
  "language": "zh-CN",
  "maxIterations": 1
}
```

---

## 6.4 创建任务交互

创建任务时：

```text
1. 点击创建任务后调用 POST /api/tasks。
2. 按钮进入 loading 状态。
3. 表单禁止重复提交。
4. 顶部显示提示：任务已提交，CA Agent 正在执行多 Agent 分析流程，真实模型模式下可能需要几十秒。
5. 成功后从返回结果中获取 taskId。
6. 保存 taskId 到 localStorage。
7. 跳转到 /tasks/:taskId。
8. 失败时展示后端 message。
```

注意：不要直接用创建接口返回数据渲染详情页。
创建成功后必须跳转详情页，由详情页重新请求任务数据。

---

## 6.5 最近任务与 taskId 直达

由于当前后端没有任务列表接口，前端使用 localStorage 保存最近任务。

```text
localStorage key: ca-agent.recent-task-ids
value: string[]
```

任务创建成功后，把 taskId 保存到最近任务中。

首页增加两个小功能：

```text
1. 最近任务入口：展示最近 5 个 taskId，可点击进入详情页。
2. 手动打开任务：输入 taskId，点击后跳转 /tasks/:taskId。
```

不要把 localStorage 当权威数据源，只作为前端便利入口。

---

# 七、任务详情页整体优化

任务详情页是项目核心页面，必须做成：

```text
顶部任务状态区 + 左侧 Agent 工作流面板 + 右侧 Tabs 内容区
```

推荐布局：

```text
┌──────────────────────────────────────────────────────────────┐
│ 顶部状态区：任务名称 / 状态 / 产品 / 轮次 / 更新时间 / 刷新按钮 │
├──────────────────────┬───────────────────────────────────────┤
│ 左侧 Agent 工作流     │ 右侧 Tabs                              │
│ PlannerAgent          │ 概览                                   │
│ CollectorAgent        │ 报告                                   │
│ ExtractorAgent        │ 证据                                   │
│ AnalyzerAgent         │ 质检                                   │
│ WriterAgent           │ Agent 轨迹                             │
│ ReviewerAgent         │                                       │
└──────────────────────┴───────────────────────────────────────┘
```

---

# 八、详情页数据请求规则

进入详情页后请求：

```text
GET /api/tasks/{taskId}
GET /api/tasks/{taskId}/agent-runs
GET /api/tasks/{taskId}/report
GET /api/tasks/{taskId}/evidence
GET /api/tasks/{taskId}/review
```

数据权威来源：

```text
1. 任务状态以 GET /api/tasks/{taskId} 为准。
2. Agent 执行过程以 GET /api/tasks/{taskId}/agent-runs 为准。
3. 报告、证据、质检接口允许在运行中为空。
4. 某个 Tab 加载失败，不应导致整个详情页失败。
5. 如果任务详情接口 404 或 success=false，整页显示任务不存在或后端错误。
```

---

# 九、状态派生规则

在 `src/utils/status.ts` 中统一定义状态逻辑，不要在各个 Vue 组件里散落判断。

```ts
export const RUNNING_STATUSES = [
  'CREATED',
  'PLANNING',
  'COLLECTING',
  'EXTRACTING',
  'ANALYZING',
  'WRITING',
  'REVIEWING',
  'REPAIRING'
] as const;

export const TERMINAL_STATUSES = [
  'COMPLETED',
  'COMPLETED_WITH_WARNINGS',
  'WAITING_HUMAN_REVIEW',
  'FAILED'
] as const;
```

需要实现这些工具函数：

```ts
isRunningStatus(status: TaskStatus): boolean

isTerminalStatus(status: TaskStatus): boolean

getStatusTagType(status: TaskStatus): 'success' | 'warning' | 'danger' | 'primary' | 'info'

getStatusText(status: TaskStatus): string

getCurrentAgentByStatus(status: TaskStatus): string
```

状态到 Agent 的映射：

| 任务状态                    | 当前阶段                    |
| ----------------------- | ----------------------- |
| CREATED                 | Task Created            |
| PLANNING                | PlannerAgent            |
| COLLECTING              | CollectorAgent          |
| EXTRACTING              | ExtractorAgent          |
| ANALYZING               | AnalyzerAgent           |
| WRITING                 | WriterAgent             |
| REVIEWING               | ReviewerAgent           |
| REPAIRING               | RepairRouter            |
| WAITING_HUMAN_REVIEW    | Human Review            |
| COMPLETED               | Completed               |
| COMPLETED_WITH_WARNINGS | Completed with Warnings |
| FAILED                  | Failed                  |

---

# 十、轮询策略

当前后端可能是同步执行，但前端必须兼容未来异步执行。

详情页轮询规则：

```text
1. 页面首次进入时并行请求所有数据。
2. 如果任务状态是运行中：
   - 每 3 秒轮询 GET /api/tasks/{taskId}
   - 每 3 秒轮询 GET /api/tasks/{taskId}/agent-runs
   - 每 6 秒轮询 GET /api/tasks/{taskId}/evidence
   - 当状态进入 WRITING、REVIEWING、REPAIRING 或终态时，再请求 report 和 review。
3. 如果任务状态进入终态：
   - 停止轮询。
   - 立即刷新一次 task、agent-runs、report、evidence、review。
4. 页面卸载时清理所有 timer。
```

终态：

```text
COMPLETED
COMPLETED_WITH_WARNINGS
WAITING_HUMAN_REVIEW
FAILED
```

---

# 十一、顶部任务状态区

创建组件：

```text
src/components/TaskStatusHeader.vue
```

展示字段：

| 字段             | 来源                        |
| -------------- | ------------------------- |
| taskName       | task detail               |
| taskId         | route param / task detail |
| status         | task detail               |
| domain         | task detail               |
| targetProducts | task detail               |
| iterationCount | task detail               |
| maxIterations  | task detail               |
| updatedAt      | task detail               |

展示效果：

```text
AI 编程工具竞品分析                    [COMPLETED]

Task ID: xxx
领域：AI_CODING_TOOLS
产品：Cursor, GitHub Copilot
修复轮次：0 / 1
最近更新：2026-xx-xx xx:xx

[刷新] [返回创建页]
```

taskId 支持复制。

---

# 十二、左侧 Agent 工作流面板

创建组件：

```text
src/components/AgentWorkflowPanel.vue
```

展示固定六个 Agent：

```text
PlannerAgent
CollectorAgent
ExtractorAgent
AnalyzerAgent
WriterAgent
ReviewerAgent
```

每个 Agent 展示：

```text
1. Agent 名称
2. 职责说明
3. 当前状态：等待中 / 运行中 / 成功 / 失败
4. 执行次数
5. 最近一次耗时
```

Agent 职责文案：

| Agent          | 职责                 |
| -------------- | ------------------ |
| PlannerAgent   | 生成采集任务计划           |
| CollectorAgent | 搜索公开资料并生成证据池       |
| ExtractorAgent | 结构化抽取产品画像和 Claim   |
| AnalyzerAgent  | 横向对比分析与 SWOT       |
| WriterAgent    | 生成 Markdown 竞品分析报告 |
| ReviewerAgent  | 质检评分并判断是否回退        |

状态来源优先级：

```text
1. 如果 agent-runs 中有该 Agent 的记录，根据最后一次 run.status 判断。
2. 如果当前 task.status 映射到该 Agent，则显示运行中。
3. 已完成的 Agent 显示成功。
4. 未开始的 Agent 显示等待中。
```

如果同一个 Agent 出现多次，显示：

```text
执行次数：2
```

用来体现自动修复回退。

---

# 十三、右侧 Tabs

详情页右侧使用 `el-tabs`，包含：

```text
概览
报告
证据
质检
Agent 轨迹
```

---

# 十四、概览 Tab

概览 Tab 分成四块：

```text
1. 任务摘要
2. 执行摘要
3. 质量摘要
4. 关键产物统计
```

## 14.1 任务摘要

展示：

```text
taskName
domain
targetProducts
analysisGoal
outputFormat
language
```

## 14.2 执行摘要

根据 task detail 和 agent-runs 计算：

```text
当前状态
当前阶段
修复轮次
AgentRun 总数
成功数
失败数
总耗时
```

## 14.3 质量摘要

根据 review 结果展示：

```text
passed
score
summary
nextAction
```

如果 review 为空且任务运行中，显示：

```text
质检尚未完成。
```

## 14.4 关键产物统计

展示：

```text
报告章节数
证据数量
质检问题数
AgentRun 数量
```

---

# 十五、报告 Tab

创建组件：

```text
src/components/ReportSectionViewer.vue
```

## 15.1 展示结构

采用左侧章节目录 + 右侧正文：

```text
┌───────────────┬─────────────────────────────┐
│ 章节目录       │ 当前章节正文                  │
│ section list  │ markdown / plain text        │
└───────────────┴─────────────────────────────┘
```

展示内容：

```text
reportTitle
reportFormat
sections
```

每个 section 展示：

```text
title
content
evidenceIds
relatedClaimIds
```

---

## 15.2 Markdown 渲染

优先使用已有依赖。
如果项目未安装 Markdown 渲染库，可以先不新增复杂依赖，使用纯文本方式展示：

```css
white-space: pre-wrap;
```

如果需要引入依赖，优先使用轻量方案：

```bash
npm install markdown-it
```

但不要为了 Markdown 引入过重编辑器。

---

## 15.3 Evidence 联动

每个章节底部展示关联证据：

```text
关联证据：
[E-001] [E-002] [E-006]

关联结论：
[C-003] [C-008]
```

点击 evidenceId 后：

第一版可实现以下任一种：

```text
方案 A：切换到证据 Tab，并在关键词搜索框中填入 evidenceId。
方案 B：打开 el-drawer，展示该 Evidence 的详情。
```

优先实现方案 A，简单稳定。

---

## 15.4 空状态

如果任务运行中且报告为空：

```text
报告生成中。
```

如果任务终态但报告为空：

```text
报告尚未生成或未保存。
```

---

# 十六、证据 Tab

创建组件：

```text
src/components/EvidenceTable.vue
```

## 16.1 表格字段

| 字段             | 展示方式     |
| -------------- | -------- |
| evidenceId     | 短 ID，可复制 |
| productName    | 文本       |
| sourceType     | 标签       |
| sourceTitle    | 文本       |
| url            | 外链       |
| contentSnippet | 摘要       |
| reliability    | 标签       |
| usedFor        | 标签组      |
| collectedAt    | 时间       |
| referenced     | 是否被报告引用  |

---

## 16.2 本地筛选能力

第一版筛选在前端本地完成：

```text
1. 按产品筛选
2. 按来源类型筛选
3. 按可靠性筛选
4. 按关键词搜索 sourceTitle / contentSnippet / evidenceId
```

不要新增后端接口。

---

## 16.3 报告引用关系

根据 report.sections 的 evidenceIds，在前端计算 evidence 是否被报告引用。

展示：

```text
已引用 / 未引用
```

这样可以突出证据链。

---

# 十七、质检 Tab

创建组件：

```text
src/components/ReviewResultPanel.vue
```

## 17.1 展示结构

```text
质检总览
下一步动作
问题列表
```

## 17.2 质检总览

展示：

```text
passed
score
summary
```

UI 规则：

| 情况            | 展示        |
| ------------- | --------- |
| passed=true   | 绿色成功状态    |
| passed=false  | 黄色或红色警告状态 |
| review 为空且运行中 | 质检尚未完成    |
| review 为空且终态  | 暂无质检结果    |

---

## 17.3 下一步动作

展示：

```text
nextAction.action
nextAction.targetAgent
nextAction.reason
```

如果 `passed=false`，该区域要突出显示，因为它体现 Reviewer 的回退决策。

文案示例：

```text
Reviewer 判定当前报告未通过，系统将根据修复建议回退到 CollectorAgent。
```

如果进入 `WAITING_HUMAN_REVIEW`：

```text
已达到最大自动修复轮次，等待人工审核。
```

---

## 17.4 issues 表格

字段：

| 字段                | 说明       |
| ----------------- | -------- |
| issueId           | 问题 ID    |
| severity          | 严重程度     |
| type              | 问题类型     |
| description       | 问题描述     |
| targetAgent       | 目标 Agent |
| targetProduct     | 目标产品     |
| targetDimension   | 目标维度     |
| repairInstruction | 修复建议     |

空状态：

```text
质检通过，未发现阻塞问题。
```

---

# 十八、Agent 轨迹 Tab

创建组件：

```text
src/components/AgentRunTimeline.vue
```

## 18.1 展示方式

使用垂直时间线或表格。推荐先用 `el-timeline`。

展示字段：

| 字段           | 展示方式     |
| ------------ | -------- |
| agentType    | Agent 名称 |
| status       | 标签       |
| inputType    | 文本       |
| outputType   | 文本       |
| startTime    | 时间       |
| endTime      | 时间       |
| durationMs   | 毫秒或秒     |
| errorMessage | 失败详情     |

---

## 18.2 展开详情

每条 AgentRun 可以点击展开：

```text
Agent：CollectorAgent
状态：SUCCESS
输入类型：TaskPlanDTO
输出类型：RawSourceSetDTO
开始时间：...
结束时间：...
耗时：...
错误信息：...
```

如果后端没有 input/output 原文，不要前端伪造。

---

## 18.3 自动修复展示

如果同一个 Agent 出现多次，按返回顺序展示即可。
可以增加文案：

```text
检测到该 Agent 多次执行，可能由 Reviewer 质检回退触发。
```

---

# 十九、API Client 规范

## 19.1 统一响应类型

在 `src/types/common.ts` 中定义：

```ts
export interface Result<T> {
  code: number;
  message: string | null;
  success: boolean;
  data: T;
}
```

---

## 19.2 request 封装

在 `src/api/request.ts` 中实现：

```ts
import type { Result } from '@/types/common';

export async function request<T>(
  path: string,
  init?: RequestInit
): Promise<T> {
  const response = await fetch(path, {
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers || {})
    },
    ...init
  });

  const result = (await response.json()) as Result<T>;

  if (!result.success) {
    throw new Error(result.message || 'Request failed');
  }

  return result.data;
}
```

注意：后端业务错误可能 HTTP 200，但 `success=false`，所以必须检查 `result.success`。

---

## 19.3 task-api.ts

在 `src/api/task-api.ts` 中封装所有任务接口：

```ts
import { request } from './request';
import type { TaskCreateRequest, TaskCreateResponse, TaskDetailResponse } from '@/types/task';
import type { ReportResponse } from '@/types/report';
import type { EvidenceItem } from '@/types/evidence';
import type { ReviewResult } from '@/types/review';
import type { AgentRun } from '@/types/agent-run';

export function createTask(payload: TaskCreateRequest) {
  return request<TaskCreateResponse>('/api/tasks', {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function getTaskDetail(taskId: string) {
  return request<TaskDetailResponse>(`/api/tasks/${taskId}`);
}

export function getTaskReport(taskId: string) {
  return request<ReportResponse>(`/api/tasks/${taskId}/report`);
}

export function getTaskEvidence(taskId: string) {
  return request<EvidenceItem[]>(`/api/tasks/${taskId}/evidence`);
}

export function getTaskReview(taskId: string) {
  return request<ReviewResult>(`/api/tasks/${taskId}/review`);
}

export function getAgentRuns(taskId: string) {
  return request<AgentRun[]>(`/api/tasks/${taskId}/agent-runs`);
}
```

页面组件不直接调用 fetch。

---

# 二十、TypeScript 类型要求

请根据 `docs/06_api_spec.md` 对齐最终字段。
至少需要定义以下类型。

## 20.1 Task

```ts
export type TaskStatus =
  | 'CREATED'
  | 'PLANNING'
  | 'COLLECTING'
  | 'EXTRACTING'
  | 'ANALYZING'
  | 'WRITING'
  | 'REVIEWING'
  | 'REPAIRING'
  | 'WAITING_HUMAN_REVIEW'
  | 'COMPLETED'
  | 'COMPLETED_WITH_WARNINGS'
  | 'FAILED';

export interface TaskCreateRequest {
  taskName: string;
  domain: string;
  targetProducts: string[];
  analysisGoal: string;
  outputFormat: string;
  language: string;
  maxIterations: number;
}

export interface TaskCreateResponse {
  taskId: string;
}

export interface TaskDetailResponse {
  taskId: string;
  taskName: string;
  domain: string;
  targetProducts: string[];
  analysisGoal: string;
  status: TaskStatus;
  iterationCount: number;
  maxIterations: number;
  createdAt: string;
  updatedAt: string;
}
```

---

# 二十一、错误、Loading、空状态

## 21.1 创建任务 Loading

创建任务期间显示：

```text
任务已提交，CA Agent 正在执行多 Agent 分析流程。真实模型模式下可能需要几十秒，请勿重复提交。
```

## 21.2 详情页运行中提示

详情页顶部显示：

```text
任务正在执行，结果会在完成后逐步展示。
```

## 21.3 Tab 独立错误

每个 Tab 独立处理 loading 和 error。

```text
报告加载失败不影响证据展示。
证据加载失败不影响 Agent 轨迹展示。
质检为空不代表任务失败。
```

## 21.4 通用错误文案

优先展示后端 message。
没有 message 时展示：

```text
请求失败，请稍后重试。
```

---

# 二十二、不要做的内容

第一版不要实现：

```text
1. 用户登录和权限控制
2. 后端任务列表分页
3. 任务取消、重跑、删除
4. WebSocket / SSE
5. 在线编辑报告
6. PDF / Word 导出
7. API Key 配置页面
8. 数据库配置页面
9. 复杂 Dashboard 首页
10. 复杂 DAG 拖拽图
```

当前目标是把一次任务的完整链路展示清楚。

---

# 二十三、开发顺序

请按以下顺序实现，避免一次性大改导致项目不可运行。

## 第一步：基础结构

```text
1. 检查当前 src 目录。
2. 保留现有 TaskCreate.vue 和 TaskDetail.vue 的可用逻辑。
3. 新增 api、types、utils、components 目录。
4. 建立统一 request 和 task-api。
5. 配置 vue-router，确保 / 和 /tasks/:taskId 可用。
```

## 第二步：任务创建页

```text
1. 重构 TaskCreate.vue。
2. 实现 Demo 一键填充。
3. 实现表单校验。
4. 实现创建 loading。
5. 成功后跳转详情页。
6. 保存 recent-task-ids。
7. 增加手动输入 taskId 打开任务。
```

## 第三步：任务详情页骨架

```text
1. 重构 TaskDetail.vue。
2. 顶部使用 TaskStatusHeader。
3. 左侧使用 AgentWorkflowPanel。
4. 右侧使用 el-tabs。
5. 实现首次进入并行加载所有数据。
6. 实现区块 loading 和错误状态。
```

## 第四步：核心内容组件

```text
1. 实现概览 Tab。
2. 实现 ReportSectionViewer。
3. 实现 EvidenceTable。
4. 实现 ReviewResultPanel。
5. 实现 AgentRunTimeline。
```

## 第五步：异步兼容

```text
1. 实现运行中状态判断。
2. 实现轮询 task detail 和 agent-runs。
3. 状态终态后停止轮询。
4. 页面卸载时清理 timer。
```

## 第六步：体验优化

```text
1. 优化状态颜色。
2. 优化空状态文案。
3. 优化错误提示。
4. 支持 evidenceId 搜索或联动。
5. 支持 taskId 复制。
```

---

# 二十四、验收标准

完成后需要满足：

```text
1. 用户打开 / 可以创建竞品分析任务。
2. 用户可以一键填充 AI 编程工具 Demo。
3. 创建任务成功后自动跳转 /tasks/:taskId。
4. 详情页可以展示任务基本信息和任务状态。
5. 详情页左侧可以展示六 Agent 工作流进度。
6. 详情页概览 Tab 可以展示任务摘要、执行摘要、质量摘要和产物统计。
7. 报告 Tab 可以展示报告章节。
8. 报告章节可以展示 evidenceIds 和 relatedClaimIds。
9. 证据 Tab 可以展示 Evidence 表格。
10. 证据 Tab 支持本地筛选和关键词搜索。
11. 质检 Tab 可以展示 passed、score、summary、nextAction 和 issues。
12. Agent 轨迹 Tab 可以展示 AgentRun 时间线。
13. 接口 success=false 时能展示明确错误。
14. 任务运行中时页面显示生成中状态，而不是整页失败。
15. 任务进入终态后停止轮询。
16. 没有任务列表接口时，仍能通过 recent-task-ids 或手动输入 taskId 打开历史任务。
```

---

# 二十五、最终效果要求

最终前端应该呈现为一个简洁的 CA Agent 工作台：

```text
首页负责创建任务和打开最近任务；
详情页负责展示一次任务的完整生命周期；
顶部展示任务状态；
左侧展示六 Agent 工作流；
右侧展示概览、报告、证据、质检和 Agent 轨迹；
报告能关联 evidenceId；
证据能追溯 URL；
质检能展示 Reviewer 的评分、问题和回退建议。
```

不要把它做成普通 CRUD 后台。
这个前端的核心价值是让用户直观看到：

```text
CA Agent 不是单次大模型生成报告，
而是一个具有计划、采集、抽取、分析、写作、质检和回退能力的多 Agent 竞品分析系统。
```
