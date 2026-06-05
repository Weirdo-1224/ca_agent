package org.example.ca_agent.service;

import org.example.ca_agent.common.BizException;
import org.example.ca_agent.dto.agent.CompetitiveAnalysisDTO;
import org.example.ca_agent.dto.agent.ProductProfileSetDTO;
import org.example.ca_agent.dto.agent.ReportDraftDTO;
import org.example.ca_agent.dto.agent.ReviewResultDTO;
import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.example.ca_agent.dto.agent.TaskPlanDTO;
import org.example.ca_agent.schema.Claim;
import org.example.ca_agent.schema.Evidence;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentOutputValidatorTest {

    private final AgentOutputValidator validator = new AgentOutputValidator();

    @Test
    void plannerRejectsCollectionTasksThatDoNotCoverEveryTargetProduct() {
        TaskInputDTO input = taskInput();
        TaskPlanDTO output = new TaskPlanDTO();
        output.setTaskId("task-1");
        output.setCollectionTasks(List.of(collectionTask("Product A")));

        assertRejected(() -> validator.validatePlanner(output, input), "Planner", "collectionTasks");
    }

    @Test
    void extractorRejectsEmptyProducts() {
        ProductProfileSetDTO output = new ProductProfileSetDTO();
        output.setTaskId("task-1");
        output.setProducts(List.of());

        assertRejected(
                () -> validator.validateExtractor(output, "task-1", evidencePool()),
                "Extractor",
                "products"
        );
    }

    @Test
    void extractorRejectsClaimWithoutEvidenceIds() {
        ProductProfileSetDTO output = profileSet(List.of());

        assertRejected(
                () -> validator.validateExtractor(output, "task-1", evidencePool()),
                "Extractor",
                "Claim evidenceIds"
        );
    }

    @Test
    void analyzerRejectsComparisonMatrixThatDoesNotCoverEveryProduct() {
        CompetitiveAnalysisDTO output = analysis(List.of(comparisonProduct("Product A", List.of("ev-1"))));

        assertRejected(
                () -> validator.validateAnalyzer(
                        output,
                        "task-1",
                        List.of("Product A", "Product B"),
                        evidencePool()
                ),
                "Analyzer",
                "comparisonMatrix"
        );
    }

    @Test
    void writerRejectsReportWithoutFourteenSections() {
        ReportDraftDTO output = report(13);

        assertRejected(
                () -> validator.validateWriter(output, "task-1", evidencePool()),
                "Writer",
                "14"
        );
    }

    @Test
    void reviewerRejectsIssueWithoutTargetAgent() {
        ReviewResultDTO output = review();
        ReviewResultDTO.ReviewIssue issue = new ReviewResultDTO.ReviewIssue();
        issue.setIssueId("issue-1");
        output.setIssues(List.of(issue));

        assertRejected(() -> validator.validateReviewer(output, "task-1"), "Reviewer", "targetAgent");
    }

    @Test
    void rejectsInventedEvidenceIds() {
        ProductProfileSetDTO output = profileSet(List.of("invented"));

        assertRejected(
                () -> validator.validateExtractor(output, "task-1", evidencePool()),
                "Extractor",
                "evidenceIds"
        );
    }

    @Test
    void rejectsTaskIdThatDoesNotMatchRealTask() {
        ReviewResultDTO output = review();
        output.setTaskId("wrong-task");

        assertRejected(() -> validator.validateReviewer(output, "task-1"), "Reviewer", "taskId");
    }

    private void assertRejected(Runnable validation, String agent, String rule) {
        assertThatThrownBy(validation::run)
                .isInstanceOf(BizException.class)
                .hasMessageContaining(agent)
                .hasMessageContaining(rule);
    }

    private TaskInputDTO taskInput() {
        TaskInputDTO input = new TaskInputDTO();
        input.setTaskId("task-1");
        input.setTargetProducts(List.of("Product A", "Product B"));
        return input;
    }

    private TaskPlanDTO.CollectionTask collectionTask(String productName) {
        TaskPlanDTO.CollectionTask task = new TaskPlanDTO.CollectionTask();
        task.setProductName(productName);
        return task;
    }

    private ProductProfileSetDTO profileSet(List<String> evidenceIds) {
        Claim claim = new Claim();
        claim.setEvidenceIds(evidenceIds);

        ProductProfileSetDTO.ProductProfile product = new ProductProfileSetDTO.ProductProfile();
        product.setProductName("Product A");
        product.setClaims(List.of(claim));

        ProductProfileSetDTO output = new ProductProfileSetDTO();
        output.setTaskId("task-1");
        output.setProducts(List.of(product));
        return output;
    }

    private CompetitiveAnalysisDTO analysis(List<CompetitiveAnalysisDTO.ComparisonProductItem> items) {
        CompetitiveAnalysisDTO.ComparisonMatrixItem matrix = new CompetitiveAnalysisDTO.ComparisonMatrixItem();
        matrix.setItems(items);

        CompetitiveAnalysisDTO output = new CompetitiveAnalysisDTO();
        output.setTaskId("task-1");
        output.setComparisonMatrix(List.of(matrix));
        return output;
    }

    private CompetitiveAnalysisDTO.ComparisonProductItem comparisonProduct(
            String productName,
            List<String> evidenceIds
    ) {
        CompetitiveAnalysisDTO.ComparisonProductItem item = new CompetitiveAnalysisDTO.ComparisonProductItem();
        item.setProductName(productName);
        item.setEvidenceIds(evidenceIds);
        return item;
    }

    private ReportDraftDTO report(int sectionCount) {
        ReportDraftDTO output = new ReportDraftDTO();
        output.setTaskId("task-1");
        output.setSections(IntStream.range(0, sectionCount)
                .mapToObj(index -> {
                    ReportDraftDTO.ReportSection section = new ReportDraftDTO.ReportSection();
                    section.setTitle("Section " + index);
                    section.setEvidenceIds(List.of("ev-1"));
                    return section;
                })
                .toList());
        return output;
    }

    private ReviewResultDTO review() {
        ReviewResultDTO.NextAction nextAction = new ReviewResultDTO.NextAction();
        nextAction.setAction("finish");

        ReviewResultDTO output = new ReviewResultDTO();
        output.setTaskId("task-1");
        output.setPassed(true);
        output.setScore(90);
        output.setIssues(List.of());
        output.setNextAction(nextAction);
        return output;
    }

    private List<Evidence> evidencePool() {
        Evidence evidence = new Evidence();
        evidence.setEvidenceId("ev-1");
        return List.of(evidence);
    }
}
