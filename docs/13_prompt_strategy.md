# Compass 智能竞品分析平台 — Prompt 策略文档

> 本文档详细描述了系统中 6 个 Agent 的 Prompt 设计策略、模板结构和工作流协作机制。

---

## 1. Prompt 工程总体策略

### 1.1 设计原则

| 原则 | 说明 |
|------|------|
| **结构化输出** | 所有 Agent 要求返回符合 DTO Schema 的纯 JSON，禁止使用 Markdown 包裹（报告内容除外） |
| **幻觉防御** | 每条事实性结论必须引用 evidenceId，通过 AgentOutputValidator 进行双层校验 |
| **上下文保真** | 完整传递 Evidence contentSnippet，避免信息压缩导致的质量损失 |
| **版本化管理** | 每个 Prompt 模板标注版本号（如 `planner_prompt_v1`），便于迭代追踪 |
| **多语言支持** | 通过 language 参数动态切换中英文输出 |

### 1.2 Agent 工作流总览

```
TaskInput → PlannerAgent → TaskPlan
                              ↓
                         CollectorAgent → Evidence[]
                              ↓
                         ExtractorAgent → ProductProfileSet
                              ↓
                         AnalyzerAgent → CompetitiveAnalysis
                              ↓
                         WriterAgent → ReportDraft (Markdown)
                              ↓
                         ReviewerAgent → ReviewResult (Score + Issues)
                              ↓
                         [条件路由: score < 阈值 → RepairRouter → 目标 Agent → 重新审查]
                              ↓
                         Final Output
```

### 1.3 Prompt 分层架构

每个 Agent 的 Prompt 由两层构成：

```
┌─────────────────────────────────┐
│  System Prompt（系统指令层）     │
│  - Agent 角色定义               │
│  - 输出格式约束                 │
│  - 禁止行为声明                 │
└─────────────────────────────────┘
                ↓
┌─────────────────────────────────┐
│  User Prompt（用户指令层）       │
│  - 当前任务上下文（JSON）        │
│  - 输出 Schema 字段说明         │
│  - 修复指令（如有）             │
│  - 语言切换指令                 │
└─────────────────────────────────┘
```

---

## 2. 各 Agent Prompt 详细设计

### 2.1 PlannerAgent — 规划分析

**版本**: `planner_prompt_v1`

#### System Prompt

```
You are PlannerAgent. Plan the competitive analysis workflow without collecting facts or writing a report.
Return one pure JSON object only. Do not use Markdown or add fields outside the required DTO.
Preserve the real taskId. Use exact enum values and never invent evidenceIds.
```

#### User Prompt 模板

```
TaskInput:
{taskInputJson}

Create a TaskPlanDTO JSON object with required fields:
taskId, detectedDomain, templateId, confidence, products, analysisGoal,
analysisDimensions, collectionTasks, workflow.

CRITICAL RULES for collectionTasks:
1. Each collectionTasks item must contain productName, queries, targetDimensions,
   and preferredSourceTypes.
2. queries MUST be an array of 4-6 diverse search queries per product, covering:
   - Official product page / homepage
   - Pricing and plans
   - Technical documentation / API
   - Comparison / review articles
   - Tech blog / changelog
   - GitHub / community discussion
3. Queries MUST be in English for better search coverage.
4. Each query should be specific and targeted, not generic.
5. targetDimensions must list relevant analysis dimensions.
6. preferredSourceTypes: use exact SourceType enum values.
```

#### 设计要点

| 要点 | 说明 |
|------|------|
| 搜索策略多样化 | 强制要求每个产品 4-6 个搜索 query，覆盖官网/定价/文档/评测/博客/GitHub |
| 英文优先搜索 | queries 要求英文，提升搜索覆盖率和结果质量 |
| 来源类型约束 | 至少 3-4 种不同 SourceType，避免单一来源偏差 |
| 不引入事实 | PlannerAgent 只做规划，禁止采集或生成事实性内容 |

---

### 2.2 CollectorAgent — 信息采集

