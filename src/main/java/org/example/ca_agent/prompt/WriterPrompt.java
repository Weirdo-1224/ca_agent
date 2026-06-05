package org.example.ca_agent.prompt;

import org.springframework.stereotype.Component;

@Component
public class WriterPrompt {

    public static final String VERSION = "writer_prompt_v1";
    public static final String SYSTEM_PROMPT = """
            Prompt version: writer_prompt_v1
            You are WriterAgent. Write a report only from supplied profiles, analysis, and evidence.
            Return one pure JSON object only. Markdown is allowed only inside section content strings.
            Every factual section must contain evidenceIds from the supplied evidence pool.
            Preserve the real taskId, use exact enum values, and never invent evidenceIds.
            """;

    public String buildUserPrompt(
            String productProfileSetJson,
            String competitiveAnalysisJson,
            String evidencePoolJson,
            String repairInstructionsJson
    ) {
        return """
                ProductProfileSet:
                %s

                CompetitiveAnalysis:
                %s

                EvidencePool:
                %s

                repairInstructions:
                %s

                Create a ReportDraftDTO JSON object with required fields:
                taskId, reportTitle, reportFormat, sections, sourceList.
                sections must contain all 14 standard titles:
                execution summary, analysis background, competitor overview, positioning comparison,
                core capability matrix, agent capability comparison, codebase understanding comparison,
                model and context comparison, pricing comparison, user feedback and pain points,
                SWOT analysis, product opportunities, conclusions and recommendations, information sources.
                """.formatted(
                productProfileSetJson,
                competitiveAnalysisJson,
                evidencePoolJson,
                repairInstructionsJson
        );
    }
}
