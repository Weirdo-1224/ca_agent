package org.example.ca_agent.service;

import lombok.RequiredArgsConstructor;
import org.example.ca_agent.common.BizException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringAiModelChatGateway implements ModelChatGateway {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    @Override
    public String call(String prompt) {
        ChatClient.Builder chatClientBuilder = chatClientBuilderProvider.getIfAvailable();
        if (chatClientBuilder == null) {
            throw new BizException(503, "LLM chat model is not configured");
        }
        return chatClientBuilder.build()
                .prompt()
                .user(prompt)
                .call()
                .content();
    }
}