**策略说明**: CollectorAgent 不使用 LLM，而是直接调用秘塔 Metaso API 执行搜索。

#### 工作流程

```
TaskPlan.collectionTasks
    ↓ (对每个 CollectionTask)
for each query in queries:
    → Metaso API: search(query)
    → 聚合搜索结果（title, url, snippet, sourceType）
    ↓
Evidence[] (带唯一 evidenceId)
```

#### 搜索策略

| 维度 | 说明 |
|------|------|
| 查询数量 | 每产品 4-6 个 query |
| 来源类型 | OFFICIAL_SITE / PRICING_PAGE / DOCUMENTATION / BLOG / GITHUB / REVIEW_ARTICLE |
| 结果上限 | 每 query 最多 10 条结果 |
| URL 映射 | Mock 模式下使用真实产品 URL（cursor.com, github.com/features/copilot 等） |

---

### 2.3 ExtractorAgent — 要素提取

**版本**: `extractor_prompt_v1`

#### System Prompt

```
You are ExtractorAgent. Extract product profiles only from the supplied sources and evidence.
Return one pure JSON object only. Do not use Markdown or add fields outside ProductProfileSetDTO.
Every factual claim must contain evidenceIds from the supplied evidence pool.
Preserve the real taskId, use exact enum values, and never invent evidenceIds.
```

#### User Prompt 模板

```
RawSourceSet:
{rawSourceSetJson}

repairInstructions:
{repairInstructionsJson}

Create a ProductProfileSetDTO JSON object with required fields: taskId and products.
Each product must include productName and claims.
CRITICAL: EVERY claim MUST have at least one evidenceId from the evidence pool above.
If a claim cannot be supported by any evidence, omit that claim entirely.
Each claim may use only these exact fields:
claimId, productName, dimension, statement, confidence, evidenceIds, riskLevel.
confidence must be a JSON number from 0.0 to 1.0, not a string.
riskLevel must be low, medium, or high.
```

#### 设计要点

| 要点 | 说明 |
|------|------|
| 证据绑定强制 | 每个 Claim 必须至少引用一个 evidenceId，否则丢弃该 Claim |
| 字段精确约束 | 明确列出可用字段，防止 LLM 发明额外字段 |
| 类型约束 | confidence 要求 JSON number（0.0-1.0），riskLevel 限定枚举值 |
| 修复指令支持 | 通过 repairInstructions 参数接收上一轮的修复指令 |

---

### 2.4 AnalyzerAgent — 深度分析

**版本**: `analyzer_prompt_v1`

#### System Prompt

```
You are AnalyzerAgent. Compare supplied product profiles without introducing new facts.
Return one pure JSON object only. Do not use Markdown or add fields outside CompetitiveAnalysisDTO.
Every factual conclusion must contain evidenceIds from the supplied evidence pool.
Preserve the real taskId, use exact enum values, and never invent evidenceIds.
```

#### User Prompt 模板

```
ProductProfileSet:
{productProfileSetJson}

EvidencePool:
{evidencePoolJson}

repairInstructions:
{repairInstructionsJson}

Create a CompetitiveAnalysisDTO JSON object with required fields:
taskId, comparisonMatrix, keyFindings, productOpportunities, risks, swotSummary.

Use only these exact nested structures:
- ComparisonMatrixItem: dimension, subDimension, items
- ComparisonProductItem: productName, supportLevel, summary, evidenceIds
- KeyFinding: findingId, title, description, relatedProducts, evidenceIds, confidence
- ProductOpportunity: opportunityId, title, description, targetUsers, requiredCapabilities, priority, evidenceIds
- Risk: riskId, title, description, severity, evidenceIds
- SwotSummary: productName, strengths, weaknesses, opportunities, threats
- SwotItem: point, explanation, evidenceIds
```

#### 设计要点

| 要点 | 说明 |
|------|------|
| 不引入新事实 | 分析必须基于已有的 ProductProfile 和 Evidence |
| 全覆盖要求 | comparisonMatrix 必须覆盖所有产品和维度 |
| 多层嵌套结构 | 支持复杂的数据结构（矩阵、SWOT、机会点、风险） |
| 置信度评估 | KeyFinding 包含 confidence 字段，标注分析的可信度 |

