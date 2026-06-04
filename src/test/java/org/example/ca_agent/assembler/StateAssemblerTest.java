package org.example.ca_agent.assembler;

import org.example.ca_agent.dto.agent.*;
import org.example.ca_agent.entity.*;
import org.example.ca_agent.enums.*;
import org.example.ca_agent.repository.*;
import org.example.ca_agent.schema.Claim;
import org.example.ca_agent.schema.Evidence;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StateAssemblerTest {

    @Mock TaskRepository taskRepository;
    @Mock EvidenceRepository evidenceRepository;
    @Mock ClaimRepository claimRepository;
    @Mock ReportRepository reportRepository;
    @Mock ReviewIssueRepository reviewIssueRepository;
    @Mock RepairInstructionRepository repairInstructionRepository;
    @Mock AgentRunRepository agentRunRepository;

    @InjectMocks StateAssembler stateAssembler;

    private CompetitiveAnalysisState state;

    @BeforeEach
    void setUp() {
        state = buildMinimalState();
    }

    // ---------- 基础存取流程 ----------

    @Test
    void saveState_deletesOldDataBeforeInsertingNew() {
        state.getRawSourceSet().getEvidencePool().add(buildEvidence("ev1"));

        stateAssembler.saveState(state);

        InOrder inOrder = inOrder(evidenceRepository);
        inOrder.verify(evidenceRepository).delete(any());
        inOrder.verify(evidenceRepository).insert(any(EvidenceEntity.class));
    }

    @Test
    void saveState_insertsEvidenceWithCorrectTaskIdAndJsonFields() {
        Evidence ev = buildEvidence("ev1");
        ev.setProductName("Cursor");
        ev.setUsedFor(List.of("pricing", "features"));
        state.getRawSourceSet().getEvidencePool().add(ev);

        stateAssembler.saveState(state);

        ArgumentCaptor<EvidenceEntity> captor = ArgumentCaptor.forClass(EvidenceEntity.class);
        verify(evidenceRepository).insert(captor.capture());

        EvidenceEntity saved = captor.getValue();
        assertEquals("task-001", saved.getTaskId());
        assertEquals("Cursor", saved.getProductName());
        assertEquals("[\"pricing\",\"features\"]", saved.getUsedForJson());
        assertEquals("OFFICIAL_SITE", saved.getSourceType());
        assertEquals("HIGH", saved.getReliability());
    }

    @Test
    void saveState_savesTaskWithUpsert_whenTaskAlreadyExists() {
        AnalysisTaskEntity existing = new AnalysisTaskEntity();
        existing.setId(1L);
        existing.setTaskId("task-001");
        when(taskRepository.selectOne(any())).thenReturn(existing);

        state.setIterationCount(2);
        state.setStatus(TaskStatus.COMPLETED);

        stateAssembler.saveState(state);

        verify(taskRepository).updateById(argThat((AnalysisTaskEntity e) ->
            e.getIterationCount() == 2 &&
            "COMPLETED".equals(e.getStatus()) &&
            "task-001".equals(e.getTaskId())
        ));
        verify(taskRepository, never()).insert(any(AnalysisTaskEntity.class));
    }

    @Test
    void saveState_savesTaskWithInsert_whenTaskIsNew() {
        when(taskRepository.selectOne(any())).thenReturn(null);

        stateAssembler.saveState(state);

        verify(taskRepository).insert(argThat((AnalysisTaskEntity e) ->
            e.getId() == null &&
            "task-001".equals(e.getTaskId()) &&
            "AI_CODING_TOOLS".equals(e.getDomain())
        ));
        verify(taskRepository, never()).updateById(any(AnalysisTaskEntity.class));
    }

    // ---------- JSON 序列化验证 ----------

    @Test
    void saveState_serializesReportSectionsToJson() {
        ReportDraftDTO report = new ReportDraftDTO();
        ReportDraftDTO.ReportSection sec1 = new ReportDraftDTO.ReportSection();
        sec1.setSectionId("sec1");
        sec1.setTitle("执行摘要");
        sec1.setContent("内容...");
        ReportDraftDTO.ReportSection sec2 = new ReportDraftDTO.ReportSection();
        sec2.setSectionId("sec2");
        sec2.setTitle("市场概况");
        sec2.setContent("内容2...");
        report.setSections(List.of(sec1, sec2));
        state.setReportDraft(report);

        stateAssembler.saveState(state);

        ArgumentCaptor<ReportEntity> captor = ArgumentCaptor.forClass(ReportEntity.class);
        verify(reportRepository).insert(captor.capture());

        ReportEntity saved = captor.getValue();
        assertTrue(saved.getSectionsJson().contains("sec1"));
        assertTrue(saved.getSectionsJson().contains("执行摘要"));
        assertTrue(saved.getSectionsJson().contains("sec2"));
        assertEquals("report_task-001", saved.getReportId());
        assertEquals("task-001", saved.getTaskId());
    }

    @Test
    void saveState_serializesTargetProductsToJson() {
        when(taskRepository.selectOne(any())).thenReturn(null);

        stateAssembler.saveState(state);

        ArgumentCaptor<AnalysisTaskEntity> captor = ArgumentCaptor.forClass(AnalysisTaskEntity.class);
        verify(taskRepository).insert(captor.capture());

        assertEquals("[\"Cursor\",\"Windsurf\"]", captor.getValue().getTargetProductsJson());
    }

    // ---------- Claims 存储 ----------

    @Test
    void saveState_groupsClaimsByProductInSingleTable() {
        ProductProfileSetDTO.ProductProfile profile = new ProductProfileSetDTO.ProductProfile();
        profile.setProductName("Cursor");
        Claim c1 = buildClaim("claim1", "Cursor", "pricing");
        Claim c2 = buildClaim("claim2", "Cursor", "features");
        profile.setClaims(List.of(c1, c2));
        state.getProductProfileSet().setProducts(List.of(profile));

        stateAssembler.saveState(state);

        ArgumentCaptor<ClaimEntity> captor = ArgumentCaptor.forClass(ClaimEntity.class);
        verify(claimRepository, times(2)).insert(captor.capture());

        List<ClaimEntity> saved = captor.getAllValues();
        assertEquals("claim1", saved.get(0).getClaimId());
        assertEquals("Cursor", saved.get(0).getProductName());
        assertEquals("pricing", saved.get(0).getDimension());
        assertEquals("claim2", saved.get(1).getClaimId());
    }

    @Test
    void saveState_serializesEvidenceIdsInClaimToJson() {
        Claim claim = buildClaim("claim1", "Cursor", "pricing");
        claim.setEvidenceIds(List.of("ev1", "ev2"));
        ProductProfileSetDTO.ProductProfile profile = new ProductProfileSetDTO.ProductProfile();
        profile.setProductName("Cursor");
        profile.setClaims(List.of(claim));
        state.getProductProfileSet().setProducts(List.of(profile));

        stateAssembler.saveState(state);

        ArgumentCaptor<ClaimEntity> captor = ArgumentCaptor.forClass(ClaimEntity.class);
        verify(claimRepository).insert(captor.capture());

        assertEquals("[\"ev1\",\"ev2\"]", captor.getValue().getEvidenceIdsJson());
    }

    // ---------- Review Issues ----------

    @Test
    void saveState_persistsReviewIssuesWithEnumStringConversion() {
        ReviewResultDTO review = new ReviewResultDTO();
        ReviewResultDTO.ReviewIssue issue = new ReviewResultDTO.ReviewIssue();
        issue.setIssueId("issue1");
        issue.setSeverity("HIGH");
        issue.setType(ReviewIssueType.MISSING_EVIDENCE);
        issue.setDescription("缺少 pricing 数据");
        issue.setTargetAgent(AgentType.COLLECTOR_AGENT);
        issue.setTargetProduct("Cursor");
        issue.setTargetDimension("pricing");
        review.setIssues(List.of(issue));
        state.setReviewResult(review);

        stateAssembler.saveState(state);

        ArgumentCaptor<ReviewIssueEntity> captor = ArgumentCaptor.forClass(ReviewIssueEntity.class);
        verify(reviewIssueRepository).insert(captor.capture());

        ReviewIssueEntity saved = captor.getValue();
        assertEquals("issue1", saved.getIssueId());
        assertEquals("MISSING_EVIDENCE", saved.getType());
        assertEquals("COLLECTOR_AGENT", saved.getTargetAgent());
    }

    // ---------- Repair Instructions ----------

    @Test
    void saveState_persistsRepairInstructions() {
        RepairInstructionDTO inst = new RepairInstructionDTO();
        inst.setRepairId("repair1");
        inst.setFromAgent(AgentType.REVIEWER_AGENT);
        inst.setTargetAgent(AgentType.COLLECTOR_AGENT);
        inst.setRepairType(RepairType.SUPPLEMENT_EVIDENCE);
        inst.setIssueIds(List.of("issue1"));
        inst.setTargetProduct("Cursor");
        inst.setTargetDimension("pricing");
        inst.setInstruction("补充 Cursor 的 pricing 数据");
        inst.setPriority("HIGH");
        state.setRepairInstructions(List.of(inst));

        stateAssembler.saveState(state);

        ArgumentCaptor<RepairInstructionEntity> captor = ArgumentCaptor.forClass(RepairInstructionEntity.class);
        verify(repairInstructionRepository).insert(captor.capture());

        RepairInstructionEntity saved = captor.getValue();
        assertEquals("repair1", saved.getRepairId());
        assertEquals("REVIEWER_AGENT", saved.getFromAgent());
        assertEquals("SUPPLEMENT_EVIDENCE", saved.getRepairType());
        assertEquals("[\"issue1\"]", saved.getIssueIdsJson());
        assertNotNull(saved.getInstructionId()); // UUID generated
    }

    // ---------- Null/空集合容错 ----------

    @Test
    void saveState_skipsNullCollectionsWithoutNpe() {
        state.setRawSourceSet(null);
        state.setProductProfileSet(null);
        state.setReportDraft(null);
        state.setReviewResult(null);
        state.setRepairInstructions(null);

        assertDoesNotThrow(() -> stateAssembler.saveState(state));

        verify(evidenceRepository, never()).insert(any(EvidenceEntity.class));
        verify(claimRepository, never()).insert(any(ClaimEntity.class));
        verify(reportRepository, never()).insert(any(ReportEntity.class));
        verify(reviewIssueRepository, never()).insert(any(ReviewIssueEntity.class));
        verify(repairInstructionRepository, never()).insert(any(RepairInstructionEntity.class));
    }

    @Test
    void saveState_skipsEmptyCollections() {
        state.getRawSourceSet().setEvidencePool(Collections.emptyList());
        state.getProductProfileSet().setProducts(Collections.emptyList());
        state.setRepairInstructions(Collections.emptyList());

        stateAssembler.saveState(state);

        verify(evidenceRepository, never()).insert(any(EvidenceEntity.class));
        verify(claimRepository, never()).insert(any(ClaimEntity.class));
        verify(repairInstructionRepository, never()).insert(any(RepairInstructionEntity.class));
    }

    // ---------- 辅助方法 ----------

    private CompetitiveAnalysisState buildMinimalState() {
        CompetitiveAnalysisState s = new CompetitiveAnalysisState();

        TaskInputDTO input = new TaskInputDTO();
        input.setTaskId("task-001");
        input.setTaskName("AI 编程工具竞品分析");
        input.setDomain("AI_CODING_TOOLS");
        input.setTargetProducts(List.of("Cursor", "Windsurf"));
        input.setAnalysisGoal("生成竞品分析报告");
        input.setMaxIterations(2);
        s.setTaskInput(input);

        RawSourceSetDTO rawSourceSet = new RawSourceSetDTO();
        rawSourceSet.setTaskId("task-001");
        rawSourceSet.setEvidencePool(new java.util.ArrayList<>());
        s.setRawSourceSet(rawSourceSet);

        ProductProfileSetDTO profileSet = new ProductProfileSetDTO();
        profileSet.setTaskId("task-001");
        profileSet.setProducts(new java.util.ArrayList<>());
        s.setProductProfileSet(profileSet);

        s.setIterationCount(1);
        s.setStatus(TaskStatus.COMPLETED);
        return s;
    }

    private Evidence buildEvidence(String id) {
        Evidence ev = new Evidence();
        ev.setEvidenceId(id);
        ev.setProductName("Cursor");
        ev.setSourceType(SourceType.OFFICIAL_SITE);
        ev.setSourceTitle("Cursor 官网");
        ev.setUrl("https://cursor.com");
        ev.setContentSnippet("Cursor 是一款 AI 编程工具...");
        ev.setCollectedAt(LocalDateTime.now());
        ev.setReliability(ReliabilityLevel.HIGH);
        ev.setUsedFor(List.of("pricing"));
        return ev;
    }

    private Claim buildClaim(String id, String product, String dimension) {
        Claim c = new Claim();
        c.setClaimId(id);
        c.setProductName(product);
        c.setDimension(dimension);
        c.setStatement("声明内容");
        c.setConfidence(0.9);
        c.setEvidenceIds(Collections.emptyList());
        c.setRiskLevel("low");
        return c;
    }
}
