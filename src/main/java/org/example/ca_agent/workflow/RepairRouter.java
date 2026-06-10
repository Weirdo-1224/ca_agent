package org.example.ca_agent.workflow;

import lombok.RequiredArgsConstructor;
import org.example.ca_agent.dto.agent.RepairInstructionDTO;
import org.example.ca_agent.dto.agent.ReviewResultDTO;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.enums.RepairType;
import org.example.ca_agent.enums.ReviewIssueType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RepairRouter {

    public AgentType chooseEarliestTargetAgent(List<ReviewResultDTO.ReviewIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return null;
        }
        if (containsTargetAgent(issues, AgentType.COLLECTOR_AGENT)) {
            return AgentType.COLLECTOR_AGENT;
        }
        if (containsTargetAgent(issues, AgentType.EXTRACTOR_AGENT)) {
            return AgentType.EXTRACTOR_AGENT;
        }
        if (containsTargetAgent(issues, AgentType.ANALYZER_AGENT)) {
            return AgentType.ANALYZER_AGENT;
        }
        if (containsTargetAgent(issues, AgentType.WRITER_AGENT)) {
            return AgentType.WRITER_AGENT;
        }
        return null;
    }

    public RepairInstructionDTO buildRepairInstruction(CompetitiveAnalysisState state) {
        List<ReviewResultDTO.ReviewIssue> selectedIssues = selectIssues(state.getReviewResult().getIssues());
        ReviewResultDTO.ReviewIssue firstIssue = selectedIssues.get(0);

        RepairInstructionDTO instruction = new RepairInstructionDTO();
        instruction.setTaskId(state.getTaskInput().getTaskId());
        instruction.setRepairId("repair_" + UUID.randomUUID());
        instruction.setFromAgent(AgentType.REVIEWER_AGENT);
        instruction.setTargetAgent(chooseEarliestTargetAgent(selectedIssues));
        instruction.setIssueIds(selectedIssues.stream()
                .map(ReviewResultDTO.ReviewIssue::getIssueId)
                .toList());
        instruction.setRepairType(toRepairType(firstIssue.getType()));
        instruction.setTargetProduct(firstIssue.getTargetProduct());
        instruction.setTargetDimension(firstIssue.getTargetDimension());
        instruction.setIteration(state.getIterationCount() != null ? state.getIterationCount() + 1 : 1);
        instruction.setInstruction(selectedIssues.stream()
                .map(ReviewResultDTO.ReviewIssue::getRepairInstruction)
                .toList()
                .toString());
        instruction.setPriority(hasHighSeverity(selectedIssues) ? "high" : "medium");

        // 增强字段：problemType、expectedFix、relatedEvidenceIds、relatedClaimIds
        instruction.setProblemType(firstIssue.getType() != null ? firstIssue.getType().name() : null);
        instruction.setExpectedFix(buildExpectedFix(firstIssue));
        instruction.setRelatedEvidenceIds(collectRelatedEvidenceIds(selectedIssues, state));
        instruction.setRelatedClaimIds(collectRelatedClaimIds(selectedIssues, state));

        return instruction;
    }

    /**
     * 根据问题类型生成期望修复动作的自然语言描述。
     */
    private String buildExpectedFix(ReviewResultDTO.ReviewIssue issue) {
        if (issue.getType() == null) {
            return "根据审核意见修复相关问题";
        }
        return switch (issue.getType()) {
            case MISSING_EVIDENCE -> "补充缺失的证据来源，增加可信网页采集";
            case UNKNOWN_FIELD_TOO_MANY -> "补充目标产品的关键信息字段";
            case EVIDENCE_NOT_LINKED -> "修正证据引用关系，确保结论与证据关联";
            case SCHEMA_MISSING_FIELD -> "完善产品画像中缺失的结构化字段";
            case COMPARISON_INCOMPLETE -> "补充对比分析矩阵，覆盖所有产品维度";
            case VAGUE_FINDING -> "重写分析结论，提供更具体、可量化的洞察";
            case REPORT_MISSING_SECTION -> "补充报告中缺失的章节内容";
            case CITATION_FORMAT_ERROR -> "修正报告中的引用格式，确保来源标注规范";
            case HALLUCINATION_RISK -> "删除或验证疑似幻觉内容，确保基于真实证据";
        };
    }

    /**
     * 从当前证据池中收集与问题相关的 evidenceId（按产品匹配）。
     * 容错处理：如果无法匹配，返回空列表。
     */
    private List<String> collectRelatedEvidenceIds(List<ReviewResultDTO.ReviewIssue> issues,
                                                   CompetitiveAnalysisState state) {
        try {
            Set<String> targetProducts = issues.stream()
                    .map(ReviewResultDTO.ReviewIssue::getTargetProduct)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (targetProducts.isEmpty() || state.getRawSourceSet() == null
                    || state.getRawSourceSet().getEvidencePool() == null) {
                return List.of();
            }
            return state.getRawSourceSet().getEvidencePool().stream()
                    .filter(e -> targetProducts.contains(e.getProductName()))
                    .map(org.example.ca_agent.schema.Evidence::getEvidenceId)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 从当前产品画像中收集与问题相关的 claimId（按产品匹配）。
     * 容错处理：如果无法匹配，返回空列表。
     */
    private List<String> collectRelatedClaimIds(List<ReviewResultDTO.ReviewIssue> issues,
                                                CompetitiveAnalysisState state) {
        try {
            Set<String> targetProducts = issues.stream()
                    .map(ReviewResultDTO.ReviewIssue::getTargetProduct)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (targetProducts.isEmpty() || state.getProductProfileSet() == null
                    || state.getProductProfileSet().getProducts() == null) {
                return List.of();
            }
            return state.getProductProfileSet().getProducts().stream()
                    .filter(p -> targetProducts.contains(p.getProductName()))
                    .flatMap(p -> p.getClaims() != null ? p.getClaims().stream() : java.util.stream.Stream.empty())
                    .map(org.example.ca_agent.schema.Claim::getClaimId)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<ReviewResultDTO.ReviewIssue> selectIssues(List<ReviewResultDTO.ReviewIssue> issues) {
        List<ReviewResultDTO.ReviewIssue> highIssues = issues.stream()
                .filter(issue -> "high".equalsIgnoreCase(issue.getSeverity()))
                .toList();
        return highIssues.isEmpty() ? issues : highIssues;
    }

    private boolean containsTargetAgent(List<ReviewResultDTO.ReviewIssue> issues, AgentType agentType) {
        return issues.stream().anyMatch(issue -> agentType == issue.getTargetAgent());
    }

    private boolean hasHighSeverity(List<ReviewResultDTO.ReviewIssue> issues) {
        return issues.stream().anyMatch(issue -> "high".equalsIgnoreCase(issue.getSeverity()));
    }

    private RepairType toRepairType(ReviewIssueType issueType) {
        return switch (issueType) {
            case MISSING_EVIDENCE, UNKNOWN_FIELD_TOO_MANY -> RepairType.SUPPLEMENT_EVIDENCE;
            case EVIDENCE_NOT_LINKED -> RepairType.RELINK_EVIDENCE;
            case SCHEMA_MISSING_FIELD -> RepairType.COMPLETE_SCHEMA;
            case COMPARISON_INCOMPLETE -> RepairType.COMPLETE_COMPARISON;
            case VAGUE_FINDING -> RepairType.REWRITE_ANALYSIS;
            case REPORT_MISSING_SECTION -> RepairType.REWRITE_REPORT;
            case CITATION_FORMAT_ERROR -> RepairType.FIX_CITATION;
            case HALLUCINATION_RISK -> RepairType.REMOVE_OR_VERIFY_CLAIM;
        };
    }
}
