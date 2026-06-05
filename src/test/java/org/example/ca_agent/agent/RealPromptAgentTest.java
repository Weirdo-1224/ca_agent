package org.example.ca_agent.agent;

import org.example.ca_agent.common.JsonUtils;
import org.example.ca_agent.config.AgentModeProperties;
import org.example.ca_agent.dto.agent.CompetitiveAnalysisDTO;
import org.example.ca_agent.dto.agent.ProductProfileSetDTO;
import org.example.ca_agent.dto.agent.RawSourceSetDTO;
import org.example.ca_agent.dto.agent.RepairInstructionDTO;
import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.example.ca_agent.dto.agent.TaskPlanDTO;
import org.example.ca_agent.prompt.AnalyzerPrompt;
import org.example.ca_agent.prompt.ExtractorPrompt;
import org.example.ca_agent.prompt.PlannerPrompt;
import org.example.ca_agent.schema.Claim;
import org.example.ca_agent.schema.Evidence;
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

    @Test
    void extractorUsesEvidenceAndRepairInstructionsInLlmMode() {
        RecordingGateway gateway = new RecordingGateway(JsonUtils.toJson(validProfileSet("model-task")));
        ExtractorAgent agent = new ExtractorAgent(
                llmMode(),
                new StructuredLlmService(new LlmChatService(gateway)),
                new ExtractorPrompt(),
                new AgentOutputValidator()
        );
        CompetitiveAnalysisState state = new CompetitiveAnalysisState();
        state.setRawSourceSet(rawSourceSet());
        RepairInstructionDTO repair = new RepairInstructionDTO();
        repair.setInstruction("relink evidence");
        state.setRepairInstructions(List.of(repair));

        agent.execute(state);

        assertThat(state.getProductProfileSet().getTaskId()).isEqualTo("task-1");
        assertThat(state.getProductProfileSet().getProducts()).hasSize(1);
        assertThat(gateway.systemPrompt).contains(ExtractorPrompt.VERSION);
        assertThat(gateway.userPrompt).contains("\"evidenceId\":\"ev-1\"", "relink evidence");
    }

    @Test
    void analyzerUsesProfilesEvidenceAndRepairInstructionsInLlmMode() {
        RecordingGateway gateway = new RecordingGateway(JsonUtils.toJson(validAnalysis("model-task")));
        AnalyzerAgent agent = new AnalyzerAgent(
                llmMode(),
                new StructuredLlmService(new LlmChatService(gateway)),
                new AnalyzerPrompt(),
                new AgentOutputValidator()
        );
        CompetitiveAnalysisState state = new CompetitiveAnalysisState();
        state.setProductProfileSet(validProfileSet("task-1"));
        state.setRawSourceSet(rawSourceSet());
        RepairInstructionDTO repair = new RepairInstructionDTO();
        repair.setInstruction("complete matrix");
        state.setRepairInstructions(List.of(repair));

        agent.execute(state);

        assertThat(state.getCompetitiveAnalysis().getTaskId()).isEqualTo("task-1");
        assertThat(state.getCompetitiveAnalysis().getComparisonMatrix()).hasSize(1);
        assertThat(gateway.systemPrompt).contains(AnalyzerPrompt.VERSION);
        assertThat(gateway.userPrompt).contains("\"productName\":\"Product A\"", "\"evidenceId\":\"ev-1\"");
        assertThat(gateway.userPrompt).contains("complete matrix");
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

    private RawSourceSetDTO rawSourceSet() {
        RawSourceSetDTO rawSourceSet = new RawSourceSetDTO();
        rawSourceSet.setTaskId("task-1");
        rawSourceSet.setEvidencePool(List.of(evidence()));
        rawSourceSet.setRawSources(List.of());
        rawSourceSet.setMissingSources(List.of());
        return rawSourceSet;
    }

    private ProductProfileSetDTO validProfileSet(String taskId) {
        Claim claim = new Claim();
        claim.setEvidenceIds(List.of("ev-1"));

        ProductProfileSetDTO.ProductProfile product = new ProductProfileSetDTO.ProductProfile();
        product.setProductName("Product A");
        product.setClaims(List.of(claim));

        ProductProfileSetDTO output = new ProductProfileSetDTO();
        output.setTaskId(taskId);
        output.setProducts(List.of(product));
        return output;
    }

    private Evidence evidence() {
        Evidence evidence = new Evidence();
        evidence.setEvidenceId("ev-1");
        evidence.setProductName("Product A");
        return evidence;
    }

    private CompetitiveAnalysisDTO validAnalysis(String taskId) {
        CompetitiveAnalysisDTO.ComparisonProductItem productItem =
                new CompetitiveAnalysisDTO.ComparisonProductItem();
        productItem.setProductName("Product A");
        productItem.setEvidenceIds(List.of("ev-1"));

        CompetitiveAnalysisDTO.ComparisonMatrixItem matrixItem =
                new CompetitiveAnalysisDTO.ComparisonMatrixItem();
        matrixItem.setDimension("core_capabilities");
        matrixItem.setItems(List.of(productItem));

        CompetitiveAnalysisDTO output = new CompetitiveAnalysisDTO();
        output.setTaskId(taskId);
        output.setComparisonMatrix(List.of(matrixItem));
        output.setKeyFindings(List.of());
        output.setProductOpportunities(List.of());
        output.setRisks(List.of());
        output.setSwotSummary(List.of());
        return output;
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
