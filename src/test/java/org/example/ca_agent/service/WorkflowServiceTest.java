package org.example.ca_agent.service;

import org.example.ca_agent.assembler.StateAssembler;
import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.example.ca_agent.entity.AnalysisTaskEntity;
import org.example.ca_agent.enums.TaskStatus;
import org.example.ca_agent.repository.TaskRepository;
import org.example.ca_agent.workflow.CompetitiveAnalysisGraph;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock CompetitiveAnalysisGraph competitiveAnalysisGraph;
    @Mock StateAssembler stateAssembler;
    @Mock TaskRepository taskRepository;

    @InjectMocks WorkflowService workflowService;

    @Test
    void runAsync_onSuccess_savesState() {
        TaskInputDTO input = buildInput("task-ok");
        CompetitiveAnalysisState state = new CompetitiveAnalysisState();
        state.setTaskInput(input);
        state.setStatus(TaskStatus.COMPLETED);
        when(competitiveAnalysisGraph.run(input)).thenReturn(state);

        workflowService.runAsync(input);

        verify(stateAssembler).saveState(state);
        verify(taskRepository, never()).update(any(), any());
    }

    @Test
    void runAsync_onException_marksTaskAsFailed() {
        TaskInputDTO input = buildInput("task-fail");
        when(competitiveAnalysisGraph.run(input)).thenThrow(new RuntimeException("LLM timeout"));

        workflowService.runAsync(input);

        verify(stateAssembler, never()).saveState(any());
        verify(taskRepository).update(isNull(), argThat(wrapper -> wrapper != null));
    }

    @Test
    void runAsync_onException_doesNotThrow() {
        TaskInputDTO input = buildInput("task-fail-2");
        when(competitiveAnalysisGraph.run(input)).thenThrow(new RuntimeException("Network error"));

        // 不应抛出异常——异步方法需要静默处理
        workflowService.runAsync(input);
    }

    @Test
    void runAsync_onException_handlesNullMessage() {
        TaskInputDTO input = buildInput("task-null-msg");
        when(competitiveAnalysisGraph.run(input)).thenThrow(new RuntimeException((String) null));

        // 不应抛出 NPE
        workflowService.runAsync(input);

        verify(taskRepository).update(isNull(), any());
    }

    @Test
    void runAsync_markFailed_survivesDbError() {
        TaskInputDTO input = buildInput("task-db-fail");
        when(competitiveAnalysisGraph.run(input)).thenThrow(new RuntimeException("Boom"));
        when(taskRepository.update(isNull(), any())).thenThrow(new RuntimeException("DB is down"));

        // 即使 DB 更新失败，也不应传播异常
        workflowService.runAsync(input);
    }

    private TaskInputDTO buildInput(String taskId) {
        TaskInputDTO input = new TaskInputDTO();
        input.setTaskId(taskId);
        input.setTaskName("测试任务");
        return input;
    }
}
