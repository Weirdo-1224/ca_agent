package org.example.ca_agent.agent;

import org.example.ca_agent.dto.agent.*;
import org.example.ca_agent.enums.ReliabilityLevel;
import org.example.ca_agent.enums.SourceType;
import org.example.ca_agent.mock.MockCompetitiveAnalysisFixtures;
import org.example.ca_agent.schema.Claim;
import org.example.ca_agent.schema.Evidence;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 真实 LLM 隔离冒烟测试。
 *
 * 每个 Agent 独立运行，使用 Mock 输入数据验证真实模型输出能否被正确解析。
 * 加载 application-local.yml（真实 API 配置），默认不被 Maven 测试套件执行。
 *
 * 运行方式：
 *   mvn test -Dtest=AgentLiveSmokeTest -Dgroups=live
 *   或 IDE 中直接运行本类
 */
@Tag("live")
@SpringBootTest
@ActiveProfiles("local")
class AgentLiveSmokeTest {

    @Autowired PlannerAgent plannerAgent;
    @Autowired ExtractorAgent extractorAgent;
    @Autowired AnalyzerAgent analyzerAgent;
    @Autowired WriterAgent writerAgent;
    @Autowired ReviewerAgent reviewerAgent;

    private static final List<String> PRODUCTS = List.of("Cursor", "Windsurf", "GitHub Copilot", "通义灵码");
    private static final List<String> SHARED_EVIDENCE_IDS = List.of(
            "ev_001", "ev_002", "ev_003", "ev_004", "ev_005", "ev_006",
            "ev_007", "ev_008", "ev_009", "ev_010", "ev_011", "ev_012"
    );

    // ---------- PlannerAgent ----------

    @Test
    void plannerAgent_generatesValidTaskPlan_withRealLlm() {
        CompetitiveAnalysisState state = initState();
        state.setTaskInput(buildRealisticTaskInput());

        plannerAgent.execute(state);

        TaskPlanDTO plan = state.getTaskPlan();
        assertNotNull(plan, "PlannerAgent 应生成 TaskPlanDTO");
        assertNotNull(plan.getCollectionTasks(), "collectionTasks 不应为空");
        assertFalse(plan.getCollectionTasks().isEmpty(), "collectionTasks 至少包含一项");

        // 验证覆盖了所有目标产品
        List<String> plannedProducts = plan.getCollectionTasks().stream()
                .map(TaskPlanDTO.CollectionTask::getProductName)
                .toList();
        assertTrue(plannedProducts.containsAll(PRODUCTS),
                "collectionTasks 应覆盖所有目标产品: " + PRODUCTS);
    }

    // ---------- ExtractorAgent ----------

    @Test
    void extractorAgent_generatesValidProductProfiles_withRealLlm() {
        CompetitiveAnalysisState state = initState();
        state.setTaskInput(buildRealisticTaskInput());
        state.setRawSourceSet(buildRealisticRawSourceSet());

        extractorAgent.execute(state);

        ProductProfileSetDTO profileSet = state.getProductProfileSet();
        assertNotNull(profileSet, "ExtractorAgent 应生成 ProductProfileSetDTO");
        assertNotNull(profileSet.getProducts(), "products 不应为空");
        assertFalse(profileSet.getProducts().isEmpty(), "products 至少包含一个产品");

        // 验证每个产品都有 claims 且 claim 绑定了 evidenceIds
        for (ProductProfileSetDTO.ProductProfile product : profileSet.getProducts()) {
            assertNotNull(product.getProductName(), "每个产品应有 productName");
            assertNotNull(product.getClaims(), "每个产品应有 claims");
            assertFalse(product.getClaims().isEmpty(), "每个产品至少有一个 claim");
            for (Claim claim : product.getClaims()) {
                assertNotNull(claim.getEvidenceIds(), "每个 claim 应有 evidenceIds");
                assertFalse(claim.getEvidenceIds().isEmpty(), "每个 claim 至少绑定一条 evidence");
            }
        }
    }

    // ---------- AnalyzerAgent ----------

