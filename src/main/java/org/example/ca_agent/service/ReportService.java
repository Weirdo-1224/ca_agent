package org.example.ca_agent.service;

import lombok.RequiredArgsConstructor;
import org.example.ca_agent.dto.agent.ReportDraftDTO;
import org.example.ca_agent.dto.agent.ReviewResultDTO;
import org.example.ca_agent.dto.response.ReportResponse;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TaskService taskService;

    public ReportResponse getReport(String taskId) {
        CompetitiveAnalysisState state = taskService.getTaskState(taskId);
        ReportDraftDTO reportDraft = state.getReportDraft();
        ReviewResultDTO reviewResult = state.getReviewResult();

        ReportResponse response = new ReportResponse();
        response.setTaskId(taskId);
        response.setReportTitle(reportDraft.getReportTitle());
        response.setReportFormat(reportDraft.getReportFormat());
        response.setSections(reportDraft.getSections());
        response.setSourceList(reportDraft.getSourceList());
        response.setReviewResult(reviewResult);
        return response;
    }
}
