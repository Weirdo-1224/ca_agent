package org.example.ca_agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ca_agent.assembler.EntityAssembler;
import org.example.ca_agent.assembler.StateAssembler;
import org.example.ca_agent.common.BizException;
import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.example.ca_agent.dto.request.TaskCreateRequest;
import org.example.ca_agent.dto.response.TaskDetailResponse;
import org.example.ca_agent.entity.AnalysisTaskEntity;
import org.example.ca_agent.enums.TaskStatus;
import org.example.ca_agent.repository.TaskRepository;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final WorkflowService workflowService;
    private final StateAssembler stateAssembler;
    private final EntityAssembler entityAssembler;
    private final TaskRepository taskRepository;

    public TaskDetailResponse createTask(TaskCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        TaskInputDTO taskInput = toTaskInput(request);

        // 1. 初始化状态并立即保存到数据库
        CompetitiveAnalysisState initialState = new CompetitiveAnalysisState();
        initialState.setTaskInput(taskInput);
        initialState.setStatus(TaskStatus.CREATED);
        initialState.setIterationCount(0);
        stateAssembler.saveState(initialState);

        // 2. 异步执行工作流（通过另一个 bean 调用，确保 @Async 代理生效）
        workflowService.runAsync(taskInput);

        // 3. 立即返回
        return toTaskDetailResponse(initialState, now);
    }

    public CompetitiveAnalysisState getTaskState(String taskId) {
        CompetitiveAnalysisState state = entityAssembler.loadState(taskId);
        if (state == null) {
            throw new BizException(404, "Task not found: " + taskId);
        }
        return state;
    }

    public TaskDetailResponse getTaskDetail(String taskId) {
        AnalysisTaskEntity entity = taskRepository.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AnalysisTaskEntity>()
                        .eq(AnalysisTaskEntity::getTaskId, taskId));
        if (entity == null) {
            throw new BizException(404, "Task not found: " + taskId);
        }
        return toTaskDetailResponse(entity);
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

    private TaskDetailResponse toTaskDetailResponse(CompetitiveAnalysisState state, LocalDateTime createdAt) {
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
        response.setCreatedAt(createdAt);
        response.setUpdatedAt(LocalDateTime.now());
        return response;
    }

    private TaskDetailResponse toTaskDetailResponse(AnalysisTaskEntity entity) {
        TaskDetailResponse response = new TaskDetailResponse();
        response.setTaskId(entity.getTaskId());
        response.setTaskName(entity.getTaskName());
        response.setDomain(entity.getDomain());
        response.setTargetProducts(parseJsonList(entity.getTargetProductsJson()));
        response.setAnalysisGoal(entity.getAnalysisGoal());
        try {
            response.setStatus(org.example.ca_agent.enums.TaskStatus.valueOf(entity.getStatus()));
        } catch (Exception e) {
            response.setStatus(null);
        }
        response.setIterationCount(entity.getIterationCount());
        response.setMaxIterations(entity.getMaxIterations());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    @SuppressWarnings("unchecked")
    private java.util.List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return java.util.Collections.emptyList();
        try {
            return org.example.ca_agent.common.JsonUtils.fromJson(json, java.util.List.class);
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }
}
