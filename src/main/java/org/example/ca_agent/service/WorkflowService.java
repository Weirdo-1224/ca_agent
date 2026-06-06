package org.example.ca_agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ca_agent.assembler.StateAssembler;
import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.example.ca_agent.workflow.CompetitiveAnalysisGraph;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final CompetitiveAnalysisGraph competitiveAnalysisGraph;
    private final StateAssembler stateAssembler;

    public CompetitiveAnalysisState run(TaskInputDTO taskInput) {
        return competitiveAnalysisGraph.run(taskInput);
    }

    @Async
    public void runAsync(TaskInputDTO taskInput) {
        try {
            log.info("[Async] Starting workflow for task: {}", taskInput.getTaskId());
            CompetitiveAnalysisState state = competitiveAnalysisGraph.run(taskInput);
            stateAssembler.saveState(state);
            log.info("[Async] Workflow completed for task: {}, status: {}", taskInput.getTaskId(), state.getStatus());
        } catch (Exception e) {
            log.error("[Async] Workflow failed for task: {}", taskInput.getTaskId(), e);
        }
    }
}
