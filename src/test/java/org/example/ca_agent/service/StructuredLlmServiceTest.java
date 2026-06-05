package org.example.ca_agent.service;

import org.example.ca_agent.common.BizException;
import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StructuredLlmServiceTest {

    @Test
    void parsesStructuredResponse() {
        RecordingGateway gateway = new RecordingGateway("{\"taskId\":\"task-1\"}");
        StructuredLlmService service = new StructuredLlmService(new LlmChatService(gateway));

        TaskInputDTO result = service.generate("system", "user", TaskInputDTO.class);

        assertThat(result.getTaskId()).isEqualTo("task-1");
        assertThat(gateway.callCount).isEqualTo(1);
    }

    @Test
    void retriesOnceAfterInvalidJson() {
        RecordingGateway gateway = new RecordingGateway("invalid", "{\"taskId\":\"task-1\"}");
        StructuredLlmService service = new StructuredLlmService(new LlmChatService(gateway));

        TaskInputDTO result = service.generate("system", "user", TaskInputDTO.class);

        assertThat(result.getTaskId()).isEqualTo("task-1");
        assertThat(gateway.callCount).isEqualTo(2);
    }

    @Test
    void throwsBizExceptionAfterRetryIsExhausted() {
        RecordingGateway gateway = new RecordingGateway("invalid", "still invalid");
        StructuredLlmService service = new StructuredLlmService(new LlmChatService(gateway));

        assertThatThrownBy(() -> service.generate("system", "user", TaskInputDTO.class))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("structured LLM response");
        assertThat(gateway.callCount).isEqualTo(2);
    }

    private static class RecordingGateway implements ModelChatGateway {

        private final Queue<String> responses;
        private int callCount;

        private RecordingGateway(String... responses) {
            this.responses = new ArrayDeque<>(Arrays.asList(responses));
        }

        @Override
        public String call(String systemPrompt, String userPrompt) {
            callCount++;
            return responses.remove();
        }
    }
}
