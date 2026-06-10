# 06 API 规格

本文档描述当前后端已经暴露的 HTTP API。文档以当前项目实现为准，包名为 `org.example.ca_agent`，核心 Controller 为 `AnalysisTaskController`。

当前接口定位是“单次竞品分析任务的创建与结果查询”。当前后端实现中，任务创建接口会同步执行完整工作流；但 API 契约按“未来可迁移到异步执行”设计：前端拿到 `taskId` 后统一进入详情页，并以后续查询接口中的 `status`、报告、证据、质检和 AgentRun 作为页面真实状态来源。

未来上线切换为异步执行时，`POST /api/tasks` 可以改为“只创建任务并返回 `taskId` 和初始状态”，现有查询接口路径保持不变。

---

## 1. 基础约定

### 1.1 Base URL

本地默认地址：

```text
http://localhost:8080
```

所有业务接口统一以 `/api/tasks` 开头。

### 1.2 Content-Type

请求体和响应体均使用 JSON：

```http
Content-Type: application/json
```

### 1.3 统一响应结构

所有接口均返回 `Result<T>`：

```json
{
  "code": 0,
  "message": null,
  "success": true,
  "data": {}
}
```

字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | Integer | 业务状态码。成功为 `0` |
| message | String | 失败原因。成功时通常为 `null` |
| success | Boolean | 是否成功 |
| data | Object / Array / null | 业务数据 |

### 1.4 错误响应

当前全局异常处理会把业务异常包装为 HTTP 200 + `success=false`。前端判断接口是否成功时必须以 `success` 字段为准，不应只依赖 HTTP 状态码。

任务不存在示例：

```json
{
  "code": 404,
  "message": "Task not found: task_missing",
  "success": false,
  "data": null
}
```

非业务异常示例：

```json
{
  "code": 500,
  "message": "Internal server error",
  "success": false,
  "data": null
}
```

---

## 2. 枚举值

### 2.1 TaskStatus

任务状态字段以 Java 枚举名返回：

```text
CREATED
PLANNING
COLLECTING
EXTRACTING
ANALYZING
WRITING
REVIEWING
REPAIRING
WAITING_HUMAN_REVIEW
COMPLETED
COMPLETED_WITH_WARNINGS
FAILED
```

前端建议展示文案：

| 枚举值 | 展示文案 |
| --- | --- |
| CREATED | 已创建 |
| PLANNING | 规划中 |
| COLLECTING | 采集中 |
| EXTRACTING | 提取中 |
| ANALYZING | 分析中 |
| WRITING | 撰写中 |
| REVIEWING | 质检中 |
| REPAIRING | 修复中 |
| WAITING_HUMAN_REVIEW | 等待人工审核 |
| COMPLETED | 已完成 |
| COMPLETED_WITH_WARNINGS | 已完成，有警告 |
| FAILED | 已失败 |

### 2.2 AgentType

```text
PLANNER_AGENT
COLLECTOR_AGENT
EXTRACTOR_AGENT
ANALYZER_AGENT
WRITER_AGENT
REVIEWER_AGENT
```

### 2.3 SourceType

```text
OFFICIAL_SITE
PRICING_PAGE
DOCUMENTATION
BLOG
CHANGELOG
GITHUB
REVIEW_ARTICLE
COMMUNITY_DISCUSSION
NEWS
USER_COMMENT
UNKNOWN
```

### 2.4 ReliabilityLevel

```text
HIGH
MEDIUM
LOW
```

### 2.5 ReviewIssueType

```text
MISSING_EVIDENCE
EVIDENCE_NOT_LINKED
SCHEMA_MISSING_FIELD
COMPARISON_INCOMPLETE
VAGUE_FINDING
REPORT_MISSING_SECTION
CITATION_FORMAT_ERROR
HALLUCINATION_RISK
UNKNOWN_FIELD_TOO_MANY
```

---

