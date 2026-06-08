package org.example.ca_agent.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmCallResult {
    private String content;
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;

    public static LlmCallResult ofContent(String content) {
        return new LlmCallResult(content, 0, 0, 0);
    }
}
