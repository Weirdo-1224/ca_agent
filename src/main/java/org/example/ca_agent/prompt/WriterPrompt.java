package org.example.ca_agent.prompt;

import java.util.List;

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
            String repairInstructionsJson,
            String language
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
                Each section may use only these exact fields:
                sectionId, title, content, relatedClaimIds, evidenceIds.
                Do not generate sourceList content; set sourceList to an empty JSON array because the application supplies it.
                sections must contain all 14 standard titles:
                %s
                %s
                """.formatted(
                productProfileSetJson,
                competitiveAnalysisJson,
                evidencePoolJson,
                repairInstructionsJson,
                getStandardTitles(language),
                languageInstruction(language)
        );
    }

    public static List<String> getStandardTitles(String language) {
        if ("zh-CN".equals(language) || "zh".equals(language)) {
            return List.of(
                    "执行摘要", "分析背景", "竞品概览", "产品定位对比",
                    "核心功能矩阵", "Agent 编程能力对比", "代码库理解能力对比",
                    "模型与上下文能力对比", "定价模式对比", "用户评价与痛点",
                    "SWOT 分析", "产品机会点", "结论与建议", "信息来源"
            );
        }
        return List.of(
                "execution summary", "analysis background", "competitor overview", "positioning comparison",
                "core capability matrix", "agent capability comparison", "codebase understanding comparison",
                "model and context comparison", "pricing comparison", "user feedback and pain points",
                "SWOT analysis", "product opportunities", "conclusions and recommendations", "information sources"
        );
    }

    private static String languageInstruction(String language) {
        return "zh-CN".equals(language) || "zh".equals(language)
                ? "Respond in Chinese (中文). All text content, titles, and descriptions must be in Chinese."
                : "Respond in English. All text content, titles, and descriptions must be in English.";
    }
}
