package org.example.ca_agent.service;

import org.example.ca_agent.common.BizException;
import org.example.ca_agent.dto.agent.LlmCallResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmChatServiceTest {

    @Test
    void callSimpleChat_delegatesTrimmedPromptToGateway() {
        RecordingModelChatGateway gateway = new RecordingModelChatGateway("pong");
        LlmChatService service = new LlmChatService(gateway, new TokenUsageAccumulator(), new LlmCallTraceCollector());

        String response = service.callSimpleChat("  ping  ");

        assertThat(response).isEqualTo("pong");
        assertThat(gateway.systemPrompt).isEmpty();
        assertThat(gateway.userPrompt).isEqualTo("ping");
    }

    @Test
    void callChat_delegatesSeparateTrimmedPromptsToGateway() {
        RecordingModelChatGateway gateway = new RecordingModelChatGateway("result");
        LlmChatService service = new LlmChatService(gateway, new TokenUsageAccumulator(), new LlmCallTraceCollector());

        String response = service.callChat("  system rules  ", "  user input  ");

        assertThat(response).isEqualTo("result");
        assertThat(gateway.systemPrompt).isEqualTo("system rules");
        assertThat(gateway.userPrompt).isEqualTo("user input");
    }

    @Test
    void callSimpleChat_rejectsBlankPrompt() {
        LlmChatService service = new LlmChatService(new RecordingModelChatGateway("unused"), new TokenUsageAccumulator(), new LlmCallTraceCollector());

        assertThatThrownBy(() -> service.callSimpleChat("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("prompt must not be blank");
    }

    @Test
    void callSimpleChat_throwsBizExceptionWhenModelGatewayUnavailable() {
        LlmChatService service = new LlmChatService((systemPrompt, userPrompt) -> {
            throw new BizException(503, "LLM chat model is not configured");
        }, new TokenUsageAccumulator(), new LlmCallTraceCollector());

        assertThatThrownBy(() -> service.callSimpleChat("ping"))
                .isInstanceOf(BizException.class)
                .hasMessage("LLM chat model is not configured");
    }

    private static class RecordingModelChatGateway implements ModelChatGateway {

        private final String response;
        private String systemPrompt;
        private String userPrompt;

        private RecordingModelChatGateway(String response) {
            this.response = response;
        }

        @Override
        public LlmCallResult call(String systemPrompt, String userPrompt) {
            this.systemPrompt = systemPrompt;
            this.userPrompt = userPrompt;
            return LlmCallResult.ofContent(response);
        }
    }
}
