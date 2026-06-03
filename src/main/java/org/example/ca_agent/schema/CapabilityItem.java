package org.example.ca_agent.schema;

import lombok.Data;

import java.util.List;

@Data
public class CapabilityItem {

    /**
     * 允许值：true / false / partial / unknown
     */
    private String supported;
    /**
     * 允许值：high / medium / low / unknown
     */
    private String maturity;
    private String description;
    private List<String> evidenceIds;
}
