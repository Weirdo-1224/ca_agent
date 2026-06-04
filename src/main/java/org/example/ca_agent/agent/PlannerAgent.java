package org.example.ca_agent.agent;

import lombok.RequiredArgsConstructor;
import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.example.ca_agent.dto.agent.TaskPlanDTO;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.enums.SourceType;
import org.example.ca_agent.enums.TaskStatus;
import org.example.ca_agent.mock.MockCompetitiveAnalysisFixtures;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PlannerAgent implements AgentNode {

    @Override
    public AgentType getAgentType() {
        return AgentType.PLANNER_AGENT;
    }

    @Override
    public CompetitiveAnalysisState execute(CompetitiveAnalysisState state) {
        state.setStatus(TaskStatus.PLANNING);
        TaskInputDTO input = state.getTaskInput();

        TaskPlanDTO taskPlan = new TaskPlanDTO();
        taskPlan.setTaskId(input.getTaskId());
        taskPlan.setDetectedDomain(MockCompetitiveAnalysisFixtures.DOMAIN);
        taskPlan.setTemplateId(MockCompetitiveAnalysisFixtures.TEMPLATE_ID);
        taskPlan.setConfidence(MockCompetitiveAnalysisFixtures.PLANNER_CONFIDENCE);
        taskPlan.setProducts(input.getTargetProducts());
        taskPlan.setAnalysisGoal(input.getAnalysisGoal());
        taskPlan.setAnalysisDimensions(MockCompetitiveAnalysisFixtures.ANALYSIS_DIMENSIONS);
        taskPlan.setCollectionTasks(input.getTargetProducts().stream()
                .map(this::buildCollectionTask)
                .toList());
        taskPlan.setWorkflow(MockCompetitiveAnalysisFixtures.WORKFLOW);
        state.setTaskPlan(taskPlan);
        return state;
    }

    private TaskPlanDTO.CollectionTask buildCollectionTask(String productName) {
        TaskPlanDTO.CollectionTask collectionTask = new TaskPlanDTO.CollectionTask();
        collectionTask.setProductName(productName);
        collectionTask.setQueries(MockCompetitiveAnalysisFixtures.collectionQueries(productName));
        collectionTask.setTargetDimensions(MockCompetitiveAnalysisFixtures.collectionTargetDimensions());
        collectionTask.setPreferredSourceTypes(List.of(
                SourceType.OFFICIAL_SITE,
                SourceType.PRICING_PAGE,
                SourceType.DOCUMENTATION
        ));
        return collectionTask;
    }
}
