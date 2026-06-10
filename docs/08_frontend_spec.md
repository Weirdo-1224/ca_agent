# 08 前端页面规格

本文档描述 Task 09 的前端实现目标。前端第一版围绕当前后端已有接口实现，不引入后端尚未提供的任务列表、导出文件或用户登录能力。虽然当前后端创建任务是同步执行，但前端页面结构和数据流必须兼容未来异步执行：创建成功后统一进入详情页，由详情页根据任务状态展示生成中、已完成、待人工审核或失败。

当前后端接口见 `docs/06_api_spec.md`。

---

## 1. 产品定位

前端是 CA Agent 的演示和操作台，用于完成以下闭环：

```text
创建竞品分析任务
  -> 查看任务状态
  -> 查看 Agent 执行轨迹
  -> 阅读最终报告
  -> 检查证据来源
  -> 查看质检结果
```

第一版重点是“可演示、可检查、可追溯”，不追求复杂后台管理。页面不要假设 `POST /api/tasks` 返回后报告一定已经生成；即使当前本地实现大多同步完成，也应按任务状态驱动 UI。

---

## 2. 信息架构

### 2.1 页面范围

| 页面 | 路由建议 | 说明 |
| --- | --- | --- |
| 任务创建页 | `/` 或 `/tasks/new` | 创建竞品分析任务 |
| 任务详情页 | `/tasks/:taskId` | 任务摘要、执行轨迹、报告、证据、质检 |

为减少实现复杂度，第一版可以把“详情、Agent 流程、报告、证据、质检”做成同一个详情页内的 Tabs。

### 2.2 详情页 Tabs

```text
概览
报告
证据
质检
Agent 轨迹
```

---

## 3. 页面布局

### 3.1 全局布局

建议使用工作台式布局：

```text
┌──────────────────────────────────────────────┐
│ 顶部栏：CA Agent / 当前任务 / 操作按钮          │
├──────────────────────────────────────────────┤
│ 主内容区                                      │
└──────────────────────────────────────────────┘
```

设计原则：

1. 信息密度适中，优先展示任务、报告和证据，不做营销式首页。
2. 页面主色保持克制，状态色用于区分成功、警告、失败和运行中。
3. 关键内容使用表格、时间线、Tabs 和分栏，不用大面积装饰卡片。
4. 移动端可以降级为单列布局，但第一优先级是桌面演示体验。

---

## 4. 任务创建页

### 4.1 表单字段

对应后端 `TaskCreateRequest`。

| 字段 | 控件 | 默认值 | 说明 |
| --- | --- | --- | --- |
| taskName | 文本输入 | AI 编程工具竞品分析 | 任务名称 |
| domain | 下拉或文本输入 | AI_CODING_TOOLS | 当前建议固定为 AI 编程工具领域 |
| targetProducts | 标签输入 / 多选输入 | Cursor, GitHub Copilot | 至少 2 个产品 |
| analysisGoal | 多行文本 | 生成面向产品团队的竞品分析报告 | 分析目标 |
| outputFormat | 下拉 | markdown | 第一版固定 markdown |
| language | 下拉 | zh-CN | 输出语言 |
| maxIterations | 数字输入 | 1 或 2 | 最大自动修复轮次 |

### 4.2 推荐 Demo 默认值

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

### 4.3 交互规则

1. 点击“创建任务”后调用 `POST /api/tasks`。
2. 请求期间按钮进入 loading 状态，并禁止重复提交。
3. 成功后保存返回的 `taskId`，一律跳转 `/tasks/:taskId`。
4. 失败时读取响应 `message`，显示在表单顶部。
5. 创建接口当前是同步执行，真实模型模式下可能等待较久，页面必须给出明确 loading 状态。
6. 跳转详情页后，由详情页重新查询任务状态和结果数据；不要只依赖创建接口返回的数据渲染整页。

### 4.3.1 未来异步兼容

未来后端改为异步执行时，创建任务接口可能很快返回，任务状态可能仍是 `CREATED`、`PLANNING` 或其他运行中状态。前端无需改变页面路由，只需要在详情页增加轮询：

```text
POST /api/tasks
  -> 返回 taskId
  -> 跳转 /tasks/:taskId
  -> GET /api/tasks/:taskId
  -> 运行中则每 2s - 5s 轮询
  -> 终态后停止轮询并展示结果
```

### 4.4 前端校验

| 字段 | 校验规则 |
| --- | --- |
| taskName | 非空 |
| domain | 非空 |
| targetProducts | 至少 2 个，去除空白项 |
| analysisGoal | 非空 |
| maxIterations | 整数，建议范围 0 到 3 |

