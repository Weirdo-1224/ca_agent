

# 竞品分析 Agent 系统领域 Schema 设计文档

版本：V1.0  
项目：CA_Agent  
主 Demo：AI 编程工具竞品分析  
技术栈：Spring Boot + Spring AI Alibaba + 多 Agent 工作流

---

## 1. 文档目标

本文档定义竞品分析 Agent 系统中的核心数据结构，包括：

1. 用户任务输入结构；
2. Agent 之间传递的 DTO；
3. AI 编程工具领域的产品画像 Schema；
4. 证据链与结论绑定结构；
5. 报告、质检、修复回退相关结构；
6. 工作流全局状态对象。

本系统的核心设计原则是：

> 所有 Agent 的输入输出必须结构化，所有事实性结论必须可追溯到证据来源。

---

## 2. Schema 设计原则

### 2.1 结构化优先

Agent 之间不能直接传递大段自然语言，而应传递结构化对象。

核心数据流为：

```text
TaskInputDTO
  ↓
TaskPlanDTO
  ↓
RawSourceSetDTO + Evidence
  ↓
ProductProfileSetDTO + Claim
  ↓
CompetitiveAnalysisDTO
  ↓
ReportDraftDTO
  ↓
ReviewResultDTO
  ↓
RepairInstructionDTO
````

---

### 2.2 证据驱动

所有事实性判断必须绑定 `evidenceIds`。

例如：

```json
{
  "statement": "Cursor 支持项目级代码问答能力",
  "evidenceIds": ["ev_cursor_001", "ev_cursor_002"]
}
```

不允许出现无证据支撑的具体价格、模型名称、排名、市场份额、企业客户案例等内容。

---

### 2.3 不确定信息显式表达

如果无法从公开资料中确认某项能力，应使用：

```text
unknown
```

如果只发现部分支持，应使用：

```text
partial
```

不允许模型凭常识补全。

---

### 2.4 领域可扩展

本版本以 AI 编程工具为主 Demo，但 Schema 应支持后续扩展到：

```text
协同办公
HR SaaS
知识库产品
低代码平台
企业采购系统
财务系统
```

因此，系统采用：

```text
通用 Schema + 领域 Schema
```

的设计方式。

---

## 3. 核心枚举规范

### 3.1 AgentType

| 枚举值             | code            | 含义          |
| --------------- | --------------- | ----------- |
| PLANNER_AGENT   | planner_agent   | 任务规划 Agent  |
| COLLECTOR_AGENT | collector_agent | 信息采集 Agent  |
| EXTRACTOR_AGENT | extractor_agent | 结构化抽取 Agent |
| ANALYZER_AGENT  | analyzer_agent  | 竞品分析 Agent  |
| WRITER_AGENT    | writer_agent    | 报告生成 Agent  |
| REVIEWER_AGENT  | reviewer_agent  | 质检 Agent    |

---

### 3.2 TaskStatus

| 枚举值                     | 含义     |
| ----------------------- | ------ |
| CREATED                 | 任务已创建  |
| PLANNING                | 规划中    |
| COLLECTING              | 信息采集中  |
| EXTRACTING              | 结构化抽取中 |
| ANALYZING               | 分析中    |
| WRITING                 | 报告生成中  |
| REVIEWING               | 质检中    |
| REPAIRING               | 修复回退中  |
| WAITING_HUMAN_REVIEW    | 等待人工介入 |
| COMPLETED               | 已完成    |
| COMPLETED_WITH_WARNINGS | 带警告完成  |
| FAILED                  | 任务失败   |

---

### 3.3 SourceType

| 枚举值                  | 含义        | 默认可靠性  |
| -------------------- | --------- | ------ |
| OFFICIAL_SITE        | 官方网站      | HIGH   |
| PRICING_PAGE         | 官方定价页     | HIGH   |
| DOCUMENTATION        | 官方文档      | HIGH   |
| BLOG                 | 官方或第三方博客  | MEDIUM |
| CHANGELOG            | 更新日志      | MEDIUM |
| GITHUB               | GitHub 页面 | MEDIUM |
| REVIEW_ARTICLE       | 测评文章      | MEDIUM |
| COMMUNITY_DISCUSSION | 社区讨论      | LOW    |
| NEWS                 | 新闻报道      | MEDIUM |
| USER_COMMENT         | 用户评论      | LOW    |
| UNKNOWN              | 未知来源      | LOW    |

---

### 3.4 ReliabilityLevel

| 枚举值    | 含义                   |
| ------ | -------------------- |
| HIGH   | 高可靠性，通常来自官方来源        |
| MEDIUM | 中等可靠性，通常来自测评、新闻、技术博客 |
| LOW    | 低可靠性，通常来自社区讨论或用户评论   |

---

### 3.5 ReviewIssueType

| 枚举值                    | 含义           | 默认回退 Agent                       |
| ---------------------- | ------------ | -------------------------------- |
| MISSING_EVIDENCE       | 缺少证据         | COLLECTOR_AGENT                  |
| EVIDENCE_NOT_LINKED    | 有证据但未绑定      | EXTRACTOR_AGENT / ANALYZER_AGENT |
| SCHEMA_MISSING_FIELD   | Schema 字段缺失  | EXTRACTOR_AGENT                  |
| COMPARISON_INCOMPLETE  | 对比矩阵不完整      | ANALYZER_AGENT                   |
| VAGUE_FINDING          | 分析结论过于空泛     | ANALYZER_AGENT                   |
| REPORT_MISSING_SECTION | 报告缺少章节       | WRITER_AGENT                     |
| CITATION_FORMAT_ERROR  | 引用格式错误       | WRITER_AGENT                     |
| HALLUCINATION_RISK     | 存在幻觉风险       | ANALYZER_AGENT / WRITER_AGENT    |
| UNKNOWN_FIELD_TOO_MANY | unknown 字段过多 | COLLECTOR_AGENT                  |

---

### 3.6 RepairType

| 枚举值                    | 含义        |
| ---------------------- | --------- |
| SUPPLEMENT_EVIDENCE    | 补充证据      |
| RELINK_EVIDENCE        | 重新绑定证据    |
| COMPLETE_SCHEMA        | 补全 Schema |
| COMPLETE_COMPARISON    | 补全对比分析    |
| REWRITE_ANALYSIS       | 重写分析结论    |
| REWRITE_REPORT         | 重写报告      |
| FIX_CITATION           | 修复引用      |
| REMOVE_OR_VERIFY_CLAIM | 删除或核验可疑结论 |

---

## 4. 通用 Schema

---

## 4.1 Evidence

### 作用

`Evidence` 是系统可信度的核心结构，用于记录公开资料来源。

所有核心结论都应通过 `evidenceIds` 追溯到 Evidence。

### 字段定义

| 字段             | 类型               | 必填 | 含义       |
| -------------- | ---------------- | -- | -------- |
| evidenceId     | String           | 是  | 证据唯一 ID  |
| productName    | String           | 是  | 对应竞品名称   |
| sourceType     | SourceType       | 是  | 来源类型     |
| sourceTitle    | String           | 是  | 来源标题     |
| url            | String           | 是  | 原始链接     |
| contentSnippet | String           | 是  | 关键证据片段   |
| collectedAt    | LocalDateTime    | 是  | 采集时间     |
| reliability    | ReliabilityLevel | 是  | 可靠性等级    |
| usedFor        | List<String>     | 是  | 可支撑的分析维度 |

### 示例

```json
{
  "evidenceId": "ev_cursor_001",
  "productName": "Cursor",
  "sourceType": "OFFICIAL_SITE",
  "sourceTitle": "Cursor Official Website",
  "url": "https://example.com/cursor/official",
  "contentSnippet": "Cursor provides AI-assisted coding features for developers.",
  "collectedAt": "2026-06-04T10:00:00",
  "reliability": "HIGH",
  "usedFor": ["positioning", "core_capabilities"]
}
```

---

## 4.2 Claim

### 作用

`Claim` 表示一个结构化结论。
每个 Claim 必须绑定证据。

### 字段定义

| 字段          | 类型           | 必填 | 含义                       |
| ----------- | ------------ | -- | ------------------------ |
| claimId     | String       | 是  | 结论 ID                    |
| productName | String       | 是  | 对应产品                     |
| dimension   | String       | 是  | 所属分析维度                   |
| statement   | String       | 是  | 结论内容                     |
| confidence  | Double       | 是  | 置信度，0 到 1                |
| evidenceIds | List<String> | 是  | 支撑证据                     |
| riskLevel   | String       | 是  | 风险等级：low / medium / high |

### 示例

```json
{
  "claimId": "claim_cursor_001",
  "productName": "Cursor",
  "dimension": "agent_capabilities",
  "statement": "Cursor 具备面向项目级任务的多文件编辑能力。",
  "confidence": 0.86,
  "evidenceIds": ["ev_cursor_001", "ev_cursor_002"],
  "riskLevel": "low"
}
```

---

## 4.3 CapabilityItem

### 作用

用于描述“核心功能”类能力，例如代码补全、代码生成、Debug 辅助等。

### 字段定义

| 字段          | 类型           | 必填 | 含义                               |
| ----------- | ------------ | -- | -------------------------------- |
| supported   | String       | 是  | true / false / partial / unknown |
| maturity    | String       | 是  | high / medium / low / unknown    |
| description | String       | 是  | 能力说明                             |
| evidenceIds | List<String> | 是  | 证据 ID                            |

### 示例

```json
{
  "supported": "true",
  "maturity": "high",
  "description": "支持基于上下文的代码生成。",
  "evidenceIds": ["ev_cursor_003"]
}
```

---

## 4.4 SupportItem

### 作用

用于描述“是否支持某能力”，例如终端执行、SSO、私有化部署等。

### 字段定义

| 字段          | 类型           | 必填 | 含义                               |
| ----------- | ------------ | -- | -------------------------------- |
| supported   | String       | 是  | true / false / partial / unknown |
| description | String       | 是  | 支持情况说明                           |
| evidenceIds | List<String> | 是  | 证据 ID                            |

### 示例

```json
{
  "supported": "partial",
  "description": "公开资料显示支持一定程度的多文件修改，但未确认完整自动执行闭环。",
  "evidenceIds": ["ev_cursor_004"]
}
```

---

## 5. 用户任务输入 Schema

---

## 5.1 TaskCreateRequest

### 作用

前端创建竞品分析任务时提交的请求对象。

### 字段定义

| 字段             | 类型           | 必填 | 含义               |
| -------------- | ------------ | -- | ---------------- |
| taskName       | String       | 是  | 任务名称             |
| domain         | String       | 是  | 分析领域             |
| targetProducts | List<String> | 是  | 待分析竞品            |
| analysisGoal   | String       | 是  | 分析目标             |
| outputFormat   | String       | 否  | 输出格式，例如 markdown |
| language       | String       | 否  | 输出语言，例如 zh-CN    |
| maxIterations  | Integer      | 否  | 最大自动修复次数         |

### 示例

```json
{
  "taskName": "AI 编程工具竞品分析",
  "domain": "AI_CODING_TOOLS",
  "targetProducts": ["Cursor", "Windsurf", "GitHub Copilot", "通义灵码"],
  "analysisGoal": "生成面向产品团队的 AI 编程工具竞品分析报告",
  "outputFormat": "markdown",
  "language": "zh-CN",
  "maxIterations": 2
}
```

---

## 5.2 TaskInputDTO

### 作用

系统内部使用的任务输入对象，在 `TaskCreateRequest` 基础上补充 `taskId`。

### 字段定义

| 字段             | 类型           | 必填 | 含义       |
| -------------- | ------------ | -- | -------- |
| taskId         | String       | 是  | 任务 ID    |
| taskName       | String       | 是  | 任务名称     |
| domain         | String       | 是  | 分析领域     |
| targetProducts | List<String> | 是  | 竞品列表     |
| analysisGoal   | String       | 是  | 分析目标     |
| outputFormat   | String       | 否  | 输出格式     |
| language       | String       | 否  | 输出语言     |
| maxIterations  | Integer      | 否  | 最大自动修复次数 |

---

## 6. Planner 输出 Schema

---

## 6.1 TaskPlanDTO

### 作用

Planner Agent 的输出。
用于描述后续 Agent 的执行计划。

### 字段定义

| 字段                 | 类型                   | 必填 | 含义         |
| ------------------ | -------------------- | -- | ---------- |
| taskId             | String               | 是  | 任务 ID      |
| detectedDomain     | String               | 是  | 识别出的领域     |
| templateId         | String               | 是  | 匹配的领域模板 ID |
| confidence         | Double               | 是  | 领域识别置信度    |
| products           | List<String>         | 是  | 竞品列表       |
| analysisGoal       | String               | 是  | 分析目标       |
| analysisDimensions | List<String>         | 是  | 分析维度       |
| collectionTasks    | List<CollectionTask> | 是  | 采集任务列表     |
| workflow           | List<String>         | 是  | 工作流节点顺序    |

---

## 6.2 CollectionTask

### 字段定义

| 字段                   | 类型               | 必填 | 含义       |
| -------------------- | ---------------- | -- | -------- |
| productName          | String           | 是  | 产品名称     |
| queries              | List<String>     | 是  | 搜索 Query |
| targetDimensions     | List<String>     | 是  | 目标分析维度   |
| preferredSourceTypes | List<SourceType> | 是  | 优先来源类型   |

### 示例

```json
{
  "productName": "Cursor",
  "queries": [
    "Cursor official AI coding tool",
    "Cursor pricing",
    "Cursor documentation agent coding",
    "Cursor codebase understanding"
  ],
  "targetDimensions": [
    "positioning",
    "pricing",
    "agent_capabilities",
    "codebase_understanding"
  ],
  "preferredSourceTypes": [
    "OFFICIAL_SITE",
    "PRICING_PAGE",
    "DOCUMENTATION"
  ]
}
```

---

## 7. Collector 输出 Schema

---

## 7.1 RawSourceSetDTO

### 作用

Collector Agent 的输出，包含原始资料、证据池和缺失来源说明。

### 字段定义

| 字段             | 类型                  | 必填 | 含义     |
| -------------- | ------------------- | -- | ------ |
| taskId         | String              | 是  | 任务 ID  |
| rawSources     | List<RawSource>     | 是  | 原始资料列表 |
| evidencePool   | List<Evidence>      | 是  | 证据池    |
| missingSources | List<MissingSource> | 否  | 缺失来源记录 |

---

## 7.2 RawSource

| 字段               | 类型               | 必填 | 含义      |
| ---------------- | ---------------- | -- | ------- |
| sourceId         | String           | 是  | 原始资料 ID |
| productName      | String           | 是  | 产品名称    |
| sourceType       | SourceType       | 是  | 来源类型    |
| title            | String           | 是  | 网页标题    |
| url              | String           | 是  | 网页链接    |
| rawText          | String           | 是  | 网页正文文本  |
| contentSnippet   | String           | 是  | 关键片段    |
| collectedAt      | LocalDateTime    | 是  | 采集时间    |
| reliability      | ReliabilityLevel | 是  | 来源可靠性   |
| targetDimensions | List<String>     | 是  | 对应分析维度  |

---

## 7.3 MissingSource

| 字段              | 类型     | 必填 | 含义           |
| --------------- | ------ | -- | ------------ |
| productName     | String | 是  | 产品名称         |
| targetDimension | String | 是  | 缺失维度         |
| reason          | String | 是  | 缺失原因         |
| suggestedQuery  | String | 否  | 建议补充搜索 Query |

---

## 8. Extractor 输出 Schema

---

## 8.1 ProductProfileSetDTO

### 作用

Extractor Agent 的输出，表示多个竞品的结构化产品画像。

### 字段定义

| 字段       | 类型                   | 必填 | 含义     |
| -------- | -------------------- | -- | ------ |
| taskId   | String               | 是  | 任务 ID  |
| products | List<ProductProfile> | 是  | 产品画像列表 |

---

## 8.2 ProductProfile

### 作用

单个 AI 编程工具的完整产品画像。

| 字段                    | 类型                    | 必填 | 含义         |
| --------------------- | --------------------- | -- | ---------- |
| productName           | String                | 是  | 产品名称       |
| company               | String                | 否  | 所属公司       |
| officialUrl           | String                | 否  | 官网地址       |
| productType           | String                | 是  | 产品类型       |
| positioning           | Positioning           | 是  | 产品定位       |
| targetUsers           | List<TargetUser>      | 是  | 目标用户       |
| coreCapabilities      | CoreCapabilities      | 是  | 核心功能       |
| agentCapabilities     | AgentCapabilities     | 是  | Agent 编程能力 |
| codebaseUnderstanding | CodebaseUnderstanding | 是  | 代码库理解能力    |
| ideEcosystem          | IdeEcosystem          | 是  | IDE 与生态集成  |
| modelContext          | ModelContext          | 是  | 模型与上下文能力   |
| pricing               | Pricing               | 是  | 定价模式       |
| enterpriseFeatures    | EnterpriseFeatures    | 是  | 企业版能力      |
| userFeedback          | UserFeedback          | 是  | 用户评价       |
| claims                | List<Claim>           | 是  | 结构化结论      |
| missingFields         | List<MissingField>    | 否  | 缺失字段       |

---

## 8.3 Positioning

| 字段              | 类型           | 必填 | 含义     |
| --------------- | ------------ | -- | ------ |
| summary         | String       | 是  | 产品定位摘要 |
| mainScenarios   | List<String> | 是  | 主要使用场景 |
| differentiation | String       | 否  | 差异化描述  |
| evidenceIds     | List<String> | 是  | 证据 ID  |

---

## 8.4 TargetUser

| 字段          | 类型           | 必填 | 含义    |
| ----------- | ------------ | -- | ----- |
| userGroup   | String       | 是  | 用户群体  |
| useCases    | List<String> | 是  | 使用场景  |
| painPoints  | List<String> | 是  | 用户痛点  |
| evidenceIds | List<String> | 是  | 证据 ID |

---

## 8.5 CoreCapabilities

| 字段                      | 类型             | 含义       |
| ----------------------- | -------------- | -------- |
| codeCompletion          | CapabilityItem | 代码补全     |
| codeGeneration          | CapabilityItem | 代码生成     |
| codeExplanation         | CapabilityItem | 代码解释     |
| refactoring             | CapabilityItem | 代码重构     |
| unitTestGeneration      | CapabilityItem | 单元测试生成   |
| debugAssistance         | CapabilityItem | Debug 辅助 |
| documentationGeneration | CapabilityItem | 文档生成     |

---

## 8.6 AgentCapabilities

| 字段                | 类型          | 含义      |
| ----------------- | ----------- | ------- |
| taskPlanning      | SupportItem | 任务规划    |
| multiFileEditing  | SupportItem | 多文件编辑   |
| terminalExecution | SupportItem | 终端执行    |
| testRunAndFix     | SupportItem | 测试运行与修复 |
| codeReview        | SupportItem | 代码审查    |
| autonomousLoop    | SupportItem | 自主循环执行  |

---

## 8.7 CodebaseUnderstanding

| 字段                 | 类型          | 含义     |
| ------------------ | ----------- | ------ |
| repositoryIndexing | SupportItem | 代码库索引  |
| crossFileReference | SupportItem | 跨文件理解  |
| projectQa          | SupportItem | 项目级问答  |
| longContextSupport | SupportItem | 长上下文支持 |

---

## 8.8 IdeEcosystem

| 字段            | 类型                 | 含义      |
| ------------- | ------------------ | ------- |
| supportedIdes | List<SupportedIde> | 支持的 IDE |
| platforms     | List<String>       | 支持平台    |
| integrations  | List<Integration>  | 生态集成    |

### SupportedIde

| 字段          | 类型           | 含义                               |
| ----------- | ------------ | -------------------------------- |
| name        | String       | IDE 名称                           |
| supportType | String       | native / plugin / fork / unknown |
| evidenceIds | List<String> | 证据 ID                            |

### Integration

| 字段          | 类型           | 含义    |
| ----------- | ------------ | ----- |
| name        | String       | 集成对象  |
| description | String       | 集成说明  |
| evidenceIds | List<String> | 证据 ID |

---

## 8.9 ModelContext

| 字段                | 类型                   | 含义         |
| ----------------- | -------------------- | ---------- |
| supportedModels   | List<SupportedModel> | 支持模型       |
| bringYourOwnKey   | SupportItem          | 是否支持自带 Key |
| localModelSupport | SupportItem          | 是否支持本地模型   |
| contextWindow     | ContextWindow        | 上下文窗口      |

### SupportedModel

| 字段          | 类型           | 含义    |
| ----------- | ------------ | ----- |
| modelName   | String       | 模型名称  |
| provider    | String       | 模型提供方 |
| evidenceIds | List<String> | 证据 ID |

### ContextWindow

| 字段          | 类型           | 含义                  |
| ----------- | ------------ | ------------------- |
| value       | String       | 上下文窗口大小，未知则 unknown |
| description | String       | 描述                  |
| evidenceIds | List<String> | 证据 ID               |

---

## 8.10 Pricing

| 字段             | 类型             | 含义                     |
| -------------- | -------------- | ---------------------- |
| hasFreePlan    | String         | true / false / unknown |
| plans          | List<Plan>     | 套餐列表                   |
| enterprisePlan | EnterprisePlan | 企业版套餐                  |

### Plan

| 字段           | 类型           | 含义                                       |
| ------------ | ------------ | ---------------------------------------- |
| planName     | String       | 套餐名称                                     |
| price        | String       | 价格                                       |
| billingCycle | String       | monthly / yearly / usage_based / unknown |
| targetUser   | String       | 目标用户                                     |
| mainLimits   | List<String> | 主要限制                                     |
| evidenceIds  | List<String> | 证据 ID                                    |

### EnterprisePlan

| 字段          | 类型           | 含义                                            |
| ----------- | ------------ | --------------------------------------------- |
| available   | String       | true / false / unknown                        |
| pricingType | String       | fixed / usage_based / contact_sales / unknown |
| features    | List<String> | 企业版功能                                         |
| evidenceIds | List<String> | 证据 ID                                         |

---

## 8.11 EnterpriseFeatures

| 字段                | 类型          | 含义    |
| ----------------- | ----------- | ----- |
| sso               | SupportItem | 单点登录  |
| adminConsole      | SupportItem | 管理后台  |
| privacyControl    | SupportItem | 隐私控制  |
| auditLog          | SupportItem | 审计日志  |
| privateDeployment | SupportItem | 私有化部署 |

---

## 8.12 UserFeedback

| 字段               | 类型                  | 含义   |
| ---------------- | ------------------- | ---- |
| positivePoints   | List<FeedbackPoint> | 正向评价 |
| negativePoints   | List<FeedbackPoint> | 负向评价 |
| commonPainPoints | List<PainPoint>     | 常见痛点 |

### FeedbackPoint

| 字段          | 类型           | 含义                            |
| ----------- | ------------ | ----------------------------- |
| point       | String       | 评价点                           |
| frequency   | String       | high / medium / low / unknown |
| evidenceIds | List<String> | 证据 ID                         |

### PainPoint

| 字段            | 类型           | 含义    |
| ------------- | ------------ | ----- |
| painPoint     | String       | 痛点    |
| affectedUsers | List<String> | 影响用户  |
| evidenceIds   | List<String> | 证据 ID |

---

## 8.13 MissingField

| 字段     | 类型     | 含义     |
| ------ | ------ | ------ |
| fieldPath | String | 缺失字段路径 |
| reason    | String | 缺失原因   |

示例：

```json
{
  "fieldPath": "enterpriseFeatures.auditLog",
  "reason": "未找到明确公开证据"
}
```

---

## 9. Analyzer 输出 Schema

---

## 9.1 CompetitiveAnalysisDTO

### 作用

Analyzer Agent 的输出，用于表示横向竞品分析结果。

| 字段                   | 类型                         | 必填 | 含义      |
| -------------------- | -------------------------- | -- | ------- |
| taskId               | String                     | 是  | 任务 ID   |
| comparisonMatrix     | List<ComparisonMatrixItem> | 是  | 对比矩阵    |
| keyFindings          | List<KeyFinding>           | 是  | 关键发现    |
| productOpportunities | List<ProductOpportunity>   | 是  | 产品机会点   |
| risks                | List<Risk>                 | 是  | 风险提示    |
| swotSummary          | List<SwotSummary>          | 是  | SWOT 总结 |

---

## 9.2 ComparisonMatrixItem

| 字段           | 类型                          | 含义     |
| ------------ | --------------------------- | ------ |
| dimension    | String                      | 一级维度   |
| subDimension | String                      | 二级维度   |
| items        | List<ComparisonProductItem> | 各产品对比项 |

### ComparisonProductItem

| 字段           | 类型           | 含义                                   |
| ------------ | ------------ | ------------------------------------ |
| productName  | String       | 产品名称                                 |
| supportLevel | String       | high / medium / low / none / unknown |
| summary      | String       | 简要说明                                 |
| evidenceIds  | List<String> | 证据 ID                                |

---

## 9.3 KeyFinding

| 字段              | 类型           | 含义    |
| --------------- | ------------ | ----- |
| findingId       | String       | 发现 ID |
| title           | String       | 标题    |
| description     | String       | 描述    |
| relatedProducts | List<String> | 相关产品  |
| evidenceIds     | List<String> | 证据 ID |
| confidence      | Double       | 置信度   |

---

## 9.4 ProductOpportunity

| 字段                   | 类型           | 含义                  |
| -------------------- | ------------ | ------------------- |
| opportunityId        | String       | 机会点 ID              |
| title                | String       | 标题                  |
| description          | String       | 描述                  |
| targetUsers          | List<String> | 目标用户                |
| requiredCapabilities | List<String> | 所需能力                |
| priority             | String       | high / medium / low |
| evidenceIds          | List<String> | 证据 ID               |

---

## 9.5 Risk

| 字段          | 类型           | 含义                  |
| ----------- | ------------ | ------------------- |
| riskId      | String       | 风险 ID               |
| title       | String       | 标题                  |
| description | String       | 描述                  |
| severity    | String       | high / medium / low |
| evidenceIds | List<String> | 证据 ID               |

---

## 9.6 SwotSummary

| 字段            | 类型             | 含义   |
| ------------- | -------------- | ---- |
| productName   | String         | 产品名称 |
| strengths     | List<SwotItem> | 优势   |
| weaknesses    | List<SwotItem> | 劣势   |
| opportunities | List<SwotItem> | 机会   |
| threats       | List<SwotItem> | 威胁   |

### SwotItem

| 字段          | 类型           | 含义    |
| ----------- | ------------ | ----- |
| point       | String       | 观点    |
| explanation | String       | 解释    |
| evidenceIds | List<String> | 证据 ID |

---

## 10. Writer 输出 Schema

---

## 10.1 ReportDraftDTO

### 作用

Writer Agent 的输出，用于表示 Markdown 报告草稿。

| 字段           | 类型                  | 必填 | 含义    |
| ------------ | ------------------- | -- | ----- |
| taskId       | String              | 是  | 任务 ID |
| reportTitle  | String              | 是  | 报告标题  |
| reportFormat | String              | 是  | 报告格式  |
| sections     | List<ReportSection> | 是  | 报告章节  |
| sourceList   | List<Evidence>      | 是  | 来源列表  |

---

## 10.2 ReportSection

| 字段              | 类型           | 含义          |
| --------------- | ------------ | ----------- |
| sectionId       | String       | 章节 ID       |
| title           | String       | 章节标题        |
| content         | String       | Markdown 内容 |
| relatedClaimIds | List<String> | 关联结论        |
| evidenceIds     | List<String> | 关联证据        |

---

## 10.3 报告必需章节

ReportDraft 必须包含以下章节：

```text
1. 执行摘要
2. 分析背景
3. 竞品概览
4. 产品定位对比
5. 核心功能矩阵
6. Agent 编程能力对比
7. 代码库理解能力对比
8. 模型与上下文能力对比
9. 定价模式对比
10. 用户评价与痛点
11. SWOT 分析
12. 产品机会点
13. 结论与建议
14. 信息来源
```

---

## 11. Reviewer 输出 Schema

---

## 11.1 ReviewResultDTO

### 作用

Reviewer Agent 的输出，用于判断报告是否通过质检，并决定下一步动作。

| 字段         | 类型                | 必填 | 含义    |
| ---------- | ----------------- | -- | ----- |
| taskId     | String            | 是  | 任务 ID |
| passed     | Boolean           | 是  | 是否通过  |
| score      | Integer           | 是  | 质检得分  |
| summary    | String            | 是  | 质检摘要  |
| issues     | List<ReviewIssue> | 是  | 问题列表  |
| nextAction | NextAction        | 是  | 下一步动作 |

---

## 11.2 ReviewIssue

| 字段                | 类型              | 含义                  |
| ----------------- | --------------- | ------------------- |
| issueId           | String          | 问题 ID               |
| severity          | String          | high / medium / low |
| type              | ReviewIssueType | 问题类型                |
| description       | String          | 问题描述                |
| targetAgent       | AgentType       | 建议回退 Agent          |
| targetProduct     | String          | 目标产品                |
| targetDimension   | String          | 目标维度                |
| repairInstruction | String          | 修复建议                |

---

## 11.3 NextAction

| 字段          | 类型        | 含义                             |
| ----------- | --------- | ------------------------------ |
| action      | String    | finish / repair / human_review |
| targetAgent | AgentType | 目标 Agent                       |
| reason      | String    | 原因                             |

---

## 11.4 通过条件

Reviewer 通过必须同时满足：

```text
1. score >= 85
2. 不存在 high severity issue
3. 报告核心章节完整
4. 核心结论均绑定 evidenceIds
5. 定价、产品定位、Agent 编程能力等关键维度具备证据支撑
```

---

## 12. RepairInstructionDTO

### 作用

表示一次自动修复回退指令。

| 字段              | 类型           | 必填 | 含义                          |
| --------------- | ------------ | -- | --------------------------- |
| taskId          | String       | 是  | 任务 ID                       |
| repairId        | String       | 是  | 修复 ID                       |
| fromAgent       | AgentType    | 是  | 来源 Agent，通常为 REVIEWER_AGENT |
| targetAgent     | AgentType    | 是  | 回退目标 Agent                  |
| issueIds        | List<String> | 是  | 对应问题 ID                     |
| repairType      | RepairType   | 是  | 修复类型                        |
| targetProduct   | String       | 否  | 目标产品                        |
| targetDimension | String       | 否  | 目标维度                        |
| instruction     | String       | 是  | 修复指令                        |
| priority        | String       | 是  | high / medium / low         |

---

## 13. Workflow State Schema

---

## 13.1 CompetitiveAnalysisState

### 作用

`CompetitiveAnalysisState` 是整个工作流的全局状态对象。
每个 Agent 从 State 中读取输入，并将输出写回 State。

### 字段定义

| 字段                  | 类型                         | 含义           |
| ------------------- | -------------------------- | ------------ |
| taskInput           | TaskInputDTO               | 用户任务输入       |
| taskPlan            | TaskPlanDTO                | Planner 输出   |
| rawSourceSet        | RawSourceSetDTO            | Collector 输出 |
| productProfileSet   | ProductProfileSetDTO       | Extractor 输出 |
| competitiveAnalysis | CompetitiveAnalysisDTO     | Analyzer 输出  |
| reportDraft         | ReportDraftDTO             | Writer 输出    |
| reviewResult        | ReviewResultDTO            | Reviewer 输出  |
| repairInstructions  | List<RepairInstructionDTO> | 修复指令列表       |
| iterationCount      | Integer                    | 当前修复轮次       |
| status              | TaskStatus                 | 当前任务状态       |

---

## 13.2 State 流转规则

```text
CREATED
  ↓
