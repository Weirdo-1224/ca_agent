package org.example.ca_agent.service;

import org.example.ca_agent.dto.agent.LlmCallResult;

public interface ModelChatGateway {

    LlmCallResult call(String systemPrompt, String userPrompt);
}
