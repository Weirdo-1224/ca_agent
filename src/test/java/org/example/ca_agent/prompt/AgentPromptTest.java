package org.example.ca_agent.prompt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentPromptTest {

    @Test
    void plannerPromptContainsVersionJsonConstraintInputAndRequiredFields() {
        String userPrompt = new PlannerPrompt().buildUserPrompt("{\"taskId\":\"task-1\"}");

        assertSystemPrompt(PlannerPrompt.SYSTEM_PROMPT, PlannerPrompt.VERSION);
        assertThat(userPrompt)
                .contains("TaskInput", "\"taskId\":\"task-1\"")
                .contains("collectionTasks", "analysisDimensions", "workflow")
                .contains(
                        "OFFICIAL_SITE",
                        "PRICING_PAGE",
                        "DOCUMENTATION",
                        "BLOG",
                        "CHANGELOG",
                        "GITHUB",
                        "REVIEW_ARTICLE",
                        "COMMUNITY_DISCUSSION",
                        "NEWS",
                        "USER_COMMENT",
                        "UNKNOWN"
                );
    }

    @Test
    void extractorPromptContainsRepairInstructionsAndRequiredFields() {
        String userPrompt = new ExtractorPrompt().buildUserPrompt(
                "{\"taskId\":\"task-1\"}",
                "[{\"instruction\":\"relink evidence\"}]"
        );

        assertSystemPrompt(ExtractorPrompt.SYSTEM_PROMPT, ExtractorPrompt.VERSION);
        assertThat(userPrompt)
                .contains("RawSourceSet", "\"taskId\":\"task-1\"")
                .contains("repairInstructions", "relink evidence")
                .contains("products", "claims", "evidenceIds")
                .contains("claimId", "productName", "dimension", "statement", "confidence", "riskLevel")
                .contains("Do not use claimText")
                .contains("confidence must be a JSON number", "riskLevel must be low, medium, or high");
    }

    @Test
    void analyzerPromptContainsRepairInstructionsAndRequiredFields() {
        String userPrompt = new AnalyzerPrompt().buildUserPrompt(
                "{\"products\":[]}",
                "[{\"evidenceId\":\"ev-1\"}]",
                "[{\"instruction\":\"complete matrix\"}]"
        );

        assertSystemPrompt(AnalyzerPrompt.SYSTEM_PROMPT, AnalyzerPrompt.VERSION);
        assertThat(userPrompt)
                .contains("ProductProfileSet", "EvidencePool", "repairInstructions")
                .contains("comparisonMatrix", "keyFindings", "evidenceIds")
                .contains(
                        "ComparisonMatrixItem",
                        "ComparisonProductItem",
                        "ProductOpportunity",
                        "SwotSummary",
                        "SwotItem"
                );
    }

    @Test
    void writerPromptContainsRepairInstructionsAndRequiredFields() {
        String userPrompt = new WriterPrompt().buildUserPrompt(
                "{\"products\":[]}",
                "{\"comparisonMatrix\":[]}",
                "[{\"evidenceId\":\"ev-1\"}]",
                "[{\"instruction\":\"add section\"}]"
        );

        assertSystemPrompt(WriterPrompt.SYSTEM_PROMPT, WriterPrompt.VERSION);
        assertThat(userPrompt)
                .contains("ProductProfileSet", "CompetitiveAnalysis", "EvidencePool", "repairInstructions")
                .contains("reportTitle", "sections", "sourceList")
                .contains("sectionId", "relatedClaimIds", "Do not generate sourceList");
    }

    @Test
    void reviewerPromptContainsIterationRepairContextAndRequiredFields() {
        String userPrompt = new ReviewerPrompt().buildUserPrompt(
                "{\"taskInput\":{\"taskId\":\"task-1\"}}",
                "[{\"instruction\":\"review again\"}]",
                1,
                3
        );

        assertSystemPrompt(ReviewerPrompt.SYSTEM_PROMPT, ReviewerPrompt.VERSION);
        assertThat(userPrompt)
                .contains("ReviewState", "repairInstructions", "IterationCount: 1", "MaxIterations: 3")
                .contains("passed", "score", "issues", "nextAction")
                .contains("ReviewIssue fields", "NextAction fields", "PLANNER_AGENT", "HALLUCINATION_RISK");
    }

    private static void assertSystemPrompt(String systemPrompt, String version) {
        assertThat(systemPrompt)
                .contains(version)
                .containsIgnoringCase("pure JSON")
                .contains("evidenceIds")
                .doesNotContain("```");
    }
}
