package org.example.ca_agent.schema;

import lombok.Data;

import java.util.List;

@Data
public class SupportItem {

    /**
     * 允许值：true / false / partial / unknown
     */
    private String supported;
    private String description;
    private List<String> evidenceIds;
}
