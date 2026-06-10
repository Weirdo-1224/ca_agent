

# 竞品分析 Agent 系统开发任务拆分文档

版本：V1.0  
项目：CA_Agent  
主 Demo：AI 编程工具竞品分析  
技术栈：Spring Boot + Spring AI Alibaba + MySQL + 前端可视化

---

## 1. 文档目标

本文档用于指导项目的分阶段开发，服务于 Spec Coding 工作流。

本项目不采用一次性提示词堆代码的方式，而是采用：

```text
需求文档
↓
Schema 文档
↓
工作流文档
↓
Prompt 文档
↓
开发任务文档
↓
分任务实现
↓
代码回查与文档同步
````

本文档定义每个开发任务的：

```text
1. 任务目标
2. 涉及文件
3. 实现内容
4. 禁止修改范围
5. 验收标准
6. 后续依赖关系
```

---

## 2. 当前项目结构

当前后端项目结构如下：

```text
org.example.ca_agent
├── CaAgentApplication.java
│
├── agent
│   ├── AnalyzerAgent.java
│   ├── CollectorAgent.java
│   ├── ExtractorAgent.java
│   ├── PlannerAgent.java
│   ├── ReviewerAgent.java
│   └── WriterAgent.java
│
├── common
│   ├── BizException.java
│   ├── JsonUtils.java
│   └── Result.java
│
├── config
│   ├── ModelConfig.java
│   ├── PromptTemplateConfig.java
│   ├── SearchToolConfig.java
│   └── SpringAiAlibabaConfig.java
│
├── controller
│   └── AnalysisTaskController.java
│
├── dto
│   ├── agent
│   │   ├── CompetitiveAnalysisDTO.java
│   │   ├── ProductProfileSetDTO.java
│   │   ├── RawSourceSetDTO.java
│   │   ├── RepairInstructionDTO.java
│   │   ├── ReportDraftDTO.java
│   │   ├── ReviewResultDTO.java
│   │   ├── TaskInputDTO.java
│   │   └── TaskPlanDTO.java
│   ├── request
│   │   └── TaskCreateRequest.java
│   └── response
│       ├── AgentRunResponse.java
│       ├── ReportResponse.java
│       └── TaskDetailResponse.java
│
├── entity
│   ├── AgentRunEntity.java
│   ├── AnalysisTaskEntity.java
│   ├── ClaimEntity.java
│   ├── EvidenceEntity.java
│   ├── ReportEntity.java
│   └── ReviewIssueEntity.java
│
├── enums
│   ├── AgentType.java
│   ├── ReliabilityLevel.java
│   ├── RepairType.java
│   ├── ReviewIssueType.java
│   ├── SourceType.java
│   └── TaskStatus.java
│
├── prompt
│   ├── AnalyzerPrompt.java
│   ├── CollectorPrompt.java
│   ├── ExtractorPrompt.java
│   ├── PlannerPrompt.java
│   ├── ReviewerPrompt.java
│   └── WriterPrompt.java
│
├── repository
│   ├── AgentRunRepository.java
│   ├── ClaimRepository.java
│   ├── EvidenceRepository.java
│   ├── ReportRepository.java
│   ├── ReviewIssueRepository.java
│   └── TaskRepository.java
│
├── schema
│   ├── CapabilityItem.java
│   ├── Claim.java
│   ├── DomainTemplate.java
│   ├── Evidence.java
│   └── SupportItem.java
│
├── service
│   ├── AgentRunService.java
│   ├── EvidenceService.java
│   ├── ReportService.java
│   ├── TaskService.java
│   └── WorkflowService.java
│
├── tool
│   ├── EvidenceStoreTool.java
│   ├── SourceRankTool.java
│   ├── WebPageReaderTool.java
│   └── WebSearchTool.java
│
└── workflow
    ├── CompetitiveAnalysisGraph.java
    ├── CompetitiveAnalysisState.java
    ├── RepairRouter.java
    └── WorkflowRouter.java
