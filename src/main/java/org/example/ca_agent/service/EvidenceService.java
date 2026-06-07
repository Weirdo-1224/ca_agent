package org.example.ca_agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.ca_agent.common.BizException;
import org.example.ca_agent.common.JsonUtils;
import org.example.ca_agent.entity.AnalysisTaskEntity;
import org.example.ca_agent.enums.ReliabilityLevel;
import org.example.ca_agent.enums.SourceType;
import org.example.ca_agent.repository.EvidenceRepository;
import org.example.ca_agent.repository.TaskRepository;
import org.example.ca_agent.schema.Evidence;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;
    private final TaskRepository taskRepository;

    public List<Evidence> getEvidenceList(String taskId) {
        ensureTaskExists(taskId);
        return evidenceRepository.selectList(
                        new LambdaQueryWrapper<org.example.ca_agent.entity.EvidenceEntity>()
                                .eq(org.example.ca_agent.entity.EvidenceEntity::getTaskId, taskId))
                .stream()
                .map(e -> {
                    Evidence ev = new Evidence();
                    ev.setEvidenceId(e.getEvidenceId());
                    ev.setProductName(e.getProductName());
                    ev.setSourceType(parseEnum(SourceType.class, e.getSourceType()));
                    ev.setSourceTitle(e.getSourceTitle());
                    ev.setUrl(e.getUrl());
                    ev.setContentSnippet(e.getContentSnippet());
                    ev.setCollectedAt(e.getCollectedAt());
                    ev.setReliability(parseEnum(ReliabilityLevel.class, e.getReliability()));
                    ev.setUsedFor(parseJsonList(e.getUsedForJson()));
                    return ev;
                }).toList();
    }

    private void ensureTaskExists(String taskId) {
        AnalysisTaskEntity entity = taskRepository.selectOne(
                new LambdaQueryWrapper<AnalysisTaskEntity>().eq(AnalysisTaskEntity::getTaskId, taskId));
        if (entity == null) {
            throw new BizException(404, "Task not found: " + taskId);
        }
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return JsonUtils.fromJsonList(json, String.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> clazz, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(clazz, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
