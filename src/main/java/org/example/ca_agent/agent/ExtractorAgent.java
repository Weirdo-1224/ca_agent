package org.example.ca_agent.agent;

import org.example.ca_agent.common.JsonUtils;
import org.example.ca_agent.config.AgentModeProperties;
import org.example.ca_agent.dto.agent.ProductProfileSetDTO;
import org.example.ca_agent.dto.agent.RawSourceSetDTO;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.enums.TaskStatus;
import org.example.ca_agent.schema.Claim;
import org.example.ca_agent.schema.Evidence;
import org.example.ca_agent.mock.MockCompetitiveAnalysisFixtures;
import org.example.ca_agent.prompt.ExtractorPrompt;
import org.example.ca_agent.service.AgentOutputValidator;
import org.example.ca_agent.service.StructuredLlmService;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExtractorAgent implements AgentNode {

    private final AgentModeProperties modeProperties;
    private final StructuredLlmService structuredLlmService;
    private final ExtractorPrompt extractorPrompt;
    private final AgentOutputValidator outputValidator;

    public ExtractorAgent() {
        this(new AgentModeProperties(), null, null, null);
    }

    @Autowired
    public ExtractorAgent(
            AgentModeProperties modeProperties,
            StructuredLlmService structuredLlmService,
            ExtractorPrompt extractorPrompt,
            AgentOutputValidator outputValidator
    ) {
        this.modeProperties = modeProperties;
        this.structuredLlmService = structuredLlmService;
        this.extractorPrompt = extractorPrompt;
        this.outputValidator = outputValidator;
    }

    @Override
    public AgentType getAgentType() {
        return AgentType.EXTRACTOR_AGENT;
    }

    @Override
    public CompetitiveAnalysisState execute(CompetitiveAnalysisState state) {
        state.setStatus(TaskStatus.EXTRACTING);
        if (modeProperties.isLlm()) {
            RawSourceSetDTO rawSourceSet = state.getRawSourceSet();
            ProductProfileSetDTO profileSet = structuredLlmService.generate(
                    ExtractorPrompt.SYSTEM_PROMPT,
                    extractorPrompt.buildUserPrompt(
                            JsonUtils.toJson(rawSourceSet),
                            JsonUtils.toJson(state.getRepairInstructions()),
                            state.getTaskInput().getLanguage()
                    ),
                    ProductProfileSetDTO.class
            );
            profileSet.setTaskId(rawSourceSet.getTaskId());
            outputValidator.validateExtractor(
                    profileSet,
                    rawSourceSet.getTaskId(),
                    rawSourceSet.getEvidencePool()
            );
            state.setProductProfileSet(profileSet);
            return state;
        }

        ProductProfileSetDTO profileSet = new ProductProfileSetDTO();
        profileSet.setTaskId(state.getRawSourceSet().getTaskId());
        profileSet.setProducts(state.getRawSourceSet().getEvidencePool().stream()
                .map(Evidence::getProductName)
                .distinct()
                .map(productName -> buildProductProfile(productName, evidenceIdsForProduct(state, productName)))
                .toList());
        state.setProductProfileSet(profileSet);
        return state;
    }

    private ProductProfileSetDTO.ProductProfile buildProductProfile(String productName, List<String> evidenceIds) {
        ProductProfileSetDTO.ProductProfile profile = new ProductProfileSetDTO.ProductProfile();
        profile.setProductName(productName);
        profile.setCompany(productName + " Company");
        profile.setOfficialUrl("https://example.com/" + productName.toLowerCase().replaceAll("[^a-z0-9]+", "_"));
        profile.setProductType("AI_CODING_TOOL");
        profile.setPositioning(MockCompetitiveAnalysisFixtures.positioning(productName, evidenceIds));
        profile.setTargetUsers(List.of(MockCompetitiveAnalysisFixtures.targetUser(evidenceIds)));
        profile.setCoreCapabilities(MockCompetitiveAnalysisFixtures.coreCapabilities(evidenceIds));
        profile.setAgentCapabilities(MockCompetitiveAnalysisFixtures.agentCapabilities(evidenceIds));
        profile.setCodebaseUnderstanding(MockCompetitiveAnalysisFixtures.codebaseUnderstanding(evidenceIds));
        profile.setIdeEcosystem(MockCompetitiveAnalysisFixtures.ideEcosystem(evidenceIds));
        profile.setModelContext(MockCompetitiveAnalysisFixtures.modelContext(evidenceIds));
        profile.setPricing(MockCompetitiveAnalysisFixtures.pricing(evidenceIds));
        profile.setEnterpriseFeatures(MockCompetitiveAnalysisFixtures.enterpriseFeatures(evidenceIds));
        profile.setUserFeedback(MockCompetitiveAnalysisFixtures.userFeedback(evidenceIds));
        profile.setClaims(List.of(claim(productName, evidenceIds)));
        profile.setMissingFields(List.of(MockCompetitiveAnalysisFixtures.missingField("modelContext.contextWindow")));
        return profile;
    }

    private List<String> evidenceIdsForProduct(CompetitiveAnalysisState state, String productName) {
        return state.getRawSourceSet().getEvidencePool().stream()
                .filter(evidence -> productName.equals(evidence.getProductName()))
                .map(Evidence::getEvidenceId)
                .toList();
    }

    private Claim claim(String productName, List<String> evidenceIds) {
        Claim claim = new Claim();
        claim.setClaimId("claim_" + productName.toLowerCase().replaceAll("[^a-z0-9]+", "_") + "_001");
        claim.setProductName(productName);
        claim.setDimension("positioning");
        claim.setStatement(productName + " has mock evidence-backed AI coding assistant capabilities.");
        claim.setConfidence(0.8);
        claim.setEvidenceIds(evidenceIds);
        claim.setRiskLevel("low");
        return claim;
    }

}
