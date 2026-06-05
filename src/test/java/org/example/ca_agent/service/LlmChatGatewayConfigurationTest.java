package org.example.ca_agent.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.ai.model.chat=openai",
        "spring.ai.openai.api-key=test-key"
})
@ActiveProfiles("test")
class LlmChatGatewayConfigurationTest {

    @Autowired
    private ModelChatGateway modelChatGateway;

    @Test
    void usesSpringAiGatewayWhenOpenAiChatModelIsEnabled() {
        assertThat(modelChatGateway).isInstanceOf(SpringAiModelChatGateway.class);
    }
}
