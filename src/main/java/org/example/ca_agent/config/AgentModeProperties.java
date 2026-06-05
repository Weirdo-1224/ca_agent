package org.example.ca_agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ca-agent.agent")
public class AgentModeProperties {

    private Mode mode = Mode.MOCK;

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public boolean isLlm() {
        return mode == Mode.LLM;
    }

    public enum Mode {
        MOCK,
        LLM
    }
}
