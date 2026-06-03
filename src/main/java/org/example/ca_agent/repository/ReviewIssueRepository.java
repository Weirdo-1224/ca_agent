package org.example.ca_agent.repository;

import org.example.ca_agent.entity.ReviewIssueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewIssueRepository extends JpaRepository<ReviewIssueEntity, Long> {
}
