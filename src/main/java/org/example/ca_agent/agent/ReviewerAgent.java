package org.example.ca_agent.agent;

import org.example.ca_agent.common.JsonUtils;
import org.example.ca_agent.config.AgentModeProperties;
import org.example.ca_agent.dto.agent.ReviewResultDTO;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.enums.TaskStatus;
import org.example.ca_agent.mock.MockCompetitiveAnalysisFixtures;
import org.example.ca_agent.prompt.ReviewerPrompt;
import org.example.ca_agent.service.AgentOutputValidator;
import org.example.ca_agent.service.StructuredLlmService;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
            ReviewResultDTO reviewResult = structuredLlmService.generate(
                    ReviewerPrompt.SYSTEM_PROMPT,
                    reviewerPrompt.buildUserPrompt(
                            JsonUtils.toJson(state),
                            JsonUtils.toJson(state.getRepairInstructions()),
                            iterationCount,
                            maxIterations
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
}
