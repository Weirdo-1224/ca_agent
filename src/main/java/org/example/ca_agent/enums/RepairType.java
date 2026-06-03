package org.example.ca_agent.enums;

import lombok.Getter;

@Getter
public enum RepairType {

    SUPPLEMENT_EVIDENCE("supplement_evidence", "补充证据"),
    RELINK_EVIDENCE("relink_evidence", "重新关联证据"),
    COMPLETE_SCHEMA("complete_schema", "补全 Schema"),
    COMPLETE_COMPARISON("complete_comparison", "补全对比"),
    REWRITE_ANALYSIS("rewrite_analysis", "重写分析"),
    REWRITE_REPORT("rewrite_report", "重写报告"),
    FIX_CITATION("fix_citation", "修复引用"),
    REMOVE_OR_VERIFY_CLAIM("remove_or_verify_claim", "移除或核实主张");

    private final String code;
    private final String description;

    RepairType(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
