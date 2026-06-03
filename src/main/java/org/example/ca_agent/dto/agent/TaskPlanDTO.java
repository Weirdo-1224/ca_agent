package org.example.ca_agent.dto.agent;

import lombok.Data;
import org.example.ca_agent.enums.SourceType;

import java.util.List;

@Data
public class TaskPlanDTO {

    private String taskId;
    private String detectedDomain;
    private String templateId;
    private Double confidence;
    private List<String> products;
    private String analysisGoal;
    private List<String> analysisDimensions;
    private List<CollectionTask> collectionTasks;
    private List<String> workflow;

    @Data
    public static class CollectionTask {
        private String productName;
        private List<String> queries;
        private List<String> targetDimensions;
        private List<SourceType> preferredSourceTypes;
    }
}
