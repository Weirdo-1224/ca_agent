package org.example.ca_agent.dto.agent;

import lombok.Data;
import org.example.ca_agent.enums.AgentType;

import java.time.LocalDateTime;
import java.util.List;

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
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private List<LlmCallRecord> llmCalls;

    /**
     * 标记是否已持久化到数据库，用于增量保存。
     * 不参与 JSON 序列化（transient 语义）。
     */
    private transient boolean persisted = false;
}