## 3. 接口清单

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/tasks` | 创建分析任务。当前实现同步执行，未来可改为异步入队 |
| GET | `/api/tasks/{taskId}` | 查询任务详情 |
| GET | `/api/tasks/{taskId}/report` | 查询报告正文和报告质检摘要 |
| GET | `/api/tasks/{taskId}/evidence` | 查询证据列表 |
| GET | `/api/tasks/{taskId}/review` | 查询完整质检结果 |
| GET | `/api/tasks/{taskId}/agent-runs` | 查询 Agent 执行轨迹 |

当前未提供任务列表、删除任务、取消任务、流式进度推送接口。前端第一版应围绕“创建后进入详情页，以查询接口展示结果”实现；这样后续从同步执行切换到异步执行时，只需要在详情页增加轮询或 SSE，不需要重做页面和资源路径。

---

## 4. 创建任务

```http
POST /api/tasks
```

### 4.1 请求体

对应 `TaskCreateRequest`。

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| taskName | String | 是 | 任务名称 |
| domain | String | 是 | 分析领域，例如 `AI_CODING_TOOLS` |
| targetProducts | List<String> | 是 | 目标竞品列表 |
| analysisGoal | String | 是 | 分析目标 |
| outputFormat | String | 否 | 输出格式，当前建议传 `markdown` |
| language | String | 否 | 输出语言，建议传 `zh-CN` |
| maxIterations | Integer | 否 | 最大自动修复轮次 |

示例：

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

### 4.2 响应体

`data` 对应 `TaskDetailResponse`。

当前同步实现中，接口返回时 `status` 通常已经是 `COMPLETED`、`WAITING_HUMAN_REVIEW` 或 `FAILED`。未来异步实现中，接口可能立即返回 `CREATED`、`PLANNING`、`COLLECTING` 等运行中状态。

```json
{
  "code": 0,
  "message": null,
  "success": true,
  "data": {
    "taskId": "task_27e3f6b4d0b44e9a8d7740dc924dcba0",
    "taskName": "AI 编程工具竞品分析",
    "domain": "AI_CODING_TOOLS",
    "targetProducts": ["Cursor", "GitHub Copilot"],
    "analysisGoal": "生成面向产品团队的 AI 编程工具竞品分析报告",
    "status": "COMPLETED",
    "iterationCount": 1,
    "maxIterations": 1,
    "createdAt": "2026-06-06T10:00:00",
    "updatedAt": "2026-06-06T10:00:30"
  }
}
```

### 4.3 前端注意事项

`POST /api/tasks` 当前是同步接口。真实模型和搜索工具开启时，请求耗时可能较长，前端需要：

- 提交后进入 loading 状态，禁用重复提交。
- 设置较长请求超时。
- 根据 `success=false` 展示业务错误。
- 成功后保存 `taskId`，并跳转任务详情页。
- 详情页必须按 `status` 判断结果是否已经可读，而不是假设创建成功后报告一定存在。

为兼容未来异步执行，前端创建成功后的统一处理应为：

```text
POST /api/tasks
  -> 保存 taskId
  -> 跳转 /tasks/{taskId}
  -> 详情页查询 task detail / report / evidence / review / agent-runs
  -> 如果 status 仍是运行中，则展示生成中状态
