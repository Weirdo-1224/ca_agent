package org.example.ca_agent.agent;

import org.example.ca_agent.common.JsonUtils;
import org.example.ca_agent.config.AgentModeProperties;
import org.example.ca_agent.dto.agent.CompetitiveAnalysisDTO;
import org.example.ca_agent.dto.agent.ProductProfileSetDTO;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.enums.TaskStatus;
import org.example.ca_agent.prompt.AnalyzerPrompt;
import org.example.ca_agent.service.AgentOutputValidator;
import org.example.ca_agent.service.StructuredLlmService;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnalyzerAgent implements AgentNode {

    private final AgentModeProperties modeProperties;
    private final StructuredLlmService structuredLlmService;
    private final AnalyzerPrompt analyzerPrompt;
    private final AgentOutputValidator outputValidator;

    public AnalyzerAgent() {
        this(new AgentModeProperties(), null, null, null);
    }

    @Autowired
    public AnalyzerAgent(
            AgentModeProperties modeProperties,
            StructuredLlmService structuredLlmService,
            AnalyzerPrompt analyzerPrompt,
            AgentOutputValidator outputValidator
    ) {
        this.modeProperties = modeProperties;
        this.structuredLlmService = structuredLlmService;
        this.analyzerPrompt = analyzerPrompt;
        this.outputValidator = outputValidator;
    }

    @Override
    public AgentType getAgentType() {
        return AgentType.ANALYZER_AGENT;
    }

    @Override
    public CompetitiveAnalysisState execute(CompetitiveAnalysisState state) {
        state.setStatus(TaskStatus.ANALYZING);
        if (modeProperties.isLlm()) {
            ProductProfileSetDTO profileSet = state.getProductProfileSet();
            CompetitiveAnalysisDTO analysis = structuredLlmService.generate(
                    AnalyzerPrompt.SYSTEM_PROMPT,
                    analyzerPrompt.buildUserPrompt(
                            JsonUtils.toJson(profileSet),
                            JsonUtils.toJson(state.getRawSourceSet().getEvidencePool()),
                            JsonUtils.toJson(state.getRepairInstructions()),
                            state.getTaskInput().getLanguage()
                    ),
                    CompetitiveAnalysisDTO.class
            );
            analysis.setTaskId(profileSet.getTaskId());
            outputValidator.validateAnalyzer(
                    analysis,
                    profileSet.getTaskId(),
                    profileSet.getProducts().stream()
                            .map(ProductProfileSetDTO.ProductProfile::getProductName)
                            .toList(),
                    state.getRawSourceSet().getEvidencePool()
            );
            state.setCompetitiveAnalysis(analysis);
            return state;
        }

        CompetitiveAnalysisDTO analysis = new CompetitiveAnalysisDTO();
        analysis.setTaskId(state.getProductProfileSet().getTaskId());
        analysis.setComparisonMatrix(List.of(
                comparison("core_capabilities", "code_generation", state),
                comparison("agent_capabilities", "multi_file_editing", state),
                comparison("codebase_understanding", "project_qa", state),
                comparison("pricing", "free_plan", state)
        ));
        analysis.setKeyFindings(List.of(keyFinding(state, "finding_001"), keyFinding(state, "finding_002")));
        analysis.setProductOpportunities(List.of(productOpportunity(state)));
        analysis.setRisks(List.of(risk("risk_001", state), risk("risk_002", state)));
        analysis.setSwotSummary(state.getProductProfileSet().getProducts().stream()
                .map(this::swot)
                .toList());
        state.setCompetitiveAnalysis(analysis);
        return state;
    }

    private CompetitiveAnalysisDTO.ComparisonMatrixItem comparison(String dimension, String subDimension, CompetitiveAnalysisState state) {
        CompetitiveAnalysisDTO.ComparisonMatrixItem item = new CompetitiveAnalysisDTO.ComparisonMatrixItem();
        item.setDimension(dimension);
        item.setSubDimension(subDimension);
        item.setItems(state.getProductProfileSet().getProducts().stream()
                .map(profile -> comparisonProduct(profile, dimension))
                .toList());
        return item;
    }

    private CompetitiveAnalysisDTO.ComparisonProductItem comparisonProduct(ProductProfileSetDTO.ProductProfile profile, String dimension) {
        CompetitiveAnalysisDTO.ComparisonProductItem item = new CompetitiveAnalysisDTO.ComparisonProductItem();
        item.setProductName(profile.getProductName());
        item.setSupportLevel("medium");
        item.setSummary(profile.getProductName() + " has mock support for " + dimension + ".");
        item.setEvidenceIds(profile.getClaims().get(0).getEvidenceIds());
        return item;
    }

    private CompetitiveAnalysisDTO.KeyFinding keyFinding(CompetitiveAnalysisState state, String findingId) {
        CompetitiveAnalysisDTO.KeyFinding finding = new CompetitiveAnalysisDTO.KeyFinding();
        finding.setFindingId(findingId);
        finding.setTitle("AI coding tools are evolving toward agent workflows");
        finding.setDescription("Mock analysis shows products are compared across coding and agent dimensions.");
        finding.setRelatedProducts(productNames(state));
        finding.setEvidenceIds(allEvidenceIds(state));
        finding.setConfidence(0.82);
        return finding;
    }

    private CompetitiveAnalysisDTO.ProductOpportunity productOpportunity(CompetitiveAnalysisState state) {
        CompetitiveAnalysisDTO.ProductOpportunity opportunity = new CompetitiveAnalysisDTO.ProductOpportunity();
        opportunity.setOpportunityId("opportunity_001");
        opportunity.setTitle("Improve evidence-backed enterprise controls");
        opportunity.setDescription("Mock opportunity based on unknown enterprise feature fields.");
        opportunity.setTargetUsers(List.of("Product teams", "Engineering managers"));
        opportunity.setRequiredCapabilities(List.of("admin_console", "audit_log"));
        opportunity.setPriority("medium");
        opportunity.setEvidenceIds(allEvidenceIds(state));
        return opportunity;
    }

    private CompetitiveAnalysisDTO.Risk risk(String riskId, CompetitiveAnalysisState state) {
        CompetitiveAnalysisDTO.Risk risk = new CompetitiveAnalysisDTO.Risk();
        risk.setRiskId(riskId);
        risk.setTitle("Evidence coverage risk");
        risk.setDescription("Some fields remain unknown in mock extraction.");
        risk.setSeverity("medium");
        risk.setEvidenceIds(allEvidenceIds(state));
        return risk;
    }

    private CompetitiveAnalysisDTO.SwotSummary swot(ProductProfileSetDTO.ProductProfile profile) {
        CompetitiveAnalysisDTO.SwotSummary swot = new CompetitiveAnalysisDTO.SwotSummary();
        swot.setProductName(profile.getProductName());
        swot.setStrengths(List.of(swotItem("Evidence-backed core capabilities", profile)));
        swot.setWeaknesses(List.of(swotItem("Some enterprise details are unknown", profile)));
        swot.setOpportunities(List.of(swotItem("Expand agent workflows", profile)));
        swot.setThreats(List.of(swotItem("Competitive feature convergence", profile)));
        return swot;
    }

    private CompetitiveAnalysisDTO.SwotItem swotItem(String point, ProductProfileSetDTO.ProductProfile profile) {
        CompetitiveAnalysisDTO.SwotItem item = new CompetitiveAnalysisDTO.SwotItem();
        item.setPoint(point);
        item.setExplanation("Mock SWOT item for " + profile.getProductName() + ".");
        item.setEvidenceIds(profile.getClaims().get(0).getEvidenceIds());
        return item;
    }

    private List<String> productNames(CompetitiveAnalysisState state) {
        return state.getProductProfileSet().getProducts().stream()
                .map(ProductProfileSetDTO.ProductProfile::getProductName)
                .toList();
    }

    private List<String> allEvidenceIds(CompetitiveAnalysisState state) {
        return state.getRawSourceSet().getEvidencePool().stream()
                .map(org.example.ca_agent.schema.Evidence::getEvidenceId)
                .toList();
    }
}
