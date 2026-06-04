package org.example.ca_agent.workflow;

import org.example.ca_agent.dto.agent.ReviewResultDTO;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.enums.ReviewIssueType;
import org.example.ca_agent.enums.TaskStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkflowRouterTest {

    @Test
    void routesToHumanReviewAndUpdatesStatusWhenNoRepairTargetCanBeChosen() {
        WorkflowRouter router = new WorkflowRouter(new RepairRouter());
        CompetitiveAnalysisState state = new CompetitiveAnalysisState();
        ReviewResultDTO reviewResult = new ReviewResultDTO();
        ReviewResultDTO.ReviewIssue issue = new ReviewResultDTO.ReviewIssue();

        issue.setType(ReviewIssueType.HALLUCINATION_RISK);
        issue.setTargetAgent(AgentType.REVIEWER_AGENT);
        reviewResult.setPassed(false);
        reviewResult.setIssues(List.of(issue));
        state.setReviewResult(reviewResult);
        state.setStatus(TaskStatus.REVIEWING);

        String route = router.routeAfterReview(state);

        assertEquals(WorkflowRouter.HUMAN_REVIEW, route);
        assertEquals(TaskStatus.WAITING_HUMAN_REVIEW, state.getStatus());
    }
}
