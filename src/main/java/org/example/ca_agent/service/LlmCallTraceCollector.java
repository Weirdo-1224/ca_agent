package org.example.ca_agent.service;

import org.example.ca_agent.dto.agent.LlmCallRecord;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ThreadLocal 收集器，透明记录当前线程（Agent 执行期间）所有 LLM 调用的完整信息。
 * 与 TokenUsageAccumulator 同构，由 AgentRunTracer 在执行前 reset、执行后 harvest。
 */
@Component
public class LlmCallTraceCollector {

    private static final ThreadLocal<List<LlmCallRecord>> RECORDS = ThreadLocal.withInitial(ArrayList::new);

    public void record(LlmCallRecord record) {
        RECORDS.get().add(record);
    }

    public List<LlmCallRecord> harvest() {
        return new ArrayList<>(RECORDS.get());
    }

    public void reset() {
        RECORDS.get().clear();
    }

    public void clear() {
        RECORDS.remove();
    }
}
