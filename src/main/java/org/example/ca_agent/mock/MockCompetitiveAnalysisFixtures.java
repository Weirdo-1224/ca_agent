package org.example.ca_agent.mock;

import org.example.ca_agent.dto.agent.ProductProfileSetDTO;
import org.example.ca_agent.dto.agent.ReviewResultDTO;
import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.enums.ReviewIssueType;
import org.example.ca_agent.schema.CapabilityItem;
import org.example.ca_agent.schema.SupportItem;

import java.util.List;

public final class MockCompetitiveAnalysisFixtures {

    public static final String DOMAIN = "AI_CODING_TOOLS";
    public static final String TEMPLATE_ID = "AI_CODING_TOOLS_TEMPLATE_V1";
    public static final double PLANNER_CONFIDENCE = 0.95;
    public static final int FIRST_REVIEW_SCORE = 78;
    public static final int PASSED_REVIEW_SCORE = 90;

    public static final List<String> DEFAULT_PRODUCTS = List.of(
            "Cursor",
            "Windsurf",
            "GitHub Copilot",
            "通义灵码"
    );

    public static final List<String> ANALYSIS_DIMENSIONS = List.of(
            "positioning",
            "target_users",
            "core_capabilities",
            "agent_capabilities",
            "codebase_understanding",
            "pricing"
    );

    public static final List<String> WORKFLOW = List.of(
            "PlannerAgent",
            "CollectorAgent",
            "ExtractorAgent",
            "AnalyzerAgent",
            "WriterAgent",
            "ReviewerAgent"
    );

    public static final List<String> STANDARD_REPORT_SECTIONS = List.of(
            "执行摘要",
            "分析背景",
            "竞品概览",
            "产品定位对比",
            "核心功能矩阵",
            "Agent 编程能力对比",
            "代码库理解能力对比",
            "模型与上下文能力对比",
            "定价模式对比",
            "用户评价与痛点",
            "SWOT 分析",
            "产品机会点",
            "结论与建议",
            "信息来源"
    );

    private MockCompetitiveAnalysisFixtures() {
        // utility class
    }

    public static TaskInputDTO mockTaskInput() {
        TaskInputDTO taskInput = new TaskInputDTO();
        taskInput.setTaskId("task_mock_001");
        taskInput.setTaskName("AI 编程工具竞品分析");
        taskInput.setDomain(DOMAIN);
        taskInput.setTargetProducts(DEFAULT_PRODUCTS);
        taskInput.setAnalysisGoal("生成面向产品团队的 AI 编程工具竞品分析报告");
        taskInput.setOutputFormat("markdown");
        taskInput.setLanguage("zh-CN");
        taskInput.setMaxIterations(2);
        return taskInput;
    }

    public static List<String> collectionQueries(String productName) {
        return List.of(
                productName + " official AI coding tool",
                productName + " pricing",
                productName + " documentation agent coding",
                productName + " codebase understanding"
        );
    }

    public static List<String> collectionTargetDimensions() {
        return List.of(
                "positioning",
                "pricing",
                "agent_capabilities",
                "codebase_understanding"
        );
    }

    public static ProductProfileSetDTO.Positioning positioning(String productName, List<String> evidenceIds) {
        ProductProfileSetDTO.Positioning positioning = new ProductProfileSetDTO.Positioning();
        positioning.setSummary(productName + " is positioned as an AI coding assistant.");
        positioning.setMainScenarios(List.of("code_generation", "codebase_qa"));
        positioning.setDifferentiation("Mock differentiation based on public evidence.");
        positioning.setEvidenceIds(evidenceIds);
        return positioning;
    }

    public static ProductProfileSetDTO.TargetUser targetUser(List<String> evidenceIds) {
        ProductProfileSetDTO.TargetUser targetUser = new ProductProfileSetDTO.TargetUser();
        targetUser.setUserGroup("Developers");
        targetUser.setUseCases(List.of("coding", "debugging"));
        targetUser.setPainPoints(List.of("context switching"));
        targetUser.setEvidenceIds(evidenceIds);
        return targetUser;
    }

