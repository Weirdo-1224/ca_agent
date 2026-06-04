package org.example.ca_agent.workflow;

import org.example.ca_agent.agent.AnalyzerAgent;
import org.example.ca_agent.agent.CollectorAgent;
import org.example.ca_agent.agent.ExtractorAgent;
import org.example.ca_agent.agent.PlannerAgent;
import org.example.ca_agent.agent.ReviewerAgent;
import org.example.ca_agent.agent.WriterAgent;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.enums.RepairType;
import org.example.ca_agent.enums.TaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompetitiveAnalysisGraphTest {

    @Test
    void runMockDemoRepairsAfterFirstReviewAndCompletes() {
        CompetitiveAnalysisGraph graph = new CompetitiveAnalysisGraph(
                new PlannerAgent(),
                new CollectorAgent(),
                new ExtractorAgent(),
                new AnalyzerAgent(),
                new WriterAgent(),
                new ReviewerAgent(),
                new WorkflowRouter(new RepairRouter()),
                new RepairRouter()
        );

        CompetitiveAnalysisState state = graph.runMockDemo();

        assertEquals(TaskStatus.COMPLETED, state.getStatus());
        assertEquals(1, state.getIterationCount());
        assertNotNull(state.getReviewResult());
        assertTrue(state.getReviewResult().getPassed());
        assertTrue(state.getReviewResult().getIssues().isEmpty());
        assertNotNull(state.getRepairInstructions());
        assertFalse(state.getRepairInstructions().isEmpty());
        assertEquals(AgentType.COLLECTOR_AGENT, state.getRepairInstructions().get(0).getTargetAgent());
        assertNotNull(state.getReportDraft());
        assertEquals(14, state.getReportDraft().getSections().size());
    }

    @Test
    void repairRouterBuildsSupplementEvidenceInstructionForMissingEvidence() {
        CompetitiveAnalysisGraph graph = new CompetitiveAnalysisGraph(
                new PlannerAgent(),
                new CollectorAgent(),
                new ExtractorAgent(),
                new AnalyzerAgent(),
                new WriterAgent(),
                new ReviewerAgent(),
                new WorkflowRouter(new RepairRouter()),
                new RepairRouter()
        );

        CompetitiveAnalysisState state = graph.runMockDemo();

        assertEquals(RepairType.SUPPLEMENT_EVIDENCE, state.getRepairInstructions().get(0).getRepairType());
        assertEquals("pricing", state.getRepairInstructions().get(0).getTargetDimension());
        assertFalse(state.getRepairInstructions().get(0).getIssueIds().isEmpty());
    }
}
