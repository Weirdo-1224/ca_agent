package org.example.ca_agent.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LlmChatService {

    private final ModelChatGateway modelChatGateway;

    public String callSimpleChat(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("prompt must not be blank");
        }
        return modelChatGateway.call(prompt.trim());
    }
}
