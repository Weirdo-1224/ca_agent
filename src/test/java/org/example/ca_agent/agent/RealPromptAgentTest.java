package org.example.ca_agent.agent;

import org.example.ca_agent.common.BizException;
import org.example.ca_agent.common.JsonUtils;
import org.example.ca_agent.config.AgentModeProperties;
import org.example.ca_agent.dto.agent.CompetitiveAnalysisDTO;
import org.example.ca_agent.dto.agent.ProductProfileSetDTO;
import org.example.ca_agent.dto.agent.RawSourceSetDTO;
import org.example.ca_agent.dto.agent.RepairInstructionDTO;
import org.example.ca_agent.dto.agent.ReportDraftDTO;
import org.example.ca_agent.dto.agent.ReviewResultDTO;
import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.example.ca_agent.dto.agent.TaskPlanDTO;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.prompt.AnalyzerPrompt;
import org.example.ca_agent.prompt.ExtractorPrompt;
import org.example.ca_agent.prompt.PlannerPrompt;
import org.example.ca_agent.prompt.ReviewerPrompt;
import org.example.ca_agent.prompt.WriterPrompt;
import org.example.ca_agent.schema.Claim;
import org.example.ca_agent.schema.Evidence;
import org.example.ca_agent.service.AgentOutputValidator;
import org.example.ca_agent.service.LlmChatService;
import org.example.ca_agent.service.ModelChatGateway;
import org.example.ca_agent.service.StructuredLlmService;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void writerUsesRealEvidencePoolAsSourceListInLlmMode() {
        RecordingGateway gateway = new RecordingGateway(JsonUtils.toJson(validReport("model-task")));
        WriterAgent agent = new WriterAgent(
                llmMode(),
                new StructuredLlmService(new LlmChatService(gateway)),
                new WriterPrompt(),
                new AgentOutputValidator()
        );
        CompetitiveAnalysisState state = new CompetitiveAnalysisState();
        state.setProductProfileSet(validProfileSet("task-1"));
        state.setCompetitiveAnalysis(validAnalysis("task-1"));
        state.setRawSourceSet(rawSourceSet());
        RepairInstructionDTO repair = new RepairInstructionDTO();
        repair.setInstruction("add section");
        state.setRepairInstructions(List.of(repair));

        agent.execute(state);

        assertThat(state.getReportDraft().getTaskId()).isEqualTo("task-1");
        assertThat(state.getReportDraft().getSections()).hasSize(14);
        assertThat(state.getReportDraft().getSourceList()).isEqualTo(state.getRawSourceSet().getEvidencePool());
        assertThat(gateway.systemPrompt).contains(WriterPrompt.VERSION);
        assertThat(gateway.userPrompt).contains("add section", "\"evidenceId\":\"ev-1\"");
    }

    @Test
    void reviewerUsesFullStateAndIterationInLlmMode() {
        RecordingGateway gateway = new RecordingGateway(JsonUtils.toJson(reviewResult(true, "finish")));
        ReviewerAgent agent = reviewerAgent(gateway);
        CompetitiveAnalysisState state = reviewState();

        agent.execute(state);

        assertThat(state.getReviewResult().getTaskId()).isEqualTo("task-1");
        assertThat(state.getReviewResult().getPassed()).isTrue();
        assertThat(gateway.systemPrompt).contains(ReviewerPrompt.VERSION);
        assertThat(gateway.userPrompt).contains("IterationCount: 1", "MaxIterations: 3");
        assertThat(gateway.userPrompt).contains("\"reportDraft\"", "\"evidenceId\":\"ev-1\"");
    }

    @Test
    void reviewerAcceptsRepairDecisionInLlmMode() {
        ReviewResultDTO repairResult = reviewResult(false, "repair");
        ReviewResultDTO.ReviewIssue issue = new ReviewResultDTO.ReviewIssue();
        issue.setIssueId("issue-1");
        issue.setTargetAgent(AgentType.ANALYZER_AGENT);
        repairResult.setIssues(List.of(issue));
        repairResult.getNextAction().setTargetAgent(AgentType.ANALYZER_AGENT);
        RecordingGateway gateway = new RecordingGateway(JsonUtils.toJson(repairResult));

        CompetitiveAnalysisState state = reviewState();
        reviewerAgent(gateway).execute(state);

        assertThat(state.getReviewResult().getPassed()).isFalse();
        assertThat(state.getReviewResult().getNextAction().getAction()).isEqualTo("repair");
    }

    @Test
    void reviewerRejectsInvalidNextActionInLlmMode() {
        RecordingGateway gateway = new RecordingGateway(JsonUtils.toJson(reviewResult(false, "invalid")));

        assertThatThrownBy(() -> reviewerAgent(gateway).execute(reviewState()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Reviewer")
                .hasMessageContaining("nextAction");
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
        input.setMaxIterations(3);
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

    private ReportDraftDTO validReport(String taskId) {
        Evidence inventedSource = new Evidence();
        inventedSource.setEvidenceId("invented-source");

        ReportDraftDTO output = new ReportDraftDTO();
        output.setTaskId(taskId);
        output.setSections(IntStream.range(0, 14)
                .mapToObj(index -> {
                    ReportDraftDTO.ReportSection section = new ReportDraftDTO.ReportSection();
                    section.setTitle("Section " + index);
                    section.setEvidenceIds(List.of("ev-1"));
                    return section;
                })
                .toList());
        output.setSourceList(List.of(inventedSource));
        return output;
    }

    private ReviewerAgent reviewerAgent(RecordingGateway gateway) {
        return new ReviewerAgent(
                llmMode(),
                new StructuredLlmService(new LlmChatService(gateway)),
                new ReviewerPrompt(),
                new AgentOutputValidator()
        );
    }

    private CompetitiveAnalysisState reviewState() {
        CompetitiveAnalysisState state = new CompetitiveAnalysisState();
        state.setTaskInput(taskInput());
        state.setRawSourceSet(rawSourceSet());
        state.setProductProfileSet(validProfileSet("task-1"));
        state.setCompetitiveAnalysis(validAnalysis("task-1"));
        state.setReportDraft(validReport("task-1"));
        state.setRepairInstructions(List.of());
        state.setIterationCount(1);
        return state;
    }

    private ReviewResultDTO reviewResult(boolean passed, String action) {
        ReviewResultDTO.NextAction nextAction = new ReviewResultDTO.NextAction();
        nextAction.setAction(action);

        ReviewResultDTO output = new ReviewResultDTO();
        output.setTaskId("model-task");
        output.setPassed(passed);
        output.setScore(passed ? 90 : 70);
        output.setIssues(List.of());
        output.setNextAction(nextAction);
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
