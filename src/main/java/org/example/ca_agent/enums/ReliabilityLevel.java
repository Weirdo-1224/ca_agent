package org.example.ca_agent.enums;

import lombok.Getter;

@Getter
public enum ReliabilityLevel {

    HIGH("high", "高"),
    MEDIUM("medium", "中"),
    LOW("low", "低");

    private final String code;
    private final String description;

    ReliabilityLevel(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