    @Test
    void analyzerAgent_generatesValidCompetitiveAnalysis_withRealLlm() {
        CompetitiveAnalysisState state = initState();
        state.setTaskInput(buildRealisticTaskInput());
        state.setRawSourceSet(buildRealisticRawSourceSet());
        state.setProductProfileSet(buildRealisticProductProfileSet());

        analyzerAgent.execute(state);

        CompetitiveAnalysisDTO analysis = state.getCompetitiveAnalysis();
        assertNotNull(analysis, "AnalyzerAgent 应生成 CompetitiveAnalysisDTO");
        assertNotNull(analysis.getComparisonMatrix(), "comparisonMatrix 不应为空");
        assertFalse(analysis.getComparisonMatrix().isEmpty(), "comparisonMatrix 至少包含一项");
        assertNotNull(analysis.getKeyFindings(), "keyFindings 不应为空");
        assertFalse(analysis.getKeyFindings().isEmpty(), "keyFindings 至少包含一项");
        assertNotNull(analysis.getSwotSummary(), "swotSummary 不应为空");
        assertFalse(analysis.getSwotSummary().isEmpty(), "swotSummary 至少包含一项");
    }

    // ---------- WriterAgent ----------

    @Test
    void writerAgent_generatesValidReportDraft_withRealLlm() {
        CompetitiveAnalysisState state = initState();
        state.setTaskInput(buildRealisticTaskInput());
        state.setRawSourceSet(buildRealisticRawSourceSet());
        state.setProductProfileSet(buildRealisticProductProfileSet());
        state.setCompetitiveAnalysis(buildRealisticCompetitiveAnalysis());

        writerAgent.execute(state);

        ReportDraftDTO report = state.getReportDraft();
        assertNotNull(report, "WriterAgent 应生成 ReportDraftDTO");
        assertNotNull(report.getSections(), "sections 不应为空");

        // 验证包含 14 个标准章节（模型可能输出英文标题，放宽断言）
        List<String> actualTitles = report.getSections().stream()
                .map(ReportDraftDTO.ReportSection::getTitle)
                .toList();
        assertEquals(14, actualTitles.size(), "报告应包含 14 个章节");
        // 验证每个章节都有标题和内容
        for (ReportDraftDTO.ReportSection section : report.getSections()) {
            assertNotNull(section.getTitle(), "每个 section 应有 title");
            assertNotNull(section.getContent(), "每个 section 应有 content");
            assertFalse(section.getTitle().isBlank(), "title 不应为空");
            assertFalse(section.getContent().isBlank(), "content 不应为空");
        }

        // sourceList 由应用提供，不应为 null
        assertNotNull(report.getSourceList(), "sourceList 不应为 null");
    }

    // ---------- ReviewerAgent ----------

    @Test
    void reviewerAgent_generatesValidReviewResult_withRealLlm() {
        CompetitiveAnalysisState state = initState();
        state.setTaskInput(buildRealisticTaskInput());
        state.setRawSourceSet(buildRealisticRawSourceSet());
        state.setProductProfileSet(buildRealisticProductProfileSet());
        state.setCompetitiveAnalysis(buildRealisticCompetitiveAnalysis());
        state.setReportDraft(buildRealisticReportDraft());
        state.setIterationCount(0);

        reviewerAgent.execute(state);

        ReviewResultDTO review = state.getReviewResult();
        assertNotNull(review, "ReviewerAgent 应生成 ReviewResultDTO");
        assertNotNull(review.getPassed(), "passed 字段不应为 null");
        assertNotNull(review.getScore(), "score 字段不应为 null");
        assertNotNull(review.getIssues(), "issues 不应为 null");
        assertNotNull(review.getNextAction(), "nextAction 不应为 null");

        if (!review.getPassed()) {
            // 如果未通过，验证 issue 中有有效的 targetAgent
            assertFalse(review.getIssues().isEmpty(), "未通过时应至少有一个 issue");
            for (ReviewResultDTO.ReviewIssue issue : review.getIssues()) {
                assertNotNull(issue.getTargetAgent(), "每个 issue 应有 targetAgent");
            }
        }
    }

    // ---------- 辅助方法：构建测试输入 ----------

    private CompetitiveAnalysisState initState() {
        CompetitiveAnalysisState state = new CompetitiveAnalysisState();
        state.setRepairInstructions(new ArrayList<>());
        state.setAgentRuns(new ArrayList<>());
        return state;
    }