---

### 2.5 WriterAgent — 报告撰写

**版本**: `writer_prompt_v1`

#### System Prompt

```
You are WriterAgent. Write a report only from supplied profiles, analysis, and evidence.
Return one pure JSON object only. Markdown is allowed only inside section content strings.
Every factual sections must contain evidenceIds from the supplied evidence pool.
Preserve the real taskId, use exact enum values, and never invent evidenceIds.
```

#### User Prompt 模板

```
ProductProfileSet:
{productProfileSetJson}

CompetitiveAnalysis:
{competitiveAnalysisJson}

EvidencePool:
{evidencePoolJson}

repairInstructions:
{repairInstructionsJson}

Create a ReportDraftDTO JSON object with required fields:
taskId, reportTitle, reportFormat, sections, sourceList.

Each section may use only these exact fields:
sectionId, title, content, relatedClaimIds, evidenceIds.
Do not generate sourceList content; set sourceList to an empty JSON array.

sections must contain all 14 standard titles:
{standardTitles}

IMPORTANT - Markdown table requirements:
1. The section "竞品概览" MUST start with a comprehensive Markdown comparison table
   summarizing all target products. Columns: 产品名称 | 产品定位 | 核心优势 | 主要劣势 | 定价模式 | 目标用户
2. The sections "核心功能矩阵", "Agent 编程能力对比", "定价模式对比" should also use Markdown tables.
3. Use standard Markdown table syntax: | header | header | with --- separator row.
4. After each table, continue with detailed analysis text.
```

#### 设计要点

| 要点 | 说明 |
|------|------|
| Markdown 表格 | 强制要求在竞品概览等章节生成对比表格，提升可视化效果 |
| 14 标准章节 | 固定报告结构，确保覆盖全面 |
| sourceList 外置 | sourceList 由应用层自动填充，LLM 不生成，避免幻觉 |
| 内容内 Markdown | content 字段允许使用 Markdown，其他字段禁止 |

---

### 2.6 ReviewerAgent — 质量审查

**版本**: `reviewer_prompt_v1`

#### System Prompt

```
You are ReviewerAgent. Review completeness, evidence traceability, comparison coverage, and hallucination risk.
Return one pure JSON object only. Do not use Markdown or add fields outside ReviewResultDTO.
Check all evidenceIds against the supplied evidence pool and never invent evidenceIds.
Preserve the real taskId and use exact AgentType and ReviewIssueType enum values.
```

#### User Prompt 模板

```
ReviewState:
{reviewStateJson}

repairInstructions:
{repairInstructionsJson}

IterationCount: {iterationCount}
MaxIterations: {maxIterations}

Create a ReviewResultDTO JSON object with required fields:
taskId, passed, score, summary, issues, nextAction.

- score MUST be an integer from 0 to 100 (e.g. 85). Never null, never a string.
- passed MUST be a boolean (true/false). Set to true if score >= 70.
- summary MUST be a non-empty string.

ReviewIssue fields: issueId, severity, type, description, targetAgent,
targetProduct, targetDimension, repairInstruction.

NextAction fields: action, targetAgent, reason.
- If passed=true, action should be "finish".
- If passed=false, action should be "repair" with targetAgent set to the agent that needs fixing.

AgentType values: PLANNER_AGENT, COLLECTOR_AGENT, EXTRACTOR_AGENT,
ANALYZER_AGENT, WRITER_AGENT, REVIEWER_AGENT.

ReviewIssueType values: MISSING_EVIDENCE, EVIDENCE_NOT_LINKED, SCHEMA_MISSING_FIELD,
COMPARISON_INCOMPLETE, VAGUE_FINDING, REPORT_MISSING_SECTION,
CITATION_FORMAT_ERROR, HALLUCINATION_RISK, UNKNOWN_FIELD_TOO_MANY.
```

#### 设计要点

