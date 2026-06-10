

# AI 驱动的竞品分析 Agent 协作系统项目总览

版本：V1.0  
项目名称：CA_Agent  
主 Demo：AI 编程工具竞品分析  
技术栈：Spring Boot + Spring AI Alibaba + MySQL + 前端可视化  
目标场景：面向企业产品团队的自动化竞品分析系统

---

## 1. 项目背景

在企业产品研发、战略分析和技术选型过程中，竞品分析是一项高频但耗时的工作。传统竞品分析通常需要产品经理或分析人员手动完成以下流程：

```text
搜索公开资料
↓
阅读官网、文档、定价页、测评文章、用户评论
↓
整理产品功能、定价、用户画像和优劣势
↓
形成对比表格
↓
撰写分析报告
↓
人工核对来源和事实
````

该流程存在以下问题：

```text
1. 信息分散，采集成本高；
2. 分析维度不统一，报告结构不稳定；
3. 结论容易缺少来源支撑；
4. 人工整理效率低；
5. 多次分析难以沉淀为结构化知识；
6. 报告可信度依赖个人经验；
7. 后续复用和更新成本高。
```

因此，本项目希望构建一个 **AI 驱动的竞品分析 Agent 协作系统**，通过多个专职 Agent 协作完成公开信息采集、结构化抽取、竞品分析、报告生成和质检回退，提升竞品分析效率、可信度和可复用性。

---

## 2. 赛题理解

本项目对应赛题为：

```text
AI 驱动的竞品分析 Agent 协作系统
```

赛题核心不是简单生成一篇竞品分析报告，而是构建一个可以模拟企业调研小组的多 Agent 协作系统。

系统应具备以下能力：

```text
1. 多 Agent 分工协作；
2. 自动采集公开竞品信息；
3. 将非结构化资料转化为结构化竞品知识；
4. 生成横向竞品分析；
5. 自动生成分析报告；
6. 对报告进行事实核验和质量检查；
7. 支持质检失败后的自动回退修复；
8. 支持信息来源追溯；
9. 支持后续扩展到不同业务领域。
```

因此，本项目的设计重点是：

```text
多 Agent 工作流
结构化 Schema
证据链追踪
质检反馈闭环
领域模板扩展
工程化可落地
```

---

## 3. 项目定位

本项目定位为：

> 面向企业产品团队的多 Agent 竞品分析协作平台。

系统不是单纯的 Chatbot，也不是固定模板填报工具，而是一个以 Agent 工作流为核心的自动化分析系统。

系统通过以下方式完成竞品分析：

```text
用户创建分析任务
↓
Planner Agent 进行任务规划
↓
Collector Agent 采集公开资料
↓
Extractor Agent 抽取结构化产品画像
↓
Analyzer Agent 进行横向竞品分析
↓
Writer Agent 生成分析报告
↓
Reviewer Agent 进行质检
↓
通过则输出最终报告
不通过则生成修复指令并回退指定 Agent
```

---

## 4. 主 Demo 场景

本项目主 Demo 选择：

```text
AI 编程工具竞品分析
```

主 Demo 竞品对象包括：

```text
1. Cursor
2. Windsurf
3. GitHub Copilot
4. 通义灵码
```

选择该场景的原因：

```text
1. AI 编程工具与 AIGC、Agent、开发者工具高度相关；
2. 公开资料丰富，便于采集官网、文档、定价页、用户评价等信息；
3. 竞品差异明显，适合做功能矩阵和产品定位对比；
4. 技术特征明显，适合展示 Agent 编程能力、代码库理解能力、模型能力等分析维度；
5. 适合计算机专业团队理解和答辩；
6. 可以自然扩展到企业研发效率工具和内部信息系统场景。
```

---

## 5. 扩展场景

虽然主 Demo 是 AI 编程工具，但系统不是为单一领域写死的。

系统通过：

```text
通用 Agent 工作流
+
领域模板配置
+
结构化 Schema
+
动态采集与质检机制
```

支持扩展到更多企业软件场景，例如：

```text
1. 协同办公产品
2. HR SaaS
3. 知识库产品
4. 低代码平台
5. CRM 系统
6. 企业采购系统
7. 财务系统
8. 审批系统
9. DevOps 平台
10. 数据分析平台
```

扩展方式为：

```text
替换领域分析维度
替换领域 Schema
替换采集来源策略
替换报告模板
替换质检规则
```

对于未知领域，后续可由 Planner Agent 自动生成候选领域模板，并由人工或 Reviewer Agent 确认后执行。

---

## 6. 系统核心能力

### 6.1 多 Agent 协作

系统包含六类核心 Agent：

| Agent          | 职责                    |
| -------------- | --------------------- |
| PlannerAgent   | 识别领域、选择模板、生成任务计划      |
| CollectorAgent | 采集公开资料，生成证据池          |
| ExtractorAgent | 抽取结构化产品画像和 Claim      |
| AnalyzerAgent  | 生成对比矩阵、关键发现、机会点和 SWOT |
| WriterAgent    | 生成 Markdown 竞品分析报告    |
| ReviewerAgent  | 检查字段、证据、报告结构和幻觉风险     |

---

### 6.2 结构化竞品知识

系统不会只输出一篇自然语言报告，而是沉淀结构化竞品知识。

核心结构包括：

```text
TaskInputDTO
TaskPlanDTO
RawSourceSetDTO
Evidence
Claim
ProductProfileSetDTO
CompetitiveAnalysisDTO
ReportDraftDTO
ReviewResultDTO
RepairInstructionDTO
CompetitiveAnalysisState
```

其中最关键的是：

```text
Evidence：证据来源
Claim：结构化结论
ProductProfile：产品画像
ReviewResult：质检结果
RepairInstruction：修复指令
```

---

### 6.3 证据链追踪

系统要求所有事实性结论都可以追溯到公开来源。

证据链结构为：

```text
ReportSection
  ↓
