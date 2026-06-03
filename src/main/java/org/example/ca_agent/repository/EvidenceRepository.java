package org.example.ca_agent.repository;

import org.example.ca_agent.entity.EvidenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvidenceRepository extends JpaRepository<EvidenceEntity, Long> {
}
