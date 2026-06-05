package org.example.ca_agent.prompt;

import org.springframework.stereotype.Component;

@Component
public class PlannerPrompt {

    public static final String VERSION = "planner_prompt_v1";
    public static final String SYSTEM_PROMPT = """
            Prompt version: planner_prompt_v1
            You are PlannerAgent. Plan the competitive analysis workflow without collecting facts or writing a report.
            Return one pure JSON object only. Do not use Markdown or add fields outside the required DTO.
            Preserve the real taskId. Use exact enum values and never invent evidenceIds.
            """;

    public String buildUserPrompt(String taskInputJson) {
        return """
                TaskInput:
                %s

                Create a TaskPlanDTO JSON object with required fields:
                taskId, detectedDomain, templateId, confidence, products, analysisGoal,
                analysisDimensions, collectionTasks, workflow.
                Each collectionTasks item must contain productName, queries, targetDimensions,
                and preferredSourceTypes using only these exact SourceType enum values:
                OFFICIAL_SITE, PRICING_PAGE, DOCUMENTATION, BLOG, CHANGELOG, GITHUB,
                REVIEW_ARTICLE, COMMUNITY_DISCUSSION, NEWS, USER_COMMENT, UNKNOWN.
                collectionTasks must cover every target product.
                """.formatted(taskInputJson);
    }
}
