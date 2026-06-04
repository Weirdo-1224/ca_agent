package org.example.ca_agent.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
@AutoConfigureMockMvc
class AnalysisTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsMockTaskAndExposesDetailReportEvidenceAndReview() throws Exception {
        String requestBody = """
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

        String response = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskId = response.replaceAll("(?s).*\"taskId\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(taskId))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.iterationCount").value(1));

        mockMvc.perform(get("/api/tasks/{taskId}/report", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(taskId))
                .andExpect(jsonPath("$.data.sections", hasSize(14)))
                .andExpect(jsonPath("$.data.reviewResult.passed").value(true));

        mockMvc.perform(get("/api/tasks/{taskId}/evidence", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(12)));

        mockMvc.perform(get("/api/tasks/{taskId}/review", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(taskId))
                .andExpect(jsonPath("$.data.passed").value(true));
    }

    @Test
    void returnsFailResultWhenTaskDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}", "task_missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Task not found: task_missing"));
    }
}
