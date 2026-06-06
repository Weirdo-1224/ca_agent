package org.example.ca_agent.dto.metaso;

import lombok.Data;

@Data
public class MetasoSearchRequest {

    private String q;
    private String scope = "webpage";
    private boolean includeSummary = false;
    private String size = "10";
}