Claim
  ↓
Evidence
  ↓
URL / Source
```

这可以避免大模型直接生成无依据结论，提高报告可信度。

---

### 6.4 质检反馈闭环

ReviewerAgent 会对系统输出进行质量检查。

检查内容包括：

```text
1. 字段是否完整；
2. 证据是否充分；
3. 核心结论是否绑定 evidenceIds；
4. 报告章节是否完整；
5. 对比矩阵是否覆盖所有竞品；
6. 是否存在无证据价格、排名、模型名称等幻觉风险；
7. unknown 字段比例是否过高。
```

如果质检失败，系统生成 `RepairInstructionDTO`，并根据问题类型回退到对应 Agent。

回退规则：

| 问题类型        | 回退 Agent                       |
| ----------- | ------------------------------ |
| 缺少证据        | CollectorAgent                 |
| 证据未绑定       | ExtractorAgent / AnalyzerAgent |
| Schema 字段缺失 | ExtractorAgent                 |
| 对比不完整       | AnalyzerAgent                  |
| 报告缺少章节      | WriterAgent                    |
| 引用格式错误      | WriterAgent                    |
| 幻觉风险        | AnalyzerAgent / WriterAgent    |

---

### 6.5 可观测性

系统会记录 Agent 执行过程，包括：

```text
1. 当前任务状态；
2. 当前执行 Agent；
3. 每个 Agent 的输入输出；
4. 每个 Agent 的执行耗时；
5. 每次质检结果；
6. 每次回退修复指令；
7. 最终报告；
8. 证据来源列表。
```

后续可以将这些信息展示在前端页面中，形成可视化执行 Trace。

---

## 7. 技术架构

### 7.1 后端技术栈

```text
Java 17
Spring Boot 3.x
Spring AI Alibaba
Maven
MySQL
Redis，可选
Lombok
Jackson
```

### 7.2 Agent 与工作流

第一阶段采用 Mock Agent 跑通流程。

后续接入 Spring AI Alibaba，实现真实模型调用和工具调用。

整体技术路线：

```text
Spring Boot
  ↓
WorkflowService / CompetitiveAnalysisGraph
  ↓
PlannerAgent / CollectorAgent / ExtractorAgent / AnalyzerAgent / WriterAgent / ReviewerAgent
  ↓
Spring AI Alibaba ChatClient / Tool Calling
  ↓
