package org.example.ca_agent.controller;

import lombok.RequiredArgsConstructor;
import org.example.ca_agent.common.Result;
import org.example.ca_agent.dto.agent.ReviewResultDTO;
import org.example.ca_agent.dto.request.TaskCreateRequest;
import org.example.ca_agent.dto.response.ReportResponse;
import org.example.ca_agent.dto.response.TaskDetailResponse;
import org.example.ca_agent.schema.Evidence;
import org.example.ca_agent.dto.response.AgentRunResponse;
import org.example.ca_agent.service.AgentRunService;
import org.example.ca_agent.service.EvidenceService;
import org.example.ca_agent.service.ReportService;
import org.example.ca_agent.service.TaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class AnalysisTaskController {

    private final TaskService taskService;
    private final ReportService reportService;
    private final EvidenceService evidenceService;
    private final AgentRunService agentRunService;

    @PostMapping
    public Result<TaskDetailResponse> createTask(@RequestBody TaskCreateRequest request) {
        return Result.success(taskService.createTask(request));
    }

    @GetMapping("/{taskId}")
    public Result<TaskDetailResponse> getTaskDetail(@PathVariable String taskId) {
        return Result.success(taskService.getTaskDetail(taskId));
    }

    @GetMapping("/{taskId}/report")
    public Result<ReportResponse> getReport(@PathVariable String taskId) {
        return Result.success(reportService.getReport(taskId));
    }

    @GetMapping("/{taskId}/evidence")
    public Result<List<Evidence>> getEvidence(@PathVariable String taskId) {
        return Result.success(evidenceService.getEvidenceList(taskId));
    }

    @GetMapping("/{taskId}/review")
    public Result<ReviewResultDTO> getReview(@PathVariable String taskId) {
        return Result.success(taskService.getTaskState(taskId).getReviewResult());
    }

    @GetMapping("/{taskId}/agent-runs")
    public Result<List<AgentRunResponse>> getAgentRuns(@PathVariable String taskId) {
        return Result.success(agentRunService.getAgentRuns(taskId));
    }
}
