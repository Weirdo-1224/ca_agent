package org.example.ca_agent.dto.agent;

import lombok.Data;
import org.example.ca_agent.schema.Evidence;

import java.util.List;

@Data
public class ReportDraftDTO {

    private String taskId;
    private String reportTitle;
    private String reportFormat;
    private List<ReportSection> sections;
    private List<Evidence> sourceList;

    @Data
    public static class ReportSection {
        private String sectionId;
        private String title;
        private String content;
        private List<String> relatedClaimIds;
        private List<String> evidenceIds;
    }
}
