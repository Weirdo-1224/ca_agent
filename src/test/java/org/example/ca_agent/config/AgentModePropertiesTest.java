package org.example.ca_agent.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AgentModePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void defaultsToMockMode() {
        contextRunner.run(context -> {
            AgentModeProperties properties = context.getBean(AgentModeProperties.class);

            assertThat(properties.getMode()).isEqualTo(AgentModeProperties.Mode.MOCK);
            assertThat(properties.isLlm()).isFalse();
        });
    }

    @Test
    void bindsLlmMode() {
        contextRunner
                .withPropertyValues("ca-agent.agent.mode=llm")
                .run(context -> {
                    AgentModeProperties properties = context.getBean(AgentModeProperties.class);

                    assertThat(properties.getMode()).isEqualTo(AgentModeProperties.Mode.LLM);
                    assertThat(properties.isLlm()).isTrue();
                });
    }

    @EnableConfigurationProperties(AgentModeProperties.class)
    static class TestConfiguration {
    }
}
