package org.example.ca_agent.enums;

import lombok.Getter;

@Getter
public enum TaskStatus {

    CREATED("created", "已创建"),
    PLANNING("planning", "规划中"),
    COLLECTING("collecting", "采集中"),
    EXTRACTING("extracting", "提取中"),
    ANALYZING("analyzing", "分析中"),
    WRITING("writing", "撰写中"),
    REVIEWING("reviewing", "审核中"),
    REPAIRING("repairing", "修复中"),
    WAITING_HUMAN_REVIEW("waiting_human_review", "等待人工审核"),
    COMPLETED("completed", "已完成"),
    COMPLETED_WITH_WARNINGS("completed_with_warnings", "已完成（含警告）"),
    FAILED("failed", "已失败");

    private final String code;
    private final String description;

    TaskStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
