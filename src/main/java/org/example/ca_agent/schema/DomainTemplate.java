package org.example.ca_agent.schema;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DomainTemplate {

    private String templateId;
    private String domain;
    private String domainName;
    private String description;
    private String version;
    private List<AnalysisDimension> analysisDimensions;
    private Map<String, Object> sourceStrategy;
    private Map<String, Object> productSchema;
    private Map<String, Object> reportTemplate;
    private Map<String, Object> reviewRules;

    @Data
    public static class AnalysisDimension {
        private String dimensionId;
        private String name;
        private String description;
    }
}
