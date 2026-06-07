package org.example.ca_agent.assembler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.ca_agent.common.JsonUtils;
import org.example.ca_agent.dto.agent.*;
import org.example.ca_agent.entity.*;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.enums.RepairType;
import org.example.ca_agent.enums.ReviewIssueType;
import org.example.ca_agent.enums.TaskStatus;
import org.example.ca_agent.repository.*;
import org.example.ca_agent.schema.Evidence;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EntityAssembler {

    private final TaskRepository taskRepository;
    private final EvidenceRepository evidenceRepository;
    private final ClaimRepository claimRepository;
    private final ReportRepository reportRepository;
    private final ReviewIssueRepository reviewIssueRepository;
    private final RepairInstructionRepository repairInstructionRepository;

    public CompetitiveAnalysisState loadState(String taskId) {
        CompetitiveAnalysisState state = new CompetitiveAnalysisState();

        // 1. 加载 taskInput + status + iterationCount
        AnalysisTaskEntity taskEntity = taskRepository.selectOne(
                new LambdaQueryWrapper<AnalysisTaskEntity>().eq(AnalysisTaskEntity::getTaskId, taskId));
        if (taskEntity == null) {
            return null;
        }

        state.setTaskInput(toTaskInput(taskEntity));
        state.setStatus(parseEnum(TaskStatus.class, taskEntity.getStatus()));
        state.setIterationCount(taskEntity.getIterationCount());

        // 2. 加载 evidencePool
        List<EvidenceEntity> evidenceEntities = evidenceRepository.selectList(
                new LambdaQueryWrapper<EvidenceEntity>().eq(EvidenceEntity::getTaskId, taskId));
        RawSourceSetDTO rawSourceSet = new RawSourceSetDTO();
        rawSourceSet.setTaskId(taskId);
        rawSourceSet.setEvidencePool(toEvidenceList(evidenceEntities));
        state.setRawSourceSet(rawSourceSet);

        // 3. 加载 claims -> productProfileSet（简化版：只填充 claims）
        List<ClaimEntity> claimEntities = claimRepository.selectList(
                new LambdaQueryWrapper<ClaimEntity>().eq(ClaimEntity::getTaskId, taskId));
        ProductProfileSetDTO profileSet = new ProductProfileSetDTO();
        profileSet.setTaskId(taskId);
        profileSet.setProducts(toProductProfiles(claimEntities));
        state.setProductProfileSet(profileSet);

        // 4. 加载 report
        ReportEntity reportEntity = reportRepository.selectOne(
                new LambdaQueryWrapper<ReportEntity>().eq(ReportEntity::getTaskId, taskId));
        if (reportEntity != null) {
            state.setReportDraft(toReportDraft(reportEntity));
        }

        // 5. 加载 reviewResult
        List<ReviewIssueEntity> issueEntities = reviewIssueRepository.selectList(
                new LambdaQueryWrapper<ReviewIssueEntity>().eq(ReviewIssueEntity::getTaskId, taskId));
        ReviewResultDTO reviewResult = new ReviewResultDTO();
        reviewResult.setTaskId(taskId);
        reviewResult.setPassed(issueEntities == null || issueEntities.isEmpty());
        reviewResult.setIssues(toReviewIssues(issueEntities));
        state.setReviewResult(reviewResult);

        // 6. 加载 repairInstructions
        List<RepairInstructionEntity> repairEntities = repairInstructionRepository.selectList(
                new LambdaQueryWrapper<RepairInstructionEntity>().eq(RepairInstructionEntity::getTaskId, taskId));
        state.setRepairInstructions(toRepairInstructions(repairEntities));

        return state;
    }

    private TaskInputDTO toTaskInput(AnalysisTaskEntity entity) {
        TaskInputDTO dto = new TaskInputDTO();
        dto.setTaskId(entity.getTaskId());
        dto.setTaskName(entity.getTaskName());
        dto.setDomain(entity.getDomain());
        dto.setTargetProducts(parseJsonList(entity.getTargetProductsJson(), String.class));
        dto.setAnalysisGoal(entity.getAnalysisGoal());
        dto.setMaxIterations(entity.getMaxIterations());
        return dto;
    }

    private List<Evidence> toEvidenceList(List<EvidenceEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        return entities.stream().map(e -> {
            Evidence ev = new Evidence();
            ev.setEvidenceId(e.getEvidenceId());
            ev.setProductName(e.getProductName());
            ev.setSourceType(parseEnum(org.example.ca_agent.enums.SourceType.class, e.getSourceType()));
            ev.setSourceTitle(e.getSourceTitle());
            ev.setUrl(e.getUrl());
            ev.setContentSnippet(e.getContentSnippet());
            ev.setCollectedAt(e.getCollectedAt());
            ev.setReliability(parseEnum(org.example.ca_agent.enums.ReliabilityLevel.class, e.getReliability()));
            ev.setUsedFor(parseJsonList(e.getUsedForJson(), String.class));
            return ev;
        }).toList();
    }

    private List<ProductProfileSetDTO.ProductProfile> toProductProfiles(List<ClaimEntity> claimEntities) {
        if (claimEntities == null || claimEntities.isEmpty()) {
            return Collections.emptyList();
        }
        // 按 productName 分组
        return claimEntities.stream()
                .map(ClaimEntity::getProductName)
                .distinct()
                .map(productName -> {
                    ProductProfileSetDTO.ProductProfile profile = new ProductProfileSetDTO.ProductProfile();
                    profile.setProductName(productName);
                    profile.setClaims(claimEntities.stream()
                            .filter(c -> productName.equals(c.getProductName()))
                            .map(this::toClaim)
                            .toList());
                    return profile;
                }).toList();
    }

    private org.example.ca_agent.schema.Claim toClaim(ClaimEntity entity) {
        org.example.ca_agent.schema.Claim claim = new org.example.ca_agent.schema.Claim();
        claim.setClaimId(entity.getClaimId());
        claim.setProductName(entity.getProductName());
        claim.setDimension(entity.getDimension());
        claim.setStatement(entity.getStatement());
        claim.setConfidence(entity.getConfidence());
        claim.setEvidenceIds(parseJsonList(entity.getEvidenceIdsJson(), String.class));
        claim.setRiskLevel(entity.getRiskLevel());
        return claim;
    }

    private ReportDraftDTO toReportDraft(ReportEntity entity) {
        ReportDraftDTO dto = new ReportDraftDTO();
        dto.setTaskId(entity.getTaskId());
        dto.setReportTitle(entity.getReportTitle());
        dto.setReportFormat(entity.getReportFormat());
        dto.setSections(parseJsonList(entity.getSectionsJson(), ReportDraftDTO.ReportSection.class));
        dto.setSourceList(parseJsonList(entity.getSourceListJson(), Evidence.class));
        return dto;
    }

    private List<ReviewResultDTO.ReviewIssue> toReviewIssues(List<ReviewIssueEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        return entities.stream().map(e -> {
            ReviewResultDTO.ReviewIssue issue = new ReviewResultDTO.ReviewIssue();
            issue.setIssueId(e.getIssueId());
            issue.setSeverity(e.getSeverity());
            issue.setType(parseEnum(ReviewIssueType.class, e.getType()));
            issue.setDescription(e.getDescription());
            issue.setTargetAgent(parseEnum(AgentType.class, e.getTargetAgent()));
            issue.setTargetProduct(e.getTargetProduct());
            issue.setTargetDimension(e.getTargetDimension());
            issue.setRepairInstruction(e.getRepairInstruction());
            return issue;
        }).toList();
    }

    private List<RepairInstructionDTO> toRepairInstructions(List<RepairInstructionEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        return entities.stream().map(e -> {
            RepairInstructionDTO dto = new RepairInstructionDTO();
            dto.setTaskId(e.getTaskId());
            dto.setRepairId(e.getRepairId());
            dto.setFromAgent(parseEnum(AgentType.class, e.getFromAgent()));
            dto.setTargetAgent(parseEnum(AgentType.class, e.getTargetAgent()));
            dto.setIssueIds(parseJsonList(e.getIssueIdsJson(), String.class));
            dto.setRepairType(parseEnum(RepairType.class, e.getRepairType()));
            dto.setTargetProduct(e.getTargetProduct());
            dto.setTargetDimension(e.getTargetDimension());
            dto.setInstruction(e.getInstruction());
            dto.setPriority(e.getPriority());
            return dto;
        }).toList();
    }

    private <T> List<T> parseJsonList(String json, Class<T> elementClass) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return JsonUtils.fromJsonList(json, elementClass);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> clazz, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(clazz, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
