package org.example.ca_agent.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LlmChatService {

    private final ModelChatGateway modelChatGateway;

    public String callSimpleChat(String prompt) {
        return callChat("", prompt);
    }

    public String callChat(String systemPrompt, String userPrompt) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            throw new IllegalArgumentException("prompt must not be blank");
        }
        String normalizedSystemPrompt = systemPrompt == null ? "" : systemPrompt.trim();
        return modelChatGateway.call(normalizedSystemPrompt, userPrompt.trim());
    }
}
