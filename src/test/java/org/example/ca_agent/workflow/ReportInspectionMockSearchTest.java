package org.example.ca_agent.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Tag("live")
@ActiveProfiles("local")
@TestPropertySource(properties = "ca-agent.search.enabled=false")
@SpringBootTest
class ReportInspectionMockSearchTest {

    @Autowired
    private CompetitiveAnalysisGraph graph;

    @Test
    void generateAndSaveReportWithMockSearch() throws Exception {
        TaskInputDTO input = new TaskInputDTO();
        input.setTaskId("report_mock_" + System.currentTimeMillis());
        input.setTaskName("AI Coding Tools Report (Mock Search)");
        input.setDomain("AI_CODING_TOOLS");
        input.setTargetProducts(List.of("Cursor", "GitHub Copilot"));
        input.setAnalysisGoal("Compare positioning and capabilities of AI coding assistants");
        input.setOutputFormat("markdown");
        input.setLanguage("zh-CN");
        input.setMaxIterations(1);

        CompetitiveAnalysisState state = graph.run(input);

        Path outDir = Path.of("target", "reports");
        Files.createDirectories(outDir);

        Path jsonPath = outDir.resolve("report-draft-mock.json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.writerWithDefaultPrettyPrinter().writeValue(jsonPath.toFile(), state.getReportDraft());

        Path mdPath = outDir.resolve("report-mock.md");
        StringBuilder md = new StringBuilder();
        md.append("# ").append(state.getReportDraft().getReportTitle()).append("\n\n");
        state.getReportDraft().getSections().forEach(s -> {
            md.append("## ").append(s.getTitle()).append("\n\n");
            md.append(s.getContent()).append("\n\n");
        });
        md.append("---\n\n");
        md.append("**Review Passed:** ").append(state.getReviewResult().getPassed()).append("\n");
        md.append("**Review Score:** ").append(state.getReviewResult().getScore()).append("\n");
        md.append("**Status:** ").append(state.getStatus()).append("\n");
        Files.writeString(mdPath, md.toString());

        System.out.println("\n========== GENERATED REPORT (MOCK SEARCH) ==========");
        System.out.println(md);
        System.out.println("========== END REPORT ==========");
        System.out.println("JSON saved to: " + jsonPath.toAbsolutePath());
        System.out.println("Markdown saved to: " + mdPath.toAbsolutePath());
    }
}
