package org.example.ca_agent.service;

import lombok.RequiredArgsConstructor;
import org.example.ca_agent.common.BizException;
import org.example.ca_agent.common.JsonUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StructuredLlmService {

    private static final int MAX_ATTEMPTS = 2;

    private final LlmChatService llmChatService;

    public <T> T generate(String systemPrompt, String userPrompt, Class<T> outputType) {
        RuntimeException lastParsingError = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String response = llmChatService.callChat(systemPrompt, userPrompt);
            try {
                return JsonUtils.fromModelJson(response, outputType);
            } catch (RuntimeException parsingError) {
                lastParsingError = parsingError;
            }
        }
        throw new BizException(502, "Failed to parse structured LLM response: "
                + lastParsingError.getMessage());
    }
}
