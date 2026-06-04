package org.example.ca_agent.service;

import lombok.RequiredArgsConstructor;
import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.example.ca_agent.workflow.CompetitiveAnalysisGraph;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final CompetitiveAnalysisGraph competitiveAnalysisGraph;

    public CompetitiveAnalysisState run(TaskInputDTO taskInput) {
        return competitiveAnalysisGraph.run(taskInput);
    }
}
