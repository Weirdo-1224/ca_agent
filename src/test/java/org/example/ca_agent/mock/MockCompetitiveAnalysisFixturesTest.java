package org.example.ca_agent.mock;

import org.example.ca_agent.dto.agent.ReviewResultDTO;
import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.example.ca_agent.enums.AgentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockCompetitiveAnalysisFixturesTest {

    @Test
    void providesTask02DemoInputAndReviewFixtures() {
        TaskInputDTO taskInput = MockCompetitiveAnalysisFixtures.mockTaskInput();

        assertEquals("task_mock_001", taskInput.getTaskId());
        assertEquals(4, taskInput.getTargetProducts().size());
        assertEquals(14, MockCompetitiveAnalysisFixtures.STANDARD_REPORT_SECTIONS.size());

        ReviewResultDTO failedReview = MockCompetitiveAnalysisFixtures.failedReview(taskInput);
        assertFalse(failedReview.getPassed());
        assertEquals(AgentType.COLLECTOR_AGENT, failedReview.getIssues().get(0).getTargetAgent());

        ReviewResultDTO passedReview = MockCompetitiveAnalysisFixtures.passedReview(taskInput.getTaskId());
        assertTrue(passedReview.getPassed());
        assertEquals("finish", passedReview.getNextAction().getAction());
    }
}
