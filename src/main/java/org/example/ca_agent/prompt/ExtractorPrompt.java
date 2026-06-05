package org.example.ca_agent.prompt;

import org.springframework.stereotype.Component;

@Component
public class ExtractorPrompt {

    public static final String VERSION = "extractor_prompt_v1";
    public static final String SYSTEM_PROMPT = """
            Prompt version: extractor_prompt_v1
            You are ExtractorAgent. Extract product profiles only from the supplied sources and evidence.
            Return one pure JSON object only. Do not use Markdown or add fields outside ProductProfileSetDTO.
            Every factual claim must contain evidenceIds from the supplied evidence pool.
            Preserve the real taskId, use exact enum values, and never invent evidenceIds.
            """;

    public String buildUserPrompt(String rawSourceSetJson, String repairInstructionsJson) {
        return """
                RawSourceSet:
                %s

                repairInstructions:
                %s

                Create a ProductProfileSetDTO JSON object with required fields taskId and products.
                Each product must include productName and claims. Every claim must include evidenceIds.
                Use unknown for facts that cannot be supported by the supplied evidence.
                """.formatted(rawSourceSetJson, repairInstructionsJson);
    }
}