    public static ProductProfileSetDTO.CoreCapabilities coreCapabilities(List<String> evidenceIds) {
        ProductProfileSetDTO.CoreCapabilities capabilities = new ProductProfileSetDTO.CoreCapabilities();
        capabilities.setCodeCompletion(capability("true", "high", "Supports code completion.", evidenceIds));
        capabilities.setCodeGeneration(capability("true", "high", "Supports code generation.", evidenceIds));
        capabilities.setCodeExplanation(capability("true", "medium", "Supports code explanation.", evidenceIds));
        capabilities.setRefactoring(capability("partial", "medium", "Supports limited refactoring.", evidenceIds));
        capabilities.setUnitTestGeneration(capability("partial", "medium", "Supports test generation.", evidenceIds));
        capabilities.setDebugAssistance(capability("partial", "medium", "Supports debug assistance.", evidenceIds));
        capabilities.setDocumentationGeneration(capability("partial", "medium", "Supports documentation generation.", evidenceIds));
        return capabilities;
    }

    public static ProductProfileSetDTO.AgentCapabilities agentCapabilities(List<String> evidenceIds) {
        ProductProfileSetDTO.AgentCapabilities capabilities = new ProductProfileSetDTO.AgentCapabilities();
        capabilities.setTaskPlanning(support("partial", "Supports task planning in mock data.", evidenceIds));
        capabilities.setMultiFileEditing(support("partial", "Supports multi-file editing in mock data.", evidenceIds));
        capabilities.setTerminalExecution(support("unknown", "Public evidence is insufficient.", evidenceIds));
        capabilities.setTestRunAndFix(support("partial", "Supports test run and fix in mock data.", evidenceIds));
        capabilities.setCodeReview(support("partial", "Supports code review in mock data.", evidenceIds));
        capabilities.setAutonomousLoop(support("unknown", "Public evidence is insufficient.", evidenceIds));
        return capabilities;
    }

    public static ProductProfileSetDTO.CodebaseUnderstanding codebaseUnderstanding(List<String> evidenceIds) {
        ProductProfileSetDTO.CodebaseUnderstanding understanding = new ProductProfileSetDTO.CodebaseUnderstanding();
        understanding.setRepositoryIndexing(support("partial", "Supports repository indexing in mock data.", evidenceIds));
        understanding.setCrossFileReference(support("partial", "Supports cross-file reference in mock data.", evidenceIds));
        understanding.setProjectQa(support("true", "Supports project-level QA in mock data.", evidenceIds));
        understanding.setLongContextSupport(support("unknown", "Public evidence is insufficient.", evidenceIds));
        return understanding;
    }

    public static ProductProfileSetDTO.IdeEcosystem ideEcosystem(List<String> evidenceIds) {
        ProductProfileSetDTO.SupportedIde ide = new ProductProfileSetDTO.SupportedIde();
        ide.setName("VS Code");
        ide.setSupportType("partial");
        ide.setEvidenceIds(evidenceIds);

        ProductProfileSetDTO.IdeEcosystem ecosystem = new ProductProfileSetDTO.IdeEcosystem();
        ecosystem.setSupportedIdes(List.of(ide));
        ecosystem.setPlatforms(List.of("Windows", "macOS"));
        ecosystem.setIntegrations(List.of());
        return ecosystem;
    }

    public static ProductProfileSetDTO.ModelContext modelContext(List<String> evidenceIds) {
        ProductProfileSetDTO.ContextWindow contextWindow = new ProductProfileSetDTO.ContextWindow();
        contextWindow.setValue("unknown");
        contextWindow.setDescription("Public evidence is insufficient.");
        contextWindow.setEvidenceIds(evidenceIds);

        ProductProfileSetDTO.ModelContext modelContext = new ProductProfileSetDTO.ModelContext();
        modelContext.setSupportedModels(List.of());
        modelContext.setBringYourOwnKey(support("unknown", "Public evidence is insufficient.", evidenceIds));
        modelContext.setLocalModelSupport(support("unknown", "Public evidence is insufficient.", evidenceIds));
        modelContext.setContextWindow(contextWindow);
        return modelContext;
    }

    public static ProductProfileSetDTO.Pricing pricing(List<String> evidenceIds) {
        ProductProfileSetDTO.Plan plan = new ProductProfileSetDTO.Plan();
        plan.setPlanName("Mock Plan");
        plan.setPrice("unknown");
        plan.setBillingCycle("unknown");
        plan.setTargetUser("developers");
        plan.setMainLimits(List.of("mock limit"));
        plan.setEvidenceIds(evidenceIds);

        ProductProfileSetDTO.EnterprisePlan enterprisePlan = new ProductProfileSetDTO.EnterprisePlan();
        enterprisePlan.setAvailable("unknown");
        enterprisePlan.setPricingType("unknown");
        enterprisePlan.setFeatures(List.of());
        enterprisePlan.setEvidenceIds(evidenceIds);

        ProductProfileSetDTO.Pricing pricing = new ProductProfileSetDTO.Pricing();
        pricing.setHasFreePlan("unknown");
        pricing.setPlans(List.of(plan));
        pricing.setEnterprisePlan(enterprisePlan);
        return pricing;
    }

