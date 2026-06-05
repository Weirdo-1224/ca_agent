package org.example.ca_agent.agent;

import org.example.ca_agent.common.JsonUtils;
import org.example.ca_agent.config.AgentModeProperties;
import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.example.ca_agent.dto.agent.TaskPlanDTO;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.enums.SourceType;
import org.example.ca_agent.enums.TaskStatus;
import org.example.ca_agent.mock.MockCompetitiveAnalysisFixtures;
import org.example.ca_agent.prompt.PlannerPrompt;
import org.example.ca_agent.service.AgentOutputValidator;
import org.example.ca_agent.service.StructuredLlmService;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlannerAgent implements AgentNode {

    private final AgentModeProperties modeProperties;
    private final StructuredLlmService structuredLlmService;
    private final PlannerPrompt plannerPrompt;
    private final AgentOutputValidator outputValidator;

    public PlannerAgent() {
        this(new AgentModeProperties(), null, null, null);
    }

    @Autowired
    public PlannerAgent(
            AgentModeProperties modeProperties,
            StructuredLlmService structuredLlmService,
            PlannerPrompt plannerPrompt,
            AgentOutputValidator outputValidator
    ) {
        this.modeProperties = modeProperties;
        this.structuredLlmService = structuredLlmService;
        this.plannerPrompt = plannerPrompt;
        this.outputValidator = outputValidator;
    }

    @Override
    public AgentType getAgentType() {
        return AgentType.PLANNER_AGENT;
    }

    @Override
    public CompetitiveAnalysisState execute(CompetitiveAnalysisState state) {
        state.setStatus(TaskStatus.PLANNING);
        TaskInputDTO input = state.getTaskInput();
        if (modeProperties.isLlm()) {
            TaskPlanDTO taskPlan = structuredLlmService.generate(
                    PlannerPrompt.SYSTEM_PROMPT,
                    plannerPrompt.buildUserPrompt(JsonUtils.toJson(input)),
                    TaskPlanDTO.class
            );
            taskPlan.setTaskId(input.getTaskId());
            outputValidator.validatePlanner(taskPlan, input);
            state.setTaskPlan(taskPlan);
            return state;
        }

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
