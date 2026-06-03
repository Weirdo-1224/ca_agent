package org.example.ca_agent.workflow;

import lombok.Data;
import org.example.ca_agent.dto.agent.CompetitiveAnalysisDTO;
import org.example.ca_agent.dto.agent.ProductProfileSetDTO;
import org.example.ca_agent.dto.agent.RawSourceSetDTO;
import org.example.ca_agent.dto.agent.RepairInstructionDTO;
import org.example.ca_agent.dto.agent.ReportDraftDTO;
import org.example.ca_agent.dto.agent.ReviewResultDTO;
import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.example.ca_agent.dto.agent.TaskPlanDTO;
import org.example.ca_agent.enums.TaskStatus;

import java.util.List;

@Data
public class CompetitiveAnalysisState {

    private TaskInputDTO taskInput;
    private TaskPlanDTO taskPlan;
    private RawSourceSetDTO rawSourceSet;
    private ProductProfileSetDTO productProfileSet;
    private CompetitiveAnalysisDTO competitiveAnalysis;
    private ReportDraftDTO reportDraft;
    private ReviewResultDTO reviewResult;
    private List<RepairInstructionDTO> repairInstructions;
    private Integer iterationCount;
    private TaskStatus status;

    public void increaseIteration() {
        if (this.iterationCount == null) {
            this.iterationCount = 0;
        }
        this.iterationCount++;
    }

    public boolean isMaxIterationReached() {
        if (this.taskInput == null || this.taskInput.getMaxIterations() == null) {
            return false;
        }
        return this.iterationCount != null && this.iterationCount >= this.taskInput.getMaxIterations();
    }
}
