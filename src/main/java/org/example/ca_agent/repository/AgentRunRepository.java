package org.example.ca_agent.repository;

import org.example.ca_agent.entity.AgentRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentRunRepository extends JpaRepository<AgentRunEntity, Long> {
}