```

---

## 3. 开发总路线

项目开发分为 10 个阶段。

```text
Task 01：补全 DTO / Schema / State / Enum / Common
Task 02：实现 AgentNode、Mock Agent、WorkflowRouter、RepairRouter
Task 03：实现 WorkflowService、TaskService、Controller Mock 接口
Task 04：实现 Entity、Repository、数据库持久化
Task 05：实现 AgentRun Trace 日志记录
Task 06：接入 Spring AI Alibaba ChatClient
Task 07：实现真实 Prompt Agent 调用
Task 08：实现 WebSearchTool / WebPageReaderTool / SourceRankTool
Task 09：实现前端页面与接口联调
Task 10：准备 Demo 数据、演示脚本和比赛材料
```

推荐开发顺序：

```text
先 Mock 闭环
再接口暴露
再数据库持久化
再接大模型
再接搜索工具
最后做前端和答辩包装
```

---

# Task 01：补全 DTO / Schema / State / Enum / Common

## 1.1 任务目标

完成系统基础数据协议层，为后续 Agent 工作流提供稳定数据结构。

本任务不实现业务逻辑，只补全数据结构。

---

## 1.2 涉及文件

```text
common
├── Result.java
├── BizException.java
└── JsonUtils.java

enums
├── AgentType.java
├── TaskStatus.java
├── SourceType.java
├── ReliabilityLevel.java
├── ReviewIssueType.java
└── RepairType.java

schema
├── DomainTemplate.java
├── Evidence.java
├── Claim.java
├── CapabilityItem.java
└── SupportItem.java

dto/request
└── TaskCreateRequest.java

dto/response
├── TaskDetailResponse.java
├── ReportResponse.java
└── AgentRunResponse.java

dto/agent
├── TaskInputDTO.java
├── TaskPlanDTO.java
├── RawSourceSetDTO.java
├── ProductProfileSetDTO.java
├── CompetitiveAnalysisDTO.java
├── ReportDraftDTO.java
├── ReviewResultDTO.java
└── RepairInstructionDTO.java

workflow
└── CompetitiveAnalysisState.java
```

---

## 1.3 实现要求

### common

实现：

```text
Result<T>
BizException
JsonUtils
```

Result 需要包含：

```text
code
message
success
data
```

并提供：

```text
success()
success(T data)
fail(String message)
fail(Integer code, String message)
```

---

### enums

枚举需要包含：

```text
code
description
```

需要补全：

```text
AgentType
TaskStatus
SourceType
ReliabilityLevel
ReviewIssueType
RepairType
```

---

### schema

需要补全：

```text
DomainTemplate
Evidence
Claim
CapabilityItem
SupportItem
```

重点：

```text
Evidence 是证据链核心结构。
Claim 是结论结构，必须绑定 evidenceIds。
CapabilityItem 用于核心功能能力。
SupportItem 用于是否支持某能力。
```

---

### dto.agent

需要补全：

```text
TaskInputDTO
TaskPlanDTO
RawSourceSetDTO
ProductProfileSetDTO
CompetitiveAnalysisDTO
ReportDraftDTO
ReviewResultDTO
RepairInstructionDTO
```

复杂嵌套结构优先使用 static inner class，避免创建过多顶级类。

---

### workflow

补全：

```text
CompetitiveAnalysisState
```

字段包括：

```text
taskInput
taskPlan
rawSourceSet
productProfileSet
competitiveAnalysis
reportDraft
reviewResult
repairInstructions
iterationCount
status
```

并增加：

```text
increaseIteration()
isMaxIterationReached()
```

---

## 1.4 禁止修改

```text
1. 不要实现 Agent 真实逻辑。
2. 不要实现 Controller 接口。
3. 不要接入 Spring AI Alibaba。
4. 不要调用外部 API。
5. 不要实现数据库逻辑。
6. 不要改变项目包名。
7. 不要重构项目结构。
```

---

## 1.5 验收标准

```text
1. 项目能正常编译。
2. 所有 DTO 字段完整。
3. 所有枚举可用。
4. Evidence / Claim / ProductProfileSetDTO 结构完整。
5. CompetitiveAnalysisState 可以承载完整工作流状态。
```

---

# Task 02：实现 AgentNode、Mock Agent、WorkflowRouter、RepairRouter

## 2.1 任务目标

在不接入真实大模型、不接入搜索工具、不接数据库的前提下，先用 Mock 数据跑通完整多 Agent 工作流。

目标是验证：

```text
1. State 是否能贯穿全流程；
2. 六个 Agent 是否能顺序执行；
3. Reviewer 是否能触发回退；
4. RepairInstruction 是否能生成；
5. 回退后是否能重新执行下游节点；
6. 第二轮质检是否能通过；
7. 最终任务状态是否为 COMPLETED。
```

---

## 2.2 涉及文件

```text
agent
├── AgentNode.java
├── PlannerAgent.java
├── CollectorAgent.java
├── ExtractorAgent.java
├── AnalyzerAgent.java
├── WriterAgent.java
└── ReviewerAgent.java

