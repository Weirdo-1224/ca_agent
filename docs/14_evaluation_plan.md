# Compass 智能竞品分析平台 — 评测方案与样例

> 本文档描述了系统的评测方法、输入输出样例和评估指标，用于验证系统功能和输出质量。

---

## 1. 评测总体方案

### 1.1 评测维度

| 维度 | 说明 | 权重 |
|------|------|------|
| **功能正确性** | 6 Agent 链路是否完整执行，报告是否生成 | 30% |
| **内容质量** | 报告内容的完整性、准确性、深度 | 30% |
| **证据可追溯** | 结论是否可追溯到原始来源 | 20% |
| **幻觉控制** | 是否编造不存在的证据或事实 | 10% |
| **修复有效性** | 自动修复是否有效提升报告质量 | 10% |

### 1.2 评测方法

| 方法 | 说明 |
|------|------|
| **自动化评测** | AgentOutputValidator 对每个 Agent 输出进行结构化校验 |
| **LLM 自评估** | ReviewerAgent 对报告进行 100 分制评分 |
| **人工评估** | 评审报告内容的专业性、可读性和实用性 |
| **E2E 端到端测试** | 从任务创建到报告生成的完整流程验证 |

---

## 2. 输入输出样例

### 2.1 样例一：AI 编程工具竞品分析

#### 输入 (TaskInput)

```json
{
  "taskName": "AI 编程助手竞品分析",
  "domain": "AI_CODE_TOOLS",
  "targetProducts": ["Cursor", "GitHub Copilot", "Windsurf"],
  "analysisGoal": "分析当前主流 AI 编程工具的功能特性、定价策略和市场表现",
  "language": "zh-CN",
  "maxIterations": 2
}
```

#### 期望输出

| Agent | 输出 | 验证点 |
|-------|------|--------|
| PlannerAgent | TaskPlanDTO | ✅ 包含 3 个产品的 collectionTask<br>✅ 每个产品 4-6 个搜索 query<br>✅ query 覆盖官网/定价/文档/评测/博客/GitHub |
| CollectorAgent | Evidence[] | ✅ 每产品至少 10 条证据<br>✅ 来源类型多样（≥3 种）<br>✅ URL 真实可访问 |
| ExtractorAgent | ProductProfileSet | ✅ 3 个产品的 Claims<br>✅ 每个 Claim 有 evidenceId<br>✅ confidence 为 0.0-1.0 数字 |
| AnalyzerAgent | CompetitiveAnalysis | ✅ comparisonMatrix 覆盖所有产品<br>✅ SWOT 分析完整<br>✅ 每个结论有 evidenceId |
| WriterAgent | ReportDraft | ✅ 14 个标准章节<br>✅ 竞品概览有对比表格<br>✅ Markdown 格式正确 |
| ReviewerAgent | ReviewResult | ✅ score 为 0-100 整数<br>✅ passed 为 boolean<br>✅ issues 有可操作的修复建议 |

#### 最终报告样例（部分）

```markdown
## 竞品概览

| 产品名称 | 产品定位 | 核心优势 | 主要劣势 | 定价模式 | 目标用户 |
|----------|----------|----------|----------|----------|----------|
| Cursor | AI-first 代码编辑器 | 深度集成 VS Code，Agent 模式强大 | 付费墙较高 | 免费/Pro $20/月 | 专业开发者 |
| GitHub Copilot | AI 编程助手 | GitHub 生态深度整合 | 上下文理解有限 | 免费/Pro $10/月 | GitHub 用户 |
| Windsurf | AI 编程平台 | Cascade 多 Agent 协作 | 生态尚不成熟 | 免费/Pro $15/月 | 团队协作用户 |

### 产品详细分析

**Cursor** 是当前市场上最受关注的 AI-first 代码编辑器...
```

---

### 2.2 样例二：云服务平台竞品分析

#### 输入 (TaskInput)

```json
{
  "taskName": "云平台竞品分析",
  "domain": "CLOUD_SERVICES",
  "targetProducts": ["AWS", "Azure", "GCP"],
  "analysisGoal": "对比三大云平台的计算服务、定价策略和企业服务",
  "language": "zh-CN",
  "maxIterations": 2
}
```

#### 期望输出特征

| 检查项 | 期望 |
|--------|------|
| 报告章节数 | 14 节完整覆盖 |
| 证据数量 | ≥30 条（每产品 ≥10 条） |
| 对比表格 | 至少 4 个 Markdown 表格 |
| Reviewer 评分 | 首次 ≥60 分，修复后 ≥80 分 |
| 幻觉率 | <5%（非法 evidenceId / 总引用数） |

---

## 3. 评估指标体系

### 3.1 ReviewerAgent 评分标准

ReviewerAgent 使用以下标准对报告进行 100 分制评分：

