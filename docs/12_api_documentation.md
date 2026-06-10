# Compass 智能竞品分析平台 — API 接口文档

> Base URL: `http://localhost:8080/api`  
> Content-Type: `application/json`  
> 响应格式: 统一 `{ code, message, data }` 包装

---

## 1. 创建分析任务

**POST** `/tasks`

### 请求体 (TaskCreateRequest)

```json
{
  "taskName": "AI 编程助手竞品分析",
  "domain": "AI_CODE_TOOLS",
  "targetProducts": ["Cursor", "GitHub Copilot", "Windsurf"],
  "analysisGoal": "分析当前主流 AI 编程工具的功能、定价和市场表现",
  "outputFormat": "markdown",
  "language": "zh-CN",
  "maxIterations": 2
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskName | String | 是 | 任务名称 |
| domain | String | 否 | 行业领域（系统可自动检测） |
| targetProducts | List<String> | 是 | 目标产品列表（2-6个） |
| analysisGoal | String | 否 | 分析目标描述 |
| outputFormat | String | 否 | 输出格式，默认 "markdown" |
| language | String | 否 | 语言代码，"zh-CN" / "en"，默认 "zh-CN" |
| maxIterations | Integer | 否 | 最大修复迭代次数，默认 2 |

### 响应体 (TaskDetailResponse)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "task_20260606_001",
    "taskName": "AI 编程助手竞品分析",
    "domain": "AI_CODE_TOOLS",
    "targetProducts": ["Cursor", "GitHub Copilot", "Windsurf"],
    "analysisGoal": "分析当前主流 AI 编程工具的功能、定价和市场表现",
    "status": "CREATED",
    "iterationCount": 0,
    "maxIterations": 2,
    "createdAt": "2026-06-06T10:00:00",
    "updatedAt": "2026-06-06T10:00:00"
  }
}
```

### 状态枚举 (TaskStatus)

| 值 | 说明 |
|---|------|
| CREATED | 任务已创建，尚未开始执行 |
| RUNNING | 工作流正在执行 |
| REPAIRING | 正在执行修复循环 |
| COMPLETED | 任务完成（通过质量审查） |
| FAILED | 任务失败 |

---

## 2. 获取任务详情

**GET** `/tasks/{taskId}`

### 路径参数

| 参数 | 类型 | 说明 |
|------|------|------|
| taskId | String | 任务唯一标识 |

### 响应体

同创建接口的 `data` 结构，`status` 字段反映当前执行状态。

---

## 3. 获取分析报告

**GET** `/tasks/{taskId}/report`

### 响应体 (ReportResponse)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "task_20260606_001",
    "reportTitle": "AI 编程助手竞品分析报告",
    "reportFormat": "markdown",
    "sections": [
      {
        "sectionId": "sec_01",
        "title": "执行摘要",
        "content": "## 执行摘要\n\n本报告对三款主流 AI 编程工具进行了深度分析...\n\n| 产品名称 | 产品定位 | 核心优势 |",
        "relatedClaimIds": ["claim_001", "claim_002"],
        "evidenceIds": ["ev_001", "ev_002"]
      }
    ],
    "sourceList": [
      {
        "evidenceId": "ev_001",
        "productName": "Cursor",
        "sourceType": "OFFICIAL_SITE",
        "sourceTitle": "Cursor - AI First Code Editor",
        "url": "https://www.cursor.com",
        "contentSnippet": "Cursor is the AI-first code editor...",
        "collectedAt": "2026-06-06T10:02:00",
        "reliability": "HIGH"
      }
    ],
    "reviewResult": {
      "taskId": "task_20260606_001",
      "passed": true,
      "score": 87,
      "summary": "报告质量良好，覆盖全面...",
      "issues": [],
      "nextAction": { "action": "finish", "targetAgent": null, "reason": "评分 87 >= 70" }
    }
  }
}
```

### 标准报告章节 (14节)

| 序号 | 章节标题 |
|------|----------|
| 1 | 执行摘要 |
| 2 | 分析背景 |
| 3 | 竞品概览（含汇总对比表格） |
| 4 | 产品定位对比 |
| 5 | 核心功能矩阵（含表格） |
| 6 | Agent 编程能力对比（含表格） |
| 7 | 代码库理解能力对比 |
| 8 | 模型与上下文能力对比 |
| 9 | 定价模式对比（含表格） |
| 10 | 用户评价与痛点 |
| 11 | SWOT 分析 |
| 12 | 产品机会点 |
| 13 | 结论与建议 |
| 14 | 信息来源 |

---

## 4. 获取证据池

**GET** `/tasks/{taskId}/evidence`

### 响应体 (List<Evidence>)

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "evidenceId": "ev_001",
      "productName": "Cursor",
      "sourceType": "OFFICIAL_SITE",
      "sourceTitle": "Cursor - AI First Code Editor",
      "url": "https://www.cursor.com",
      "contentSnippet": "Cursor is the AI-first code editor built on VS Code...",
      "collectedAt": "2026-06-06T10:02:15",
      "reliability": "HIGH",
      "usedForJson": ["sec_01", "sec_03"]
    }
  ]
}
```

### Evidence 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| evidenceId | String | 证据唯一 ID（全局唯一） |
| productName | String | 所属产品名称 |
| sourceType | Enum | 来源类型：OFFICIAL_SITE / PRICING_PAGE / DOCUMENTATION / BLOG / CHANGELOG / GITHUB / REVIEW_ARTICLE / COMMUNITY_DISCUSSION / NEWS / USER_COMMENT / UNKNOWN |
| sourceTitle | String | 来源标题 |
| url | String | 原始来源 URL（可点击跳转） |
| contentSnippet | String | 内容摘要 |
| collectedAt | LocalDateTime | 采集时间 |
| reliability | Enum | 可靠性：HIGH / MEDIUM / LOW |
| usedForJson | String | 被引用的报告章节 ID（JSON 格式） |

