package org.example.ca_agent.workflow;

import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.example.ca_agent.enums.TaskStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("live")
@ActiveProfiles("local")
@SpringBootTest
class EndToEndLiveTest {

    @Autowired
    private CompetitiveAnalysisGraph graph;

    @Test
    void fullWorkflow_withRealSearchAndLlm_completesSuccessfully() {
        TaskInputDTO input = new TaskInputDTO();
        input.setTaskId("e2e_live_" + System.currentTimeMillis());
        input.setTaskName("AI Coding Tools E2E Live Test");
        input.setDomain("AI_CODING_TOOLS");
        input.setTargetProducts(List.of("Cursor", "GitHub Copilot"));
        input.setAnalysisGoal("Compare positioning and capabilities of AI coding assistants");
        input.setOutputFormat("markdown");
        input.setLanguage("zh-CN");
        input.setMaxIterations(2);

        CompetitiveAnalysisState state = graph.run(input);

        assertThat(state.getStatus()).isIn(TaskStatus.COMPLETED, TaskStatus.WAITING_HUMAN_REVIEW);
        assertThat(state.getTaskPlan()).isNotNull();
        assertThat(state.getTaskPlan().getCollectionTasks()).isNotEmpty();
        assertThat(state.getRawSourceSet()).isNotNull();
        assertThat(state.getRawSourceSet().getEvidencePool()).isNotEmpty();
        assertThat(state.getProductProfileSet()).isNotNull();
        assertThat(state.getCompetitiveAnalysis()).isNotNull();
        assertThat(state.getReportDraft()).isNotNull();
        assertThat(state.getReportDraft().getSections()).isNotEmpty();
        assertThat(state.getReviewResult()).isNotNull();
    }
}
