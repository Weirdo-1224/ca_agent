package org.example.ca_agent.dto.response;

import lombok.Data;
import org.example.ca_agent.dto.agent.LlmCallRecord;
import org.example.ca_agent.enums.AgentType;

import java.time.LocalDateTime;
import java.util.List;

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
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private List<LlmCallRecord> llmCalls;
}
