# 竞品分析 Agent 系统工作流设计文档

版本：V1.0  
项目：CA_Agent  
主 Demo：AI 编程工具竞品分析  
技术栈：Spring Boot + Spring AI Alibaba + 多 Agent 工作流

---

## 1. 文档目标

本文档定义竞品分析 Agent 系统的多 Agent 工作流，包括：

1. Agent 角色划分；
2. 每个 Agent 的输入输出；
3. 正常执行链路；
4. Reviewer 质检逻辑；
5. 自动回退修复机制；
6. WorkflowRouter 路由规则；
7. RepairRouter 修复指令生成规则；
8. 最大迭代次数与人工介入规则；
9. Mock 阶段执行逻辑；
10. 后续接入 Spring AI Alibaba 的扩展方式。

本系统不是单次 LLM 文本生成，而是一个可追踪、可回退、可质检的多 Agent 协作流程。

---

## 2. 总体工作流

核心执行链路如下：

```text
TaskInputDTO
  ↓
PlannerAgent
  ↓
CollectorAgent
  ↓
ExtractorAgent
  ↓
AnalyzerAgent
  ↓
WriterAgent
  ↓
ReviewerAgent
  ↓
WorkflowRouter
  ├── passed = true  → COMPLETED
  ├── passed = false 且未超过最大迭代次数 → RepairInstructionDTO → 回退指定 Agent
  └── passed = false 且达到最大迭代次数 → WAITING_HUMAN_REVIEW
````

---

## 3. 工作流设计原则

### 3.1 Agent 职责单一

每个 Agent 只负责自己的任务，不越权处理其他环节。

| Agent          | 只负责       | 不负责         |
| -------------- | --------- | ----------- |
| PlannerAgent   | 任务规划      | 采集资料、写报告    |
| CollectorAgent | 信息采集与证据生成 | 分析竞品、写报告    |
| ExtractorAgent | 结构化抽取     | 横向对比、写报告    |
| AnalyzerAgent  | 竞品对比分析    | 采集资料、生成最终报告 |
| WriterAgent    | 报告组织与表达   | 事实核验        |
| ReviewerAgent  | 质检与回退建议   | 直接修改报告      |

---

### 3.2 State 驱动

所有 Agent 共享同一个全局状态对象：

```text
CompetitiveAnalysisState
```

每个 Agent 从 State 中读取输入，并把输出写回 State。

```text
PlannerAgent:
  读取 taskInput
  写入 taskPlan

CollectorAgent:
  读取 taskPlan / repairInstructions
  写入 rawSourceSet

ExtractorAgent:
  读取 rawSourceSet
  写入 productProfileSet

AnalyzerAgent:
  读取 productProfileSet
  写入 competitiveAnalysis

WriterAgent:
  读取 productProfileSet + competitiveAnalysis
  写入 reportDraft

ReviewerAgent:
  读取全局 State
  写入 reviewResult
```

---

### 3.3 证据链贯穿全流程

系统中的事实性结论必须遵循：

```text
ReportSection
  ↓
Claim
  ↓
Evidence
  ↓
URL / Source
```

任何没有证据支撑的关键结论，都应被 Reviewer 标记为问题。

---

### 3.4 质检闭环

ReviewerAgent 不是简单打分模块，而是工作流闭环的核心节点。

它需要判断：

```text
是否通过
问题类型
问题严重程度
应该回退到哪个 Agent
应该如何修复
是否需要人工介入
```

---

## 4. Agent 角色定义

---

## 4.1 PlannerAgent

### 4.1.1 职责

PlannerAgent 负责将用户输入转化为可执行任务计划。

主要任务：

```text
1. 识别分析领域；
2. 匹配领域模板；
3. 确认竞品列表；
4. 确认分析维度；
5. 为每个竞品生成采集任务；
6. 为每个采集任务生成搜索 Query；
7. 指定优先来源类型；
8. 生成后续工作流节点顺序。
```

---

### 4.1.2 输入

```text
CompetitiveAnalysisState.taskInput
```

对应结构：

```text
TaskInputDTO
```

---

### 4.1.3 输出

```text
CompetitiveAnalysisState.taskPlan
```

对应结构：

```text
TaskPlanDTO
```

---

### 4.1.4 输入输出关系

```text
TaskInputDTO
  ↓
PlannerAgent
  ↓
