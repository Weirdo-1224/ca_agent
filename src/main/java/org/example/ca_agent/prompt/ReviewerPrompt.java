package org.example.ca_agent.prompt;

import org.springframework.stereotype.Component;

@Component
public class ReviewerPrompt {

    public static final String VERSION = "reviewer_prompt_v1";
    public static final String SYSTEM_PROMPT = """
            Prompt version: reviewer_prompt_v1
            You are ReviewerAgent. Review completeness, evidence traceability, comparison coverage, and hallucination risk.
            Return one pure JSON object only. Do not use Markdown or add fields outside ReviewResultDTO.
            Check all evidenceIds against the supplied evidence pool and never invent evidenceIds.
            Preserve the real taskId and use exact AgentType and ReviewIssueType enum values.
            """;

    public String buildUserPrompt(
            String reviewStateJson,
            String repairInstructionsJson,
            int iterationCount,
            int maxIterations
    ) {
        return """
                ReviewState:
                %s

                repairInstructions:
                %s

                IterationCount: %d
                MaxIterations: %d

                Create a ReviewResultDTO JSON object with required fields:
                taskId, passed, score, summary, issues, nextAction.
                Each issue targetAgent and nextAction targetAgent must use a valid AgentType enum value.
                nextAction.action must be finish, repair, or human_review.
                """.formatted(reviewStateJson, repairInstructionsJson, iterationCount, maxIterations);
    }
}
