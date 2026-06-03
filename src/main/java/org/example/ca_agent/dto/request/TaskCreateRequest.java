package org.example.ca_agent.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class TaskCreateRequest {

    private String taskName;
    private String domain;
    private List<String> targetProducts;
    private String analysisGoal;
    private String outputFormat;
    private String language;
    private Integer maxIterations;
}