TaskPlanDTO
```

---

### 4.1.5 输出要求

TaskPlanDTO 必须包含：

```text
taskId
detectedDomain
templateId
confidence
products
analysisGoal
analysisDimensions
collectionTasks
workflow
```

其中 AI 编程工具默认分析维度为：

```text
positioning
target_users
core_capabilities
agent_capabilities
codebase_understanding
ide_ecosystem
model_context
pricing
enterprise_features
user_feedback
```

---

### 4.1.6 Mock 阶段行为

在 Mock 阶段，PlannerAgent 固定输出：

```text
detectedDomain = AI_CODING_TOOLS
templateId = AI_CODING_TOOLS_TEMPLATE_V1
confidence = 0.95
```

对每个竞品生成 4 类 Query：

```text
{productName} official AI coding tool
{productName} pricing
{productName} documentation agent coding
{productName} codebase understanding
```

---

## 4.2 CollectorAgent

### 4.2.1 职责

CollectorAgent 负责公开信息采集和证据池生成。

主要任务：

```text
1. 根据 TaskPlanDTO 中的 collectionTasks 执行搜索；
2. 获取官网、定价页、文档、博客、测评等公开资料；
3. 生成 rawSources；
4. 从 rawSources 中抽取关键片段；
5. 生成 evidencePool；
6. 标记 sourceType 和 reliability；
7. 对缺失信息生成 missingSources。
```

---

### 4.2.2 输入

正常执行时：

```text
CompetitiveAnalysisState.taskPlan
```

回退修复时：

```text
CompetitiveAnalysisState.taskPlan
CompetitiveAnalysisState.repairInstructions
```

---

### 4.2.3 输出

```text
CompetitiveAnalysisState.rawSourceSet
```

对应结构：

```text
RawSourceSetDTO
```

---

### 4.2.4 输入输出关系

```text
TaskPlanDTO
  ↓
CollectorAgent
  ↓
RawSourceSetDTO + Evidence
```

---

### 4.2.5 输出要求

RawSourceSetDTO 必须包含：

```text
taskId
rawSources
evidencePool
missingSources
```

其中 Evidence 必须包含：

```text
evidenceId
productName
sourceType
sourceTitle
url
contentSnippet
collectedAt
reliability
usedFor
```

---

### 4.2.6 约束

CollectorAgent 不允许：

```text
1. 生成竞品分析结论；
2. 生成 SWOT；
3. 生成最终报告；
4. 编造来源；
5. 把没有 URL 的信息放入 evidencePool；
6. 对价格、排名、模型名称等敏感事实进行无来源判断。
```

---

### 4.2.7 Mock 阶段行为

在 Mock 阶段，对每个 product 至少生成 3 条 Evidence：

```text
1. OFFICIAL_SITE 证据；
2. PRICING_PAGE 证据；
3. DOCUMENTATION 证据。
```

示例 Evidence：

```json
{
  "evidenceId": "ev_cursor_001",
  "productName": "Cursor",
  "sourceType": "OFFICIAL_SITE",
  "sourceTitle": "Cursor Official Page",
  "url": "https://example.com/cursor/official",
  "contentSnippet": "Cursor provides AI-assisted coding features for developers.",
  "reliability": "HIGH",
  "usedFor": ["positioning", "core_capabilities"]
}
```

---

## 4.3 ExtractorAgent

### 4.3.1 职责

ExtractorAgent 负责将原始资料和证据池抽取为结构化产品画像。

主要任务：

```text
1. 抽取产品基础信息；
2. 抽取产品定位；
3. 抽取目标用户；
4. 抽取核心功能；
5. 抽取 Agent 编程能力；
6. 抽取代码库理解能力；
7. 抽取 IDE 与生态集成；
8. 抽取模型与上下文能力；
9. 抽取定价模式；
10. 抽取企业版能力；
11. 抽取用户评价；
12. 生成 Claim；
13. 记录 missingFields。
```

---

### 4.3.2 输入

```text
CompetitiveAnalysisState.rawSourceSet
```

---

### 4.3.3 输出

```text
CompetitiveAnalysisState.productProfileSet
```

对应结构：

```text
ProductProfileSetDTO
```

---

### 4.3.4 输入输出关系

```text
RawSourceSetDTO + Evidence
  ↓
ExtractorAgent
  ↓
ProductProfileSetDTO + Claim
```

---

### 4.3.5 输出要求

ProductProfileSetDTO 中每个 ProductProfile 必须包含：

```text
productName
company
officialUrl
productType
positioning
targetUsers
coreCapabilities
agentCapabilities
codebaseUnderstanding
ideEcosystem
modelContext
pricing
enterpriseFeatures
userFeedback
claims
missingFields
```

---

### 4.3.6 约束

ExtractorAgent 不允许：

```text
1. 进行横向竞品对比；
2. 生成最终报告；
3. 编造 evidenceId；
4. 凭常识补全未知字段；
5. 生成没有证据支撑的具体价格、模型、企业客户案例。
```

如果公开资料无法确认，应使用：

```text
unknown
```

如果只发现部分支持，应使用：

```text
partial
```

---

### 4.3.7 Mock 阶段行为

在 Mock 阶段，每个产品生成一个 ProductProfile。

默认能力填充规则：

```text
codeCompletion = true / high
codeGeneration = true / high
codeExplanation = true / medium
refactoring = partial / medium
unitTestGeneration = partial / medium
debugAssistance = partial / medium
documentationGeneration = partial / medium

