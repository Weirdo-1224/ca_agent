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
            // 先保存结果到 state，确保即使验证失败也能持久化 score/summary
            state.setReviewResult(reviewResult);
            outputValidator.validateReviewer(reviewResult, taskId);
            return state;
        }

        state.setReviewResult(iterationCount == 0
                ? MockCompetitiveAnalysisFixtures.failedReview(state.getTaskInput())
                : MockCompetitiveAnalysisFixtures.passedReview(state.getTaskInput().getTaskId()));
        return state;
    }

    /**
     * 构建 Review 上下文，仅排除 agentRuns 和 rawText 等无关大体积字段。
     * 保留 Reviewer 质检必需的完整信息：证据内容、完整报告、claim、对比分析。
     */
    private String buildCompactReviewContext(CompetitiveAnalysisState state) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("taskId", state.getTaskInput().getTaskId());
        context.put("domain", state.getTaskInput().getDomain());
        context.put("targetProducts", state.getTaskInput().getTargetProducts());

        // Evidence pool: 保留完整 contentSnippet 供 Reviewer 校验引用
        if (state.getRawSourceSet() != null && state.getRawSourceSet().getEvidencePool() != null) {
            List<Map<String, Object>> evidenceList = state.getRawSourceSet().getEvidencePool().stream().map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("evidenceId", e.getEvidenceId());
                m.put("productName", e.getProductName());
                m.put("sourceType", e.getSourceType());
                m.put("sourceTitle", e.getSourceTitle());
                m.put("url", e.getUrl());
                m.put("contentSnippet", e.getContentSnippet());
                m.put("reliability", e.getReliability());
                return m;
            }).collect(Collectors.toList());
            context.put("evidencePool", evidenceList);
        }

        // ProductProfileSet: claims + missingFields
        context.put("productProfiles", buildCompactProfiles(state.getProductProfileSet()));

        // CompetitiveAnalysis: full structure (already concise)
        if (state.getCompetitiveAnalysis() != null) {
            context.put("competitiveAnalysis", state.getCompetitiveAnalysis());
        }

        // ReportDraft: 完整内容（不截断），供 Reviewer 逐节验证
        if (state.getReportDraft() != null) {
            context.put("reportDraft", state.getReportDraft());
        }

        return JsonUtils.toJson(context);
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

    private static String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
