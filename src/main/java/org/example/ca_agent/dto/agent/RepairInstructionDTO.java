package org.example.ca_agent.dto.agent;

import lombok.Data;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.enums.RepairType;

import java.util.List;

@Data
public class RepairInstructionDTO {

    private String taskId;
    private String repairId;
    private AgentType fromAgent;
    private AgentType targetAgent;
    private List<String> issueIds;
    private RepairType repairType;
    private String targetProduct;
    private String targetDimension;
    private String instruction;
    private String priority;
}
