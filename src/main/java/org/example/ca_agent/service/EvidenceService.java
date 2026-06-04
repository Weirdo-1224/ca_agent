package org.example.ca_agent.service;

import lombok.RequiredArgsConstructor;
import org.example.ca_agent.schema.Evidence;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvidenceService {

    private final TaskService taskService;

    public List<Evidence> getEvidenceList(String taskId) {
        CompetitiveAnalysisState state = taskService.getTaskState(taskId);
        return state.getRawSourceSet().getEvidencePool();
    }
}
