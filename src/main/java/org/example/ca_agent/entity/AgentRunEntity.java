package org.example.ca_agent.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "agent_run")
public class AgentRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
