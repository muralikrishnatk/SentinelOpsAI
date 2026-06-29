package com.sentinel.aiops.repository;

import com.sentinel.aiops.domain.RemediationExecution;
import com.sentinel.aiops.domain.enums.RemediationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface RemediationExecutionRepository extends JpaRepository<RemediationExecution, Long> {
    List<RemediationExecution> findTop50ByOrderByCreatedAtDesc();
    long countByRunbookIdAndCreatedAtAfter(Long runbookId, Instant after);
    boolean existsByRunbookIdAndIncidentIdAndStatus(Long runbookId, Long incidentId, RemediationStatus status);
}