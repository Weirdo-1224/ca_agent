package org.example.ca_agent.agent;

import org.example.ca_agent.common.JsonUtils;
import org.example.ca_agent.config.AgentModeProperties;
import org.example.ca_agent.dto.agent.*;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.enums.TaskStatus;
import org.example.ca_agent.mock.MockCompetitiveAnalysisFixtures;
import org.example.ca_agent.prompt.ReviewerPrompt;
import org.example.ca_agent.schema.Evidence;
import org.example.ca_agent.service.AgentOutputValidator;
import org.example.ca_agent.service.StructuredLlmService;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ReviewerAgent implements AgentNode {

    private final AgentModeProperties modeProperties;
    private final StructuredLlmService structuredLlmService;
    private final ReviewerPrompt reviewerPrompt;
    private final AgentOutputValidator outputValidator;

    public ReviewerAgent() {
        this(new AgentModeProperties(), null, null, null);
    }

    @Autowired
    public ReviewerAgent(
            AgentModeProperties modeProperties,
            StructuredLlmService structuredLlmService,
            ReviewerPrompt reviewerPrompt,
            AgentOutputValidator outputValidator
    ) {
        this.modeProperties = modeProperties;
        this.structuredLlmService = structuredLlmService;
        this.reviewerPrompt = reviewerPrompt;
        this.outputValidator = outputValidator;
    }

    @Override
    public AgentType getAgentType() {
        return AgentType.REVIEWER_AGENT;
    }

    @Override
    public CompetitiveAnalysisState execute(CompetitiveAnalysisState state) {
        state.setStatus(TaskStatus.REVIEWING);
        int iterationCount = state.getIterationCount() == null ? 0 : state.getIterationCount();
        if (modeProperties.isLlm()) {
            int maxIterations = state.getTaskInput().getMaxIterations() == null
                    ? 0
                    : state.getTaskInput().getMaxIterations();
            String taskId = state.getTaskInput().getTaskId();
            String compactContext = buildCompactReviewContext(state);
            ReviewResultDTO reviewResult = structuredLlmService.generate(
                    ReviewerPrompt.SYSTEM_PROMPT,
                    reviewerPrompt.buildUserPrompt(
                            compactContext,
                            JsonUtils.toJson(state.getRepairInstructions()),
                            iterationCount,
                            maxIterations,
                            state.getTaskInput().getLanguage()
                    ),
                    ReviewResultDTO.class
            );
            reviewResult.setTaskId(taskId);
            outputValidator.validateReviewer(reviewResult, taskId);
            state.setReviewResult(reviewResult);
            return state;
        }

        state.setReviewResult(iterationCount == 0
                ? MockCompetitiveAnalysisFixtures.failedReview(state.getTaskInput())
                : MockCompetitiveAnalysisFixtures.passedReview(state.getTaskInput().getTaskId()));
        return state;
    }

    /**
     * 构建精简的 Review 上下文，排除 agentRuns、rawSources 等大体积字段。
     * 只保留 Reviewer 质检必需的信息：报告摘要、claim、对比矩阵、evidence索引。
     */
    private String buildCompactReviewContext(CompetitiveAnalysisState state) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("taskId", state.getTaskInput().getTaskId());
        context.put("domain", state.getTaskInput().getDomain());
        context.put("targetProducts", state.getTaskInput().getTargetProducts());

        // Evidence index: evidenceId -> sourceTitle (no full contentSnippet)
        context.put("evidenceIndex", buildEvidenceIndex(state));

        // ProductProfileSet: only claims (lightweight)
        context.put("productProfiles", buildCompactProfiles(state.getProductProfileSet()));

        // CompetitiveAnalysis: full structure (already concise)
        if (state.getCompetitiveAnalysis() != null) {
            context.put("competitiveAnalysis", state.getCompetitiveAnalysis());
        }

        // ReportDraft: section titles + truncated content + evidenceIds
        context.put("reportDraft", buildCompactReport(state.getReportDraft()));

        return JsonUtils.toJson(context);
    }

    private Map<String, String> buildEvidenceIndex(CompetitiveAnalysisState state) {
        Map<String, String> index = new LinkedHashMap<>();
        if (state.getRawSourceSet() != null && state.getRawSourceSet().getEvidencePool() != null) {
            for (Evidence e : state.getRawSourceSet().getEvidencePool()) {
                index.put(e.getEvidenceId(), e.getSourceTitle());
            }
        }
        return index;
    }

    private List<Map<String, Object>> buildCompactProfiles(ProductProfileSetDTO profileSet) {
        if (profileSet == null || profileSet.getProducts() == null) {
            return List.of();
        }
        return profileSet.getProducts().stream().map(product -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productName", product.getProductName());
            m.put("claims", product.getClaims());
            m.put("missingFields", product.getMissingFields());
            return m;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> buildCompactReport(ReportDraftDTO reportDraft) {
        if (reportDraft == null) {
            return Map.of();
        }
        Map<String, Object> compact = new LinkedHashMap<>();
        compact.put("reportTitle", reportDraft.getReportTitle());
        if (reportDraft.getSections() != null) {
            List<Map<String, Object>> sections = reportDraft.getSections().stream().map(section -> {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("title", section.getTitle());
                s.put("contentPreview", truncate(section.getContent(), 200));
                s.put("evidenceIds", section.getEvidenceIds());
                s.put("relatedClaimIds", section.getRelatedClaimIds());
                return s;
            }).collect(Collectors.toList());
            compact.put("sections", sections);
        }
        return compact;
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