taskPlanning = partial
multiFileEditing = partial
terminalExecution = unknown
testRunAndFix = partial
codeReview = partial
autonomousLoop = unknown

repositoryIndexing = partial
crossFileReference = partial
projectQa = true
longContextSupport = unknown
```

unknown 字段必须写入 missingFields。

---

## 4.4 AnalyzerAgent

### 4.4.1 职责

AnalyzerAgent 负责基于结构化产品画像进行横向竞品分析。

主要任务：

```text
1. 生成核心功能对比矩阵；
2. 生成 Agent 编程能力对比；
3. 生成代码库理解能力对比；
4. 生成定价模式对比；
5. 提炼关键发现；
6. 生成产品机会点；
7. 识别风险；
8. 生成 SWOT 总结。
```

---

### 4.4.2 输入

```text
CompetitiveAnalysisState.productProfileSet
CompetitiveAnalysisState.rawSourceSet.evidencePool
```

---

### 4.4.3 输出

```text
CompetitiveAnalysisState.competitiveAnalysis
```

对应结构：

```text
CompetitiveAnalysisDTO
```

---

### 4.4.4 输入输出关系

```text
ProductProfileSetDTO + Evidence
  ↓
AnalyzerAgent
  ↓
CompetitiveAnalysisDTO
```

---

### 4.4.5 输出要求

CompetitiveAnalysisDTO 必须包含：

```text
comparisonMatrix
keyFindings
productOpportunities
risks
swotSummary
```

comparisonMatrix 至少覆盖：

```text
core_capabilities / code_generation
agent_capabilities / multi_file_editing
codebase_understanding / project_qa
pricing / free_plan
```

---

### 4.4.6 约束

AnalyzerAgent 不允许：

```text
1. 新增不在 ProductProfileSet 中的事实；
2. 直接生成最终报告；
3. 编造市场排名、份额、价格；
4. 输出空泛结论。
```

空泛结论示例：

```text
技术先进
市场广阔
竞争激烈
生态完善
```

应改为更具体的表述，例如：

```text
AI 编程工具正在从单点代码补全，扩展到多文件编辑、项目级问答和任务执行。
```

---

### 4.4.7 Mock 阶段行为

Mock 阶段至少生成：

```text
4 个 comparisonMatrix 维度；
2 条 keyFindings；
1 条 productOpportunity；
2 条 risks；
每个产品 1 组 SWOT。
```

默认 keyFindings：

```text
1. AI 编程工具正在从代码补全走向 Agent 编程；
2. 企业研发场景更关注代码库理解、权限控制和数据安全。
```

---

## 4.5 WriterAgent

### 4.5.1 职责

WriterAgent 负责将结构化分析结果组织成 Markdown 报告草稿。

主要任务：

```text
1. 生成报告标题；
2. 生成标准报告章节；
3. 将对比矩阵、关键发现、机会点组织成可读文本；
4. 维护章节与 claimIds / evidenceIds 的关联；
5. 生成 sourceList。
```

---

### 4.5.2 输入

```text
CompetitiveAnalysisState.productProfileSet
CompetitiveAnalysisState.competitiveAnalysis
CompetitiveAnalysisState.rawSourceSet.evidencePool
```

---

### 4.5.3 输出

```text
CompetitiveAnalysisState.reportDraft
```

对应结构：

```text
ReportDraftDTO
```

---

### 4.5.4 输入输出关系

```text
ProductProfileSetDTO + CompetitiveAnalysisDTO + Evidence
  ↓
WriterAgent
  ↓
ReportDraftDTO
```

---

### 4.5.5 报告必需章节

ReportDraft 必须包含 14 个标准章节：

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

### 4.5.6 约束

WriterAgent 不允许：

```text
1. 新增未出现在 ProductProfileSet 或 CompetitiveAnalysis 中的事实；
2. 编造 evidenceId；
3. 删除信息来源章节；
4. 使用无证据的绝对化表述；
5. 对证据不足的内容做强结论。
```

---

### 4.5.7 Mock 阶段行为

Mock 阶段每个章节生成一段简短 Markdown 内容。

每个章节至少包含：

```text
sectionId
title
content
relatedClaimIds
evidenceIds
```

sourceList 直接使用：

```text
rawSourceSet.evidencePool
```

---

## 4.6 ReviewerAgent

### 4.6.1 职责

ReviewerAgent 负责质量检查，并决定是否需要回退修复。

主要任务：

```text
1. 检查字段完整性；
2. 检查证据完整性；
3. 检查报告章节完整性；
4. 检查对比矩阵完整性；
5. 检查幻觉风险；
6. 生成 ReviewResultDTO；
7. 给出 nextAction；
8. 标记建议回退 Agent。
```

---

### 4.6.2 输入

```text
CompetitiveAnalysisState
```

ReviewerAgent 需要读取全局 State，包括：

```text
taskInput
taskPlan
rawSourceSet
productProfileSet
competitiveAnalysis
reportDraft
repairInstructions
iterationCount
```

---

### 4.6.3 输出

```text
CompetitiveAnalysisState.reviewResult
```

对应结构：

```text
ReviewResultDTO
```

---

### 4.6.4 输入输出关系

```text
CompetitiveAnalysisState
  ↓