workflow
├── CompetitiveAnalysisGraph.java
├── WorkflowRouter.java
└── RepairRouter.java
```

---

## 2.3 实现内容

### AgentNode

新增统一 Agent 接口：

```java
public interface AgentNode {

    AgentType getAgentType();

    CompetitiveAnalysisState execute(CompetitiveAnalysisState state);
}
```

所有 Agent 实现该接口。

---

### PlannerAgent

读取：

```text
state.taskInput
```

写入：

```text
state.taskPlan
```

Mock 输出：

```text
detectedDomain = AI_CODING_TOOLS
templateId = AI_CODING_TOOLS_TEMPLATE_V1
confidence = 0.95
```

为每个产品生成 collectionTasks。

---

### CollectorAgent

读取：

```text
state.taskPlan
state.repairInstructions
```

写入：

```text
state.rawSourceSet
```

Mock 行为：

```text
每个产品生成 3 条 Evidence：
1. OFFICIAL_SITE
2. PRICING_PAGE
3. DOCUMENTATION
```

---

### ExtractorAgent

读取：

```text
state.rawSourceSet
```

写入：

```text
state.productProfileSet
```

Mock 行为：

```text
每个产品生成一个 ProductProfile。
每个 ProductProfile 生成 claims 和 missingFields。
```

---

### AnalyzerAgent

读取：

```text
state.productProfileSet
state.rawSourceSet.evidencePool
```

写入：

```text
state.competitiveAnalysis
```

Mock 输出：

```text
comparisonMatrix
keyFindings
productOpportunities
risks
swotSummary
```

---

### WriterAgent

读取：

```text
state.productProfileSet
state.competitiveAnalysis
state.rawSourceSet.evidencePool
```

写入：

```text
state.reportDraft
```

Mock 输出 14 个标准章节。

---

### ReviewerAgent

读取：

```text
全局 state
```

写入：

```text
state.reviewResult
```

Mock 规则：

```text
iterationCount == 0：
  passed = false
  score = 78
  生成 high issue
  targetAgent = COLLECTOR_AGENT

iterationCount >= 1：
  passed = true
  score = 90
  issues = []
