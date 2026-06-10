package org.example.ca_agent.assembler;

import lombok.RequiredArgsConstructor;
import org.example.ca_agent.common.JsonUtils;
import org.example.ca_agent.dto.agent.*;
import org.example.ca_agent.entity.*;
import org.example.ca_agent.repository.*;
import org.example.ca_agent.schema.Evidence;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StateAssembler {

    private final TaskRepository taskRepository;
    private final EvidenceRepository evidenceRepository;
    private final ClaimRepository claimRepository;
    private final ReportRepository reportRepository;
    private final ReviewIssueRepository reviewIssueRepository;
    private final RepairInstructionRepository repairInstructionRepository;
    private final AgentRunRepository agentRunRepository;
    private final RepairDiffRepository repairDiffRepository;

    @Transactional
    public void saveState(CompetitiveAnalysisState state) {
        String taskId = state.getTaskInput().getTaskId();
        LocalDateTime now = LocalDateTime.now();

        // 1. 保存/更新 analysis_task
        saveAnalysisTask(state, now);

        // 2. 清除旧记录（除 analysis_task 外）
        evidenceRepository.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EvidenceEntity>()
                .eq(EvidenceEntity::getTaskId, taskId));
        claimRepository.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ClaimEntity>()
                .eq(ClaimEntity::getTaskId, taskId));
        reportRepository.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReportEntity>()
                .eq(ReportEntity::getTaskId, taskId));
        reviewIssueRepository.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewIssueEntity>()
                .eq(ReviewIssueEntity::getTaskId, taskId));
        repairInstructionRepository.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RepairInstructionEntity>()
                .eq(RepairInstructionEntity::getTaskId, taskId));

        // 3. 插入新记录
        saveEvidence(state, now);
        saveClaims(state, now);
        saveReport(state, now);
        saveReviewIssues(state, now);
        saveRepairInstructions(state, now);
        saveAgentRuns(state, now);
        saveRepairDiffs(state, now);
    }

    private void saveAnalysisTask(CompetitiveAnalysisState state, LocalDateTime now) {
        TaskInputDTO input = state.getTaskInput();
        AnalysisTaskEntity entity = taskRepository.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AnalysisTaskEntity>()
                        .eq(AnalysisTaskEntity::getTaskId, input.getTaskId()));

        if (entity == null) {
            entity = new AnalysisTaskEntity();
            entity.setTaskId(input.getTaskId());
            entity.setCreatedAt(now);
        }
        entity.setTaskName(input.getTaskName());
        entity.setDomain(input.getDomain());
        entity.setTargetProductsJson(JsonUtils.toJson(input.getTargetProducts()));
        entity.setAnalysisGoal(input.getAnalysisGoal());
        entity.setStatus(Optional.ofNullable(state.getStatus()).map(Enum::name).orElse(null));
        entity.setIterationCount(state.getIterationCount());
        entity.setMaxIterations(input.getMaxIterations());

        // 持久化质检结果摘要（score, summary, passed, nextAction）
        ReviewResultDTO reviewResult = state.getReviewResult();
        if (reviewResult != null) {
            entity.setReviewPassed(reviewResult.getPassed());
            entity.setReviewScore(reviewResult.getScore());
            entity.setReviewSummary(reviewResult.getSummary());
            entity.setNextActionJson(JsonUtils.toJson(reviewResult.getNextAction()));
        }

        entity.setUpdatedAt(now);

        if (entity.getId() == null) {
            taskRepository.insert(entity);
        } else {
            taskRepository.updateById(entity);
        }
    }

    private void saveEvidence(CompetitiveAnalysisState state, LocalDateTime now) {
        RawSourceSetDTO rawSourceSet = state.getRawSourceSet();
        if (rawSourceSet == null || rawSourceSet.getEvidencePool() == null) {
            return;
        }
        List<Evidence> evidencePool = rawSourceSet.getEvidencePool();
        for (Evidence evidence : evidencePool) {
            EvidenceEntity entity = new EvidenceEntity();
            entity.setEvidenceId(evidence.getEvidenceId());
            entity.setTaskId(state.getTaskInput().getTaskId());
            entity.setProductName(evidence.getProductName());
            entity.setSourceType(Optional.ofNullable(evidence.getSourceType()).map(Enum::name).orElse(null));
            entity.setSourceTitle(evidence.getSourceTitle());
            entity.setUrl(evidence.getUrl());
            entity.setContentSnippet(evidence.getContentSnippet());
            entity.setCollectedAt(evidence.getCollectedAt());
            entity.setReliability(Optional.ofNullable(evidence.getReliability()).map(Enum::name).orElse(null));
            entity.setUsedForJson(JsonUtils.toJson(evidence.getUsedFor()));
            entity.setCreatedAt(now);
            evidenceRepository.insert(entity);
        }
    }

    private void saveClaims(CompetitiveAnalysisState state, LocalDateTime now) {
        ProductProfileSetDTO profileSet = state.getProductProfileSet();
        if (profileSet == null || profileSet.getProducts() == null) {
            return;
        }
        for (ProductProfileSetDTO.ProductProfile product : profileSet.getProducts()) {
            if (product.getClaims() == null) {
                continue;
            }
            for (org.example.ca_agent.schema.Claim claim : product.getClaims()) {
                ClaimEntity entity = new ClaimEntity();
                entity.setClaimId(claim.getClaimId());
                entity.setTaskId(state.getTaskInput().getTaskId());
                entity.setProductName(claim.getProductName());
                entity.setDimension(claim.getDimension());
                entity.setStatement(claim.getStatement());
                entity.setConfidence(claim.getConfidence());
                entity.setEvidenceIdsJson(JsonUtils.toJson(claim.getEvidenceIds()));
                entity.setRiskLevel(claim.getRiskLevel());
                entity.setCreatedAt(now);
                claimRepository.insert(entity);
            }
        }
    }

    private void saveReport(CompetitiveAnalysisState state, LocalDateTime now) {
        ReportDraftDTO reportDraft = state.getReportDraft();
        if (reportDraft == null) {
            return;
        }
        ReportEntity entity = new ReportEntity();
        entity.setReportId("report_" + state.getTaskInput().getTaskId());
        entity.setTaskId(state.getTaskInput().getTaskId());
        entity.setReportTitle(reportDraft.getReportTitle());
        entity.setReportFormat(reportDraft.getReportFormat());
        entity.setSectionsJson(JsonUtils.toJson(reportDraft.getSections()));
        entity.setSourceListJson(JsonUtils.toJson(reportDraft.getSourceList()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        reportRepository.insert(entity);
    }

    private void saveReviewIssues(CompetitiveAnalysisState state, LocalDateTime now) {
        ReviewResultDTO reviewResult = state.getReviewResult();
        if (reviewResult == null || reviewResult.getIssues() == null) {
            return;
        }
        for (ReviewResultDTO.ReviewIssue issue : reviewResult.getIssues()) {
            ReviewIssueEntity entity = new ReviewIssueEntity();
            entity.setIssueId(issue.getIssueId());
            entity.setTaskId(state.getTaskInput().getTaskId());
            entity.setSeverity(issue.getSeverity());
            entity.setType(Optional.ofNullable(issue.getType()).map(Enum::name).orElse(null));
            entity.setDescription(issue.getDescription());
            entity.setTargetAgent(Optional.ofNullable(issue.getTargetAgent()).map(Enum::name).orElse(null));
            entity.setTargetProduct(issue.getTargetProduct());
            entity.setTargetDimension(issue.getTargetDimension());
            entity.setRepairInstruction(issue.getRepairInstruction());
            entity.setCreatedAt(now);
            reviewIssueRepository.insert(entity);
        }
    }

    private void saveRepairInstructions(CompetitiveAnalysisState state, LocalDateTime now) {
        List<RepairInstructionDTO> instructions = state.getRepairInstructions();
        if (instructions == null || instructions.isEmpty()) {
            return;
        }
        for (RepairInstructionDTO dto : instructions) {
            RepairInstructionEntity entity = new RepairInstructionEntity();
            entity.setInstructionId("inst_" + UUID.randomUUID().toString().replace("-", ""));
            entity.setTaskId(state.getTaskInput().getTaskId());
            entity.setRepairId(dto.getRepairId());
            entity.setFromAgent(Optional.ofNullable(dto.getFromAgent()).map(Enum::name).orElse(null));
            entity.setTargetAgent(Optional.ofNullable(dto.getTargetAgent()).map(Enum::name).orElse(null));
            entity.setIssueIdsJson(JsonUtils.toJson(dto.getIssueIds()));
            entity.setRepairType(Optional.ofNullable(dto.getRepairType()).map(Enum::name).orElse(null));
            entity.setTargetProduct(dto.getTargetProduct());
            entity.setTargetSection(dto.getTargetSection());
            entity.setTargetDimension(dto.getTargetDimension());
            entity.setProblemType(dto.getProblemType());
            entity.setExpectedFix(dto.getExpectedFix());
            entity.setRelatedEvidenceIdsJson(JsonUtils.toJson(dto.getRelatedEvidenceIds()));
            entity.setRelatedClaimIdsJson(JsonUtils.toJson(dto.getRelatedClaimIds()));
            entity.setIteration(dto.getIteration());
            entity.setInstruction(dto.getInstruction());
            entity.setPriority(dto.getPriority());
            entity.setCreatedAt(now);
            repairInstructionRepository.insert(entity);
        }
    }

    /**
     * 持久化 RepairDiff 记录。
     * 采用「跳过已存在 iteration」的策略，避免每次 saveState 重复插入。
     */
    private void saveRepairDiffs(CompetitiveAnalysisState state, LocalDateTime now) {
        List<RepairDiffDTO> diffs = state.getRepairDiffs();
        if (diffs == null || diffs.isEmpty()) {
            return;
        }
        String taskId = state.getTaskInput().getTaskId();

        // 查询已持久化的 iteration 集合
        Set<Integer> existingIterations = repairDiffRepository.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RepairDiffEntity>()
                        .eq(RepairDiffEntity::getTaskId, taskId)
                        .select(RepairDiffEntity::getIteration)
        ).stream().map(RepairDiffEntity::getIteration).collect(Collectors.toSet());

        // 只插入新的 diff
        for (RepairDiffDTO diff : diffs) {
            if (diff.getIteration() != null && existingIterations.contains(diff.getIteration())) {
                continue;
            }
            RepairDiffEntity entity = new RepairDiffEntity();
            entity.setTaskId(taskId);
            entity.setIteration(diff.getIteration());
            entity.setTargetAgent(diff.getTargetAgent());
            entity.setBeforeScore(diff.getBeforeScore());
            entity.setAfterScore(diff.getAfterScore());
            entity.setBeforeIssueCount(diff.getBeforeIssueCount());
            entity.setAfterIssueCount(diff.getAfterIssueCount());
            entity.setFixedIssueCount(diff.getFixedIssueCount());
            entity.setAddedEvidenceIdsJson(JsonUtils.toJson(diff.getAddedEvidenceIds()));
            entity.setAddedClaimIdsJson(JsonUtils.toJson(diff.getAddedClaimIds()));
            entity.setChangedSectionsJson(JsonUtils.toJson(diff.getChangedSectionTitles()));
            entity.setChangedProductsJson(JsonUtils.toJson(diff.getChangedProducts()));
            entity.setSummary(diff.getSummary());
            entity.setCreatedAt(now);
            repairDiffRepository.insert(entity);
        }
    }

    private void saveAgentRuns(CompetitiveAnalysisState state, LocalDateTime now) {
        if (state.getAgentRuns() == null || state.getAgentRuns().isEmpty()) {
            return;
        }
        for (org.example.ca_agent.dto.agent.AgentRunTrace trace : state.getAgentRuns()) {
            if (trace.isPersisted()) {
                continue;
            }
            AgentRunEntity entity = new AgentRunEntity();
            entity.setRunId(trace.getRunId());
            entity.setTaskId(trace.getTaskId());
            entity.setAgentType(trace.getAgentType() != null ? trace.getAgentType().name() : null);
            entity.setInputType(trace.getInputType());
            entity.setOutputType(trace.getOutputType());
            entity.setStatus(trace.getStatus());
            entity.setStartTime(trace.getStartTime());
            entity.setEndTime(trace.getEndTime());
            entity.setDurationMs(trace.getDurationMs());
            entity.setErrorMessage(trace.getErrorMessage());
            entity.setPromptTokens(trace.getPromptTokens());
            entity.setCompletionTokens(trace.getCompletionTokens());
            entity.setTotalTokens(trace.getTotalTokens());
            entity.setLlmCallsJson(JsonUtils.toJson(trace.getLlmCalls()));
            agentRunRepository.insert(entity);
            trace.setPersisted(true);
        }
    }
}
