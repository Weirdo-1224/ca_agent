package org.example.ca_agent.schema;

import lombok.Data;

import java.util.List;

@Data
public class Claim {

    private String claimId;
    private String productName;
    private String dimension;
    private String statement;
    private Double confidence;
    private List<String> evidenceIds;
    /**
     * 风险等级：low / medium / high
     */
    private String riskLevel;
}
