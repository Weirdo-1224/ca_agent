package org.example.ca_agent.dto.response;

import lombok.Data;
import org.example.ca_agent.enums.AgentType;

import java.time.LocalDateTime;

@Data
public class AgentRunResponse {

    private String runId;
    private String taskId;
    private AgentType agentType;
    private String inputType;
    private String outputType;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private String errorMessage;
}
