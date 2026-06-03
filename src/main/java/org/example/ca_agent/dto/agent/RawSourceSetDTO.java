package org.example.ca_agent.dto.agent;

import lombok.Data;
import org.example.ca_agent.enums.ReliabilityLevel;
import org.example.ca_agent.enums.SourceType;
import org.example.ca_agent.schema.Evidence;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RawSourceSetDTO {

    private String taskId;
    private List<RawSource> rawSources;
    private List<Evidence> evidencePool;
    private List<MissingSource> missingSources;

    @Data
    public static class RawSource {
        private String sourceId;
        private String productName;
        private SourceType sourceType;
        private String title;
        private String url;
        private String rawText;
        private String contentSnippet;
        private LocalDateTime collectedAt;
        private ReliabilityLevel reliability;
        private List<String> targetDimensions;
    }

    @Data
    public static class MissingSource {
        private String productName;
        private String targetDimension;
        private String reason;
        private String suggestedQuery;
    }
}
