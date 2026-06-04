package org.example.ca_agent.repository;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.ca_agent.entity.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceLayerContractTest {

    @Test
    void allRepositoriesExtendBaseMapper() {
        assertTrue(BaseMapper.class.isAssignableFrom(TaskRepository.class));
        assertTrue(BaseMapper.class.isAssignableFrom(EvidenceRepository.class));
        assertTrue(BaseMapper.class.isAssignableFrom(ClaimRepository.class));
        assertTrue(BaseMapper.class.isAssignableFrom(ReportRepository.class));
        assertTrue(BaseMapper.class.isAssignableFrom(ReviewIssueRepository.class));
        assertTrue(BaseMapper.class.isAssignableFrom(RepairInstructionRepository.class));
        assertTrue(BaseMapper.class.isAssignableFrom(AgentRunRepository.class));
    }

    @Test
    void allEntitiesDeclareTableName() {
        assertEquals("analysis_task", AnalysisTaskEntity.class.getAnnotation(TableName.class).value());
        assertEquals("evidence", EvidenceEntity.class.getAnnotation(TableName.class).value());
        assertEquals("claim", ClaimEntity.class.getAnnotation(TableName.class).value());
        assertEquals("report", ReportEntity.class.getAnnotation(TableName.class).value());
        assertEquals("review_issue", ReviewIssueEntity.class.getAnnotation(TableName.class).value());
        assertEquals("repair_instruction", RepairInstructionEntity.class.getAnnotation(TableName.class).value());
    }

    @Test
    void allEntitiesHaveAutoIncrementId() throws NoSuchFieldException {
        assertNotNull(AnalysisTaskEntity.class.getDeclaredField("id").getAnnotation(TableId.class));
        assertNotNull(EvidenceEntity.class.getDeclaredField("id").getAnnotation(TableId.class));
        assertNotNull(ClaimEntity.class.getDeclaredField("id").getAnnotation(TableId.class));
        assertNotNull(ReportEntity.class.getDeclaredField("id").getAnnotation(TableId.class));
        assertNotNull(ReviewIssueEntity.class.getDeclaredField("id").getAnnotation(TableId.class));
        assertNotNull(RepairInstructionEntity.class.getDeclaredField("id").getAnnotation(TableId.class));
    }
}