ReviewerAgent
  ↓
ReviewResultDTO
```

---

### 4.6.5 通过条件

Reviewer 通过必须同时满足：

```text
1. score >= 85；
2. 不存在 high severity issue；
3. 报告核心章节完整；
4. 核心结论均绑定 evidenceIds；
5. 定价、产品定位、Agent 编程能力等关键维度具备证据支撑。
```

---

### 4.6.6 问题类型与回退目标

| issue.type             | 含义           | targetAgent                      |
| ---------------------- | ------------ | -------------------------------- |
| MISSING_EVIDENCE       | 缺少证据         | COLLECTOR_AGENT                  |
| EVIDENCE_NOT_LINKED    | 有证据但未绑定      | EXTRACTOR_AGENT / ANALYZER_AGENT |
| SCHEMA_MISSING_FIELD   | Schema 字段缺失  | EXTRACTOR_AGENT                  |
| COMPARISON_INCOMPLETE  | 对比矩阵不完整      | ANALYZER_AGENT                   |
| VAGUE_FINDING          | 分析结论空泛       | ANALYZER_AGENT                   |
| REPORT_MISSING_SECTION | 报告缺少章节       | WRITER_AGENT                     |
| CITATION_FORMAT_ERROR  | 引用格式错误       | WRITER_AGENT                     |
| HALLUCINATION_RISK     | 存在幻觉风险       | ANALYZER_AGENT / WRITER_AGENT    |
| UNKNOWN_FIELD_TOO_MANY | unknown 字段过多 | COLLECTOR_AGENT                  |

---

### 4.6.7 Mock 阶段行为

Mock 阶段采用可测试逻辑：

```text
1. 如果 iterationCount == 0：
   - passed = false
   - score = 78
   - 生成一个 high issue
   - issue.type = MISSING_EVIDENCE
   - issue.targetAgent = COLLECTOR_AGENT
   - issue.targetDimension = pricing
   - nextAction.action = repair

2. 如果 iterationCount >= 1：
   - passed = true
   - score = 90
   - issues = []
   - nextAction.action = finish
```

这样可以验证：

```text
第一次质检失败 → 回退 Collector → 重新执行下游节点 → 第二次质检通过。
```

---

## 5. WorkflowRouter 设计

---

## 5.1 职责

WorkflowRouter 负责在 ReviewerAgent 执行后决定下一步。

输入：

```text
CompetitiveAnalysisState.reviewResult
CompetitiveAnalysisState.iterationCount
CompetitiveAnalysisState.taskInput.maxIterations
```

输出：

```text
END
HUMAN_REVIEW
CollectorAgent
ExtractorAgent
AnalyzerAgent
WriterAgent
```

---

## 5.2 路由规则

### 规则 1：Reviewer 通过

如果：

```text
reviewResult.passed == true
```

则返回：

```text
END
```

任务状态变为：

```text
COMPLETED
```

---

### 规则 2：Reviewer 不通过，但超过最大修复轮次

如果：

```text
reviewResult.passed == false
iterationCount >= maxIterations
```

则返回：

```text
HUMAN_REVIEW
```

任务状态变为：

```text
WAITING_HUMAN_REVIEW
```

---

### 规则 3：Reviewer 不通过，且可自动修复

如果：

```text
reviewResult.passed == false
iterationCount < maxIterations
```

则调用 RepairRouter 选择最早回退 Agent。

---

## 5.3 伪代码

```java
public String routeAfterReview(CompetitiveAnalysisState state) {
    ReviewResultDTO review = state.getReviewResult();

    if (Boolean.TRUE.equals(review.getPassed())) {
        return "END";
    }

    Integer iterationCount = state.getIterationCount() == null ? 0 : state.getIterationCount();
    Integer maxIterations = state.getTaskInput().getMaxIterations() == null
            ? 2
            : state.getTaskInput().getMaxIterations();

    if (iterationCount >= maxIterations) {
        return "HUMAN_REVIEW";
    }

    AgentType targetAgent = repairRouter.chooseEarliestTargetAgent(review.getIssues());

    if (targetAgent == null) {
        return "HUMAN_REVIEW";
    }

    return switch (targetAgent) {
        case COLLECTOR_AGENT -> "CollectorAgent";
        case EXTRACTOR_AGENT -> "ExtractorAgent";
        case ANALYZER_AGENT -> "AnalyzerAgent";
        case WRITER_AGENT -> "WriterAgent";
        default -> "HUMAN_REVIEW";
    };
}
```

---

## 6. RepairRouter 设计

---

## 6.1 职责

RepairRouter 负责：

```text
1. 根据 ReviewIssue 列表选择最早需要回退的 Agent；
2. 根据 ReviewIssue 生成 RepairInstructionDTO；
3. 将 issue.type 映射为 repairType；
4. 聚合多个问题的修复指令。
```

---

## 6.2 回退优先级

多个问题同时存在时，应回退到最早的上游节点。

优先级为：

```text
COLLECTOR_AGENT
  >
