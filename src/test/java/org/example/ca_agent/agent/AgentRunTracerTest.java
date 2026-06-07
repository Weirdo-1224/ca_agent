package org.example.ca_agent.agent;

import org.example.ca_agent.assembler.StateAssembler;
import org.example.ca_agent.dto.agent.AgentRunTrace;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AgentRunTracerTest {

    private AgentRunTracer tracer;

    @BeforeEach
    void setUp() {
        StateAssembler stateAssembler = mock(StateAssembler.class);
        tracer = new AgentRunTracer(stateAssembler);
    }

    @Test
    void trace_recordsSuccessAgentRun() {
        CompetitiveAnalysisState state = buildState("task-001");
        AgentNode mockAgent = new AgentNode() {
            @Override
            public AgentType getAgentType() {
                return AgentType.PLANNER_AGENT;
            }

            @Override
            public CompetitiveAnalysisState execute(CompetitiveAnalysisState s) {
                return s;
            }
        };

        tracer.trace(mockAgent, state);

        assertEquals(1, state.getAgentRuns().size());
        AgentRunTrace trace = state.getAgentRuns().get(0);
        assertEquals("task-001", trace.getTaskId());
        assertEquals(AgentType.PLANNER_AGENT, trace.getAgentType());
        assertEquals("SUCCESS", trace.getStatus());
        assertEquals("TaskInputDTO", trace.getInputType());
        assertEquals("TaskPlanDTO", trace.getOutputType());
        assertNotNull(trace.getRunId());
        assertNotNull(trace.getStartTime());
        assertNotNull(trace.getEndTime());
        assertNotNull(trace.getDurationMs());
        assertTrue(trace.getDurationMs() >= 0);
        assertNull(trace.getErrorMessage());
    }

    @Test
    void trace_recordsFailureAgentRun_whenAgentThrowsException() {
        CompetitiveAnalysisState state = buildState("task-002");
        RuntimeException expectedError = new RuntimeException("Agent crashed");
        AgentNode failingAgent = new AgentNode() {
            @Override
            public AgentType getAgentType() {
                return AgentType.COLLECTOR_AGENT;
            }

            @Override
            public CompetitiveAnalysisState execute(CompetitiveAnalysisState s) {
                throw expectedError;
            }
        };

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> tracer.trace(failingAgent, state));

        assertEquals("Agent crashed", thrown.getMessage());
        assertEquals(1, state.getAgentRuns().size());
        AgentRunTrace trace = state.getAgentRuns().get(0);
        assertEquals(AgentType.COLLECTOR_AGENT, trace.getAgentType());
        assertEquals("FAILED", trace.getStatus());
        assertEquals("Agent crashed", trace.getErrorMessage());
        assertNotNull(trace.getDurationMs());
    }

    @Test
    void trace_generatesUniqueRunIdPerExecution() {
        CompetitiveAnalysisState state = buildState("task-003");
        AgentNode mockAgent = new AgentNode() {
            @Override
            public AgentType getAgentType() {
                return AgentType.WRITER_AGENT;
            }

            @Override
            public CompetitiveAnalysisState execute(CompetitiveAnalysisState s) {
                return s;
            }
        };

        tracer.trace(mockAgent, state);
        tracer.trace(mockAgent, state);

        List<AgentRunTrace> runs = state.getAgentRuns();
        assertEquals(2, runs.size());
        assertNotEquals(runs.get(0).getRunId(), runs.get(1).getRunId());
    }

    @Test
    void trace_recordsAllSixAgentTypesCorrectly() {
        CompetitiveAnalysisState state = buildState("task-004");

        for (AgentType type : AgentType.values()) {
            AgentNode agent = createAgent(type);
            tracer.trace(agent, state);
        }

        assertEquals(6, state.getAgentRuns().size());
        for (int i = 0; i < 6; i++) {
            assertEquals(AgentType.values()[i], state.getAgentRuns().get(i).getAgentType());
            assertEquals("SUCCESS", state.getAgentRuns().get(i).getStatus());
        }
    }

    @Test
    void trace_inputOutputTypesMappedCorrectly() {
        CompetitiveAnalysisState state = buildState("task-005");

        tracer.trace(createAgent(AgentType.PLANNER_AGENT), state);
        tracer.trace(createAgent(AgentType.COLLECTOR_AGENT), state);
        tracer.trace(createAgent(AgentType.EXTRACTOR_AGENT), state);
        tracer.trace(createAgent(AgentType.ANALYZER_AGENT), state);
        tracer.trace(createAgent(AgentType.WRITER_AGENT), state);
        tracer.trace(createAgent(AgentType.REVIEWER_AGENT), state);

        assertEquals("TaskInputDTO", state.getAgentRuns().get(0).getInputType());
        assertEquals("TaskPlanDTO + RepairInstructions", state.getAgentRuns().get(1).getInputType());
        assertEquals("RawSourceSetDTO", state.getAgentRuns().get(2).getInputType());
        assertEquals("ProductProfileSetDTO", state.getAgentRuns().get(3).getInputType());
        assertEquals("ProductProfileSetDTO + CompetitiveAnalysisDTO", state.getAgentRuns().get(4).getInputType());
        assertEquals("CompetitiveAnalysisState", state.getAgentRuns().get(5).getInputType());

        assertEquals("TaskPlanDTO", state.getAgentRuns().get(0).getOutputType());
        assertEquals("RawSourceSetDTO", state.getAgentRuns().get(1).getOutputType());
        assertEquals("ProductProfileSetDTO", state.getAgentRuns().get(2).getOutputType());
        assertEquals("CompetitiveAnalysisDTO", state.getAgentRuns().get(3).getOutputType());
        assertEquals("ReportDraftDTO", state.getAgentRuns().get(4).getOutputType());
        assertEquals("ReviewResultDTO", state.getAgentRuns().get(5).getOutputType());
    }

    @Test
    void trace_preservesExceptionType() {
        CompetitiveAnalysisState state = buildState("task-006");
        AgentNode agent = new AgentNode() {
            @Override
            public AgentType getAgentType() {
                return AgentType.ANALYZER_AGENT;
            }

            @Override
            public CompetitiveAnalysisState execute(CompetitiveAnalysisState s) {
                throw new IllegalStateException("Invalid state");
            }
        };

        assertThrows(IllegalStateException.class, () -> tracer.trace(agent, state));
    }

    private CompetitiveAnalysisState buildState(String taskId) {
        CompetitiveAnalysisState state = new CompetitiveAnalysisState();
        org.example.ca_agent.dto.agent.TaskInputDTO input = new org.example.ca_agent.dto.agent.TaskInputDTO();
        input.setTaskId(taskId);
        state.setTaskInput(input);
        return state;
    }

    private AgentNode createAgent(AgentType type) {
        return new AgentNode() {
            @Override
            public AgentType getAgentType() {
                return type;
            }

            @Override
            public CompetitiveAnalysisState execute(CompetitiveAnalysisState s) {
                return s;
            }
        };
    }
}