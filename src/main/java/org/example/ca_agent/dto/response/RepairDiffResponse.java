package org.example.ca_agent.dto.response;

import lombok.Data;
import org.example.ca_agent.dto.agent.RepairDiffDTO;

import java.util.List;

@Data
public class RepairDiffResponse {

    private String taskId;
    private List<RepairDiffDTO> repairDiffs;
}