```

---

### RepairRouter

实现：

```text
chooseEarliestTargetAgent(List<ReviewIssue>)
buildRepairInstruction(CompetitiveAnalysisState)
```

回退优先级：

```text
COLLECTOR_AGENT
EXTRACTOR_AGENT
ANALYZER_AGENT
WRITER_AGENT
```

RepairType 映射：

```text
MISSING_EVIDENCE → SUPPLEMENT_EVIDENCE
EVIDENCE_NOT_LINKED → RELINK_EVIDENCE
SCHEMA_MISSING_FIELD → COMPLETE_SCHEMA
COMPARISON_INCOMPLETE → COMPLETE_COMPARISON
VAGUE_FINDING → REWRITE_ANALYSIS
REPORT_MISSING_SECTION → REWRITE_REPORT
CITATION_FORMAT_ERROR → FIX_CITATION
HALLUCINATION_RISK → REMOVE_OR_VERIFY_CLAIM
UNKNOWN_FIELD_TOO_MANY → SUPPLEMENT_EVIDENCE
```

---

### WorkflowRouter

实现：

```text
routeAfterReview(CompetitiveAnalysisState state)
```

返回：

```text
END
HUMAN_REVIEW
CollectorAgent
ExtractorAgent
AnalyzerAgent
WriterAgent
```

规则：

```text
passed = true → END
iterationCount >= maxIterations → HUMAN_REVIEW
否则根据 issue.targetAgent 回退
```

---

### CompetitiveAnalysisGraph

实现：

```text
run(TaskInputDTO taskInput)
runMockDemo()
```

执行链路：

```text
PlannerAgent
CollectorAgent
ExtractorAgent
AnalyzerAgent
WriterAgent
ReviewerAgent
```

Reviewer 不通过后：

```text
生成 RepairInstruction
iterationCount + 1
从目标 Agent 重新执行下游节点
```

---

## 2.4 禁止修改

```text
1. 不要接入真实 LLM。
2. 不要接入真实搜索。
3. 不要接数据库。
4. 不要实现 Controller 接口。
5. 不要改变 DTO 字段。
6. 不要把流程跳转逻辑写进 Agent 内部。
```

---

## 2.5 验收标准

执行：

```text
CompetitiveAnalysisGraph.runMockDemo()
```

预期：

```text
1. 第一次 Reviewer 不通过。
2. 生成 RepairInstruction。
3. 回退 CollectorAgent。
4. iterationCount = 1。
5. 第二次 Reviewer 通过。
6. state.status = COMPLETED。
7. state.reviewResult.passed = true。
8. state.reportDraft.sections 包含 14 个章节。
9. state.repairInstructions.size >= 1。
```

---

# Task 03：实现 WorkflowService、TaskService、Controller Mock 接口

## 3.1 任务目标

将 Mock 工作流通过 HTTP 接口暴露出来，方便前端或接口测试工具调用。

---

## 3.2 涉及文件

```text
controller
└── AnalysisTaskController.java

service
├── TaskService.java
├── WorkflowService.java
├── ReportService.java
├── EvidenceService.java
└── AgentRunService.java

dto/request
└── TaskCreateRequest.java

dto/response
├── TaskDetailResponse.java
├── ReportResponse.java
└── AgentRunResponse.java
```

---

## 3.3 实现内容

### AnalysisTaskController

提供接口：

```text
POST /api/tasks
GET  /api/tasks/{taskId}
GET  /api/tasks/{taskId}/report
GET  /api/tasks/{taskId}/evidence
GET  /api/tasks/{taskId}/review
```

---

### WorkflowService

职责：

```text
1. 接收 TaskInputDTO；
2. 调用 CompetitiveAnalysisGraph.run(taskInput)；
3. 返回 CompetitiveAnalysisState。
```

---

### TaskService

Mock 阶段使用内存 Map 保存任务状态。

```text
Map<String, CompetitiveAnalysisState>
```

提供：

```text
createTask(TaskCreateRequest request)
getTaskState(String taskId)
```

---

### ReportService

从内存 State 中读取：

```text
reportDraft
reviewResult
```

生成 ReportResponse。

---

### EvidenceService

从内存 State 中读取：

```text
rawSourceSet.evidencePool
```

---

### AgentRunService

Mock 阶段可以返回空列表或简单 Mock 日志。

---

## 3.4 禁止修改

```text
1. 不要接数据库。
2. 不要接真实大模型。
3. 不要引入复杂异步任务。
4. 不要做用户权限系统。
```

---

## 3.5 验收标准

使用接口创建任务：

```http
POST /api/tasks
```

请求：

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

预期：

```text
1. 返回 taskId。
2. 任务最终状态为 COMPLETED。
3. 可以查询报告。
4. 可以查询证据。
5. 可以查询 reviewResult。
```

---

# Task 04：实现 Entity、Repository、数据库持久化

## 4.1 任务目标

将 Mock 阶段的内存状态改为 MySQL 持久化。

---

## 4.2 涉及文件

```text
entity
├── AnalysisTaskEntity.java
├── AgentRunEntity.java
├── EvidenceEntity.java
├── ClaimEntity.java
├── ReportEntity.java
└── ReviewIssueEntity.java

repository
├── TaskRepository.java
├── AgentRunRepository.java
├── EvidenceRepository.java
├── ClaimRepository.java
├── ReportRepository.java
└── ReviewIssueRepository.java

