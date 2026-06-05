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
            String repairInstructionsJson
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
                comparisonMatrix must cover every supplied product and every supported conclusion must use evidenceIds.
                """.formatted(productProfileSetJson, evidencePoolJson, repairInstructionsJson);
    }
}
