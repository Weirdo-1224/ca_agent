package org.example.ca_agent.dto.agent;

import lombok.Data;
import org.example.ca_agent.enums.AgentType;

import java.time.LocalDateTime;

/**
 * Agent 执行过程追踪记录，暂存在 CompetitiveAnalysisState 中，
 * 由 StateAssembler 统一持久化到数据库。
 */
@Data
public class AgentRunTrace {

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
