package org.example.ca_agent.agent;

import lombok.RequiredArgsConstructor;
import org.example.ca_agent.dto.agent.RawSourceSetDTO;
import org.example.ca_agent.dto.agent.TaskPlanDTO;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.enums.ReliabilityLevel;
import org.example.ca_agent.enums.SourceType;
import org.example.ca_agent.enums.TaskStatus;
import org.example.ca_agent.schema.Evidence;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class CollectorAgent implements AgentNode {

    @Override
    public AgentType getAgentType() {
        return AgentType.COLLECTOR_AGENT;
    }

    @Override
    public CompetitiveAnalysisState execute(CompetitiveAnalysisState state) {
        state.setStatus(TaskStatus.COLLECTING);
        TaskPlanDTO taskPlan = state.getTaskPlan();
        List<RawSourceSetDTO.RawSource> rawSources = new ArrayList<>();
        List<Evidence> evidencePool = new ArrayList<>();

        for (String productName : taskPlan.getProducts()) {
            addSourceAndEvidence(taskPlan.getTaskId(), productName, SourceType.OFFICIAL_SITE, "official", rawSources, evidencePool);
            addSourceAndEvidence(taskPlan.getTaskId(), productName, SourceType.PRICING_PAGE, "pricing", rawSources, evidencePool);
            addSourceAndEvidence(taskPlan.getTaskId(), productName, SourceType.DOCUMENTATION, "documentation", rawSources, evidencePool);
        }

        RawSourceSetDTO rawSourceSet = new RawSourceSetDTO();
        rawSourceSet.setTaskId(taskPlan.getTaskId());
        rawSourceSet.setRawSources(rawSources);
        rawSourceSet.setEvidencePool(evidencePool);
        rawSourceSet.setMissingSources(List.of());
        state.setRawSourceSet(rawSourceSet);
        return state;
    }

    private void addSourceAndEvidence(
            String taskId,
            String productName,
            SourceType sourceType,
            String dimension,
            List<RawSourceSetDTO.RawSource> rawSources,
            List<Evidence> evidencePool
    ) {
        String normalized = productName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        int index = evidencePool.size() + 1;
        String sourceId = "src_" + normalized + "_" + index;
        String evidenceId = "ev_" + normalized + "_" + index;
        String title = productName + " " + sourceType.name();
        String url = "https://example.com/" + normalized + "/" + dimension;
        String snippet = productName + " mock " + dimension + " information for competitive analysis.";

        RawSourceSetDTO.RawSource rawSource = new RawSourceSetDTO.RawSource();
        rawSource.setSourceId(sourceId);
        rawSource.setProductName(productName);
        rawSource.setSourceType(sourceType);
        rawSource.setTitle(title);
        rawSource.setUrl(url);
        rawSource.setRawText(snippet);
        rawSource.setContentSnippet(snippet);
        rawSource.setCollectedAt(LocalDateTime.now());
        rawSource.setReliability(ReliabilityLevel.HIGH);
        rawSource.setTargetDimensions(List.of(dimension));
        rawSources.add(rawSource);

        Evidence evidence = new Evidence();
        evidence.setEvidenceId(evidenceId);
        evidence.setProductName(productName);
        evidence.setSourceType(sourceType);
        evidence.setSourceTitle(title);
        evidence.setUrl(url);
        evidence.setContentSnippet(snippet);
        evidence.setCollectedAt(rawSource.getCollectedAt());
        evidence.setReliability(ReliabilityLevel.HIGH);
        evidence.setUsedFor(List.of(dimension));
        evidencePool.add(evidence);
    }
}
