package org.example.ca_agent.dto.agent;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import org.example.ca_agent.common.StringOrListDeserializer;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.enums.ReviewIssueType;

import java.util.List;

@Data
public class ReviewResultDTO {

    private String taskId;
    private Boolean passed;
    private Integer score;
    private String summary;
    private List<ReviewIssue> issues;
    private NextAction nextAction;

    @Data
    public static class ReviewIssue {
        private String issueId;
        private String severity;
        private ReviewIssueType type;
        private String description;
        private AgentType targetAgent;
        private String targetProduct;
        private String targetDimension;
        @JsonDeserialize(using = StringOrListDeserializer.class)
        private String repairInstruction;
    }

    @Data
    public static class NextAction {
        private String action;
        private AgentType targetAgent;
        private String reason;
    }
}
