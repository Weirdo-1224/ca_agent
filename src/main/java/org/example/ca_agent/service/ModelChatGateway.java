package org.example.ca_agent.service;

public interface ModelChatGateway {

    String call(String systemPrompt, String userPrompt);
}
