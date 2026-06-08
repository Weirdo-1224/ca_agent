package org.example.ca_agent.service;

import org.springframework.stereotype.Component;

/**
 * ThreadLocal-based token usage accumulator.
 * Tracks token consumption per agent execution transparently.
 * Reset before each agent run, read after completion.
 */
@Component
public class TokenUsageAccumulator {

    private static final ThreadLocal<int[]> USAGE = ThreadLocal.withInitial(() -> new int[3]);

    public void reset() {
        USAGE.set(new int[3]);
    }

    public void add(int promptTokens, int completionTokens) {
        int[] usage = USAGE.get();
        usage[0] += promptTokens;
        usage[1] += completionTokens;
        usage[2] += (promptTokens + completionTokens);
    }

    public int getPromptTokens() {
        return USAGE.get()[0];
    }

    public int getCompletionTokens() {
        return USAGE.get()[1];
    }

    public int getTotalTokens() {
        return USAGE.get()[2];
    }

    public void clear() {
        USAGE.remove();
    }
}
