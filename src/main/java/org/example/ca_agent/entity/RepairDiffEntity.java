package org.example.ca_agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("repair_diff")
public class RepairDiffEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;
    private Integer iteration;
    private String targetAgent;
    private Integer beforeScore;
    private Integer afterScore;
    private Integer beforeIssueCount;
    private Integer afterIssueCount;
    private Integer fixedIssueCount;
    private String addedEvidenceIdsJson;
    private String addedClaimIdsJson;
    private String changedSectionsJson;
    private String changedProductsJson;
    private String summary;
    private LocalDateTime createdAt;
}
