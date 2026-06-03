package org.example.ca_agent.dto.agent;

import lombok.Data;

import java.util.List;

@Data
public class CompetitiveAnalysisDTO {

    private String taskId;
    private List<ComparisonMatrixItem> comparisonMatrix;
    private List<KeyFinding> keyFindings;
    private List<ProductOpportunity> productOpportunities;
    private List<Risk> risks;
    private List<SwotSummary> swotSummary;

    @Data
    public static class ComparisonMatrixItem {
        private String dimension;
        private String subDimension;
        private List<ComparisonProductItem> items;
    }

    @Data
    public static class ComparisonProductItem {
        private String productName;
        private String supportLevel;
        private String summary;
        private List<String> evidenceIds;
    }

    @Data
    public static class KeyFinding {
        private String findingId;
        private String title;
        private String description;
        private List<String> relatedProducts;
        private List<String> evidenceIds;
        private Double confidence;
    }

    @Data
    public static class ProductOpportunity {
        private String opportunityId;
        private String title;
        private String description;
        private List<String> targetUsers;
        private List<String> requiredCapabilities;
        private String priority;
        private List<String> evidenceIds;
    }

    @Data
    public static class Risk {
        private String riskId;
        private String title;
        private String description;
        private String severity;
        private List<String> evidenceIds;
    }

    @Data
    public static class SwotSummary {
        private String productName;
        private List<SwotItem> strengths;
        private List<SwotItem> weaknesses;
        private List<SwotItem> opportunities;
        private List<SwotItem> threats;
    }

    @Data
    public static class SwotItem {
        private String point;
        private String explanation;
        private List<String> evidenceIds;
    }
}