service
├── TaskService.java
├── EvidenceService.java
├── ReportService.java
└── AgentRunService.java
```

---

## 4.3 建议数据库表

```text
analysis_task
agent_run
evidence
claim
report
review_issue
repair_instruction
```

---

## 4.4 实现内容

### analysis_task

保存：

```text
taskId
taskName
domain
targetProductsJson
analysisGoal
status
iterationCount
maxIterations
createdAt
updatedAt
```

---

### evidence

保存：

```text
evidenceId
taskId
productName
sourceType
sourceTitle
url
contentSnippet
collectedAt
reliability
usedForJson
```

---

### claim

保存：

```text
claimId
taskId
productName
dimension
statement
confidence
evidenceIdsJson
riskLevel
```

---

### report

保存：

```text
reportId
taskId
reportTitle
reportFormat
sectionsJson
sourceListJson
createdAt
updatedAt
```

---

### review_issue

保存：

```text
issueId
taskId
severity
type
description
targetAgent
targetProduct
targetDimension
repairInstruction
createdAt
```

---

### agent_run

保存：

```text
runId
taskId
agentType
inputType
outputType
status
startTime
endTime
durationMs
errorMessage
```

---

## 4.5 禁止修改

```text
1. 不要接真实模型。
2. 不要改变 DTO。
3. 不要改变工作流核心逻辑。
```

---

## 4.6 验收标准

```text
1. 创建任务后 analysis_task 有记录。
2. evidence 表有证据记录。
3. report 表有最终报告。
4. review_issue 表有第一次质检问题。
5. 查询接口不再依赖内存 Map。
```

---

# Task 05：实现 AgentRun Trace 日志记录

## 5.1 任务目标

记录每个 Agent 的执行过程，支持前端展示 Agent Trace。

---

## 5.2 涉及文件

```text
service
└── AgentRunService.java

entity
└── AgentRunEntity.java

repository
└── AgentRunRepository.java

agent
├── PlannerAgent.java
├── CollectorAgent.java
├── ExtractorAgent.java
├── AnalyzerAgent.java
├── WriterAgent.java
└── ReviewerAgent.java
```

---

## 5.3 实现内容

每个 Agent 执行时记录：

```text
runId
taskId
agentType
inputType
outputType
status
startTime
endTime
durationMs
errorMessage
```

后续接入真实模型后可扩展：

```text
modelName
promptTokens
completionTokens
totalTokens
toolCallsJson
inputSnapshotJson
outputSnapshotJson
```

---

## 5.4 验收标准

```text
1. 每次任务执行至少生成 6 条 AgentRun。
2. 回退后会继续生成新的 AgentRun。
3. 前端或接口可以查询 AgentRun 列表。
```

---

# Task 06：接入 Spring AI Alibaba ChatClient

## 6.1 任务目标

完成 Spring AI Alibaba 基础接入，为后续真实 Agent 调用做准备。

---

## 6.2 涉及文件

```text
config
├── SpringAiAlibabaConfig.java
├── ModelConfig.java
└── PromptTemplateConfig.java

pom.xml
application.yml
```

---

## 6.3 实现内容

配置：

```text
DashScope / 通义千问模型
API Key
baseUrl，如需要
modelName
temperature
maxTokens
```

实现一个简单测试方法：

```text
callSimpleChat(String prompt)
```

用于验证模型是否能正常调用。

---

## 6.4 禁止修改

```text
1. 不要直接替换所有 Mock Agent。
2. 不要接入搜索工具。
3. 不要改变工作流。
```

---

## 6.5 验收标准

```text
1. 项目启动正常。
2. 简单 Prompt 能返回模型响应。
3. API Key 从 application.yml 或环境变量读取。
4. 不在代码中硬编码 API Key。
```

---

# Task 07：实现真实 Prompt Agent 调用

## 7.1 任务目标

用 Spring AI Alibaba ChatClient 替换 Mock Agent 内部逻辑，按照 `05_agent_prompt_spec.md` 输出结构化 JSON。

---

## 7.2 涉及文件

```text
agent
├── PlannerAgent.java
├── ExtractorAgent.java
├── AnalyzerAgent.java
├── WriterAgent.java
└── ReviewerAgent.java

