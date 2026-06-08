package org.example.ca_agent.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 记录单次 LLM 调用的完整信息：Prompt、Response、Token、耗时。
 * 用于可观测性 Trace，支持前端查看 Agent 的决策过程。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LlmCallRecord {

    private String systemPrompt;
    private String userPrompt;
    private String response;
    private int promptTokens;
    private int completionTokens;
    private long durationMs;
}
