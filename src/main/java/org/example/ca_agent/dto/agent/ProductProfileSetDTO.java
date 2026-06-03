package org.example.ca_agent.dto.agent;

import lombok.Data;
import org.example.ca_agent.schema.CapabilityItem;
import org.example.ca_agent.schema.Claim;
import org.example.ca_agent.schema.SupportItem;

import java.util.List;

@Data
public class ProductProfileSetDTO {

    private String taskId;
    private List<ProductProfile> products;

    @Data
    public static class ProductProfile {
        private String productName;
        private String company;
        private String officialUrl;
        private String productType;
        private Positioning positioning;
        private List<TargetUser> targetUsers;
        private CoreCapabilities coreCapabilities;
        private AgentCapabilities agentCapabilities;
        private CodebaseUnderstanding codebaseUnderstanding;
        private IdeEcosystem ideEcosystem;
        private ModelContext modelContext;
        private Pricing pricing;
        private EnterpriseFeatures enterpriseFeatures;
        private UserFeedback userFeedback;
        private List<Claim> claims;
        private List<MissingField> missingFields;
    }

    @Data
    public static class MissingField {
        private String fieldPath;
        private String reason;
    }

    @Data
    public static class Positioning {
        private String summary;
        private List<String> mainScenarios;
        private String differentiation;
        private List<String> evidenceIds;
    }

    @Data
    public static class TargetUser {
        private String userGroup;
        private List<String> useCases;
        private List<String> painPoints;
        private List<String> evidenceIds;
    }

    @Data
    public static class CoreCapabilities {
        private CapabilityItem codeCompletion;
        private CapabilityItem codeGeneration;
        private CapabilityItem codeExplanation;
        private CapabilityItem refactoring;
        private CapabilityItem unitTestGeneration;
        private CapabilityItem debugAssistance;
        private CapabilityItem documentationGeneration;
    }

    @Data
    public static class AgentCapabilities {
        private SupportItem taskPlanning;
        private SupportItem multiFileEditing;
        private SupportItem terminalExecution;
        private SupportItem testRunAndFix;
        private SupportItem codeReview;
        private SupportItem autonomousLoop;
    }

    @Data
    public static class CodebaseUnderstanding {
        private SupportItem repositoryIndexing;
        private SupportItem crossFileReference;
        private SupportItem projectQa;
        private SupportItem longContextSupport;
    }

    @Data
    public static class IdeEcosystem {
        private List<SupportedIde> supportedIdes;
        private List<String> platforms;
        private List<Integration> integrations;
    }

    @Data
    public static class SupportedIde {
        private String name;
        private String supportType;
        private List<String> evidenceIds;
    }

    @Data
    public static class Integration {
        private String name;
        private String description;
        private List<String> evidenceIds;
    }

    @Data
    public static class ModelContext {
        private List<SupportedModel> supportedModels;
        private SupportItem bringYourOwnKey;
        private SupportItem localModelSupport;
        private ContextWindow contextWindow;
    }

    @Data
    public static class SupportedModel {
        private String modelName;
        private String provider;
        private List<String> evidenceIds;
    }

    @Data
    public static class ContextWindow {
        private String value;
        private String description;
        private List<String> evidenceIds;
    }

    @Data
    public static class Pricing {
        private String hasFreePlan;
        private List<Plan> plans;
        private EnterprisePlan enterprisePlan;
    }

    @Data
    public static class Plan {
        private String planName;
        private String price;
        private String billingCycle;
        private String targetUser;
        private List<String> mainLimits;
        private List<String> evidenceIds;
    }

    @Data
    public static class EnterprisePlan {
        private String available;
        private String pricingType;
        private List<String> features;
        private List<String> evidenceIds;
    }

    @Data
    public static class EnterpriseFeatures {
        private SupportItem sso;
        private SupportItem adminConsole;
        private SupportItem privacyControl;
        private SupportItem auditLog;
        private SupportItem privateDeployment;
    }

    @Data
    public static class UserFeedback {
        private List<FeedbackPoint> positivePoints;
        private List<FeedbackPoint> negativePoints;
        private List<PainPoint> commonPainPoints;
    }

    @Data
    public static class FeedbackPoint {
        private String point;
        private String frequency;
        private List<String> evidenceIds;
    }

    @Data
    public static class PainPoint {
        private String painPoint;
        private List<String> affectedUsers;
        private List<String> evidenceIds;
    }
}
