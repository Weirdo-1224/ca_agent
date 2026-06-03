package org.example.ca_agent.dto.response;

import lombok.Data;
import org.example.ca_agent.dto.agent.ReportDraftDTO;
import org.example.ca_agent.dto.agent.ReviewResultDTO;
import org.example.ca_agent.schema.Evidence;

import java.util.List;

@Data
public class ReportResponse {

    private String taskId;
    private String reportTitle;
    private String reportFormat;
    private List<ReportDraftDTO.ReportSection> sections;
    private List<Evidence> sourceList;
    private ReviewResultDTO reviewResult;
}
