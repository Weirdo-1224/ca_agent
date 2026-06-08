package org.example.ca_agent.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ca_agent.assembler.StateAssembler;
import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.example.ca_agent.entity.AnalysisTaskEntity;
import org.example.ca_agent.enums.TaskStatus;
import org.example.ca_agent.repository.TaskRepository;
import org.example.ca_agent.workflow.CompetitiveAnalysisGraph;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final CompetitiveAnalysisGraph competitiveAnalysisGraph;
    private final StateAssembler stateAssembler;
    private final TaskRepository taskRepository;

    public CompetitiveAnalysisState run(TaskInputDTO taskInput) {
        return competitiveAnalysisGraph.run(taskInput);
    }

    @Async
    public void runAsync(TaskInputDTO taskInput) {
        try {
            log.info("[Async] Starting workflow for task: {}", taskInput.getTaskId());
            CompetitiveAnalysisState state = competitiveAnalysisGraph.run(taskInput);
            stateAssembler.saveState(state);
            log.info("[Async] Workflow completed for task: {}, status: {}", taskInput.getTaskId(), state.getStatus());
        } catch (Exception e) {
            log.error("[Async] Workflow failed for task: {}", taskInput.getTaskId(), e);
            markTaskFailed(taskInput.getTaskId(), e);
        }
    }

    private void markTaskFailed(String taskId, Exception e) {
        try {
            String errorMsg = e.getMessage() != null
                    ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500))
                    : "Unknown error";
            taskRepository.update(null, new UpdateWrapper<AnalysisTaskEntity>()
                    .eq("task_id", taskId)
                    .set("status", TaskStatus.FAILED.name())
                    .set("updated_at", LocalDateTime.now()));
            log.info("[Async] Marked task {} as FAILED, reason: {}", taskId, errorMsg);
        } catch (Exception updateEx) {
            log.error("[Async] Failed to mark task {} as FAILED", taskId, updateEx);
        }
    }
}
