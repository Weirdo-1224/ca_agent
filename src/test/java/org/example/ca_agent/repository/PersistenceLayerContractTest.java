package org.example.ca_agent.repository;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.ca_agent.entity.AnalysisTaskEntity;
import org.example.ca_agent.entity.EvidenceEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistenceLayerContractTest {

    @Test
    void repositoriesUseMybatisPlusMapperContracts() {
        assertTrue(BaseMapper.class.isAssignableFrom(TaskRepository.class));
        assertTrue(BaseMapper.class.isAssignableFrom(EvidenceRepository.class));
    }

    @Test
    void entitiesDeclareDatabaseTableNamesForMybatisPlus() {
        assertEquals("analysis_task", AnalysisTaskEntity.class.getAnnotation(TableName.class).value());
        assertEquals("evidence", EvidenceEntity.class.getAnnotation(TableName.class).value());
    }
}