PLANNING
  ↓
COLLECTING
  ↓
EXTRACTING
  ↓
ANALYZING
  ↓
WRITING
  ↓
REVIEWING
  ├── COMPLETED
  ├── REPAIRING
  └── WAITING_HUMAN_REVIEW
```

---

## 13.3 修复轮次规则

默认：

```text
maxIterations = 2
```

规则：

```text
1. iterationCount 初始为 0。
2. Reviewer 第一次不通过后，iterationCount + 1。
3. 如果 iterationCount < maxIterations，可以自动回退。
4. 如果 iterationCount >= maxIterations，进入 WAITING_HUMAN_REVIEW。
```

---

## 14. Agent 输入输出关系

| Agent          | 输入                                                       | 输出                           |
| -------------- | -------------------------------------------------------- | ---------------------------- |
| PlannerAgent   | TaskInputDTO                                             | TaskPlanDTO                  |
| CollectorAgent | TaskPlanDTO / RepairInstructionDTO                       | RawSourceSetDTO + Evidence   |
| ExtractorAgent | RawSourceSetDTO + Evidence                               | ProductProfileSetDTO + Claim |
| AnalyzerAgent  | ProductProfileSetDTO + Evidence                          | CompetitiveAnalysisDTO       |
| WriterAgent    | ProductProfileSetDTO + CompetitiveAnalysisDTO + Evidence | ReportDraftDTO               |
| ReviewerAgent  | 全局 State                                                 | ReviewResultDTO              |
| RepairRouter   | ReviewResultDTO                                          | RepairInstructionDTO         |

---

## 15. 核心质量约束

### 15.1 Evidence 约束

```text
1. Evidence 必须包含 url。
2. Evidence 必须包含 contentSnippet。
3. Evidence 必须包含 reliability。
4. Evidence.usedFor 必须指明支撑维度。
```

---

### 15.2 Claim 约束

```text
1. Claim 必须绑定至少一个 evidenceId。
2. Claim.confidence 范围为 0 到 1。
3. Claim.statement 不允许包含无证据的绝对化表达。
```

---

### 15.3 ProductProfile 约束

```text
1. 每个产品必须有 productName。
2. 每个产品必须有 positioning。
3. 每个产品必须有 coreCapabilities。
4. 每个产品必须有 agentCapabilities。
5. 每个产品必须有 pricing。
6. 无法确认字段必须使用 unknown，并写入 missingFields。
```

---

### 15.4 ReportDraft 约束

```text
1. 必须包含 14 个标准章节。
2. 每个核心章节必须绑定 evidenceIds。
3. 信息来源章节必须列出 sourceList。
4. 报告不得新增未出现在结构化结果中的事实。
```

---

### 15.5 ReviewResult 约束

```text
1. high severity issue 存在时，passed 必须为 false。
2. passed 为 true 时，nextAction.action 必须为 finish。
3. passed 为 false 时，nextAction.action 应为 repair 或 human_review。
4. issue.targetAgent 必须能映射到可回退 Agent。
```

---

## 16. 示例：完整最小数据流

### 16.1 TaskInputDTO

```json
{
  "taskId": "task_mock_001",
  "taskName": "AI 编程工具竞品分析",
  "domain": "AI_CODING_TOOLS",
  "targetProducts": ["Cursor", "Windsurf", "GitHub Copilot", "通义灵码"],
  "analysisGoal": "生成面向产品团队的 AI 编程工具竞品分析报告",
  "outputFormat": "markdown",
  "language": "zh-CN",
  "maxIterations": 2
}
```

---

### 16.2 ReviewResultDTO：不通过示例

```json
{
  "taskId": "task_mock_001",
  "passed": false,
  "score": 78,
  "summary": "报告结构基本完整，但存在定价证据不足问题。",
  "issues": [
    {
      "issueId": "issue_001",
      "severity": "high",
      "type": "MISSING_EVIDENCE",
      "description": "通义灵码的定价模式缺少官方来源。",
      "targetAgent": "COLLECTOR_AGENT",
      "targetProduct": "通义灵码",
      "targetDimension": "pricing",
      "repairInstruction": "补充通义灵码官方定价页或官方文档中的定价信息。"
    }
  ],
  "nextAction": {
    "action": "repair",
    "targetAgent": "COLLECTOR_AGENT",
    "reason": "存在高优先级缺失证据问题"
  }
}
```

---

### 16.3 RepairInstructionDTO

```json
{
  "taskId": "task_mock_001",
  "repairId": "repair_001",
  "fromAgent": "REVIEWER_AGENT",
  "targetAgent": "COLLECTOR_AGENT",
  "issueIds": ["issue_001"],
  "repairType": "SUPPLEMENT_EVIDENCE",
  "targetProduct": "通义灵码",
  "targetDimension": "pricing",
  "instruction": "补充通义灵码官方定价页或官方文档中的定价信息。",
  "priority": "high"
}
```

---

## 17. 后续扩展方向

后续版本可以增加：

```text
1. DomainTemplate 的数据库持久化；
2. 不同领域模板的 Schema 配置；
3. Evidence 的全文快照；
4. Claim 与 ReportSection 的多对多关系；
5. 结构化输出 JSON Schema 校验；
6. 人工修正字段的版本记录；
7. 证据可信度自动评分；
8. AgentRun Trace 详细字段；
9. 多模型输出一致性检查；
10. 报告版本管理。
```

---

## 18. 当前版本边界

本 V1 文档只定义数据结构，不包含：

```text
1. Controller API 设计；
2. 数据库表结构；
3. Spring AI Alibaba 调用方式；
4. Agent Prompt 具体内容；
5. 前端页面结构；
6. 真实搜索工具实现；
7. 文件导出逻辑。
```

这些内容分别在后续文档中定义：

```text
04_agent_workflow.md
05_agent_prompt_spec.md
06_api_spec.md
07_database_spec.md
08_frontend_spec.md
```


