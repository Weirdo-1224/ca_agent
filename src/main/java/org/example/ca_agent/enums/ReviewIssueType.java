package org.example.ca_agent.enums;

import lombok.Getter;

@Getter
public enum ReviewIssueType {

    MISSING_EVIDENCE("missing_evidence", "证据缺失"),
    EVIDENCE_NOT_LINKED("evidence_not_linked", "证据未关联"),
    SCHEMA_MISSING_FIELD("schema_missing_field", "Schema 字段缺失"),
    COMPARISON_INCOMPLETE("comparison_incomplete", "对比不完整"),
    VAGUE_FINDING("vague_finding", "发现描述模糊"),
    REPORT_MISSING_SECTION("report_missing_section", "报告缺少章节"),
    CITATION_FORMAT_ERROR("citation_format_error", "引用格式错误"),
    HALLUCINATION_RISK("hallucination_risk", "幻觉风险"),
    UNKNOWN_FIELD_TOO_MANY("unknown_field_too_many", "未知字段过多");

    private final String code;
    private final String description;

    ReviewIssueType(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
