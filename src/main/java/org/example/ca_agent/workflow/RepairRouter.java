package org.example.ca_agent.workflow;

import lombok.RequiredArgsConstructor;
import org.example.ca_agent.dto.agent.RepairInstructionDTO;
import org.example.ca_agent.dto.agent.ReviewResultDTO;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.enums.RepairType;
import org.example.ca_agent.enums.ReviewIssueType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

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
        instruction.setInstruction(selectedIssues.stream()
                .map(ReviewResultDTO.ReviewIssue::getRepairInstruction)
                .toList()
                .toString());
        instruction.setPriority(hasHighSeverity(selectedIssues) ? "high" : "medium");
        return instruction;
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
