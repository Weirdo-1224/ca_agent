package org.example.ca_agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("repair_instruction")
public class RepairInstructionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String instructionId;
    private String taskId;
    private String repairId;
    private String fromAgent;
    private String targetAgent;
    private String issueIdsJson;
    private String repairType;
    private String targetProduct;
    private String targetSection;
    private String targetDimension;
    private String problemType;
    private String expectedFix;
    private String relatedEvidenceIdsJson;
    private String relatedClaimIdsJson;
    private Integer iteration;
    private String instruction;
    private String priority;
    private LocalDateTime createdAt;
}