EXTRACTOR_AGENT
  >
ANALYZER_AGENT
  >
WRITER_AGENT
```

含义：

```text
只要存在需要回退 Collector 的问题，就回退 Collector。
否则如果存在需要回退 Extractor 的问题，就回退 Extractor。
否则如果存在需要回退 Analyzer 的问题，就回退 Analyzer。
否则如果存在需要回退 Writer 的问题，就回退 Writer。
```

---

## 6.3 为什么选择最早回退节点

因为上游结果会影响下游结果。

例如：

```text
定价缺少证据 → CollectorAgent 问题
```

如果只回退 WriterAgent，即使报告重新写了，也无法解决证据缺失问题。

因此必须从 CollectorAgent 开始重新执行：

```text
CollectorAgent
  ↓
ExtractorAgent
  ↓
AnalyzerAgent
  ↓
WriterAgent
  ↓
ReviewerAgent
```

---

## 6.4 chooseEarliestTargetAgent 规则

输入：

```text
List<ReviewIssue>
```

输出：

```text
AgentType
```

伪代码：

```java
public AgentType chooseEarliestTargetAgent(List<ReviewIssue> issues) {
    boolean needCollector = false;
    boolean needExtractor = false;
    boolean needAnalyzer = false;
    boolean needWriter = false;

    for (ReviewIssue issue : issues) {
        if (issue.getTargetAgent() == AgentType.COLLECTOR_AGENT) {
            needCollector = true;
        } else if (issue.getTargetAgent() == AgentType.EXTRACTOR_AGENT) {
            needExtractor = true;
        } else if (issue.getTargetAgent() == AgentType.ANALYZER_AGENT) {
            needAnalyzer = true;
        } else if (issue.getTargetAgent() == AgentType.WRITER_AGENT) {
            needWriter = true;
        }
    }

    if (needCollector) {
        return AgentType.COLLECTOR_AGENT;
    }
    if (needExtractor) {
        return AgentType.EXTRACTOR_AGENT;
    }
    if (needAnalyzer) {
        return AgentType.ANALYZER_AGENT;
    }
    if (needWriter) {
        return AgentType.WRITER_AGENT;
    }

    return null;
}
```

---

## 6.5 RepairType 映射规则

| ReviewIssueType        | RepairType             |
| ---------------------- | ---------------------- |
| MISSING_EVIDENCE       | SUPPLEMENT_EVIDENCE    |
| EVIDENCE_NOT_LINKED    | RELINK_EVIDENCE        |
| SCHEMA_MISSING_FIELD   | COMPLETE_SCHEMA        |
| COMPARISON_INCOMPLETE  | COMPLETE_COMPARISON    |
| VAGUE_FINDING          | REWRITE_ANALYSIS       |
| REPORT_MISSING_SECTION | REWRITE_REPORT         |
| CITATION_FORMAT_ERROR  | FIX_CITATION           |
| HALLUCINATION_RISK     | REMOVE_OR_VERIFY_CLAIM |
| UNKNOWN_FIELD_TOO_MANY | SUPPLEMENT_EVIDENCE    |

---

## 6.6 buildRepairInstruction 规则

输入：

```text
CompetitiveAnalysisState
```

输出：

```text
RepairInstructionDTO
```

生成逻辑：

```text
1. 读取 reviewResult.issues；
2. 优先选择 severity = high 的问题；
3. 如果没有 high 问题，则选择全部问题；
4. 根据 selectedIssues 选择最早回退 Agent；
5. 收集 issueIds；
6. 根据 issue.type 推断 repairType；
7. targetProduct 使用第一个 selectedIssue 的 targetProduct；
8. targetDimension 使用第一个 selectedIssue 的 targetDimension；
9. instruction 拼接所有 selectedIssue 的 repairInstruction；
10. priority 若存在 high，则为 high，否则为 medium。
```

---

## 6.7 RepairInstruction 示例

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

## 7. 回退执行链路

---

## 7.1 回退到 CollectorAgent

触发场景：

```text
缺少证据
unknown 字段过多
定价缺少官方来源
用户评价来源不足
```

执行链路：

```text
CollectorAgent
  ↓
