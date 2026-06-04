package org.example.ca_agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("claim")
public class ClaimEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String claimId;
    private String taskId;
    private String productName;
    private String dimension;
    private String statement;
    private Double confidence;
    private String evidenceIdsJson;
    private String riskLevel;
    private LocalDateTime createdAt;
}
