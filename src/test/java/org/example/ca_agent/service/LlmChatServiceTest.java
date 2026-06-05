package org.example.ca_agent.service;

import org.example.ca_agent.common.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmChatServiceTest {

    @Test
    void callSimpleChat_delegatesTrimmedPromptToGateway() {
        RecordingModelChatGateway gateway = new RecordingModelChatGateway("pong");
        LlmChatService service = new LlmChatService(gateway);

        String response = service.callSimpleChat("  ping  ");

        assertThat(response).isEqualTo("pong");
        assertThat(gateway.prompt).isEqualTo("ping");
    }

    @Test
    void callSimpleChat_rejectsBlankPrompt() {
        LlmChatService service = new LlmChatService(new RecordingModelChatGateway("unused"));

        assertThatThrownBy(() -> service.callSimpleChat("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("prompt must not be blank");
    }

    @Test
    void callSimpleChat_throwsBizExceptionWhenModelGatewayUnavailable() {
        LlmChatService service = new LlmChatService(prompt -> {
            throw new BizException(503, "LLM chat model is not configured");
        });

        assertThatThrownBy(() -> service.callSimpleChat("ping"))
                .isInstanceOf(BizException.class)
                .hasMessage("LLM chat model is not configured");
    }

    private static class RecordingModelChatGateway implements ModelChatGateway {

        private final String response;
        private String prompt;

        private RecordingModelChatGateway(String response) {
            this.response = response;
        }

        @Override
        public String call(String prompt) {
            this.prompt = prompt;
            return response;
        }
    }
}