WebSearchTool / WebPageReaderTool / EvidenceStoreTool
  ↓
MySQL 持久化 Evidence、Claim、Report、AgentRun
```

---

## 8. 项目包结构

当前后端包结构如下：

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

## 9. 核心工作流

系统核心工作流为：

```text
START
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
  ├── END
  ├── REPAIR
  └── HUMAN_REVIEW
```

详细流程：

```text
用户提交任务
↓
创建 TaskInputDTO
↓
PlannerAgent 生成 TaskPlanDTO
↓
CollectorAgent 生成 RawSourceSetDTO 和 Evidence
↓
ExtractorAgent 生成 ProductProfileSetDTO 和 Claim
↓
AnalyzerAgent 生成 CompetitiveAnalysisDTO
↓
WriterAgent 生成 ReportDraftDTO
↓
ReviewerAgent 生成 ReviewResultDTO
↓
如果通过，任务完成
↓
如果不通过，RepairRouter 生成 RepairInstructionDTO
↓
根据问题类型回退 Collector / Extractor / Analyzer / Writer
↓
重新执行下游 Agent
↓
再次质检
```

---

## 10. MVP 范围

第一版 MVP 的目标是：

```text
先跑通完整多 Agent 工作流闭环。
```

MVP 必须完成：

```text
1. DTO / Schema / State 定义；
2. AgentNode 统一接口；
3. 六个 Mock Agent；
4. CompetitiveAnalysisGraph 工作流执行器；
5. WorkflowRouter 路由逻辑；
6. RepairRouter 修复指令生成；
7. 第一次 Reviewer 不通过；
8. 自动回退 CollectorAgent；
9. 第二次 Reviewer 通过；
10. 最终生成完整 State；
11. 通过接口返回最终报告和质检结果。
```

MVP 暂不包含：

```text
1. 真实大模型调用；
2. 真实搜索；
3. 复杂数据库持久化；
4. 用户登录权限；
5. 文件导出；
6. SSE 实时推送；
7. 前端复杂可视化。
```

---

## 11. 后续迭代计划

### 阶段一：Mock 工作流闭环

目标：

```text
验证多 Agent 状态流转和回退机制。
```

产出：

```text
1. DTO / Schema；
2. Mock Agent；
3. WorkflowRouter；
4. RepairRouter；
5. CompetitiveAnalysisGraph；
6. 简单 Controller 接口。
```

---

### 阶段二：接口与前端展示

目标：

```text
通过 HTTP API 创建任务、查看报告、查看证据、查看质检结果。
```

产出：

```text
1. AnalysisTaskController；
2. TaskService；
3. WorkflowService；
4. ReportService；
5. 基础前端页面。
```

---

### 阶段三：数据库持久化

目标：

```text
将任务、报告、证据、质检结果、Agent 执行日志存入 MySQL。
```

产出：

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

### 阶段四：接入 Spring AI Alibaba

目标：

```text
用真实模型替换 Mock Agent。
```

产出：

```text
1. ChatClient 配置；
2. Agent Prompt；
3. Structured Output；
4. Tool Calling；
5. 真实 Planner / Extractor / Analyzer / Writer / Reviewer。
```

---

### 阶段五：接入公开信息采集工具

目标：

```text
实现真实公开资料采集。
```

产出：

```text
1. WebSearchTool；
2. WebPageReaderTool；
3. SourceRankTool；
4. EvidenceStoreTool。
```

---

### 阶段六：产品化与答辩包装

目标：

```text
形成可演示、可答辩、可扩展的完整系统。
```

产出：

```text
1. 前端页面；
2. Agent 执行流程可视化；
3. 报告展示页；
4. 证据来源页；
5. 质检结果页；
6. Demo 脚本；
7. 答辩 PPT。
```

---

## 12. 预期演示流程

比赛或项目展示时，主 Demo 流程如下：

```text
1. 用户进入系统首页；
2. 创建竞品分析任务；
3. 输入竞品：
   Cursor
   Windsurf
   GitHub Copilot
   通义灵码