---

## 5. 任务详情页

### 5.1 数据请求

进入详情页后并行请求：

```text
GET /api/tasks/{taskId}
GET /api/tasks/{taskId}/report
GET /api/tasks/{taskId}/evidence
GET /api/tasks/{taskId}/review
GET /api/tasks/{taskId}/agent-runs
```

任一接口返回 `success=false` 时，对应区域显示错误状态；如果任务详情接口返回 404，整页显示“任务不存在”。

详情页的数据源优先级：

1. `GET /api/tasks/{taskId}` 是状态权威来源。
2. `GET /api/tasks/{taskId}/agent-runs` 是失败定位和执行过程来源。
3. 报告、证据和质检接口允许在运行中阶段返回空数据或错误，前端应展示“生成中”而不是整页失败。

### 5.2 运行中状态兼容

以下状态视为运行中：

```text
CREATED
PLANNING
COLLECTING
EXTRACTING
ANALYZING
WRITING
REVIEWING
REPAIRING
```

运行中时：

1. 概览区展示当前状态。
2. 报告 Tab 显示“报告生成中”。
3. 证据 Tab 可以展示已返回证据；如果为空，显示“证据采集中”。
4. 质检 Tab 显示“质检尚未完成”。
5. Agent 轨迹 Tab 展示已产生的执行记录。

终态：

```text
COMPLETED
COMPLETED_WITH_WARNINGS
WAITING_HUMAN_REVIEW
FAILED
```

进入终态后停止轮询。

### 5.3 概览 Tab

展示内容：

| 模块 | 数据来源 | 展示字段 |
| --- | --- | --- |
| 任务摘要 | `/api/tasks/{taskId}` | taskName, domain, targetProducts, analysisGoal |
| 状态摘要 | `/api/tasks/{taskId}` | status, iterationCount, maxIterations |
| 质量摘要 | `/api/tasks/{taskId}/review` | passed, score, summary |
| 执行摘要 | `/api/tasks/{taskId}/agent-runs` | Agent 总数、失败数、总耗时 |

状态展示建议：

| 状态 | UI 表现 |
| --- | --- |
| COMPLETED | 绿色或成功徽标 |
| WAITING_HUMAN_REVIEW | 黄色或警告徽标 |
| FAILED | 红色或错误徽标 |
| 运行中状态 | 蓝色或中性 loading 状态 |

---

## 6. 报告 Tab

### 6.1 数据来源

```text
GET /api/tasks/{taskId}/report
```

### 6.2 展示内容

1. 报告标题 `reportTitle`。
2. 报告格式 `reportFormat`。
3. 报告章节 `sections`。
4. 每个章节展示 `title`、`content`、`evidenceIds`、`relatedClaimIds`。

### 6.3 渲染规则

第一版可以直接把 `content` 作为 Markdown 渲染。若暂不接 Markdown 渲染库，则按纯文本保留换行展示。

章节建议使用左侧目录 + 右侧正文：

```text
┌───────────────┬─────────────────────────────┐
│ 章节目录       │ 当前章节正文                  │
│ section list  │ markdown / plain text        │
└───────────────┴─────────────────────────────┘
```

### 6.4 空状态

如果 `sections` 为空且任务仍在运行中，显示：

```text
报告生成中。
```

如果任务已进入终态但 `sections` 仍为空，显示：

```text
报告尚未生成或未保存。
```

---

## 7. 证据 Tab

### 7.1 数据来源

```text
GET /api/tasks/{taskId}/evidence
```

### 7.2 展示字段

| 字段 | 展示方式 |
| --- | --- |
| evidenceId | 短 ID / 可复制 |
| productName | 文本 |
| sourceType | 标签 |
| sourceTitle | 文本 |
| url | 外链 |
| contentSnippet | 摘要文本 |
| reliability | 可靠性标签 |
| usedFor | 标签组 |
| collectedAt | 时间 |

### 7.3 筛选能力

第一版建议支持：

1. 按产品筛选。
2. 按来源类型筛选。
3. 按可靠性筛选。
4. 按关键词搜索 `sourceTitle` 和 `contentSnippet`。

这些筛选可以在前端本地完成，不需要新增后端接口。

---

## 8. 质检 Tab

### 8.1 数据来源

```text
GET /api/tasks/{taskId}/review
```

### 8.2 展示内容

| 模块 | 字段 |
| --- | --- |
| 通过状态 | passed |
| 分数 | score |
| 摘要 | summary |
| 下一步 | nextAction.action, nextAction.targetAgent, nextAction.reason |
| 问题列表 | issues |

