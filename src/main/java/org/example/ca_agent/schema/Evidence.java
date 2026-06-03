package org.example.ca_agent.schema;

import lombok.Data;
import org.example.ca_agent.enums.ReliabilityLevel;
import org.example.ca_agent.enums.SourceType;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Evidence {

    private String evidenceId;
    private String productName;
    private SourceType sourceType;
    private String sourceTitle;
    private String url;
    private String contentSnippet;
    private LocalDateTime collectedAt;
    private ReliabilityLevel reliability;
    private List<String> usedFor;
}
