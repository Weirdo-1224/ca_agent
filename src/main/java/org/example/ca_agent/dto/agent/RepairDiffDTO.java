package org.example.ca_agent.dto.agent;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 修复前后变化的 Diff 记录。
 * 每一轮修复生成一条 RepairDiffDTO，用于前端展示修复闭环。
 */
@Data
public class RepairDiffDTO {

    private String taskId;
    private Integer iteration;
    private String targetAgent;

    /** 修复前 Reviewer 评分 */
    private Integer beforeScore;
    /** 修复后 Reviewer 评分 */
    private Integer afterScore;
    /** 修复前 issue 数量 */
    private Integer beforeIssueCount;
    /** 修复后 issue 数量 */
    private Integer afterIssueCount;
    /** 已修复 issue 数量 */
    private Integer fixedIssueCount;

    /** 本轮新增的证据 ID 列表 */
    private List<String> addedEvidenceIds;
    /** 本轮新增的 Claim ID 列表 */
    private List<String> addedClaimIds;
    /** 内容发生变化的报告章节标题 */
    private List<String> changedSectionTitles;
    /** 本轮涉及的产品名称 */
    private List<String> changedProducts;

    /** 自然语言总结 */
    private String summary;
    private LocalDateTime createdAt;
}
