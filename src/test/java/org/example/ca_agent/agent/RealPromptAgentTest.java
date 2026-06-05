package org.example.ca_agent.agent;

import org.example.ca_agent.common.JsonUtils;
import org.example.ca_agent.config.AgentModeProperties;
import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.example.ca_agent.dto.agent.TaskPlanDTO;
import org.example.ca_agent.prompt.PlannerPrompt;
import org.example.ca_agent.service.AgentOutputValidator;
import org.example.ca_agent.service.LlmChatService;
import org.example.ca_agent.service.ModelChatGateway;
import org.example.ca_agent.service.StructuredLlmService;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RealPromptAgentTest {

    @Test
    void plannerUsesLlmPromptAndOverwritesModelTaskId() {
        RecordingGateway gateway = new RecordingGateway(JsonUtils.toJson(validPlan("model-task")));
        PlannerAgent agent = new PlannerAgent(
                llmMode(),
                new StructuredLlmService(new LlmChatService(gateway)),
                new PlannerPrompt(),
                new AgentOutputValidator()
        );
        CompetitiveAnalysisState state = new CompetitiveAnalysisState();
        state.setTaskInput(taskInput());

        agent.execute(state);

        assertThat(state.getTaskPlan().getTaskId()).isEqualTo("task-1");
        assertThat(state.getTaskPlan().getCollectionTasks())
                .extracting(TaskPlanDTO.CollectionTask::getProductName)
                .containsExactlyInAnyOrder("Product A", "Product B");
        assertThat(gateway.systemPrompt).contains(PlannerPrompt.VERSION);
        assertThat(gateway.userPrompt).contains("\"taskId\":\"task-1\"");
    }

    private AgentModeProperties llmMode() {
        AgentModeProperties properties = new AgentModeProperties();
        properties.setMode(AgentModeProperties.Mode.LLM);
        return properties;
    }

    private TaskInputDTO taskInput() {
        TaskInputDTO input = new TaskInputDTO();
        input.setTaskId("task-1");
        input.setTargetProducts(List.of("Product A", "Product B"));
        input.setAnalysisGoal("compare");
        return input;
    }

    private TaskPlanDTO validPlan(String taskId) {
        TaskPlanDTO plan = new TaskPlanDTO();
        plan.setTaskId(taskId);
        plan.setCollectionTasks(List.of(collectionTask("Product A"), collectionTask("Product B")));
        return plan;
    }

    private TaskPlanDTO.CollectionTask collectionTask(String productName) {
        TaskPlanDTO.CollectionTask task = new TaskPlanDTO.CollectionTask();
        task.setProductName(productName);
        return task;
    }

    private static class RecordingGateway implements ModelChatGateway {

        private final String response;
        private String systemPrompt;
        private String userPrompt;

        private RecordingGateway(String response) {
            this.response = response;
        }

        @Override
        public String call(String systemPrompt, String userPrompt) {
            this.systemPrompt = systemPrompt;
            this.userPrompt = userPrompt;
            return response;
        }
    }
}
