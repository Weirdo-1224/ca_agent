package org.example.ca_agent.enums;

import lombok.Getter;

@Getter
public enum AgentType {

    PLANNER_AGENT("planner_agent", "规划 Agent"),
    COLLECTOR_AGENT("collector_agent", "采集 Agent"),
    EXTRACTOR_AGENT("extractor_agent", "提取 Agent"),
    ANALYZER_AGENT("analyzer_agent", "分析 Agent"),
    WRITER_AGENT("writer_agent", "撰写 Agent"),
    REVIEWER_AGENT("reviewer_agent", "审核 Agent");

    private final String code;
    private final String description;

    AgentType(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
