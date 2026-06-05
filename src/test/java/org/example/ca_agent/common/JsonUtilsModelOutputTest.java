package org.example.ca_agent.common;

import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonUtilsModelOutputTest {

    @Test
    void extractsPureJsonObject() {
        assertThat(JsonUtils.extractJsonObject("{\"taskId\":\"task-1\"}"))
                .isEqualTo("{\"taskId\":\"task-1\"}");
    }

    @Test
    void extractsJsonFromMarkdownCodeBlock() {
        String response = "```json\n{\"taskId\":\"task-1\"}\n```";

        assertThat(JsonUtils.extractJsonObject(response))
                .isEqualTo("{\"taskId\":\"task-1\"}");
    }

    @Test
    void extractsJsonSurroundedByExplanation() {
        String response = "Here is the result: {\"taskId\":\"task-1\"} End.";

        assertThat(JsonUtils.extractJsonObject(response))
                .isEqualTo("{\"taskId\":\"task-1\"}");
    }

    @Test
    void ignoresBracesInsideJsonStrings() {
        String response = "prefix {\"analysisGoal\":\"compare {price} and \\\"value}\\\"\"} suffix";

        assertThat(JsonUtils.extractJsonObject(response))
                .isEqualTo("{\"analysisGoal\":\"compare {price} and \\\"value}\\\"\"}");
    }

    @Test
    void rejectsTextWithoutCompleteJsonObject() {
        assertThatThrownBy(() -> JsonUtils.extractJsonObject("not json {\"taskId\":\"task-1\""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JSON object");
    }

    @Test
    void parsesExtractedModelJson() {
        TaskInputDTO result = JsonUtils.fromModelJson(
                "Result: ```json\n{\"taskId\":\"task-1\",\"taskName\":\"demo\"}\n```",
                TaskInputDTO.class
        );

        assertThat(result.getTaskId()).isEqualTo("task-1");
        assertThat(result.getTaskName()).isEqualTo("demo");
    }
}