4. 点击开始分析；
5. 系统展示 Agent 执行过程；
6. 展示 Planner 生成的任务计划；
7. 展示 Collector 生成的证据池；
8. 展示 Extractor 生成的产品画像；
9. 展示 Analyzer 生成的对比矩阵；
10. 展示 Writer 生成的报告；
11. 展示 Reviewer 第一次发现问题；
12. 展示系统自动生成 RepairInstruction；
13. 展示自动回退和二次质检通过；
14. 展示最终报告；
15. 展示证据来源和质检结果。
```

---

## 13. 项目亮点

### 13.1 多 Agent 分工明确

不同 Agent 分别负责规划、采集、抽取、分析、写作、质检，避免单模型一次性生成报告导致不可控。

---

### 13.2 结构化 Schema 驱动

所有中间结果都通过 DTO 和 Schema 传递，使系统具备稳定性、可测试性和可扩展性。

---

### 13.3 证据链可追溯

每条核心结论都绑定 evidenceIds，可以追溯到原始 URL 和证据片段。

---

### 13.4 质检反馈闭环

ReviewerAgent 可以发现缺证据、缺字段、报告缺章节、幻觉风险等问题，并触发自动回退修复。

---

### 13.5 领域模板可扩展

系统不是写死 AI 编程工具领域，而是可以通过领域模板扩展到协同办公、HR SaaS、知识库等企业软件场景。

---

### 13.6 Java Agent 工程化

项目基于 Spring Boot 和 Spring AI Alibaba 构建，更贴近企业级 Java 技术栈和后端工程实践。

---

## 14. 与传统竞品分析方式对比

| 维度   | 传统人工竞品分析 | 本系统                    |
| ---- | -------- | ---------------------- |
| 信息采集 | 人工搜索     | CollectorAgent 自动采集    |
| 分析维度 | 依赖个人经验   | 领域模板统一约束               |
| 数据结构 | 文档为主     | 结构化 Schema             |
| 结论来源 | 容易缺失     | Evidence 证据链           |
| 报告生成 | 人工撰写     | WriterAgent 自动生成       |
| 质量检查 | 人工复核     | ReviewerAgent 自动质检     |
| 修复机制 | 人工返工     | RepairInstruction 自动回退 |
| 复用能力 | 较弱       | 可沉淀为结构化知识库             |
| 扩展能力 | 低        | 可扩展不同领域模板              |

---

## 15. 当前版本边界

当前 V1 阶段重点是：

```text
多 Agent 工作流闭环
```

不追求一次性完成全部功能。

当前版本不包含：

```text
1. 完整线上部署；
2. 真实商业数据采集；
3. 内部企业数据接入；
4. 用户权限系统；
5. 多租户隔离；
6. 大规模并发任务调度；
7. 高级可观测平台；
8. 自动生成 PPT；
9. 私有知识库 RAG。
```

这些能力可作为后续版本扩展。

---

## 16. 相关文档

本项目文档体系如下：

```text
docs/
├── 00_project_overview.md        # 项目总览
├── 01_product_requirement.md     # 产品需求
├── 02_system_architecture.md     # 系统架构
├── 03_domain_schema.md           # 领域 Schema
├── 04_agent_workflow.md          # Agent 工作流
├── 05_agent_prompt_spec.md       # Agent Prompt 规格
├── 06_api_spec.md                # API 规格
├── 07_database_spec.md           # 数据库设计
├── 08_frontend_spec.md           # 前端页面设计
├── 09_development_tasks.md       # 开发任务拆分
├── 10_demo_script.md             # 演示脚本
└── 11_competition_materials.md   # 比赛材料
```

其中当前最核心的文档为：

```text
03_domain_schema.md
04_agent_workflow.md
05_agent_prompt_spec.md
09_development_tasks.md
```

---

## 17. 总结

本项目的核心目标是构建一个：

```text
面向企业产品团队的 AI 驱动竞品分析多 Agent 协作系统。
```

系统通过：

```text
多 Agent 分工
结构化 Schema
证据链追踪
质检反馈闭环
领域模板扩展
Spring Boot 工程化实现
```

实现从公开信息采集到竞品分析报告生成的自动化流程。

主 Demo 聚焦 AI 编程工具竞品分析，后续可扩展到协同办公、HR SaaS、知识库等企业软件场景。

```
```