prompt
├── PlannerPrompt.java
├── ExtractorPrompt.java
├── AnalyzerPrompt.java
├── WriterPrompt.java
└── ReviewerPrompt.java

common
└── JsonUtils.java
```

CollectorAgent 可以先保留 Mock 或半 Mock，等 Task 08 接入真实搜索工具。

---

## 7.3 实现内容

每个 Agent 调用流程：

```text
1. 从 State 读取输入；
2. 使用 JsonUtils 转成 JSON 字符串；
3. 拼接 Prompt；
4. 调用 ChatClient；
5. 获取模型输出；
6. 清洗 JSON；
7. 反序列化为 DTO；
8. 写回 State；
9. 记录 AgentRun。
```

---

## 7.4 输出校验

每个 Agent 输出后要做基础校验：

```text
PlannerAgent：
  collectionTasks 覆盖所有产品

ExtractorAgent：
  products 不为空
  claims 绑定 evidenceIds

AnalyzerAgent：
  comparisonMatrix 不为空

WriterAgent：
  sections 包含 14 个标准章节

ReviewerAgent：
  passed / score / nextAction 不为空
```

---

## 7.5 禁止修改

```text
1. 不要改 DTO 字段。
2. 不要改工作流路由逻辑。
3. 不要让 Agent 返回自然语言文本。
4. 不要让一个 Agent 同时做多个 Agent 的工作。
```

---

## 7.6 验收标准

```text
1. PlannerAgent 能真实生成 TaskPlanDTO。
2. ExtractorAgent 能根据 Evidence 生成 ProductProfileSetDTO。
3. AnalyzerAgent 能生成 CompetitiveAnalysisDTO。
4. WriterAgent 能生成 ReportDraftDTO。
5. ReviewerAgent 能生成 ReviewResultDTO。
6. 模型输出可被 JsonUtils 正常解析。
```

---

# Task 08：实现 WebSearchTool / WebPageReaderTool / SourceRankTool

## 8.1 任务目标

实现 CollectorAgent 的真实公开信息采集能力。

---

## 8.2 涉及文件

```text
tool
├── WebSearchTool.java
├── WebPageReaderTool.java
├── SourceRankTool.java
└── EvidenceStoreTool.java

agent
└── CollectorAgent.java

