package org.example.ca_agent.prompt;

import org.springframework.stereotype.Component;

@Component
public class AnalyzerPrompt {

    public static final String VERSION = "analyzer_prompt_v1";
    public static final String SYSTEM_PROMPT = """
            Prompt version: analyzer_prompt_v1
            You are AnalyzerAgent. Compare supplied product profiles without introducing new facts.
            Return one pure JSON object only. Do not use Markdown or add fields outside CompetitiveAnalysisDTO.
            Every factual conclusion must contain evidenceIds from the supplied evidence pool.
            Preserve the real taskId, use exact enum values, and never invent evidenceIds.
            """;

    public String buildUserPrompt(
            String productProfileSetJson,
            String evidencePoolJson,
            String repairInstructionsJson,
            String language
    ) {
        return """
                ProductProfileSet:
                %s

                EvidencePool:
                %s

                repairInstructions:
                %s

                Create a CompetitiveAnalysisDTO JSON object with required fields:
                taskId, comparisonMatrix, keyFindings, productOpportunities, risks, swotSummary.
                Use only these exact nested structures:
                ComparisonMatrixItem: dimension, subDimension, items.
                ComparisonProductItem in items: productName, supportLevel, summary, evidenceIds.
                KeyFinding: findingId, title, description, relatedProducts, evidenceIds, confidence.
                ProductOpportunity: opportunityId, title, description, targetUsers, requiredCapabilities, priority, evidenceIds.
                Risk: riskId, title, description, severity, evidenceIds.
                SwotSummary: productName, strengths, weaknesses, opportunities, threats.
                Each SwotItem: point, explanation, evidenceIds.
                comparisonMatrix and swotSummary must be JSON arrays. confidence must be a JSON number.
                comparisonMatrix must cover every supplied product and every supported conclusion must use evidenceIds.
                %s
                """.formatted(productProfileSetJson, evidencePoolJson, repairInstructionsJson, languageInstruction(language));
    }

    private static String languageInstruction(String language) {
        return "zh-CN".equals(language) || "zh".equals(language)
                ? "Respond in Chinese (中文). All text content, titles, and descriptions must be in Chinese."
                : "Respond in English. All text content, titles, and descriptions must be in English.";
    }
}
