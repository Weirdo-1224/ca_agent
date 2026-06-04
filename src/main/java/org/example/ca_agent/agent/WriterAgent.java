package org.example.ca_agent.agent;

import lombok.RequiredArgsConstructor;
import org.example.ca_agent.dto.agent.ReportDraftDTO;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.enums.TaskStatus;
import org.example.ca_agent.mock.MockCompetitiveAnalysisFixtures;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class WriterAgent implements AgentNode {

    @Override
    public AgentType getAgentType() {
        return AgentType.WRITER_AGENT;
    }

    @Override
    public CompetitiveAnalysisState execute(CompetitiveAnalysisState state) {
        state.setStatus(TaskStatus.WRITING);

        ReportDraftDTO reportDraft = new ReportDraftDTO();
        reportDraft.setTaskId(state.getCompetitiveAnalysis().getTaskId());
        reportDraft.setReportTitle("AI 编程工具竞品分析报告");
        reportDraft.setReportFormat("markdown");
        reportDraft.setSections(buildSections(state));
        reportDraft.setSourceList(state.getRawSourceSet().getEvidencePool());
        state.setReportDraft(reportDraft);
        return state;
    }

    private List<ReportDraftDTO.ReportSection> buildSections(CompetitiveAnalysisState state) {
        AtomicInteger counter = new AtomicInteger(1);
        List<String> evidenceIds = state.getRawSourceSet().getEvidencePool().stream()
                .map(org.example.ca_agent.schema.Evidence::getEvidenceId)
                .toList();
        List<String> claimIds = state.getProductProfileSet().getProducts().stream()
                .flatMap(profile -> profile.getClaims().stream())
                .map(org.example.ca_agent.schema.Claim::getClaimId)
                .toList();

        return MockCompetitiveAnalysisFixtures.STANDARD_REPORT_SECTIONS.stream()
                .map(title -> section(counter.getAndIncrement(), title, evidenceIds, claimIds))
                .toList();
    }

    private ReportDraftDTO.ReportSection section(int index, String title, List<String> evidenceIds, List<String> claimIds) {
        ReportDraftDTO.ReportSection section = new ReportDraftDTO.ReportSection();
        section.setSectionId("section_" + String.format("%02d", index));
        section.setTitle(title);
        section.setContent("## " + title + "\n\n本章节基于 Mock 结构化结果生成。");
        section.setEvidenceIds(evidenceIds);
        section.setRelatedClaimIds(claimIds);
        return section;
    }
}