ExtractorAgent
  ↓
AnalyzerAgent
  ↓
WriterAgent
  ↓
ReviewerAgent
```

---

## 7.2 回退到 ExtractorAgent

触发场景：

```text
Schema 字段缺失
Evidence 已存在但未绑定到字段
ProductProfile 抽取不完整
```

执行链路：

```text
ExtractorAgent
  ↓
AnalyzerAgent
  ↓
WriterAgent
  ↓
ReviewerAgent
```

---

## 7.3 回退到 AnalyzerAgent

触发场景：

```text
对比矩阵缺失
关键发现空泛
机会点不完整
SWOT 分析质量差
分析结论存在幻觉风险
```

执行链路：

```text
AnalyzerAgent
  ↓
WriterAgent
  ↓
ReviewerAgent
```

---

## 7.4 回退到 WriterAgent

触发场景：

```text
报告缺少章节
引用格式错误
报告表达不清晰
报告使用了不合适的绝对化表达
```

执行链路：

```text
WriterAgent
  ↓
ReviewerAgent
```

---

## 8. CompetitiveAnalysisGraph 设计

---

## 8.1 职责

CompetitiveAnalysisGraph 是工作流执行器。

它负责：

```text
1. 初始化 CompetitiveAnalysisState；
2. 执行完整 Agent 链路；
3. 调用 WorkflowRouter 判断下一步；
4. 调用 RepairRouter 生成修复指令；
5. 根据回退节点重新执行下游链路；
6. 控制最大迭代次数；
7. 返回最终 State。
```

---

## 8.2 核心方法

```java
CompetitiveAnalysisState run(TaskInputDTO taskInput);
```

可选 Mock 方法：

```java
CompetitiveAnalysisState runMockDemo();
```

---

## 8.3 run 方法执行流程

```text
1. 创建 CompetitiveAnalysisState；
2. 写入 taskInput；
3. iterationCount = 0；
4. status = CREATED；
5. 执行完整链路：
   PlannerAgent
   CollectorAgent
   ExtractorAgent
   AnalyzerAgent
   WriterAgent
   ReviewerAgent
6. 调用 WorkflowRouter.routeAfterReview(state)；
7. 如果返回 END：
   status = COMPLETED；
   返回 state；
8. 如果返回 HUMAN_REVIEW：
   status = WAITING_HUMAN_REVIEW；
   返回 state；
9. 如果返回具体 Agent：
   生成 RepairInstructionDTO；
   加入 state.repairInstructions；
   iterationCount + 1；
   status = REPAIRING；
   从目标 Agent 开始执行后续链路；
10. 重复步骤 6～9，直到 END 或 HUMAN_REVIEW。
```

---

## 8.4 主循环伪代码

```java
public CompetitiveAnalysisState run(TaskInputDTO taskInput) {
    CompetitiveAnalysisState state = initState(taskInput);

    executeFullChain(state);

    while (true) {
        String route = workflowRouter.routeAfterReview(state);

        if ("END".equals(route)) {
            state.setStatus(TaskStatus.COMPLETED);
            return state;
        }

        if ("HUMAN_REVIEW".equals(route)) {
            state.setStatus(TaskStatus.WAITING_HUMAN_REVIEW);
            return state;
        }

        RepairInstructionDTO repairInstruction = repairRouter.buildRepairInstruction(state);
        state.getRepairInstructions().add(repairInstruction);
        state.increaseIteration();
        state.setStatus(TaskStatus.REPAIRING);

        executeFrom(route, state);
    }
}
```

---

## 8.5 executeFrom 规则

```java
private void executeFrom(String route, CompetitiveAnalysisState state) {
    switch (route) {
        case "CollectorAgent" -> executeFromCollector(state);
        case "ExtractorAgent" -> executeFromExtractor(state);
        case "AnalyzerAgent" -> executeFromAnalyzer(state);
        case "WriterAgent" -> executeFromWriter(state);
        default -> throw new BizException("Unknown workflow route: " + route);
    }
}
```

---

## 8.6 分段执行规则

```java
private void executeFullChain(CompetitiveAnalysisState state) {
    plannerAgent.execute(state);
    collectorAgent.execute(state);
    extractorAgent.execute(state);
    analyzerAgent.execute(state);
    writerAgent.execute(state);
    reviewerAgent.execute(state);
}

