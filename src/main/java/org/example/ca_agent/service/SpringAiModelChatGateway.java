package org.example.ca_agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ca_agent.common.BizException;
import org.example.ca_agent.dto.agent.LlmCallResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringAiModelChatGateway implements ModelChatGateway {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    @Override
    @Retryable(
            retryFor = {ResourceAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public LlmCallResult call(String systemPrompt, String userPrompt) {
        ChatClient.Builder chatClientBuilder = chatClientBuilderProvider.getIfAvailable();
        if (chatClientBuilder == null) {
            throw new BizException(503, "LLM chat model is not configured");
        }
        log.debug("Calling LLM, prompt length: system={}, user={}",
                systemPrompt.length(), userPrompt.length());

        ChatResponse chatResponse = chatClientBuilder.build()
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .chatResponse();

        String content = "";
        int promptTokens = 0;
        int completionTokens = 0;

        if (chatResponse != null && chatResponse.getResult() != null) {
            content = chatResponse.getResult().getOutput().getText();
            if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                promptTokens = (int) chatResponse.getMetadata().getUsage().getPromptTokens();
                completionTokens = (int) chatResponse.getMetadata().getUsage().getCompletionTokens();
            }
        }

        return new LlmCallResult(content, promptTokens, completionTokens,
                promptTokens + completionTokens);
    }
}
