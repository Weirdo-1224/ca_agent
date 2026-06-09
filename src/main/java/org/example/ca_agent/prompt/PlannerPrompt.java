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

    public String buildUserPrompt(String taskInputJson, String language) {
        return """
                TaskInput:
                %s

                Create a TaskPlanDTO JSON object with required fields:
                taskId, detectedDomain, templateId, confidence, products, analysisGoal,
                analysisDimensions, collectionTasks, workflow.

                CRITICAL RULES for collectionTasks:
                1. Each collectionTasks item must contain productName, queries, targetDimensions,
                   and preferredSourceTypes.
                2. queries MUST be an array of 4-6 diverse search queries per product, covering:
                   - Official product page / homepage (e.g. "ProductName official site")
                   - Pricing and plans (e.g. "ProductName pricing plans 2024")
                   - Technical documentation / API (e.g. "ProductName developer documentation API")
                   - Comparison / review articles (e.g. "ProductName vs CompetitorName comparison")
                   - Tech blog / changelog (e.g. "ProductName new features blog 2024")
                   - GitHub / community discussion (e.g. "ProductName GitHub stars features")
                3. Queries MUST be in English for better search coverage, even if the final report is in Chinese.
                4. Each query should be specific and targeted, not generic.
                5. targetDimensions must list the analysis dimensions relevant to this product.
                6. preferredSourceTypes: use only these exact SourceType enum values:
                   OFFICIAL_SITE, PRICING_PAGE, DOCUMENTATION, BLOG, CHANGELOG, GITHUB,
                   REVIEW_ARTICLE, COMMUNITY_DISCUSSION, NEWS, USER_COMMENT, UNKNOWN.
                   Include at least 3-4 different source types to ensure diversity.

                queries and targetDimensions must be arrays of plain strings only (never objects).
                preferredSourceTypes must be an array of plain SourceType enum string values only.
                workflow must be an array of plain strings only.
                collectionTasks must cover every target product.
                %s
                """.formatted(taskInputJson, languageInstruction(language));
    }

    private static String languageInstruction(String language) {
        return "zh-CN".equals(language) || "zh".equals(language)
                ? "Respond in Chinese (中文). All text content, titles, and descriptions must be in Chinese."
                : "Respond in English. All text content, titles, and descriptions must be in English.";
    }
}