private void executeFromCollector(CompetitiveAnalysisState state) {
    collectorAgent.execute(state);
    extractorAgent.execute(state);
    analyzerAgent.execute(state);
    writerAgent.execute(state);
    reviewerAgent.execute(state);
}

private void executeFromExtractor(CompetitiveAnalysisState state) {
    extractorAgent.execute(state);
    analyzerAgent.execute(state);
    writerAgent.execute(state);
    reviewerAgent.execute(state);
}

private void executeFromAnalyzer(CompetitiveAnalysisState state) {
    analyzerAgent.execute(state);
    writerAgent.execute(state);
    reviewerAgent.execute(state);
}

private void executeFromWriter(CompetitiveAnalysisState state) {
    writerAgent.execute(state);
    reviewerAgent.execute(state);
}
```

---

## 9. 任务状态机

---

## 9.1 状态定义

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

---

## 9.2 正常状态流转

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
  ↓
COMPLETED
```

---

## 9.3 修复状态流转

```text
REVIEWING
  ↓
REPAIRING
  ↓
COLLECTING / EXTRACTING / ANALYZING / WRITING
  ↓
REVIEWING
  ↓
COMPLETED / WAITING_HUMAN_REVIEW
```

---

## 9.4 人工介入状态

当自动修复无法解决问题时，状态变为：

```text
WAITING_HUMAN_REVIEW
```

触发条件：

```text
1. iterationCount >= maxIterations；
2. 无法判断应该回退哪个 Agent；
3. 连续多次出现相同问题；
4. 出现事实冲突；
5. 缺失信息无法通过公开来源补齐。
```

---

## 10. 最大迭代次数

---

## 10.1 默认值

```text
maxIterations = 2
```

如果用户未设置，则系统默认使用 2。

---

## 10.2 规则

```text
1. 初始执行不计入修复轮次；
2. Reviewer 第一次不通过后，iterationCount + 1；
3. 每次自动回退修复后，重新进入 Reviewer；
4. 如果 iterationCount >= maxIterations 仍未通过，则进入 WAITING_HUMAN_REVIEW；
5. 不允许无限循环。
```

---

## 10.3 示例

```text
初始：
iterationCount = 0

第一次 Reviewer 不通过：
iterationCount = 1
回退 CollectorAgent

第二次 Reviewer 不通过：
iterationCount = 2
如果 maxIterations = 2，则进入 WAITING_HUMAN_REVIEW

如果第二次 Reviewer 通过：
任务 COMPLETED
```

---

## 11. Mock 阶段执行标准

---

## 11.1 Mock 阶段目标

Mock 阶段不接入真实模型，不接入搜索工具，不接入数据库。

目标是验证：

```text
1. Agent 链路是否能跑通；
2. State 是否能正确传递；
3. Reviewer 是否能触发回退；
4. RepairInstruction 是否能生成；
5. 回退后是否能重新执行下游节点；
6. 第二轮是否能完成任务；
7. 最终 State 是否完整。
```

---

## 11.2 Mock 执行预期

给定任务：

```text
竞品：Cursor、Windsurf、GitHub Copilot、通义灵码
maxIterations = 2
```

预期过程：

```text
1. PlannerAgent 生成 TaskPlanDTO；
2. CollectorAgent 生成 RawSourceSetDTO 和 Evidence；
3. ExtractorAgent 生成 ProductProfileSetDTO；
4. AnalyzerAgent 生成 CompetitiveAnalysisDTO；
5. WriterAgent 生成 ReportDraftDTO；
6. ReviewerAgent 第一次返回 passed = false；
7. WorkflowRouter 路由到 CollectorAgent；
8. RepairRouter 生成 RepairInstructionDTO；
9. iterationCount 从 0 变为 1；
10. 执行 CollectorAgent → ExtractorAgent → AnalyzerAgent → WriterAgent → ReviewerAgent；
11. ReviewerAgent 第二次返回 passed = true；
12. 最终状态为 COMPLETED。
```

---

## 11.3 Mock 最终 State 必须包含

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

其中：

```text
status = COMPLETED
iterationCount = 1
repairInstructions.size >= 1
reviewResult.passed = true
```

---

## 12. 后续接入 Spring AI Alibaba 的扩展方式

---

## 12.1 当前 Mock 阶段

当前 Agent 是规则化 Mock 实现：

```text
PlannerAgent: 构造固定 TaskPlanDTO
CollectorAgent: 构造固定 Evidence
ExtractorAgent: 构造固定 ProductProfile
AnalyzerAgent: 构造固定 CompetitiveAnalysis
WriterAgent: 构造固定 ReportDraft
ReviewerAgent: 根据 iterationCount 模拟通过或不通过
```

