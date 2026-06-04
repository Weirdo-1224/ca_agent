package org.example.ca_agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.ca_agent.common.BizException;
import org.example.ca_agent.common.JsonUtils;
import org.example.ca_agent.dto.agent.ReportDraftDTO;
import org.example.ca_agent.dto.agent.ReviewResultDTO;
import org.example.ca_agent.dto.response.ReportResponse;
import org.example.ca_agent.entity.AnalysisTaskEntity;
import org.example.ca_agent.entity.ReportEntity;
import org.example.ca_agent.entity.ReviewIssueEntity;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.enums.ReviewIssueType;
import org.example.ca_agent.repository.ReportRepository;
import org.example.ca_agent.repository.ReviewIssueRepository;
import org.example.ca_agent.repository.TaskRepository;
import org.example.ca_agent.schema.Evidence;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReviewIssueRepository reviewIssueRepository;
    private final TaskRepository taskRepository;

    public ReportResponse getReport(String taskId) {
        ensureTaskExists(taskId);
        ReportEntity reportEntity = reportRepository.selectOne(
                new LambdaQueryWrapper<ReportEntity>().eq(ReportEntity::getTaskId, taskId));

        List<ReviewIssueEntity> issueEntities = reviewIssueRepository.selectList(
                new LambdaQueryWrapper<ReviewIssueEntity>().eq(ReviewIssueEntity::getTaskId, taskId));

        ReportResponse response = new ReportResponse();
        response.setTaskId(taskId);

        if (reportEntity != null) {
            response.setReportTitle(reportEntity.getReportTitle());
            response.setReportFormat(reportEntity.getReportFormat());
            response.setSections(parseJsonList(reportEntity.getSectionsJson(), ReportDraftDTO.ReportSection.class));
            response.setSourceList(parseJsonList(reportEntity.getSourceListJson(), Evidence.class));
        }

        response.setReviewResult(toReviewResult(taskId, issueEntities));
        return response;
    }

    private void ensureTaskExists(String taskId) {
        AnalysisTaskEntity entity = taskRepository.selectOne(
                new LambdaQueryWrapper<AnalysisTaskEntity>().eq(AnalysisTaskEntity::getTaskId, taskId));
        if (entity == null) {
            throw new BizException(404, "Task not found: " + taskId);
        }
    }

    private ReviewResultDTO toReviewResult(String taskId, List<ReviewIssueEntity> issueEntities) {
        ReviewResultDTO result = new ReviewResultDTO();
        result.setTaskId(taskId);
        result.setPassed(issueEntities == null || issueEntities.isEmpty());
        result.setIssues(issueEntities.stream().map(this::toReviewIssue).toList());
        return result;
    }

    private ReviewResultDTO.ReviewIssue toReviewIssue(ReviewIssueEntity entity) {
        ReviewResultDTO.ReviewIssue issue = new ReviewResultDTO.ReviewIssue();
        issue.setIssueId(entity.getIssueId());
        issue.setSeverity(entity.getSeverity());
        issue.setType(parseEnum(ReviewIssueType.class, entity.getType()));
        issue.setDescription(entity.getDescription());
        issue.setTargetAgent(parseEnum(AgentType.class, entity.getTargetAgent()));
        issue.setTargetProduct(entity.getTargetProduct());
        issue.setTargetDimension(entity.getTargetDimension());
        issue.setRepairInstruction(entity.getRepairInstruction());
        return issue;
    }

    private <T> List<T> parseJsonList(String json, Class<T> elementClass) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return JsonUtils.fromJsonList(json, elementClass);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> clazz, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(clazz, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