config
└── SearchToolConfig.java
```

---

## 8.3 实现内容

### WebSearchTool

输入：

```text
query
```

输出：

```text
title
url
snippet
```

可以先使用：

```text
搜索 API
或 Mock Search
或 手动配置的 URL 列表
```

---

### WebPageReaderTool

输入：

```text
url
```

输出：

```text
title
rawText
contentSnippet
```

需要做简单清洗：

```text
去除 HTML 标签
去除脚本
截断超长文本
```

---

### SourceRankTool

根据 URL 和来源信息判断：

```text
sourceType
reliability
```

规则：

```text
官网 / pricing / docs → HIGH
blog / changelog / news → MEDIUM
community / user_comment → LOW
```

---

### EvidenceStoreTool

将证据保存为 Evidence 对象，后续持久化到数据库。

---

## 8.4 禁止修改

```text
1. 不要让 CollectorAgent 直接生成分析结论。
2. 不要把无 URL 的内容放入 Evidence。
3. 不要把低可靠性来源用于定价官方结论。
```

---

## 8.5 验收标准

```text
1. CollectorAgent 能根据 TaskPlanDTO 采集公开资料。
2. 每个产品至少生成 3 条 Evidence。
3. pricing 维度优先来自官方来源。
4. Evidence 中 url、contentSnippet、usedFor 不为空。
```

---

# Task 09：实现前端页面与接口联调

## 9.1 任务目标

实现基础前端页面，完成任务创建、报告展示、证据展示、质检结果展示。

---

## 9.2 页面范围

```text
1. 任务创建页
2. 任务详情页
3. Agent 执行流程页
4. 报告展示页
5. 证据来源页
6. 质检结果页
```

---

## 9.3 核心接口

```text
POST /api/tasks
GET  /api/tasks/{taskId}
GET  /api/tasks/{taskId}/report
GET  /api/tasks/{taskId}/evidence
GET  /api/tasks/{taskId}/review
GET  /api/tasks/{taskId}/agent-runs
```

---

## 9.4 验收标准

```text
1. 用户可以创建 AI 编程工具竞品分析任务。
2. 页面能展示最终报告。
3. 页面能展示 evidence source list。
4. 页面能展示 reviewResult。
5. 页面能展示 AgentRun 执行日志。
```

---

# Task 10：准备 Demo 数据、演示脚本和比赛材料

## 10.1 任务目标

准备比赛演示和答辩材料。

---

## 10.2 涉及文档

```text
docs/10_demo_script.md
docs/11_competition_materials.md
README.md
答辩 PPT
演示视频
```

---

## 10.3 Demo 固定输入

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

## 10.4 演示重点

展示：

```text
1. 用户创建任务；
2. PlannerAgent 生成任务计划；
3. CollectorAgent 生成证据池；
4. ExtractorAgent 生成产品画像；
5. AnalyzerAgent 生成竞品对比；
6. WriterAgent 生成报告；
7. ReviewerAgent 第一次发现问题；
8. RepairRouter 生成修复指令；
9. 系统自动回退；
10. 第二次质检通过；
11. 最终报告展示；
12. 证据来源展示；
13. Agent 执行日志展示。
```

---

## 10.5 比赛材料重点

答辩时突出：

```text
1. 多 Agent 分工明确；
2. 结构化 Schema 驱动；
3. Evidence + Claim 证据链；
4. Reviewer 质检反馈闭环；
5. RepairInstruction 自动回退；
6. 领域模板可扩展；
7. Spring Boot + Spring AI Alibaba 工程化实现；
8. 可扩展到协同办公、HR SaaS、知识库等企业系统场景。
```

---

## 10.6 验收标准

```text
1. Demo 流程稳定；
2. 演示数据可复现；
3. 报告内容完整；
4. 证据来源可查看；
5. 质检回退能演示；
6. README 能说明启动方式；
7. PPT 能清楚说明系统价值和技术架构。
```

---

# 4. 推荐当前开发顺序

当前建议立即执行：

```text
Task 01：补全 DTO / Schema / State / Enum / Common
Task 02：实现 AgentNode、Mock Agent、WorkflowRouter、RepairRouter
Task 03：实现 WorkflowService、TaskService、Controller Mock 接口
```

这三步完成后，就可以获得一个可演示的最小闭环系统。

---

# 5. Vibe Coding 使用方式

每次让 Vibe Coding 写代码时，应采用如下格式：

```text
请阅读 docs/09_development_tasks.md 中的 Task XX。
只实现 Task XX 中定义的内容。
不要实现后续任务。
不要重构项目结构。
不要改变已定义 DTO / Schema。
实现完成后保证项目可编译。
```

例如：

```text
请阅读 docs/09_development_tasks.md 中的 Task 02：实现 AgentNode、Mock Agent、WorkflowRouter、RepairRouter。
只完成 Task 02，不要接入 Spring AI Alibaba，不要接数据库，不要实现 Controller。
```

---

# 6. 开发纪律

所有开发任务必须遵守：

```text
1. 先保证编译通过；
2. 每次只实现一个 Task；
3. 不跨阶段实现；
4. 不随意改 DTO；
5. 不随意改包结构；
6. Agent 不越权；
7. 工作流路由统一由 WorkflowRouter 控制；
8. 修复回退统一由 RepairRouter 控制；
9. 所有事实性结论最终必须能绑定 Evidence；
10. 文档和代码保持同步。
```

---

# 7. 当前版本边界

本文档 V1 只定义开发任务拆分。

不包含：

```text
1. 详细 API 字段设计；
2. 详细数据库字段设计；
3. 前端页面布局细节；
4. Prompt 全量内容；
5. Spring AI Alibaba 具体依赖版本；
6. 搜索 API 具体选型。
```

这些内容由其他文档定义：

```text
06_api_spec.md
07_database_spec.md
08_frontend_spec.md
05_agent_prompt_spec.md
02_system_architecture.md
```

