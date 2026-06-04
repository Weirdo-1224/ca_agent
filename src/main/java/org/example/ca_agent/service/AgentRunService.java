package org.example.ca_agent.service;

import lombok.RequiredArgsConstructor;
import org.example.ca_agent.dto.response.AgentRunResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentRunService {

    public List<AgentRunResponse> getAgentRuns(String taskId) {
        return List.of();
    }
}
