package org.example.ca_agent.agent;

import lombok.RequiredArgsConstructor;
import org.example.ca_agent.assembler.StateAssembler;
import org.example.ca_agent.dto.agent.AgentRunTrace;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.service.LlmCallTraceCollector;
import org.example.ca_agent.service.TokenUsageAccumulator;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 统一包装 Agent 执行，记录 AgentRun Trace。
 * 不侵入 Agent 内部逻辑，在 CompetitiveAnalysisGraph 调用层拦截。
 */
@Component
@RequiredArgsConstructor
public class AgentRunTracer {

    private final StateAssembler stateAssembler;
    private final TokenUsageAccumulator tokenUsageAccumulator;
    private final LlmCallTraceCollector llmCallTraceCollector;

    /**
     * 追踪 Agent 执行全过程，记录 start/end/duration/token 和状态。
     * 每个 Agent 执行后实时保存中间状态到数据库，支持前端轮询进度。
     */
    public void trace(AgentNode agent, CompetitiveAnalysisState state) {
        String taskId = state.getTaskInput().getTaskId();
        String runId = UUID.randomUUID().toString();
        LocalDateTime startTime = LocalDateTime.now();
        long startMs = System.currentTimeMillis();

        // Reset token accumulator and call trace collector before agent execution
        tokenUsageAccumulator.reset();
        llmCallTraceCollector.reset();

        try {
            agent.execute(state);
            long durationMs = System.currentTimeMillis() - startMs;
            AgentRunTrace trace = buildSuccessTrace(runId, taskId, agent.getAgentType(), startTime, durationMs);
            attachTokenUsage(trace);
            attachLlmCalls(trace);
            record(state, trace);
            // 实时保存中间状态
            stateAssembler.saveState(state);
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startMs;
            AgentRunTrace trace = buildFailureTrace(runId, taskId, agent.getAgentType(), startTime, durationMs, e);
            attachTokenUsage(trace);
            attachLlmCalls(trace);
            record(state, trace);
            // 失败时也保存状态
            stateAssembler.saveState(state);
            throw e;
        }
    }

    private void attachTokenUsage(AgentRunTrace trace) {
        trace.setPromptTokens(tokenUsageAccumulator.getPromptTokens());
        trace.setCompletionTokens(tokenUsageAccumulator.getCompletionTokens());
        trace.setTotalTokens(tokenUsageAccumulator.getTotalTokens());
    }

    private void attachLlmCalls(AgentRunTrace trace) {
        trace.setLlmCalls(llmCallTraceCollector.harvest());
    }

    private void record(CompetitiveAnalysisState state, AgentRunTrace trace) {
        state.getAgentRuns().add(trace);
    }

    private AgentRunTrace buildSuccessTrace(String runId, String taskId, AgentType agentType,
                                             LocalDateTime startTime, long durationMs) {
        AgentRunTrace trace = new AgentRunTrace();
        trace.setRunId(runId);
        trace.setTaskId(taskId);
        trace.setAgentType(agentType);
        trace.setInputType(resolveInputType(agentType));
        trace.setOutputType(resolveOutputType(agentType));
        trace.setStatus("SUCCESS");
        trace.setStartTime(startTime);
        trace.setEndTime(LocalDateTime.now());
        trace.setDurationMs(durationMs);
        return trace;
    }

    private AgentRunTrace buildFailureTrace(String runId, String taskId, AgentType agentType,
                                             LocalDateTime startTime, long durationMs, Exception e) {
        AgentRunTrace trace = buildSuccessTrace(runId, taskId, agentType, startTime, durationMs);
        trace.setStatus("FAILED");
        trace.setErrorMessage(e.getMessage());
        return trace;
    }

    /**
     * 根据 Agent 类型推断输入数据类型。
     */
    private String resolveInputType(AgentType agentType) {
        return switch (agentType) {
            case PLANNER_AGENT -> "TaskInputDTO";
            case COLLECTOR_AGENT -> "TaskPlanDTO + RepairInstructions";
            case EXTRACTOR_AGENT -> "RawSourceSetDTO";
            case ANALYZER_AGENT -> "ProductProfileSetDTO";
            case WRITER_AGENT -> "ProductProfileSetDTO + CompetitiveAnalysisDTO";
            case REVIEWER_AGENT -> "CompetitiveAnalysisState";
        };
    }

    /**
     * 根据 Agent 类型推断输出数据类型。
     */
    private String resolveOutputType(AgentType agentType) {
        return switch (agentType) {
            case PLANNER_AGENT -> "TaskPlanDTO";
            case COLLECTOR_AGENT -> "RawSourceSetDTO";
            case EXTRACTOR_AGENT -> "ProductProfileSetDTO";
            case ANALYZER_AGENT -> "CompetitiveAnalysisDTO";
            case WRITER_AGENT -> "ReportDraftDTO";
            case REVIEWER_AGENT -> "ReviewResultDTO";
        };
    }
}
