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
        String lastResponse = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String response = llmChatService.callChat(systemPrompt, userPrompt);
            lastResponse = response;
            try {
                return JsonUtils.fromModelJson(response, outputType);
            } catch (RuntimeException parsingError) {
                lastParsingError = parsingError;
            }
        }
        String preview = lastResponse != null && lastResponse.length() > 500
                ? lastResponse.substring(0, 500) + "..."
                : lastResponse;
        throw new BizException(502, "Failed to parse structured LLM response after "
                + MAX_ATTEMPTS + " attempts. Last response preview: " + preview
                + " | Error: " + lastParsingError.getMessage());
    }
}
