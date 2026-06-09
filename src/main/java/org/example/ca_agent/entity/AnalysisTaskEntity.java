package org.example.ca_agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("analysis_task")
public class AnalysisTaskEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;
    private String taskName;
    private String domain;
    private String targetProductsJson;
    private String analysisGoal;
    private String status;
    private Integer iterationCount;
    private Integer maxIterations;
    private Boolean reviewPassed;
    private Integer reviewScore;
    private String reviewSummary;
    private String nextActionJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
