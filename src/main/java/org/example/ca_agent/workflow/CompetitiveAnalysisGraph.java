package org.example.ca_agent.workflow;

import lombok.RequiredArgsConstructor;
import org.example.ca_agent.agent.AgentRunTracer;
import org.example.ca_agent.agent.AnalyzerAgent;
import org.example.ca_agent.agent.CollectorAgent;
import org.example.ca_agent.agent.ExtractorAgent;
import org.example.ca_agent.agent.PlannerAgent;
import org.example.ca_agent.agent.ReviewerAgent;
import org.example.ca_agent.agent.WriterAgent;
import org.example.ca_agent.common.BizException;
import org.example.ca_agent.dto.agent.RepairInstructionDTO;
import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.example.ca_agent.enums.TaskStatus;
import org.example.ca_agent.mock.MockCompetitiveAnalysisFixtures;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class CompetitiveAnalysisGraph {

    private final PlannerAgent plannerAgent;
    private final CollectorAgent collectorAgent;
    private final ExtractorAgent extractorAgent;
    private final AnalyzerAgent analyzerAgent;
    private final WriterAgent writerAgent;
    private final ReviewerAgent reviewerAgent;
    private final WorkflowRouter workflowRouter;
    private final RepairRouter repairRouter;
    private final AgentRunTracer agentRunTracer;

    public CompetitiveAnalysisState run(TaskInputDTO taskInput) {
        CompetitiveAnalysisState state = initState(taskInput);
        executeFullChain(state);
        return routeUntilFinished(state);
    }

    public CompetitiveAnalysisState runMockDemo() {
        return run(MockCompetitiveAnalysisFixtures.mockTaskInput());
    }

    private CompetitiveAnalysisState initState(TaskInputDTO taskInput) {
        CompetitiveAnalysisState state = new CompetitiveAnalysisState();
        state.setTaskInput(taskInput);
        state.setRepairInstructions(new ArrayList<>());
        state.setIterationCount(0);
        state.setStatus(TaskStatus.CREATED);
        return state;
    }

    private CompetitiveAnalysisState routeUntilFinished(CompetitiveAnalysisState state) {
        while (true) {
            String route = workflowRouter.routeAfterReview(state);

            if (WorkflowRouter.END.equals(route) || WorkflowRouter.HUMAN_REVIEW.equals(route)) {
                return state;
            }

            RepairInstructionDTO repairInstruction = repairRouter.buildRepairInstruction(state);
            state.getRepairInstructions().add(repairInstruction);
            state.increaseIteration();
            state.setStatus(TaskStatus.REPAIRING);
            executeFrom(route, state);
        }
    }

    private void executeFullChain(CompetitiveAnalysisState state) {
        agentRunTracer.trace(plannerAgent, state);
        agentRunTracer.trace(collectorAgent, state);
        agentRunTracer.trace(extractorAgent, state);
        agentRunTracer.trace(analyzerAgent, state);
        agentRunTracer.trace(writerAgent, state);
        agentRunTracer.trace(reviewerAgent, state);
    }

    private void executeFrom(String route, CompetitiveAnalysisState state) {
        switch (route) {
            case WorkflowRouter.COLLECTOR_AGENT -> executeFromCollector(state);
            case WorkflowRouter.EXTRACTOR_AGENT -> executeFromExtractor(state);
            case WorkflowRouter.ANALYZER_AGENT -> executeFromAnalyzer(state);
            case WorkflowRouter.WRITER_AGENT -> executeFromWriter(state);
            default -> throw new BizException("Unknown workflow route: " + route);
        }
    }

    private void executeFromCollector(CompetitiveAnalysisState state) {
        agentRunTracer.trace(collectorAgent, state);
        executeFromExtractor(state);
    }

    private void executeFromExtractor(CompetitiveAnalysisState state) {
        agentRunTracer.trace(extractorAgent, state);
        executeFromAnalyzer(state);
    }

    private void executeFromAnalyzer(CompetitiveAnalysisState state) {
        agentRunTracer.trace(analyzerAgent, state);
        executeFromWriter(state);
    }

    private void executeFromWriter(CompetitiveAnalysisState state) {
        agentRunTracer.trace(writerAgent, state);
        agentRunTracer.trace(reviewerAgent, state);
    }
}
