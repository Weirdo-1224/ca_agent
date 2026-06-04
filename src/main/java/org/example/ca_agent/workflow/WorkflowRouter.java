package org.example.ca_agent.workflow;

import lombok.RequiredArgsConstructor;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.enums.TaskStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkflowRouter {

    public static final String END = "END";
    public static final String HUMAN_REVIEW = "HUMAN_REVIEW";
    public static final String COLLECTOR_AGENT = "CollectorAgent";
    public static final String EXTRACTOR_AGENT = "ExtractorAgent";
    public static final String ANALYZER_AGENT = "AnalyzerAgent";
    public static final String WRITER_AGENT = "WriterAgent";

    private final RepairRouter repairRouter;

    public String routeAfterReview(CompetitiveAnalysisState state) {
        if (Boolean.TRUE.equals(state.getReviewResult().getPassed())) {
            state.setStatus(TaskStatus.COMPLETED);
            return END;
        }

        if (state.isMaxIterationReached()) {
            return routeToHumanReview(state);
        }

        AgentType targetAgent = repairRouter.chooseEarliestTargetAgent(state.getReviewResult().getIssues());
        if (targetAgent == null) {
            return routeToHumanReview(state);
        }

        return switch (targetAgent) {
            case COLLECTOR_AGENT -> COLLECTOR_AGENT;
            case EXTRACTOR_AGENT -> EXTRACTOR_AGENT;
            case ANALYZER_AGENT -> ANALYZER_AGENT;
            case WRITER_AGENT -> WRITER_AGENT;
            default -> routeToHumanReview(state);
        };
    }

    private String routeToHumanReview(CompetitiveAnalysisState state) {
        state.setStatus(TaskStatus.WAITING_HUMAN_REVIEW);
        return HUMAN_REVIEW;
    }
}