`issues` 表格字段：

| 字段 | 说明 |
| --- | --- |
| issueId | 问题 ID |
| severity | 严重程度 |
| type | 问题类型 |
| description | 问题描述 |
| targetAgent | 目标 Agent |
| targetProduct | 目标产品 |
| targetDimension | 目标维度 |
| repairInstruction | 修复建议 |

### 8.3 空状态

如果 `issues` 为空且 `passed=true`，显示“质检通过，未发现阻塞问题”。

---

## 9. Agent 轨迹 Tab

### 9.1 数据来源

```text
GET /api/tasks/{taskId}/agent-runs
```

### 9.2 展示方式

建议使用垂直时间线或表格：

```text
Planner -> Collector -> Extractor -> Analyzer -> Writer -> Reviewer
```

字段：

| 字段 | 展示方式 |
| --- | --- |
| agentType | Agent 名称 |
| status | 成功 / 失败标签 |
| inputType | 文本 |
| outputType | 文本 |
| startTime | 时间 |
| endTime | 时间 |
| durationMs | 毫秒或秒 |
| errorMessage | 失败详情 |

### 9.3 自动修复展示

如果同一个 Agent 出现多次，说明工作流可能发生过自动修复。前端不需要推断复杂拓扑，按返回顺序展示即可。

---

## 10. 前端数据模型

建议在前端定义与后端响应一致的 TypeScript 类型。

```ts
export interface Result<T> {
  code: number;
  message: string | null;
  success: boolean;
  data: T;
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
```

其余类型以 `docs/06_api_spec.md` 为准。

---

## 11. API Client 约定

建议封装一个统一请求函数：

```ts
async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, init);
  const result = await response.json() as Result<T>;

  if (!result.success) {
    throw new Error(result.message || 'Request failed');
  }

  return result.data;
}
```

注意：后端业务错误当前也返回 HTTP 200，所以必须检查 `result.success`。

---

## 12. 轮询策略

当前同步后端下，轮询不是必需功能；但为了兼容未来异步执行，详情页的数据结构应预留轮询能力。

建议轮询规则：

1. 只轮询 `GET /api/tasks/{taskId}` 和 `GET /api/tasks/{taskId}/agent-runs`。
2. 任务状态为运行中时，每 2s - 5s 请求一次。
3. 状态进入终态后停止轮询。
4. 报告、证据、质检可以在状态接近完成或进入终态后再刷新。
5. 页面离开详情页时停止轮询。

终态列表：

```text
COMPLETED
COMPLETED_WITH_WARNINGS
WAITING_HUMAN_REVIEW
FAILED
```

---

## 13. 本地状态

由于当前后端没有任务列表接口，前端可以在浏览器本地保存最近创建的任务：

```text
localStorage key: ca-agent.recent-task-ids
value: string[]
```

用途：

1. 创建任务后记录 `taskId`。
2. 首页展示“最近任务”快捷入口。
3. 仅作为前端便利功能，不作为权威数据源。

---

## 14. 加载与错误状态

### 14.1 创建任务 Loading

创建任务期间显示：

```text
正在创建竞品分析任务，真实模型模式下可能需要数十秒。
```

### 14.2 详情页运行中提示

详情页运行中时显示：

```text
任务正在执行，结果会在完成后展示。
```

### 14.3 区块 Loading

详情页各 Tab 可以独立 loading。报告加载失败不应影响证据或 Agent 轨迹展示。

### 14.4 错误展示

错误内容优先展示后端 `message`。如果没有 message，则展示通用文案：

```text
请求失败，请稍后重试。
```

---

## 15. 第一版不做项

1. 不做用户登录和权限控制。
2. 不做任务列表后端分页。
3. 不做任务取消、重跑、删除。
4. 不做 WebSocket / SSE 流式进度。
5. 不做在线编辑报告。
6. 不做后端导出接口。
7. 不在前端保存 API Key 或数据库凭据。

---

## 16. 验收标准

1. 用户可以在页面创建 AI 编程工具竞品分析任务。
2. 创建成功后可以进入任务详情页。
3. 页面可以根据任务状态展示运行中、已完成、待人工审核或失败。
4. 页面可以展示最终报告章节。
5. 页面可以展示 evidence source list。
6. 页面可以展示 reviewResult 和 issue 列表。
7. 页面可以展示 AgentRun 执行轨迹。
8. 接口返回 `success=false` 时，页面能展示明确错误。
9. 没有任务列表接口时，页面仍可通过最近任务或直接输入 `taskId` 查看历史任务。