    private TaskInputDTO buildRealisticTaskInput() {
        TaskInputDTO input = new TaskInputDTO();
        input.setTaskId("task_smoke_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        input.setTaskName("AI 编程工具竞品分析");
        input.setDomain("AI_CODING_TOOLS");
        input.setTargetProducts(PRODUCTS);
        input.setAnalysisGoal("生成面向产品团队的 AI 编程工具竞品分析报告");
        input.setOutputFormat("markdown");
        input.setLanguage("zh-CN");
        input.setMaxIterations(2);
        return input;
    }

    private RawSourceSetDTO buildRealisticRawSourceSet() {
        RawSourceSetDTO rawSourceSet = new RawSourceSetDTO();
        rawSourceSet.setTaskId("task_smoke");
        rawSourceSet.setEvidencePool(new ArrayList<>());

        int idx = 0;
        for (String product : PRODUCTS) {
            rawSourceSet.getEvidencePool().add(buildEvidence(product, idx++, "OFFICIAL_SITE",
                    product + " 官网", "https://" + product.toLowerCase().replaceAll("[^a-z0-9]+", "") + ".com",
                    product + " 是一款 AI 编程助手，支持代码补全、生成和解释。"));
            rawSourceSet.getEvidencePool().add(buildEvidence(product, idx++, "PRICING_PAGE",
                    product + " 定价页", "https://" + product.toLowerCase().replaceAll("[^a-z0-9]+", "") + ".com/pricing",
                    product + " 提供免费版和付费版，付费版月费约 20 美元。"));
            rawSourceSet.getEvidencePool().add(buildEvidence(product, idx++, "DOCUMENTATION",
                    product + " 文档", "https://docs." + product.toLowerCase().replaceAll("[^a-z0-9]+", "") + ".com",
                    product + " 支持多种 IDE 插件，包括 VS Code 和 JetBrains。"));
        }
        return rawSourceSet;
    }

    private Evidence buildEvidence(String product, int index, String sourceType, String title, String url, String snippet) {
        Evidence ev = new Evidence();
        ev.setEvidenceId(SHARED_EVIDENCE_IDS.get(index));
        ev.setProductName(product);
        ev.setSourceType(SourceType.valueOf(sourceType));
        ev.setSourceTitle(title);
        ev.setUrl(url);
        ev.setContentSnippet(snippet);
        ev.setCollectedAt(LocalDateTime.now());
        ev.setReliability(ReliabilityLevel.HIGH);
        ev.setUsedFor(List.of("pricing", "features", "capabilities"));
        return ev;
    }

    private ProductProfileSetDTO buildRealisticProductProfileSet() {
        ProductProfileSetDTO profileSet = new ProductProfileSetDTO();
        profileSet.setTaskId("task_smoke");
        profileSet.setProducts(new ArrayList<>());

        for (String product : PRODUCTS) {
            ProductProfileSetDTO.ProductProfile profile = new ProductProfileSetDTO.ProductProfile();
            profile.setProductName(product);
            profile.setCompany(product + " Inc.");
            profile.setOfficialUrl("https://example.com/" + product);
            profile.setProductType("AI_CODING_TOOL");
            List<String> productEvidenceIds = rawSourceSetEvidenceIdsForProduct(product);
            profile.setPositioning(MockCompetitiveAnalysisFixtures.positioning(product, productEvidenceIds));
            profile.setTargetUsers(List.of(MockCompetitiveAnalysisFixtures.targetUser(productEvidenceIds)));
            profile.setCoreCapabilities(MockCompetitiveAnalysisFixtures.coreCapabilities(productEvidenceIds));
            profile.setAgentCapabilities(MockCompetitiveAnalysisFixtures.agentCapabilities(productEvidenceIds));
            profile.setCodebaseUnderstanding(MockCompetitiveAnalysisFixtures.codebaseUnderstanding(productEvidenceIds));
            profile.setIdeEcosystem(MockCompetitiveAnalysisFixtures.ideEcosystem(productEvidenceIds));
            profile.setModelContext(MockCompetitiveAnalysisFixtures.modelContext(productEvidenceIds));
            profile.setPricing(MockCompetitiveAnalysisFixtures.pricing(productEvidenceIds));
            profile.setEnterpriseFeatures(MockCompetitiveAnalysisFixtures.enterpriseFeatures(productEvidenceIds));
            profile.setUserFeedback(MockCompetitiveAnalysisFixtures.userFeedback(productEvidenceIds));

            Claim claim = new Claim();
            claim.setClaimId("claim_" + product.toLowerCase().replaceAll("[^a-z0-9]+", "_") + "_001");
            claim.setProductName(product);
            claim.setDimension("positioning");
            claim.setStatement(product + " is an AI coding assistant with strong code generation capabilities.");
            claim.setConfidence(0.85);
            claim.setEvidenceIds(productEvidenceIds);
            claim.setRiskLevel("low");
            profile.setClaims(List.of(claim));

            profile.setMissingFields(List.of(MockCompetitiveAnalysisFixtures.missingField("modelContext.supportedModels")));
            profileSet.getProducts().add(profile);
        }
        return profileSet;
    }

    private List<String> rawSourceSetEvidenceIdsForProduct(String product) {
        int productIndex = PRODUCTS.indexOf(product);
        int base = productIndex * 3;
        return List.of(
                SHARED_EVIDENCE_IDS.get(base),
                SHARED_EVIDENCE_IDS.get(base + 1),
                SHARED_EVIDENCE_IDS.get(base + 2)
        );
    }

    private CompetitiveAnalysisDTO buildRealisticCompetitiveAnalysis() {
        CompetitiveAnalysisDTO analysis = new CompetitiveAnalysisDTO();
        analysis.setTaskId("task_smoke");

        CompetitiveAnalysisDTO.ComparisonMatrixItem matrixItem = new CompetitiveAnalysisDTO.ComparisonMatrixItem();
        matrixItem.setDimension("core_capabilities");
        matrixItem.setSubDimension("code_generation");
        matrixItem.setItems(PRODUCTS.stream().map(p -> {
            CompetitiveAnalysisDTO.ComparisonProductItem item = new CompetitiveAnalysisDTO.ComparisonProductItem();
            item.setProductName(p);
            item.setSupportLevel("high");
            item.setSummary(p + " supports code generation.");
            item.setEvidenceIds(List.of("ev_001"));
            return item;
        }).toList());
        analysis.setComparisonMatrix(List.of(matrixItem));

        CompetitiveAnalysisDTO.KeyFinding finding = new CompetitiveAnalysisDTO.KeyFinding();
        finding.setFindingId("finding_001");
        finding.setTitle("All products support AI code generation");
        finding.setDescription("Comparison shows strong capabilities across all four tools.");
        finding.setRelatedProducts(PRODUCTS);
        finding.setEvidenceIds(SHARED_EVIDENCE_IDS);
        finding.setConfidence(0.9);
        analysis.setKeyFindings(List.of(finding));

        CompetitiveAnalysisDTO.ProductOpportunity opportunity = new CompetitiveAnalysisDTO.ProductOpportunity();
        opportunity.setOpportunityId("opportunity_001");
        opportunity.setTitle("Expand enterprise features");
        opportunity.setDescription("All products could strengthen enterprise controls.");
        opportunity.setTargetUsers(List.of("Enterprise teams"));
        opportunity.setRequiredCapabilities(List.of("sso", "audit_log"));
        opportunity.setPriority("medium");
        opportunity.setEvidenceIds(SHARED_EVIDENCE_IDS);
        analysis.setProductOpportunities(List.of(opportunity));

        CompetitiveAnalysisDTO.Risk risk = new CompetitiveAnalysisDTO.Risk();
        risk.setRiskId("risk_001");
        risk.setTitle("Feature convergence risk");
        risk.setDescription("Products are becoming similar in core capabilities.");
        risk.setSeverity("medium");
        risk.setEvidenceIds(SHARED_EVIDENCE_IDS);
        analysis.setRisks(List.of(risk));

        analysis.setSwotSummary(PRODUCTS.stream().map(p -> {
            CompetitiveAnalysisDTO.SwotSummary swot = new CompetitiveAnalysisDTO.SwotSummary();
            swot.setProductName(p);
            swot.setStrengths(List.of(swotItem("Strong code generation", p)));
            swot.setWeaknesses(List.of(swotItem("Limited enterprise features", p)));
            swot.setOpportunities(List.of(swotItem("Expand to more IDEs", p)));
            swot.setThreats(List.of(swotItem("Competition from big tech", p)));
            return swot;
        }).toList());

        return analysis;
    }

    private CompetitiveAnalysisDTO.SwotItem swotItem(String point, String product) {
        CompetitiveAnalysisDTO.SwotItem item = new CompetitiveAnalysisDTO.SwotItem();
        item.setPoint(point);
        item.setExplanation("Analysis for " + product);
        item.setEvidenceIds(SHARED_EVIDENCE_IDS);
        return item;
    }

    private ReportDraftDTO buildRealisticReportDraft() {
        ReportDraftDTO report = new ReportDraftDTO();
        report.setTaskId("task_smoke");
        report.setReportTitle("AI 编程工具竞品分析报告");
        report.setReportFormat("markdown");
        report.setSections(List.of());
        report.setSourceList(List.of());
        return report;
    }
}