```

---

## 5. 查询任务详情

```http
GET /api/tasks/{taskId}
```

### 5.1 路径参数

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| taskId | String | 任务 ID |

### 5.2 响应体

`data` 对应 `TaskDetailResponse`，字段同创建任务响应。

---

## 6. 查询报告

```http
GET /api/tasks/{taskId}/report
```

### 6.1 响应体

`data` 对应 `ReportResponse`。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| taskId | String | 任务 ID |
| reportTitle | String | 报告标题 |
| reportFormat | String | 报告格式 |
| sections | List<ReportSection> | 报告章节 |
| sourceList | List<Evidence> | 报告引用证据 |
| reviewResult | ReviewResultDTO | 报告质检摘要 |

`ReportSection`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| sectionId | String | 章节 ID |
| title | String | 章节标题 |
| content | String | 章节正文，通常为 Markdown 文本 |
| relatedClaimIds | List<String> | 关联 Claim ID |
| evidenceIds | List<String> | 关联 Evidence ID |

示例：

```json
{
  "code": 0,
  "message": null,
  "success": true,
  "data": {
    "taskId": "task_xxx",
    "reportTitle": "AI 编程工具竞品分析报告",
    "reportFormat": "markdown",
    "sections": [
      {
        "sectionId": "section_01",
        "title": "execution summary",
        "content": "## execution summary\n\n...",
        "relatedClaimIds": ["claim_1"],
        "evidenceIds": ["ev_1"]
      }
    ],
    "sourceList": [],
    "reviewResult": {
      "taskId": "task_xxx",
      "passed": true,
      "score": null,
      "summary": null,
      "issues": [],
      "nextAction": null
    }
  }
}
```

### 6.2 前端注意事项

- `content` 可以按 Markdown 渲染，也可以第一版按纯文本展示。
- `sourceList` 可能为空，完整证据列表应调用 `/evidence`。
- 当前 `ReportService` 从数据库重建 `reviewResult` 时主要包含 `passed` 和 `issues`，`score`、`summary`、`nextAction` 可能为 `null`。

---

## 7. 查询证据列表

```http
GET /api/tasks/{taskId}/evidence
```

### 7.1 响应体

`data` 为 `List<Evidence>`。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| evidenceId | String | 证据 ID |
| productName | String | 产品名称 |
| sourceType | SourceType | 来源类型 |
| sourceTitle | String | 来源标题 |
| url | String | 来源 URL |
| contentSnippet | String | 内容片段 |
| collectedAt | LocalDateTime | 采集时间 |
| reliability | ReliabilityLevel | 可靠性 |
| usedFor | List<String> | 支撑的分析维度 |

示例：

```json
{
  "code": 0,
  "message": null,
  "success": true,
  "data": [
    {
      "evidenceId": "ev_cursor_1",
      "productName": "Cursor",
      "sourceType": "OFFICIAL_SITE",
      "sourceTitle": "Cursor official site",
      "url": "https://example.com/cursor",
      "contentSnippet": "Cursor official information...",
      "collectedAt": "2026-06-06T10:00:10",
      "reliability": "HIGH",
      "usedFor": ["official"]
    }
  ]
}
```

---

## 8. 查询质检结果

```http
GET /api/tasks/{taskId}/review
```

### 8.1 响应体

`data` 对应 `ReviewResultDTO`。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| taskId | String | 任务 ID |
| passed | Boolean | 是否通过 |
| score | Integer | 质检分数 |
| summary | String | 质检摘要 |
| issues | List<ReviewIssue> | 问题列表 |
| nextAction | NextAction | 下一步动作 |

`ReviewIssue`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| issueId | String | 问题 ID |
| severity | String | 严重程度 |
| type | ReviewIssueType | 问题类型 |
| description | String | 问题描述 |
| targetAgent | AgentType | 建议回退的 Agent |
| targetProduct | String | 相关产品 |
| targetDimension | String | 相关维度 |
| repairInstruction | String | 修复建议 |

`NextAction`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| action | String | `finish` / `repair` / `human_review` |
| targetAgent | AgentType | 修复目标 Agent |
| reason | String | 原因 |

---

## 9. 查询 Agent 执行轨迹

```http
GET /api/tasks/{taskId}/agent-runs
```

### 9.1 响应体

`data` 为 `List<AgentRunResponse>`。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| runId | String | 单次 Agent 执行 ID |
| taskId | String | 任务 ID |
| agentType | AgentType | Agent 类型 |
| inputType | String | 输入类型描述 |
| outputType | String | 输出类型描述 |
| status | String | `SUCCESS` / `FAILED` |
| startTime | LocalDateTime | 开始时间 |
| endTime | LocalDateTime | 结束时间 |
| durationMs | Long | 执行耗时，毫秒 |
| errorMessage | String | 失败原因 |

示例：

```json
{
  "code": 0,
  "message": null,
  "success": true,
  "data": [
    {
      "runId": "d0f6f1f4-1111-4222-9333-abcdefabcdef",
      "taskId": "task_xxx",
      "agentType": "PLANNER_AGENT",
      "inputType": "TaskInputDTO",
      "outputType": "TaskPlanDTO",
      "status": "SUCCESS",
      "startTime": "2026-06-06T10:00:00",
      "endTime": "2026-06-06T10:00:02",
      "durationMs": 2000,
      "errorMessage": null
    }
  ]
}
```

### 9.2 前端展示建议

- 按返回顺序展示执行时间线。
- 对 `FAILED` 节点突出显示 `errorMessage`。
- 如果工作流发生自动修复，同一 Agent 可能出现多次执行记录。

---

## 10. 前端调用顺序

创建任务后的推荐调用顺序：

```text
POST /api/tasks
  -> 取得 taskId 和当前 status
GET /api/tasks/{taskId}
GET /api/tasks/{taskId}/report
GET /api/tasks/{taskId}/evidence
GET /api/tasks/{taskId}/review
GET /api/tasks/{taskId}/agent-runs
```

如果用户直接打开详情页，只要 URL 中有 `taskId`，可以并行请求详情、报告、证据、质检和 AgentRun。

### 10.1 异步迁移兼容规则

前端和 API Client 从现在开始按以下规则实现：

1. `POST /api/tasks` 只被视为“创建任务并返回 taskId”，不要把它绑定为“报告已生成”。
2. 是否完成以 `GET /api/tasks/{taskId}` 的 `status` 为准。
3. 当状态为 `CREATED`、`PLANNING`、`COLLECTING`、`EXTRACTING`、`ANALYZING`、`WRITING`、`REVIEWING`、`REPAIRING` 时，详情页展示运行中。
4. 当状态为 `COMPLETED` 或 `COMPLETED_WITH_WARNINGS` 时，展示报告、证据、质检和执行轨迹。
5. 当状态为 `WAITING_HUMAN_REVIEW` 时，展示已有报告和质检问题，并提示需要人工处理。
6. 当状态为 `FAILED` 时，展示失败状态和 AgentRun 中的错误信息。
7. 未来异步上线后，可在详情页增加轮询：

```text
GET /api/tasks/{taskId}
  every 2s - 5s while status is running
  stop when status is COMPLETED / COMPLETED_WITH_WARNINGS / WAITING_HUMAN_REVIEW / FAILED
```

---

## 11. 当前限制

1. 创建任务当前为同步执行，但 API 使用方式应按未来异步兼容方式设计。
2. 尚未提供任务列表接口，前端如需历史记录，第一版只能使用本地存储保存最近创建的 `taskId`。
3. 尚未提供报告导出接口，前端可先基于 `sections` 在浏览器侧导出 Markdown。
4. 当前错误响应 HTTP 状态码仍为 200，前端必须检查 `success`。
5. 当前无鉴权、无用户体系、无多租户隔离。
