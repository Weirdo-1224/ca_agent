package org.example.ca_agent.agent;

import lombok.RequiredArgsConstructor;
import org.example.ca_agent.dto.agent.ReviewResultDTO;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.enums.TaskStatus;
import org.example.ca_agent.mock.MockCompetitiveAnalysisFixtures;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewerAgent implements AgentNode {

    @Override
    public AgentType getAgentType() {
        return AgentType.REVIEWER_AGENT;
    }

    @Override
    public CompetitiveAnalysisState execute(CompetitiveAnalysisState state) {
        state.setStatus(TaskStatus.REVIEWING);
        int iterationCount = state.getIterationCount() == null ? 0 : state.getIterationCount();
        state.setReviewResult(iterationCount == 0
                ? MockCompetitiveAnalysisFixtures.failedReview(state.getTaskInput())
                : MockCompetitiveAnalysisFixtures.passedReview(state.getTaskInput().getTaskId()));
        return state;
    }
}
