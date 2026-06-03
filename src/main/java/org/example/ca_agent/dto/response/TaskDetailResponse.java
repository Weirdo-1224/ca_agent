package org.example.ca_agent.dto.response;

import lombok.Data;
import org.example.ca_agent.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskDetailResponse {

    private String taskId;
    private String taskName;
    private String domain;
    private List<String> targetProducts;
    private String analysisGoal;
    private TaskStatus status;
    private Integer iterationCount;
    private Integer maxIterations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