| 评分维度 | 分值 | 评分标准 |
|----------|------|----------|
| **完整性** | 25分 | 14 个标准章节是否全部覆盖 |
| **证据支撑** | 25分 | 每个结论是否有 evidenceId 引用 |
| **对比深度** | 20分 | 对比矩阵是否覆盖所有产品和维度 |
| **可读性** | 15分 | 报告结构是否清晰，表述是否专业 |
| **准确性** | 15分 | 是否存在幻觉风险或事实错误 |

### 3.2 通过率阈值

| 分数区间 | 判定 | 后续动作 |
|----------|------|----------|
| 90-100 | 优秀 | finish |
| 70-89 | 良好 | finish |
| 50-69 | 需修复 | repair（自动触发修复循环） |
| 0-49 | 严重问题 | repair 或 human_review |

### 3.3 修复效果评估

修复循环的预期效果：

| 指标 | 第 1 轮 | 第 2 轮 | 第 3 轮 |
|------|---------|---------|---------|
| 平均评分 | 55-70 | 75-85 | 85-95 |
| 平均问题数 | 5-8 | 1-3 | 0-1 |
| 修复成功率 | 60-80% | 70-90% | 80-100% |

---

## 4. 自动化测试方案

### 4.1 单元测试

| 测试类 | 覆盖内容 | 数量 |
|--------|----------|------|
| PlannerPromptTest | Prompt 模板结构、语言切换 | 3+ |
| ExtractorPromptTest | Prompt 模板、字段约束 | 3+ |
| AgentOutputValidatorTest | 校验逻辑、幻觉防护 | 10+ |
| RepairDiffServiceTest | Diff 计算、快照对比 | 5+ |

### 4.2 集成测试

| 测试类 | 覆盖内容 |
|--------|----------|
| CompetitiveAnalysisGraphTest | 完整 6 Agent 链路执行 |
| WorkflowServiceTest | 异步工作流、状态持久化 |
| AgentRunTracerTest | LLM 调用追踪记录 |
| ReportInspectionTest | 报告内容完整性验证 |

### 4.3 E2E 端到端测试

```java
@Test
void testFullPipeline_CursorVsCopilot() {
    // 输入
    TaskInputDTO input = TaskInputDTO.builder()
        .taskId("test_001")
        .targetProducts(List.of("Cursor", "GitHub Copilot"))
        .analysisGoal("Compare AI coding tools")
        .language("zh-CN")
        .build();
    
    // 执行
    CompetitiveAnalysisState state = graph.run(input);
    
    // 验证
    assertNotNull(state.getReportDraft());
    assertEquals(14, state.getReportDraft().getSections().size());
    assertTrue(state.getReviewResult().getScore() >= 60);
    assertTrue(state.getEvidencePool().size() >= 10);
}
```

### 4.4 Live 测试（真实 LLM 调用）

```bash
# 运行包含 live 标签的测试（需要真实 API Key）
mvn test -Dtest-profile=live
```

Live 测试验证：
- 真实 LLM 输出是否符合 Schema
- 真实搜索结果是否能被正确处理
- 端到端流程是否完整执行

---

## 5. 人工评估检查表

### 5.1 报告质量检查表

| # | 检查项 | 通过标准 |
|---|--------|----------|
| 1 | 执行摘要是否简明扼要 | 200-500 字，涵盖核心发现 |
| 2 | 竞品概览是否有对比表格 | Markdown 表格格式正确 |
| 3 | 功能矩阵是否覆盖主要产品 | 每个产品都有对比 |
| 4 | SWOT 分析是否完整 | 每个产品都有 S/W/O/T 四项 |
| 5 | 定价对比是否清晰 | 表格形式展示各产品定价 |
| 6 | 结论是否有可操作性 | 包含明确的产品选择建议 |
| 7 | 证据链接是否可访问 | URL 指向真实页面 |
| 8 | Markdown 渲染是否正确 | 标题、列表、表格显示正常 |

### 5.2 证据质量检查表

| # | 检查项 | 通过标准 |
|---|--------|----------|
| 1 | 证据来源是否多样 | ≥3 种来源类型 |
| 2 | 证据是否覆盖所有产品 | 每个产品 ≥5 条证据 |
| 3 | contentSnippet 是否有信息量 | 非空且有实际内容 |
| 4 | URL 是否可访问 | 链接指向真实页面 |
| 5 | reliability 标注是否合理 | 官网=HIGH，博客=MEDIUM |

---

## 6. 评测结果记录模板

| 任务 | 产品 | 首次评分 | 修复轮次 | 最终评分 | 证据数 | 章节数 | 幻觉率 |
|------|------|----------|----------|----------|--------|--------|--------|
| AI 编程工具 | Cursor, Copilot, Windsurf | 62 | 2 | 87 | 45 | 14 | 2% |
| 云平台 | AWS, Azure, GCP | 68 | 1 | 82 | 38 | 14 | 3% |
| 设计工具 | Figma, Sketch | 71 | 0 | 71 | 22 | 14 | 1% |
