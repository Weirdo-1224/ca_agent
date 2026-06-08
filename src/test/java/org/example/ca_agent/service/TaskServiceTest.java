package org.example.ca_agent.service;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock WorkflowService workflowService;
    @Mock StateAssembler stateAssembler;
    @Mock EntityAssembler entityAssembler;
    @Mock TaskRepository taskRepository;

    @InjectMocks TaskService taskService;

    // ---------- createTask ----------

    @Test
    void createTask_savesInitialStateAndTriggersAsync() {
        TaskCreateRequest request = buildRequest();

        TaskDetailResponse response = taskService.createTask(request);

        // 验证初始状态保存
        verify(stateAssembler).saveState(argThat(state ->
            state.getStatus() == TaskStatus.CREATED &&
            state.getTaskInput().getTaskName().equals("编程工具竞品分析")
        ));
        // 验证异步工作流被触发
        verify(workflowService).runAsync(argThat(input ->
            input.getTaskName().equals("编程工具竞品分析") &&
            input.getDomain().equals("AI_CODING_TOOLS") &&
            input.getMaxIterations() == 2
        ));
        // 立即返回 CREATED 状态
        assertNotNull(response.getTaskId());
        assertEquals("编程工具竞品分析", response.getTaskName());
        assertEquals(TaskStatus.CREATED, response.getStatus());
        assertEquals(0, response.getIterationCount());
        assertEquals(2, response.getMaxIterations());
    }

    @Test
    void createTask_generatesUniqueTaskId() {
        TaskCreateRequest request = buildRequest();

        TaskDetailResponse response1 = taskService.createTask(request);
        TaskDetailResponse response2 = taskService.createTask(request);

        assertNotNull(response1.getTaskId());
        assertNotNull(response2.getTaskId());
        assertNotEquals(response1.getTaskId(), response2.getTaskId());
    }

    // ---------- getTaskState ----------

    @Test
    void getTaskState_returnsState_whenFound() {
        CompetitiveAnalysisState state = buildCompletedState();
        when(entityAssembler.loadState("task-001")).thenReturn(state);

        CompetitiveAnalysisState result = taskService.getTaskState("task-001");

        assertSame(state, result);
    }

    @Test
    void getTaskState_throwsBizException_whenNotFound() {
        when(entityAssembler.loadState("missing")).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> taskService.getTaskState("missing"));

        assertEquals(404, ex.getCode());
        assertEquals("Task not found: missing", ex.getMessage());
    }

    // ---------- getTaskDetail ----------

    @Test
    void getTaskDetail_returnsResponse_whenEntityFound() {
        AnalysisTaskEntity entity = new AnalysisTaskEntity();
        entity.setTaskId("task-001");
        entity.setTaskName("AI 竞品分析");
        entity.setDomain("AI_CODING_TOOLS");
        entity.setTargetProductsJson("[\"Cursor\",\"Windsurf\"]");
        entity.setAnalysisGoal("生成报告");
        entity.setStatus("COMPLETED");
        entity.setIterationCount(1);
        entity.setMaxIterations(2);
        when(taskRepository.selectOne(any())).thenReturn(entity);

        TaskDetailResponse response = taskService.getTaskDetail("task-001");

        assertEquals("task-001", response.getTaskId());
        assertEquals("AI 竞品分析", response.getTaskName());
        assertEquals(TaskStatus.COMPLETED, response.getStatus());
        assertEquals(1, response.getIterationCount());
    }

    @Test
    void getTaskDetail_throwsBizException_whenEntityNotFound() {
        when(taskRepository.selectOne(any())).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> taskService.getTaskDetail("missing"));

        assertEquals(404, ex.getCode());
        assertEquals("Task not found: missing", ex.getMessage());
    }

    @Test
    void getTaskDetail_handlesNullStatusGracefully() {
        AnalysisTaskEntity entity = new AnalysisTaskEntity();
        entity.setTaskId("task-001");
        entity.setStatus(null);
        when(taskRepository.selectOne(any())).thenReturn(entity);

        TaskDetailResponse response = taskService.getTaskDetail("task-001");

        assertNull(response.getStatus());
    }

    @Test
    void getTaskDetail_handlesInvalidStatusGracefully() {
        AnalysisTaskEntity entity = new AnalysisTaskEntity();
        entity.setTaskId("task-001");
        entity.setStatus("UNKNOWN_STATUS");
        when(taskRepository.selectOne(any())).thenReturn(entity);

        TaskDetailResponse response = taskService.getTaskDetail("task-001");

        assertNull(response.getStatus());
    }

    // ---------- 辅助方法 ----------

    private TaskCreateRequest buildRequest() {
        TaskCreateRequest req = new TaskCreateRequest();
        req.setTaskName("编程工具竞品分析");
        req.setDomain("AI_CODING_TOOLS");
        req.setTargetProducts(java.util.List.of("Cursor", "Windsurf"));
        req.setAnalysisGoal("生成竞品分析报告");
        req.setOutputFormat("markdown");
        req.setLanguage("zh-CN");
        req.setMaxIterations(2);
        return req;
    }

    private CompetitiveAnalysisState buildCompletedState() {
        CompetitiveAnalysisState state = new CompetitiveAnalysisState();

        TaskInputDTO input = new TaskInputDTO();
        input.setTaskId("task_" + java.util.UUID.randomUUID().toString().replace("-", ""));
        input.setTaskName("AI 编程工具竞品分析");
        input.setDomain("AI_CODING_TOOLS");
        input.setTargetProducts(java.util.List.of("Cursor", "Windsurf"));
        input.setAnalysisGoal("生成竞品分析报告");
        input.setMaxIterations(2);
        state.setTaskInput(input);

        state.setIterationCount(1);
        state.setStatus(TaskStatus.COMPLETED);
        return state;
    }
}