---

## 5. 获取质检结果

**GET** `/tasks/{taskId}/review`

### 响应体 (ReviewResultDTO)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "task_20260606_001",
    "passed": true,
    "score": 87,
    "summary": "报告整体质量良好，14个章节覆盖完整，证据链可追溯。竞品对比表格清晰直观...",
    "issues": [
      {
        "issueId": "issue_001",
        "severity": "minor",
        "type": "VAGUE_FINDING",
        "description": "用户评价部分缺少具体的用户反馈引用",
        "targetAgent": "WRITER_AGENT",
        "targetProduct": "Cursor",
        "targetDimension": "user_feedback",
        "repairInstruction": "补充具体的用户评论或社区讨论引用"
      }
    ],
    "nextAction": {
      "action": "finish",
      "targetAgent": null,
      "reason": "评分 87 >= 70，质量达标"
    }
  }
}
```

### ReviewIssue 类型枚举

| 值 | 说明 |
|---|------|
| MISSING_EVIDENCE | 缺少证据支撑 |
| EVIDENCE_NOT_LINKED | 证据未关联到报告 |
| SCHEMA_MISSING_FIELD | DTO 缺少必填字段 |
| COMPARISON_INCOMPLETE | 对比不完整 |
| VAGUE_FINDING | 结论过于模糊 |
| REPORT_MISSING_SECTION | 报告缺少标准章节 |
| CITATION_FORMAT_ERROR | 引用格式错误 |
| HALLUCINATION_RISK | 幻觉风险 |
| UNKNOWN_FIELD_TOO_MANY | 未知字段过多 |

### NextAction action 枚举

| 值 | 说明 |
|---|------|
| finish | 质量达标，结束 |
| repair | 需要修复，触发迭代 |
| human_review | 需要人工审查 |

---

## 6. 获取 Agent 执行轨迹

**GET** `/tasks/{taskId}/agent-runs`

### 响应体 (List<AgentRunResponse>)

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "runId": "run_001",
      "taskId": "task_20260606_001",
      "agentType": "PLANNER_AGENT",
      "inputType": "TaskInputDTO",
      "outputType": "TaskPlanDTO",
      "status": "SUCCESS",
      "startTime": "2026-06-06T10:00:05",
      "endTime": "2026-06-06T10:00:18",
      "durationMs": 13245,
      "errorMessage": null,
      "promptTokens": 320,
      "completionTokens": 580,
      "totalTokens": 900,
      "llmCalls": [
        {
          "systemPrompt": "You are PlannerAgent...",
          "userPrompt": "TaskInput: {...}",
          "response": "{ \"taskId\": \"task_20260606_001\", ... }",
          "promptTokens": 320,
          "completionTokens": 580,
          "durationMs": 12800
        }
      ]
    }
  ]
}
```

### AgentType 枚举

| 值 | 中文名称 | 说明 |
|---|----------|------|
| PLANNER_AGENT | 规划分析 | 任务规划、维度拆解、搜索策略 |
| COLLECTOR_AGENT | 信息采集 | 调用 Metaso API 搜索采集 |
| EXTRACTOR_AGENT | 要素提取 | 从证据中提取结构化 Claims |
| ANALYZER_AGENT | 深度分析 | 交叉验证、对比分析、SWOT |
| WRITER_AGENT | 报告撰写 | 生成 Markdown 报告 |
| REVIEWER_AGENT | 质量审查 | 100分制评分 + 修复指令 |

---

## 7. 获取修复 Diff 记录

**GET** `/tasks/{taskId}/repair-diffs`

### 响应体 (RepairDiffResponse)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "task_20260606_001",
    "repairDiffs": [
      {
        "taskId": "task_20260606_001",
        "iteration": 1,
        "targetAgent": "WRITER_AGENT",
        "beforeScore": 62,
        "afterScore": 87,
        "beforeIssueCount": 5,
        "afterIssueCount": 1,
        "fixedIssueCount": 4,
        "addedEvidenceIds": ["ev_045", "ev_046"],
        "addedClaimIds": ["claim_023"],
        "changedSectionTitles": ["用户评价与痛点", "SWOT 分析"],
        "changedProducts": ["Cursor"],
        "summary": "第 1 轮修复：补充了用户评价的具体引用，完善了 Cursor 的 SWOT 分析",
        "createdAt": "2026-06-06T10:15:30"
      }
    ]
  }
}
```

### RepairDiffDTO 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| iteration | Integer | 修复轮次编号 |
| targetAgent | String | 被修复的目标 Agent |
| beforeScore | Integer | 修复前评分 |
| afterScore | Integer | 修复后评分 |
| beforeIssueCount | Integer | 修复前问题数 |
| afterIssueCount | Integer | 修复后问题数 |
| fixedIssueCount | Integer | 本轮修复的问题数 |
| addedEvidenceIds | List<String> | 新增证据 ID |
| addedClaimIds | List<String> | 新增 Claim ID |
| changedSectionTitles | List<String> | 发生变化的报告章节标题 |
| changedProducts | List<String> | 涉及的产品名称 |
| summary | String | 修复摘要 |

---

## 附录：Postman Collection 使用说明

上述 7 个接口可通过 Postman 快速导入测试：

1. 新建 Collection，设置 Base URL 为 `http://localhost:8080/api`
2. 按顺序创建 7 个 Request（1 POST + 6 GET）
3. 创建任务后使用返回的 `taskId` 查询各接口
4. 注意：工作流为异步执行，创建任务后需等待一段时间再查询报告/证据等结果