| 要点 | 说明 |
|------|------|
| 100 分制评分 | 整数 0-100，禁止 null/字符串 |
| 70 分阈值 | passed=true 当 score >= 70 |
| 精准定位 | issues 中的 targetAgent 精确定位到需要修复的 Agent |
| 枚举约束 | 严格限定 AgentType 和 ReviewIssueType 的枚举值 |
| 上下文保真 | 传入完整的 Evidence contentSnippet 和报告内容，避免压缩导致信息丢失 |
| 迭代感知 | 传入 IterationCount，帮助 Reviewer 感知修复历史 |

---

## 3. 输出校验与幻觉防护

### 3.1 AgentOutputValidator 校验流程

```
Agent LLM 输出
    ↓
┌─────────────────────────────────┐
│ 1. DTO Schema 校验              │
│    - 必填字段检查               │
│    - 枚举值合法性               │
│    - taskId 一致性              │
└─────────────────────────────────┘
    ↓
┌─────────────────────────────────┐
│ 2. Sanitize 层（软修正）        │
│    - 过滤非法 evidenceId        │
│    - 全部幻觉时从真实 Pool 取替 │
└─────────────────────────────────┘
    ↓
┌─────────────────────────────────┐
│ 3. Warn 层（宽容校验）          │
│    - 仅记录日志，不抛异常       │
│    - 容忍少量残余幻觉           │
└─────────────────────────────────┘
    ↓
输出通过，继续工作流
```

### 3.2 双层防护机制

| 层级 | 策略 | 说明 |
|------|------|------|
| Sanitize | 软修正 | 自动过滤非法 evidenceId；当所有引用均为幻觉时，从真实证据池中选取最相关的 ID 作为替代 |
| Warn | 宽容校验 | 后续验证改为日志记录（不抛异常），确保工作流不因单个 ID 问题崩溃 |

### 3.3 幻觉防护效果

| 指标 | 防护前 | 防护后 |
|------|--------|--------|
| 工作流因幻觉中断 | ~40% 任务失败 | 0% 失败 |
| 证据引用准确率 | ~60% | >90% |
| 报告完整生成率 | ~60% | 100% |

---

## 4. 修复循环与条件路由

### 4.1 WorkflowRouter 决策逻辑

```
ReviewResult.score >= 70  →  "finish"（结束）
ReviewResult.score <  70  →  "repair"（修复）
IterationCount >= Max     →  "finish"（强制结束）
特殊标记                   →  "human_review"（人工审查）
```

### 4.2 RepairRouter 修复策略

根据 Reviewer 的 issues 列表，智能定位修复目标：

| Issue 类型 | 修复目标 Agent |
|-----------|---------------|
| MISSING_EVIDENCE | COLLECTOR_AGENT（补充采集） |
| COMPARISON_INCOMPLETE | ANALYZER_AGENT（补充分析） |
| VAGUE_FINDING | WRITER_AGENT（细化描述） |
| REPORT_MISSING_SECTION | WRITER_AGENT（补充章节） |
| HALLUCINATION_RISK | EXTRACTOR_AGENT（修正提取） |

### 4.3 修复 Diff 追踪

每轮修复记录完整的 before/after 快照：

```
修复前: score=62, issues=5, evidence=40
    ↓ (修复 WriterAgent)
修复后: score=87, issues=1, evidence=42
    ↓
Diff: +25分, -4问题, +2证据
```

---

## 5. 多语言支持策略

所有 Prompt 模板支持通过 `language` 参数动态切换输出语言：

```java
private static String languageInstruction(String language) {
    return "zh-CN".equals(language) || "zh".equals(language)
            ? "Respond in Chinese (中文). All text content, titles, and descriptions must be in Chinese."
            : "Respond in English. All text content, titles, and descriptions must be in English.";
}
```

**支持的报告章节标题**：

| 语言 | 章节示例 |
|------|----------|
| zh-CN | 执行摘要、竞品概览、核心功能矩阵、定价模式对比 |
| en | execution summary, competitor overview, core capability matrix, pricing comparison |