    public static ProductProfileSetDTO.EnterpriseFeatures enterpriseFeatures(List<String> evidenceIds) {
        ProductProfileSetDTO.EnterpriseFeatures features = new ProductProfileSetDTO.EnterpriseFeatures();
        features.setSso(support("unknown", "Public evidence is insufficient.", evidenceIds));
        features.setAdminConsole(support("unknown", "Public evidence is insufficient.", evidenceIds));
        features.setPrivacyControl(support("unknown", "Public evidence is insufficient.", evidenceIds));
        features.setAuditLog(support("unknown", "Public evidence is insufficient.", evidenceIds));
        features.setPrivateDeployment(support("unknown", "Public evidence is insufficient.", evidenceIds));
        return features;
    }

    public static ProductProfileSetDTO.UserFeedback userFeedback(List<String> evidenceIds) {
        ProductProfileSetDTO.FeedbackPoint point = new ProductProfileSetDTO.FeedbackPoint();
        point.setPoint("Mock positive feedback.");
        point.setFrequency("medium");
        point.setEvidenceIds(evidenceIds);

        ProductProfileSetDTO.UserFeedback feedback = new ProductProfileSetDTO.UserFeedback();
        feedback.setPositivePoints(List.of(point));
        feedback.setNegativePoints(List.of());
        feedback.setCommonPainPoints(List.of());
        return feedback;
    }

    public static ProductProfileSetDTO.MissingField missingField(String fieldPath) {
        ProductProfileSetDTO.MissingField missingField = new ProductProfileSetDTO.MissingField();
        missingField.setFieldPath(fieldPath);
        missingField.setReason("Public evidence is insufficient in mock data.");
        return missingField;
    }

    public static ReviewResultDTO failedReview(TaskInputDTO taskInput) {
        ReviewResultDTO.ReviewIssue issue = new ReviewResultDTO.ReviewIssue();
        issue.setIssueId("issue_001");
        issue.setSeverity("high");
        issue.setType(ReviewIssueType.MISSING_EVIDENCE);
        issue.setDescription("通义灵码的定价模式缺少官方来源。");
        issue.setTargetAgent(AgentType.COLLECTOR_AGENT);
        issue.setTargetProduct(taskInput.getTargetProducts().get(taskInput.getTargetProducts().size() - 1));
        issue.setTargetDimension("pricing");
        issue.setRepairInstruction("补充官方定价页或官方文档中的定价信息。");

        ReviewResultDTO.NextAction nextAction = new ReviewResultDTO.NextAction();
        nextAction.setAction("repair");
        nextAction.setTargetAgent(AgentType.COLLECTOR_AGENT);
        nextAction.setReason("存在高优先级缺失证据问题。");

        ReviewResultDTO result = new ReviewResultDTO();
        result.setTaskId(taskInput.getTaskId());
        result.setPassed(false);
        result.setScore(FIRST_REVIEW_SCORE);
        result.setSummary("报告结构基本完整，但存在定价证据不足问题。");
        result.setIssues(List.of(issue));
        result.setNextAction(nextAction);
        return result;
    }

    public static ReviewResultDTO passedReview(String taskId) {
        ReviewResultDTO.NextAction nextAction = new ReviewResultDTO.NextAction();
        nextAction.setAction("finish");
        nextAction.setReason("Mock 二次质检通过。");

        ReviewResultDTO result = new ReviewResultDTO();
        result.setTaskId(taskId);
        result.setPassed(true);
        result.setScore(PASSED_REVIEW_SCORE);
        result.setSummary("Mock 二次质检通过。");
        result.setIssues(List.of());
        result.setNextAction(nextAction);
        return result;
    }

    private static CapabilityItem capability(String supported, String maturity, String description, List<String> evidenceIds) {
        CapabilityItem item = new CapabilityItem();
        item.setSupported(supported);
        item.setMaturity(maturity);
        item.setDescription(description);
        item.setEvidenceIds(evidenceIds);
        return item;
    }

    private static SupportItem support(String supported, String description, List<String> evidenceIds) {
        SupportItem item = new SupportItem();
        item.setSupported(supported);
        item.setDescription(description);
        item.setEvidenceIds(evidenceIds);
        return item;
    }
}
