package org.example.ca_agent.agent;

import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;

public interface AgentNode {

    AgentType getAgentType();

    CompetitiveAnalysisState execute(CompetitiveAnalysisState state);
}
