package org.example.ca_agent.agent;

import org.example.ca_agent.common.JsonUtils;
import org.example.ca_agent.config.AgentModeProperties;
import org.example.ca_agent.dto.agent.ReportDraftDTO;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.enums.TaskStatus;
import org.example.ca_agent.mock.MockCompetitiveAnalysisFixtures;
import org.example.ca_agent.prompt.WriterPrompt;
import org.example.ca_agent.service.AgentOutputValidator;
import org.example.ca_agent.service.StructuredLlmService;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WriterAgent implements AgentNode {

    private final AgentModeProperties modeProperties;
    private final StructuredLlmService structuredLlmService;
    private final WriterPrompt writerPrompt;
    private final AgentOutputValidator outputValidator;

    public WriterAgent() {
        this(new AgentModeProperties(), null, null, null);
    }

    @Autowired
    public WriterAgent(
            AgentModeProperties modeProperties,
            StructuredLlmService structuredLlmService,
            WriterPrompt writerPrompt,
            AgentOutputValidator outputValidator
    ) {
        this.modeProperties = modeProperties;
        this.structuredLlmService = structuredLlmService;
        this.writerPrompt = writerPrompt;
        this.outputValidator = outputValidator;
    }

    @Override
    public AgentType getAgentType() {
        return AgentType.WRITER_AGENT;
    }

    @Override
    public CompetitiveAnalysisState execute(CompetitiveAnalysisState state) {
        state.setStatus(TaskStatus.WRITING);
        if (modeProperties.isLlm()) {
            String taskId = state.getCompetitiveAnalysis().getTaskId();
            ReportDraftDTO reportDraft = structuredLlmService.generate(
                    WriterPrompt.SYSTEM_PROMPT,
                    writerPrompt.buildUserPrompt(
                            JsonUtils.toJson(state.getProductProfileSet()),
                            JsonUtils.toJson(state.getCompetitiveAnalysis()),
                            JsonUtils.toJson(state.getRawSourceSet().getEvidencePool()),
                            JsonUtils.toJson(state.getRepairInstructions())
                    ),
                    ReportDraftDTO.class
            );
            reportDraft.setTaskId(taskId);
            reportDraft.setSourceList(state.getRawSourceSet().getEvidencePool());
            outputValidator.validateWriter(
                    reportDraft,
                    taskId,
                    state.getRawSourceSet().getEvidencePool()
            );
            state.setReportDraft(reportDraft);
            return state;
        }

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
