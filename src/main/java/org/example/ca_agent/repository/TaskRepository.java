package org.example.ca_agent.repository;

import org.example.ca_agent.entity.AnalysisTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<AnalysisTaskEntity, Long> {
}
