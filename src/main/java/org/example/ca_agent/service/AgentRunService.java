package org.example.ca_agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.ca_agent.dto.response.AgentRunResponse;
import org.example.ca_agent.entity.AgentRunEntity;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.repository.AgentRunRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentRunService {

    private final AgentRunRepository agentRunRepository;

    public List<AgentRunResponse> getAgentRuns(String taskId) {
        return agentRunRepository.selectList(
                        new LambdaQueryWrapper<AgentRunEntity>()
                                .eq(AgentRunEntity::getTaskId, taskId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AgentRunResponse toResponse(AgentRunEntity entity) {
        AgentRunResponse response = new AgentRunResponse();
        response.setRunId(entity.getRunId());
        response.setTaskId(entity.getTaskId());
        response.setAgentType(parseEnum(AgentType.class, entity.getAgentType()));
        response.setInputType(entity.getInputType());
        response.setOutputType(entity.getOutputType());
        response.setStatus(entity.getStatus());
        response.setStartTime(entity.getStartTime());
        response.setEndTime(entity.getEndTime());
        response.setDurationMs(entity.getDurationMs());
        response.setErrorMessage(entity.getErrorMessage());
        return response;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> clazz, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(clazz, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
