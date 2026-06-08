package org.example.ca_agent.service;

import lombok.RequiredArgsConstructor;
import org.example.ca_agent.dto.agent.LlmCallRecord;
import org.example.ca_agent.dto.agent.LlmCallResult;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LlmChatService {

    private final ModelChatGateway modelChatGateway;
    private final TokenUsageAccumulator tokenUsageAccumulator;
    private final LlmCallTraceCollector llmCallTraceCollector;

    public String callSimpleChat(String prompt) {
        return callChat("", prompt);
    }

    public String callChat(String systemPrompt, String userPrompt) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            throw new IllegalArgumentException("prompt must not be blank");
        }
        String normalizedSystemPrompt = systemPrompt == null ? "" : systemPrompt.trim();
        String trimmedUserPrompt = userPrompt.trim();

        long start = System.currentTimeMillis();
        LlmCallResult result = modelChatGateway.call(normalizedSystemPrompt, trimmedUserPrompt);
        long duration = System.currentTimeMillis() - start;

        tokenUsageAccumulator.add(result.getPromptTokens(), result.getCompletionTokens());
        llmCallTraceCollector.record(new LlmCallRecord(
                truncate(normalizedSystemPrompt, 2000),
                truncate(trimmedUserPrompt, 3000),
                truncate(result.getContent(), 5000),
                result.getPromptTokens(),
                result.getCompletionTokens(),
                duration
        ));
        return result.getContent();
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