---

## 12.2 接入模型后的变化

未来接入 Spring AI Alibaba 后，各 Agent 的结构不变，只替换内部实现：

```text
PlannerAgent:
  Mock 逻辑 → LLM 任务规划

CollectorAgent:
  Mock 证据 → Tool Calling + WebSearchTool + WebPageReaderTool

ExtractorAgent:
  Mock 产品画像 → LLM Structured Output

AnalyzerAgent:
  Mock 分析 → LLM 竞品分析

WriterAgent:
  Mock 报告 → LLM Markdown 报告生成

ReviewerAgent:
  Mock 质检 → 规则校验 + LLM 质检
```

---

## 12.3 不变部分

以下部分不应因接入模型而改变：

```text
1. CompetitiveAnalysisState；
2. DTO / Schema；
3. AgentNode 接口；
4. WorkflowRouter；
5. RepairRouter；
6. 状态机；
7. 最大迭代规则；
8. 人工介入规则。
```

---

## 13. AgentNode 接口建议

所有 Agent 实现统一接口：

```java
public interface AgentNode {

    AgentType getAgentType();

    CompetitiveAnalysisState execute(CompetitiveAnalysisState state);
}
```

约定：

```text
1. execute 方法读取 State；
2. execute 方法更新 State；
3. execute 方法返回 State；
4. Agent 内部不得直接跳转流程；
5. 流程跳转统一交给 WorkflowRouter。
```

---

## 14. 错误处理原则

---

## 14.1 Agent 执行异常

如果某个 Agent 执行异常：

```text
1. 捕获异常；
2. 记录 AgentRun 日志；
3. 将任务状态设置为 FAILED；
4. 返回错误信息；
5. 后续可支持重试。
```

Mock 阶段可以先不实现完整异常日志，但应预留异常处理位置。

---

## 14.2 无法路由

如果 WorkflowRouter 无法判断回退目标：

```text
status = WAITING_HUMAN_REVIEW
```

---

## 14.3 关键 State 缺失

如果某个 Agent 缺少必要输入：

示例：

```text
ExtractorAgent 缺少 rawSourceSet
AnalyzerAgent 缺少 productProfileSet
WriterAgent 缺少 competitiveAnalysis
```

应抛出 BizException。

---

## 15. 人工介入设计

---

## 15.1 进入人工介入的条件

```text
1. 自动修复次数达到 maxIterations；
2. 仍存在 high severity issue；
3. 缺少官方来源且自动搜索无法补齐；
4. 出现多个来源之间的事实冲突；
5. Reviewer 无法判断回退 Agent。
```

---

## 15.2 人工介入后支持的操作

后续版本可以支持：

```text
1. 用户补充来源 URL；
2. 用户手动修改 ProductProfile 字段；
3. 用户忽略低风险 Issue；
4. 用户增加一次自动修复机会；
5. 用户重新运行某个 Agent。
```

---

## 15.3 人工介入后的路由

### 用户补充 URL

```text
WebPageReaderTool
  ↓
CollectorAgent
  ↓
ExtractorAgent
  ↓
AnalyzerAgent
  ↓
WriterAgent
  ↓
ReviewerAgent
```

### 用户修改 ProductProfile

```text
AnalyzerAgent
  ↓
WriterAgent
  ↓
ReviewerAgent
```

### 用户修改报告文字

```text
ReviewerAgent
```

### 用户忽略低风险 Issue

```text
如果无 high issue：
  status = COMPLETED_WITH_WARNINGS
```

---

## 16. 当前版本边界

V1 工作流只定义：

```text
1. 多 Agent 执行顺序；
2. State 传递规则；
3. Reviewer 质检结果处理；
4. WorkflowRouter 路由逻辑；
5. RepairRouter 修复指令生成；
6. Mock 阶段执行标准。
```

V1 不包含：

```text
1. 真实大模型调用；
2. 真实搜索工具；
3. 数据库持久化；
4. 前端页面；
5. SSE 任务进度推送；
6. AgentRun Trace 完整记录；
7. 多任务并发调度；
8. 用户权限系统。
```

这些内容在后续文档中定义。

---

## 17. 本文档对应开发任务

对应 `09_development_tasks.md` 中的任务：

```text
Task 02：实现 WorkflowRouter、RepairRouter、AgentNode 接口与 Mock Agent
```

验收标准：

```text
1. 项目可编译；
2. CompetitiveAnalysisGraph.runMockDemo() 可执行；
3. 第一次 Reviewer 不通过；
4. 自动生成 RepairInstruction；
5. 自动回退 CollectorAgent；
6. 第二次 Reviewer 通过；
7. 最终状态为 COMPLETED；
8. State 中包含完整中间产物。
```
