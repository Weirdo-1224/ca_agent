package org.example.ca_agent.controller;

import org.example.ca_agent.config.SyncAsyncTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(SyncAsyncTestConfig.class)
@Transactional
@Rollback
class AnalysisTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String DEFAULT_REQUEST_BODY = """
            {
              "taskName": "AI 编程工具竞品分析",
              "domain": "AI_CODING_TOOLS",
              "targetProducts": ["Cursor", "Windsurf", "GitHub Copilot", "通义灵码"],
              "analysisGoal": "生成面向产品团队的 AI 编程工具竞品分析报告",
              "outputFormat": "markdown",
              "language": "zh-CN",
              "maxIterations": 2
            }
            """;

    @Test
    void fullWorkflow_createsTaskAndExposesDetailReportEvidenceAndReview() throws Exception {
        // 1. 创建任务并获取 taskId（SyncAsyncTestConfig 使异步变同步，返回时工作流已完成）
        String response = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DEFAULT_REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskId = response.replaceAll("(?s).*\"taskId\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        // 2. 验证任务详情接口（异步同步化后工作流已在 create 调用中完成）
        mockMvc.perform(get("/api/tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(taskId))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 3. 验证报告接口（数据细节在 AssemblerTest 中验证）
        mockMvc.perform(get("/api/tasks/{taskId}/report", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(taskId))
                .andExpect(jsonPath("$.data.sections").isArray());

        // 4. 验证证据接口
        mockMvc.perform(get("/api/tasks/{taskId}/evidence", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].usedFor").isArray())
                .andExpect(jsonPath("$.data[0].usedFor[0]").value("official"));

        // 5. 验证评审接口
        mockMvc.perform(get("/api/tasks/{taskId}/review", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(taskId))
                .andExpect(jsonPath("$.data.passed").value(true));

        // 6. 验证 AgentRun Trace 接口（Task 05 新增）
        // 第一轮 6 个 Agent + 回退到 Collector 后 5 个 Agent = 11 条 Trace
        mockMvc.perform(get("/api/tasks/{taskId}/agent-runs", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(11));
    }

    @Test
    void createTask_allowsMultipleTasksWithSameMockBusinessIds() throws Exception {
        String firstTaskId = createTaskAndReturnId();

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DEFAULT_REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").isNotEmpty())
                .andExpect(jsonPath("$.data.taskId").value(not(firstTaskId)));
    }

    @Test
    void returnsFailResultWhenTaskDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}", "task_missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Task not found: task_missing"));

        mockMvc.perform(get("/api/tasks/{taskId}/report", "task_missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Task not found: task_missing"));

        mockMvc.perform(get("/api/tasks/{taskId}/evidence", "task_missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Task not found: task_missing"));

        mockMvc.perform(get("/api/tasks/{taskId}/agent-runs", "task_missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Task not found: task_missing"));
    }

    private String createTaskAndReturnId() throws Exception {
        String response = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DEFAULT_REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return response.replaceAll("(?s).*\"taskId\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }
}
