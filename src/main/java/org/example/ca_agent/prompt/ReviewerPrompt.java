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
            int maxIterations,
            String language
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
                ReviewIssue fields: issueId, severity, type, description, targetAgent,
                targetProduct, targetDimension, repairInstruction.
                repairInstruction must be a single string (not an array). If multiple instructions exist, join them with semicolons inside one string.
                NextAction fields: action, targetAgent, reason.
                AgentType values: PLANNER_AGENT, COLLECTOR_AGENT, EXTRACTOR_AGENT,
                ANALYZER_AGENT, WRITER_AGENT, REVIEWER_AGENT.
                ReviewIssueType values: MISSING_EVIDENCE, EVIDENCE_NOT_LINKED, SCHEMA_MISSING_FIELD,
                COMPARISON_INCOMPLETE, VAGUE_FINDING, REPORT_MISSING_SECTION,
                CITATION_FORMAT_ERROR, HALLUCINATION_RISK, UNKNOWN_FIELD_TOO_MANY.
                Each issue targetAgent and nextAction targetAgent must use a valid AgentType enum value.
                nextAction.action must be finish, repair, or human_review.
                %s
                """.formatted(reviewStateJson, repairInstructionsJson, iterationCount, maxIterations, languageInstruction(language));
    }

    private static String languageInstruction(String language) {
        return "zh-CN".equals(language) || "zh".equals(language)
                ? "Respond in Chinese (中文). All text content, titles, and descriptions must be in Chinese."
                : "Respond in English. All text content, titles, and descriptions must be in English.";
    }
}
