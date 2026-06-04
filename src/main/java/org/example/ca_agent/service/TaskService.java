package org.example.ca_agent.service;

import lombok.RequiredArgsConstructor;
import org.example.ca_agent.common.BizException;
import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.example.ca_agent.dto.request.TaskCreateRequest;
import org.example.ca_agent.dto.response.TaskDetailResponse;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final WorkflowService workflowService;
    private final Map<String, CompetitiveAnalysisState> taskStateStore = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> taskCreatedAtStore = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> taskUpdatedAtStore = new ConcurrentHashMap<>();

    public TaskDetailResponse createTask(TaskCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        TaskInputDTO taskInput = toTaskInput(request);
        CompetitiveAnalysisState state = workflowService.run(taskInput);
        taskStateStore.put(taskInput.getTaskId(), state);
        taskCreatedAtStore.put(taskInput.getTaskId(), now);
        taskUpdatedAtStore.put(taskInput.getTaskId(), LocalDateTime.now());
        return toTaskDetailResponse(state);
    }

    public CompetitiveAnalysisState getTaskState(String taskId) {
        CompetitiveAnalysisState state = taskStateStore.get(taskId);
        if (state == null) {
            throw new BizException(404, "Task not found: " + taskId);
        }
        return state;
    }

    public TaskDetailResponse getTaskDetail(String taskId) {
        return toTaskDetailResponse(getTaskState(taskId));
    }

    private TaskInputDTO toTaskInput(TaskCreateRequest request) {
        TaskInputDTO taskInput = new TaskInputDTO();
        taskInput.setTaskId("task_" + UUID.randomUUID().toString().replace("-", ""));
        taskInput.setTaskName(request.getTaskName());
        taskInput.setDomain(request.getDomain());
        taskInput.setTargetProducts(request.getTargetProducts());
        taskInput.setAnalysisGoal(request.getAnalysisGoal());
        taskInput.setOutputFormat(request.getOutputFormat());
        taskInput.setLanguage(request.getLanguage());
        taskInput.setMaxIterations(request.getMaxIterations());
        return taskInput;
    }

    private TaskDetailResponse toTaskDetailResponse(CompetitiveAnalysisState state) {
        TaskInputDTO taskInput = state.getTaskInput();
        TaskDetailResponse response = new TaskDetailResponse();
        response.setTaskId(taskInput.getTaskId());
        response.setTaskName(taskInput.getTaskName());
        response.setDomain(taskInput.getDomain());
        response.setTargetProducts(taskInput.getTargetProducts());
        response.setAnalysisGoal(taskInput.getAnalysisGoal());
        response.setStatus(state.getStatus());
        response.setIterationCount(state.getIterationCount());
        response.setMaxIterations(taskInput.getMaxIterations());
        response.setCreatedAt(taskCreatedAtStore.get(taskInput.getTaskId()));
        response.setUpdatedAt(taskUpdatedAtStore.get(taskInput.getTaskId()));
        return response;
    }
}
