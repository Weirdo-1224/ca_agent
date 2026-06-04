package org.example.ca_agent.assembler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.ca_agent.dto.agent.*;
import org.example.ca_agent.entity.*;
import org.example.ca_agent.enums.*;
import org.example.ca_agent.repository.*;
import org.example.ca_agent.schema.Claim;
import org.example.ca_agent.schema.Evidence;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntityAssemblerTest {

    @Mock TaskRepository taskRepository;
    @Mock EvidenceRepository evidenceRepository;
    @Mock ClaimRepository claimRepository;
    @Mock ReportRepository reportRepository;
    @Mock ReviewIssueRepository reviewIssueRepository;
    @Mock RepairInstructionRepository repairInstructionRepository;

    @InjectMocks EntityAssembler entityAssembler;

    // ---------- Task 加载 ----------

    @Test
    void loadState_returnsNull_whenTaskNotFound() {
        when(taskRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        CompetitiveAnalysisState state = entityAssembler.loadState("missing");

        assertNull(state);
        verify(evidenceRepository, never()).selectList(any());
    }

    @Test
    void loadState_reconstructsTaskInputFromEntity() {
        AnalysisTaskEntity task = buildTaskEntity("task-001");
        task.setTaskName("AI 竞品分析");
        task.setDomain("AI_CODING_TOOLS");
        task.setTargetProductsJson("[\"Cursor\",\"Windsurf\"]");
        task.setAnalysisGoal("生成报告");
        task.setIterationCount(2);
        task.setStatus("COMPLETED");
        when(taskRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);
        stubEmptyDependencies();

        CompetitiveAnalysisState state = entityAssembler.loadState("task-001");

        assertNotNull(state);
        assertEquals("task-001", state.getTaskInput().getTaskId());
        assertEquals("AI 竞品分析", state.getTaskInput().getTaskName());
        assertEquals(List.of("Cursor", "Windsurf"), state.getTaskInput().getTargetProducts());
        assertEquals(2, state.getIterationCount());
        assertEquals(TaskStatus.COMPLETED, state.getStatus());
    }

    // ---------- Evidence Pool 重建 ----------

    @Test
    void loadState_reconstructsEvidencePoolFromEntities() {
        when(taskRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildTaskEntity("task-001"));

        EvidenceEntity ev = new EvidenceEntity();
        ev.setEvidenceId("ev1");
        ev.setProductName("Cursor");
        ev.setSourceType("OFFICIAL_SITE");
        ev.setSourceTitle("Cursor 官网");
        ev.setUrl("https://cursor.com");
        ev.setContentSnippet("Snippet...");
        ev.setCollectedAt(LocalDateTime.of(2024, 1, 1, 0, 0));
        ev.setReliability("HIGH");
        ev.setUsedForJson("[\"pricing\",\"features\"]");
        when(evidenceRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(ev));
        stubEmptyForOthers();

        CompetitiveAnalysisState state = entityAssembler.loadState("task-001");

        assertEquals(1, state.getRawSourceSet().getEvidencePool().size());
        Evidence restored = state.getRawSourceSet().getEvidencePool().get(0);
        assertEquals("Cursor", restored.getProductName());
        assertEquals(SourceType.OFFICIAL_SITE, restored.getSourceType());
        assertEquals(ReliabilityLevel.HIGH, restored.getReliability());
        assertEquals(List.of("pricing", "features"), restored.getUsedFor());
    }

    @Test
    void loadState_returnsEmptyEvidencePool_whenNoEvidence() {
        when(taskRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildTaskEntity("task-001"));
        when(evidenceRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        stubEmptyForOthers();

        CompetitiveAnalysisState state = entityAssembler.loadState("task-001");

        assertNotNull(state.getRawSourceSet());
        assertTrue(state.getRawSourceSet().getEvidencePool().isEmpty());
    }

    // ---------- Product Profiles / Claims 分组 ----------

    @Test
    void loadState_groupsClaimsByProductNameIntoProfiles() {
        when(taskRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildTaskEntity("task-001"));
        when(evidenceRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        ClaimEntity c1 = buildClaimEntity("c1", "Cursor", "pricing");
        ClaimEntity c2 = buildClaimEntity("c2", "Cursor", "features");
        ClaimEntity c3 = buildClaimEntity("c3", "Windsurf", "pricing");
        when(claimRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(c1, c2, c3));
        stubEmptyForReportAndReview();

        CompetitiveAnalysisState state = entityAssembler.loadState("task-001");

        assertEquals(2, state.getProductProfileSet().getProducts().size());

        ProductProfileSetDTO.ProductProfile cursorProfile = findProduct(state, "Cursor");
        assertNotNull(cursorProfile);
        assertEquals(2, cursorProfile.getClaims().size());

        ProductProfileSetDTO.ProductProfile windsurfProfile = findProduct(state, "Windsurf");
        assertNotNull(windsurfProfile);
        assertEquals(1, windsurfProfile.getClaims().size());
    }

    @Test
    void loadState_returnsEmptyProducts_whenNoClaims() {
        when(taskRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildTaskEntity("task-001"));
        when(evidenceRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(claimRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        stubEmptyForReportAndReview();

        CompetitiveAnalysisState state = entityAssembler.loadState("task-001");

        assertTrue(state.getProductProfileSet().getProducts().isEmpty());
    }

    @Test
    void loadState_reconstructsClaimsWithEvidenceIds() {
        when(taskRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildTaskEntity("task-001"));
        when(evidenceRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        ClaimEntity c1 = buildClaimEntity("c1", "Cursor", "pricing");
        c1.setEvidenceIdsJson("[\"ev1\",\"ev2\"]");
        when(claimRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(c1));
        stubEmptyForReportAndReview();

        CompetitiveAnalysisState state = entityAssembler.loadState("task-001");

        Claim restored = state.getProductProfileSet().getProducts().get(0).getClaims().get(0);
        assertEquals(List.of("ev1", "ev2"), restored.getEvidenceIds());
    }

    // ---------- Report 重建 ----------

    @Test
    void loadState_reconstructsReportWithSections() {
        when(taskRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildTaskEntity("task-001"));
        when(evidenceRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(claimRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        ReportEntity report = new ReportEntity();
        report.setReportId("r1");
        report.setTaskId("task-001");
        report.setReportTitle("AI 编程工具竞品分析报告");
        report.setReportFormat("markdown");
        report.setSectionsJson("""
            [{"sectionId":"s1","title":"执行摘要","content":"内容1","relatedClaimIds":[],"evidenceIds":[]},
             {"sectionId":"s2","title":"市场概况","content":"内容2","relatedClaimIds":[],"evidenceIds":[]}]
            """);
        report.setSourceListJson("[]");
        when(reportRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(report);
        stubEmptyForReview();

        CompetitiveAnalysisState state = entityAssembler.loadState("task-001");

        assertNotNull(state.getReportDraft());
        assertEquals("AI 编程工具竞品分析报告", state.getReportDraft().getReportTitle());
        assertEquals(2, state.getReportDraft().getSections().size());
        assertEquals("执行摘要", state.getReportDraft().getSections().get(0).getTitle());
    }

    @Test
    void loadState_handlesMissingReport() {
        when(taskRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildTaskEntity("task-001"));
        when(evidenceRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(claimRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(reportRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        stubEmptyForReview();

        CompetitiveAnalysisState state = entityAssembler.loadState("task-001");

        assertNull(state.getReportDraft());
    }

    // ---------- Review Result / passed 推断 ----------

    @Test
    void loadState_infersPassedTrue_whenNoIssues() {
        when(taskRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildTaskEntity("task-001"));
        stubEmptyDependenciesExceptReview();
        when(reviewIssueRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        CompetitiveAnalysisState state = entityAssembler.loadState("task-001");

        assertNotNull(state.getReviewResult());
        assertTrue(state.getReviewResult().getPassed());
        assertTrue(state.getReviewResult().getIssues().isEmpty());
    }

    @Test
    void loadState_infersPassedFalse_whenIssuesExist() {
        when(taskRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildTaskEntity("task-001"));
        stubEmptyDependenciesExceptReview();

        ReviewIssueEntity issue = new ReviewIssueEntity();
        issue.setIssueId("issue1");
        issue.setSeverity("HIGH");
        issue.setType("MISSING_EVIDENCE");
        issue.setDescription("缺少数据");
        issue.setTargetAgent("COLLECTOR_AGENT");
        when(reviewIssueRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(issue));

        CompetitiveAnalysisState state = entityAssembler.loadState("task-001");

        assertFalse(state.getReviewResult().getPassed());
        assertEquals(1, state.getReviewResult().getIssues().size());
        assertEquals(ReviewIssueType.MISSING_EVIDENCE, state.getReviewResult().getIssues().get(0).getType());
        assertEquals(AgentType.COLLECTOR_AGENT, state.getReviewResult().getIssues().get(0).getTargetAgent());
    }

    // ---------- Repair Instructions ----------

    @Test
    void loadState_reconstructsRepairInstructions() {
        when(taskRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildTaskEntity("task-001"));
        stubEmptyDependenciesExceptRepair();

        RepairInstructionEntity inst = new RepairInstructionEntity();
        inst.setInstructionId("inst1");
        inst.setRepairId("repair1");
        inst.setFromAgent("REVIEWER_AGENT");
        inst.setTargetAgent("COLLECTOR_AGENT");
        inst.setIssueIdsJson("[\"issue1\"]");
        inst.setRepairType("SUPPLEMENT_EVIDENCE");
        inst.setTargetProduct("Cursor");
        inst.setTargetDimension("pricing");
        inst.setInstruction("补充数据");
        inst.setPriority("HIGH");
        when(repairInstructionRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(inst));

        CompetitiveAnalysisState state = entityAssembler.loadState("task-001");

        assertEquals(1, state.getRepairInstructions().size());
        RepairInstructionDTO restored = state.getRepairInstructions().get(0);
        assertEquals("repair1", restored.getRepairId());
        assertEquals(AgentType.REVIEWER_AGENT, restored.getFromAgent());
        assertEquals(RepairType.SUPPLEMENT_EVIDENCE, restored.getRepairType());
        assertEquals(List.of("issue1"), restored.getIssueIds());
    }

    @Test
    void loadState_returnsEmptyRepairInstructions_whenNoneExist() {
        when(taskRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildTaskEntity("task-001"));
        stubEmptyDependenciesExceptRepair();
        when(repairInstructionRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        CompetitiveAnalysisState state = entityAssembler.loadState("task-001");

        assertNotNull(state.getRepairInstructions());
        assertTrue(state.getRepairInstructions().isEmpty());
    }

    // ---------- Null JSON 容错 ----------

    @Test
    void loadState_handlesNullJsonFieldsGracefully() {
        AnalysisTaskEntity task = buildTaskEntity("task-001");
        task.setTargetProductsJson(null);
        when(taskRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);
        stubEmptyDependencies();

        CompetitiveAnalysisState state = entityAssembler.loadState("task-001");

        assertNotNull(state.getTaskInput().getTargetProducts());
        assertTrue(state.getTaskInput().getTargetProducts().isEmpty());
    }

    @Test
    void loadState_handlesBlankJsonFieldsGracefully() {
        AnalysisTaskEntity task = buildTaskEntity("task-001");
        task.setTargetProductsJson("   ");
        when(taskRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);
        stubEmptyDependencies();

        CompetitiveAnalysisState state = entityAssembler.loadState("task-001");

        assertNotNull(state.getTaskInput().getTargetProducts());
        assertTrue(state.getTaskInput().getTargetProducts().isEmpty());
    }

    // ---------- 辅助方法 ----------

    private AnalysisTaskEntity buildTaskEntity(String taskId) {
        AnalysisTaskEntity e = new AnalysisTaskEntity();
        e.setId(1L);
        e.setTaskId(taskId);
        e.setTaskName("测试任务");
        e.setDomain("AI_CODING_TOOLS");
        e.setTargetProductsJson("[\"Cursor\"]");
        e.setAnalysisGoal("测试");
        e.setStatus("COMPLETED");
        e.setIterationCount(1);
        e.setMaxIterations(2);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }

    private ClaimEntity buildClaimEntity(String id, String product, String dimension) {
        ClaimEntity c = new ClaimEntity();
        c.setClaimId(id);
        c.setTaskId("task-001");
        c.setProductName(product);
        c.setDimension(dimension);
        c.setStatement("声明");
        c.setConfidence(0.9);
        c.setEvidenceIdsJson("[]");
        c.setRiskLevel("low");
        return c;
    }

    private void stubEmptyDependencies() {
        when(evidenceRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(claimRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(reportRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(reviewIssueRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(repairInstructionRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
    }

    private void stubEmptyForOthers() {
        when(claimRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        stubEmptyForReportAndReview();
    }

    private void stubEmptyForReportAndReview() {
        when(reportRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(reviewIssueRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(repairInstructionRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
    }

    private void stubEmptyForReview() {
        when(reviewIssueRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(repairInstructionRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
    }

    private void stubEmptyDependenciesExceptReview() {
        when(evidenceRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(claimRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(reportRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(repairInstructionRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
    }

    private void stubEmptyDependenciesExceptRepair() {
        when(evidenceRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(claimRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(reportRepository.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(reviewIssueRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
    }

    private ProductProfileSetDTO.ProductProfile findProduct(CompetitiveAnalysisState state, String name) {
        return state.getProductProfileSet().getProducts().stream()
                .filter(p -> name.equals(p.getProductName()))
                .findFirst()
                .orElse(null);
    }
}
